package com.toyota.techmob.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.service.MaquinaService;

import lombok.RequiredArgsConstructor;

/**
 * Controller oficial para GET /api/dashboard/{maquinaId}, conforme
 * especificado no Documento do Projeto (seção 5.4).
 *
 * Substitui a versão anterior que acessava MaquinaRepository e
 * IndicadorOEERepository diretamente e expunha um DashboardResponse
 * (record) com campos BigDecimal crus. Agora delega ao MaquinaService,
 * que já centraliza essa lógica (usado também pelo endpoint legado em
 * MaquinaController, que será removido).
 */
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