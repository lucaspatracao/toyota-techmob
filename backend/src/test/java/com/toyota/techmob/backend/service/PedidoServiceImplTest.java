package com.toyota.techmob.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.EtapaPedido;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.domain.Pedido;
import com.toyota.techmob.backend.domain.SituacaoPedido;
import com.toyota.techmob.backend.dto.PedidoDTO;
import com.toyota.techmob.backend.dto.ResumoPedidosDTO;
import com.toyota.techmob.backend.exception.MaquinaNotFoundException;
import com.toyota.techmob.backend.exception.PedidoNotFoundException;
import com.toyota.techmob.backend.repository.MaquinaRepository;
import com.toyota.techmob.backend.repository.PedidoRepository;

/** Consulta e resumo dos pedidos (repositórios mockados). */
@ExtendWith(MockitoExtension.class)
class PedidoServiceImplTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-17T13:00:00Z");

    @Mock
    private MaquinaRepository maquinaRepository;
    @Mock
    private PedidoRepository pedidoRepository;

    private PedidoService pedidoService;
    private final Maquina maquina = Maquina.builder().id(1L).build();

    @BeforeEach
    void setUp() {
        pedidoService = new PedidoServiceImpl(maquinaRepository, pedidoRepository);
    }

    /** Pedido concluído com uma etapa só, que carrega o tempo padrão inteiro. */
    private Pedido concluido(int numero, double leadTime, double parado, double padrao, boolean reprovado) {
        Pedido pedido = Pedido.builder()
                .id((long) numero)
                .maquina(maquina)
                .numero(numero)
                .origem(OrigemPedido.REPLAY)
                .situacao(SituacaoPedido.CONCLUIDO)
                .inicio(T0)
                .fim(T0.plusSeconds((long) leadTime))
                .leadTimeS(BigDecimal.valueOf(leadTime))
                .tempoParadoS(BigDecimal.valueOf(parado))
                .reprovado(reprovado)
                .pecasBoas(reprovado ? 0 : 3)
                .pecasDefeituosas(reprovado ? 3 : 0)
                .build();
        pedido.adicionarEtapa(EtapaPedido.builder()
                .estacao(Estacao.MANIPULACAO)
                .inicio(T0)
                .fim(T0.plusSeconds((long) leadTime))
                .duracaoS(BigDecimal.valueOf(leadTime))
                .tempoParadoS(BigDecimal.valueOf(parado))
                .tempoPadraoS(BigDecimal.valueOf(padrao))
                .build());
        return pedido;
    }

    private Pedido comSituacao(Pedido pedido, SituacaoPedido situacao) {
        pedido.setSituacao(situacao);
        return pedido;
    }

    @Test
    void listar_repassaOsFiltrosEmUtcEConverteParaDTO() {
        Instant de = Instant.parse("2026-11-17T03:00:00Z");
        Instant ate = Instant.parse("2026-11-18T03:00:00Z");
        when(maquinaRepository.existsById(1L)).thenReturn(true);
        when(pedidoRepository.buscarPorPeriodo(1L, OffsetDateTime.parse("2026-11-17T03:00:00Z"),
                OffsetDateTime.parse("2026-11-18T03:00:00Z"), OrigemPedido.REPLAY))
                .thenReturn(List.of(concluido(42, 100, 12, 80, false)));

        List<PedidoDTO> pedidos = pedidoService.listar(1L, de, ate, OrigemPedido.REPLAY);

        assertThat(pedidos).extracting(PedidoDTO::numero).containsExactly(42);
        assertThat(pedidos.getFirst().tempoPadraoS()).isEqualTo(80.0);
    }

    @Test
    void listar_semFiltros_buscaTodosOsPedidos() {
        when(maquinaRepository.existsById(1L)).thenReturn(true);
        when(pedidoRepository.buscarPorPeriodo(1L, null, null, null)).thenReturn(List.of());

        assertThat(pedidoService.listar(1L, null, null, null)).isEmpty();
        verify(pedidoRepository).buscarPorPeriodo(1L, null, null, null);
    }

    @Test
    void listar_maquinaInexistente_lancaExcecao() {
        when(maquinaRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> pedidoService.listar(999L, null, null, null))
                .isInstanceOf(MaquinaNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void detalhe_pedidoInexistente_lancaExcecao() {
        when(pedidoRepository.findDetalheById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pedidoService.detalhe(999L))
                .isInstanceOf(PedidoNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void detalhe_trazAsEtapas() {
        when(pedidoRepository.findDetalheById(42L)).thenReturn(Optional.of(concluido(42, 100, 12, 80, false)));

        assertThat(pedidoService.detalhe(42L).etapas()).hasSize(1);
    }

    @Test
    void resumir_somaOsTemposEmVezDeTirarAMediaDosOees() {
        // Pedido 1: 100 s, 12 s parado, padrão 80 s -> OEE 80%.
        // Pedido 2: 400 s, sem parada, padrão 80 s -> OEE 20%.
        // A média dos OEEs daria 50%; somando os tempos: 160 / 500 = 32%.
        when(maquinaRepository.existsById(1L)).thenReturn(true);
        when(pedidoRepository.buscarPorPeriodo(1L, null, null, null)).thenReturn(List.of(
                concluido(2, 400, 0, 80, false),
                concluido(1, 100, 12, 80, false)));

        ResumoPedidosDTO resumo = pedidoService.resumir(1L, null, null, null);

        assertThat(resumo.oee()).isEqualTo(32.0);
        assertThat(resumo.disponibilidade()).isEqualTo(97.6);
        assertThat(resumo.performance()).isEqualTo(32.787);
        assertThat(resumo.qualidade()).isEqualTo(100.0);
        assertThat(resumo.leadTimeMedioS()).isEqualTo(250.0);
        assertThat(resumo.tempoParadoTotalS()).isEqualTo(12.0);
    }

    @Test
    void resumir_contaReprovadosEInterrompidosAParte() {
        when(maquinaRepository.existsById(1L)).thenReturn(true);
        when(pedidoRepository.buscarPorPeriodo(1L, null, null, null)).thenReturn(List.of(
                comSituacao(concluido(4, 30, 0, 55, false), SituacaoPedido.EM_ANDAMENTO),
                comSituacao(concluido(3, 38, 0, 55, false), SituacaoPedido.INTERROMPIDO),
                concluido(2, 90, 0, 80, true),
                concluido(1, 100, 12, 80, false)));

        ResumoPedidosDTO resumo = pedidoService.resumir(1L, null, null, null);

        assertThat(resumo.concluidos()).isEqualTo(2);
        assertThat(resumo.aprovados()).isEqualTo(1);
        assertThat(resumo.reprovados()).isEqualTo(1);
        assertThat(resumo.interrompidos()).isEqualTo(1);
        assertThat(resumo.pecasProduzidas()).isEqualTo(6);
        assertThat(resumo.taxaReprovacao()).isEqualTo(50.0);
        assertThat(resumo.leadTimeMedioS()).isEqualTo(95.0);
        // Q = aprovados / concluídos; D e P somam os tempos dos dois concluídos.
        assertThat(resumo.qualidade()).isEqualTo(50.0);
        assertThat(resumo.disponibilidade()).isEqualTo(93.684);
        assertThat(resumo.performance()).isEqualTo(89.888);
        assertThat(resumo.oee()).isEqualTo(42.105);
    }

    @Test
    void resumir_semPedidoConcluido_vemComMediasEOeeNulos() {
        when(maquinaRepository.existsById(1L)).thenReturn(true);
        when(pedidoRepository.buscarPorPeriodo(1L, null, null, null)).thenReturn(List.of(
                comSituacao(concluido(1, 38, 0, 55, false), SituacaoPedido.INTERROMPIDO)));

        ResumoPedidosDTO resumo = pedidoService.resumir(1L, null, null, null);

        assertThat(resumo).isEqualTo(new ResumoPedidosDTO(0, 0, 0, 1, 0, null, null, 0.0, null, null, null, null));
    }
}
