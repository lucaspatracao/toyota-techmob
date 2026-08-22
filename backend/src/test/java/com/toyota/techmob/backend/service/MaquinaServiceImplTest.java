package com.toyota.techmob.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.dto.IndicadorOEEDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

/**
 * Testes unitários do IndicadorOEEServiceImpl.
 *
 * Repositórios são mockados (Mockito) — nenhum destes testes toca o banco
 * de verdade nem depende do Supabase estar acessível.
 *
 * Rodar com: ./mvnw test
 */
@ExtendWith(MockitoExtension.class)
class IndicadorOEEServiceImplTest {

    @Mock
    private MaquinaRepository maquinaRepository;

    @Mock
    private IndicadorOEERepository indicadorOEERepository;

    private IndicadorOEEService indicadorOEEService;

    private Maquina criarMaquinaExemplo() {
        Maquina maquina = new Maquina();
        maquina.setId(1L);
        maquina.setIdentificadorBancada("BANCADA_SMART_01");
        maquina.setNome("Bancada Smart 4.0");
        maquina.setStatusOperacional("EM_PRODUCAO");
        return maquina;
    }

    private IndicadorOEE criarIndicadorExemplo(Maquina maquina, String criadoEm) {
        IndicadorOEE indicador = new IndicadorOEE();
        indicador.setMaquina(maquina);
        indicador.setDisponibilidade(BigDecimal.valueOf(85.0));
        indicador.setPerformance(BigDecimal.valueOf(90.0));
        indicador.setQualidade(BigDecimal.valueOf(97.5));
        indicador.setOee(BigDecimal.valueOf(74.6));
        indicador.setCriadoEm(OffsetDateTime.parse(criadoEm));
        return indicador;
    }

    @Test
    void historico_deveRetornarListaDeIndicadoresMapeadaParaDTO_quandoMaquinaExiste() {
        indicadorOEEService = new IndicadorOEEServiceImpl(maquinaRepository, indicadorOEERepository);
        Maquina maquina = criarMaquinaExemplo();

        when(maquinaRepository.existsById(1L)).thenReturn(true);
        when(indicadorOEERepository.findByMaquinaIdOrderByPeriodoInicioDesc(1L))
                .thenReturn(List.of(
                        criarIndicadorExemplo(maquina, "2026-08-18T10:00:00Z"),
                        criarIndicadorExemplo(maquina, "2026-08-17T10:00:00Z")));

        List<IndicadorOEEDTO> resultado = indicadorOEEService.historico(1L);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getMaquinaId()).isEqualTo(1L);
        assertThat(resultado.get(0).getOee()).isEqualTo(74.6);
    }

    @Test
    void historico_deveRetornarListaVazia_quandoMaquinaExisteMasNaoTemIndicadores() {
        indicadorOEEService = new IndicadorOEEServiceImpl(maquinaRepository, indicadorOEERepository);

        when(maquinaRepository.existsById(1L)).thenReturn(true);
        when(indicadorOEERepository.findByMaquinaIdOrderByPeriodoInicioDesc(1L))
                .thenReturn(List.of());

        List<IndicadorOEEDTO> resultado = indicadorOEEService.historico(1L);

        assertThat(resultado).isEmpty();
    }

    @Test
    void historico_deveLancarExcecao_quandoMaquinaNaoExiste() {
        indicadorOEEService = new IndicadorOEEServiceImpl(maquinaRepository, indicadorOEERepository);

        when(maquinaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> indicadorOEEService.historico(999L))
                .isInstanceOf(MaquinaNotFoundException.class);
    }
}