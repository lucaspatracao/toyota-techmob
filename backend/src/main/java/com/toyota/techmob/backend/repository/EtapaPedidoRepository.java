package com.toyota.techmob.backend.repository;

import com.toyota.techmob.backend.domain.EtapaPedido;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EtapaPedidoRepository extends JpaRepository<EtapaPedido, Long> {
    List<EtapaPedido> findByPedidoIdOrderByInicioAsc(Long pedidoId);
}
