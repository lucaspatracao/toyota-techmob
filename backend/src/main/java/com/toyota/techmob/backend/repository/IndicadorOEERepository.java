package com.toyota.techmob.backend.repository;

import com.toyota.techmob.backend.domain.IndicadorOEE;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndicadorOEERepository extends JpaRepository<IndicadorOEE, Long> {
    List<IndicadorOEE> findByMaquinaIdOrderByPeriodoInicioDesc(Long maquinaId);

    Optional<IndicadorOEE> findTopByMaquinaIdOrderByCriadoEmDesc(Long maquinaId);
}