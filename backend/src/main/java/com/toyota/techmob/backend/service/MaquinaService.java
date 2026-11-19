package com.toyota.techmob.backend.service;

import java.util.List;

import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.dto.StatusPlantaDTO;

public interface MaquinaService {

    List<MaquinaDTO> listarTodas();

    /** OEE do último pedido, estado ao vivo, resumo de hoje e meta, numa chamada só. */
    DashboardResponseDTO buscarDashboard(Long maquinaId);

    /** Estado ao vivo da bancada (memória da coleta, não o banco). */
    StatusPlantaDTO buscarStatusPlanta(Long maquinaId);
}
