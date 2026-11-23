package com.toyota.techmob.backend.coleta;

/**
 * Leitura do texto de status de uma estação, para o Andon:
 * OPERANDO (verde), AGUARDANDO (cinza), PARADO (vermelho).
 */
public enum SituacaoEstacao {
    OPERANDO,
    AGUARDANDO,
    PARADO,
    SEM_CONEXAO
}
