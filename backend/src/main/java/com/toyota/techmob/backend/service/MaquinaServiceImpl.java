package com.toyota.techmob.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.dto.IndicadorOEEDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.mapper.DtoMapper;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Regras de negócio da máquina e do dashboard.
 *
 * readOnly = true: as duas consultas do dashboard rodam na mesma
 * transação, e o relacionamento lazy com Maquina continua acessível
 * durante a conversão para DTO.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MaquinaServiceImpl implements MaquinaService {

    private static final Logger logger = LoggerFactory.getLogger(MaquinaServiceImpl.class);

    private final MaquinaRepository maquinaRepository;
    private final IndicadorOEERepository indicadorOEERepository;

    @Override
    public List<MaquinaDTO> listarTodas() {
        logger.debug("Listando todas as máquinas cadastradas");
        return maquinaRepository.findAll()
                .stream()
                .map(DtoMapper::paraDTO)
                .toList();
    }

    @Override
    public DashboardResponseDTO buscarDashboard(Long maquinaId) {
        logger.debug("Buscando dashboard para maquinaId={}", maquinaId);

        Maquina maquina = maquinaRepository.findById(maquinaId)
                .orElseThrow(() -> new MaquinaNotFoundException(maquinaId));

        // Sem OEE calculado ainda (ex.: o Python nunca rodou), devolve o
        // dashboard com indicador nulo; o frontend mostra o estado vazio.
        IndicadorOEEDTO indicadorAtual = indicadorOEERepository
                .findTopByMaquinaIdOrderByCriadoEmDesc(maquinaId)
                .map(DtoMapper::paraDTO)
                .orElse(null);

        if (indicadorAtual == null) {
            logger.warn("Nenhum indicador OEE calculado ainda para maquinaId={}", maquinaId);
        }

        return new DashboardResponseDTO(DtoMapper.paraDTO(maquina), indicadorAtual);
    }
}
