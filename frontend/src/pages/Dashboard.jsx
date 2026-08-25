import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import ArcGauge from '../components/ArcGauge.jsx'
import ProgressBar from '../components/ProgressBar.jsx'
import SummaryDonut from '../components/SummaryDonut.jsx'
import { oeeSeries, periodSummary } from '../data/mockData.js'
import '../styles/dashboard.css'

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

export default function Dashboard() {
  return (
    <>
      <PageHeader title="Dashboard" subtitle="Visão geral do desempenho da máquina em tempo real." />

      <div className="kpi-row">
        <KpiCard
          label="OEE"
          gauge={<ArcGauge value={78.4} />}
          caption="Últimas 24 horas"
          trend={{ direction: 'up', value: '6,2%' }}
        />
        <KpiCard
          label="DISPONIBILIDADE"
          valueNode={<div className="kpi-linear-value">85,7%</div>}
          gauge={<ProgressBar value={85.7} color="var(--accent-green)" />}
          caption="Últimas 24 horas"
          trend={{ direction: 'up', value: '4,3%' }}
        />
        <KpiCard
          label="PERFORMANCE"
          valueNode={<div className="kpi-linear-value">81,2%</div>}
          gauge={<ProgressBar value={81.2} color="var(--accent-blue-light)" />}
          caption="Últimas 24 horas"
          trend={{ direction: 'up', value: '3,1%' }}
        />
        <KpiCard
          label="QUALIDADE"
          valueNode={<div className="kpi-linear-value">94,8%</div>}
          gauge={<ProgressBar value={94.8} color="var(--accent-green)" />}
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
            <AreaChart data={oeeSeries} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
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
            <SummaryDonut good={1186} rejected={62} />
            <ul className="summary-legend">
              <li>
                <span className="dot dot-green" /> Peças boas <b>1.186 (94,8%)</b>
              </li>
              <li>
                <span className="dot dot-red" /> Peças rejeitadas <b>62 (5,0%)</b>
              </li>
              <li>
                <span className="dot dot-orange" /> Taxa de rejeição <b>5,0%</b>
              </li>
            </ul>
          </div>
          <div className="summary-footer-row">
            <span>Tempo de ciclo médio</span>
            <b>8,42 s</b>
          </div>
        </Panel>
      </div>

      <Panel title="PRODUÇÃO (ÚLTIMAS 24 HORAS)" className="table-panel" style={{ marginTop: 20 }}>
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
