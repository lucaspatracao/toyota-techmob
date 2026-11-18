package com.toyota.techmob.backend.coleta;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.OrigemPedido;

/**
 * Máquina de estados que descobre os pedidos nas leituras do /dadosPlanta.
 * Classe pura (sem Spring): as mesmas regras do data/oee_calculator.py.
 *
 * 1. Início: "aguardando" passa de 1 para 0 sem pedido aberto.
 * 2. Etapa: abre quando a estação sai do status ocioso (AGUARDANDO) e fecha
 *    quando volta. Se a estação voltar a operar no mesmo pedido, o tempo
 *    soma na mesma etapa. Tempo da estação em status de parada conta como
 *    parada da etapa.
 * 3. Fim: "aguardando" volta para 1.
 * 4. Reprovado: "pedidos_reprovados" aumentou durante o pedido. Se diminuir,
 *    é reinício da bancada, não reprovação.
 * 5. Interrompido: sem leitura boa por mais que o limite com pedido aberto,
 *    ou troca da fonte (bancada / simulador) no meio do pedido.
 *
 * O pedido está parado quando alguma estação está em parada ou quando
 * "aguardando" e "produzindo" estão os dois em 0. Cada intervalo entre duas
 * leituras usa o estado da leitura anterior.
 *
 * Não é thread-safe: é usado só pelo ColetorPlanta, numa thread.
 */
public class DetectorPedidos {

    /** O que mudou numa leitura. */
    public sealed interface Evento permits PedidoIniciado, EtapaFechada, PedidoConcluido, PedidoInterrompido {
    }

    public record PedidoIniciado(Instant inicio, OrigemPedido origem) implements Evento {
    }

    /** Estação voltou para o status ocioso; duracaoS é o tempo acumulado dela no pedido. */
    public record EtapaFechada(Estacao estacao, Instant fim, double duracaoS) implements Evento {
    }

    public record PedidoConcluido(PedidoDetectado pedido) implements Evento {
    }

    public record PedidoInterrompido(PedidoDetectado pedido) implements Evento {
    }

    public record EtapaDetectada(Estacao estacao, Instant inicio, Instant fim,
                                 double duracaoS, double tempoParadoS, double tempoPadraoS) {
    }

    /** Pedido fechado (concluído ou interrompido), com as etapas em ordem de início. */
    public record PedidoDetectado(OrigemPedido origem, Instant inicio, Instant fim, double leadTimeS,
                                  double tempoParadoS, boolean reprovado, List<EtapaDetectada> etapas) {

        public double tempoPadraoS() {
            return etapas.stream().mapToDouble(EtapaDetectada::tempoPadraoS).sum();
        }

        public CalculadoraOEE.TemposPedido tempos() {
            return new CalculadoraOEE.TemposPedido(leadTimeS, tempoParadoS, tempoPadraoS(), reprovado);
        }
    }

    /** Pedido aberto agora, para o estado ao vivo. etapaAtual: última estação que entrou em operação. */
    public record PedidoAberto(Instant inicio, OrigemPedido origem, Estacao etapaAtual) {
    }

    private final Map<Estacao, Double> tempoPadraoS;
    private final String statusParado;
    private final String statusParada;
    private final double segundosSemConexao;

    private LeituraPlanta anterior;
    private Instant instanteAnterior;
    private Instant ultimaLeituraOk;
    private OrigemPedido origemAnterior;
    private Aberto pedido;

    /**
     * @param tempoPadraoS       cronometragem de cada estação
     * @param statusParado       texto da estação ociosa (AGUARDANDO)
     * @param statusParada       texto da estação em parada (PARADO)
     * @param segundosSemConexao limite sem leitura boa antes de interromper o pedido
     */
    public DetectorPedidos(Map<Estacao, Double> tempoPadraoS, String statusParado, String statusParada,
                           double segundosSemConexao) {
        this.tempoPadraoS = new EnumMap<>(tempoPadraoS);
        this.statusParado = statusParado;
        this.statusParada = statusParada;
        this.segundosSemConexao = segundosSemConexao;
    }

