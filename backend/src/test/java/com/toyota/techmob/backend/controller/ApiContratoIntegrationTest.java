package com.toyota.techmob.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.toyota.techmob.backend.coleta.EstadoPlantaAtual;
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
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;
import com.toyota.techmob.backend.repository.PedidoRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Teste de integração: sobe a aplicação inteira com um banco H2 em memória
 * e chama os endpoints via HTTP.
 *
 * Garante o contrato JSON que o frontend consome: se algum campo mudar de
 * nome, sumir ou mudar de formato, este teste falha.
 *
 * O relógio é fixo em 17/08/2026, 12:00 em Brasília. Pedidos gravados:
 * 1 (ontem, bancada, OEE 100%), 2 (o exemplo do plano: 100 s, 12 s parado,
 * OEE 80%), 3 (reprovado, OEE 0%) e 4 (interrompido, sem OEE).
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:techmob;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=false",
                "techmob.coleta.habilitada=false"
        })
class ApiContratoIntegrationTest {

    /** 17/08/2026 às 12:00 em Brasília: "hoje" vai de 03:00 UTC do dia 17 a 03:00 UTC do dia 18. */
    static final Instant AGORA = Instant.parse("2026-08-17T15:00:00Z");

    @TestConfiguration
    static class RelogioFixo {
        @Bean
        @Primary
        Clock relogioDoTeste() {
            return Clock.fixed(AGORA, ZoneId.of("America/Sao_Paulo"));
        }
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private MaquinaRepository maquinaRepository;

    @Autowired
    private IndicadorOEERepository indicadorOEERepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    @Autowired
    private EstadoPlantaAtual estadoPlanta;

    private final HttpClient http = HttpClient.newHttpClient();

    private Long maquinaComDadosId;
    private Long maquinaSemDadosId;
    private Long pedido1Id;
    private Long pedido2Id;
    private Long pedido3Id;
    private Long pedido4Id;

    @BeforeEach
    void popularBanco() {
        indicadorOEERepository.deleteAll();
        pedidoRepository.deleteAll();
        maquinaRepository.deleteAll();

        OffsetDateTime t0 = OffsetDateTime.parse("2026-08-01T08:00:00Z");

        Maquina bancada = maquinaRepository.save(Maquina.builder()
                .identificadorBancada("BANCADA_SMART_01")
                .nome("Bancada Smart 4.0")
                .statusOperacional("PRODUZINDO")
                .tempoPadraoPedidoS(new BigDecimal("80.000"))
                .pecasPorPedido(3)
                .ultimaLeituraEm(OffsetDateTime.parse("2026-08-17T14:59:59.500Z"))
                .criadoEm(t0)
                .atualizadoEm(t0)
                .build());
        maquinaComDadosId = bancada.getId();

        maquinaSemDadosId = maquinaRepository.save(Maquina.builder()
                .identificadorBancada("BANCADA_SMART_02")
                .nome("Bancada Reserva")
                .statusOperacional("SEM_CONEXAO")
                .tempoPadraoPedidoS(new BigDecimal("80.000"))
                .pecasPorPedido(3)
                .criadoEm(t0)
                .atualizadoEm(t0)
                .build()).getId();

        // Ontem: tempos exatamente no padrão.
        Pedido pedido1 = pedido(bancada, 1, OrigemPedido.BANCADA, SituacaoPedido.CONCLUIDO,
                "2026-08-16T12:00:00Z", "80", "0", false);
        etapa(pedido1, Estacao.ESTOQUE, 0, 20, "0", "20");
        etapa(pedido1, Estacao.MANIPULACAO, 20, 35, "0", "35");
        etapa(pedido1, Estacao.SEPARACAO, 55, 25, "0", "25");
        pedido1Id = pedidoRepository.save(pedido1).getId();
        indicadorOEERepository.save(indicador(pedido1, "100.000", "100.000", "100.000", "100.000"));

        // O exemplo do plano: 100 s, 12 s parado na manipulação, padrão 80 s.
        Pedido pedido2 = pedido(bancada, 2, OrigemPedido.SINTETICO, SituacaoPedido.CONCLUIDO,
                "2026-08-17T12:00:00Z", "100", "12", false);
        etapa(pedido2, Estacao.ESTOQUE, 0, 21, "0", "20");
        etapa(pedido2, Estacao.MANIPULACAO, 21, 47, "12", "35");
        etapa(pedido2, Estacao.SEPARACAO, 68, 26, "0", "25");
        pedido2Id = pedidoRepository.save(pedido2).getId();
        indicadorOEERepository.save(indicador(pedido2, "88.000", "90.909", "100.000", "80.000"));

        Pedido pedido3 = pedido(bancada, 3, OrigemPedido.REPLAY, SituacaoPedido.CONCLUIDO,
                "2026-08-17T13:00:00Z", "90", "0", true);
        etapa(pedido3, Estacao.ESTOQUE, 0, 20, "0", "20");
        etapa(pedido3, Estacao.MANIPULACAO, 20, 40, "0", "35");
        etapa(pedido3, Estacao.SEPARACAO, 60, 25, "0", "25");
        pedido3Id = pedidoRepository.save(pedido3).getId();
        indicadorOEERepository.save(indicador(pedido3, "100.000", "88.889", "0.000", "0.000"));

        // Bancada caiu no meio da manipulação: sem OEE.
        Pedido pedido4 = pedido(bancada, 4, OrigemPedido.REPLAY, SituacaoPedido.INTERROMPIDO,
                "2026-08-17T14:00:00Z", "38", "0", false);
        etapa(pedido4, Estacao.ESTOQUE, 0, 20, "0", "20");
        etapa(pedido4, Estacao.MANIPULACAO, 20, 18, "0", "35");
        pedido4Id = pedidoRepository.save(pedido4).getId();

        // Coleta desligada nos testes: estado ao vivo começa sem conexão.
        estadoPlanta.atualizar(Estado.semConexao(maquinaComDadosId));
    }

