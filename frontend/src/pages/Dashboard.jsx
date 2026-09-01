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

function KpiCard({ label, value, unit, trend, caption, featured, progress }) {
  return (
    <div className={`panel kpi-card${featured ? ' kpi-card-featured' : ''}`}>
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
  const step = w / (points.length - 1)
  const coords = points.map((v, i) => [i * step, h - (v / max) * h])
  const linePath = coords.map(([x, y], i) => `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`).join(' ')
  const areaPath = `${linePath} L${w},${h} L0,${h} Z`
  const last = coords[coords.length - 1]
  const mid = coords[Math.floor(coords.length * 0.6)]

  return (
    <svg viewBox={`0 0 ${w} ${h}`} className="dash-trend-svg" preserveAspectRatio="none">
      <defs>
        <linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--content-accent)" stopOpacity="0.35" />
          <stop offset="100%" stopColor="var(--content-accent)" stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={areaPath} fill="url(#trendFill)" />
      <path d={linePath} fill="none" stroke="var(--content-accent)" strokeWidth="2.5" strokeLinecap="round" />
      <circle cx={mid[0]} cy={mid[1]} r="4" fill="var(--content-accent)" stroke="#fff" strokeWidth="2" />
      <circle cx={last[0]} cy={last[1]} r="4" fill="var(--content-accent)" stroke="#fff" strokeWidth="2" />
    </svg>
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
    disponibilidade: 92.1,
    performance: 84.7,
    qualidade: 99.6,
    resumo: { boas: 1248, rejeitadas: 62, tempoCicloMedio: 12.8 },
    periodSummary: mockPeriodSummary,
  }

  if (loading && !data) return <LoadingState label="Carregando dashboard..." />

  const trendPoints = [42, 46, 44, 50, 55, 53, 58, 62, 60, 66, 70, 68, 74, 78, d.oee ?? 78]

  return (
    <div>
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
          trend={{ direction: 'up', value: '4,8%' }}
          progress={d.oee ?? 0}
          featured
        />
        <KpiCard
          label="DISPONIBILIDADE"
          value={fmtPct(d.disponibilidade)}
          caption={`Tempo operacional  ${d.tempoOperacional ?? '7h 22min'}`}
          trend={{ direction: 'up', value: '2,1%' }}
        />
        <KpiCard
          label="PERFORMANCE"
          value={fmtPct(d.performance)}
          caption={`Ciclo médio  ${d.resumo?.tempoCicloMedio?.toFixed?.(1)?.replace('.', ',') ?? '12,8'}s`}
          trend={{ direction: 'down', value: '1,3%' }}
        />
        <KpiCard
          label="QUALIDADE"
          value={fmtPct(d.qualidade)}
          caption={`Peças boas  ${(d.resumo?.boas ?? 0).toLocaleString('pt-BR')}`}
          trend={{ direction: 'up', value: '0,6%' }}
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
              <div className="status-line-machine-sub">Último ciclo há 12 segundos</div>
            </div>
          </div>
          <div className="status-line-metric">
            <span>Produção atual</span>
            <b>128 un/h</b>
          </div>
          <div className="status-line-metric">
            <span>Tempo de ciclo</span>
            <b>12,5 seg</b>
          </div>
          <div className="status-line-metric">
            <span>Eficiência do turno</span>
            <b className="accent">{fmtPct(d.oee)}</b>
          </div>
        </Panel>
      </div>

      <p className="updated-note">Dados atualizados a cada 5 segundos via MQTT.</p>
    </div>
  )
}
