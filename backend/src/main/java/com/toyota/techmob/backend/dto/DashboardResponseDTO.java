package com.toyota.techmob.backend.dto;

/**
 * Resposta de GET /api/dashboard/{maquinaId}: dados da máquina mais o
 * indicador de OEE mais recente, para o frontend fazer uma chamada só.
 *
 * indicadorAtual vem null quando ainda não há OEE calculado.
 */
public record DashboardResponseDTO(
        MaquinaDTO maquina,
        IndicadorOEEDTO indicadorAtual) {
}