    private Pedido pedido(Maquina maquina, int numero, OrigemPedido origem, SituacaoPedido situacao,
                          String inicio, String leadTime, String parado, boolean reprovado) {
        OffsetDateTime comeco = OffsetDateTime.parse(inicio);
        boolean concluido = situacao == SituacaoPedido.CONCLUIDO;
        return Pedido.builder()
                .maquina(maquina)
                .numero(numero)
                .origem(origem)
                .situacao(situacao)
                .inicio(comeco)
                .fim(comeco.plusSeconds(Long.parseLong(leadTime)))
                .leadTimeS(new BigDecimal(leadTime))
                .tempoParadoS(new BigDecimal(parado))
                .reprovado(reprovado)
                .pecasBoas(concluido && !reprovado ? 3 : 0)
                .pecasDefeituosas(concluido && reprovado ? 3 : 0)
                .criadoEm(comeco)
                .build();
    }

    private void etapa(Pedido pedido, Estacao estacao, int inicioS, int duracaoS, String parado, String padrao) {
        pedido.adicionarEtapa(EtapaPedido.builder()
                .estacao(estacao)
                .inicio(pedido.getInicio().plusSeconds(inicioS))
                .fim(pedido.getInicio().plusSeconds(inicioS + duracaoS))
                .duracaoS(BigDecimal.valueOf(duracaoS))
                .tempoParadoS(new BigDecimal(parado))
                .tempoPadraoS(new BigDecimal(padrao))
                .build());
    }

