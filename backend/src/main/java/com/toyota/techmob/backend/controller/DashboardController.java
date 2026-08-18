package com.toyota.techmob.backend.controller;

import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.dto.DashboardResponse;
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
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MaquinaRepository maquinaRepository;
    private final IndicadorOEERepository indicadorOEERepository;

    @GetMapping("/{maquinaId}")
    public DashboardResponse buscarDashboard(@PathVariable Long maquinaId) {

        Maquina maquina = maquinaRepository.findById(maquinaId)
                .orElseThrow(() -> new MaquinaNotFoundException(maquinaId));

        List<IndicadorOEE> indicadores =
                indicadorOEERepository.findByMaquinaIdOrderByPeriodoInicioDesc(maquinaId);

        IndicadorOEE ultimo = indicadores.isEmpty() ? null : indicadores.get(0);

        return new DashboardResponse(
                maquina.getId(),
                maquina.getIdentificadorBancada(),
                maquina.getNome(),
                maquina.getStatusOperacional(),
                ultimo != null ? ultimo.getOee() : null,
                ultimo != null ? ultimo.getDisponibilidade() : null,
                ultimo != null ? ultimo.getPerformance() : null,
                ultimo != null ? ultimo.getQualidade() : null,
                ultimo != null ? ultimo.getPeriodoInicio() : null,
                ultimo != null ? ultimo.getPeriodoFim() : null
        );
    }
}