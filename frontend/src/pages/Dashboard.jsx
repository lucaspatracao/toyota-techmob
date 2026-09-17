import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import { LoadingState } from '../components/StateFeedback.jsx'
import { usePolling } from '../hooks/usePolling.js'
import { buscarDashboard } from '../services/machineService.js'
import { adaptDashboard } from '../services/adapters.js'
import { periodSummary as mockPeriodSummary } from '../data/mockData.js'
import '../styles/dashboard.css'

// ID da máquina — hoje só existe a Bancada Smart 4.0, então fixamos 1.
// Quando houver seleção de máquina na UI, isso vira estado/prop.
const MAQUINA_ID = import.meta.env.VITE_MAQUINA_ID || 1

function KpiCard({ label, value, unit, trend, caption, featured, progress, refreshKey }) {
  return (
    <div key={refreshKey} className={`panel kpi-card${featured ? ' kpi-card-featured' : ''}`}>
      <div className="kpi-card-top">
        <span className="kpi-label">{label}</span>
        {trend && (
          <span className={`kpi-trend ${trend.direction}`}>
            {trend.direction === 'up' ? '↑' : '↓'} {trend.value}
          </span>
        )}
      </div>
      <div className="kpi-value">
        {value}
        {unit && <span className="kpi-value-unit">{unit}</span>}
      </div>
      {progress !== undefined && (
        <div className="kpi-progress-track">
          <div className="kpi-progress-fill" style={{ width: `${Math.min(100, Math.max(0, progress))}%` }} />
        </div>
      )}
      <div className="kpi-caption">{caption}</div>
    </div>
  )
}

/** Mini gráfico de área da evolução do OEE — decorativo, a partir do resumo do período. */
function TrendChart({ points }) {
  const w = 640
  const h = 190
  const max = 100
  const safePoints = points.length ? points : [0, 0]
  const step = safePoints.length > 1 ? w / (safePoints.length - 1) : w
  const coords = safePoints.map((v, i) => [i * step, h - (v / max) * h])

  const buildSmoothPath = (values) => {
    if (!values.length) return ''
    if (values.length === 1) return `M ${values[0][0]} ${values[0][1]}`

    let d = `M ${values[0][0].toFixed(1)} ${values[0][1].toFixed(1)}`

    for (let i = 1; i < values.length; i++) {
      const prev = values[i - 1]
      const curr = values[i]
      const cx = (prev[0] + curr[0]) / 2
      d += ` Q ${prev[0].toFixed(1)} ${prev[1].toFixed(1)} ${cx.toFixed(1)} ${(prev[1] + curr[1]) / 2}`
      d += ` T ${curr[0].toFixed(1)} ${curr[1].toFixed(1)}`
    }

    return d
  }

  const linePath = buildSmoothPath(coords)
  const areaPath = `${linePath} L ${w} ${h} L 0 ${h} Z`

  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="dash-trend-svg" preserveAspectRatio="none">
      <defs>
        <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--content-accent)" stopOpacity="0.35" />
          <stop offset="100%" stopColor="var(--content-accent)" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={areaPath} fill="url(#trendFill)" style={{ transition: 'all 0.7s ease' }} />
      <path
        d={linePath}
        fill="none"
        stroke="var(--content-accent)"
        strokeWidth="2.5"
        strokeLinecap="round"
        style={{ transition: 'all 0.7s ease' }}
      />
      {coords.map(([x, y], index) => (
        <circle
          key={`${x}-${y}-${index}`}
          cx={x}
          cy={y}
          r={index === coords.length - 1 ? 4 : 2.5}
          fill="var(--content-accent)"
          stroke="#fff"
          strokeWidth="2"
          style={{ transition: 'all 0.7s ease' }}
        />
      ))}
    </svg>
  )
}

function fmtPct(v) {
  return v === undefined || v === null ? '-' : `${v.toFixed(1).replace('.', ',')}%`
}

