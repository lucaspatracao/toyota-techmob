package com.toyota.techmob.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.service.MaquinaService;

/**
 * Controller responsável apenas pelo recurso Maquina (GET /api/maquinas).
 *
 * O endpoint de dashboard (antes aqui como /api/maquinas/{id}/dashboard)
 * foi movido para DashboardController, em GET /api/dashboard/{maquinaId},
 * para bater com a rota oficial definida no Documento do Projeto
 * (seção 5.4) e eliminar a duplicidade de implementação que existia
 * entre este controller e o antigo DashboardController.
 */
@RestController
@RequestMapping("/api")
public class MaquinaController {

    private final MaquinaService maquinaService;

    @Autowired
    public MaquinaController(MaquinaService maquinaService) {
        this.maquinaService = maquinaService;
    }

    @GetMapping("/maquinas")
    public ResponseEntity<List<MaquinaDTO>> listarMaquinas() {
        return ResponseEntity.ok(maquinaService.listarTodas());
    }
}