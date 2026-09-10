import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import {
  historicoRows,
  hourlyBars,
  periodSummary,
  productionSeries,
} from '../data/mockData.js'

const DEMO_INTERVAL_MS = 10000
const DemoDataContext = createContext(null)

function vary(value, amount = 0.08) {
  return value * (1 + (Math.random() - 0.5) * amount)
}

function createSnapshot() {
  const oee = Math.min(99, Math.max(55, vary(78.4, 0.14)))
  const boas = Math.round(vary(1248, 0.16))
  const rejeitadas = Math.max(1, Math.round(vary(62, 0.2)))

  return {
    dashboard: {
      oee,
      disponibilidade: Math.min(99, Math.max(70, vary(92.1, 0.1))),
      performance: Math.min(99, Math.max(65, vary(84.7, 0.12))),
      qualidade: Math.min(99.9, Math.max(90, vary(99.6, 0.04))),
      resumo: {
        boas,
        rejeitadas,
        tempoCicloMedio: vary(12.8, 0.1),
      },
      periodSummary: periodSummary.map((row) => ({
        ...row,
        boas: Math.round(vary(row.boas, 0.14)),
        rejeitadas: Math.max(1, Math.round(vary(row.rejeitadas, 0.2))),
      })),
    },
    production: {
      series: productionSeries.map((row) => ({
        ...row,
        boas: Math.round(vary(row.boas, 0.18)),
        rejeitadas: Math.max(1, Math.round(vary(row.rejeitadas, 0.22))),
        taxa: Number(vary(row.taxa, 0.2).toFixed(1)),
      })),
      hourly: hourlyBars.map((row) => ({
        ...row,
        boas: Math.round(vary(row.boas, 0.16)),
        rejeitadas: Math.max(1, Math.round(vary(row.rejeitadas, 0.2))),
      })),
      boas,
      rejeitadas,
      ciclo: vary(8.42, 0.1),
      updatedAt: new Date().toLocaleTimeString('pt-BR'),
    },
    history: historicoRows.map((row) => ({
      ...row,
      boas: Math.round(vary(row.boas, 0.16)),
      rejeitadas: Math.max(1, Math.round(vary(row.rejeitadas, 0.22))),
      taxa: `${vary(Number(row.taxa.replace(',', '.').replace('%', '')), 0.2).toFixed(1).replace('.', ',')}%`,
      ciclo: `${vary(Number(row.ciclo.replace(',', '.').replace(' s', '')), 0.1).toFixed(2).replace('.', ',')} s`,
      oee: `${vary(Number(row.oee.replace(',', '.').replace('%', '')), 0.1).toFixed(1).replace('.', ',')}%`,
    })),
    updatedAt: new Date(),
  }
}

export function DemoDataProvider({ children }) {
  const [data, setData] = useState(createSnapshot)

  useEffect(() => {
    const intervalId = setInterval(() => setData(createSnapshot()), DEMO_INTERVAL_MS)
    return () => clearInterval(intervalId)
  }, [])

  const value = useMemo(() => ({ data, intervalMs: DEMO_INTERVAL_MS }), [data])
  return <DemoDataContext.Provider value={value}>{children}</DemoDataContext.Provider>
}

export function useDemoData() {
  const context = useContext(DemoDataContext)
  if (!context) throw new Error('useDemoData deve ser usado dentro de DemoDataProvider')
  return context
}