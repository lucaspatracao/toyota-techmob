package com.toyota.techmob.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toyota.techmob.backend.coleta.CalculadoraOEE;
import com.toyota.techmob.backend.coleta.CalculadoraOEE.ResultadoOEE;
import com.toyota.techmob.backend.coleta.DetectorPedidos.EtapaDetectada;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoDetectado;
import com.toyota.techmob.backend.domain.EtapaPedido;
import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.LeituraBruta;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.domain.Pedido;
import com.toyota.techmob.backend.domain.SituacaoPedido;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.LeituraBrutaRepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;
import com.toyota.techmob.backend.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class RegistroPedidoServiceImpl implements RegistroPedidoService {

    private static final Logger logger = LoggerFactory.getLogger(RegistroPedidoServiceImpl.class);

    private final MaquinaRepository maquinaRepository;
    private final PedidoRepository pedidoRepository;
    private final IndicadorOEERepository indicadorOEERepository;
    private final LeituraBrutaRepository leituraBrutaRepository;
    private final Clock clock;

    @Override
    public PedidoRegistrado iniciarPedido(Long maquinaId, OrigemPedido origem, Instant inicio) {
        Pedido pedido = pedidoRepository.save(novoPedido(buscarMaquina(maquinaId), origem, inicio));
        return new PedidoRegistrado(pedido.getId(), pedido.getNumero());
    }

    @Override
    public ResultadoOEE concluirPedido(Long maquinaId, Long pedidoId, PedidoDetectado detectado) {
        Maquina maquina = buscarMaquina(maquinaId);
        Pedido pedido = pedidoExistenteOuNovo(maquina, pedidoId, detectado);
        ResultadoOEE resultado = CalculadoraOEE.doPedido(detectado.tempos());

        int pecas = maquina.getPecasPorPedido();
        preencher(pedido, detectado, SituacaoPedido.CONCLUIDO);
        pedido.setPecasBoas(detectado.reprovado() ? 0 : pecas);
        pedido.setPecasDefeituosas(detectado.reprovado() ? pecas : 0);
        pedidoRepository.save(pedido);

        indicadorOEERepository.save(IndicadorOEE.builder()
                .maquina(maquina)
                .pedido(pedido)
                .periodoInicio(data(detectado.inicio()))
                .periodoFim(data(detectado.fim()))
                .disponibilidade(ResultadoOEE.percentual(resultado.disponibilidade()))
                .performance(ResultadoOEE.percentual(resultado.performance()))
                .qualidade(ResultadoOEE.percentual(resultado.qualidade()))
                .oee(ResultadoOEE.percentual(resultado.oee()))
                .pecasBoas(pedido.getPecasBoas())
                .pecasDefeituosas(pedido.getPecasDefeituosas())
                .criadoEm(agora())
                .build());
        return resultado;
    }

    @Override
    public void interromperPedido(Long maquinaId, Long pedidoId, PedidoDetectado detectado) {
        Pedido pedido = pedidoExistenteOuNovo(buscarMaquina(maquinaId), pedidoId, detectado);
        preencher(pedido, detectado, SituacaoPedido.INTERROMPIDO);
        pedidoRepository.save(pedido);
    }

    @Override
    public void registrarLeitura(Long maquinaId, Instant lidoEm, String json) {
        leituraBrutaRepository.save(LeituraBruta.builder()
                .maquina(maquinaRepository.getReferenceById(maquinaId))
                .lidoEm(data(lidoEm))
                .payload(json)
                .build());
    }

    @Override
    public void atualizarMaquina(Long maquinaId, String statusOperacional, Instant ultimaLeituraEm) {
        Maquina maquina = buscarMaquina(maquinaId);
        maquina.setStatusOperacional(statusOperacional);
        if (ultimaLeituraEm != null) {
            maquina.setUltimaLeituraEm(data(ultimaLeituraEm));
        }
        maquina.setAtualizadoEm(agora());
    }

    @Override
    public int interromperPedidosAbertos() {
        List<Pedido> abertos = pedidoRepository.findBySituacao(SituacaoPedido.EM_ANDAMENTO);
        abertos.forEach(pedido -> pedido.setSituacao(SituacaoPedido.INTERROMPIDO));
        if (!abertos.isEmpty()) {
            logger.warn("{} pedido(s) em andamento de uma execução anterior marcado(s) como INTERROMPIDO",
                    abertos.size());
        }
        return abertos.size();
    }

    // ---- apoio -------------------------------------------------------------

    private Maquina buscarMaquina(Long maquinaId) {
        return maquinaRepository.findById(maquinaId).orElseThrow(() -> new MaquinaNotFoundException(maquinaId));
    }

    private Pedido pedidoExistenteOuNovo(Maquina maquina, Long pedidoId, PedidoDetectado detectado) {
        if (pedidoId != null) {
            var existente = pedidoRepository.findById(pedidoId);
            if (existente.isPresent()) {
                return existente.get();
            }
        }
        return novoPedido(maquina, detectado.origem(), detectado.inicio());
    }

    private Pedido novoPedido(Maquina maquina, OrigemPedido origem, Instant inicio) {
        return Pedido.builder()
                .maquina(maquina)
                .numero(pedidoRepository.ultimoNumero(maquina.getId()) + 1)
                .origem(origem)
                .situacao(SituacaoPedido.EM_ANDAMENTO)
                .inicio(data(inicio))
                .tempoParadoS(BigDecimal.ZERO)
                .reprovado(false)
                .pecasBoas(0)
                .pecasDefeituosas(0)
                .criadoEm(agora())
                .build();
    }

    private void preencher(Pedido pedido, PedidoDetectado detectado, SituacaoPedido situacao) {
        pedido.setSituacao(situacao);
        pedido.setFim(data(detectado.fim()));
        pedido.setLeadTimeS(segundos(detectado.leadTimeS()));
        pedido.setTempoParadoS(segundos(detectado.tempoParadoS()));
        pedido.setReprovado(detectado.reprovado());
        pedido.getEtapas().clear();
        for (EtapaDetectada etapa : detectado.etapas()) {
            pedido.adicionarEtapa(EtapaPedido.builder()
                    .estacao(etapa.estacao())
                    .inicio(data(etapa.inicio()))
                    .fim(data(etapa.fim()))
                    .duracaoS(segundos(etapa.duracaoS()))
                    .tempoParadoS(segundos(etapa.tempoParadoS()))
                    .tempoPadraoS(segundos(etapa.tempoPadraoS()))
                    .build());
        }
    }

    private OffsetDateTime agora() {
        return data(clock.instant());
    }

    private static OffsetDateTime data(Instant instante) {
        return OffsetDateTime.ofInstant(instante, ZoneOffset.UTC);
    }

    private static BigDecimal segundos(double valor) {
        return BigDecimal.valueOf(valor).setScale(3, RoundingMode.HALF_UP);
    }
}
