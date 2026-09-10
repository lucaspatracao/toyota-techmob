import { useMemo, useState } from 'react'
import PageHeader from '../components/PageHeader.jsx'
import Panel from '../components/Panel.jsx'
import StatCard from '../components/StatCard.jsx'
import Pagination from '../components/Pagination.jsx'
import { historicoRows } from '../data/mockData.js'
import '../styles/historico.css'

const DOT_CLASS = { green: 'dot-green', orange: 'dot-orange', red: 'dot-red' }

export default function Historico() {
  const [page, setPage] = useState(1)
  const [itemsPerPage, setItemsPerPage] = useState(10)
  const [dateFrom, setDateFrom] = useState('2024-05-11')
  const [dateTo, setDateTo] = useState('2024-05-18')
  const [groupBy, setGroupBy] = useState('Hora')
  const [shiftFilter, setShiftFilter] = useState('Todos os turnos')

  // Suposição: no protótipo visual, repetimos as linhas de exemplo para
  // simular uma base maior e demonstrar a paginação (225 itens, como na
  // referência).
  const allRows = useMemo(() => {
    const rows = []
    for (let i = 0; i < 23; i++) rows.push(...historicoRows)
    return rows.slice(0, 225)
  }, [])

  const totalPages = Math.ceil(allRows.length / itemsPerPage)
  const startItem = (page - 1) * itemsPerPage + 1
  const endItem = Math.min(page * itemsPerPage, allRows.length)
  const visibleRows = allRows.slice(startItem - 1, endItem)

  return (
    <>
      <PageHeader title="Histórico" subtitle="Consulte o histórico completo de produção da máquina." />

      <div className="kpi-row">
        <StatCard icon="total" label="PRODUÇÃO TOTAL" value="45.678" unit="peças" caption="No período selecionado" />
        <StatCard icon="good" label="PEÇAS BOAS" value="43.286" unit="peças" caption="No período selecionado" />
        <StatCard icon="rejected" label="PEÇAS REJEITADAS" value="2.392" unit="peças" caption="No período selecionado" />
        <StatCard icon="rate" label="TAXA DE REJEIÇÃO MÉDIA" value="5,2%" caption="No período selecionado" />
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
                <input type="date" value={dateFrom} onChange={(e) => setDateFrom(e.target.value)} />
              </div>
              <span>até</span>
              <div className="date-input">
                <input type="date" value={dateTo} onChange={(e) => setDateTo(e.target.value)} />
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="3" y="5" width="18" height="16" rx="2" /><path d="M3 10h18M8 3v4M16 3v4" />
                </svg>
              </div>
            </div>
          </div>

          <div className="filter-field">
            <label>AGRUPAR POR</label>
            <select value={groupBy} onChange={(e) => setGroupBy(e.target.value)}>
              <option>Hora</option>
              <option>Turno</option>
              <option>Dia</option>
            </select>
          </div>

          <div className="filter-field">
            <label>FILTRO RÁPIDO</label>
            <select value={shiftFilter} onChange={(e) => setShiftFilter(e.target.value)}>
              <option>Todos os turnos</option>
              <option>Manhã</option>
              <option>Tarde</option>
              <option>Noite</option>
            </select>
          </div>

          <button
            className="clear-filters-btn"
            onClick={() => {
              setDateFrom('2024-05-11')
              setDateTo('2024-05-18')
              setGroupBy('Hora')
              setShiftFilter('Todos os turnos')
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
                <th>DATA / HORA ⇅</th>
                <th>PERÍODO</th>
                <th>TURNO ⇅</th>
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
          page={page}
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

      <p className="updated-note">Dados atualizados a cada 5 segundos via MQTT.</p>
    </>
  )
}
