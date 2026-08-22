package com.toyota.techmob.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.toyota.techmob.backend.dto.HistoricoProducaoDTO;
import com.toyota.techmob.backend.service.HistoricoProducaoService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/historico-producao")
@RequiredArgsConstructor
public class HistoricoProducaoController {

    private final HistoricoProducaoService historicoProducaoService;

    @GetMapping("/{maquinaId}")
    public ResponseEntity<List<HistoricoProducaoDTO>> historico(@PathVariable Long maquinaId) {
        return ResponseEntity.ok(historicoProducaoService.historico(maquinaId));
    }
}