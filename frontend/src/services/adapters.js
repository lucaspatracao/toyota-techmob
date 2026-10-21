/**
 * Adapters: convertem o JSON cru retornado pela API (formato dos DTOs do
 * back-end) para o shape que os componentes de tela já esperam (o mesmo
 * shape usado em `src/data/mockData.js`).
 *
 * JSON que a API envia hoje (DTOs em backend/.../dto):
 *   GET /api/dashboard/{id}
 *     { maquina: { id, bancadaId, nome, statusAtual },
 *       indicadorAtual: { maquinaId, disponibilidade, performance, qualidade, oee, calculadoEm } | null }
 *   GET /api/indicadores/historico/{id}
 *     [{ maquinaId, disponibilidade, performance, qualidade, oee, calculadoEm }]
 *   GET /api/historico-producao/{id}
 *     [{ maquinaId, timestamp, statusOperacional, pecasBoas, pecasDefeituosas, tempoCicloSegundos }]
 *   GET /api/maquinas
 *     [{ id, bancadaId, nome, statusAtual }]
 *
 * Pendente: só adaptDashboard está ligado à API, e só os 4 indicadores
 * de indicadorAtual existem de fato. Série, resumo e as funções de
 * histórico/máquinas ainda esperam campos que a API não envia
 * (tendenciaOee, resumo24h, pecasRejeitadas, dataHora, status...) e
 * precisam ser ajustadas antes de trocar os mocks pela API.
 */

// ---------- Dashboard ----------
// Espera-se algo como:
// {
//   oee: 78.4, disponibilidade: 85.7, performance: 81.2, qualidade: 94.8,
//   tendenciaOee: [{ timestamp, oee }, ...],
//   resumo24h: { pecasBoas, pecasRejeitadas, tempoCicloMedioSegundos },
//   producaoPorPeriodo: [{ periodo, pecasBoas, pecasRejeitadas, taxaRejeicao, tempoCicloMedio, oee }, ...]
// }
export function adaptDashboard(dto) {
  if (!dto) return null

  // O backend só considera a dashboard pronta quando há indicador OEE calculado.
  const indicador = dto.indicadorAtual ?? (Object.prototype.hasOwnProperty.call(dto, 'indicadorAtual') ? null : dto)
  if (!indicador) return null
  if ([indicador.oee, indicador.disponibilidade, indicador.performance, indicador.qualidade].every((value) => value == null)) {
    return null
  }

  return {
    oee: indicador.oee,
    disponibilidade: indicador.disponibilidade,
    performance: indicador.performance,
    qualidade: indicador.qualidade,
    oeeSeries: (dto.tendenciaOee ?? dto.historicoOee ?? []).map((p) => ({
      time: p.horario ?? p.timestamp ?? p.time,
      oee: p.oee ?? p.valorOee,
    })),
    resumo: {
      boas: dto.resumo24h?.pecasBoas ?? dto.pecasBoas,
      rejeitadas: dto.resumo24h?.pecasRejeitadas ?? dto.pecasRejeitadas,
      tempoCicloMedio: dto.resumo24h?.tempoCicloMedioSegundos ?? dto.tempoCicloMedio,
    },
    periodSummary: (dto.producaoPorPeriodo ?? []).map((p) => ({
      periodo: p.periodo,
      boas: p.pecasBoas,
      rejeitadas: p.pecasRejeitadas,
      taxa: formatPercent(p.taxaRejeicao),
      ciclo: formatSeconds(p.tempoCicloMedio),
      oee: formatPercent(p.oee),
    })),
  }
}

// ---------- Histórico de Produção ----------
// Espera-se uma lista de:
// { dataHora, periodoInicio, periodoFim, turno, pecasBoas, pecasRejeitadas,
//   taxaRejeicao, tempoCicloMedio, oeeMedio, status }
export function adaptHistorico(list) {
  return (list ?? []).map((h) => ({
    status: mapStatus(h.status), // confirmar: back-end pode não enviar "status" — nesse caso, calcular no front a partir da taxa de rejeição
    dataHora: h.dataHora ?? `${h.periodoInicio} - ${h.periodoFim}`,
    periodo: h.periodo ?? `${h.periodoInicio} - ${h.periodoFim}`,
    turno: h.turno,
    boas: h.pecasBoas,
    rejeitadas: h.pecasRejeitadas,
    taxa: formatPercent(h.taxaRejeicao),
    ciclo: formatSeconds(h.tempoCicloMedio),
    oee: formatPercent(h.oeeMedio ?? h.oee),
  }))
}

// ---------- Indicadores OEE (série histórica) ----------
// Espera-se uma lista de: { timestamp, oee, disponibilidade, performance, qualidade }
export function adaptIndicadoresOee(list) {
  return (list ?? []).map((i) => ({
    time: i.timestamp ?? i.horario,
    oee: i.oee,
  }))
}

// ---------- Máquinas ----------
// Espera-se: { id, nome, status } — usado no card da Sidebar.
export function adaptMaquinas(list) {
  return (list ?? []).map((m) => ({
    id: m.id,
    nome: m.nome ?? m.name,
    status: m.status ?? (m.operando ? 'Operando' : 'Parada'),
  }))
}

// ---------- Helpers ----------
function formatPercent(v) {
  if (v === undefined || v === null) return '-'
  return `${v.toFixed(1).replace('.', ',')}%`
}
function formatSeconds(v) {
  if (v === undefined || v === null) return '-'
  return `${v.toFixed(2).replace('.', ',')} s`
}
function mapStatus(status) {
  if (!status) return 'green'
  const s = status.toLowerCase()
  if (s.includes('crit') || s.includes('alto')) return 'red'
  if (s.includes('atenc') || s.includes('médio') || s.includes('medio')) return 'orange'
  return 'green'
}
