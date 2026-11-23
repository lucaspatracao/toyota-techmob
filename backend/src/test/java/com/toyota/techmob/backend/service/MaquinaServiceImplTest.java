package com.toyota.techmob.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toyota.techmob.backend.coleta.EstadoPlantaAtual;
import com.toyota.techmob.backend.config.PlantaProperties;
import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.dto.ResumoPedidosDTO;
import com.toyota.techmob.backend.dto.StatusPlantaDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

/**
 * Testes unitários do MaquinaServiceImpl (listagem, dashboard e estado ao
 * vivo). Repositórios e PedidoService são mockados: nenhum teste toca o banco.
 */
@ExtendWith(MockitoExtension.class)
class MaquinaServiceImplTest {

    /** 17/11/2026 às 10:00 em Brasília (13:00 UTC). */
    private static final Clock RELOGIO = Clock.fixed(Instant.parse("2026-11-17T13:00:00Z"),
            ZoneId.of("America/Sao_Paulo"));

    private static final PlantaProperties PROPRIEDADES = new PlantaProperties(1L,
            "http://bancada:1880/dadosPlanta", "http://simulador:1881/dadosPlanta", 1000, 2000, 10,
            "AGUARDANDO", "PARADO", 75, new PlantaProperties.TempoPadrao(20, 35, 25));

    @Mock
    private MaquinaRepository maquinaRepository;

    @Mock
    private IndicadorOEERepository indicadorOEERepository;

    @Mock
    private PedidoService pedidoService;

    private final EstadoPlantaAtual estadoPlanta = new EstadoPlantaAtual();

    private MaquinaService maquinaService;

    @BeforeEach
    void setUp() {
        maquinaService = new MaquinaServiceImpl(maquinaRepository, indicadorOEERepository, pedidoService,
                estadoPlanta, PROPRIEDADES, RELOGIO);
    }

    private Maquina maquinaExemplo() {
        Maquina maquina = new Maquina();
        maquina.setId(1L);
        maquina.setIdentificadorBancada("BANCADA_SMART_01");
        maquina.setNome("Bancada Smart 4.0");
        maquina.setStatusOperacional("PRODUZINDO");
        return maquina;
    }

    private ResumoPedidosDTO resumoVazio() {
        return new ResumoPedidosDTO(0, 0, 0, 0, 0, null, null, 0.0, null, null, null, null);
    }

    /** Resumo de hoje: de 00:00 a 24:00 em Brasília, ou seja, 03:00 UTC a 03:00 UTC. */
    private void quandoResumirHoje(ResumoPedidosDTO resumo) {
        when(pedidoService.resumir(1L, Instant.parse("2026-11-17T03:00:00Z"),
                Instant.parse("2026-11-18T03:00:00Z"), null)).thenReturn(resumo);
    }

    @Test
    void listarTodas_deveMapearEntidadeParaDTO() {
        when(maquinaRepository.findAll()).thenReturn(List.of(maquinaExemplo()));

        List<MaquinaDTO> resultado = maquinaService.listarTodas();

        assertThat(resultado).containsExactly(
                new MaquinaDTO(1L, "BANCADA_SMART_01", "Bancada Smart 4.0", "PRODUZINDO", null));
    }

    @Test
    void buscarDashboard_juntaIndicadorEstadoAoVivoResumoDeHojeEMeta() {
        Maquina maquina = maquinaExemplo();
        IndicadorOEE indicador = new IndicadorOEE();
        indicador.setMaquina(maquina);
        indicador.setDisponibilidade(BigDecimal.valueOf(88));
        indicador.setPerformance(BigDecimal.valueOf(90.909));
        indicador.setQualidade(BigDecimal.valueOf(100));
        indicador.setOee(BigDecimal.valueOf(80));
        indicador.setCriadoEm(OffsetDateTime.parse("2026-11-17T12:59:00Z"));
        ResumoPedidosDTO resumo = new ResumoPedidosDTO(1, 1, 0, 0, 3, 0.0, 100.0, 12.0, 88.0, 90.909, 100.0, 80.0);

        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
        when(indicadorOEERepository.findTopByMaquinaIdOrderByPeriodoFimDesc(1L)).thenReturn(Optional.of(indicador));
        quandoResumirHoje(resumo);

        DashboardResponseDTO resultado = maquinaService.buscarDashboard(1L);

        assertThat(resultado.maquina().bancadaId()).isEqualTo("BANCADA_SMART_01");
        assertThat(resultado.indicadorAtual().oee()).isEqualTo(80.0);
        assertThat(resultado.indicadorAtual().calculadoEm()).hasToString("2026-11-17T12:59:00Z");
        assertThat(resultado.resumoHoje()).isEqualTo(resumo);
        assertThat(resultado.metaOee()).isEqualTo(75.0);
        // Coleta sem leitura dessa máquina: o estado ao vivo é SEM_CONEXAO.
        assertThat(resultado.planta().status()).isEqualTo("SEM_CONEXAO");
        assertThat(resultado.planta().tempoPadraoPedidoS()).isEqualTo(80.0);
    }

    @Test
    void buscarDashboard_semIndicador_deveRetornarIndicadorNulo() {
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquinaExemplo()));
        when(indicadorOEERepository.findTopByMaquinaIdOrderByPeriodoFimDesc(1L)).thenReturn(Optional.empty());
        quandoResumirHoje(resumoVazio());

        DashboardResponseDTO resultado = maquinaService.buscarDashboard(1L);

        assertThat(resultado.maquina()).isNotNull();
        assertThat(resultado.indicadorAtual()).isNull();
        assertThat(resultado.resumoHoje().concluidos()).isZero();
    }

    @Test
    void buscarDashboard_deveLancarExcecao_quandoMaquinaNaoExiste() {
        when(maquinaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maquinaService.buscarDashboard(999L))
                .isInstanceOf(MaquinaNotFoundException.class)
                .hasMessageContaining("999");
        verifyNoInteractions(pedidoService);
    }

    @Test
    void buscarStatusPlanta_trazOEstadoDaMemoria() {
        when(maquinaRepository.existsById(1L)).thenReturn(true);

        StatusPlantaDTO status = maquinaService.buscarStatusPlanta(1L);

        assertThat(status.maquinaId()).isEqualTo(1L);
        assertThat(status.conectado()).isFalse();
        assertThat(status.estacoes()).extracting(StatusPlantaDTO.EstacaoStatus::tempoPadraoS)
                .containsExactly(20.0, 35.0, 25.0);
    }

    @Test
    void buscarStatusPlanta_deveLancarExcecao_quandoMaquinaNaoExiste() {
        when(maquinaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> maquinaService.buscarStatusPlanta(999L))
                .isInstanceOf(MaquinaNotFoundException.class)
                .hasMessageContaining("999");
    }
}
