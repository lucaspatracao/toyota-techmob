package com.toyota.techmob.backend.dto;

import java.time.Instant;

/**
 * Resposta da API para um indicador de OEE, calculado pelo módulo Python
 * e gravado na tabela indicador_oee (MySQL).
 */
public record IndicadorOEEDTO(
        Long maquinaId,
        Double disponibilidade,
        Double performance,
        Double qualidade,
        Double oee,
        Instant calculadoEm) {
}
