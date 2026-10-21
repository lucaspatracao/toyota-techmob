package com.toyota.techmob.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.service.MaquinaService;

import lombok.RequiredArgsConstructor;

/** GET /api/dashboard/{maquinaId}: máquina + OEE mais recente (Documento do Projeto, seção 5.4). */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final MaquinaService maquinaService;

    @GetMapping("/{maquinaId}")
    public ResponseEntity<DashboardResponseDTO> buscarDashboard(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(maquinaService.buscarDashboard(maquinaId));
    }
}
