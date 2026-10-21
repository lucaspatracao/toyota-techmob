package com.toyota.techmob.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toyota.techmob.backend.dto.IndicadorOEEDTO;
import com.toyota.techmob.backend.service.IndicadorOEEService;

import lombok.RequiredArgsConstructor;

/** GET /api/indicadores/historico/{maquinaId}: série de OEE, do mais recente ao mais antigo. */
@RestController
@RequestMapping("/api/indicadores")
@RequiredArgsConstructor
public class IndicadorOEEController {

    private final IndicadorOEEService indicadorOEEService;

    @GetMapping("/historico/{maquinaId}")
    public ResponseEntity<List<IndicadorOEEDTO>> historico(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(indicadorOEEService.historico(maquinaId));
    }
}
