package com.toyota.techmob.backend.dto;

import java.time.Instant;

/**
 * Resposta da API para uma máquina.
 *
 * Nunca expomos a entidade JPA direto: assim não vazam colunas internas
 * e o banco pode mudar sem quebrar o contrato com o frontend.
 *
 * statusAtual: AGUARDANDO, PRODUZINDO, PARADO ou SEM_CONEXAO.
 * ultimaLeituraEm vem null antes da primeira leitura da bancada.
 */
public record MaquinaDTO(
        Long id,
        String bancadaId,
        String nome,
        String statusAtual,
        Instant ultimaLeituraEm) {
}
