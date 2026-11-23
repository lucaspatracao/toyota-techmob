package com.toyota.techmob.backend.coleta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.toyota.techmob.backend.coleta.DetectorPedidos.EtapaDetectada;
import com.toyota.techmob.backend.coleta.DetectorPedidos.Evento;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoConcluido;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoDetectado;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoIniciado;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoInterrompido;
import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.OrigemPedido;

/**
 * O detector com as gravações de src/test/resources/gravacoes/. Os números
 * esperados são os mesmos de data/tests/test_oee.py.
 */
class DetectorPedidosTest {

    private static List<PedidoDetectado> fechados(List<Evento> eventos) {
        return eventos.stream()
                .<PedidoDetectado>mapMulti((evento, saida) -> {
                    if (evento instanceof PedidoConcluido c) {
                        saida.accept(c.pedido());
                    } else if (evento instanceof PedidoInterrompido i) {
                        saida.accept(i.pedido());
                    }
                })
                .toList();
    }

    private static double oeePct(PedidoDetectado pedido) {
        return CalculadoraOEE.ResultadoOEE.percentual(CalculadoraOEE.doPedido(pedido.tempos()).oee()).doubleValue();
    }

    @Test
    void gravacaoDoExemplo_detectaOPedidoDe80PorCento() {
        List<Evento> eventos = Gravacao.reproduzir("exemplo_80.jsonl", Gravacao.detector());

        List<PedidoDetectado> pedidos = fechados(eventos);
        assertThat(pedidos).hasSize(1);
        PedidoDetectado pedido = pedidos.getFirst();
        assertThat(eventos.getLast()).isInstanceOf(PedidoConcluido.class);
        assertThat(pedido.leadTimeS()).isEqualTo(100.0);
        assertThat(pedido.tempoParadoS()).isEqualTo(12.0);
        assertThat(pedido.tempoPadraoS()).isEqualTo(80.0);
        assertThat(oeePct(pedido)).isEqualTo(80.0);
        assertThat(pedido.etapas())
                .extracting(EtapaDetectada::estacao, EtapaDetectada::duracaoS, EtapaDetectada::tempoParadoS)
                .containsExactly(
                        tuple(Estacao.ESTOQUE, 21.0, 0.0),
                        tuple(Estacao.MANIPULACAO, 47.0, 12.0),
                        tuple(Estacao.SEPARACAO, 26.0, 0.0));
    }

    @Test
    void gravacaoSintetica_detectaOsMesmosPedidosDoPython() {
        List<Evento> eventos = Gravacao.reproduzir("sintetica_seed12.jsonl", Gravacao.detector());

        assertThat(eventos).filteredOn(PedidoIniciado.class::isInstance).hasSize(5);
        List<PedidoDetectado> pedidos = fechados(eventos);
        assertThat(pedidos)
                .extracting(p -> Gravacao.segundos(p.inicio()), p -> Gravacao.segundos(p.fim()),
                        PedidoDetectado::tempoParadoS, PedidoDetectado::reprovado)
                .containsExactly(
                        tuple(5.0, 107.0, 21.0, false),
                        tuple(137.0, 213.0, 0.0, true),
                        tuple(245.0, 362.0, 37.0, false),
                        tuple(389.0, 418.0, 0.0, false),
                        tuple(495.0, 610.0, 34.0, false));
        assertThat(eventos).filteredOn(PedidoInterrompido.class::isInstance).hasSize(1);
        assertThat(List.of(oeePct(pedidos.get(0)), oeePct(pedidos.get(1)), oeePct(pedidos.get(2)), oeePct(pedidos.get(4))))
                .containsExactly(78.431, 0.0, 68.376, 69.565);
    }

    @Test
    void quedaDeConexao_interrompeOPedidoNaUltimaLeituraBoa() {
        List<Evento> eventos = Gravacao.reproduzir("sintetica_seed12.jsonl", Gravacao.detector());

        PedidoDetectado interrompido = eventos.stream()
                .filter(PedidoInterrompido.class::isInstance)
                .map(e -> ((PedidoInterrompido) e).pedido())
                .findFirst().orElseThrow();
        // Queda em t = 419 s; a última leitura boa foi 1 s antes.
        assertThat(interrompido.leadTimeS()).isEqualTo(29.0);
        assertThat(interrompido.etapas())
                .extracting(EtapaDetectada::estacao, EtapaDetectada::duracaoS)
                .containsExactly(tuple(Estacao.ESTOQUE, 22.0), tuple(Estacao.MANIPULACAO, 7.0));
    }

    // ---- casos pontuais ----------------------------------------------------

    private static final Instant T0 = Gravacao.BASE;

    private static LeituraPlanta leitura(int aguardando, int reprovados, String estoque) {
        return new LeituraPlanta(estoque, "AGUARDANDO", "AGUARDANDO", estoque, aguardando, 1 - aguardando,
                0, reprovados, 0, 0.0, 0.0);
    }

