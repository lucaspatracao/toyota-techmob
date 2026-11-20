package com.toyota.techmob.backend.dto;

import java.time.Instant;

/**
 * OEE de um pedido concluído (tabela indicador_oee). Percentuais de 0 a 100.
 * periodoInicio e periodoFim são o início e o fim do pedido.
 */
public record IndicadorOEEDTO(
        Long maquinaId,
        Long pedidoId,
        Double disponibilidade,
        Double performance,
        Double qualidade,
        Double oee,
        Instant periodoInicio,
        Instant periodoFim,
        Instant calculadoEm) {
}
