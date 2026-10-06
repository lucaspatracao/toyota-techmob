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
  const pageNumbers = [...new Set([1, 2, 3, page - 1, page, page + 1, totalPages])]
    .filter((number) => number >= 1 && number <= totalPages)
    .sort((first, second) => first - second)

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
        {pageNumbers.map((number, index) => (
          <span className="page-number-group" key={number}>
            {index > 0 && number - pageNumbers[index - 1] > 1 && <span className="page-ellipsis">...</span>}
            <button className={`page-btn num${number === page ? ' active' : ''}`} onClick={() => onChange(number)}>
              {number}
            </button>
          </span>
        ))}
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