    @Test
    void contadorQueDiminui_eReinicioENaoReprovacao() {
        DetectorPedidos detector = Gravacao.detector();
        detector.processar(leitura(1, 5, "AGUARDANDO"), T0, OrigemPedido.BANCADA);
        detector.processar(leitura(0, 5, "PRODUZINDO"), T0.plusSeconds(1), OrigemPedido.BANCADA);
        detector.processar(leitura(0, 0, "PRODUZINDO"), T0.plusSeconds(30), OrigemPedido.BANCADA);
        List<Evento> fim = detector.processar(leitura(1, 0, "AGUARDANDO"), T0.plusSeconds(60), OrigemPedido.BANCADA);

        assertThat(fechados(fim)).singleElement().extracting(PedidoDetectado::reprovado).isEqualTo(false);
    }

    @Test
    void quedaCurta_naoInterrompeOPedido() {
        DetectorPedidos detector = Gravacao.detector();
        detector.processar(leitura(1, 0, "AGUARDANDO"), T0, OrigemPedido.BANCADA);
        detector.processar(leitura(0, 0, "PRODUZINDO"), T0.plusSeconds(1), OrigemPedido.BANCADA);
        for (int s = 2; s <= 9; s++) {
            assertThat(detector.falhaDeLeitura(T0.plusSeconds(s))).isEmpty();
        }
        List<Evento> fim = detector.processar(leitura(1, 0, "AGUARDANDO"), T0.plusSeconds(81), OrigemPedido.BANCADA);

        assertThat(fim).last().isInstanceOf(PedidoConcluido.class);
        assertThat(fechados(fim)).singleElement().extracting(PedidoDetectado::leadTimeS).isEqualTo(80.0);
    }

    @Test
    void trocaDeFonte_interrompeOPedidoAberto() {
        DetectorPedidos detector = Gravacao.detector();
        detector.processar(leitura(1, 0, "AGUARDANDO"), T0, OrigemPedido.BANCADA);
        detector.processar(leitura(0, 0, "PRODUZINDO"), T0.plusSeconds(1), OrigemPedido.BANCADA);
        detector.processar(leitura(0, 0, "PRODUZINDO"), T0.plusSeconds(2), OrigemPedido.BANCADA);

        List<Evento> eventos = detector.processar(leitura(1, 0, "AGUARDANDO"), T0.plusSeconds(6), OrigemPedido.REPLAY);

        assertThat(eventos).singleElement().isInstanceOf(PedidoInterrompido.class);
        assertThat(fechados(eventos).getFirst().fim()).isEqualTo(T0.plusSeconds(2));
        assertThat(detector.pedidoAberto()).isEmpty();
    }

    @Test
    void pedidoSoComecaComATransicaoDeAguardando() {
        DetectorPedidos detector = Gravacao.detector();
        // O backend subiu com a bancada já produzindo: não sabemos o início.
        assertThat(detector.processar(leitura(0, 0, "PRODUZINDO"), T0, OrigemPedido.BANCADA)).isEmpty();
        assertThat(detector.processar(leitura(1, 0, "AGUARDANDO"), T0.plusSeconds(50), OrigemPedido.BANCADA)).isEmpty();
    }

    @Test
    void pedidoAberto_trazAEtapaAtual() {
        DetectorPedidos detector = Gravacao.detector();
        detector.processar(leitura(1, 0, "AGUARDANDO"), T0, OrigemPedido.REPLAY);
        detector.processar(leitura(0, 0, "PRODUZINDO"), T0.plusSeconds(1), OrigemPedido.REPLAY);

        assertThat(detector.pedidoAberto()).hasValueSatisfying(p -> {
            assertThat(p.inicio()).isEqualTo(T0.plusSeconds(1));
            assertThat(p.origem()).isEqualTo(OrigemPedido.REPLAY);
            assertThat(p.etapaAtual()).isEqualTo(Estacao.ESTOQUE);
        });
    }

    @Test
    void classificaStatusDaBancadaEDasEstacoes() {
        DetectorPedidos detector = Gravacao.detector();
        LeituraPlanta parada = new LeituraPlanta("AGUARDANDO", "PARADO", "AGUARDANDO", "PARADO", 0, 0, 0, 0, 0, 0.0, 0.0);

        assertThat(detector.statusDa(leitura(1, 0, "AGUARDANDO"))).isEqualTo(StatusMaquina.AGUARDANDO);
        assertThat(detector.statusDa(leitura(0, 0, "PRODUZINDO"))).isEqualTo(StatusMaquina.PRODUZINDO);
        assertThat(detector.statusDa(parada)).isEqualTo(StatusMaquina.PARADO);
        assertThat(detector.situacaoDa("AGUARDANDO")).isEqualTo(SituacaoEstacao.AGUARDANDO);
        assertThat(detector.situacaoDa("PARADO")).isEqualTo(SituacaoEstacao.PARADO);
        assertThat(detector.situacaoDa("SEPARANDO COR")).isEqualTo(SituacaoEstacao.OPERANDO);
    }
}
