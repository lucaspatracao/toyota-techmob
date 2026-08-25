import '../styles/pagination.css'

const ChevronsLeft = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M11 17l-5-5 5-5M18 17l-5-5 5-5" />
  </svg>
)
const ChevronLeft = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M15 18l-6-6 6-6" />
  </svg>
)
const ChevronRight = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M9 18l6-6-6-6" />
  </svg>
)
const ChevronsRight = () => (
  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M13 17l5-5-5-5M6 17l5-5-5-5" />
  </svg>
)

export default function Pagination({ page, totalPages, onChange, itemsPerPage, onItemsPerPageChange, totalItems, startItem, endItem }) {
  return (
    <div className="pagination">
      <div className="pagination-perpage">
        <span>Itens por página:</span>
        <select value={itemsPerPage} onChange={(e) => onItemsPerPageChange(Number(e.target.value))}>
          <option value={10}>10</option>
          <option value={25}>25</option>
          <option value={50}>50</option>
        </select>
      </div>

      <div className="pagination-pages">
        <button className="page-btn" disabled={page === 1} onClick={() => onChange(1)}>
          <ChevronsLeft />
        </button>
        <button className="page-btn" disabled={page === 1} onClick={() => onChange(page - 1)}>
          <ChevronLeft />
        </button>
        {Array.from({ length: Math.min(3, totalPages) }, (_, i) => i + 1).map((n) => (
          <button key={n} className={`page-btn num${n === page ? ' active' : ''}`} onClick={() => onChange(n)}>
            {n}
          </button>
        ))}
        {totalPages > 3 && <span className="page-ellipsis">...</span>}
        {totalPages > 3 && (
          <button className={`page-btn num${page === totalPages ? ' active' : ''}`} onClick={() => onChange(totalPages)}>
            {totalPages}
          </button>
        )}
        <button className="page-btn" disabled={page === totalPages} onClick={() => onChange(page + 1)}>
          <ChevronRight />
        </button>
        <button className="page-btn" disabled={page === totalPages} onClick={() => onChange(totalPages)}>
          <ChevronsRight />
        </button>
      </div>

      <div className="pagination-count">
        {startItem}-{endItem} de {totalItems} itens
      </div>
    </div>
  )
}
