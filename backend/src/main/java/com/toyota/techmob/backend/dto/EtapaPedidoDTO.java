package com.toyota.techmob.backend.dto;

import java.time.Instant;

import com.toyota.techmob.backend.domain.Estacao;

/**
 * Uma etapa do pedido (tabela etapa_pedido). Tempos em segundos;
 * tempoPadraoS é a cronometragem da estação.
 */
public record EtapaPedidoDTO(
        Estacao estacao,
        Instant inicio,
        Instant fim,
        Double duracaoS,
        Double tempoParadoS,
        Double tempoPadraoS) {
}
