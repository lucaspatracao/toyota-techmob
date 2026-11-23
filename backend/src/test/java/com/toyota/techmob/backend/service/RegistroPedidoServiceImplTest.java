package com.toyota.techmob.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.toyota.techmob.backend.coleta.CalculadoraOEE.ResultadoOEE;
import com.toyota.techmob.backend.coleta.DetectorPedidos.EtapaDetectada;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoDetectado;
import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.domain.Pedido;
import com.toyota.techmob.backend.domain.SituacaoPedido;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.LeituraBrutaRepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;
import com.toyota.techmob.backend.repository.PedidoRepository;

/** Gravação dos pedidos detectados (repositórios mockados). */
@ExtendWith(MockitoExtension.class)
class RegistroPedidoServiceImplTest {

    private static final Instant T0 = Instant.parse("2026-11-17T13:02:10Z");

    @Mock
    private MaquinaRepository maquinaRepository;
    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private IndicadorOEERepository indicadorOEERepository;
    @Mock
    private LeituraBrutaRepository leituraBrutaRepository;

    private RegistroPedidoService registro;
    private Maquina maquina;

    @BeforeEach
    void setUp() {
        registro = new RegistroPedidoServiceImpl(maquinaRepository, pedidoRepository, indicadorOEERepository,
                leituraBrutaRepository, Clock.fixed(T0.plusSeconds(100), ZoneOffset.UTC));
        maquina = Maquina.builder().id(1L).pecasPorPedido(3).statusOperacional("AGUARDANDO").build();
        when(maquinaRepository.findById(1L)).thenReturn(Optional.of(maquina));
    }

    /** O pedido do exemplo do plano: 100 s, 12 s parado na manipulação, padrão 80 s. */
    private PedidoDetectado exemplo(boolean reprovado) {
        return new PedidoDetectado(OrigemPedido.REPLAY, T0, T0.plusSeconds(100), 100, 12, reprovado, List.of(
                new EtapaDetectada(Estacao.ESTOQUE, T0, T0.plusSeconds(21), 21, 0, 20),
                new EtapaDetectada(Estacao.MANIPULACAO, T0.plusSeconds(21), T0.plusSeconds(68), 47, 12, 35),
                new EtapaDetectada(Estacao.SEPARACAO, T0.plusSeconds(68), T0.plusSeconds(94), 26, 0, 25)));
    }

    private Pedido pedidoEmAndamento() {
        return Pedido.builder().id(42L).maquina(maquina).numero(42).origem(OrigemPedido.REPLAY)
                .situacao(SituacaoPedido.EM_ANDAMENTO).reprovado(false).build();
    }

    @Test
    void iniciarPedido_gravaEmAndamentoComOProximoNumero() {
        when(pedidoRepository.ultimoNumero(1L)).thenReturn(41);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            p.setId(42L);
            return p;
        });

        RegistroPedidoService.PedidoRegistrado registrado = registro.iniciarPedido(1L, OrigemPedido.BANCADA, T0);

        assertThat(registrado).isEqualTo(new RegistroPedidoService.PedidoRegistrado(42L, 42));
        ArgumentCaptor<Pedido> salvo = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getSituacao()).isEqualTo(SituacaoPedido.EM_ANDAMENTO);
        assertThat(salvo.getValue().getInicio().toInstant()).isEqualTo(T0);
    }

    @Test
    void concluirPedido_gravaEtapasEIndicadorDeUmaVez() {
        Pedido pedido = pedidoEmAndamento();
        when(pedidoRepository.findById(42L)).thenReturn(Optional.of(pedido));

        ResultadoOEE resultado = registro.concluirPedido(1L, 42L, exemplo(false));

        assertThat(ResultadoOEE.percentual(resultado.oee())).isEqualByComparingTo("80.000");
        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.CONCLUIDO);
        assertThat(pedido.getLeadTimeS()).isEqualByComparingTo("100.000");
        assertThat(pedido.getTempoParadoS()).isEqualByComparingTo("12.000");
        assertThat(pedido.getPecasBoas()).isEqualTo(3);
        assertThat(pedido.getEtapas()).extracting(e -> e.getEstacao())
                .containsExactly(Estacao.ESTOQUE, Estacao.MANIPULACAO, Estacao.SEPARACAO);
        assertThat(pedido.getEtapas()).allMatch(e -> e.getPedido() == pedido);

        ArgumentCaptor<IndicadorOEE> indicador = ArgumentCaptor.forClass(IndicadorOEE.class);
        verify(indicadorOEERepository).save(indicador.capture());
        assertThat(indicador.getValue().getPedido()).isSameAs(pedido);
        assertThat(indicador.getValue().getDisponibilidade()).isEqualByComparingTo("88.000");
        assertThat(indicador.getValue().getPerformance()).isEqualByComparingTo("90.909");
        assertThat(indicador.getValue().getQualidade()).isEqualByComparingTo("100.000");
        assertThat(indicador.getValue().getOee()).isEqualByComparingTo("80.000");
        assertThat(indicador.getValue().getPeriodoFim().toInstant()).isEqualTo(T0.plusSeconds(100));
    }

    @Test
    void concluirPedidoReprovado_contaAsTresPecasComoDefeituosas() {
        Pedido pedido = pedidoEmAndamento();
        when(pedidoRepository.findById(42L)).thenReturn(Optional.of(pedido));

        registro.concluirPedido(1L, 42L, exemplo(true));

        assertThat(pedido.getReprovado()).isTrue();
        assertThat(pedido.getPecasBoas()).isZero();
        assertThat(pedido.getPecasDefeituosas()).isEqualTo(3);
        ArgumentCaptor<IndicadorOEE> indicador = ArgumentCaptor.forClass(IndicadorOEE.class);
        verify(indicadorOEERepository).save(indicador.capture());
        assertThat(indicador.getValue().getOee()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void concluirSemPedidoGravado_criaOPedidoNaHora() {
        when(pedidoRepository.ultimoNumero(1L)).thenReturn(7);

        registro.concluirPedido(1L, null, exemplo(false));

        ArgumentCaptor<Pedido> salvo = ArgumentCaptor.forClass(Pedido.class);
        verify(pedidoRepository).save(salvo.capture());
        assertThat(salvo.getValue().getNumero()).isEqualTo(8);
        assertThat(salvo.getValue().getSituacao()).isEqualTo(SituacaoPedido.CONCLUIDO);
    }

    @Test
    void interromperPedido_naoGravaIndicador() {
        Pedido pedido = pedidoEmAndamento();
        when(pedidoRepository.findById(42L)).thenReturn(Optional.of(pedido));

        registro.interromperPedido(1L, 42L, exemplo(false));

        assertThat(pedido.getSituacao()).isEqualTo(SituacaoPedido.INTERROMPIDO);
        assertThat(pedido.getEtapas()).hasSize(3);
        verify(indicadorOEERepository, never()).save(any());
    }

    @Test
    void atualizarMaquina_gravaStatusEUltimaLeitura() {
        registro.atualizarMaquina(1L, "PRODUZINDO", T0);

        assertThat(maquina.getStatusOperacional()).isEqualTo("PRODUZINDO");
        assertThat(maquina.getUltimaLeituraEm().toInstant()).isEqualTo(T0);
    }
}
