package com.toyota.techmob.backend.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * DTO de resposta para o histórico bruto de produção (tabela
 * techmob.historico_producao), gerado a partir dos dados capturados
 * pelo Node-RED via MQTT e persistidos ciclo a ciclo.
 *
 * Segue o mesmo princípio dos demais DTOs do projeto: nunca expor a
 * entidade JPA diretamente (evita LazyInitializationException ao
 * serializar o relacionamento `maquina` e vazamento de colunas de
 * auditoria internas).
 */
public class HistoricoProducaoDTO {

    private Long maquinaId;
    private Instant timestamp;
    private String statusOperacional;
    private Integer pecasBoas;
    private Integer pecasDefeituosas;
    private BigDecimal tempoCicloSegundos;

    public HistoricoProducaoDTO() {
    }

    public HistoricoProducaoDTO(Long maquinaId, Instant timestamp, String statusOperacional,
            Integer pecasBoas, Integer pecasDefeituosas, BigDecimal tempoCicloSegundos) {
        this.maquinaId = maquinaId;
        this.timestamp = timestamp;
        this.statusOperacional = statusOperacional;
        this.pecasBoas = pecasBoas;
        this.pecasDefeituosas = pecasDefeituosas;
        this.tempoCicloSegundos = tempoCicloSegundos;
    }

    public Long getMaquinaId() {
        return maquinaId;
    }

    public void setMaquinaId(Long maquinaId) {
        this.maquinaId = maquinaId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatusOperacional() {
        return statusOperacional;
    }

    public void setStatusOperacional(String statusOperacional) {
        this.statusOperacional = statusOperacional;
    }

    public Integer getPecasBoas() {
        return pecasBoas;
    }

    public void setPecasBoas(Integer pecasBoas) {
        this.pecasBoas = pecasBoas;
    }

    public Integer getPecasDefeituosas() {
        return pecasDefeituosas;
    }

    public void setPecasDefeituosas(Integer pecasDefeituosas) {
        this.pecasDefeituosas = pecasDefeituosas;
    }

    public BigDecimal getTempoCicloSegundos() {
        return tempoCicloSegundos;
    }

    public void setTempoCicloSegundos(BigDecimal tempoCicloSegundos) {
        this.tempoCicloSegundos = tempoCicloSegundos;
    }
}