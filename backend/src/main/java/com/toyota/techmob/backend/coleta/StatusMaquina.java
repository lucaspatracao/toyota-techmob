package com.toyota.techmob.backend.coleta;

/**
 * Status da bancada como um todo, gravado em maquina.status_operacional.
 *
 * AGUARDANDO: sem pedido ("aguardando" = 1).
 * PRODUZINDO: pedido em andamento.
 * PARADO: estação em parada, ou "aguardando" e "produzindo" em 0.
 * SEM_CONEXAO: a última leitura falhou.
 */
public enum StatusMaquina {
    AGUARDANDO,
    PRODUZINDO,
    PARADO,
    SEM_CONEXAO
}
