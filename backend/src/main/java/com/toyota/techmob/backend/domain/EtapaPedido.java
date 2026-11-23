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

/**
 * Tempo de uma estação dentro de um pedido: abre quando a estação sai de
 * AGUARDANDO e fecha quando volta.
 */
@Entity
@Table(name = "etapa_pedido")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "estacao", nullable = false, length = 20)
    private Estacao estacao;

    @Column(name = "inicio", nullable = false)
    private OffsetDateTime inicio;

    @Column(name = "fim", nullable = false)
    private OffsetDateTime fim;

    /** Tempo com a estação fora de AGUARDANDO, em segundos. */
    @Column(name = "duracao_s", nullable = false, precision = 10, scale = 3)
    private BigDecimal duracaoS;

    @Column(name = "tempo_parado_s", nullable = false, precision = 10, scale = 3)
    private BigDecimal tempoParadoS;

    /** Cronometragem da estação no momento do pedido. */
    @Column(name = "tempo_padrao_s", nullable = false, precision = 10, scale = 3)
    private BigDecimal tempoPadraoS;
}
