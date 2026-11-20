import { useState } from 'react'
import {
  ComposedChart, Bar, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Cell,
} from 'recharts'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import StatCard from '../components/StatCard.jsx'
import SummaryDonut from '../components/SummaryDonut.jsx'
import { ErrorState, LoadingState } from '../components/StateFeedback.jsx'
import { usePolling } from '../hooks/usePolling.js'
import { MAQUINA_ID, listarPedidos } from '../services/machineService.js'
import { ESTACOES, adaptPedidos, formatarDuracao, formatarHora, formatarPercentual } from '../services/adapters.js'
import { mediaPorEstacao, resumirPedidos } from '../services/resumoPedidos.js'
import '../styles/producao.css'

/** Faixas do seletor: a partir de quando buscar os pedidos (null = todos). */
const FAIXAS = [
  { id: 'HOJE', rotulo: 'Hoje', titulo: 'hoje', de: () => { const d = new Date(); d.setHours(0, 0, 0, 0); return d } },
  { id: '7D', rotulo: '7D', titulo: 'últimos 7 dias', de: () => new Date(Date.now() - 7 * 864e5) },
  { id: '30D', rotulo: '30D', titulo: 'últimos 30 dias', de: () => new Date(Date.now() - 30 * 864e5) },
  { id: 'TUDO', rotulo: 'Tudo', titulo: 'todos os pedidos', de: () => null },
]

/** O gráfico de etapas mostra no máximo estes pedidos (os mais recentes da faixa). */
const MAX_PEDIDOS_GRAFICO = 30

/** A lista de pedidos é maior que o estado ao vivo: consulta a cada 5 s. */
const INTERVALO_MS = 5000

/** Cor de cada estação nos gráficos (tokens do global.css). */
const COR_ESTACAO = {
  ESTOQUE: 'var(--accent-blue)',
  MANIPULACAO: 'var(--accent-purple)',
  SEPARACAO: 'var(--accent-orange)',
}

const ESTILO_TOOLTIP = {
  background: 'var(--bg-card)', color: 'var(--text-primary)', border: '1px solid var(--border-subtle)',
  borderRadius: 8, fontSize: 12, boxShadow: 'var(--shadow-md)',
}

const segundos = (valor) => formatarDuracao(valor)

