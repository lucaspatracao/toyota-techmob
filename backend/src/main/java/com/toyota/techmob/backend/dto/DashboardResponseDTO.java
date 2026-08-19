package com.toyota.techmob.backend.dto;

/**
 * DTO de resposta para GET /api/dashboard/{maquinaId}.
 *
 * Combina os dados da máquina com o indicador OEE mais recente calculado
 * para ela, evitando que o front-end precise fazer duas chamadas e juntar
 * os dados manualmente.
 */
public class DashboardResponseDTO {

    private MaquinaDTO maquina;
    private IndicadorOEEDTO indicadorAtual;

    public DashboardResponseDTO() {
    }

    public DashboardResponseDTO(MaquinaDTO maquina, IndicadorOEEDTO indicadorAtual) {
        this.maquina = maquina;
        this.indicadorAtual = indicadorAtual;
    }

    public MaquinaDTO getMaquina() {
        return maquina;
    }

    public void setMaquina(MaquinaDTO maquina) {
        this.maquina = maquina;
    }

    public IndicadorOEEDTO getIndicadorAtual() {
        return indicadorAtual;
    }

    public void setIndicadorAtual(IndicadorOEEDTO indicadorAtual) {
        this.indicadorAtual = indicadorAtual;
    }
}