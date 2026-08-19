package com.toyota.techmob.backend.service;

import java.util.List;

import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;

public interface MaquinaService {

    List<MaquinaDTO> listarTodas();

    DashboardResponseDTO buscarDashboard(Long maquinaId);
}