export default function Dashboard({ simulationEnabled = true, simulationState }) {
  const { data, loading, error } = usePolling(
    () => buscarDashboard(MAQUINA_ID).then(adaptDashboard),
    [MAQUINA_ID, simulationEnabled],
    5000
  )

  const usingFallback = simulationEnabled && Boolean(error) && !data && !simulationState
  const simulatedData = simulationState ?? {
    oee: 78.4,
    disponibilidade: 92.1,
    performance: 84.7,
    qualidade: 99.6,
    resumo: { boas: 1248, rejeitadas: 62, tempoCicloMedio: 12.8 },
    oeeSeries: [
      { time: '00:00', oee: 72 },
      { time: '02:00', oee: 74 },
      { time: '04:00', oee: 76 },
      { time: '06:00', oee: 78 },
      { time: '08:00', oee: 79 },
      { time: '10:00', oee: 80 },
      { time: '12:00', oee: 82 },
      { time: '14:00', oee: 83 },
      { time: '16:00', oee: 80 },
      { time: '18:00', oee: 79 },
      { time: '20:00', oee: 82 },
      { time: '22:00', oee: 85 },
    ],
    periodSummary: mockPeriodSummary,
    updatedAt: new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' }),
  }
  const d = simulationEnabled ? (simulationState ?? data ?? simulatedData) : (data ?? simulatedData)

  // Sem indicador da SMART 4.0, mantém a tela em espera e não monta os gráficos.
  if (!simulationEnabled && !d) {
    return <LoadingState label={error ? 'Aguardando dados da SMART 4.0...' : 'Buscando dados da SMART 4.0...'} />
  }

  if (loading && !data && !simulationState) return <LoadingState label="Carregando dashboard..." />

  const trendSeries = d?.oeeSeries ?? [
    { time: '00:00', oee: 72 },
    { time: '02:00', oee: 74 },
    { time: '04:00', oee: 76 },
    { time: '06:00', oee: 78 },
    { time: '08:00', oee: 79 },
    { time: '10:00', oee: 80 },
    { time: '12:00', oee: 82 },
    { time: '14:00', oee: 83 },
    { time: '16:00', oee: 80 },
    { time: '18:00', oee: 79 },
    { time: '20:00', oee: 82 },
    { time: '22:00', oee: 85 },
  ]
  const trendPoints = trendSeries.map((point) => Number(point.oee ?? point.value ?? 0))

  const updatedAt = simulationEnabled ? (simulationState?.updatedAt || new Date().toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit' })) : 'Atualizado agora'
  const refreshKey = `${updatedAt}-${d?.oee ?? 0}-${d?.disponibilidade ?? 0}-${d?.performance ?? 0}-${d?.qualidade ?? 0}`

  const makeTrend = (currentValue, previousValue, direction = 'up') => {
    if (currentValue === undefined || currentValue === null) return null
    const reference = previousValue ?? currentValue
    const delta = Math.abs(Number((currentValue - reference).toFixed(1)))
    return {
      direction,
      value: `${delta.toFixed(1).replace('.', ',')}%`,
    }
  }

  const lastTrendValue = trendSeries.at(-1)?.oee ?? d?.oee ?? 0
  const previousTrendValue = trendSeries.at(-2)?.oee ?? lastTrendValue

  const oeeTrend = makeTrend(d?.oee ?? 0, previousTrendValue, 'up')
  const disponibilidadeTrend = makeTrend(d?.disponibilidade ?? 0, d?.disponibilidade ?? 0, 'up')
  const performanceTrend = makeTrend(d?.performance ?? 0, d?.performance ?? 0, 'down')
  const qualidadeTrend = makeTrend(d?.qualidade ?? 0, d?.qualidade ?? 0, 'up')

  const statusProduzindo = `${Math.max(60, Math.round((d.resumo?.boas ?? 1100) / 9)).toLocaleString('pt-BR')} un/h`
  const statusCiclo = `${(d.resumo?.tempoCicloMedio ?? 12.5).toFixed(1).replace('.', ',')} seg`
  const statusUltimoCiclo = `${Math.max(8, Math.round((d.resumo?.tempoCicloMedio ?? 12.5) * 0.9))} segundos`
  const eficienciaTurno = d?.oee ?? 0

  return (
    <div className="dashboard-page">
      <PageHeader
        breadcrumb="OPERAÇÃO / DASHBOARD OEE"
        eyebrow="MONITORAMENTO DE EFICIÊNCIA FABRIL"
        title="Bom dia, equipe ↗"
        subtitle="Acompanhe a performance da Bancada Smart 4.0 em tempo quase real."
        right={
          <>
            <select className="select-control" defaultValue="bancada-1">
              <option value="bancada-1">Bancada Smart 01</option>
            </select>
            <select className="select-control" defaultValue="24h">
              <option value="24h">Últimas 24h</option>
              <option value="7d">Últimos 7 dias</option>
            </select>
          </>
        }
      />

      {usingFallback && (
        <div className="api-fallback-banner">
          <svg
            className="api-fallback-banner-icon"
            width="20"
            height="20"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            strokeWidth="2"
          >
            <path d="M10.29 3.86 1.82 18a1 1 0 0 0 .86 1.5h18.64a1 1 0 0 0 .86-1.5L13.71 3.86a1 1 0 0 0-1.72 0Z" />
            <path d="M12 9v4M12 17h.01" />
          </svg>
          <div className="api-fallback-banner-text">
            <strong>Modo de demonstração — dados locais sendo exibidos</strong>
            <span>Não foi possível conectar à API ({String(error?.message)})</span>
          </div>
          <button className="api-fallback-reconnect" onClick={() => window.location.reload()}>
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 12a9 9 0 1 1-3-6.7" />
              <path d="M21 3v6h-6" />
            </svg>
            Reconectar
          </button>
        </div>
      )}

      <div className="kpi-row">
        <KpiCard
          label="OEE CONSOLIDADO"
          value={fmtPct(d.oee)}
          caption="Meta do turno: 75%"
          trend={oeeTrend}
          progress={d.oee ?? 0}
          featured
          refreshKey={refreshKey}
        />
        <KpiCard
          label="DISPONIBILIDADE"
          value={fmtPct(d.disponibilidade)}
          caption={`Tempo operacional  ${d.tempoOperacional ?? '7h 22min'}`}
          trend={disponibilidadeTrend}
          refreshKey={refreshKey}
        />
        <KpiCard
          label="PERFORMANCE"
          value={fmtPct(d.performance)}
          caption={`Ciclo médio  ${d.resumo?.tempoCicloMedio?.toFixed?.(1)?.replace('.', ',') ?? '12,8'}s`}
          trend={performanceTrend}
          refreshKey={refreshKey}
        />
        <KpiCard
          label="QUALIDADE"
          value={fmtPct(d.qualidade)}
          caption={`Peças boas  ${(d.resumo?.boas ?? 0).toLocaleString('pt-BR')}`}
          trend={qualidadeTrend}
          refreshKey={refreshKey}
        />
      </div>

      <div className="dash-top-row">
        <Panel
          title="TENDÊNCIA"
          right={<span className="dash-trend-legend"><span className="dot" style={{ background: 'var(--content-accent)' }} /> OEE por hora</span>}
        >
          <div className="dash-trend-title">Evolução do OEE</div>
          <TrendChart points={trendPoints} />
        </Panel>

        <Panel
          title="STATUS DA LINHA"
          right={<span className="status-online-pill"><span className="dot dot-green" /> Em produção</span>}
        >
          <div className="status-line-title">Agora</div>
          <div className="status-line-machine">
            <span className="status-line-machine-icon">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                <rect x="3" y="3" width="18" height="18" rx="3" />
                <rect x="9" y="9" width="6" height="6" rx="1" />
              </svg>
            </span>
            <div>
              <div className="status-line-machine-name">Bancada Smart 01</div>
              <div className="status-line-machine-sub" key={`cycle-${refreshKey}`}>Último ciclo há {statusUltimoCiclo}</div>
            </div>
          </div>
          <div className="status-line-metric" key={`prod-${refreshKey}`}>
            <span>Produção atual</span>
            <b>{statusProduzindo}</b>
          </div>
          <div className="status-line-metric" key={`ciclo-${refreshKey}`}>
            <span>Tempo de ciclo</span>
            <b>{statusCiclo}</b>
          </div>
          <div className="status-line-metric" key={`eficiencia-${refreshKey}`}>
            <span>Eficiência do turno</span>
            <b className="accent">{fmtPct(eficienciaTurno)}</b>
          </div>
        </Panel>
      </div>

      <p className="updated-note">Dados atualizados em tempo real pela simulação · {updatedAt}</p>
    </div>
  )
}
