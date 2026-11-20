package com.toyota.techmob.backend.dto;

import java.time.Instant;
import java.util.List;

import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.OrigemPedido;

/**
 * Estado ao vivo da bancada (GET /api/planta/{maquinaId}/status), lido pelo
 * frontend a cada 2 s. Vem da memória do backend, não do banco.
 *
 * status: AGUARDANDO, PRODUZINDO, PARADO ou SEM_CONEXAO.
 * fonte: de onde vem a leitura (BANCADA, REPLAY ou SINTETICO); null antes
 * da primeira leitura.
 * operacao: texto de operacao_string, como a bancada envia.
 * pedidoEmAndamento: null quando a bancada está aguardando pedido.
 */
public record StatusPlantaDTO(
        Long maquinaId,
        String status,
        boolean conectado,
        OrigemPedido fonte,
        String operacao,
        List<EstacaoStatus> estacoes,
        PedidoEmAndamento pedidoEmAndamento,
        Double tempoPadraoPedidoS,
        Instant ultimaLeituraEm) {

    /**
     * Uma estação no Andon. status é o texto da bancada; situacao é a
     * leitura dele: OPERANDO, AGUARDANDO, PARADO ou SEM_CONEXAO.
     */
    public record EstacaoStatus(
            Estacao estacao,
            String status,
            String situacao,
            Double tempoPadraoS) {
    }

    /** Pedido aberto agora: decorridoS é calculado no momento da consulta. */
    public record PedidoEmAndamento(
            Long pedidoId,
            Integer numero,
            Instant inicio,
            Double decorridoS,
            Estacao etapaAtual) {
    }
}