    private IndicadorOEE indicador(Pedido pedido, String d, String p, String q, String oee) {
        return IndicadorOEE.builder()
                .maquina(pedido.getMaquina())
                .pedido(pedido)
                .periodoInicio(pedido.getInicio())
                .periodoFim(pedido.getFim())
                .disponibilidade(new BigDecimal(d))
                .performance(new BigDecimal(p))
                .qualidade(new BigDecimal(q))
                .oee(new BigDecimal(oee))
                .pecasBoas(pedido.getPecasBoas())
                .pecasDefeituosas(pedido.getPecasDefeituosas())
                .criadoEm(pedido.getFim())
                .build();
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    /** Números dos pedidos de uma listagem, na ordem em que vieram. */
    private List<Integer> numeros(HttpResponse<String> resp) {
        JsonNode pedidos = JSON.readTree(resp.body());
        List<Integer> numeros = new ArrayList<>();
        for (int i = 0; i < pedidos.size(); i++) {
            numeros.add(pedidos.get(i).get("numero").asInt());
        }
        return numeros;
    }

    private String maquinaComDadosJson() {
        return "{\"id\":" + maquinaComDadosId + ",\"bancadaId\":\"BANCADA_SMART_01\",\"nome\":\"Bancada Smart 4.0\","
                + "\"statusAtual\":\"PRODUZINDO\",\"ultimaLeituraEm\":\"2026-08-17T14:59:59.500Z\"}";
    }

    private String plantaSemConexaoJson(Long maquinaId) {
        return "{\"maquinaId\":" + maquinaId + ",\"status\":\"SEM_CONEXAO\",\"conectado\":false,\"fonte\":null,\"operacao\":null,"
                + "\"estacoes\":[{\"estacao\":\"ESTOQUE\",\"status\":null,\"situacao\":\"SEM_CONEXAO\",\"tempoPadraoS\":20.0},"
                + "{\"estacao\":\"MANIPULACAO\",\"status\":null,\"situacao\":\"SEM_CONEXAO\",\"tempoPadraoS\":35.0},"
                + "{\"estacao\":\"SEPARACAO\",\"status\":null,\"situacao\":\"SEM_CONEXAO\",\"tempoPadraoS\":25.0}],"
                + "\"pedidoEmAndamento\":null,\"tempoPadraoPedidoS\":80.0,\"ultimaLeituraEm\":null}";
    }

    // ---- máquinas e dashboard ---------------------------------------------

    @Test
    void listarMaquinas_retornaContratoEsperado() throws Exception {
        HttpResponse<String> resp = get("/api/maquinas");

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("[" + maquinaComDadosJson() + ","
                + "{\"id\":" + maquinaSemDadosId + ",\"bancadaId\":\"BANCADA_SMART_02\",\"nome\":\"Bancada Reserva\","
                + "\"statusAtual\":\"SEM_CONEXAO\",\"ultimaLeituraEm\":null}"
                + "]");
    }

    @Test
    void dashboard_juntaUltimoPedidoEstadoAoVivoResumoDeHojeEMeta() throws Exception {
        HttpResponse<String> resp = get("/api/dashboard/" + maquinaComDadosId);

        // Resumo de hoje: pedidos 2, 3 e 4 (o 1 é de ontem). Somando os
        // tempos dos concluídos: L = 190 s, parado = 12 s, padrão = 160 s,
        // 1 aprovado de 2 -> D 93,684%, P 89,888%, Q 50%, OEE 42,105%.
        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("{"
                + "\"maquina\":" + maquinaComDadosJson() + ","
                + "\"indicadorAtual\":{\"maquinaId\":" + maquinaComDadosId + ",\"pedidoId\":" + pedido3Id + ","
                + "\"disponibilidade\":100.0,\"performance\":88.889,\"qualidade\":0.0,\"oee\":0.0,"
                + "\"periodoInicio\":\"2026-08-17T13:00:00Z\",\"periodoFim\":\"2026-08-17T13:01:30Z\","
                + "\"calculadoEm\":\"2026-08-17T13:01:30Z\"},"
                + "\"planta\":" + plantaSemConexaoJson(maquinaComDadosId) + ","
                + "\"resumoHoje\":{\"concluidos\":2,\"aprovados\":1,\"reprovados\":1,\"interrompidos\":1,"
                + "\"pecasProduzidas\":6,\"taxaReprovacao\":50.0,\"leadTimeMedioS\":95.0,\"tempoParadoTotalS\":12.0,"
                + "\"disponibilidade\":93.684,\"performance\":89.888,\"qualidade\":50.0,\"oee\":42.105},"
                + "\"metaOee\":75.0"
                + "}");
    }

    @Test
    void dashboard_semPedido_retornaIndicadorNuloEResumoZerado() throws Exception {
        HttpResponse<String> resp = get("/api/dashboard/" + maquinaSemDadosId);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body())
                .contains("\"indicadorAtual\":null")
                .contains("\"resumoHoje\":{\"concluidos\":0,\"aprovados\":0,\"reprovados\":0,\"interrompidos\":0,"
                        + "\"pecasProduzidas\":0,\"taxaReprovacao\":null,\"leadTimeMedioS\":null,\"tempoParadoTotalS\":0.0,"
                        + "\"disponibilidade\":null,\"performance\":null,\"qualidade\":null,\"oee\":null}");
    }

    // ---- estado ao vivo ---------------------------------------------------

    @Test
    void statusPlanta_semLeitura_retornaSemConexao() throws Exception {
        HttpResponse<String> resp = get("/api/planta/" + maquinaComDadosId + "/status");

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo(plantaSemConexaoJson(maquinaComDadosId));
    }

