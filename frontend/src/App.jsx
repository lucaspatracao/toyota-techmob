import { useState, useEffect, cloneElement } from 'react'
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import Sidebar from './components/Sidebar.jsx'
import Topbar from './components/Topbar.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Producao from './pages/Producao.jsx'
import Historico from './pages/Historico.jsx'
import { usePolling } from './hooks/usePolling.js'
import { MAQUINA_ID, buscarStatusPlanta } from './services/machineService.js'
import { adaptStatusPlanta } from './services/adapters.js'
import './styles/layout.css'

function lerTemaSalvo() {
  try {
    return localStorage.getItem('smart40-theme') || 'light'
  } catch {
    return 'light'
  }
}

/**
 * Layout principal (sidebar, topbar e a tela da rota).
 *
 * Consulta o estado ao vivo da bancada (GET /api/planta/{id}/status) a cada
 * VITE_POLLING_MS e repassa para a Topbar, a Sidebar e a tela:
 *   - `planta`: estado ao vivo (adaptStatusPlanta), ou null antes da 1ª resposta;
 *   - `apiErro`: erro da última consulta (API fora do ar), ou null.
 * A fonte do dado (bancada, replay ou simulação) é escolhida ao subir o
 * backend; o frontend só mostra o que a API entrega.
 */
function AppLayout({ children }) {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [theme, setTheme] = useState(lerTemaSalvo)
  const { data: planta, error: apiErro } = usePolling(
    () => buscarStatusPlanta(MAQUINA_ID).then(adaptStatusPlanta),
    [MAQUINA_ID]
  )

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    try {
      localStorage.setItem('smart40-theme', theme)
    } catch {
      // sem armazenamento (aba anônima): o tema vale só nesta sessão
    }
  }, [theme])

  const handleToggleTheme = () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))

  const handleToggleSidebar = () => {
    if (window.innerWidth <= 1024) {
      setMobileOpen((v) => !v)
    } else {
      setCollapsed((v) => !v)
    }
  }

  const pageContent = children ? cloneElement(children, { planta, apiErro }) : null

  return (
    <div className="app-shell">
      <Sidebar
        collapsed={collapsed}
        mobileOpen={mobileOpen}
        planta={planta}
        apiErro={apiErro}
        onCloseMobile={() => setMobileOpen(false)}
        onToggleSidebar={handleToggleSidebar}
      />
      <div className="app-main">
        <Topbar
          planta={planta}
          apiErro={apiErro}
          theme={theme}
          onToggleTheme={handleToggleTheme}
          onToggleSidebar={handleToggleSidebar}
          mobileOpen={mobileOpen}
        />
        <main className="app-content">
          {pageContent}
        </main>
      </div>
    </div>
  )
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/dashboard" replace />} />
      <Route
        path="/dashboard"
        element={
          <AppLayout>
            <Dashboard />
          </AppLayout>
        }
      />
      <Route
        path="/producao"
        element={
          <AppLayout>
            <Producao />
          </AppLayout>
        }
      />
      <Route
        path="/historico"
        element={
          <AppLayout>
            <Historico />
          </AppLayout>
        }
      />
    </Routes>
  )
}

export default function App() {
  return (
    <HashRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <AppRoutes />
    </HashRouter>
  )
}
