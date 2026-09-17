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

export default function Producao({ simulationEnabled = true, simulationState }) {
  const [range, setRange] = useState('24H')
  const data = simulationEnabled ? simulationState : null

  const summary = data ? {
    total: data.resumo?.boas ?? 1248,
    boas: data.resumo?.boas ?? 1186,
    rejeitadas: data.resumo?.rejeitadas ?? 62,
    taxa: ((data.resumo?.rejeitadas ?? 62) / Math.max((data.resumo?.boas ?? 1248) + (data.resumo?.rejeitadas ?? 62), 1) * 100),
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

  const chartData = data?.productionSeries ?? productionSeries
  const hourlyData = data?.hourlyBars ?? hourlyBars

  return (
    <>
      <PageHeader
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
        <StatCard icon="total" label="PRODUÇÃO TOTAL" value={summary.total.toLocaleString('pt-BR')} unit="peças" caption="Últimas 24 horas" trend={{ direction: 'up', value: '6,2%', label: 'vs ontem' }} />
        <StatCard icon="good" label="PEÇAS BOAS" value={summary.boas.toLocaleString('pt-BR')} unit="peças" caption="Últimas 24 horas" trend={{ direction: 'up', value: '6,4%', label: 'vs ontem' }} />
        <StatCard icon="rejected" label="PEÇAS REJEITADAS" value={String(summary.rejeitadas)} unit="peças" caption="Últimas 24 horas" trend={{ direction: 'down', value: '3,1%', label: 'vs ontem' }} />
        <StatCard icon="rate" label="TAXA DE REJEIÇÃO" value={`${summary.taxa.toFixed(1).replace('.', ',')}%`} caption="Últimas 24 horas" trend={{ direction: 'down', value: '0,2 p.p.', label: 'vs ontem' }} />
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
            <span><i className="dash-legend" /> Taxa de rejeição (%)</span>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <ComposedChart data={chartData} margin={{ top: 10, right: 30, left: -10, bottom: 0 }}>
              <CartesianGrid stroke="#E2E8F0" vertical={false} />
              <XAxis dataKey="time" stroke="#64748B" fontSize={11} tickLine={false} axisLine={false} />
              <YAxis yAxisId="left" stroke="#64748B" fontSize={11} tickLine={false} axisLine={false} label={{ value: 'Peças', position: 'insideTopLeft', fill: '#64748B', fontSize: 11 }} />
              <YAxis yAxisId="right" orientation="right" stroke="#64748B" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(v) => `${v}%`} />
              <Tooltip contentStyle={{ background: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: 8, fontSize: 12, boxShadow: '0 4px 10px rgba(15,23,42,0.08)' }} />
              <Bar yAxisId="left" dataKey="boas" stackId="a" fill="#22C55E" radius={[2, 2, 0, 0]} />
              <Bar yAxisId="left" dataKey="rejeitadas" stackId="a" fill="#EF4444" radius={[0, 0, 0, 0]} />
              <Line yAxisId="right" type="monotone" dataKey="taxa" stroke="#94A3B8" strokeDasharray="4 3" dot={{ r: 3, fill: '#94A3B8' }} />
            </ComposedChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="RESUMO (ÚLTIMAS 24 HORAS)" className="summary-panel">
          <div className="summary-donut-wrap">
            <SummaryDonut good={summary.boas} rejected={summary.rejeitadas} />
            <ul className="summary-legend">
              <li><span className="dot dot-green" /> Peças boas <b>{summary.boas.toLocaleString('pt-BR')} ({((summary.boas / (summary.boas + summary.rejeitadas)) * 100).toFixed(1).replace('.', ',')}%)</b></li>
              <li><span className="dot dot-red" /> Peças rejeitadas <b>{summary.rejeitadas.toLocaleString('pt-BR')} ({summary.taxa.toFixed(1).replace('.', ',')}%)</b></li>
              <li><span className="dot dot-orange" /> Taxa de rejeição <b>{summary.taxa.toFixed(1).replace('.', ',')}%</b></li>
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
            <CartesianGrid stroke="#E2E8F0" vertical={false} />
            <XAxis dataKey="hour" stroke="#64748B" fontSize={10} tickLine={false} axisLine={false} interval={0} />
            <YAxis stroke="#64748B" fontSize={11} tickLine={false} axisLine={false} />
            <Tooltip contentStyle={{ background: '#FFFFFF', border: '1px solid #E2E8F0', borderRadius: 8, fontSize: 12, boxShadow: '0 4px 10px rgba(15,23,42,0.08)' }} />
            <Bar dataKey="boas" fill="#22C55E" radius={[2, 2, 0, 0]} />
            <Bar dataKey="rejeitadas" fill="#EF4444" radius={[2, 2, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </Panel>

      <p className="updated-note">Dados atualizados em tempo real pela simulação · {summary.updatedAt}</p>
    </>
  )
}
