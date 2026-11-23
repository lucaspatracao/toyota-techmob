package com.toyota.techmob.backend.coleta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.toyota.techmob.backend.config.PlantaProperties;
import com.toyota.techmob.backend.domain.OrigemPedido;

/** PlantaClient contra um servidor HTTP falso (MockRestServiceServer). */
class PlantaClientTest {

    // JSON real lido na bancada parada (14/09).
    private static final String JSON_BANCADA = "{\"operacao_estoque\":\"AGUARDANDO\", \"operacao_manipulacao\":\"AGUARDANDO\", "
            + "\"operacao_separacao\":\"AGUARDANDO\", \"operacao_string\":\"AGUARDANDO\", \"aguardando\":1, \"produzindo\":0, "
            + "\"total_pedidos\":1, \"pedidos_reprovados\":0, \"pedidos15min\":1, \"Temperatura\":0, \"Umidade\":0}";

    private final PlantaProperties propriedades = Gravacao.propriedades();
    private final String principal = propriedades.url();
    private final String fallback = propriedades.urlFallback();

    private MockRestServiceServer servidor;
    private PlantaClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        cliente = new PlantaClient(propriedades, Gravacao.JSON, builder);
    }

    private static HttpHeaders fonte(String valor) {
        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.add("X-TechMob-Fonte", valor);
        return cabecalhos;
    }

    @Test
    void respostaNormal_semCabecalho_eDaBancada() {
        servidor.expect(once(), requestTo(principal)).andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(JSON_BANCADA, MediaType.APPLICATION_JSON));

        Optional<PlantaClient.Resultado> resultado = cliente.ler();

        assertThat(resultado).hasValueSatisfying(r -> {
            assertThat(r.origem()).isEqualTo(OrigemPedido.BANCADA);
            assertThat(r.leitura().aguardando()).isEqualTo(1);
            assertThat(r.leitura().operacaoString()).isEqualTo("AGUARDANDO");
            assertThat(r.leitura().totalPedidos()).isEqualTo(1);
            assertThat(r.json()).isEqualTo(JSON_BANCADA);
        });
        servidor.verify();
    }

    @Test
    void cabecalhoDoSimulador_defineAOrigem() {
        servidor.expect(once(), requestTo(principal))
                .andRespond(withSuccess(JSON_BANCADA, MediaType.APPLICATION_JSON).headers(fonte("replay")));
        assertThat(cliente.ler()).map(PlantaClient.Resultado::origem).contains(OrigemPedido.REPLAY);

        assertThat(PlantaClient.origem("sintetico")).isEqualTo(OrigemPedido.SINTETICO);
        assertThat(PlantaClient.origem(null)).isEqualTo(OrigemPedido.BANCADA);
    }

    @Test
    void jsonInvalido_contaComoFalha() {
        // O Node-RED concatena texto: com uma variável indefinida o JSON quebra.
        servidor.expect(once(), requestTo(principal))
                .andRespond(withSuccess("{\"operacao_estoque\":\"AGUARDANDO\", \"aguardando\":undefined}", MediaType.TEXT_PLAIN));

        assertThat(cliente.ler()).isEmpty();
        servidor.verify();
    }

    @Test
    void erroHttp_contaComoFalha() {
        servidor.expect(once(), requestTo(principal)).andRespond(withServerError());

        assertThat(cliente.ler()).isEmpty();
    }

    @Test
    void tresFalhasSeguidas_passaParaOFallback() {
        servidor.expect(times(3), requestTo(principal)).andRespond(withException(new ConnectException("recusada")));
        servidor.expect(times(2), requestTo(fallback))
                .andRespond(withSuccess(JSON_BANCADA, MediaType.APPLICATION_JSON).headers(fonte("replay")));

        assertThat(cliente.ler()).isEmpty();
        assertThat(cliente.ler()).isEmpty();
        // Na terceira falha já lê o simulador.
        assertThat(cliente.ler()).map(PlantaClient.Resultado::origem).contains(OrigemPedido.REPLAY);
        assertThat(cliente.usandoFallback()).isTrue();
        // E continua nele, sem esperar a bancada.
        assertThat(cliente.ler()).isPresent();
        servidor.verify();
    }

    @Test
    void noFallback_tentaABancadaDeTemposEmTempos() throws IOException {
        servidor.expect(times(3), requestTo(principal)).andRespond(withException(new ConnectException("recusada")));
        servidor.expect(times(PlantaClient.LEITURAS_ATE_TENTAR_PRINCIPAL), requestTo(fallback))
                .andRespond(withSuccess(JSON_BANCADA, MediaType.APPLICATION_JSON).headers(fonte("sintetico")));
        servidor.expect(once(), requestTo(principal)).andRespond(withSuccess(JSON_BANCADA, MediaType.APPLICATION_JSON));

        for (int i = 0; i < 2 + PlantaClient.LEITURAS_ATE_TENTAR_PRINCIPAL; i++) {
            cliente.ler();
        }
        Optional<PlantaClient.Resultado> deVolta = cliente.ler();

        assertThat(deVolta).map(PlantaClient.Resultado::origem).contains(OrigemPedido.BANCADA);
        assertThat(cliente.usandoFallback()).isFalse();
        servidor.verify();
    }
}
