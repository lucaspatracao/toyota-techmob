import '../styles/pageheader.css'

function useClock() {
  // Mostra data/hora "ao vivo" — suposição: a referência exibe um timestamp
  // fixo, aqui deixamos dinâmico para simular atualização em tempo real.
  const now = new Date()
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(now.getDate())}/${pad(now.getMonth() + 1)}/${now.getFullYear()} ${pad(
    now.getHours()
  )}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

export default function PageHeader({ title, subtitle, right }) {
  const timestamp = useClock()
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        <p>{subtitle}</p>
      </div>
      <div className="page-header-right">
        <span className="pill pill-online">
          <span className="dot dot-green" /> SISTEMA ONLINE
        </span>
        <span className="pill pill-date">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M3 10h18M8 3v4M16 3v4" />
          </svg>
          {timestamp}
        </span>
        {right}
      </div>
    </header>
  )
}
