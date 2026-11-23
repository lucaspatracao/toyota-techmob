package com.toyota.techmob.backend.exception;

public class PedidoNotFoundException extends RuntimeException {

    public PedidoNotFoundException(Long pedidoId) {
        super("Pedido não encontrado com id: " + pedidoId);
    }
}
