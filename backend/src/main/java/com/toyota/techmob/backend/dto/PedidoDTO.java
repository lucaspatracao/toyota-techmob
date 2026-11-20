package com.toyota.techmob.backend.dto;

import java.time.Instant;
import java.util.Map;

import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.domain.SituacaoPedido;

/**
 * Um pedido na listagem de GET /api/pedidos/{maquinaId}.
 *
 * Tempos em segundos. tempoPadraoS é a soma da cronometragem das etapas.
 * duracaoEtapasS traz o tempo de cada estação (chave = estação), para os
 * gráficos de etapa sem precisar buscar o detalhe.
 * disponibilidade, performance, qualidade e oee (0-100) vêm null em pedido
 * em andamento ou interrompido.
 */
public record PedidoDTO(
        Long id,
        Long maquinaId,
        Integer numero,
        OrigemPedido origem,
        SituacaoPedido situacao,
        Instant inicio,
        Instant fim,
        Double leadTimeS,
        Double tempoParadoS,
        Double tempoPadraoS,
        Boolean reprovado,
        Integer pecasBoas,
        Integer pecasDefeituosas,
        Map<Estacao, Double> duracaoEtapasS,
        Double disponibilidade,
        Double performance,
        Double qualidade,
        Double oee) {
}
