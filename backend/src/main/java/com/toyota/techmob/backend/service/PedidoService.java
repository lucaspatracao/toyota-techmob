package com.toyota.techmob.backend.service;

import java.time.Instant;
import java.util.List;

import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.dto.PedidoDTO;
import com.toyota.techmob.backend.dto.PedidoDetalheDTO;
import com.toyota.techmob.backend.dto.ResumoPedidosDTO;

/**
 * Consulta dos pedidos gravados pela coleta (somente leitura).
 *
 * O período é pelo início do pedido, no intervalo [de, ate). Qualquer
 * filtro nulo fica de fora: sem de e ate, todos os pedidos; sem origem,
 * todas as origens.
 */
public interface PedidoService {

    /** Pedidos da máquina no período, do mais recente ao mais antigo, com o OEE de cada um. */
    List<PedidoDTO> listar(Long maquinaId, Instant de, Instant ate, OrigemPedido origem);

    /** Um pedido com as etapas em ordem de início. */
    PedidoDetalheDTO detalhe(Long pedidoId);

    /**
     * Resumo do período: contagens, médias e o OEE acumulado, que soma os
     * tempos dos pedidos concluídos e aplica a fórmula uma vez.
     */
    ResumoPedidosDTO resumir(Long maquinaId, Instant de, Instant ate, OrigemPedido origem);
}
