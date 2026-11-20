package com.toyota.techmob.backend.config;

import java.util.EnumMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import com.toyota.techmob.backend.domain.Estacao;

/**
 * Configuração da coleta da Bancada Smart 4.0 (prefixo techmob.planta).
 * Todos os valores têm variável de ambiente e padrão em application.properties.
 *
 * @param maquinaId          máquina que recebe os pedidos (seed: 1)
 * @param url                GET /dadosPlanta do Node-RED da bancada (somente leitura)
 * @param urlFallback        simulador usado depois de 3 falhas seguidas na url (vazio = sem fallback)
 * @param intervaloMs        intervalo entre leituras
 * @param timeoutMs          tempo máximo de cada leitura
 * @param segundosSemConexao sem leitura boa por mais que isso, o pedido aberto é interrompido
 * @param statusParado       texto da estação ociosa, esperando peça (confirmado: AGUARDANDO)
 * @param statusParada       texto da estação em parada (provisório: PARADO)
 * @param metaOee            meta de OEE em % (provisório: 75)
 * @param tempoPadraoS       cronometragem de cada estação, em segundos (provisório: 20/35/25)
 */
@ConfigurationProperties("techmob.planta")
public record PlantaProperties(
        @DefaultValue("1") Long maquinaId,
        @DefaultValue("http://localhost:1880/dadosPlanta") String url,
        @DefaultValue("http://localhost:1881/dadosPlanta") String urlFallback,
        @DefaultValue("1000") long intervaloMs,
        @DefaultValue("2000") int timeoutMs,
        @DefaultValue("10") int segundosSemConexao,
        @DefaultValue("AGUARDANDO") String statusParado,
        @DefaultValue("PARADO") String statusParada,
        @DefaultValue("75") double metaOee,
        @DefaultValue TempoPadrao tempoPadraoS) {

    /** Tempo padrão (cronometragem) de cada estação, em segundos. */
    public record TempoPadrao(
            @DefaultValue("20") double estoque,
            @DefaultValue("35") double manipulacao,
            @DefaultValue("25") double separacao) {

        public double de(Estacao estacao) {
            return switch (estacao) {
                case ESTOQUE -> estoque;
                case MANIPULACAO -> manipulacao;
                case SEPARACAO -> separacao;
            };
        }

        public Map<Estacao, Double> porEstacao() {
            Map<Estacao, Double> tempos = new EnumMap<>(Estacao.class);
            for (Estacao estacao : Estacao.values()) {
                tempos.put(estacao, de(estacao));
            }
            return tempos;
        }

        /** Tempo padrão de um pedido inteiro. */
        public double total() {
            return estoque + manipulacao + separacao;
        }
    }
}