    @Test
    void statusPlanta_comPedidoAberto_retornaEstacoesEPedidoEmAndamento() throws Exception {
        estadoPlanta.atualizar(new Estado(maquinaComDadosId, true, OrigemPedido.REPLAY, StatusMaquina.PRODUZINDO,
                "PRODUZINDO",
                List.of(new EstacaoAtual(Estacao.ESTOQUE, "AGUARDANDO", SituacaoEstacao.AGUARDANDO),
                        new EstacaoAtual(Estacao.MANIPULACAO, "PRODUZINDO", SituacaoEstacao.OPERANDO),
                        new EstacaoAtual(Estacao.SEPARACAO, "AGUARDANDO", SituacaoEstacao.AGUARDANDO)),
                new PedidoAtual(99L, 5, Instant.parse("2026-08-17T14:59:27.700Z"), Estacao.MANIPULACAO),
                Instant.parse("2026-08-17T14:59:59.500Z")));

        HttpResponse<String> resp = get("/api/planta/" + maquinaComDadosId + "/status");

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("{\"maquinaId\":" + maquinaComDadosId + ",\"status\":\"PRODUZINDO\","
                + "\"conectado\":true,\"fonte\":\"REPLAY\",\"operacao\":\"PRODUZINDO\","
                + "\"estacoes\":[{\"estacao\":\"ESTOQUE\",\"status\":\"AGUARDANDO\",\"situacao\":\"AGUARDANDO\",\"tempoPadraoS\":20.0},"
                + "{\"estacao\":\"MANIPULACAO\",\"status\":\"PRODUZINDO\",\"situacao\":\"OPERANDO\",\"tempoPadraoS\":35.0},"
                + "{\"estacao\":\"SEPARACAO\",\"status\":\"AGUARDANDO\",\"situacao\":\"AGUARDANDO\",\"tempoPadraoS\":25.0}],"
                + "\"pedidoEmAndamento\":{\"pedidoId\":99,\"numero\":5,\"inicio\":\"2026-08-17T14:59:27.700Z\","
                + "\"decorridoS\":32.3,\"etapaAtual\":\"MANIPULACAO\"},"
                + "\"tempoPadraoPedidoS\":80.0,\"ultimaLeituraEm\":\"2026-08-17T14:59:59.500Z\"}");
    }

    @Test
    void statusPlanta_maquinaQueNaoEstaSendoColetada_retornaSemConexao() throws Exception {
        HttpResponse<String> resp = get("/api/planta/" + maquinaSemDadosId + "/status");

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo(plantaSemConexaoJson(maquinaSemDadosId));
    }

    // ---- pedidos ----------------------------------------------------------

    @Test
    void pedidos_filtradosPorPeriodoEOrigem_retornaContratoEsperado() throws Exception {
        HttpResponse<String> resp = get("/api/pedidos/" + maquinaComDadosId
                + "?de=2026-08-17T03:00:00Z&ate=2026-08-18T03:00:00Z&origem=REPLAY");

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("["
                + "{\"id\":" + pedido4Id + ",\"maquinaId\":" + maquinaComDadosId + ",\"numero\":4,\"origem\":\"REPLAY\","
                + "\"situacao\":\"INTERROMPIDO\",\"inicio\":\"2026-08-17T14:00:00Z\",\"fim\":\"2026-08-17T14:00:38Z\","
                + "\"leadTimeS\":38.0,\"tempoParadoS\":0.0,\"tempoPadraoS\":55.0,\"reprovado\":false,"
                + "\"pecasBoas\":0,\"pecasDefeituosas\":0,\"duracaoEtapasS\":{\"ESTOQUE\":20.0,\"MANIPULACAO\":18.0},"
                + "\"disponibilidade\":null,\"performance\":null,\"qualidade\":null,\"oee\":null},"
                + "{\"id\":" + pedido3Id + ",\"maquinaId\":" + maquinaComDadosId + ",\"numero\":3,\"origem\":\"REPLAY\","
                + "\"situacao\":\"CONCLUIDO\",\"inicio\":\"2026-08-17T13:00:00Z\",\"fim\":\"2026-08-17T13:01:30Z\","
                + "\"leadTimeS\":90.0,\"tempoParadoS\":0.0,\"tempoPadraoS\":80.0,\"reprovado\":true,"
                + "\"pecasBoas\":0,\"pecasDefeituosas\":3,"
                + "\"duracaoEtapasS\":{\"ESTOQUE\":20.0,\"MANIPULACAO\":40.0,\"SEPARACAO\":25.0},"
                + "\"disponibilidade\":100.0,\"performance\":88.889,\"qualidade\":0.0,\"oee\":0.0}"
                + "]");
    }

