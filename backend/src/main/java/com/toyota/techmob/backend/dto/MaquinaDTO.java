package com.toyota.techmob.backend.dto;

/**
 * Resposta da API para uma máquina.
 *
 * Nunca expomos a entidade JPA direto: assim não vazam colunas internas
 * e o banco pode mudar sem quebrar o contrato com o frontend.
 */
public record MaquinaDTO(
        Long id,
        String bancadaId,
        String nome,
        String statusAtual) {
}
