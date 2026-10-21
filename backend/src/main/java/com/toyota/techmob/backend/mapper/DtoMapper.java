package com.toyota.techmob.backend.mapper;

import com.toyota.techmob.backend.domain.HistoricoProducao;
import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.dto.HistoricoProducaoDTO;
import com.toyota.techmob.backend.dto.IndicadorOEEDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;

/**
 * Converte entidades JPA em DTOs de resposta.
 *
 * Fica num lugar só para que todos os endpoints devolvam o mesmo formato.
 */
public final class DtoMapper {

    private DtoMapper() {
    }

    public static MaquinaDTO paraDTO(Maquina maquina) {
        return new MaquinaDTO(
                maquina.getId(),
                maquina.getIdentificadorBancada(),
                maquina.getNome(),
                maquina.getStatusOperacional());
    }

    public static IndicadorOEEDTO paraDTO(IndicadorOEE indicador) {
        return new IndicadorOEEDTO(
                indicador.getMaquina().getId(),
                indicador.getDisponibilidade().doubleValue(),
                indicador.getPerformance().doubleValue(),
                indicador.getQualidade().doubleValue(),
                indicador.getOee().doubleValue(),
                indicador.getCriadoEm().toInstant());
    }

    public static HistoricoProducaoDTO paraDTO(HistoricoProducao registro) {
        return new HistoricoProducaoDTO(
                registro.getMaquina().getId(),
                registro.getTimestamp().toInstant(),
                registro.getStatusOperacional(),
                registro.getPecasBoas(),
                registro.getPecasDefeituosas(),
                registro.getTempoCicloSegundos());
    }
}