export default function Producao({ planta = null }) {
  const [faixaId, setFaixaId] = useState('HOJE')
  const faixa = FAIXAS.find((f) => f.id === faixaId)

  const { data: pedidos, loading, error, atualizadoEm, recarregar } = usePolling(
    () => listarPedidos(MAQUINA_ID, { de: faixa.de() }).then(adaptPedidos),
    [MAQUINA_ID, faixaId],
    INTERVALO_MS
  )

  if (loading && !pedidos) return <LoadingState label="Carregando produção..." />
  if (!pedidos) {
    return (
      <ErrorState
        message="Não foi possível conectar à API. Confira se o backend está rodando (porta 8080)."
        onRetry={recarregar}
      />
    )
  }

  const resumo = resumirPedidos(pedidos)
  const concluidos = pedidos.filter((p) => p.situacao === 'CONCLUIDO')

  // Um grupo por pedido, do mais antigo ao mais recente: barras empilhadas
  // com o tempo de cada estação e a linha do tempo padrão do pedido.
  const porPedido = concluidos.slice(0, MAX_PEDIDOS_GRAFICO).reverse().map((p) => ({
    pedido: `nº ${p.numero}`,
    ...Object.fromEntries(ESTACOES.map((e) => [e.codigo, p.duracaoEtapasS[e.codigo] ?? 0])),
    padrao: p.tempoPadraoS,
    leadTime: p.leadTimeS,
  }))

  // Tempo médio de cada estação contra o padrão dela (cronometragem atual,
  // vinda do estado ao vivo). Gargalo: a estação que mais passa do padrão.
  const padraoDa = Object.fromEntries((planta?.estacoes ?? []).map((e) => [e.estacao, e.tempoPadraoS]))
  const porEstacao = mediaPorEstacao(pedidos, ESTACOES).map((e) => {
    const padrao = padraoDa[e.estacao] ?? null
    return { ...e, padrao, diferenca: e.media != null && padrao != null ? e.media - padrao : null }
  })
  const gargalo = porEstacao
    .filter((e) => e.diferenca != null && e.diferenca > 0)
    .sort((a, b) => b.diferenca - a.diferenca)[0]?.estacao ?? null

  const legenda = faixa.titulo.charAt(0).toUpperCase() + faixa.titulo.slice(1)
  const pctDe = (n) => (resumo.concluidos ? formatarPercentual((100 * n) / resumo.concluidos) : '-')

  return (
    <>
      <PageHeader
        systemActive={!error}
        title="Produção"
        subtitle="Pedidos da bancada, o tempo de cada etapa e onde o pedido atrasou."
        right={
          <button className="pill pill-refresh" onClick={recarregar} title="Atualizar agora">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 12a9 9 0 1 1-3-6.7" />
              <path d="M21 3v6h-6" />
            </svg>
            Atualizado às {formatarHora(atualizadoEm)}
          </button>
        }
      />

      <div className="kpi-row">
        <StatCard icon="total" label="PEDIDOS CONCLUÍDOS" value={resumo.concluidos.toLocaleString('pt-BR')}
          caption={resumo.interrompidos ? `${legenda} · ${resumo.interrompidos} interrompido(s)` : legenda} />
        <StatCard icon="good" label="PEÇAS PRODUZIDAS" value={resumo.pecasProduzidas.toLocaleString('pt-BR')} unit="peças"
          caption="3 peças por pedido" />
        <StatCard icon="rejected" label="PEDIDOS REPROVADOS" value={resumo.reprovados.toLocaleString('pt-BR')}
          caption={legenda} />
        <StatCard icon="rate" label="TAXA DE REPROVAÇÃO" value={formatarPercentual(resumo.taxaReprovacao)}
          caption={legenda} />
      </div>

      <div className="grid-row" style={{ marginTop: 20, alignItems: 'stretch' }}>
        <Panel
          className="chart-panel"
          title="TEMPO DE CADA ETAPA POR PEDIDO"
          subtitle={concluidos.length > MAX_PEDIDOS_GRAFICO ? `Os ${MAX_PEDIDOS_GRAFICO} pedidos mais recentes do período` : undefined}
          right={
            <div className="range-toggle" role="group" aria-label="Período">
              {FAIXAS.map((f) => (
                <button key={f.id} className={f.id === faixaId ? 'active' : ''} aria-pressed={f.id === faixaId}
                  onClick={() => setFaixaId(f.id)}>
                  {f.rotulo}
                </button>
              ))}
            </div>
          }
        >
          <div className="chart-legend">
            {ESTACOES.map((e) => (
              <span key={e.codigo}><i className="dot" style={{ background: COR_ESTACAO[e.codigo] }} /> {e.nome}</span>
            ))}
            <span><i className="dash-legend dash-legend-padrao" /> Tempo padrão do pedido</span>
          </div>
          {porPedido.length ? (
            <ResponsiveContainer width="100%" height={260}>
              <ComposedChart data={porPedido} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                <CartesianGrid stroke="var(--chart-grid)" vertical={false} />
                <XAxis dataKey="pedido" stroke="var(--chart-axis)" fontSize={11} tickLine={false} axisLine={false} />
                <YAxis stroke="var(--chart-axis)" fontSize={11} tickLine={false} axisLine={false} unit=" s" />
                <Tooltip contentStyle={ESTILO_TOOLTIP} formatter={(valor, nome) => [segundos(valor), nome]} />
                {ESTACOES.map((e, i) => (
                  <Bar key={e.codigo} dataKey={e.codigo} name={e.nome} stackId="etapas" fill={COR_ESTACAO[e.codigo]}
                    radius={i === ESTACOES.length - 1 ? [2, 2, 0, 0] : [0, 0, 0, 0]} />
                ))}
                <Line type="stepAfter" dataKey="padrao" name="Tempo padrão" stroke="var(--text-primary)"
                  strokeDasharray="5 4" dot={false} strokeWidth={1.5} />
              </ComposedChart>
            </ResponsiveContainer>
          ) : (
            <p className="chart-vazio">Nenhum pedido concluído {faixa.titulo === 'todos os pedidos' ? 'ainda' : `(${faixa.titulo})`}.</p>
          )}
        </Panel>

        <Panel title={`RESUMO (${faixa.titulo.toUpperCase()})`} className="summary-panel">
          <div className="summary-donut-wrap">
            <SummaryDonut good={resumo.aprovados} rejected={resumo.reprovados} size={190} stroke={18} unit="pedidos" />
            <ul className="summary-legend">
              <li><span className="dot dot-green" /> Aprovados <b>{resumo.aprovados} ({pctDe(resumo.aprovados)})</b></li>
              <li><span className="dot dot-red" /> Reprovados <b>{resumo.reprovados} ({pctDe(resumo.reprovados)})</b></li>
              <li><span className="dot dot-gray" /> Interrompidos <b>{resumo.interrompidos}</b></li>
            </ul>
          </div>
          <div className="summary-footer-row">
            <span>Lead time médio</span>
            <b>{formatarDuracao(resumo.leadTimeMedioS)}</b>
          </div>
        </Panel>
      </div>

      <Panel title="TEMPO MÉDIO POR ETAPA x PADRÃO" style={{ marginTop: 20 }}>
        <div className="chart-legend">
          <span><i className="dot" style={{ background: 'var(--content-accent)' }} /> Tempo médio</span>
          <span><i className="dot" style={{ background: 'var(--border-strong)' }} /> Tempo padrão</span>
          <span><i className="dot dot-red" /> Gargalo</span>
        </div>
        <div className="etapas-grid">
          <ResponsiveContainer width="100%" height={200}>
            <BarChart data={porEstacao} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
              <CartesianGrid stroke="var(--chart-grid)" vertical={false} />
              <XAxis dataKey="nome" stroke="var(--chart-axis)" fontSize={11} tickLine={false} axisLine={false} />
              <YAxis stroke="var(--chart-axis)" fontSize={11} tickLine={false} axisLine={false} unit=" s" />
              <Tooltip contentStyle={ESTILO_TOOLTIP} formatter={(valor, nome) => [segundos(valor), nome]} />
              <Bar dataKey="media" name="Tempo médio" radius={[2, 2, 0, 0]}>
                {porEstacao.map((e) => (
                  <Cell key={e.estacao} fill={e.estacao === gargalo ? 'var(--chart-rejected)' : 'var(--content-accent)'} />
                ))}
              </Bar>
              <Bar dataKey="padrao" name="Tempo padrão" fill="var(--border-strong)" radius={[2, 2, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>

          <ul className="etapas-lista">
            {porEstacao.map((e) => (
              <li key={e.estacao} className={e.estacao === gargalo ? 'gargalo' : ''}>
                <span className="etapas-nome">
                  {e.nome}
                  {e.estacao === gargalo && <span className="selo-gargalo">gargalo</span>}
                </span>
                <span className="etapas-tempos">
                  {segundos(e.media)} <small>/ padrão {segundos(e.padrao)}</small>
                </span>
                <span className={`etapas-diferenca${e.diferenca > 0 ? ' acima' : ''}`}>
                  {e.diferenca == null ? '-' : `${e.diferenca > 0 ? '+' : e.diferenca < 0 ? '−' : ''}${segundos(Math.abs(e.diferenca))}`}
                </span>
              </li>
            ))}
          </ul>
        </div>
      </Panel>

      <p className="updated-note">
        {planta?.fonteNome ? `Fonte: ${planta.fonteNome}` : 'Sem leitura da bancada'} · atualizado às {formatarHora(atualizadoEm)}
      </p>
    </>
  )
}
