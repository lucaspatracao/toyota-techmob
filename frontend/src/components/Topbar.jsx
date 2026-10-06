import '../styles/topbar.css'

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

const IconSmartMark = () => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
    <path d="M12 2 3 7v10l9 5 9-5V7l-9-5Z" />
    <path d="M3 7l9 5 9-5M12 12v10" />
  </svg>
)

/**
 * Barra superior fixa: identidade, conexão MQTT e controles do sistema.
 */
export default function Topbar({
  mqttConnected = true,
  theme = 'light',
  onToggleTheme,
  simulationEnabled = true,
  onToggleSimulation,
  onToggleSidebar,
  mobileOpen = false,
}) {
  return (
    <header className="topbar">
      <div className="topbar-left">
        {!mobileOpen && (
          <button type="button" className="sidebar-mobile-toggle" aria-label="Abrir menu Smart 4.0" onClick={onToggleSidebar}>
            <IconSmartMark />
          </button>
        )}
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
          className={`simulation-toggle ${simulationEnabled ? 'simulation-on' : 'simulation-off'}`}
          aria-pressed={simulationEnabled}
          onClick={onToggleSimulation}
        >
          <span className="simulation-toggle-dot" />
          {simulationEnabled ? 'Desligar simulação' : 'Ativar simulação'}
        </button>
        <button
          type="button"
          className="theme-toggle"
          aria-label={theme === 'dark' ? 'Ativar tema claro' : 'Ativar tema escuro'}
          onClick={onToggleTheme}
        >
          {theme === 'dark' ? <IconSun /> : <IconMoon />}
        </button>

      </div>
    </header>
  )
}
