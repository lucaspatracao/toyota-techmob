package com.toyota.techmob.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toyota.techmob.backend.coleta.CalculadoraOEE;
import com.toyota.techmob.backend.coleta.CalculadoraOEE.ResultadoOEE;
import com.toyota.techmob.backend.coleta.CalculadoraOEE.TemposPedido;
import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.domain.Pedido;
import com.toyota.techmob.backend.domain.SituacaoPedido;
import com.toyota.techmob.backend.dto.PedidoDTO;
import com.toyota.techmob.backend.dto.PedidoDetalheDTO;
import com.toyota.techmob.backend.dto.ResumoPedidosDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.exception.PedidoNotFoundException;
import com.toyota.techmob.backend.mapper.DtoMapper;
import com.toyota.techmob.backend.repository.MaquinaRepository;
import com.toyota.techmob.backend.repository.PedidoRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PedidoServiceImpl implements PedidoService {

    private final MaquinaRepository maquinaRepository;
    private final PedidoRepository pedidoRepository;

    @Override
    public List<PedidoDTO> listar(Long maquinaId, Instant de, Instant ate, OrigemPedido origem) {
        return buscar(maquinaId, de, ate, origem).stream()
                .map(DtoMapper::paraDTO)
                .toList();
    }

    @Override
    public PedidoDetalheDTO detalhe(Long pedidoId) {
        return pedidoRepository.findDetalheById(pedidoId)
                .map(DtoMapper::paraDetalheDTO)
                .orElseThrow(() -> new PedidoNotFoundException(pedidoId));
    }

    @Override
    public ResumoPedidosDTO resumir(Long maquinaId, Instant de, Instant ate, OrigemPedido origem) {
        return resumo(buscar(maquinaId, de, ate, origem));
    }

    private List<Pedido> buscar(Long maquinaId, Instant de, Instant ate, OrigemPedido origem) {
        if (!maquinaRepository.existsById(maquinaId)) {
            throw new MaquinaNotFoundException(maquinaId);
        }
        return pedidoRepository.buscarPorPeriodo(maquinaId, data(de), data(ate), origem);
    }

    /**
     * Contagens e médias só com os pedidos concluídos; os interrompidos são
     * contados à parte e os em andamento ficam de fora. Sem pedido
     * concluído, a taxa, a média e o OEE vêm nulos.
     */
    static ResumoPedidosDTO resumo(List<Pedido> pedidos) {
        List<Pedido> concluidos = pedidos.stream()
                .filter(p -> p.getSituacao() == SituacaoPedido.CONCLUIDO)
                .toList();
        int interrompidos = (int) pedidos.stream()
                .filter(p -> p.getSituacao() == SituacaoPedido.INTERROMPIDO)
                .count();
        int reprovados = (int) concluidos.stream()
                .filter(p -> Boolean.TRUE.equals(p.getReprovado()))
                .count();
        int pecas = concluidos.stream()
                .mapToInt(p -> p.getPecasBoas() + p.getPecasDefeituosas())
                .sum();
        double leadTimeTotal = concluidos.stream().mapToDouble(p -> p.getLeadTimeS().doubleValue()).sum();
        double paradoTotal = concluidos.stream().mapToDouble(p -> p.getTempoParadoS().doubleValue()).sum();

        Optional<ResultadoOEE> oee = CalculadoraOEE.acumulado(
                concluidos.stream().map(PedidoServiceImpl::tempos).toList());
        int total = concluidos.size();

        return new ResumoPedidosDTO(
                total,
                total - reprovados,
                reprovados,
                interrompidos,
                pecas,
                total == 0 ? null : arredondar(100.0 * reprovados / total),
                total == 0 ? null : arredondar(leadTimeTotal / total),
                arredondar(paradoTotal),
                oee.map(r -> percentual(r.disponibilidade())).orElse(null),
                oee.map(r -> percentual(r.performance())).orElse(null),
                oee.map(r -> percentual(r.qualidade())).orElse(null),
                oee.map(r -> percentual(r.oee())).orElse(null));
    }

    private static TemposPedido tempos(Pedido pedido) {
        return new TemposPedido(
                pedido.getLeadTimeS().doubleValue(),
                pedido.getTempoParadoS().doubleValue(),
                pedido.somaTempoPadraoS().doubleValue(),
                Boolean.TRUE.equals(pedido.getReprovado()));
    }

    /** Mesma precisão do banco: 3 casas. */
    private static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(3, RoundingMode.HALF_UP).doubleValue();
    }

    private static double percentual(double fracao) {
        return ResultadoOEE.percentual(fracao).doubleValue();
    }

    private static OffsetDateTime data(Instant instante) {
        return instante != null ? OffsetDateTime.ofInstant(instante, ZoneOffset.UTC) : null;
    }
}
