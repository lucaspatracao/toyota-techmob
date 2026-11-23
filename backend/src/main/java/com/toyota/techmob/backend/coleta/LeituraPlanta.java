package com.toyota.techmob.backend.coleta;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.toyota.techmob.backend.domain.Estacao;

/**
 * Espelho do JSON do GET /dadosPlanta da bancada, com os mesmos nomes
 * (inclusive "Temperatura" e "Umidade" com maiúscula). É uma foto do
 * momento, não um evento: os pedidos saem das mudanças entre leituras.
 *
 * Temperatura e Umidade chegam sempre 0 (a bancada não tem esses sensores)
 * e total_pedidos/pedidos15min servem só de conferência.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LeituraPlanta(
        @JsonProperty("operacao_estoque") String operacaoEstoque,
        @JsonProperty("operacao_manipulacao") String operacaoManipulacao,
        @JsonProperty("operacao_separacao") String operacaoSeparacao,
        @JsonProperty("operacao_string") String operacaoString,
        @JsonProperty("aguardando") Integer aguardando,
        @JsonProperty("produzindo") Integer produzindo,
        @JsonProperty("total_pedidos") Integer totalPedidos,
        @JsonProperty("pedidos_reprovados") Integer pedidosReprovados,
        @JsonProperty("pedidos15min") Integer pedidos15min,
        @JsonProperty("Temperatura") Double temperatura,
        @JsonProperty("Umidade") Double umidade) {

    /** Texto de status da estação, como a bancada envia. */
    public String statusDa(Estacao estacao) {
        return switch (estacao) {
            case ESTOQUE -> operacaoEstoque;
            case MANIPULACAO -> operacaoManipulacao;
            case SEPARACAO -> operacaoSeparacao;
        };
    }

    /** Sem "aguardando" não dá para saber se há pedido: a leitura não serve. */
    public boolean valida() {
        return aguardando != null;
    }

    public boolean aguardandoPedido() {
        return Integer.valueOf(1).equals(aguardando);
    }
}
