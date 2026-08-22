package com.toyota.techmob.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.dto.IndicadorOEEDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

@Service
@Transactional(readOnly = true)
public class IndicadorOEEServiceImpl implements IndicadorOEEService {

    private static final Logger logger = LoggerFactory.getLogger(IndicadorOEEServiceImpl.class);

    private final MaquinaRepository maquinaRepository;
    private final IndicadorOEERepository indicadorOEERepository;

    @Autowired
    public IndicadorOEEServiceImpl(MaquinaRepository maquinaRepository,
            IndicadorOEERepository indicadorOEERepository) {
        this.maquinaRepository = maquinaRepository;
        this.indicadorOEERepository = indicadorOEERepository;
    }

    @Override
    public List<IndicadorOEEDTO> historico(Long maquinaId) {
        logger.debug("Buscando histórico de indicadores para maquinaId={}", maquinaId);

        if (!maquinaRepository.existsById(maquinaId)) {
            throw new MaquinaNotFoundException(maquinaId);
        }

        return indicadorOEERepository
                .findByMaquinaIdOrderByPeriodoInicioDesc(maquinaId)
                .stream()
                .map(this::paraDTO)
                .toList();
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