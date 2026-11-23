package com.toyota.techmob.backend.repository;

import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.domain.Pedido;
import com.toyota.techmob.backend.domain.SituacaoPedido;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    /**
     * Pedidos de uma máquina que começaram em [de, ate), do mais recente ao
     * mais antigo, já com etapas e indicador (uma consulta só).
     * origem nula = todas as origens.
     */
    @EntityGraph(attributePaths = {"etapas", "indicador"})
    @Query("""
            select p from Pedido p
            where p.maquina.id = :maquinaId
              and p.inicio >= :de and p.inicio < :ate
              and (:origem is null or p.origem = :origem)
            order by p.inicio desc
            """)
    List<Pedido> buscarPorPeriodo(@Param("maquinaId") Long maquinaId,
                                  @Param("de") OffsetDateTime de,
                                  @Param("ate") OffsetDateTime ate,
                                  @Param("origem") OrigemPedido origem);

    /** Pedido com etapas e indicador, para a tela de detalhe. */
    @EntityGraph(attributePaths = {"etapas", "indicador"})
    Optional<Pedido> findDetalheById(Long id);

    /** Maior número de pedido da máquina (0 se ainda não houver pedido). */
    @Query("select coalesce(max(p.numero), 0) from Pedido p where p.maquina.id = :maquinaId")
    int ultimoNumero(@Param("maquinaId") Long maquinaId);

    List<Pedido> findBySituacao(SituacaoPedido situacao);
}
