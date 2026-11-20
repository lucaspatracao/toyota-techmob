package com.toyota.techmob.backend.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toyota.techmob.backend.coleta.EstadoPlantaAtual;
import com.toyota.techmob.backend.config.PlantaProperties;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.dto.IndicadorOEEDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.dto.ResumoPedidosDTO;
import com.toyota.techmob.backend.dto.StatusPlantaDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.mapper.DtoMapper;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

import lombok.RequiredArgsConstructor;

/**
 * Regras de negócio da máquina e do dashboard.
 *
 * readOnly = true: as consultas do dashboard rodam na mesma transação, e
 * os relacionamentos lazy continuam acessíveis durante a conversão para DTO.
 *
 * "Hoje" é o dia corrente no fuso do relógio da aplicação (o do sistema;
 * no laboratório, o de Brasília).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MaquinaServiceImpl implements MaquinaService {

    private static final Logger logger = LoggerFactory.getLogger(MaquinaServiceImpl.class);

    private final MaquinaRepository maquinaRepository;
    private final IndicadorOEERepository indicadorOEERepository;
    private final PedidoService pedidoService;
    private final EstadoPlantaAtual estadoPlanta;
    private final PlantaProperties propriedades;
    private final Clock clock;

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

        // Antes do primeiro pedido concluído o indicador vem nulo e o
        // frontend mostra o estado vazio.
        IndicadorOEEDTO indicadorAtual = indicadorOEERepository
                .findTopByMaquinaIdOrderByPeriodoFimDesc(maquinaId)
                .map(DtoMapper::paraDTO)
                .orElse(null);

        ZoneId fuso = clock.getZone();
        LocalDate hoje = LocalDate.now(clock);
        Instant inicioDoDia = hoje.atStartOfDay(fuso).toInstant();
        Instant fimDoDia = hoje.plusDays(1).atStartOfDay(fuso).toInstant();
        ResumoPedidosDTO resumoHoje = pedidoService.resumir(maquinaId, inicioDoDia, fimDoDia, null);

        return new DashboardResponseDTO(
                DtoMapper.paraDTO(maquina),
                indicadorAtual,
                statusPlanta(maquinaId),
                resumoHoje,
                propriedades.metaOee());
    }

    @Override
    public StatusPlantaDTO buscarStatusPlanta(Long maquinaId) {
        if (!maquinaRepository.existsById(maquinaId)) {
            throw new MaquinaNotFoundException(maquinaId);
        }
        return statusPlanta(maquinaId);
    }

    private StatusPlantaDTO statusPlanta(Long maquinaId) {
        return DtoMapper.paraDTO(estadoPlanta.da(maquinaId), propriedades.tempoPadraoS().porEstacao(), clock.instant());
    }
}
