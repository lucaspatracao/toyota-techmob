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
@Table(name = "maquina")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Maquina {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identificador_bancada", nullable = false, unique = true, length = 50)
    private String identificadorBancada;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "status_operacional", nullable = false, length = 30)
    private String statusOperacional;

    @Column(name = "capacidade_teorica", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacidadeTeorica;

    @Column(name = "tempo_planejado_producao_s", nullable = false, precision = 12, scale = 2)
    private BigDecimal tempoPlanejadoProducaoS;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private OffsetDateTime atualizadoEm;
}