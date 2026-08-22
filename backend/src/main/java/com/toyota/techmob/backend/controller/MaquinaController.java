package com.toyota.techmob.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.service.MaquinaService;

/**
 * Controller refatorado: agora só orquestra a requisição HTTP e delega a
 * lógica de negócio para o MaquinaService. Isso substitui a versão anterior
 * que (presumivelmente) chamava o MaquinaRepository diretamente.
 *
 * O tratamento de "máquina não encontrada" continua no
 * GlobalExceptionHandler (@ControllerAdvice) já existente, via
 * MaquinaNotFoundException lançada pelo Service.
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

    @GetMapping("/maquinas/{maquinaId}/dashboard")
    public ResponseEntity<DashboardResponseDTO> buscarDashboard(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(maquinaService.buscarDashboard(maquinaId));
}

}