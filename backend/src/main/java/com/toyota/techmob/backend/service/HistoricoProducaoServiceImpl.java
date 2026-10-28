package com.toyota.techmob.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toyota.techmob.backend.dto.HistoricoProducaoDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.mapper.DtoMapper;
import com.toyota.techmob.backend.repository.HistoricoProducaoRepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

import lombok.RequiredArgsConstructor;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HistoricoProducaoServiceImpl implements HistoricoProducaoService {

    private static final Logger logger = LoggerFactory.getLogger(HistoricoProducaoServiceImpl.class);

    private final MaquinaRepository maquinaRepository;
    private final HistoricoProducaoRepository historicoProducaoRepository;

    @Override
    public List<HistoricoProducaoDTO> historico(Long maquinaId) {
        logger.debug("Buscando histórico de produção para maquinaId={}", maquinaId);

        if (!maquinaRepository.existsById(maquinaId)) {
            throw new MaquinaNotFoundException(maquinaId);
        }

        return historicoProducaoRepository
                .findByMaquinaIdOrderByTimestampDesc(maquinaId)
                .stream()
                .map(DtoMapper::paraDTO)
                .toList();
    }
}
