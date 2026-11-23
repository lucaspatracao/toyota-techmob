package com.toyota.techmob.backend.domain;

/**
 * As três estações da Bancada Smart 4.0, na ordem em que o pedido passa
 * por elas. Cada uma vira uma etapa do pedido (tabela etapa_pedido).
 */
public enum Estacao {
    ESTOQUE,
    MANIPULACAO,
    SEPARACAO
}
