package com.toyota.techmob.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toyota.techmob.backend.dto.StatusPlantaDTO;
import com.toyota.techmob.backend.service.MaquinaService;

import lombok.RequiredArgsConstructor;

/**
 * GET /api/planta/{maquinaId}/status: estado ao vivo da bancada (estações,
 * pedido em andamento, fonte e conexão), lido pelo frontend a cada 2 s.
 */
@RestController
@RequestMapping("/api/planta")
@RequiredArgsConstructor
public class PlantaController {

    private final MaquinaService maquinaService;

    @GetMapping("/{maquinaId}/status")
    public ResponseEntity<StatusPlantaDTO> status(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(maquinaService.buscarStatusPlanta(maquinaId));
    }
}
