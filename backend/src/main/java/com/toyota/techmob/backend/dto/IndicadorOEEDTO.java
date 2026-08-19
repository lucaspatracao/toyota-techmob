package com.toyota.techmob.backend.dto;

import java.time.Instant;

/**
 * DTO de resposta para os indicadores de OEE calculados pelo módulo Python
 * e persistidos no PostgreSQL (Supabase), tabela techmob.indicador_oee.
 */
public class IndicadorOEEDTO {

    private Long maquinaId;
    private Double disponibilidade;
    private Double performance;
    private Double qualidade;
    private Double oee;
    private Instant calculadoEm;

    public IndicadorOEEDTO() {
    }

    public IndicadorOEEDTO(Long maquinaId, Double disponibilidade, Double performance,
                            Double qualidade, Double oee, Instant calculadoEm) {
        this.maquinaId = maquinaId;
        this.disponibilidade = disponibilidade;
        this.performance = performance;
        this.qualidade = qualidade;
        this.oee = oee;
        this.calculadoEm = calculadoEm;
    }

    public Long getMaquinaId() {
        return maquinaId;
    }

    public void setMaquinaId(Long maquinaId) {
        this.maquinaId = maquinaId;
    }

    public Double getDisponibilidade() {
        return disponibilidade;
    }

    public void setDisponibilidade(Double disponibilidade) {
        this.disponibilidade = disponibilidade;
    }

    public Double getPerformance() {
        return performance;
    }

    public void setPerformance(Double performance) {
        this.performance = performance;
    }

    public Double getQualidade() {
        return qualidade;
    }

    public void setQualidade(Double qualidade) {
        this.qualidade = qualidade;
    }

    public Double getOee() {
        return oee;
    }

    public void setOee(Double oee) {
        this.oee = oee;
    }

    public Instant getCalculadoEm() {
        return calculadoEm;
    }

    public void setCalculadoEm(Instant calculadoEm) {
        this.calculadoEm = calculadoEm;
    }
}