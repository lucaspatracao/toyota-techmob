/**
 * Adapters: convertem o JSON cru retornado pela API (formato dos DTOs do
 * back-end) para o shape que os componentes de tela já esperam (o mesmo
 * shape usado em `src/data/mockData.js`).
 *
 * ⚠️ TODOS os nomes de campo abaixo (ex.: `d.oee`, `d.disponibilidade`)
 * são PALPITES baseados no Documento do Projeto e nas telas de
 * referência — ainda não confirmados com o código real dos DTOs
 * (DashboardResponseDTO, MaquinaDTO, HistoricoProducaoDTO,
 * IndicadorOEEDTO). Assim que você me passar esses 4 arquivos, eu
 * atualizo só este arquivo — nenhum componente de tela precisa mudar.
 *
 * Cada função tem um comentário "// confirmar:" apontando o campo que
 * precisa ser validado.
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
  return {
    oee: dto.oee, // confirmar: pode vir como dto.indicadores.oee
    disponibilidade: dto.disponibilidade,
    performance: dto.performance,
    qualidade: dto.qualidade,
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
