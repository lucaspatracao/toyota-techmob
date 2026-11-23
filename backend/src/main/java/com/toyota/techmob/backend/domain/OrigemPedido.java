package com.toyota.techmob.backend.domain;

/**
 * De onde veio o dado do pedido.
 *
 * BANCADA: Node-RED real do laboratório (não envia cabeçalho).
 * REPLAY: simulador reproduzindo uma gravação real (X-TechMob-Fonte: replay).
 * SINTETICO: simulador gerando pedidos a partir da cronometragem
 * (X-TechMob-Fonte: sintetico).
 */
public enum OrigemPedido {
    BANCADA,
    REPLAY,
    SINTETICO
}
