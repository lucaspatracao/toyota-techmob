import { useState, useEffect, cloneElement } from 'react'
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import AuthProvider from './context/AuthContext'
import Sidebar from './components/Sidebar.jsx'
import Topbar from './components/Topbar.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Producao from './pages/Producao.jsx'
import Historico from './pages/Historico.jsx'
import { createInitialSimulationState } from './data/simulation.js'
import './styles/layout.css'

/**
 * Layout principal (com sidebar, topbar, etc)
 */
function AppLayout({ children }) {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [theme, setTheme] = useState(() => localStorage.getItem('smart40-theme') || 'light')
  const [simulationEnabled, setSimulationEnabled] = useState(true)
  const [simulationState, setSimulationState] = useState(() => createInitialSimulationState())

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('smart40-theme', theme)
  }, [theme])

  useEffect(() => {
    if (!simulationEnabled) return

    const interval = setInterval(() => {
      setSimulationState((current) => {
        const nextStep = (current?.step ?? 0) + 1
        return { ...createInitialSimulationState(nextStep), step: nextStep }
      })
    }, 4000)

    return () => clearInterval(interval)
  }, [simulationEnabled])

  const handleToggleTheme = () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))

  const handleToggleSidebar = () => {
    if (window.innerWidth <= 1024) {
      setMobileOpen((v) => !v)
    } else {
      setCollapsed((v) => !v)
    }
  }

  const pageContent = children ? cloneElement(children, { simulationEnabled, simulationState }) : null

  return (
    <div className="app-shell">
      <Sidebar
        collapsed={collapsed}
        mobileOpen={mobileOpen}
        onCloseMobile={() => setMobileOpen(false)}
        onToggleSidebar={handleToggleSidebar}
      />
      <div className="app-main">
        <Topbar
          mqttConnected
          theme={theme}
          onToggleTheme={handleToggleTheme}
          simulationEnabled={simulationEnabled}
          onToggleSimulation={() => setSimulationEnabled((enabled) => !enabled)}
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
    <AuthProvider>
      <HashRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <AppRoutes />
      </HashRouter>
    </AuthProvider>
  )
}
