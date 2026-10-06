import { useState } from 'react'
import {
  ComposedChart, Bar, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart,
} from 'recharts'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import StatCard from '../components/StatCard.jsx'
import SummaryDonut from '../components/SummaryDonut.jsx'
import { productionSeries, hourlyBars } from '../data/mockData.js'
import '../styles/producao.css'

const RANGES = ['1H', '6H', '12H', '24H', '7D', '30D']
const RANGE_HOURS = { '1H': 1, '6H': 6, '12H': 12, '24H': 24, '7D': 168, '30D': 720 }

function sumField(rows, field) {
  return rows.reduce((sum, row) => sum + Number(row[field] ?? 0), 0)
}

export default function Producao({ simulationEnabled = true, simulationState }) {
  const [range, setRange] = useState('24H')
  const data = simulationState
  const allChartData = data?.productionSeries ?? productionSeries
  const chartData = allChartData.slice(-RANGE_HOURS[range])
  const previousChartData = allChartData.slice(-RANGE_HOURS[range] * 2, -RANGE_HOURS[range])
  const boasNoPeriodo = sumField(chartData, 'boas')
  const rejeitadasNoPeriodo = sumField(chartData, 'rejeitadas')
  const boasPeriodoAnterior = sumField(previousChartData, 'boas')
  const rejeitadasPeriodoAnterior = sumField(previousChartData, 'rejeitadas')
  const makeTrend = (current, previous, inverse = false) => {
    const delta = previous ? ((current - previous) / previous) * 100 : 0
    return { direction: (delta >= 0) !== inverse ? 'up' : 'down', value: `${Math.abs(delta).toFixed(1).replace('.', ',')}%`, label: 'vs. período anterior' }
  }

  const summary = data ? {
    total: boasNoPeriodo + rejeitadasNoPeriodo,
    boas: boasNoPeriodo,
    rejeitadas: rejeitadasNoPeriodo,
    taxa: (rejeitadasNoPeriodo / Math.max(boasNoPeriodo + rejeitadasNoPeriodo, 1) * 100),
    ciclo: data.resumo?.tempoCicloMedio ?? 12.8,
    updatedAt: data.updatedAt ?? 'agora',
  } : {
    total: 1248,
    boas: 1186,
    rejeitadas: 62,
    taxa: 5,
    ciclo: 12.8,
    updatedAt: 'agora',
  }

  const hourlyData = data?.hourlyBars ?? hourlyBars

  return (
    <>
      <PageHeader
        systemActive={simulationEnabled}
        title="Produção"
        subtitle="Acompanhe a produção da máquina ao longo do tempo."
        right={
          <button className="pill pill-refresh">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M21 12a9 9 0 1 1-3-6.7" />
              <path d="M21 3v6h-6" />
            </svg>
            Atualizado em {summary.updatedAt}
          </button>
        }
      />

      <div className="kpi-row">
        <StatCard icon="total" label="PRODUÇÃO TOTAL" value={summary.total.toLocaleString('pt-BR')} unit="peças" caption={range} trend={makeTrend(summary.total, boasPeriodoAnterior + rejeitadasPeriodoAnterior)} />
        <StatCard icon="good" label="PEÇAS BOAS" value={summary.boas.toLocaleString('pt-BR')} unit="peças" caption={range} trend={makeTrend(summary.boas, boasPeriodoAnterior)} />
        <StatCard icon="rejected" label="PEÇAS REJEITADAS" value={String(summary.rejeitadas)} unit="peças" caption={range} trend={makeTrend(summary.rejeitadas, rejeitadasPeriodoAnterior, true)} />
        <StatCard icon="rate" label="TAXA DE REJEIÇÃO" value={`${summary.taxa.toFixed(1).replace('.', ',')}%`} caption={range} trend={makeTrend(summary.taxa, previousChartData.length ? (rejeitadasPeriodoAnterior / Math.max(boasPeriodoAnterior + rejeitadasPeriodoAnterior, 1)) * 100 : 0, true)} />
      </div>

      <div className="grid-row" style={{ marginTop: 20, alignItems: 'stretch' }}>
        <Panel
          className="chart-panel"
          title="PRODUÇÃO AO LONGO DO TEMPO ⓘ"
          right={
            <div className="range-toggle">
              {RANGES.map((r) => (
                <button key={r} className={r === range ? 'active' : ''} onClick={() => setRange(r)}>
                  {r}
                </button>
              ))}
            </div>
          }
        >
          <div className="chart-legend">
            <span><i className="dot dot-green" /> Peças boas</span>
            <span><i className="dot dot-red" /> Peças rejeitadas</span>
            <span><i className="dash-legend dash-legend-red" /> Taxa de rejeição (%)</span>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <ComposedChart data={chartData} margin={{ top: 10, right: 30, left: -10, bottom: 0 }}>
              <CartesianGrid stroke="var(--chart-grid)" vertical={false} />
              <XAxis dataKey="time" stroke="var(--chart-axis)" fontSize={11} tickLine={false} axisLine={false} />
              <YAxis yAxisId="left" stroke="var(--chart-axis)" fontSize={11} tickLine={false} axisLine={false} label={{ value: 'Peças', position: 'insideTopLeft', fill: 'var(--chart-axis)', fontSize: 11 }} />
              <YAxis yAxisId="right" orientation="right" stroke="var(--chart-axis)" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(v) => `${v}%`} />
              <Tooltip contentStyle={{ background: 'var(--bg-card)', color: 'var(--text-primary)', border: '1px solid var(--border-subtle)', borderRadius: 8, fontSize: 12, boxShadow: 'var(--shadow-md)' }} />
              <Bar yAxisId="left" dataKey="boas" stackId="a" fill="var(--chart-good)" radius={[2, 2, 0, 0]} />
              <Bar yAxisId="left" dataKey="rejeitadas" stackId="a" fill="var(--chart-rejected)" radius={[0, 0, 0, 0]} />
              <Line yAxisId="right" type="monotone" dataKey="taxa" stroke="var(--chart-rejected)" strokeDasharray="4 3" dot={{ r: 3, fill: 'var(--chart-rejected)' }} />
            </ComposedChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="RESUMO (ÚLTIMAS 24 HORAS)" className="summary-panel">
          <div className="summary-donut-wrap">
            <SummaryDonut good={summary.boas} rejected={summary.rejeitadas} size={190} stroke={18} />
            <ul className="summary-legend">
              <li><span className="dot dot-green" /> Peças boas <b>{summary.boas.toLocaleString('pt-BR')} ({((summary.boas / (summary.boas + summary.rejeitadas)) * 100).toFixed(1).replace('.', ',')}%)</b></li>
              <li><span className="dot dot-red" /> Peças rejeitadas <b>{summary.rejeitadas.toLocaleString('pt-BR')} ({summary.taxa.toFixed(1).replace('.', ',')}%)</b></li>
              <li><span className="dot dot-red" /> Taxa de rejeição <b>{summary.taxa.toFixed(1).replace('.', ',')}%</b></li>
            </ul>
          </div>
          <div className="summary-footer-row">
            <span>Tempo de ciclo médio</span>
            <b>{summary.ciclo.toFixed(1).replace('.', ',')} s</b>
          </div>
        </Panel>
      </div>

      <Panel title="PRODUÇÃO POR HORA - PEÇAS BOAS x REJEITADAS" style={{ marginTop: 20 }}>
        <div className="chart-legend">
          <span><i className="dot dot-green" /> Peças boas</span>
          <span><i className="dot dot-red" /> Peças rejeitadas</span>
        </div>
        <ResponsiveContainer width="100%" height={200}>
          <BarChart data={hourlyData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
            <CartesianGrid stroke="var(--chart-grid)" vertical={false} />
            <XAxis dataKey="hour" stroke="var(--chart-axis)" fontSize={10} tickLine={false} axisLine={false} interval={0} />
            <YAxis stroke="var(--chart-axis)" fontSize={11} tickLine={false} axisLine={false} />
            <Tooltip contentStyle={{ background: 'var(--bg-card)', color: 'var(--text-primary)', border: '1px solid var(--border-subtle)', borderRadius: 8, fontSize: 12, boxShadow: 'var(--shadow-md)' }} />
            <Bar dataKey="boas" fill="var(--chart-good)" radius={[2, 2, 0, 0]} />
            <Bar dataKey="rejeitadas" fill="var(--chart-rejected)" radius={[2, 2, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </Panel>

      <p className="updated-note">Dados atualizados em tempo real pela simulação · {summary.updatedAt}</p>
    </>
  )
}