    /** Processa uma leitura boa e devolve os eventos que ela gerou. */
    public List<Evento> processar(LeituraPlanta leitura, Instant agora, OrigemPedido origem) {
        List<Evento> eventos = new ArrayList<>();

        if (origemAnterior != null && origem != origemAnterior) {
            // A fonte mudou (ex.: bancada caiu e entrou o simulador): não dá
            // para emendar um pedido de uma fonte na outra.
            if (pedido != null) {
                eventos.add(new PedidoInterrompido(fechar(ultimaLeituraOk)));
            }
            anterior = null;
        }

        if (pedido != null && anterior != null) {
            acumular(anterior, segundos(instanteAnterior, agora));
        }

        if (pedido == null) {
            if (anterior != null && anterior.aguardandoPedido() && Integer.valueOf(0).equals(leitura.aguardando())) {
                iniciar(leitura, agora, origem);
                eventos.add(new PedidoIniciado(agora, origem));
            }
        } else {
            atualizarReprovado(leitura);
            eventos.addAll(transicoes(leitura, agora));
            if (leitura.aguardandoPedido()) {
                eventos.add(new PedidoConcluido(fechar(agora)));
            }
        }

        anterior = leitura;
        instanteAnterior = agora;
        ultimaLeituraOk = agora;
        origemAnterior = origem;
        return eventos;
    }

    /** Leitura falhou (sem conexão, erro HTTP ou JSON inválido). */
    public List<Evento> falhaDeLeitura(Instant agora) {
        if (ultimaLeituraOk == null || segundos(ultimaLeituraOk, agora) <= segundosSemConexao) {
            return List.of();
        }
        List<Evento> eventos = new ArrayList<>();
        if (pedido != null) {
            // O pedido termina na última leitura boa.
            eventos.add(new PedidoInterrompido(fechar(ultimaLeituraOk)));
        }
        anterior = null; // depois da queda, só abre pedido com nova transição
        return eventos;
    }

    public Optional<PedidoAberto> pedidoAberto() {
        if (pedido == null) {
            return Optional.empty();
        }
        Estacao atual = pedido.etapas.values().stream()
                .filter(EtapaAberta::ativa)
                .max(Comparator.comparing((EtapaAberta e) -> e.ativaDesde))
                .map(e -> e.estacao)
                .orElse(null);
        return Optional.of(new PedidoAberto(pedido.inicio, pedido.origem, atual));
    }

    /** Status da bancada inteira para a leitura. */
    public StatusMaquina statusDa(LeituraPlanta leitura) {
        if (leitura.aguardandoPedido()) {
            return StatusMaquina.AGUARDANDO;
        }
        return emParada(leitura) ? StatusMaquina.PARADO : StatusMaquina.PRODUZINDO;
    }

    /** Leitura do texto de status de uma estação. */
    public SituacaoEstacao situacaoDa(String status) {
        if (estaOciosa(status)) {
            return SituacaoEstacao.AGUARDANDO;
        }
        return status.equals(statusParada) ? SituacaoEstacao.PARADO : SituacaoEstacao.OPERANDO;
    }

    // ---- regras ------------------------------------------------------------

    private void iniciar(LeituraPlanta leitura, Instant agora, OrigemPedido origem) {
        pedido = new Aberto(agora, origem, leitura.pedidosReprovados());
        transicoes(leitura, agora);
    }

    private void acumular(LeituraPlanta estado, double dt) {
        if (emParada(estado)) {
            pedido.tempoParadoS += dt;
        }
        for (EtapaAberta etapa : pedido.etapas.values()) {
            if (etapa.ativa()) {
                etapa.duracaoS += dt;
                if (statusParada.equals(estado.statusDa(etapa.estacao))) {
                    etapa.tempoParadoS += dt;
                }
            }
        }
    }

