package com.toyota.techmob.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Um pedido da bancada (3 peças): do momento em que "aguardando" passa
 * de 1 para 0 até voltar para 1. Substitui a antiga HistoricoProducao.
 */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "maquina_id", nullable = false)
    private Maquina maquina;

    /** Sequência do pedido na máquina (1, 2, 3...). */
    @Column(name = "numero", nullable = false)
    private Integer numero;

    // VARCHAR explícito: sem isso o Hibernate espera o tipo ENUM nativo do MySQL.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "origem", nullable = false, length = 20)
    private OrigemPedido origem;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "situacao", nullable = false, length = 20)
    private SituacaoPedido situacao;

    @Column(name = "inicio", nullable = false)
    private OffsetDateTime inicio;

    /** Nulo enquanto o pedido está em andamento. */
    @Column(name = "fim")
    private OffsetDateTime fim;

    @Column(name = "lead_time_s", precision = 10, scale = 3)
    private BigDecimal leadTimeS;

    @Column(name = "tempo_parado_s", nullable = false, precision = 10, scale = 3)
    private BigDecimal tempoParadoS;

    @Column(name = "reprovado", nullable = false)
    private Boolean reprovado;

    @Column(name = "pecas_boas", nullable = false)
    private Integer pecasBoas;

    @Column(name = "pecas_defeituosas", nullable = false)
    private Integer pecasDefeituosas;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("inicio ASC")
    @Builder.Default
    private List<EtapaPedido> etapas = new ArrayList<>();

    /** OEE do pedido; nulo enquanto não concluído e em pedidos interrompidos. */
    @OneToOne(mappedBy = "pedido", fetch = FetchType.LAZY)
    private IndicadorOEE indicador;

    /** Liga a etapa a este pedido nos dois lados do relacionamento. */
    public void adicionarEtapa(EtapaPedido etapa) {
        etapa.setPedido(this);
        etapas.add(etapa);
    }

    /**
     * Tempo padrão do pedido: soma da cronometragem das etapas que ele teve
     * (zero sem etapas). É o T_padrão da performance.
     */
    public BigDecimal somaTempoPadraoS() {
        return etapas.stream()
                .map(EtapaPedido::getTempoPadraoS)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
