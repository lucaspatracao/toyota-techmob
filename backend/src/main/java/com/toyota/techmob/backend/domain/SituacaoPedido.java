package com.toyota.techmob.backend.domain;

/**
 * Situação do pedido na tabela pedido.
 *
 * INTERROMPIDO: a bancada ficou sem conexão com o pedido aberto; o pedido
 * fica gravado, mas sem OEE.
 */
public enum SituacaoPedido {
    EM_ANDAMENTO,
    CONCLUIDO,
    INTERROMPIDO
}
