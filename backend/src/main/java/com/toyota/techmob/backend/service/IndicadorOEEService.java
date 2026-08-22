package com.toyota.techmob.backend.service;

import java.util.List;

import com.toyota.techmob.backend.dto.IndicadorOEEDTO;

/**
 * Camada de serviço responsável pelo histórico de indicadores OEE
 * de uma máquina. Extraído do IndicadorOEEController, que antes
 * acessava MaquinaRepository e IndicadorOEERepository diretamente
 * e retornava a entidade JPA crua (risco de LazyInitializationException
 * e N+1 na serialização do relacionamento maquina).
 */
public interface IndicadorOEEService {

    List<IndicadorOEEDTO> historico(Long maquinaId);
}