    private boolean emParada(LeituraPlanta leitura) {
        for (Estacao estacao : Estacao.values()) {
            if (statusParada.equals(leitura.statusDa(estacao))) {
                return true;
            }
        }
        return Integer.valueOf(0).equals(leitura.aguardando()) && Integer.valueOf(0).equals(leitura.produzindo());
    }

    private void atualizarReprovado(LeituraPlanta leitura) {
        Integer atual = leitura.pedidosReprovados();
        if (atual == null) {
            return;
        }
        if (pedido.reprovadosReferencia != null && atual > pedido.reprovadosReferencia) {
            pedido.reprovado = true;
        }
        pedido.reprovadosReferencia = atual; // se diminuiu, é reinício: só acompanha
    }

    private List<Evento> transicoes(LeituraPlanta leitura, Instant agora) {
        List<Evento> eventos = new ArrayList<>();
        for (Estacao estacao : Estacao.values()) {
            boolean ociosa = estaOciosa(leitura.statusDa(estacao));
            EtapaAberta etapa = pedido.etapas.get(estacao);
            if (!ociosa && (etapa == null || !etapa.ativa())) {
                if (etapa == null) {
                    etapa = new EtapaAberta(estacao, agora, tempoPadraoS.getOrDefault(estacao, 0.0));
                    pedido.etapas.put(estacao, etapa);
                }
                etapa.ativaDesde = agora;
            } else if (ociosa && etapa != null && etapa.ativa()) {
                etapa.ativaDesde = null;
                etapa.fim = agora;
                eventos.add(new EtapaFechada(estacao, agora, etapa.duracaoS));
            }
        }
        return eventos;
    }

    private PedidoDetectado fechar(Instant fim) {
        List<EtapaDetectada> etapas = new ArrayList<>();
        for (EtapaAberta etapa : pedido.etapas.values()) {
            if (etapa.ativa()) {
                etapa.ativaDesde = null;
                etapa.fim = fim;
            }
            etapas.add(new EtapaDetectada(etapa.estacao, etapa.inicio, etapa.fim,
                    etapa.duracaoS, etapa.tempoParadoS, etapa.tempoPadraoS));
        }
        etapas.sort(Comparator.comparing(EtapaDetectada::inicio));
        PedidoDetectado detectado = new PedidoDetectado(pedido.origem, pedido.inicio, fim,
                segundos(pedido.inicio, fim), pedido.tempoParadoS, pedido.reprovado, List.copyOf(etapas));
        pedido = null;
        return detectado;
    }

    private boolean estaOciosa(String status) {
        return status == null || status.equals(statusParado);
    }

    private static double segundos(Instant de, Instant ate) {
        return Duration.between(de, ate).toNanos() / 1e9;
    }

    // ---- estado interno ----------------------------------------------------

    private static final class Aberto {
        private final Instant inicio;
        private final OrigemPedido origem;
        private final Map<Estacao, EtapaAberta> etapas = new EnumMap<>(Estacao.class);
        private Integer reprovadosReferencia;
        private double tempoParadoS;
        private boolean reprovado;

        private Aberto(Instant inicio, OrigemPedido origem, Integer reprovadosReferencia) {
            this.inicio = Objects.requireNonNull(inicio);
            this.origem = origem;
            this.reprovadosReferencia = reprovadosReferencia;
        }
    }

    private static final class EtapaAberta {
        private final Estacao estacao;
        private final Instant inicio;
        private final double tempoPadraoS;
        private Instant fim;
        private Instant ativaDesde;
        private double duracaoS;
        private double tempoParadoS;

        private EtapaAberta(Estacao estacao, Instant inicio, double tempoPadraoS) {
            this.estacao = estacao;
            this.inicio = inicio;
            this.tempoPadraoS = tempoPadraoS;
        }

        private boolean ativa() {
            return ativaDesde != null;
        }
    }
}
