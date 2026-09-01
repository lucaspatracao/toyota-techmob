import { useEffect } from 'react'
import { NavLink, useLocation } from 'react-router-dom'
import '../styles/sidebar.css'

// Ícones em SVG inline (sem dependências externas de ícone)
const IconDashboard = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
    <rect x="3" y="3" width="7" height="9" rx="1.5" />
    <rect x="14" y="3" width="7" height="5" rx="1.5" />
    <rect x="14" y="12" width="7" height="9" rx="1.5" />
    <rect x="3" y="16" width="7" height="5" rx="1.5" />
  </svg>
)
const IconProducao = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
    <path d="M3 17l5-5 4 4 8-9" />
    <path d="M14 7h6v6" />
  </svg>
)
const IconHistorico = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5">
    <path d="M3 12a9 9 0 1 0 3-6.7" />
    <path d="M3 4v5h5" />
    <path d="M12 7v5l3 3" />
  </svg>
)
const IconMenu = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M4 6h16M4 12h16M4 18h16" />
  </svg>
)

/**
 * Sidebar fixa de navegação.
 * - `collapsed`: modo desktop recolhido (64px, somente ícones), controlado
 *   pelo botão hambúrguer embutido na própria sidebar.
 * - `mobileOpen` / `onCloseMobile`: modo off-canvas em telas pequenas.
 */
export default function Sidebar({ collapsed = false, mobileOpen = false, onCloseMobile, onToggleSidebar }) {
  const location = useLocation()

  // Fecha o menu móvel automaticamente ao navegar entre telas
  useEffect(() => {
    onCloseMobile?.()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.pathname])

  // Evita rolagem do conteúdo por trás enquanto o menu móvel está aberto
  useEffect(() => {
    document.body.style.overflow = mobileOpen ? 'hidden' : ''
    return () => {
      document.body.style.overflow = ''
    }
  }, [mobileOpen])

  return (
    <>
      {mobileOpen && <div className="sidebar-overlay" onClick={onCloseMobile} />}

      <aside
        className={`sidebar${collapsed ? ' collapsed' : ''}${mobileOpen ? ' open' : ''}`}
        title={collapsed ? 'SMART 4.0' : undefined}
      >
        <div className="sidebar-brand">
          <span className="sidebar-brand-id">
            <span className="sidebar-brand-mark">
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
                <path d="M12 2 3 7v10l9 5 9-5V7l-9-5Z" stroke="#fff" strokeWidth="1.8" />
                <path d="M3 7l9 5 9-5M12 12v10" stroke="#fff" strokeWidth="1.8" />
              </svg>
            </span>
            <span className="sidebar-brand-text">SMART 4.0</span>
          </span>
          <button
            type="button"
            className="sidebar-burger"
            aria-label="Recolher/expandir menu"
            onClick={onToggleSidebar}
          >
            <IconMenu />
          </button>
        </div>

        <div className="sidebar-search">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <circle cx="11" cy="11" r="7" />
            <path d="m21 21-4.3-4.3" />
          </svg>
          <input type="text" placeholder="Buscar..." />
        </div>

        <nav className="sidebar-nav">
          <NavLink to="/dashboard" className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`} title="Dashboard">
            <IconDashboard />
            <span className="sidebar-link-label">Dashboard</span>
          </NavLink>
          <NavLink to="/producao" className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`} title="Produção">
            <IconProducao />
            <span className="sidebar-link-label">Produção</span>
          </NavLink>
          <NavLink to="/historico" className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`} title="Histórico">
            <IconHistorico />
            <span className="sidebar-link-label">Histórico</span>
          </NavLink>
        </nav>

        <div className="sidebar-spacer" />

        <div className="sidebar-status-card">
          <div className="sidebar-status-icon">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="var(--text-on-dark-muted)" strokeWidth="1.6">
              <rect x="3" y="4" width="18" height="12" rx="1.5" />
              <path d="M8 20h8M12 16v4" />
            </svg>
          </div>
          <div className="sidebar-status-text">
            <div className="sidebar-status-label">MÁQUINA</div>
            <div className="sidebar-status-value">SMART 4.0</div>
            <div className="sidebar-status-online">
              <span className="dot dot-green" /> Operando
            </div>
          </div>
        </div>

        <p className="sidebar-footer">SMART 4.0 © 2026<br />Todos os direitos reservados.</p>
      </aside>
    </>
  )
}
