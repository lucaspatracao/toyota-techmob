package com.toyota.techmob.backend.repository;

import com.toyota.techmob.backend.domain.Maquina;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaquinaRepository extends JpaRepository<Maquina, Long> {
    Optional<Maquina> findByIdentificadorBancada(String identificadorBancada);
}