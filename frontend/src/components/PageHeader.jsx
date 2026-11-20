import { useEffect, useState } from 'react'
import '../styles/pageheader.css'

/** Data e hora do navegador, atualizadas a cada segundo. */
function useClock() {
  const [now, setNow] = useState(() => new Date())

  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), 1000)
    return () => clearInterval(id)
  }, [])

  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(now.getDate())}/${pad(now.getMonth() + 1)}/${now.getFullYear()} ${pad(
    now.getHours()
  )}:${pad(now.getMinutes())}:${pad(now.getSeconds())}`
}

/**
 * Cabeçalho de página.
 * - `breadcrumb`: trilha curta acima do título (ex.: "OPERAÇÃO / DASHBOARD OEE").
 * - `eyebrow`: rótulo pequeno acima do título (ex.: "MONITORAMENTO DE EFICIÊNCIA FABRIL").
 * - `title`/`subtitle`: título e descrição da tela.
 * - `systemActive`: a API está respondendo (SISTEMA ONLINE) ou não (SISTEMA EM ESPERA).
 */
export default function PageHeader({ breadcrumb, eyebrow, title, subtitle, right, systemActive = true }) {
  const timestamp = useClock()
  return (
    <header className="page-header">
      <div className="page-header-left">
        {breadcrumb && <p className="page-breadcrumb">{breadcrumb}</p>}
        {eyebrow && <p className="page-eyebrow">{eyebrow}</p>}
        <h1>{title}</h1>
        {subtitle && <p className="page-subtitle">{subtitle}</p>}
      </div>
      <div className="page-header-right">
        <span className={`pill ${systemActive ? 'pill-online' : 'pill-paused'}`}>
          <span className={`dot ${systemActive ? 'dot-green' : 'dot-red'}`} />
          {systemActive ? 'SISTEMA ONLINE' : 'SISTEMA EM ESPERA'}
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
