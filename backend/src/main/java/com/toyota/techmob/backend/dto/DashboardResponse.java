package com.toyota.techmob.backend.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record DashboardResponse(
        Long maquinaId,
        String identificadorBancada,
        String nome,
        String statusOperacional,
        BigDecimal ultimoOee,
        BigDecimal ultimaDisponibilidade,
        BigDecimal ultimaPerformance,
        BigDecimal ultimaQualidade,
        OffsetDateTime periodoInicio,
        OffsetDateTime periodoFim
) {
}