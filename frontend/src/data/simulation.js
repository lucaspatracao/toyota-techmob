const clamp = (value, min, max) => Math.min(max, Math.max(min, value))

function makePeriodLabel(index, offsetMinutes = 0) {
  const date = new Date(Date.now() + (index + 1) * 60 * 60 * 1000 + offsetMinutes * 60 * 1000)
  return `${String(date.getHours()).padStart(2, '0')}:00 - ${String((date.getHours() + 1) % 24).padStart(2, '0')}:00`
}

export function generateSimulationState(step = 0) {
  const wave = Number(step) || 0
  const disponibilidade = Number((92 + Math.sin(wave * 1.3) * 5 + (Math.random() * 4 - 2)).toFixed(1))
  const performance = Number((85 + Math.cos(wave * 1.6) * 7 + (Math.random() * 4 - 2)).toFixed(1))
  const qualidade = Number((98 + Math.sin(wave * 0.9) * 1.8 + (Math.random() * 1.5 - 0.75)).toFixed(1))
  const oeeBase = (disponibilidade * performance * qualidade) / 10000
  const oee = Number(
    clamp(
      oeeBase + Math.sin(wave * 1.2) * 5 + (Math.random() * 4 - 2),
      68,
      97
    ).toFixed(1)
  )

  const boas = Math.round(clamp(1180 + Math.sin(wave * 0.8) * 160 + Math.random() * 200, 900, 1700))
  const rejeitadas = Math.round(clamp(42 + Math.cos(wave * 1.1) * 18 + Math.random() * 30, 18, 160))
  const tempoCicloMedio = Number(clamp(12.5 + Math.sin(wave * 1.5) * 1.8 + (Math.random() * 1.2 - 0.6), 9.5, 16).toFixed(1))

  const toPercent = (value) => Number(value.toFixed(1))

  const oeeSeries = Array.from({ length: 168 }, (_, index) => {
    const date = new Date(Date.now() - (167 - index) * 60 * 60 * 1000)
    const valor = Number(clamp(oee + Math.sin((wave + index) * 1.1) * 5 + (Math.random() * 3 - 1.5), 68, 97).toFixed(1))
    return {
      time: `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`,
      oee: valor,
    }
  })

  const productionSeries = Array.from({ length: 720 }, (_, index) => {
    const timestamp = Date.now() - (719 - index) * 60 * 60 * 1000
    const date = new Date(timestamp)
    const boasHora = Math.round(clamp(boas / 12 + Math.sin((wave + index) * 1.2) * 40 + Math.random() * 90, 40, 220))
    const rejeitadasHora = Math.round(clamp(rejeitadas / 12 + Math.cos((wave + index) * 1.1) * 10 + Math.random() * 25, 2, 80))
    const taxa = toPercent((rejeitadasHora / (boasHora + rejeitadasHora)) * 100)

    return {
      timestamp,
      time: `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:00`,
      boas: boasHora,
      rejeitadas: rejeitadasHora,
      taxa,
    }
  })

  const hourlyBars = productionSeries.slice(-24).map((point) => ({
    hour: point.time.slice(-5, -3),
    boas: point.boas,
    rejeitadas: point.rejeitadas,
  }))

  const periodSummary = Array.from({ length: 6 }, (_, index) => {
    const boasPeriodo = Math.round(clamp(boas / 6 + Math.random() * 120 + index * 8, 180, 600))
    const rejeitadasPeriodo = Math.round(clamp(rejeitadas / 6 + Math.random() * 18 + index * 2, 10, 80))
    const taxa = toPercent((rejeitadasPeriodo / (boasPeriodo + rejeitadasPeriodo)) * 100)
    const ciclo = Number((tempoCicloMedio + Math.random() * 1.5 - 0.5).toFixed(2))
    const oeePeriodo = Number(clamp(oee + Math.random() * 6 - 3, 68, 96).toFixed(1))

    return {
      periodo: makePeriodLabel(index, wave * 15),
      boas: boasPeriodo,
      rejeitadas: rejeitadasPeriodo,
      taxa: `${taxa.toFixed(1).replace('.', ',')}%`,
      ciclo: `${ciclo.toFixed(2).replace('.', ',')} s`,
      oee: `${oeePeriodo.toFixed(1).replace('.', ',')}%`,
    }
  })

  const historicoRows = productionSeries.map((point) => {
    const date = new Date(point.timestamp)
    const taxa = point.taxa
    const oeePeriodo = Number(clamp(oee + Math.sin(point.timestamp / 3600000) * 5, 66, 96).toFixed(1))
    const status = oeePeriodo < 72 ? 'red' : oeePeriodo < 78 ? 'orange' : 'green'
    const hour = date.getHours()

    return {
      timestamp: point.timestamp,
      status,
      dataHora: `${date.toLocaleDateString('pt-BR')} ${String(date.getHours()).padStart(2, '0')}:00`,
      periodo: `${String(hour).padStart(2, '0')}:00 - ${String((hour + 1) % 24).padStart(2, '0')}:00`,
      turno: hour < 12 ? 'Manhã' : hour < 18 ? 'Tarde' : 'Noite',
      boas: point.boas,
      rejeitadas: point.rejeitadas,
      taxa: `${taxa.toFixed(1).replace('.', ',')}%`,
      ciclo: `${(tempoCicloMedio + Math.sin(point.timestamp / 60000) * 1.5).toFixed(2).replace('.', ',')} s`,
      oee: `${oeePeriodo.toFixed(1).replace('.', ',')}%`,
    }
  })

  return {
    oee,
    disponibilidade: Number(disponibilidade.toFixed(1)),
    performance: Number(performance.toFixed(1)),
    qualidade: Number(qualidade.toFixed(1)),
    resumo: {
      boas,
      rejeitadas,
      tempoCicloMedio,
    },
    oeeSeries,
    periodSummary,
    productionSeries,
    hourlyBars,
    historicoRows,
    updatedAt: new Date().toLocaleTimeString('pt-BR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
  }
}

export function createInitialSimulationState(step = 0) {
  return generateSimulationState(step)
}
