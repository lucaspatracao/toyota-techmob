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

/**
 * Controller para GET /api/indicadores/historico/{maquinaId}.
 *
 * Antes acessava MaquinaRepository e IndicadorOEERepository direto e
 * retornava List<IndicadorOEE> (entidade JPA crua). Agora delega ao
 * IndicadorOEEService, que retorna IndicadorOEEDTO — elimina o risco de
 * LazyInitializationException/N+1 ao serializar o relacionamento
 * `maquina` da entidade.
 */
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