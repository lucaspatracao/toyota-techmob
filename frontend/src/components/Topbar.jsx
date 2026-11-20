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
 * Selo de conexão: de onde vem o dado que as telas mostram.
 * online (verde): a fonte está respondendo, seja a bancada, o simulador ou
 * o replay; fallback (laranja): esperando a primeira resposta; offline
 * (vermelho): sem dado.
 */
function seloDeConexao(planta, apiErro) {
  if (apiErro) {
    return { classe: 'offline', texto: 'API fora do ar', titulo: 'O backend não respondeu à última consulta' }
  }
  if (!planta) {
    return { classe: 'fallback', texto: 'Conectando...', titulo: 'Aguardando a primeira resposta da API' }
  }
  if (!planta.conectado) {
    return { classe: 'offline', texto: 'Bancada sem conexão', titulo: 'Nem a bancada nem o simulador responderam' }
  }
  switch (planta.fonte) {
    case 'BANCADA':
      return { classe: 'online', texto: 'Bancada conectada', titulo: 'Lendo o /dadosPlanta da Bancada Smart 4.0' }
    case 'REPLAY':
      return { classe: 'online', texto: 'Replay', titulo: 'Reproduzindo uma execução gravada da bancada' }
    default:
      return {
        classe: 'online',
        texto: 'Simulação',
        titulo: 'Simulador da bancada: o JSON do /dadosPlanta com a cronometragem da equipe',
      }
  }
}

/**
 * Barra superior fixa: identidade, fonte do dado e tema.
 * A fonte (bancada, replay ou simulação) é escolhida ao subir o backend.
 */
export default function Topbar({
  planta = null,
  apiErro = null,
  theme = 'light',
  onToggleTheme,
  onToggleSidebar,
  mobileOpen = false,
}) {
  const selo = seloDeConexao(planta, apiErro)
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

        <span className={`mqtt-badge ${selo.classe}`} title={selo.titulo} role="status">
          <span className="mqtt-dot" />
          {selo.texto}
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

      </div>
    </header>
  )
}
