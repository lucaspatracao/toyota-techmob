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

import com.toyota.techmob.backend.domain.HistoricoProducao;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.dto.HistoricoProducaoDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.repository.HistoricoProducaoRepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

/**
 * Testes unitários do HistoricoProducaoServiceImpl.
 *
 * Repositórios são mockados (Mockito) — nenhum destes testes toca o banco
 * de verdade nem depende do Supabase estar acessível.
 *
 * Rodar com: ./mvnw test
 */
@ExtendWith(MockitoExtension.class)
class HistoricoProducaoServiceImplTest {

    @Mock
    private MaquinaRepository maquinaRepository;

    @Mock
    private HistoricoProducaoRepository historicoProducaoRepository;

    private HistoricoProducaoService historicoProducaoService;

    private Maquina criarMaquinaExemplo() {
        Maquina maquina = new Maquina();
        maquina.setId(1L);
        maquina.setIdentificadorBancada("BANCADA_SMART_01");
        maquina.setNome("Bancada Smart 4.0");
        maquina.setStatusOperacional("EM_PRODUCAO");
        return maquina;
    }

    private HistoricoProducao criarRegistroExemplo(Maquina maquina, String timestamp) {
        HistoricoProducao registro = new HistoricoProducao();
        registro.setMaquina(maquina);
        registro.setTimestamp(OffsetDateTime.parse(timestamp));
        registro.setStatusOperacional("EM_PRODUCAO");
        registro.setPecasBoas(128);
        registro.setPecasDefeituosas(3);
        registro.setTempoCicloSegundos(BigDecimal.valueOf(12.5));
        registro.setCriadoEm(OffsetDateTime.parse(timestamp));
        return registro;
    }

    @Test
    void historico_deveRetornarListaDeRegistrosMapeadaParaDTO_quandoMaquinaExiste() {
        historicoProducaoService = new HistoricoProducaoServiceImpl(maquinaRepository, historicoProducaoRepository);
        Maquina maquina = criarMaquinaExemplo();

        when(maquinaRepository.existsById(1L)).thenReturn(true);
        when(historicoProducaoRepository.findByMaquinaIdOrderByTimestampDesc(1L))
                .thenReturn(List.of(
                        criarRegistroExemplo(maquina, "2026-08-18T14:32:10Z"),
                        criarRegistroExemplo(maquina, "2026-08-18T14:31:57Z")));

        List<HistoricoProducaoDTO> resultado = historicoProducaoService.historico(1L);

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getMaquinaId()).isEqualTo(1L);
        assertThat(resultado.get(0).getPecasBoas()).isEqualTo(128);
        assertThat(resultado.get(0).getTempoCicloSegundos()).isEqualByComparingTo(BigDecimal.valueOf(12.5));
    }

    @Test
    void historico_deveRetornarListaVazia_quandoMaquinaExisteMasNaoTemRegistros() {
        historicoProducaoService = new HistoricoProducaoServiceImpl(maquinaRepository, historicoProducaoRepository);

        when(maquinaRepository.existsById(1L)).thenReturn(true);
        when(historicoProducaoRepository.findByMaquinaIdOrderByTimestampDesc(1L))
                .thenReturn(List.of());

        List<HistoricoProducaoDTO> resultado = historicoProducaoService.historico(1L);

        assertThat(resultado).isEmpty();
    }

    @Test
    void historico_deveLancarExcecao_quandoMaquinaNaoExiste() {
        historicoProducaoService = new HistoricoProducaoServiceImpl(maquinaRepository, historicoProducaoRepository);

        when(maquinaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> historicoProducaoService.historico(999L))
                .isInstanceOf(MaquinaNotFoundException.class);
    }
}