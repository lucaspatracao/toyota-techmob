package com.toyota.techmob.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "historico_producao", schema = "techmob")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricoProducao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "status_operacional", nullable = false, length = 30)
    private String statusOperacional;

    @Column(name = "pecas_boas", nullable = false)
    private Integer pecasBoas;

    @Column(name = "pecas_defeituosas", nullable = false)
    private Integer pecasDefeituosas;

    @Column(name = "tempo_ciclo_segundos", precision = 10, scale = 3)
    private BigDecimal tempoCicloSegundos;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;
}