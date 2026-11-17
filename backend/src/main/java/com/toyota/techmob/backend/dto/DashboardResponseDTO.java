package com.toyota.techmob.backend.dto;

/**
 * Resposta de GET /api/dashboard/{maquinaId}: tudo o que a tela Dashboard
 * precisa numa chamada só.
 *
 * indicadorAtual: OEE do último pedido concluído (null se ainda não houver).
 * planta: estado ao vivo da bancada (o mesmo de /api/planta/{id}/status).
 * resumoHoje: pedidos do dia corrente.
 * metaOee: meta de OEE em % (variável META_OEE).
 */
public record DashboardResponseDTO(
        MaquinaDTO maquina,
        IndicadorOEEDTO indicadorAtual,
        StatusPlantaDTO planta,
        ResumoPedidosDTO resumoHoje,
        Double metaOee) {
}
