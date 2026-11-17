package com.toyota.techmob.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * Linha da tabela leitura_planta: o JSON bruto de cada mudança lida no
 * /dadosPlanta. Permite rastrear qualquer OEE até o dado da bancada.
 */
@Entity
@Table(name = "leitura_planta")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LeituraBruta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    @Column(name = "lido_em", nullable = false)
    private OffsetDateTime lidoEm;

    /** JSON exatamente como veio da bancada (ou do simulador). */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private String payload;
}
