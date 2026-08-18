package com.toyota.techmob.backend.controller;

import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.repository.MaquinaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/maquinas")
@RequiredArgsConstructor
public class MaquinaController {

    private final MaquinaRepository maquinaRepository;

    @GetMapping
    public List<Maquina> listar() {
        return maquinaRepository.findAll();
    }
}