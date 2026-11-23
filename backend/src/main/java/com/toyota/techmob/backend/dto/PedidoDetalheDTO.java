package com.toyota.techmob.backend.dto;

import java.util.List;

/**
 * Resposta de GET /api/pedidos/detalhe/{pedidoId}: o pedido e as etapas,
 * em ordem de início, cada uma com o tempo padrão da estação.
 */
public record PedidoDetalheDTO(
        PedidoDTO pedido,
        List<EtapaPedidoDTO> etapas) {
}
