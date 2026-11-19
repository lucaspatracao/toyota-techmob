/**
 * Resumo de um conjunto de pedidos (já adaptados por adaptPedido), para as
 * telas que agrupam pedidos: Produção (por período) e Histórico (por hora
 * ou dia).
 *
 * É a mesma conta do backend (CalculadoraOEE.acumulado e o
 * ResumoPedidosDTO): contagens e médias só com os pedidos concluídos, os
 * interrompidos à parte, e o OEE acumulado SOMA os tempos de todos os
 * pedidos e aplica a fórmula uma vez. Não é a média dos OEEs, que distorce
 * quando os pedidos têm durações diferentes.
 *
 *   D = (L - T_parado) / L
 *   P = mín(1; T_padrão / (L - T_parado))
 *   Q = aprovados / concluídos
 *
 * Percentuais de 0 a 100; taxa, médias e OEE vêm null sem pedido concluído.
 */
export function resumirPedidos(pedidos) {
  const concluidos = pedidos.filter((p) => p.situacao === 'CONCLUIDO')
  const interrompidos = pedidos.filter((p) => p.situacao === 'INTERROMPIDO').length
  const reprovados = concluidos.filter((p) => p.reprovado).length
  const total = concluidos.length
  const somar = (campo) => concluidos.reduce((soma, p) => soma + (p[campo] ?? 0), 0)

  const leadTime = somar('leadTimeS')
  const parado = somar('tempoParadoS')
  const padrao = somar('tempoPadraoS')
  const operando = leadTime - parado
  const d = leadTime > 0 ? operando / leadTime : 0
  const p = operando > 0 ? Math.min(1, padrao / operando) : 0
  const q = total ? (total - reprovados) / total : 0

  return {
    concluidos: total,
    aprovados: total - reprovados,
    reprovados,
    interrompidos,
    pecasProduzidas: concluidos.reduce((soma, pedido) => soma + pedido.pecasBoas + pedido.pecasDefeituosas, 0),
    taxaReprovacao: total ? (100 * reprovados) / total : null,
    leadTimeMedioS: total ? leadTime / total : null,
    tempoParadoTotalS: parado,
    disponibilidade: total ? 100 * d : null,
    performance: total ? 100 * p : null,
    qualidade: total ? 100 * q : null,
    oee: total ? 100 * d * p * q : null,
  }
}

/**
 * Tempo médio de cada estação nos pedidos concluídos, em segundos, na ordem
 * das estações. `media` é null quando nenhum pedido passou pela estação.
 */
export function mediaPorEstacao(pedidos, estacoes) {
  const concluidos = pedidos.filter((p) => p.situacao === 'CONCLUIDO')
  return estacoes.map(({ codigo, nome }) => {
    const tempos = concluidos.map((p) => p.duracaoEtapasS[codigo]).filter((t) => t != null)
    return {
      estacao: codigo,
      nome,
      media: tempos.length ? tempos.reduce((soma, t) => soma + t, 0) / tempos.length : null,
    }
  })
}
