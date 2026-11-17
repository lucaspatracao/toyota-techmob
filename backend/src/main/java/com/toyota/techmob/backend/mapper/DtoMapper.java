package com.toyota.techmob.backend.mapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.toyota.techmob.backend.domain.Estacao;
import com.toyota.techmob.backend.domain.EtapaPedido;
import com.toyota.techmob.backend.domain.IndicadorOEE;
import com.toyota.techmob.backend.domain.Maquina;
import com.toyota.techmob.backend.domain.Pedido;
import com.toyota.techmob.backend.dto.EtapaPedidoDTO;
import com.toyota.techmob.backend.dto.IndicadorOEEDTO;
import com.toyota.techmob.backend.dto.MaquinaDTO;
import com.toyota.techmob.backend.dto.PedidoDTO;
import com.toyota.techmob.backend.dto.PedidoDetalheDTO;

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
                maquina.getStatusOperacional(),
                instante(maquina.getUltimaLeituraEm()));
    }

    public static IndicadorOEEDTO paraDTO(IndicadorOEE indicador) {
        return new IndicadorOEEDTO(
                indicador.getMaquina().getId(),
                indicador.getPedido() != null ? indicador.getPedido().getId() : null,
                indicador.getDisponibilidade().doubleValue(),
                indicador.getPerformance().doubleValue(),
                indicador.getQualidade().doubleValue(),
                indicador.getOee().doubleValue(),
                instante(indicador.getPeriodoInicio()),
                instante(indicador.getPeriodoFim()),
                instante(indicador.getCriadoEm()));
    }

    public static EtapaPedidoDTO paraDTO(EtapaPedido etapa) {
        return new EtapaPedidoDTO(
                etapa.getEstacao(),
                instante(etapa.getInicio()),
                instante(etapa.getFim()),
                numero(etapa.getDuracaoS()),
                numero(etapa.getTempoParadoS()),
                numero(etapa.getTempoPadraoS()));
    }

    public static PedidoDTO paraDTO(Pedido pedido) {
        List<EtapaPedido> etapas = pedido.getEtapas();
        IndicadorOEE indicador = pedido.getIndicador();

        // Soma por estação: se uma estação aparecer duas vezes no mesmo
        // pedido, o tempo dela é o total das duas.
        Map<Estacao, Double> duracaoEtapas = new LinkedHashMap<>();
        etapas.stream()
                .sorted(Comparator.comparing(EtapaPedido::getEstacao))
                .forEach(e -> duracaoEtapas.merge(e.getEstacao(), numero(e.getDuracaoS()), Double::sum));

        Double tempoPadrao = etapas.isEmpty() ? null : etapas.stream()
                .map(EtapaPedido::getTempoPadraoS)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .doubleValue();

        return new PedidoDTO(
                pedido.getId(),
                pedido.getMaquina().getId(),
                pedido.getNumero(),
                pedido.getOrigem(),
                pedido.getSituacao(),
                instante(pedido.getInicio()),
                instante(pedido.getFim()),
                numero(pedido.getLeadTimeS()),
                numero(pedido.getTempoParadoS()),
                tempoPadrao,
                pedido.getReprovado(),
                pedido.getPecasBoas(),
                pedido.getPecasDefeituosas(),
                duracaoEtapas,
                indicador != null ? indicador.getDisponibilidade().doubleValue() : null,
                indicador != null ? indicador.getPerformance().doubleValue() : null,
                indicador != null ? indicador.getQualidade().doubleValue() : null,
                indicador != null ? indicador.getOee().doubleValue() : null);
    }

    public static PedidoDetalheDTO paraDetalheDTO(Pedido pedido) {
        List<EtapaPedidoDTO> etapas = pedido.getEtapas().stream()
                .sorted(Comparator.comparing(EtapaPedido::getInicio))
                .map(DtoMapper::paraDTO)
                .toList();
        return new PedidoDetalheDTO(paraDTO(pedido), etapas);
    }

    private static Instant instante(OffsetDateTime data) {
        return data != null ? data.toInstant() : null;
    }

    private static Double numero(BigDecimal valor) {
        return valor != null ? valor.doubleValue() : null;
    }
}
