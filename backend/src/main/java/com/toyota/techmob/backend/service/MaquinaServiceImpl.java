package com.toyota.techmob.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.dto.IndicadorOEEDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

/**
 * Camada de serviço responsável pela lógica de negócio envolvendo Maquina
 * e seus indicadores de OEE.
 */
@Service
public class MaquinaServiceImpl implements MaquinaService {

    private static final Logger logger = LoggerFactory.getLogger(MaquinaServiceImpl.class);

    private final MaquinaRepository maquinaRepository;
    private final IndicadorOEERepository indicadorOEERepository;

    @Autowired
    public MaquinaServiceImpl(MaquinaRepository maquinaRepository,
            IndicadorOEERepository indicadorOEERepository) {
        this.maquinaRepository = maquinaRepository;
        this.indicadorOEERepository = indicadorOEERepository;
    }

    @Override
    public List<MaquinaDTO> listarTodas() {
        logger.debug("Listando todas as máquinas cadastradas");
        return maquinaRepository.findAll()
                .stream()
                .map(this::paraDTO)
                .toList();
    }

    @Override
    public DashboardResponseDTO buscarDashboard(Long maquinaId) {
        logger.debug("Buscando dashboard para maquinaId={}", maquinaId);

        Maquina maquina = maquinaRepository.findById(maquinaId)
                .orElseThrow(() -> new MaquinaNotFoundException(maquinaId));

        // Pega o indicador de OEE mais recente calculado para essa máquina.
        // Se nenhum indicador foi calculado ainda (ex.: módulo Python nunca
        // rodou para essa máquina), retorna o dashboard sem indicador em vez
        // de estourar erro — o front-end decide como exibir esse estado vazio.
        IndicadorOEEDTO indicadorDTO = indicadorOEERepository
                .findTopByMaquinaIdOrderByCriadoEmDesc(maquinaId)
                .map(this::paraDTO)
                .orElse(null);

        if (indicadorDTO == null) {
            logger.warn("Nenhum indicador OEE calculado ainda para maquinaId={}", maquinaId);
        }

        return new DashboardResponseDTO(paraDTO(maquina), indicadorDTO);
    }

    private MaquinaDTO paraDTO(Maquina maquina) {
        return new MaquinaDTO(
                maquina.getId(),
                maquina.getIdentificadorBancada(),
                maquina.getNome(),
                maquina.getStatusOperacional());
    }

    private IndicadorOEEDTO paraDTO(IndicadorOEE indicador) {
        return new IndicadorOEEDTO(
                indicador.getMaquina().getId(),
                indicador.getDisponibilidade().doubleValue(),
                indicador.getPerformance().doubleValue(),
                indicador.getQualidade().doubleValue(),
                indicador.getOee().doubleValue(),
                indicador.getCriadoEm().toInstant());
    }
}