package com.toyota.techmob.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Resposta da API para um registro do histórico de produção
 * (tabela historico_producao), ciclo a ciclo.
 */
public record HistoricoProducaoDTO(
        Long maquinaId,
        Instant timestamp,
        String statusOperacional,
        Integer pecasBoas,
        Integer pecasDefeituosas,
        BigDecimal tempoCicloSegundos) {
}