    @Test
    void pedidos_semFiltros_retornaTodosDoMaisRecenteAoMaisAntigo() throws Exception {
        HttpResponse<String> resp = get("/api/pedidos/" + maquinaComDadosId);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(numeros(resp)).containsExactly(4, 3, 2, 1);
    }

    @Test
    void pedidos_filtrosOpcionaisSaoIndependentes() throws Exception {
        String base = "/api/pedidos/" + maquinaComDadosId;

        assertThat(numeros(get(base + "?origem=BANCADA"))).containsExactly(1);
        assertThat(numeros(get(base + "?de=2026-08-17T13:00:00Z"))).containsExactly(4, 3);
        assertThat(numeros(get(base + "?ate=2026-08-17T03:00:00Z"))).containsExactly(1);
        // Meia-noite de Brasília com o fuso na data: é o mesmo instante 03:00 UTC.
        assertThat(numeros(get(base + "?de=2026-08-17T00:00:00-03:00"))).containsExactly(4, 3, 2);
    }

    @Test
    void pedidos_maquinaSemPedidos_retornaListaVazia() throws Exception {
        HttpResponse<String> resp = get("/api/pedidos/" + maquinaSemDadosId);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("[]");
    }

    @Test
    void detalhePedido_retornaPedidoEEtapasEmOrdemDeInicio() throws Exception {
        HttpResponse<String> resp = get("/api/pedidos/detalhe/" + pedido2Id);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("{"
                + "\"pedido\":{\"id\":" + pedido2Id + ",\"maquinaId\":" + maquinaComDadosId + ",\"numero\":2,"
                + "\"origem\":\"SINTETICO\",\"situacao\":\"CONCLUIDO\","
                + "\"inicio\":\"2026-08-17T12:00:00Z\",\"fim\":\"2026-08-17T12:01:40Z\","
                + "\"leadTimeS\":100.0,\"tempoParadoS\":12.0,\"tempoPadraoS\":80.0,\"reprovado\":false,"
                + "\"pecasBoas\":3,\"pecasDefeituosas\":0,"
                + "\"duracaoEtapasS\":{\"ESTOQUE\":21.0,\"MANIPULACAO\":47.0,\"SEPARACAO\":26.0},"
                + "\"disponibilidade\":88.0,\"performance\":90.909,\"qualidade\":100.0,\"oee\":80.0},"
                + "\"etapas\":["
                + "{\"estacao\":\"ESTOQUE\",\"inicio\":\"2026-08-17T12:00:00Z\",\"fim\":\"2026-08-17T12:00:21Z\","
                + "\"duracaoS\":21.0,\"tempoParadoS\":0.0,\"tempoPadraoS\":20.0},"
                + "{\"estacao\":\"MANIPULACAO\",\"inicio\":\"2026-08-17T12:00:21Z\",\"fim\":\"2026-08-17T12:01:08Z\","
                + "\"duracaoS\":47.0,\"tempoParadoS\":12.0,\"tempoPadraoS\":35.0},"
                + "{\"estacao\":\"SEPARACAO\",\"inicio\":\"2026-08-17T12:01:08Z\",\"fim\":\"2026-08-17T12:01:34Z\","
                + "\"duracaoS\":26.0,\"tempoParadoS\":0.0,\"tempoPadraoS\":25.0}"
                + "]}");
    }

