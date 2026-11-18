package com.toyota.techmob.backend.coleta;

import java.net.Proxy;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.toyota.techmob.backend.config.PlantaProperties;
import com.toyota.techmob.backend.domain.OrigemPedido;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lê o GET /dadosPlanta. Só faz GET: o Node-RED da bancada é somente leitura.
 *
 * Tenta a url principal; depois de 3 falhas seguidas passa para a
 * urlFallback (o simulador) e, de tempos em tempos, tenta a principal de
 * novo. Resposta que não é JSON válido (o Node-RED monta o texto na mão e
 * quebra quando falta uma variável) conta como falha.
 *
 * A origem vem do cabeçalho X-TechMob-Fonte, que só o simulador envia:
 * "replay" = REPLAY, outro valor = SINTETICO, sem cabeçalho = BANCADA.
 */
@Component
public class PlantaClient {

    static final String CABECALHO_FONTE = "X-TechMob-Fonte";
    static final int FALHAS_PARA_FALLBACK = 3;
    static final int LEITURAS_ATE_TENTAR_PRINCIPAL = 30;

    private static final Logger logger = LoggerFactory.getLogger(PlantaClient.class);

    /** Uma leitura boa: o objeto, o JSON bruto (para a tabela leitura_planta) e a origem. */
    public record Resultado(LeituraPlanta leitura, String json, OrigemPedido origem) {
    }

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String url;
    private final String urlFallback;

    private int falhasSeguidas;
    private boolean usandoFallback;
    private int leiturasNoFallback;

    @Autowired
    public PlantaClient(PlantaProperties propriedades, JsonMapper jsonMapper) {
        this(propriedades, jsonMapper, RestClient.builder().requestFactory(fabrica(propriedades.timeoutMs())));
    }

    /** Usado nos testes, com o builder ligado ao MockRestServiceServer. */
    PlantaClient(PlantaProperties propriedades, JsonMapper jsonMapper, RestClient.Builder builder) {
        this.restClient = builder.build();
        this.jsonMapper = jsonMapper;
        this.url = propriedades.url();
        this.urlFallback = propriedades.urlFallback();
    }

    private static SimpleClientHttpRequestFactory fabrica(int timeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        // Sem proxy: a rede do laboratório pode ter proxy configurado no
        // Windows, e ele derrubaria a conexão com o IP local da bancada.
        fabrica.setProxy(Proxy.NO_PROXY);
        fabrica.setConnectTimeout(timeoutMs);
        fabrica.setReadTimeout(timeoutMs);
        return fabrica;
    }

    /** Uma leitura. Vazio quando falhou (sem conexão, erro HTTP ou JSON inválido). */
    public synchronized Optional<Resultado> ler() {
        if (usandoFallback) {
            return lerNoFallback();
        }
        Optional<Resultado> resultado = tentar(url);
        if (resultado.isPresent()) {
            falhasSeguidas = 0;
            return resultado;
        }
        falhasSeguidas++;
        if (falhasSeguidas >= FALHAS_PARA_FALLBACK && temFallback()) {
            logger.warn("Bancada sem resposta em {} ({} falhas seguidas); lendo o simulador em {}",
                    url, falhasSeguidas, urlFallback);
            usandoFallback = true;
            leiturasNoFallback = 0;
            return tentar(urlFallback);
        }
        return Optional.empty();
    }

    public synchronized boolean usandoFallback() {
        return usandoFallback;
    }

    private Optional<Resultado> lerNoFallback() {
        leiturasNoFallback++;
        if (leiturasNoFallback >= LEITURAS_ATE_TENTAR_PRINCIPAL) {
            leiturasNoFallback = 0;
            Optional<Resultado> principal = tentar(url);
            if (principal.isPresent()) {
                logger.info("Bancada respondeu de novo em {}; saindo do simulador", url);
                usandoFallback = false;
                falhasSeguidas = 0;
                return principal;
            }
        }
        return tentar(urlFallback);
    }

    private Optional<Resultado> tentar(String endereco) {
        try {
            ResponseEntity<String> resposta = restClient.get().uri(endereco).retrieve().toEntity(String.class);
            String json = resposta.getBody();
            if (json == null || json.isBlank()) {
                logger.debug("Resposta vazia de {}", endereco);
                return Optional.empty();
            }
            LeituraPlanta leitura = jsonMapper.readValue(json, LeituraPlanta.class);
            if (leitura == null || !leitura.valida()) {
                logger.debug("Leitura sem o campo aguardando em {}: {}", endereco, json);
                return Optional.empty();
            }
            return Optional.of(new Resultado(leitura, json, origem(resposta.getHeaders().getFirst(CABECALHO_FONTE))));
        } catch (RestClientException e) {
            logger.debug("Falha ao ler {}: {}", endereco, e.getMessage());
            return Optional.empty();
        } catch (JacksonException e) {
            logger.debug("JSON inválido em {}: {}", endereco, e.getOriginalMessage());
            return Optional.empty();
        }
    }

    private boolean temFallback() {
        return urlFallback != null && !urlFallback.isBlank() && !urlFallback.equals(url);
    }

    static OrigemPedido origem(String cabecalho) {
        if (cabecalho == null || cabecalho.isBlank()) {
            return OrigemPedido.BANCADA;
        }
        return "replay".equalsIgnoreCase(cabecalho.trim()) ? OrigemPedido.REPLAY : OrigemPedido.SINTETICO;
    }
}
