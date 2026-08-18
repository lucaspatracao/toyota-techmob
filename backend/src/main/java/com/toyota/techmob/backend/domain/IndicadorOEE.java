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
@Table(name = "indicador_oee", schema = "techmob")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IndicadorOEE {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(name = "periodo_inicio", nullable = false)
    private OffsetDateTime periodoInicio;

    @Column(name = "periodo_fim", nullable = false)
    private OffsetDateTime periodoFim;

    @Column(name = "disponibilidade", nullable = false, precision = 6, scale = 3)
    private BigDecimal disponibilidade;

    @Column(name = "performance", nullable = false, precision = 6, scale = 3)
    private BigDecimal performance;

    @Column(name = "qualidade", nullable = false, precision = 6, scale = 3)
    private BigDecimal qualidade;

    @Column(name = "oee", nullable = false, precision = 6, scale = 3)
    private BigDecimal oee;

    @Column(name = "pecas_boas", nullable = false)
    private Integer pecasBoas;

    @Column(name = "pecas_defeituosas", nullable = false)
    private Integer pecasDefeituosas;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;
}