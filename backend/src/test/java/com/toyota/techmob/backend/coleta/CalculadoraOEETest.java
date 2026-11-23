package com.toyota.techmob.backend.coleta;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.toyota.techmob.backend.coleta.CalculadoraOEE.ResultadoOEE;
import com.toyota.techmob.backend.coleta.CalculadoraOEE.TemposPedido;

/** Mesmos casos de data/tests/test_oee.py: Python e Java têm que dar o mesmo número. */
class CalculadoraOEETest {

    private static BigDecimal pct(double fracao) {
        return ResultadoOEE.percentual(fracao);
    }

    @Test
    void exemploDoPlano_daOitentaPorCento() {
        // Padrão de 80 s, pedido de 100 s com 12 s parado.
        ResultadoOEE r = CalculadoraOEE.doPedido(new TemposPedido(100, 12, 80, false));

        assertThat(pct(r.disponibilidade())).isEqualByComparingTo("88.000");
        assertThat(pct(r.performance())).isEqualByComparingTo("90.909");
        assertThat(pct(r.qualidade())).isEqualByComparingTo("100.000");
        assertThat(pct(r.oee())).isEqualByComparingTo("80.000");
    }

    @Test
    void pedidoReprovado_zeraOOee() {
        ResultadoOEE r = CalculadoraOEE.doPedido(new TemposPedido(100, 12, 80, true));

        assertThat(r.qualidade()).isZero();
        assertThat(r.oee()).isZero();
    }

    @Test
    void performance_limitadaA100PorCento() {
        // Mais rápido que o padrão: 70 s operando para 80 s de padrão.
        ResultadoOEE r = CalculadoraOEE.doPedido(new TemposPedido(70, 0, 80, false));

        assertThat(r.performance()).isEqualTo(1.0);
        assertThat(r.oee()).isEqualTo(1.0);
    }

    @Test
    void pedidoTodoParado_naoDividePorZero() {
        ResultadoOEE r = CalculadoraOEE.doPedido(new TemposPedido(30, 30, 80, false));

        assertThat(r.disponibilidade()).isZero();
        assertThat(r.performance()).isZero();
    }

    @Test
    void acumulado_somaOsTemposEmVezDeTirarMedia() {
        TemposPedido curto = new TemposPedido(60, 0, 80, false);    // OEE 100%
        TemposPedido longo = new TemposPedido(200, 100, 80, false); // OEE 40%

        ResultadoOEE total = CalculadoraOEE.acumulado(List.of(curto, longo)).orElseThrow();

        // A média dos OEEs daria 70%. Somando: D = 160/260, P = mín(1; 160/160), OEE = 61,5%.
        assertThat(pct(total.oee())).isEqualByComparingTo("61.538");
    }

    @Test
    void acumulado_daGravacaoSintetica_igualAoPython() {
        // Os 4 pedidos concluídos de sintetica_seed12.jsonl (um reprovado).
        List<TemposPedido> pedidos = List.of(
                new TemposPedido(102, 21, 80, false),
                new TemposPedido(76, 0, 80, true),
                new TemposPedido(117, 37, 80, false),
                new TemposPedido(115, 34, 80, false));

        ResultadoOEE total = CalculadoraOEE.acumulado(pedidos).orElseThrow();

        assertThat(pct(total.disponibilidade())).isEqualByComparingTo("77.561");
        assertThat(pct(total.performance())).isEqualByComparingTo("100.000");
        assertThat(pct(total.qualidade())).isEqualByComparingTo("75.000");
        assertThat(pct(total.oee())).isEqualByComparingTo("58.171");
    }

    @Test
    void acumulado_semPedido_vazio() {
        assertThat(CalculadoraOEE.acumulado(List.of())).isEmpty();
    }
}
