import { NavLink } from 'react-router-dom'
import '../styles/sidebar.css'

// Ícones em SVG inline (sem dependências externas de ícone)
const IconDashboard = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <rect x="3" y="3" width="7" height="9" rx="1.5" />
    <rect x="14" y="3" width="7" height="5" rx="1.5" />
    <rect x="14" y="12" width="7" height="9" rx="1.5" />
    <rect x="3" y="16" width="7" height="5" rx="1.5" />
  </svg>
)
const IconProducao = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M3 17l5-5 4 4 8-9" />
    <path d="M14 7h6v6" />
  </svg>
)
const IconHistorico = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
    <path d="M3 12a9 9 0 1 0 3-6.7" />
    <path d="M3 4v5h5" />
    <path d="M12 7v5l3 3" />
  </svg>
)

// Ilustração simplificada da "Bancada Smart 4.0" — recriada em SVG
// (suposição: não temos o asset original isolado, então desenhamos um
// equivalente estilizado mantendo proporções e paleta da referência)
const MachineArt = () => (
  <svg viewBox="0 0 200 190" width="100%" height="auto">
    <rect x="10" y="20" width="180" height="150" rx="10" fill="#0f1420" stroke="#1f2636" />
    <rect x="30" y="40" width="140" height="70" rx="4" fill="none" stroke="#2a3247" strokeWidth="2" />
    <circle cx="60" cy="55" r="4" fill="#2f6feb" />
    <circle cx="80" cy="55" r="4" fill="#22c55e" />
    <circle cx="100" cy="55" r="4" fill="#f59e0b" />
    <path d="M40 100 L160 100" stroke="#2a3247" strokeWidth="2" />
    <rect x="45" y="10" width="6" height="35" fill="#2a3247" />
    <rect x="150" y="10" width="6" height="35" fill="#2a3247" />
    <rect x="30" y="120" width="140" height="35" rx="4" fill="#0b0f1c" stroke="#1f2636" />
    <circle cx="55" cy="137" r="6" fill="#2f6feb" opacity="0.7" />
    <rect x="80" y="130" width="70" height="14" rx="2" fill="#161d2e" />
  </svg>
)

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
          <path d="M12 2 3 7v10l9 5 9-5V7l-9-5Z" stroke="#4d8bff" strokeWidth="1.6" />
          <path d="M3 7l9 5 9-5M12 12v10" stroke="#4d8bff" strokeWidth="1.6" />
        </svg>
        <span className="sidebar-logo-text">
          SMART <b>4.0</b>
        </span>
      </div>

      <div className="sidebar-machine">
        <MachineArt />
      </div>

      <nav className="sidebar-nav">
        <NavLink to="/dashboard" className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}>
          <IconDashboard />
          Dashboard
        </NavLink>
        <NavLink to="/producao" className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}>
          <IconProducao />
          Produção
        </NavLink>
        <NavLink to="/historico" className={({ isActive }) => `sidebar-link${isActive ? ' active' : ''}`}>
          <IconHistorico />
          Histórico
        </NavLink>
      </nav>

      <div className="sidebar-spacer" />

      <div className="sidebar-status-card">
        <div className="sidebar-status-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4d8bff" strokeWidth="2">
            <rect x="3" y="4" width="18" height="12" rx="2" />
            <path d="M8 20h8M12 16v4" />
          </svg>
        </div>
        <div>
          <div className="sidebar-status-label">MÁQUINA</div>
          <div className="sidebar-status-value">SMART 4.0</div>
          <div className="sidebar-status-label" style={{ marginTop: 10 }}>
            STATUS
          </div>
          <div className="sidebar-status-online">
            <span className="dot dot-green" /> Operando
          </div>
        </div>
      </div>

      <p className="sidebar-footer">SMART 4.0 © 2026<br />Todos os direitos reservados.</p>
    </aside>
  )
}
