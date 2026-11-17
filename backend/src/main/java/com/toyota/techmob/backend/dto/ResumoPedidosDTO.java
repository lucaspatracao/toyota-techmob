package com.toyota.techmob.backend.dto;

/**
 * Resumo dos pedidos de um período (no Dashboard: o dia de hoje).
 *
 * Contagens e médias consideram só os pedidos concluídos; interrompidos
 * aparecem à parte e ficam fora do OEE. O OEE acumulado soma os tempos de
 * todos os pedidos e aplica a fórmula uma vez (não é a média dos OEEs).
 * taxaReprovacao, médias e OEE (0-100) vêm null sem pedido concluído.
 */
public record ResumoPedidosDTO(
        int concluidos,
        int aprovados,
        int reprovados,
        int interrompidos,
        int pecasProduzidas,
        Double taxaReprovacao,
        Double leadTimeMedioS,
        Double tempoParadoTotalS,
        Double disponibilidade,
        Double performance,
        Double qualidade,
        Double oee) {
}
