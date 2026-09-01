import { useState } from 'react'
import '../styles/topbar.css'

const IconBell = () => (
  <svg width="19" height="19" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
    <path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
    <path d="M13.7 21a2 2 0 0 1-3.4 0" />
  </svg>
)

const IconSun = () => (
  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
    <circle cx="12" cy="12" r="4.5" />
    <path d="M12 2v2.5M12 19.5V22M4.2 4.2l1.8 1.8M18 18l1.8 1.8M2 12h2.5M19.5 12H22M4.2 19.8 6 18M18 6l1.8-1.8" />
  </svg>
)

const IconMoon = () => (
  <svg width="17" height="17" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
    <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8Z" />
  </svg>
)

/**
 * Barra superior fixa: identidade do sistema, status de conexão MQTT,
 * notificações e avatar.
 */
export default function Topbar({ mqttConnected = true, theme = 'light', onToggleTheme }) {
  const [notifOpen, setNotifOpen] = useState(false)

  return (
    <header className="topbar">
      <div className="topbar-left">
        <div className="topbar-brand">
          <span className="topbar-brand-mark">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none">
              <path d="M12 2 3 7v10l9 5 9-5V7l-9-5Z" stroke="#fff" strokeWidth="1.8" />
              <path d="M3 7l9 5 9-5M12 12v10" stroke="#fff" strokeWidth="1.8" />
            </svg>
          </span>
          <span className="topbar-brand-text">
            SMART <b>4.0</b>
          </span>
        </div>

        <span className={`mqtt-badge ${mqttConnected ? 'online' : 'offline'}`}>
          <span className="mqtt-dot" />
          {mqttConnected ? 'MQTT conectado' : 'MQTT desconectado'}
        </span>
      </div>

      <div className="topbar-right">
        <button
          type="button"
          className="theme-toggle"
          aria-label={theme === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro'}
          onClick={onToggleTheme}
        >
          {theme === 'dark' ? <IconSun /> : <IconMoon />}
        </button>

        <button
          type="button"
          className="topbar-icon-btn"
          aria-label="Notificações"
          onClick={() => setNotifOpen((v) => !v)}
        >
          <IconBell />
          <span className="topbar-badge">3</span>
        </button>

        <div className="topbar-user">
          <div className="topbar-avatar">RS</div>
          <div className="topbar-user-meta">
            <span className="topbar-user-name">Rafael Souza</span>
            <span className="topbar-user-role">Supervisor de Produção</span>
          </div>
        </div>
      </div>

      {notifOpen && (
        <div className="topbar-notif-panel">
          <div className="topbar-notif-header">Notificações</div>
          <div className="topbar-notif-item">
            <span className="dot dot-red" />
            <div>
              <b>Taxa de rejeição elevada</b>
              <p>Estação 3 acima do limite (7,8%) — há 12 min</p>
            </div>
          </div>
          <div className="topbar-notif-item">
            <span className="dot dot-orange" />
            <div>
              <b>Manutenção programada</b>
              <p>Bancada Smart 4.0 — amanhã às 06:00</p>
            </div>
          </div>
          <div className="topbar-notif-item">
            <span className="dot dot-green" />
            <div>
              <b>Meta diária atingida</b>
              <p>OEE acima da meta de 75% — há 1h</p>
            </div>
          </div>
        </div>
      )}
    </header>
  )
}
