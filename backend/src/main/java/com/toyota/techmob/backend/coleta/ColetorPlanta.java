package com.toyota.techmob.backend.coleta;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.toyota.techmob.backend.coleta.CalculadoraOEE.ResultadoOEE;
import com.toyota.techmob.backend.coleta.DetectorPedidos.EtapaFechada;
import com.toyota.techmob.backend.coleta.DetectorPedidos.Evento;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoConcluido;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoIniciado;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoInterrompido;
import com.toyota.techmob.backend.coleta.EstadoPlantaAtual.EstacaoAtual;
import com.toyota.techmob.backend.coleta.EstadoPlantaAtual.Estado;
import com.toyota.techmob.backend.coleta.EstadoPlantaAtual.PedidoAtual;
import com.toyota.techmob.backend.config.PlantaProperties;
import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.service.RegistroPedidoService;
import com.toyota.techmob.backend.service.RegistroPedidoService.PedidoRegistrado;

/**
 * Laço da coleta: a cada intervalo lê a bancada, passa a leitura ao
 * detector, guarda o estado ao vivo em memória e grava os pedidos nos
 * eventos. Uma falha (bancada fora do ar, banco fora do ar) é registrada no
 * log e o laço continua no próximo ciclo.
 *
 * Desligado com techmob.coleta.habilitada=false (é assim nos testes).
 */
@Component
@ConditionalOnProperty(name = "techmob.coleta.habilitada", havingValue = "true", matchIfMissing = true)
public class ColetorPlanta {

    /** Sem mudança de status, a última leitura vai para o banco no máximo a cada 10 s. */
    static final Duration ATUALIZAR_MAQUINA_A_CADA = Duration.ofSeconds(10);

    private static final Logger logger = LoggerFactory.getLogger(ColetorPlanta.class);

    private final PlantaClient plantaClient;
    private final RegistroPedidoService registro;
    private final EstadoPlantaAtual estadoAtual;
    private final PlantaProperties propriedades;
    private final Clock clock;
    private final DetectorPedidos detector;
    private final Long maquinaId;

    private LeituraPlanta ultimaGravada;
    private Instant ultimaLeituraOk;
    private OrigemPedido fonte;
    private Long pedidoId;
    private Integer pedidoNumero;
    private StatusMaquina statusGravado;
    private Instant maquinaGravadaEm;

    public ColetorPlanta(PlantaClient plantaClient, RegistroPedidoService registro, EstadoPlantaAtual estadoAtual,
                         PlantaProperties propriedades, Clock clock) {
        this.plantaClient = plantaClient;
        this.registro = registro;
        this.estadoAtual = estadoAtual;
        this.propriedades = propriedades;
        this.clock = clock;
        this.maquinaId = propriedades.maquinaId();
        this.detector = new DetectorPedidos(propriedades.tempoPadraoS().porEstacao(),
                propriedades.statusParado(), propriedades.statusParada(), propriedades.segundosSemConexao());
        estadoAtual.atualizar(Estado.semConexao(maquinaId));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        logger.info("Coleta ligada: lendo {} a cada {} ms (fallback: {})",
                propriedades.url(), propriedades.intervaloMs(), propriedades.urlFallback());
        try {
            registro.interromperPedidosAbertos();
        } catch (RuntimeException e) {
            logger.error("Não foi possível fechar os pedidos abertos da execução anterior", e);
        }
    }

    @Scheduled(fixedDelayString = "${techmob.planta.intervalo-ms:1000}")
    public void coletar() {
        try {
            ciclo();
        } catch (RuntimeException e) {
            logger.error("Falha no ciclo de coleta", e);
        }
    }

    /** Um ciclo: lê, detecta, grava e publica o estado. */
    void ciclo() {
        Instant agora = clock.instant();
        Optional<PlantaClient.Resultado> resultado = plantaClient.ler();
        LeituraPlanta leitura = resultado.map(PlantaClient.Resultado::leitura).orElse(null);

        List<Evento> eventos;
        if (resultado.isPresent()) {
            PlantaClient.Resultado lido = resultado.get();
            eventos = detector.processar(leitura, agora, lido.origem());
            ultimaLeituraOk = agora;
            fonte = lido.origem();
            gravarLeituraSeMudou(leitura, lido.json(), agora);
        } else {
            eventos = detector.falhaDeLeitura(agora);
            ultimaGravada = null; // a primeira leitura depois da falha é gravada
        }

        eventos.forEach(this::tratar);
        StatusMaquina status = leitura != null ? detector.statusDa(leitura) : StatusMaquina.SEM_CONEXAO;
        estadoAtual.atualizar(montarEstado(leitura, status));
        atualizarMaquina(status, agora);
    }

