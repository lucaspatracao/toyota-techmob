import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import ArcGauge from '../components/ArcGauge.jsx'
import ProgressBar from '../components/ProgressBar.jsx'
import SummaryDonut from '../components/SummaryDonut.jsx'
import { LoadingState, ErrorState } from '../components/StateFeedback.jsx'
import { usePolling } from '../hooks/usePolling.js'
import { buscarDashboard } from '../services/machineService.js'
import { adaptDashboard } from '../services/adapters.js'
import { oeeSeries as mockOeeSeries, periodSummary as mockPeriodSummary } from '../data/mockData.js'
import '../styles/dashboard.css'

// ID da máquina — hoje só existe a Bancada Smart 4.0, então fixamos 1.
// Quando houver seleção de máquina na UI, isso vira estado/prop.
const MAQUINA_ID = import.meta.env.VITE_MAQUINA_ID || 1

function KpiCard({ label, gauge, valueNode, trend, caption }) {
  return (
    <div className="panel kpi-card">
      <div className="kpi-label">{label}</div>
      <div className="kpi-main">
        {gauge}
        {valueNode}
      </div>
      <div className="kpi-caption">
        {caption}
        {trend && (
          <span className={`stat-trend ${trend.direction}`}>
            {trend.direction === 'up' ? '↑' : '↓'} {trend.value}
            <span className="stat-trend-label"> vs ontem</span>
          </span>
        )}
      </div>
    </div>
  )
}

function fmtPct(v) {
  return v === undefined || v === null ? '-' : `${v.toFixed(1).replace('.', ',')}%`
}

