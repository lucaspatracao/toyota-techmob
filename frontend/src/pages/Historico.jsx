import { useMemo, useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import StatCard from '../components/StatCard.jsx'
import Pagination from '../components/Pagination.jsx'
import { historicoRows } from '../data/mockData.js'
import '../styles/historico.css'

const DOT_CLASS = { green: 'dot-green', orange: 'dot-orange', red: 'dot-red' }

function toDateInputValue(date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000)
  return localDate.toISOString().slice(0, 10)
}

function parseFormattedNumber(value) {
  return Number(String(value).replace('%', '').replace(' s', '').replace(',', '.')) || 0
}

function dateKey(timestamp) {
  const date = new Date(timestamp)
  return `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`
}

function timestampForRow(row) {
  if (Number.isFinite(row.timestamp)) return row.timestamp
  const [datePart, timePart = '00:00'] = row.dataHora.split(' ')
  const [day, month, year] = datePart.split('/').map(Number)
  const [hour, minute] = timePart.split(':').map(Number)
  return new Date(year, month - 1, day, hour, minute).getTime()
}

function aggregateRows(rows, groupBy) {
  const groups = new Map()
  rows.forEach((row) => {
    const key = groupBy === 'Dia'
      ? dateKey(row.timestamp)
      : groupBy === 'Turno'
        ? `${dateKey(row.timestamp)}-${row.turno}`
        : String(row.timestamp)
    const group = groups.get(key) ?? []
    group.push(row)
    groups.set(key, group)
  })

  return Array.from(groups.values(), (group) => {
    const boas = group.reduce((sum, row) => sum + row.boas, 0)
    const rejeitadas = group.reduce((sum, row) => sum + row.rejeitadas, 0)
    const oee = group.reduce((sum, row) => sum + parseFormattedNumber(row.oee), 0) / group.length
    const ciclo = group.reduce((sum, row) => sum + parseFormattedNumber(row.ciclo), 0) / group.length
    const latest = group.reduce((current, row) => row.timestamp > current.timestamp ? row : current)
    const day = dateKey(latest.timestamp)
    const periodo = groupBy === 'Dia' ? day : groupBy === 'Turno' ? `${latest.turno} · ${day}` : latest.periodo

    return {
      ...latest,
      periodo,
      boas,
      rejeitadas,
      taxa: `${((rejeitadas / Math.max(boas + rejeitadas, 1)) * 100).toFixed(1).replace('.', ',')}%`,
      ciclo: `${ciclo.toFixed(2).replace('.', ',')} s`,
      oee: `${oee.toFixed(1).replace('.', ',')}%`,
      status: oee < 72 ? 'red' : oee < 78 ? 'orange' : 'green',
    }
  })
}

