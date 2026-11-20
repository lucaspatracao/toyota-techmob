package com.toyota.techmob.backend.controller;

import java.time.Instant;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.toyota.techmob.backend.domain.OrigemPedido;
import com.toyota.techmob.backend.dto.PedidoDTO;
import com.toyota.techmob.backend.dto.PedidoDetalheDTO;
import com.toyota.techmob.backend.service.PedidoService;

import lombok.RequiredArgsConstructor;

/**
 * Pedidos da bancada (substitui o antigo /api/historico-producao).
 *
 * GET /api/pedidos/{maquinaId}?de=&ate=&origem=: pedidos do período, do mais
 * recente ao mais antigo. de e ate são instantes ISO-8601 (filtro pelo
 * início do pedido, em [de, ate)); origem é BANCADA, REPLAY ou SINTETICO.
 * Todos opcionais.
 *
 * GET /api/pedidos/detalhe/{pedidoId}: um pedido com as etapas.
 */
@RestController
@RequestMapping("/api/pedidos")
@RequiredArgsConstructor
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping("/{maquinaId}")
    public ResponseEntity<List<PedidoDTO>> listar(
            @PathVariable Long maquinaId,
            @RequestParam(required = false) Instant de,
            @RequestParam(required = false) Instant ate,
            @RequestParam(required = false) OrigemPedido origem) {
        return ResponseEntity.ok(pedidoService.listar(maquinaId, de, ate, origem));
    }

    @GetMapping("/detalhe/{pedidoId}")
    public ResponseEntity<PedidoDetalheDTO> detalhe(@PathVariable Long pedidoId) {
        return ResponseEntity.ok(pedidoService.detalhe(pedidoId));
    }
}
