package com.toyota.techmob.backend.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.domain.Pedido;
import com.toyota.techmob.backend.domain.SituacaoPedido;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;
import com.toyota.techmob.backend.repository.PedidoRepository;

/**
 * Teste de integração: sobe a aplicação inteira com um banco H2 em memória
 * e chama os endpoints via HTTP.
 *
 * Garante o contrato JSON que o frontend consome: se algum campo mudar de
 * nome, sumir ou mudar de formato, este teste falha.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:techmob;MODE=MySQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.jpa.show-sql=false"
        })
class ApiContratoIntegrationTest {

    @Value("${local.server.port}")
    private int porta;

    @Autowired
    private MaquinaRepository maquinaRepository;

    @Autowired
    private IndicadorOEERepository indicadorOEERepository;

    @Autowired
    private PedidoRepository pedidoRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    private Long maquinaComDadosId;
    private Long maquinaSemDadosId;
    private Long pedido1Id;
    private Long pedido2Id;

    @BeforeEach
    void popularBanco() {
        indicadorOEERepository.deleteAll();
        pedidoRepository.deleteAll();
        maquinaRepository.deleteAll();

        OffsetDateTime t0 = OffsetDateTime.parse("2026-08-17T08:00:00Z");

        Maquina bancada = maquinaRepository.save(Maquina.builder()
                .identificadorBancada("BANCADA_SMART_01")
                .nome("Bancada Smart 4.0")
                .statusOperacional("EM_PRODUCAO")
                .tempoPadraoPedidoS(new BigDecimal("80.000"))
                .pecasPorPedido(3)
                .criadoEm(t0)
                .atualizadoEm(t0)
                .build());
        maquinaComDadosId = bancada.getId();

        maquinaSemDadosId = maquinaRepository.save(Maquina.builder()
                .identificadorBancada("BANCADA_SMART_02")
                .nome("Bancada Reserva")
                .statusOperacional("PARADA")
                .tempoPadraoPedidoS(new BigDecimal("80.000"))
                .pecasPorPedido(3)
                .criadoEm(t0)
                .atualizadoEm(t0)
                .build()).getId();

        Pedido pedido1 = pedidoRepository.save(pedido(bancada, 1, "2026-08-17T08:00:00Z", "2026-08-17T09:00:00Z"));
        Pedido pedido2 = pedidoRepository.save(pedido(bancada, 2, "2026-08-17T09:00:00Z", "2026-08-17T10:00:00Z"));
        pedido1Id = pedido1.getId();
        pedido2Id = pedido2.getId();
        indicadorOEERepository.save(indicador(pedido1, "70.000"));
        indicadorOEERepository.save(indicador(pedido2, "74.600"));
    }

    private Pedido pedido(Maquina maquina, int numero, String inicio, String fim) {
        return Pedido.builder()
                .maquina(maquina)
                .numero(numero)
                .origem(OrigemPedido.SINTETICO)
                .situacao(SituacaoPedido.CONCLUIDO)
                .inicio(OffsetDateTime.parse(inicio))
                .fim(OffsetDateTime.parse(fim))
                .leadTimeS(new BigDecimal("3600.000"))
                .tempoParadoS(BigDecimal.ZERO)
                .reprovado(false)
                .pecasBoas(3)
                .pecasDefeituosas(0)
                .criadoEm(OffsetDateTime.parse(fim))
                .build();
    }

    private IndicadorOEE indicador(Pedido pedido, String oee) {
        return IndicadorOEE.builder()
                .maquina(pedido.getMaquina())
                .pedido(pedido)
                .periodoInicio(pedido.getInicio())
                .periodoFim(pedido.getFim())
                .disponibilidade(new BigDecimal("90.000"))
                .performance(new BigDecimal("85.000"))
                .qualidade(new BigDecimal("97.500"))
                .oee(new BigDecimal(oee))
                .pecasBoas(39)
                .pecasDefeituosas(1)
                .criadoEm(pedido.getFim())
                .build();
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void listarMaquinas_retornaContratoEsperado() throws Exception {
        HttpResponse<String> resp = get("/api/maquinas");

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("["
                + "{\"id\":" + maquinaComDadosId + ",\"bancadaId\":\"BANCADA_SMART_01\",\"nome\":\"Bancada Smart 4.0\",\"statusAtual\":\"EM_PRODUCAO\",\"ultimaLeituraEm\":null},"
                + "{\"id\":" + maquinaSemDadosId + ",\"bancadaId\":\"BANCADA_SMART_02\",\"nome\":\"Bancada Reserva\",\"statusAtual\":\"PARADA\",\"ultimaLeituraEm\":null}"
                + "]");
    }

    @Test
    void dashboard_retornaMaquinaEIndicadorMaisRecente() throws Exception {
        HttpResponse<String> resp = get("/api/dashboard/" + maquinaComDadosId);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("{"
                + "\"maquina\":{\"id\":" + maquinaComDadosId + ",\"bancadaId\":\"BANCADA_SMART_01\",\"nome\":\"Bancada Smart 4.0\",\"statusAtual\":\"EM_PRODUCAO\",\"ultimaLeituraEm\":null},"
                + "\"indicadorAtual\":{\"maquinaId\":" + maquinaComDadosId + ",\"pedidoId\":" + pedido2Id + ",\"disponibilidade\":90.0,\"performance\":85.0,"
                + "\"qualidade\":97.5,\"oee\":74.6,\"periodoInicio\":\"2026-08-17T09:00:00Z\",\"periodoFim\":\"2026-08-17T10:00:00Z\","
                + "\"calculadoEm\":\"2026-08-17T10:00:00Z\"},"
                + "\"planta\":null,\"resumoHoje\":null,\"metaOee\":null"
                + "}");
    }

    @Test
    void dashboard_semIndicador_retornaIndicadorNulo() throws Exception {
        HttpResponse<String> resp = get("/api/dashboard/" + maquinaSemDadosId);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).contains("\"indicadorAtual\":null");
    }

    @Test
    void dashboard_maquinaInexistente_retorna404ComApiError() throws Exception {
        HttpResponse<String> resp = get("/api/dashboard/999999");

        assertThat(resp.statusCode()).isEqualTo(404);
        assertThat(resp.body())
                .contains("\"status\":404")
                .contains("\"erro\":\"Recurso não encontrado\"")
                .contains("\"caminho\":\"/api/dashboard/999999\"");
    }

    @Test
    void historicoIndicadores_retornaDoMaisRecenteParaOMaisAntigo() throws Exception {
        HttpResponse<String> resp = get("/api/indicadores/historico/" + maquinaComDadosId);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("["
                + "{\"maquinaId\":" + maquinaComDadosId + ",\"pedidoId\":" + pedido2Id + ",\"disponibilidade\":90.0,\"performance\":85.0,\"qualidade\":97.5,\"oee\":74.6,"
                + "\"periodoInicio\":\"2026-08-17T09:00:00Z\",\"periodoFim\":\"2026-08-17T10:00:00Z\",\"calculadoEm\":\"2026-08-17T10:00:00Z\"},"
                + "{\"maquinaId\":" + maquinaComDadosId + ",\"pedidoId\":" + pedido1Id + ",\"disponibilidade\":90.0,\"performance\":85.0,\"qualidade\":97.5,\"oee\":70.0,"
                + "\"periodoInicio\":\"2026-08-17T08:00:00Z\",\"periodoFim\":\"2026-08-17T09:00:00Z\",\"calculadoEm\":\"2026-08-17T09:00:00Z\"}"
                + "]");
    }

    @Test
    void health_respondeUp() throws Exception {
        HttpResponse<String> resp = get("/actuator/health");

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).contains("\"status\":\"UP\"");
    }
}