export default function Historico({ simulationEnabled = true, simulationState }) {
  const [page, setPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(10)
  const [dateFrom, setDateFrom] = useState(() => {
    const date = new Date()
    date.setDate(date.getDate() - 30)
    return toDateInputValue(date)
  })
  const [dateTo, setDateTo] = useState(() => toDateInputValue(new Date()))
  const [groupBy, setGroupBy] = useState('Hora')
  const [shiftFilter, setShiftFilter] = useState('Todos os turnos')
  const [sortBy, setSortBy] = useState('timestamp')
  const [sortDirection, setSortDirection] = useState('desc')

  const sourceRows = (simulationState?.historicoRows ?? historicoRows).map((row) => ({
    ...row,
    timestamp: timestampForRow(row),
  }))
  const allRows = useMemo(() => {
    const start = new Date(`${dateFrom}T00:00:00`).getTime()
    const end = new Date(`${dateTo}T23:59:59.999`).getTime()
    const filtered = sourceRows.filter((row) => (
      row.timestamp >= start && row.timestamp <= end &&
      (shiftFilter === 'Todos os turnos' || row.turno === shiftFilter)
    ))
    const grouped = aggregateRows(filtered, groupBy)
    return grouped.sort((a, b) => {
      const first = a[sortBy]
      const second = b[sortBy]
      const comparison = typeof first === 'number'
        ? first - second
        : String(first).localeCompare(String(second), 'pt-BR')
      return sortDirection === 'asc' ? comparison : -comparison
    })
  }, [dateFrom, dateTo, groupBy, shiftFilter, sourceRows, sortBy, sortDirection])

  const totalPages = Math.max(1, Math.ceil(allRows.length / itemsPerPage))
  const currentPage = Math.min(page, Math.max(totalPages, 1))
  const startItem = allRows.length ? (currentPage - 1) * itemsPerPage + 1 : 0
  const endItem = Math.min(currentPage * itemsPerPage, allRows.length)
  const visibleRows = allRows.slice(startItem - 1, endItem)
  const totalBoas = allRows.reduce((sum, row) => sum + row.boas, 0)
  const totalRejeitadas = allRows.reduce((sum, row) => sum + row.rejeitadas, 0)
  const handleSort = (column) => {
    if (sortBy === column) setSortDirection((direction) => direction === 'asc' ? 'desc' : 'asc')
    else {
      setSortBy(column)
      setSortDirection('desc')
    }
    setPage(1)
  }

  return (
    <>
      <PageHeader systemActive={simulationEnabled} title="Histórico" subtitle="Consulte o histórico completo de produção da máquina." />

      <div className="kpi-row">
        <StatCard icon="total" label="PRODUÇÃO TOTAL" value={(totalBoas + totalRejeitadas).toLocaleString('pt-BR')} unit="peças" caption="No período selecionado" />
        <StatCard icon="good" label="PEÇAS BOAS" value={totalBoas.toLocaleString('pt-BR')} unit="peças" caption="No período selecionado" />
        <StatCard icon="rejected" label="PEÇAS REJEITADAS" value={totalRejeitadas.toLocaleString('pt-BR')} unit="peças" caption="No período selecionado" />
        <StatCard icon="rate" label="TAXA DE REJEIÇÃO MÉDIA" value={`${(totalRejeitadas / Math.max(totalBoas + totalRejeitadas, 1) * 100).toFixed(1).replace('.', ',')}%`} caption="No período selecionado" />
      </div>

      <Panel className="filters-panel" style={{ marginTop: 20 }}>
        <div className="filters-row">
          <div className="filter-field">
            <label>PERÍODO</label>
            <div className="filter-dates">
              <div className="date-input">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" />
                </svg>
                <input type="date" value={dateFrom} max={dateTo} onChange={(e) => { setDateFrom(e.target.value); setPage(1) }} />
              </div>
              <span>até</span>
              <div className="date-input">
                <input type="date" value={dateTo} min={dateFrom} onChange={(e) => { setDateTo(e.target.value); setPage(1) }} />
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" />
                </svg>
              </div>
            </div>
          </div>

          <div className="filter-field">
            <label>AGRUPAR POR</label>
            <select value={groupBy} onChange={(e) => { setGroupBy(e.target.value); setPage(1) }}>
              <option>Hora</option>
              <option>Turno</option>
              <option>Dia</option>
            </select>
          </div>

          <div className="filter-field">
            <label>FILTRO RÁPIDO</label>
            <select value={shiftFilter} onChange={(e) => { setShiftFilter(e.target.value); setPage(1) }}>
              <option>Todos os turnos</option>
              <option>Manhã</option>
              <option>Tarde</option>
              <option>Noite</option>
            </select>
          </div>

          <button
            className="clear-filters-btn"
            onClick={() => {
              const date = new Date()
              date.setDate(date.getDate() - 30)
              setDateFrom(toDateInputValue(date))
              setDateTo(toDateInputValue(new Date()))
              setGroupBy('Hora')
              setShiftFilter('Todos os turnos')
              setSortBy('timestamp')
              setSortDirection('desc')
              setPage(1)
            }}
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M4 6h16M8 6V4h8v2M6 6l1 14h10l1-14" />
            </svg>
            Limpar filtros
          </button>
        </div>
      </Panel>

      <Panel title="HISTÓRICO DE PRODUÇÃO" style={{ marginTop: 20 }}>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th><button type="button" className="table-sort" onClick={() => handleSort('timestamp')}>DATA / HORA ⇅</button></th>
                <th>PERÍODO</th>
                <th><button type="button" className="table-sort" onClick={() => handleSort('turno')}>TURNO ⇅</button></th>
                <th>PEÇAS BOAS</th>
                <th>PEÇAS REJEITADAS</th>
                <th>TAXA DE REJEIÇÃO</th>
                <th>TEMPO DE CICLO MÉDIO</th>
                <th>OEE MÉDIO</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {visibleRows.map((row, idx) => (
                <tr key={idx}>
                  <td>
                    <span className="row-status">
                      <span className={`dot ${DOT_CLASS[row.status]}`} />
                      {row.dataHora}
                    </span>
                  </td>
                  <td>{row.periodo}</td>
                  <td>{row.turno}</td>
                  <td>{row.boas}</td>
                  <td>{row.rejeitadas}</td>
                  <td>{row.taxa}</td>
                  <td>{row.ciclo}</td>
                  <td>{row.oee}</td>
                  <td>
                    <button className="chevron-btn" aria-label="Ver detalhes">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                        <path d="M9 18l6-6-6-6" />
                      </svg>
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <Pagination
          page={currentPage}
          totalPages={totalPages}
          onChange={setPage}
          itemsPerPage={itemsPerPage}
          onItemsPerPageChange={(n) => {
            setItemsPerPage(n)
            setPage(1)
          }}
          totalItems={allRows.length}
          startItem={startItem}
          endItem={endItem}
        />
      </Panel>

      <p className="updated-note">{simulationEnabled ? 'Dados atualizados pela simulação' : 'Simulação pausada'} · {simulationState?.updatedAt || 'agora'}</p>
    </>
  )
}