    private void tratar(Evento evento) {
        try {
            switch (evento) {
                case PedidoIniciado iniciado -> {
                    PedidoRegistrado registrado = registro.iniciarPedido(maquinaId, iniciado.origem(), iniciado.inicio());
                    pedidoId = registrado.id();
                    pedidoNumero = registrado.numero();
                    logger.info("Pedido {} iniciado ({})", pedidoNumero, iniciado.origem());
                }
                case EtapaFechada etapa ->
                        logger.debug("Etapa {} fechada: {} s", etapa.estacao(), etapa.duracaoS());
                case PedidoConcluido concluido -> {
                    ResultadoOEE oee = registro.concluirPedido(maquinaId, pedidoId, concluido.pedido());
                    logger.info("Pedido {} concluído em {} s: OEE {}%{}", pedidoNumero,
                            Math.round(concluido.pedido().leadTimeS()), ResultadoOEE.percentual(oee.oee()),
                            concluido.pedido().reprovado() ? " (reprovado)" : "");
                    limparPedido();
                }
                case PedidoInterrompido interrompido -> {
                    registro.interromperPedido(maquinaId, pedidoId, interrompido.pedido());
                    logger.warn("Pedido {} interrompido: bancada sem conexão ou troca de fonte", pedidoNumero);
                    limparPedido();
                }
            }
        } catch (RuntimeException e) {
            logger.error("Não foi possível gravar o evento {}", evento, e);
            if (!(evento instanceof PedidoIniciado) && !(evento instanceof EtapaFechada)) {
                limparPedido();
            }
        }
    }

    private void limparPedido() {
        pedidoId = null;
        pedidoNumero = null;
    }

    private void gravarLeituraSeMudou(LeituraPlanta leitura, String json, Instant agora) {
        if (leitura.equals(ultimaGravada)) {
            return;
        }
        try {
            registro.registrarLeitura(maquinaId, agora, json);
            ultimaGravada = leitura;
        } catch (RuntimeException e) {
            logger.warn("Não foi possível gravar a leitura bruta: {}", e.getMessage());
        }
    }

    private void atualizarMaquina(StatusMaquina status, Instant agora) {
        boolean mudou = status != statusGravado;
        boolean venceu = maquinaGravadaEm == null
                || Duration.between(maquinaGravadaEm, agora).compareTo(ATUALIZAR_MAQUINA_A_CADA) >= 0;
        if (!mudou && !venceu) {
            return;
        }
        try {
            registro.atualizarMaquina(maquinaId, status.name(), ultimaLeituraOk);
            statusGravado = status;
            maquinaGravadaEm = agora;
        } catch (RuntimeException e) {
            logger.warn("Não foi possível atualizar a máquina {}: {}", maquinaId, e.getMessage());
        }
    }

    private Estado montarEstado(LeituraPlanta leitura, StatusMaquina status) {
        List<EstacaoAtual> estacoes = Arrays.stream(Estacao.values())
                .map(estacao -> leitura == null
                        ? new EstacaoAtual(estacao, null, SituacaoEstacao.SEM_CONEXAO)
                        : new EstacaoAtual(estacao, leitura.statusDa(estacao), detector.situacaoDa(leitura.statusDa(estacao))))
                .toList();
        PedidoAtual pedido = detector.pedidoAberto()
                .map(aberto -> new PedidoAtual(pedidoId, pedidoNumero, aberto.inicio(), aberto.etapaAtual()))
                .orElse(null);
        return new Estado(maquinaId, leitura != null, fonte, status,
                leitura != null ? leitura.operacaoString() : null, estacoes, pedido, ultimaLeituraOk);
    }
}