export default function Dashboard() {
  const { data, loading, error } = usePolling(
    () => buscarDashboard(MAQUINA_ID).then(adaptDashboard),
    [MAQUINA_ID],
    5000
  )

  // Enquanto a API real não está disponível (ou falha), caímos para os
  // dados mock — assim a tela nunca fica vazia durante o desenvolvimento
  // visual. Quando a API estiver 100% integrada, essa checagem de `error`
  // pode ser trocada por <ErrorState onRetry={...} /> puro, sem fallback.
  const usingFallback = Boolean(error) && !data
  const d = data ?? {
    oee: 78.4,
    disponibilidade: 85.7,
    performance: 81.2,
    qualidade: 94.8,
    oeeSeries: mockOeeSeries,
    resumo: { boas: 1186, rejeitadas: 62, tempoCicloMedio: 8.42 },
    periodSummary: mockPeriodSummary,
  }

  if (loading && !data) return <LoadingState label="Carregando dashboard..." />

  const totalResumo = (d.resumo?.boas ?? 0) + (d.resumo?.rejeitadas ?? 0)
  const pctBoas = totalResumo ? ((d.resumo.boas / totalResumo) * 100).toFixed(1) : '0.0'
  const pctRej = totalResumo ? ((d.resumo.rejeitadas / totalResumo) * 100).toFixed(1) : '0.0'

  return (
    <>
      <PageHeader title="Dashboard" subtitle="Visão geral do desempenho da máquina em tempo real." />

      {usingFallback && (
        <p className="api-fallback-note">
          Não foi possível conectar à API ({String(error?.message)}) — exibindo dados de exemplo.
        </p>
      )}

      <div className="kpi-row">
        <KpiCard
          label="OEE"
          gauge={<ArcGauge value={d.oee ?? 0} />}
          caption="Últimas 24 horas"
          trend={{ direction: 'up', value: '6,2%' }}
        />
        <KpiCard
          label="DISPONIBILIDADE"
          valueNode={<div className="kpi-linear-value">{fmtPct(d.disponibilidade)}</div>}
          gauge={<ProgressBar value={d.disponibilidade ?? 0} color="var(--accent-green)" />}
          caption="Últimas 24 horas"
          trend={{ direction: 'up', value: '4,3%' }}
        />
        <KpiCard
          label="PERFORMANCE"
          valueNode={<div className="kpi-linear-value">{fmtPct(d.performance)}</div>}
          gauge={<ProgressBar value={d.performance ?? 0} color="var(--accent-blue-light)" />}
          caption="Últimas 24 horas"
          trend={{ direction: 'up', value: '3,1%' }}
        />
        <KpiCard
          label="QUALIDADE"
          valueNode={<div className="kpi-linear-value">{fmtPct(d.qualidade)}</div>}
          gauge={<ProgressBar value={d.qualidade ?? 0} color="var(--accent-green)" />}
          caption="Últimas 24 horas"
          trend={{ direction: 'up', value: '2,7%' }}
        />
      </div>

      <div className="grid-row" style={{ marginTop: 20, alignItems: 'stretch' }}>
        <Panel
          className="chart-panel"
          title="OEE AO LONGO DO TEMPO"
          subtitle="Últimas 24 horas"
          right={
            <select className="select-control">
              <option>Últimas 24 horas</option>
              <option>Últimas 7 dias</option>
              <option>Últimos 30 dias</option>
            </select>
          }
        >
          <ResponsiveContainer width="100%" height={280}>
            <AreaChart data={d.oeeSeries} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
              <defs>
                <linearGradient id="oeeFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#4d8bff" stopOpacity={0.4} />
                  <stop offset="100%" stopColor="#4d8bff" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid stroke="#1f2636" vertical={false} />
              <XAxis dataKey="time" stroke="#5c6478" fontSize={11} tickLine={false} axisLine={false} interval={3} />
              <YAxis
                stroke="#5c6478"
                fontSize={11}
                tickLine={false}
                axisLine={false}
                domain={[0, 100]}
                tickFormatter={(v) => `${v}%`}
              />
              <Tooltip
                contentStyle={{ background: '#131826', border: '1px solid #1f2636', borderRadius: 8, fontSize: 12 }}
                formatter={(v) => [`${v}%`, 'OEE']}
              />
              <Area type="monotone" dataKey="oee" stroke="#4d8bff" strokeWidth={2} fill="url(#oeeFill)" dot={{ r: 3, fill: '#4d8bff' }} />
            </AreaChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="RESUMO (ÚLTIMAS 24 HORAS)" className="summary-panel">
          <div className="summary-donut-wrap">
            <SummaryDonut good={d.resumo?.boas ?? 0} rejected={d.resumo?.rejeitadas ?? 0} />
            <ul className="summary-legend">
              <li>
                <span className="dot dot-green" /> Peças boas <b>{(d.resumo?.boas ?? 0).toLocaleString('pt-BR')} ({pctBoas}%)</b>
              </li>
              <li>
                <span className="dot dot-red" /> Peças rejeitadas <b>{(d.resumo?.rejeitadas ?? 0).toLocaleString('pt-BR')} ({pctRej}%)</b>
              </li>
              <li>
                <span className="dot dot-orange" /> Taxa de rejeição <b>{pctRej}%</b>
              </li>
            </ul>
          </div>
          <div className="summary-footer-row">
            <span>Tempo de ciclo médio</span>
            <b>{d.resumo?.tempoCicloMedio?.toFixed?.(2)?.replace('.', ',') ?? '-'} s</b>
          </div>
        </Panel>
      </div>

      <Panel title="PRODUÇÃO (ÚLTIMAS 24 HORAS)" className="table-panel" style={{ marginTop: 20 }}>
        {error && !data ? (
          <ErrorState message="Não foi possível carregar a tabela de produção." />
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>PERÍODO</th>
                  <th>PEÇAS BOAS</th>
                  <th>PEÇAS REJEITADAS</th>
                  <th>TAXA DE REJEIÇÃO</th>
                  <th>TEMPO DE CICLO MÉDIO</th>
                  <th>OEE</th>
                </tr>
              </thead>
              <tbody>
                {d.periodSummary.map((row) => (
                  <tr key={row.periodo}>
                    <td>{row.periodo}</td>
                    <td>{row.boas}</td>
                    <td>{row.rejeitadas}</td>
                    <td>{row.taxa}</td>
                    <td>{row.ciclo}</td>
                    <td>{row.oee}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Panel>

      <p className="updated-note">Dados atualizados a cada 5 segundos via MQTT.</p>
    </>
  )
}
