package com.toyota.techmob.backend.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toyota.techmob.backend.domain.HistoricoProducao;
import com.toyota.techmob.backend.dto.HistoricoProducaoDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.repository.HistoricoProducaoRepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

@Service
@Transactional(readOnly = true)
public class HistoricoProducaoServiceImpl implements HistoricoProducaoService {

    private static final Logger logger = LoggerFactory.getLogger(HistoricoProducaoServiceImpl.class);

    private final MaquinaRepository maquinaRepository;
    private final HistoricoProducaoRepository historicoProducaoRepository;

    @Autowired
    public HistoricoProducaoServiceImpl(MaquinaRepository maquinaRepository,
            HistoricoProducaoRepository historicoProducaoRepository) {
        this.maquinaRepository = maquinaRepository;
        this.historicoProducaoRepository = historicoProducaoRepository;
    }

    @Override
    public List<HistoricoProducaoDTO> historico(Long maquinaId) {
        logger.debug("Buscando histórico de produção para maquinaId={}", maquinaId);

        if (!maquinaRepository.existsById(maquinaId)) {
            throw new MaquinaNotFoundException(maquinaId);
        }

        return historicoProducaoRepository
                .findByMaquinaIdOrderByTimestampDesc(maquinaId)
                .stream()
                .map(this::paraDTO)
                .toList();
    }

    private HistoricoProducaoDTO paraDTO(HistoricoProducao h) {
        return new HistoricoProducaoDTO(
                h.getMaquina().getId(),
                h.getTimestamp().toInstant(),
                h.getStatusOperacional(),
                h.getPecasBoas(),
                h.getPecasDefeituosas(),
                h.getTempoCicloSegundos());
    }
}