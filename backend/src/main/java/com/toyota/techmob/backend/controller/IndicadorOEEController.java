package com.toyota.techmob.backend.controller;

import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/indicadores")
@RequiredArgsConstructor
public class IndicadorOEEController {

    private final MaquinaRepository maquinaRepository;
    private final IndicadorOEERepository indicadorOEERepository;

    @GetMapping("/historico/{maquinaId}")
    public List<IndicadorOEE> historico(@PathVariable Long maquinaId) {

        if (!maquinaRepository.existsById(maquinaId)) {
            throw new MaquinaNotFoundException(maquinaId);
        }

        return indicadorOEERepository.findByMaquinaIdOrderByPeriodoInicioDesc(maquinaId);
    }
}