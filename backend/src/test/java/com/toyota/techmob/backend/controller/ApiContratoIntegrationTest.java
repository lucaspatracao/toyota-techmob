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

import com.toyota.techmob.backend.domain.HistoricoProducao;
import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.repository.HistoricoProducaoRepository;
import com.toyota.techmob.backend.repository.IndicadorOEERepository;
import com.toyota.techmob.backend.repository.MaquinaRepository;

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
    private HistoricoProducaoRepository historicoProducaoRepository;

    private final HttpClient http = HttpClient.newHttpClient();

    private Long maquinaComDadosId;
    private Long maquinaSemDadosId;

    @BeforeEach
    void popularBanco() {
        historicoProducaoRepository.deleteAll();
        indicadorOEERepository.deleteAll();
        maquinaRepository.deleteAll();

        OffsetDateTime t0 = OffsetDateTime.parse("2026-08-17T08:00:00Z");

        Maquina bancada = maquinaRepository.save(Maquina.builder()
                .identificadorBancada("BANCADA_SMART_01")
                .nome("Bancada Smart 4.0")
                .statusOperacional("EM_PRODUCAO")
                .capacidadeTeorica(new BigDecimal("0.10"))
                .tempoPlanejadoProducaoS(new BigDecimal("28800.00"))
                .criadoEm(t0)
                .atualizadoEm(t0)
                .build());
        maquinaComDadosId = bancada.getId();

        maquinaSemDadosId = maquinaRepository.save(Maquina.builder()
                .identificadorBancada("BANCADA_SMART_02")
                .nome("Bancada Reserva")
                .statusOperacional("PARADA")
                .capacidadeTeorica(new BigDecimal("0.10"))
                .tempoPlanejadoProducaoS(new BigDecimal("28800.00"))
                .criadoEm(t0)
                .atualizadoEm(t0)
                .build()).getId();

        indicadorOEERepository.save(indicador(bancada, "2026-08-17T08:00:00Z", "2026-08-17T09:00:00Z", "70.000"));
        indicadorOEERepository.save(indicador(bancada, "2026-08-17T09:00:00Z", "2026-08-17T10:00:00Z", "74.600"));

        historicoProducaoRepository.save(HistoricoProducao.builder()
                .maquina(bancada)
                .timestamp(OffsetDateTime.parse("2026-08-17T08:00:10Z"))
                .statusOperacional("EM_PRODUCAO")
                .pecasBoas(1)
                .pecasDefeituosas(0)
                .tempoCicloSegundos(new BigDecimal("9.800"))
                .criadoEm(t0)
                .build());
    }

    private IndicadorOEE indicador(Maquina maquina, String inicio, String fim, String oee) {
        return IndicadorOEE.builder()
                .maquina(maquina)
                .periodoInicio(OffsetDateTime.parse(inicio))
                .periodoFim(OffsetDateTime.parse(fim))
                .disponibilidade(new BigDecimal("90.000"))
                .performance(new BigDecimal("85.000"))
                .qualidade(new BigDecimal("97.500"))
                .oee(new BigDecimal(oee))
                .pecasBoas(39)
                .pecasDefeituosas(1)
                .criadoEm(OffsetDateTime.parse(fim))
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
                + "{\"id\":" + maquinaComDadosId + ",\"bancadaId\":\"BANCADA_SMART_01\",\"nome\":\"Bancada Smart 4.0\",\"statusAtual\":\"EM_PRODUCAO\"},"
                + "{\"id\":" + maquinaSemDadosId + ",\"bancadaId\":\"BANCADA_SMART_02\",\"nome\":\"Bancada Reserva\",\"statusAtual\":\"PARADA\"}"
                + "]");
    }

    @Test
    void dashboard_retornaMaquinaEIndicadorMaisRecente() throws Exception {
        HttpResponse<String> resp = get("/api/dashboard/" + maquinaComDadosId);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("{"
                + "\"maquina\":{\"id\":" + maquinaComDadosId + ",\"bancadaId\":\"BANCADA_SMART_01\",\"nome\":\"Bancada Smart 4.0\",\"statusAtual\":\"EM_PRODUCAO\"},"
                + "\"indicadorAtual\":{\"maquinaId\":" + maquinaComDadosId + ",\"disponibilidade\":90.0,\"performance\":85.0,"
                + "\"qualidade\":97.5,\"oee\":74.6,\"calculadoEm\":\"2026-08-17T10:00:00Z\"}"
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
                + "{\"maquinaId\":" + maquinaComDadosId + ",\"disponibilidade\":90.0,\"performance\":85.0,\"qualidade\":97.5,\"oee\":74.6,\"calculadoEm\":\"2026-08-17T10:00:00Z\"},"
                + "{\"maquinaId\":" + maquinaComDadosId + ",\"disponibilidade\":90.0,\"performance\":85.0,\"qualidade\":97.5,\"oee\":70.0,\"calculadoEm\":\"2026-08-17T09:00:00Z\"}"
                + "]");
    }

    @Test
    void historicoProducao_retornaContratoEsperado() throws Exception {
        HttpResponse<String> resp = get("/api/historico-producao/" + maquinaComDadosId);

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).isEqualTo("["
                + "{\"maquinaId\":" + maquinaComDadosId + ",\"timestamp\":\"2026-08-17T08:00:10Z\",\"statusOperacional\":\"EM_PRODUCAO\","
                + "\"pecasBoas\":1,\"pecasDefeituosas\":0,\"tempoCicloSegundos\":9.800}"
                + "]");
    }

    @Test
    void health_respondeUp() throws Exception {
        HttpResponse<String> resp = get("/actuator/health");

        assertThat(resp.statusCode()).isEqualTo(200);
        assertThat(resp.body()).contains("\"status\":\"UP\"");
    }
}