    @Test
    void historicoIndicadores_retornaUmPorPedidoConcluidoDoMaisRecenteAoMaisAntigo() throws Exception {
        HttpResponse<String> resp = get("/api/indicadores/historico/" + maquinaComDadosId);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("["
                + "{\"maquinaId\":" + maquinaComDadosId + ",\"pedidoId\":" + pedido3Id + ",\"disponibilidade\":100.0,"
                + "\"performance\":88.889,\"qualidade\":0.0,\"oee\":0.0,"
                + "\"periodoInicio\":\"2026-08-17T13:00:00Z\",\"periodoFim\":\"2026-08-17T13:01:30Z\",\"calculadoEm\":\"2026-08-17T13:01:30Z\"},"
                + "{\"maquinaId\":" + maquinaComDadosId + ",\"pedidoId\":" + pedido2Id + ",\"disponibilidade\":88.0,"
                + "\"performance\":90.909,\"qualidade\":100.0,\"oee\":80.0,"
                + "\"periodoInicio\":\"2026-08-17T12:00:00Z\",\"periodoFim\":\"2026-08-17T12:01:40Z\",\"calculadoEm\":\"2026-08-17T12:01:40Z\"},"
                + "{\"maquinaId\":" + maquinaComDadosId + ",\"pedidoId\":" + pedido1Id + ",\"disponibilidade\":100.0,"
                + "\"performance\":100.0,\"qualidade\":100.0,\"oee\":100.0,"
                + "\"periodoInicio\":\"2026-08-16T12:00:00Z\",\"periodoFim\":\"2026-08-16T12:01:20Z\",\"calculadoEm\":\"2026-08-16T12:01:20Z\"}"
                + "]");
    }

    // ---- erros ------------------------------------------------------------

    @Test
    void recursoInexistente_retorna404ComApiError() throws Exception {
        assertThat(get("/api/dashboard/999999")).satisfies(resp -> {
            assertThat(resp.statusCode()).isEqualTo(404);
            assertThat(resp.body())
                    .contains("\"status\":404")
                    .contains("\"erro\":\"Recurso não encontrado\"")
                    .contains("\"mensagem\":\"Máquina não encontrada com id: 999999\"")
                    .contains("\"caminho\":\"/api/dashboard/999999\"");
        });
        assertThat(get("/api/pedidos/999999").statusCode()).isEqualTo(404);
        assertThat(get("/api/planta/999999/status").statusCode()).isEqualTo(404);
        assertThat(get("/api/pedidos/detalhe/999999")).satisfies(resp -> {
            assertThat(resp.statusCode()).isEqualTo(404);
            assertThat(resp.body()).contains("\"mensagem\":\"Pedido não encontrado com id: 999999\"");
        });
    }

    @Test
    void rotaInexistente_retorna404() throws Exception {
        HttpResponse<String> resp = get("/api/rota-que-nao-existe");

        assertThat(resp.statusCode()).isEqualTo(404);
        assertThat(resp.body())
                .contains("\"erro\":\"Recurso não encontrado\"")
                .contains("\"mensagem\":\"Rota inexistente\"")
                .contains("\"caminho\":\"/api/rota-que-nao-existe\"");
    }

    @Test
    void idEmFormatoInvalido_retorna400() throws Exception {
        HttpResponse<String> resp = get("/api/dashboard/abc");

        assertThat(resp.statusCode()).isEqualTo(400);
        assertThat(resp.body())
                .contains("\"status\":400")
                .contains("\"erro\":\"Requisição inválida\"")
                .contains("\"mensagem\":\"Valor inválido para 'maquinaId'\"");
    }

    @Test
    void filtroEmFormatoInvalido_retorna400ComOFormatoEsperado() throws Exception {
        String base = "/api/pedidos/" + maquinaComDadosId;

        HttpResponse<String> origem = get(base + "?origem=XYZ");
        assertThat(origem.statusCode()).isEqualTo(400);
        assertThat(origem.body()).contains("Valores aceitos: BANCADA, REPLAY, SINTETICO");

        HttpResponse<String> data = get(base + "?de=ontem");
        assertThat(data.statusCode()).isEqualTo(400);
        assertThat(data.body()).contains("Use data e hora ISO-8601");
    }

    @Test
    void metodoDiferenteDeGet_retorna405() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/maquinas"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        assertThat(resp.statusCode()).isEqualTo(405);
        assertThat(resp.body()).contains("\"status\":405");
    }

    @Test
    void cors_liberaSoGetParaOFrontend() throws Exception {
        assertThat(preflight("GET").statusCode()).isEqualTo(200);
        assertThat(preflight("GET").headers().firstValue("Access-Control-Allow-Methods"))
                .hasValueSatisfying(metodos -> assertThat(metodos).contains("GET").doesNotContain("POST"));
        assertThat(preflight("POST").statusCode()).isEqualTo(403);
    }

    private HttpResponse<String> preflight(String metodo) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/api/maquinas"))
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody())
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", metodo)
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void health_respondeUp() throws Exception {
        HttpResponse<String> resp = get("/actuator/health");

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).contains("\"status\":\"UP\"");
    }
}
