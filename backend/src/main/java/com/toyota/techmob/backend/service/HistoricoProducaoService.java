package com.toyota.techmob.backend.service;

import java.util.List;

import com.toyota.techmob.backend.dto.HistoricoProducaoDTO;

/**
 * Camada de serviço responsável pelo histórico bruto de produção
 * (dados capturados ciclo a ciclo via Node-RED/MQTT, persistidos em
 * techmob.historico_producao).
 */
public interface HistoricoProducaoService {

    List<HistoricoProducaoDTO> historico(Long maquinaId);
}