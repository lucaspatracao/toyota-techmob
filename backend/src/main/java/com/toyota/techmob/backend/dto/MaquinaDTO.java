package com.toyota.techmob.backend.dto;

/**
 * DTO de resposta para a entidade Maquina.
 *
 * Nunca expomos a entidade JPA diretamente na API: isso evita vazar detalhes
 * internos do schema (ex.: colunas de auditoria, relacionamentos lazy que
 * disparam LazyInitializationException na serialização) e permite mudar o
 * banco sem quebrar o contrato com o front-end.
 *
 * Ajuste os campos abaixo para bater exatamente com os atributos da sua
 * entidade Maquina, se os nomes forem diferentes.
 */
public class MaquinaDTO {

    private Long id;
    private String bancadaId;
    private String nome;
    private String statusAtual;

    public MaquinaDTO() {
    }

    public MaquinaDTO(Long id, String bancadaId, String nome, String statusAtual) {
        this.id = id;
        this.bancadaId = bancadaId;
        this.nome = nome;
        this.statusAtual = statusAtual;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBancadaId() {
        return bancadaId;
    }

    public void setBancadaId(String bancadaId) {
        this.bancadaId = bancadaId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getStatusAtual() {
        return statusAtual;
    }

    public void setStatusAtual(String statusAtual) {
        this.statusAtual = statusAtual;
    }
}