package com.toyota.techmob.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

/**
 * Testes unitários do MaquinaServiceImpl (listagem e dashboard).
 * Repositórios são mockados: nenhum teste toca o banco.
 */
@ExtendWith(MockitoExtension.class)
class MaquinaServiceImplTest {

    @Mock
    private MaquinaRepository maquinaRepository;

    @Mock
    private IndicadorOEERepository indicadorOEERepository;

    private MaquinaService maquinaService;

    @BeforeEach
    void setUp() {
        maquinaService = new MaquinaServiceImpl(maquinaRepository, indicadorOEERepository);
    }

    private Maquina maquinaExemplo() {
        Maquina maquina = new Maquina();
        maquina.setId(1L);
        maquina.setIdentificadorBancada("BANCADA_SMART_01");
        maquina.setNome("Bancada Smart 4.0");
        maquina.setStatusOperacional("EM_PRODUCAO");
        return maquina;
    }

    @Test
    void listarTodas_deveMapearEntidadeParaDTO() {
        when(maquinaRepository.findAll()).thenReturn(List.of(maquinaExemplo()));

        List<MaquinaDTO> resultado = maquinaService.listarTodas();

        assertThat(resultado).containsExactly(
                new MaquinaDTO(1L, "BANCADA_SMART_01", "Bancada Smart 4.0", "EM_PRODUCAO"));
    }

    @Test
    void buscarDashboard_deveTrazerIndicadorMaisRecente() {
        Maquina maquina = maquinaExemplo();
        IndicadorOEE indicador = new IndicadorOEE();
        indicador.setMaquina(maquina);
        indicador.setDisponibilidade(BigDecimal.valueOf(90));
        indicador.setPerformance(BigDecimal.valueOf(85));
        indicador.setQualidade(BigDecimal.valueOf(97.5));
        indicador.setOee(BigDecimal.valueOf(74.6));
        indicador.setCriadoEm(OffsetDateTime.parse("2026-08-17T10:00:00Z"));

        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
        when(indicadorOEERepository.findTopByMaquinaIdOrderByCriadoEmDesc(1L)).thenReturn(Optional.of(indicador));

        DashboardResponseDTO resultado = maquinaService.buscarDashboard(1L);

        assertThat(resultado.maquina().bancadaId()).isEqualTo("BANCADA_SMART_01");
        assertThat(resultado.indicadorAtual().oee()).isEqualTo(74.6);
        assertThat(resultado.indicadorAtual().calculadoEm()).hasToString("2026-08-17T10:00:00Z");
    }

    @Test
    void buscarDashboard_semIndicador_deveRetornarIndicadorNulo() {
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquinaExemplo()));
        when(indicadorOEERepository.findTopByMaquinaIdOrderByCriadoEmDesc(1L)).thenReturn(Optional.empty());

        DashboardResponseDTO resultado = maquinaService.buscarDashboard(1L);

        assertThat(resultado.maquina()).isNotNull();
        assertThat(resultado.indicadorAtual()).isNull();
    }

    @Test
    void buscarDashboard_deveLancarExcecao_quandoMaquinaNaoExiste() {
        when(maquinaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maquinaService.buscarDashboard(999L))
                .isInstanceOf(MaquinaNotFoundException.class)
                .hasMessageContaining("999");
    }
}
