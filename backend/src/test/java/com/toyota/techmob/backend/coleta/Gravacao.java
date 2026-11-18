package com.toyota.techmob.backend.coleta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.toyota.techmob.backend.config.PlantaProperties;
import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.OrigemPedido;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Lê as gravações de src/test/resources/gravacoes/ (formato do
 * coletor_planta.py; as mesmas de data/tests/gravacoes/) e as reproduz como
 * o ColetorPlanta faria: uma consulta por segundo, com falha de leitura
 * enquanto a gravação mostra queda de conexão.
 */
final class Gravacao {

    /** t_s = 0 da gravação. */
    static final Instant BASE = Instant.parse("2026-11-17T10:00:00Z");

    static final JsonMapper JSON = JsonMapper.builder().build();

    /** Uma linha da gravação: leitura (com os dados) ou erro. */
    record Registro(String tipo, double t, LeituraPlanta leitura) {
    }

    private Gravacao() {
    }

    static PlantaProperties propriedades() {
        return new PlantaProperties(1L, "http://bancada:1880/dadosPlanta", "http://simulador:1881/dadosPlanta",
                1000, 2000, 10, "AGUARDANDO", "PARADO", 75, new PlantaProperties.TempoPadrao(20, 35, 25));
    }

    static DetectorPedidos detector() {
        PlantaProperties p = propriedades();
        Map<Estacao, Double> padrao = p.tempoPadraoS().porEstacao();
        return new DetectorPedidos(padrao, p.statusParado(), p.statusParada(), p.segundosSemConexao());
    }

    static List<Registro> ler(String nome) {
        List<Registro> registros = new ArrayList<>();
        try (InputStream entrada = Gravacao.class.getResourceAsStream("/gravacoes/" + nome);
             BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (linha.isBlank()) {
                    continue;
                }
                JsonNode no = JSON.readTree(linha);
                String tipo = no.get("tipo").asString();
                double t = no.get("t_s").asDouble();
                LeituraPlanta leitura = "leitura".equals(tipo) ? JSON.treeToValue(no.get("dados"), LeituraPlanta.class) : null;
                registros.add(new Registro(tipo, t, leitura));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return registros;
    }

    /** Reproduz a gravação no detector, com uma consulta por segundo. */
    static List<DetectorPedidos.Evento> reproduzir(String nome, DetectorPedidos detector) {
        List<Registro> registros = ler(nome).stream()
                .filter(r -> !"inicio".equals(r.tipo()))
                .toList();
        double inicio = registros.getFirst().t();
        double fim = registros.getLast().t();
        List<DetectorPedidos.Evento> eventos = new ArrayList<>();
        int proximo = 0;
        Registro atual = null;
        for (int i = 0; inicio + i <= fim; i++) {
            double t = inicio + i;
            while (proximo < registros.size() && registros.get(proximo).t() <= t) {
                atual = registros.get(proximo++);
            }
            if ("leitura".equals(atual.tipo())) {
                eventos.addAll(detector.processar(atual.leitura(), instante(t), OrigemPedido.SINTETICO));
            } else if ("erro".equals(atual.tipo())) {
                eventos.addAll(detector.falhaDeLeitura(instante(t)));
            }
        }
        return eventos;
    }

    static Instant instante(double t) {
        return BASE.plusMillis(Math.round(t * 1000));
    }

    static double segundos(Instant instante) {
        return Duration.between(BASE, instante).toMillis() / 1000.0;
    }
}
