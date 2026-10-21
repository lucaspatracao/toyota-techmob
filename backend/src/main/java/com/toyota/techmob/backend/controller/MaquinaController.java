package com.toyota.techmob.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.service.MaquinaService;

import lombok.RequiredArgsConstructor;

/** GET /api/maquinas: lista as máquinas cadastradas. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MaquinaController {

    private final MaquinaService maquinaService;

    @GetMapping("/maquinas")
    public ResponseEntity<List<MaquinaDTO>> listarMaquinas() {
        return ResponseEntity.ok(maquinaService.listarTodas());
    }
}
