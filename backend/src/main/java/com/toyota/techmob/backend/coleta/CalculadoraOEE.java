package com.toyota.techmob.backend.coleta;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Optional;

/**
 * Cálculo do OEE por pedido. Classe pura (sem Spring): a mesma conta do
 * data/oee_calculator.py, que serve de conferência.
 *
 * L = lead time, T_parado = soma das paradas, T_padrão = soma do tempo
 * padrão das etapas:
 *   D = (L - T_parado) / L
 *   P = mín(1; T_padrão / (L - T_parado))
 *   Q = 1 se aprovado, 0 se reprovado
 *   OEE = D x P x Q
 * Para vários pedidos, soma os tempos e aplica a mesma fórmula; Q vira
 * aprovados / concluídos. Não é a média dos OEEs, que distorce quando os
 * pedidos têm durações diferentes.
 */
public final class CalculadoraOEE {

    private CalculadoraOEE() {
    }

    /** Tempos de um pedido concluído, em segundos. */
    public record TemposPedido(double leadTimeS, double tempoParadoS, double tempoPadraoS, boolean reprovado) {
    }

    /** Componentes do OEE em fração (0 a 1). */
    public record ResultadoOEE(double disponibilidade, double performance, double qualidade) {

        public double oee() {
            return disponibilidade * performance * qualidade;
        }

        /** Fração para percentual com 3 casas, como fica no banco (DECIMAL(6,3)). */
        public static BigDecimal percentual(double fracao) {
            return BigDecimal.valueOf(fracao * 100).setScale(3, RoundingMode.HALF_UP);
        }
    }

    /** OEE a partir dos tempos. qualidade: 1 aprovado, 0 reprovado (ou a fração de aprovados). */
    public static ResultadoOEE calcular(double leadTimeS, double tempoParadoS, double tempoPadraoS, double qualidade) {
        double operando = leadTimeS - tempoParadoS;
        double disponibilidade = leadTimeS > 0 ? operando / leadTimeS : 0;
        double performance = operando > 0 ? Math.min(1, tempoPadraoS / operando) : 0;
        return new ResultadoOEE(disponibilidade, performance, qualidade);
    }

    public static ResultadoOEE doPedido(TemposPedido pedido) {
        return calcular(pedido.leadTimeS(), pedido.tempoParadoS(), pedido.tempoPadraoS(), pedido.reprovado() ? 0 : 1);
    }

    /** OEE acumulado de vários pedidos concluídos; vazio se não houver nenhum. */
    public static Optional<ResultadoOEE> acumulado(Collection<TemposPedido> pedidos) {
        if (pedidos.isEmpty()) {
            return Optional.empty();
        }
        double leadTime = 0;
        double parado = 0;
        double padrao = 0;
        long aprovados = 0;
        for (TemposPedido pedido : pedidos) {
            leadTime += pedido.leadTimeS();
            parado += pedido.tempoParadoS();
            padrao += pedido.tempoPadraoS();
            aprovados += pedido.reprovado() ? 0 : 1;
        }
        return Optional.of(calcular(leadTime, parado, padrao, (double) aprovados / pedidos.size()));
    }
}
