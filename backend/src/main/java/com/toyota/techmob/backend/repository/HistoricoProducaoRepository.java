package com.toyota.techmob.backend.repository;

import com.toyota.techmob.backend.domain.HistoricoProducao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoProducaoRepository extends JpaRepository<HistoricoProducao, Long> {
    List<HistoricoProducao> findByMaquinaIdOrderByTimestampDesc(Long maquinaId);
}