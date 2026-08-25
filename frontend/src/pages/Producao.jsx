import { useState } from 'react'
import {
  ComposedChart, Bar, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart,
} from 'recharts'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import StatCard from '../components/StatCard.jsx'
import SummaryDonut from '../components/SummaryDonut.jsx'
import { productionSeries, hourlyBars, periodSummary } from '../data/mockData.js'
import '../styles/producao.css'

const RANGES = ['1H', '6H', '12H', '24H', '7D', '30D']

export default function Producao() {
  const [range, setRange] = useState('24H')

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
            Atualizado em 14:32:18
          </button>
        }
      />

      <div className="kpi-row">
        <StatCard icon="total" label="PRODUÇÃO TOTAL" value="1.248" unit="peças" caption="Últimas 24 horas" trend={{ direction: 'up', value: '6,2%', label: 'vs ontem' }} />
        <StatCard icon="good" label="PEÇAS BOAS" value="1.186" unit="peças" caption="Últimas 24 horas" trend={{ direction: 'up', value: '6,4%', label: 'vs ontem' }} />
        <StatCard icon="rejected" label="PEÇAS REJEITADAS" value="62" unit="peças" caption="Últimas 24 horas" trend={{ direction: 'down', value: '3,1%', label: 'vs ontem' }} />
        <StatCard icon="rate" label="TAXA DE REJEIÇÃO" value="5,0%" caption="Últimas 24 horas" trend={{ direction: 'down', value: '0,2 p.p.', label: 'vs ontem' }} />
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
            <ComposedChart data={productionSeries} margin={{ top: 10, right: 30, left: -10, bottom: 0 }}>
              <CartesianGrid stroke="#1f2636" vertical={false} />
              <XAxis dataKey="time" stroke="#5c6478" fontSize={11} tickLine={false} axisLine={false} />
              <YAxis yAxisId="left" stroke="#5c6478" fontSize={11} tickLine={false} axisLine={false} label={{ value: 'Peças', position: 'insideTopLeft', fill: '#5c6478', fontSize: 11 }} />
              <YAxis yAxisId="right" orientation="right" stroke="#5c6478" fontSize={11} tickLine={false} axisLine={false} tickFormatter={(v) => `${v}%`} />
              <Tooltip contentStyle={{ background: '#131826', border: '1px solid #1f2636', borderRadius: 8, fontSize: 12 }} />
              <Bar yAxisId="left" dataKey="boas" stackId="a" fill="#22c55e" radius={[3, 3, 0, 0]} />
              <Bar yAxisId="left" dataKey="rejeitadas" stackId="a" fill="#ef4444" radius={[0, 0, 0, 0]} />
              <Line yAxisId="right" type="monotone" dataKey="taxa" stroke="#c7ccd8" strokeDasharray="4 3" dot={{ r: 3, fill: '#c7ccd8' }} />
            </ComposedChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="RESUMO (ÚLTIMAS 24 HORAS)" className="summary-panel">
          <div className="summary-donut-wrap">
            <SummaryDonut good={1186} rejected={62} />
            <ul className="summary-legend">
              <li><span className="dot dot-green" /> Peças boas <b>1.186 (94,8%)</b></li>
              <li><span className="dot dot-red" /> Peças rejeitadas <b>62 (5,0%)</b></li>
              <li><span className="dot dot-orange" /> Taxa de rejeição <b>5,0%</b></li>
            </ul>
          </div>
          <div className="summary-footer-row">
            <span>Tempo de ciclo médio</span>
            <b>8,42 s</b>
          </div>
        </Panel>
      </div>

      <Panel title="PRODUÇÃO POR HORA - PEÇAS BOAS x REJEITADAS" style={{ marginTop: 20 }}>
        <div className="chart-legend">
          <span><i className="dot dot-green" /> Peças boas</span>
          <span><i className="dot dot-red" /> Peças rejeitadas</span>
        </div>
        <ResponsiveContainer width="100%" height={200}>
          <BarChart data={hourlyBars} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
            <CartesianGrid stroke="#1f2636" vertical={false} />
            <XAxis dataKey="hour" stroke="#5c6478" fontSize={10} tickLine={false} axisLine={false} interval={0} />
            <YAxis stroke="#5c6478" fontSize={11} tickLine={false} axisLine={false} />
            <Tooltip contentStyle={{ background: '#131826', border: '1px solid #1f2636', borderRadius: 8, fontSize: 12 }} />
            <Bar dataKey="boas" fill="#22c55e" radius={[2, 2, 0, 0]} />
            <Bar dataKey="rejeitadas" fill="#ef4444" radius={[2, 2, 0, 0]} />
          </BarChart>
        </ResponsiveContainer>
      </Panel>

      <Panel title="RESUMO POR PERÍODO" style={{ marginTop: 20 }}>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>PERÍODO</th>
                <th>PEÇAS BOAS</th>
                <th>PEÇAS REJEITADAS</th>
                <th>TAXA DE REJEIÇÃO</th>
                <th>TEMPO DE CICLO MÉDIO</th>
                <th>OEE MÉDIO</th>
              </tr>
            </thead>
            <tbody>
              {periodSummary.map((row) => (
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
      </Panel>

      <p className="updated-note">Dados atualizados a cada 5 segundos via MQTT.</p>
    </>
  )
}
