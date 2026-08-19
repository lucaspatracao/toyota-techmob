package com.toyota.techmob.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toyota.techmob.backend.dto.DashboardResponseDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.model.IndicadorOEE;
import com.toyota.techmob.backend.model.Maquina;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

/**
 * Testes unitários do MaquinaServiceImpl.
 *
 * Repositórios são mockados (Mockito) — nenhum destes testes toca o banco
 * de verdade nem depende do Supabase estar acessível. Isso permite validar
 * a lógica de negócio (o que acontece quando a máquina não existe, ou
 * quando ainda não há indicador calculado) independente do bloqueio de
 * rede que a equipe está enfrentando.
 *
 * Rodar com: ./mvnw test
 */
@ExtendWith(MockitoExtension.class)
class MaquinaServiceImplTest {

    @Mock
    private MaquinaRepository maquinaRepository;

    @Mock
    private IndicadorOEERepository indicadorOEERepository;

    private MaquinaService maquinaService;

    private Maquina criarMaquinaExemplo() {
        Maquina maquina = new Maquina();
        maquina.setId(1L);
        maquina.setBancadaId("BANCADA_SMART_01");
        maquina.setNome("Bancada Smart 4.0");
        maquina.setStatusAtual("EM_PRODUCAO");
        return maquina;
    }

    private IndicadorOEE criarIndicadorExemplo() {
        IndicadorOEE indicador = new IndicadorOEE();
        indicador.setMaquinaId(1L);
        indicador.setDisponibilidade(85.0);
        indicador.setPerformance(90.0);
        indicador.setQualidade(97.5);
        indicador.setOee(74.6);
        indicador.setCalculadoEm(Instant.parse("2026-08-18T10:00:00Z"));
        return indicador;
    }

    @Test
    void listarTodas_deveRetornarListaDeMaquinasMapeadaParaDTO() {
        maquinaService = new MaquinaServiceImpl(maquinaRepository, indicadorOEERepository);
        when(maquinaRepository.findAll()).thenReturn(List.of(criarMaquinaExemplo()));

        List<MaquinaDTO> resultado = maquinaService.listarTodas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getBancadaId()).isEqualTo("BANCADA_SMART_01");
    }

    @Test
    void buscarDashboard_deveRetornarMaquinaEIndicador_quandoAmbosExistem() {
        maquinaService = new MaquinaServiceImpl(maquinaRepository, indicadorOEERepository);
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(criarMaquinaExemplo()));
        when(indicadorOEERepository.findTopByMaquinaIdOrderByCalculadoEmDesc(1L))
                .thenReturn(Optional.of(criarIndicadorExemplo()));

        DashboardResponseDTO resultado = maquinaService.buscarDashboard(1L);

        assertThat(resultado.getMaquina().getBancadaId()).isEqualTo("BANCADA_SMART_01");
        assertThat(resultado.getIndicadorAtual().getOee()).isEqualTo(74.6);
    }

    @Test
    void buscarDashboard_deveRetornarIndicadorNulo_quandoAindaNaoHaCalculoDeOEE() {
        maquinaService = new MaquinaServiceImpl(maquinaRepository, indicadorOEERepository);
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(criarMaquinaExemplo()));
        when(indicadorOEERepository.findTopByMaquinaIdOrderByCalculadoEmDesc(1L))
                .thenReturn(Optional.empty());

        DashboardResponseDTO resultado = maquinaService.buscarDashboard(1L);

        assertThat(resultado.getMaquina()).isNotNull();
        assertThat(resultado.getIndicadorAtual()).isNull();
    }

    @Test
    void buscarDashboard_deveLancarExcecao_quandoMaquinaNaoExiste() {
        maquinaService = new MaquinaServiceImpl(maquinaRepository, indicadorOEERepository);
        when(maquinaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maquinaService.buscarDashboard(999L))
                .isInstanceOf(MaquinaNotFoundException.class);
    }
}