import { useEffect, useMemo, useRef, useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import StatCard from '../components/StatCard.jsx'
import Pagination from '../components/Pagination.jsx'
import { ErrorState, LoadingState } from '../components/StateFeedback.jsx'
import { usePolling } from '../hooks/usePolling.js'
import { MAQUINA_ID, buscarDashboard, buscarPedido, listarPedidos } from '../services/machineService.js'
import {
  ORIGENS, adaptPedidoDetalhe, adaptPedidos, formatarData, formatarDataHora, formatarDuracao,
  formatarHora, formatarPercentual,
} from '../services/adapters.js'
import { resumirPedidos } from '../services/resumoPedidos.js'
import '../styles/historico.css'

const AGRUPAMENTOS = ['Pedido', 'Hora', 'Dia']
const DIAS_PADRAO = 30
const INTERVALO_MS = 5000
const UMA_HORA_MS = 3600000

/** Cor da estação na linha do tempo do detalhe (as mesmas da tela Produção). */
const COR_ESTACAO = {
  ESTOQUE: 'var(--accent-blue)',
  MANIPULACAO: 'var(--accent-purple)',
  SEPARACAO: 'var(--accent-orange)',
}

function paraInputData(date) {
  const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return local.toISOString().slice(0, 10)
}

function diasAtras(dias) {
  const date = new Date()
  date.setDate(date.getDate() - dias)
  return paraInputData(date)
}

/** "2026-11-17" -> meia-noite local desse dia. */
function inicioDoDia(texto, somarDias = 0) {
  const date = new Date(`${texto}T00:00:00`)
  date.setDate(date.getDate() + somarDias)
  return date
}

/** Ponto de status contra a meta: verde atingiu, laranja até 10 p.p. abaixo, vermelho abaixo disso. */
function corDaMeta(oee, meta) {
  if (oee == null || meta == null) return 'gray'
  if (oee >= meta) return 'green'
  return oee >= meta - 10 ? 'orange' : 'red'
}

/** Uma linha por pedido. */
function linhaDoPedido(p) {
  return {
    chave: `p-${p.id}`,
    pedidoId: p.id,
    inicio: p.inicio,
    titulo: `nº ${p.numero}`,
    quando: formatarDataHora(p.inicio, { segundos: true }),
    origem: p.origemNome,
    situacao: p.situacao === 'CONCLUIDO' ? (p.reprovado ? 'Reprovado' : 'Aprovado') : p.situacaoNome,
    leadTimeS: p.leadTimeS,
    tempoParadoS: p.tempoParadoS,
    disponibilidade: p.disponibilidade,
    performance: p.performance,
    qualidade: p.qualidade,
    oee: p.oee,
  }
}

/**
 * Uma linha por hora ou por dia. O OEE do grupo soma os tempos dos pedidos
 * (resumirPedidos), em vez de tirar a média dos OEEs.
 */
function linhasAgrupadas(pedidos, agrupamento) {
  const grupos = new Map()
  for (const p of pedidos) {
    const inicio = new Date(p.inicio)
    if (agrupamento === 'Dia') inicio.setHours(0, 0, 0, 0)
    else inicio.setMinutes(0, 0, 0)
    const chave = inicio.getTime()
    if (!grupos.has(chave)) grupos.set(chave, { inicio, pedidos: [] })
    grupos.get(chave).pedidos.push(p)
  }
  return [...grupos.values()].map(({ inicio, pedidos: doGrupo }) => {
    const r = resumirPedidos(doGrupo)
    const origens = [...new Set(doGrupo.map((p) => p.origemNome))]
    const fim = new Date(inicio.getTime() + UMA_HORA_MS)
    return {
      chave: `${agrupamento}-${inicio.getTime()}`,
      pedidoId: null,
      inicio,
      titulo: `${doGrupo.length} ${doGrupo.length === 1 ? 'pedido' : 'pedidos'}`,
      quando: agrupamento === 'Dia'
        ? formatarData(inicio)
        : `${formatarData(inicio)} ${formatarHora(inicio, { segundos: false })}–${formatarHora(fim, { segundos: false })}`,
      origem: origens.length === 1 ? origens[0] : 'Várias',
      situacao: `${r.aprovados} aprov. · ${r.reprovados} reprov.${r.interrompidos ? ` · ${r.interrompidos} interr.` : ''}`,
      leadTimeS: r.leadTimeMedioS,
      tempoParadoS: r.tempoParadoTotalS,
      disponibilidade: r.disponibilidade,
      performance: r.performance,
      qualidade: r.qualidade,
      oee: r.oee,
    }
  })
}

const COLUNAS_ORDENAVEIS = { inicio: 'DATA / HORA', leadTimeS: 'LEAD TIME', oee: 'OEE' }

/** Detalhe do pedido: as 3 etapas numa linha do tempo, cada uma contra o padrão. */
function DetalhePedido({ pedidoId, onFechar }) {
  const ref = useRef(null)
  const { data, error, recarregar } = usePolling(
    () => buscarPedido(pedidoId).then(adaptPedidoDetalhe),
    [pedidoId],
    60000
  )

  useEffect(() => {
    ref.current?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
  }, [pedidoId])

  const detalhe = data?.pedido?.id === pedidoId ? data : null
  const pedido = detalhe?.pedido
  const etapas = detalhe?.etapas ?? []
  // Escala da linha do tempo: o maior entre o lead time e o fim do padrão de cada etapa.
  const inicioMs = pedido?.inicio?.getTime() ?? 0
  const deslocamento = (e) => (e.inicio.getTime() - inicioMs) / 1000
  const escala = Math.max(
    pedido?.leadTimeS ?? 0,
    ...etapas.map((e) => deslocamento(e) + Math.max(e.duracaoS, e.tempoPadraoS)),
    1
  )
  const pct = (s) => `${(100 * s) / escala}%`

  return (
    <div ref={ref}>
      <Panel
        title={pedido ? `PEDIDO Nº ${pedido.numero} · LINHA DO TEMPO` : 'DETALHE DO PEDIDO'}
        subtitle={pedido ? `${formatarDataHora(pedido.inicio, { segundos: true })} · ${pedido.origemNome} · ${pedido.situacaoNome}` : undefined}
        right={<button className="clear-filters-btn" onClick={onFechar}>Fechar</button>}
        style={{ marginTop: 20 }}
      >
        {!detalhe && (error
          ? <ErrorState message="Não foi possível carregar o pedido." onRetry={recarregar} />
          : <LoadingState label="Carregando pedido..." />)}

        {detalhe && (
          <>
            <div className="detalhe-resumo">
              <span>Lead time <b>{formatarDuracao(pedido.leadTimeS)}</b></span>
              <span>Padrão <b>{formatarDuracao(pedido.tempoPadraoS)}</b></span>
              <span>Parado <b>{formatarDuracao(pedido.tempoParadoS)}</b></span>
              <span>D <b>{formatarPercentual(pedido.disponibilidade)}</b></span>
              <span>P <b>{formatarPercentual(pedido.performance)}</b></span>
              <span>Q <b>{formatarPercentual(pedido.qualidade)}</b></span>
              <span>OEE <b>{formatarPercentual(pedido.oee)}</b></span>
            </div>

            {etapas.length ? (
              <ul className="detalhe-etapas">
                {etapas.map((e) => {
                  const atraso = e.duracaoS - e.tempoPadraoS
                  return (
                    <li key={`${e.estacao}-${e.inicio.getTime()}`}>
                      <span className="detalhe-etapa-nome">{e.nome}</span>
                      <div className="detalhe-trilho" aria-hidden="true">
                        <div className="detalhe-padrao" style={{ left: pct(deslocamento(e)), width: pct(e.tempoPadraoS) }} />
                        <div className="detalhe-barra"
                          style={{ left: pct(deslocamento(e)), width: pct(e.duracaoS), background: COR_ESTACAO[e.estacao] }}>
                          {e.tempoParadoS > 0 && (
                            <div className="detalhe-parado" style={{ width: `${(100 * e.tempoParadoS) / e.duracaoS}%` }} />
                          )}
                        </div>
                      </div>
                      <span className={`detalhe-etapa-tempos${atraso > 0.5 ? ' acima' : ''}`}>
                        {formatarDuracao(e.duracaoS)} / {formatarDuracao(e.tempoPadraoS)}
                        {atraso > 0.5 && ` (+${formatarDuracao(atraso)})`}
                        {e.tempoParadoS > 0 && <small> · {formatarDuracao(e.tempoParadoS)} parado</small>}
                      </span>
                    </li>
                  )
                })}
              </ul>
            ) : (
              <p className="detalhe-vazio">
                {pedido.situacao === 'EM_ANDAMENTO'
                  ? 'As etapas são gravadas quando o pedido termina.'
                  : 'O pedido não chegou a abrir nenhuma etapa.'}
              </p>
            )}

            <div className="chart-legend detalhe-legenda">
              <span><i className="detalhe-legenda-barra" /> Tempo da etapa</span>
              <span><i className="detalhe-legenda-padrao" /> Tempo padrão</span>
              <span><i className="detalhe-legenda-parado" /> Parado</span>
            </div>
          </>
        )}
      </Panel>
    </div>
  )
}

export default function Historico() {
  const [page, setPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(10)
  const [dateFrom, setDateFrom] = useState(() => diasAtras(DIAS_PADRAO))
  const [dateTo, setDateTo] = useState(() => paraInputData(new Date()))
  const [agrupamento, setAgrupamento] = useState('Pedido')
  const [origem, setOrigem] = useState('')
  const [sortBy, setSortBy] = useState('inicio')
  const [sortDirection, setSortDirection] = useState('desc')
  const [pedidoAberto, setPedidoAberto] = useState(null)

  const { data: pedidos, loading, error, atualizadoEm, recarregar } = usePolling(
    () => listarPedidos(MAQUINA_ID, {
      de: inicioDoDia(dateFrom),
      ate: inicioDoDia(dateTo, 1),
      origem: origem || undefined,
    }).then(adaptPedidos),
    [MAQUINA_ID, dateFrom, dateTo, origem],
    INTERVALO_MS
  )
  // A meta vem da API (META_OEE); ela quase nunca muda.
  const { data: metaOee } = usePolling(() => buscarDashboard(MAQUINA_ID).then((d) => d.metaOee), [MAQUINA_ID], 60000)

  // Só recalcula quando chegam pedidos novos ou um filtro da tabela muda.
  const linhas = useMemo(() => {
    const lista = pedidos ?? []
    const base = agrupamento === 'Pedido' ? lista.map(linhaDoPedido) : linhasAgrupadas(lista, agrupamento)
    const valor = (linha) => (sortBy === 'inicio' ? linha.inicio.getTime() : linha[sortBy])
    return base.sort((a, b) => {
      const [x, y] = [valor(a), valor(b)]
      // Sem valor (pedido interrompido) vai sempre para o fim.
      if (x == null || y == null) return x == null ? (y == null ? 0 : 1) : -1
      return sortDirection === 'asc' ? x - y : y - x
    })
  }, [pedidos, agrupamento, sortBy, sortDirection])

  const resumo = useMemo(() => resumirPedidos(pedidos ?? []), [pedidos])

  if (loading && !pedidos) return <LoadingState label="Carregando histórico..." />
  if (!pedidos) {
    return (
      <ErrorState
        message="Não foi possível conectar à API. Confira se o backend está rodando (porta 8080)."
        onRetry={recarregar}
      />
    )
  }

  const totalPages = Math.max(1, Math.ceil(linhas.length / itemsPerPage))
  const currentPage = Math.min(page, totalPages)
  const startItem = linhas.length ? (currentPage - 1) * itemsPerPage + 1 : 0
  const endItem = Math.min(currentPage * itemsPerPage, linhas.length)
  const visibleRows = linhas.slice(startItem ? startItem - 1 : 0, endItem)
  const porPedido = agrupamento === 'Pedido'

  const handleSort = (column) => {
    if (sortBy === column) setSortDirection((direction) => (direction === 'asc' ? 'desc' : 'asc'))
    else {
      setSortBy(column)
      setSortDirection('desc')
    }
    setPage(1)
  }
  const cabecalhoOrdenavel = (coluna, rotulo = COLUNAS_ORDENAVEIS[coluna]) => (
    <button type="button" className="table-sort" onClick={() => handleSort(coluna)}
      aria-label={`Ordenar por ${rotulo.toLowerCase()}`}>
      {rotulo} {sortBy === coluna ? (sortDirection === 'asc' ? '↑' : '↓') : '⇅'}
    </button>
  )
  const limparFiltros = () => {
    setDateFrom(diasAtras(DIAS_PADRAO))
    setDateTo(paraInputData(new Date()))
    setAgrupamento('Pedido')
    setOrigem('')
    setSortBy('inicio')
    setSortDirection('desc')
    setPage(1)
    setPedidoAberto(null)
  }

  return (
    <>
      <PageHeader systemActive={!error} title="Histórico" subtitle="Todos os pedidos da bancada, com o OEE de cada um." />

      <div className="kpi-row">
        <StatCard icon="good" label="PEDIDOS CONCLUÍDOS" value={resumo.concluidos.toLocaleString('pt-BR')}
          caption={resumo.interrompidos ? `${resumo.interrompidos} interrompido(s) fora do OEE` : 'No período selecionado'} />
        <StatCard icon="rate" label="OEE ACUMULADO" value={formatarPercentual(resumo.oee)}
          caption="Soma dos tempos de todos os pedidos" />
        <StatCard icon="total" label="LEAD TIME MÉDIO" value={formatarDuracao(resumo.leadTimeMedioS)}
          caption="Do início ao fim do pedido" />
        <StatCard icon="rejected" label="TAXA DE REPROVAÇÃO" value={formatarPercentual(resumo.taxaReprovacao)}
          caption={`${resumo.reprovados} de ${resumo.concluidos} pedidos`} />
      </div>

      <Panel className="filters-panel" style={{ marginTop: 20 }}>
        <div className="filters-row">
          <div className="filter-field">
            <label htmlFor="historico-de">PERÍODO</label>
            <div className="filter-dates">
              <div className="date-input">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" />
                </svg>
                <input id="historico-de" type="date" value={dateFrom} max={dateTo}
                  onChange={(e) => { if (e.target.value) { setDateFrom(e.target.value); setPage(1) } }} />
              </div>
              <span>até</span>
              <div className="date-input">
                <input type="date" value={dateTo} min={dateFrom} aria-label="Até"
                  onChange={(e) => { if (e.target.value) { setDateTo(e.target.value); setPage(1) } }} />
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" />
                </svg>
              </div>
            </div>
          </div>

          <div className="filter-field">
            <label htmlFor="historico-agrupar">AGRUPAR POR</label>
            <select id="historico-agrupar" value={agrupamento}
              onChange={(e) => { setAgrupamento(e.target.value); setPage(1); setPedidoAberto(null) }}>
              {AGRUPAMENTOS.map((a) => <option key={a}>{a}</option>)}
            </select>
          </div>

          <div className="filter-field">
            <label htmlFor="historico-origem">ORIGEM</label>
            <select id="historico-origem" value={origem} onChange={(e) => { setOrigem(e.target.value); setPage(1) }}>
              <option value="">Todas</option>
              {ORIGENS.map((o) => <option key={o.codigo} value={o.codigo}>{o.nome}</option>)}
            </select>
          </div>

          <button className="clear-filters-btn" onClick={limparFiltros}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 6h16M8 6V4h8v2M6 6l1 14h10l1-14" />
            </svg>
            Limpar filtros
          </button>
        </div>
      </Panel>

      <Panel title={porPedido ? 'PEDIDOS' : `PEDIDOS POR ${agrupamento.toUpperCase()}`} style={{ marginTop: 20 }}
        subtitle={metaOee != null ? `Ponto de status: OEE contra a meta de ${formatarPercentual(metaOee, 0)}` : undefined}>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>{porPedido ? 'PEDIDO' : 'PEDIDOS'}</th>
                <th>{cabecalhoOrdenavel('inicio', porPedido ? 'DATA / HORA' : 'PERÍODO')}</th>
                <th>ORIGEM</th>
                <th>{porPedido ? 'SITUAÇÃO' : 'RESULTADO'}</th>
                <th>{cabecalhoOrdenavel('leadTimeS', porPedido ? 'LEAD TIME' : 'LEAD TIME MÉDIO')}</th>
                <th>{porPedido ? 'TEMPO PARADO' : 'PARADO (TOTAL)'}</th>
                <th>D</th>
                <th>P</th>
                <th>Q</th>
                <th>{cabecalhoOrdenavel('oee')}</th>
                <th aria-label="Detalhe" />
              </tr>
            </thead>
            <tbody>
              {visibleRows.map((row) => (
                <tr key={row.chave} className={row.pedidoId != null && row.pedidoId === pedidoAberto ? 'linha-aberta' : ''}>
                  <td>
                    <span className="row-status" title={row.oee == null ? 'Sem OEE (pedido não concluído)' : undefined}>
                      <span className={`dot dot-${corDaMeta(row.oee, metaOee)}`} />
                      {row.titulo}
                    </span>
                  </td>
                  <td>{row.quando}</td>
                  <td>{row.origem}</td>
                  <td>{row.situacao}</td>
                  <td>{formatarDuracao(row.leadTimeS)}</td>
                  <td>{formatarDuracao(row.tempoParadoS)}</td>
                  <td>{formatarPercentual(row.disponibilidade)}</td>
                  <td>{formatarPercentual(row.performance)}</td>
                  <td>{formatarPercentual(row.qualidade)}</td>
                  <td><b>{formatarPercentual(row.oee)}</b></td>
                  <td>
                    {row.pedidoId != null && (
                      <button className="chevron-btn" aria-label={`Ver detalhes do pedido ${row.titulo}`}
                        onClick={() => setPedidoAberto((aberto) => (aberto === row.pedidoId ? null : row.pedidoId))}>
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                          <path d="M9 18l6-6-6-6" />
                        </svg>
                      </button>
                    )}
                  </td>
                </tr>
              ))}
              {!visibleRows.length && (
                <tr>
                  <td colSpan={11} className="tabela-vazia">Nenhum pedido no período e na origem escolhidos.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <Pagination
          page={currentPage}
          totalPages={totalPages}
          onChange={setPage}
          itemsPerPage={itemsPerPage}
          onItemsPerPageChange={(n) => {
            setItemsPerPage(n)
            setPage(1)
          }}
          totalItems={linhas.length}
          startItem={startItem}
          endItem={endItem}
        />
      </Panel>

      {pedidoAberto != null && porPedido && (
        <DetalhePedido pedidoId={pedidoAberto} onFechar={() => setPedidoAberto(null)} />
      )}

      <p className="updated-note">Dados da API · atualizado às {formatarHora(atualizadoEm)}</p>
    </>
  )
}
