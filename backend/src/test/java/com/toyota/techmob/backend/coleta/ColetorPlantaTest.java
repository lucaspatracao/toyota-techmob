package com.toyota.techmob.backend.coleta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.toyota.techmob.backend.coleta.CalculadoraOEE.ResultadoOEE;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoDetectado;
import com.toyota.techmob.backend.coleta.EstadoPlantaAtual.Estado;
import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.service.RegistroPedidoService;

/**
 * O laço da coleta com o cliente e o serviço de registro falsos: a
 * gravação do exemplo do plano passa pelo coletor e vira um pedido gravado.
 */
class ColetorPlantaTest {

    /** Relógio que o teste avança a cada ciclo. */
    private static final class RelogioManual extends Clock {
        private Instant agora = Gravacao.BASE;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final PlantaClient cliente = mock(PlantaClient.class);
    private final RegistroPedidoService registro = mock(RegistroPedidoService.class);
    private final EstadoPlantaAtual estado = new EstadoPlantaAtual();
    private final RelogioManual relogio = new RelogioManual();
    private ColetorPlanta coletor;

    @BeforeEach
    void setUp() {
        coletor = new ColetorPlanta(cliente, registro, estado, Gravacao.propriedades(), relogio);
        when(registro.iniciarPedido(eq(1L), any(), any())).thenReturn(new RegistroPedidoService.PedidoRegistrado(42L, 42));
        when(registro.concluirPedido(eq(1L), eq(42L), any())).thenReturn(new ResultadoOEE(0.88, 80 / 88.0, 1));
    }

    /** Roda um ciclo no instante t (s) com a leitura dada (null = falha). */
    private void ciclo(double t, LeituraPlanta leitura) {
        relogio.agora = Gravacao.instante(t);
        when(cliente.ler()).thenReturn(leitura == null ? Optional.empty()
                : Optional.of(new PlantaClient.Resultado(leitura, "{}", OrigemPedido.REPLAY)));
        coletor.ciclo();
    }

    @Test
    void gravacaoDoExemplo_viraUmPedidoGravadoComOee() {
        List<Gravacao.Registro> leituras = Gravacao.ler("exemplo_80.jsonl").stream()
                .filter(r -> "leitura".equals(r.tipo())).toList();
        LeituraPlanta atual = null;
        int proxima = 0;
        for (double t = 0.5; t <= 115.5; t++) {
            while (proxima < leituras.size() && leituras.get(proxima).t() <= t) {
                atual = leituras.get(proxima++).leitura();
            }
            ciclo(t, atual);
            if (t == 40.5) {
                // No meio do pedido: estado ao vivo mostra o pedido 42 na manipulação.
                Estado meio = estado.da(1L);
                assertThat(meio.status()).isEqualTo(StatusMaquina.PRODUZINDO);
                assertThat(meio.pedido().numero()).isEqualTo(42);
                assertThat(meio.pedido().etapaAtual()).isEqualTo(Estacao.MANIPULACAO);
                assertThat(meio.fonte()).isEqualTo(OrigemPedido.REPLAY);
            }
        }

        ArgumentCaptor<PedidoDetectado> concluido = ArgumentCaptor.forClass(PedidoDetectado.class);
        verify(registro).iniciarPedido(1L, OrigemPedido.REPLAY, Gravacao.instante(10.5));
        verify(registro).concluirPedido(eq(1L), eq(42L), concluido.capture());
        assertThat(concluido.getValue().leadTimeS()).isEqualTo(100.0);
        assertThat(concluido.getValue().tempoParadoS()).isEqualTo(12.0);
        // Só as 8 leituras que mudaram vão para a tabela leitura_planta.
        verify(registro, times(8)).registrarLeitura(eq(1L), any(), anyString());
        Estado fim = estado.da(1L);
        assertThat(fim.status()).isEqualTo(StatusMaquina.AGUARDANDO);
        assertThat(fim.pedido()).isNull();
    }

    @Test
    void semResposta_estadoFicaSemConexao() {
        ciclo(0, null);

        Estado atual = estado.da(1L);
        assertThat(atual.conectado()).isFalse();
        assertThat(atual.status()).isEqualTo(StatusMaquina.SEM_CONEXAO);
        assertThat(atual.estacoes()).allMatch(e -> e.situacao() == SituacaoEstacao.SEM_CONEXAO);
        verify(registro).atualizarMaquina(1L, "SEM_CONEXAO", null);
    }

    @Test
    void erroAoGravar_naoDerrubaOLaco() {
        when(registro.iniciarPedido(eq(1L), any(), any())).thenThrow(new IllegalStateException("banco fora do ar"));
        LeituraPlanta aguardando = new LeituraPlanta("AGUARDANDO", "AGUARDANDO", "AGUARDANDO", "AGUARDANDO", 1, 0, 0, 0, 0, 0.0, 0.0);
        LeituraPlanta produzindo = new LeituraPlanta("PRODUZINDO", "AGUARDANDO", "AGUARDANDO", "PRODUZINDO", 0, 1, 0, 0, 0, 0.0, 0.0);

        ciclo(0, aguardando);
        ciclo(1, produzindo);

        Estado atual = estado.da(1L);
        assertThat(atual.pedido()).isNotNull();
        assertThat(atual.pedido().pedidoId()).isNull(); // o início não foi gravado; o fim cria o pedido
    }
}
