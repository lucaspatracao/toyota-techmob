package com.toyota.techmob.backend.service;

import java.time.Instant;

import com.toyota.techmob.backend.coleta.CalculadoraOEE;
import com.toyota.techmob.backend.coleta.DetectorPedidos.PedidoDetectado;
import com.toyota.techmob.backend.domain.OrigemPedido;

/**
 * Gravação do que a coleta descobre na bancada: pedido, etapas, OEE,
 * leitura bruta e status da máquina. É a única parte do backend que
 * escreve no banco.
 */
public interface RegistroPedidoService {

    /** Pedido gravado: id e número na máquina. */
    record PedidoRegistrado(Long id, Integer numero) {
    }

    /** Grava o pedido como EM_ANDAMENTO assim que ele começa. */
    PedidoRegistrado iniciarPedido(Long maquinaId, OrigemPedido origem, Instant inicio);

    /**
     * Fecha o pedido como CONCLUIDO, grava as etapas e o indicador de OEE de
     * uma vez. pedidoId nulo (o início não foi gravado) cria o pedido agora.
     */
    CalculadoraOEE.ResultadoOEE concluirPedido(Long maquinaId, Long pedidoId, PedidoDetectado pedido);

    /** Fecha o pedido como INTERROMPIDO, com as etapas, sem OEE. */
    void interromperPedido(Long maquinaId, Long pedidoId, PedidoDetectado pedido);

    /** Guarda o JSON bruto de uma leitura que mudou (tabela leitura_planta). */
    void registrarLeitura(Long maquinaId, Instant lidoEm, String json);

    /** Atualiza status_operacional e ultima_leitura_em da máquina. */
    void atualizarMaquina(Long maquinaId, String statusOperacional, Instant ultimaLeituraEm);

    /**
     * Pedidos que ficaram EM_ANDAMENTO (o backend parou no meio) viram
     * INTERROMPIDO. Chamado quando a coleta começa. Devolve quantos eram.
     */
    int interromperPedidosAbertos();
}
