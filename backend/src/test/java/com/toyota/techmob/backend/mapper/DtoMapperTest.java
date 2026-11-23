package com.toyota.techmob.backend.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.toyota.techmob.backend.coleta.EstadoPlantaAtual.EstacaoAtual;
import com.toyota.techmob.backend.coleta.EstadoPlantaAtual.Estado;
import com.toyota.techmob.backend.coleta.EstadoPlantaAtual.PedidoAtual;
import com.toyota.techmob.backend.coleta.SituacaoEstacao;
import com.toyota.techmob.backend.coleta.StatusMaquina;
import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.EtapaPedido;
import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.domain.Pedido;
import com.toyota.techmob.backend.domain.SituacaoPedido;
import com.toyota.techmob.backend.dto.PedidoDTO;
import com.toyota.techmob.backend.dto.PedidoDetalheDTO;
import com.toyota.techmob.backend.dto.StatusPlantaDTO;

/** Conversão de Pedido, EtapaPedido e do estado ao vivo para os DTOs da API. */
class DtoMapperTest {

    private static final OffsetDateTime T0 = OffsetDateTime.parse("2026-11-17T10:00:00Z");

    private static final Map<Estacao, Double> PADRAO = Map.of(
            Estacao.ESTOQUE, 20.0, Estacao.MANIPULACAO, 35.0, Estacao.SEPARACAO, 25.0);

    private Pedido pedido() {
        Maquina maquina = new Maquina();
        maquina.setId(1L);
        Pedido pedido = Pedido.builder()
                .id(7L)
                .maquina(maquina)
                .numero(3)
                .origem(OrigemPedido.REPLAY)
                .situacao(SituacaoPedido.CONCLUIDO)
                .inicio(T0)
                .fim(T0.plusSeconds(100))
                .leadTimeS(new BigDecimal("100.000"))
                .tempoParadoS(new BigDecimal("12.000"))
                .reprovado(false)
                .pecasBoas(3)
                .pecasDefeituosas(0)
                .build();
        // Fora de ordem de propósito: o DTO ordena por estação e por início.
        pedido.adicionarEtapa(etapa(Estacao.SEPARACAO, 70, 95, "25.000", "25"));
        pedido.adicionarEtapa(etapa(Estacao.ESTOQUE, 0, 30, "30.000", "20"));
        pedido.adicionarEtapa(etapa(Estacao.MANIPULACAO, 30, 70, "40.000", "35"));
        return pedido;
    }

    private EtapaPedido etapa(Estacao estacao, int inicio, int fim, String duracao, String padrao) {
        return EtapaPedido.builder()
                .estacao(estacao)
                .inicio(T0.plusSeconds(inicio))
                .fim(T0.plusSeconds(fim))
                .duracaoS(new BigDecimal(duracao))
                .tempoParadoS(BigDecimal.ZERO)
                .tempoPadraoS(new BigDecimal(padrao))
                .build();
    }

    @Test
    void pedidoSemIndicador_temTempoPadraoSomadoEOeeNulo() {
        PedidoDTO dto = DtoMapper.paraDTO(pedido());

        assertThat(dto.tempoPadraoS()).isEqualTo(80.0);
        assertThat(dto.duracaoEtapasS()).containsExactly(
                entry(Estacao.ESTOQUE, 30.0),
                entry(Estacao.MANIPULACAO, 40.0),
                entry(Estacao.SEPARACAO, 25.0));
        assertThat(dto.oee()).isNull();
        assertThat(dto.inicio()).hasToString("2026-11-17T10:00:00Z");
    }

    @Test
    void pedidoComIndicador_trazOsQuatroPercentuais() {
        Pedido pedido = pedido();
        pedido.setIndicador(IndicadorOEE.builder()
                .disponibilidade(new BigDecimal("88.000"))
                .performance(new BigDecimal("90.909"))
                .qualidade(new BigDecimal("100.000"))
                .oee(new BigDecimal("80.000"))
                .build());

        PedidoDTO dto = DtoMapper.paraDTO(pedido);

        assertThat(dto.disponibilidade()).isEqualTo(88.0);
        assertThat(dto.performance()).isEqualTo(90.909);
        assertThat(dto.oee()).isEqualTo(80.0);
    }

    @Test
    void detalhe_listaAsEtapasPorOrdemDeInicio() {
        PedidoDetalheDTO dto = DtoMapper.paraDetalheDTO(pedido());

        assertThat(dto.pedido().numero()).isEqualTo(3);
        assertThat(dto.etapas()).extracting(e -> e.estacao())
                .containsExactly(Estacao.ESTOQUE, Estacao.MANIPULACAO, Estacao.SEPARACAO);
        assertThat(dto.etapas().get(1).tempoPadraoS()).isEqualTo(35.0);
    }

    @Test
    void statusPlanta_semConexao_temEstacoesSemConexaoESemPedido() {
        StatusPlantaDTO dto = DtoMapper.paraDTO(Estado.semConexao(1L), PADRAO, T0.toInstant());

        assertThat(dto.status()).isEqualTo("SEM_CONEXAO");
        assertThat(dto.conectado()).isFalse();
        assertThat(dto.fonte()).isNull();
        assertThat(dto.estacoes()).extracting(StatusPlantaDTO.EstacaoStatus::situacao)
                .containsOnly("SEM_CONEXAO");
        assertThat(dto.pedidoEmAndamento()).isNull();
        assertThat(dto.tempoPadraoPedidoS()).isEqualTo(80.0);
        assertThat(dto.ultimaLeituraEm()).isNull();
    }

    @Test
    void statusPlanta_comPedidoAberto_calculaODecorridoNaHoraDaConsulta() {
        Instant inicio = Instant.parse("2026-11-17T13:04:40.102Z");
        Instant ultimaLeitura = Instant.parse("2026-11-17T13:05:12.000Z");
        Estado estado = new Estado(1L, true, OrigemPedido.REPLAY, StatusMaquina.PRODUZINDO, "PRODUZINDO",
                List.of(new EstacaoAtual(Estacao.ESTOQUE, "AGUARDANDO", SituacaoEstacao.AGUARDANDO),
                        new EstacaoAtual(Estacao.MANIPULACAO, "PRODUZINDO", SituacaoEstacao.OPERANDO),
                        new EstacaoAtual(Estacao.SEPARACAO, "AGUARDANDO", SituacaoEstacao.AGUARDANDO)),
                new PedidoAtual(43L, 43, inicio, Estacao.MANIPULACAO), ultimaLeitura);

        StatusPlantaDTO dto = DtoMapper.paraDTO(estado, PADRAO, Instant.parse("2026-11-17T13:05:12.402Z"));

        assertThat(dto.status()).isEqualTo("PRODUZINDO");
        assertThat(dto.fonte()).isEqualTo(OrigemPedido.REPLAY);
        assertThat(dto.estacoes()).containsExactly(
                new StatusPlantaDTO.EstacaoStatus(Estacao.ESTOQUE, "AGUARDANDO", "AGUARDANDO", 20.0),
                new StatusPlantaDTO.EstacaoStatus(Estacao.MANIPULACAO, "PRODUZINDO", "OPERANDO", 35.0),
                new StatusPlantaDTO.EstacaoStatus(Estacao.SEPARACAO, "AGUARDANDO", "AGUARDANDO", 25.0));
        assertThat(dto.pedidoEmAndamento()).isEqualTo(
                new StatusPlantaDTO.PedidoEmAndamento(43L, 43, inicio, 32.3, Estacao.MANIPULACAO));
        assertThat(dto.ultimaLeituraEm()).isEqualTo(ultimaLeitura);
    }
}
