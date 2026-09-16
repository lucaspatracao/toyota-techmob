import { useState, useEffect } from 'react'
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import AuthProvider, { USER_TYPES } from './context/AuthContext'
import { useAuth } from './hooks/useAuth'
import Sidebar from './components/Sidebar.jsx'
import Topbar from './components/Topbar.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Producao from './pages/Producao.jsx'
import Historico from './pages/Historico.jsx'
import Login from './pages/Login.jsx'
import './styles/layout.css'

/**
 * Componente privado que verifica autenticação
 * Se não autenticado, redireciona para login
 */
function PrivateRoute({ children }) {
  const { isAuthenticated, loading } = useAuth()

  if (loading) {
    return <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', height: '100vh' }}>Carregando...</div>
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }

  return children
}

/**
 * Layout principal (com sidebar, topbar, etc)
 * Só exibido quando autenticado
 */
function AppLayout({ children }) {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [theme, setTheme] = useState(() => localStorage.getItem('smart40-theme') || 'light')
  const [simulationEnabled, setSimulationEnabled] = useState(true)

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('smart40-theme', theme)
  }, [theme])

  const handleToggleTheme = () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))

  const handleToggleSidebar = () => {
    if (window.innerWidth <= 1024) {
      setMobileOpen((v) => !v)
    } else {
      setCollapsed((v) => !v)
    }
  }

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
          {children}
        </main>
      </div>
    </div>
  )
}

/**
 * Rotas da aplicação
 */
function AppRoutes() {
  const { isAuthenticated } = useAuth()

  return (
    <Routes>
      {/* Rota pública: Login */}
      <Route path="/login" element={<Login />} />

      {/* Rotas privadas */}
      <Route path="/" element={isAuthenticated ? <Navigate to="/dashboard" replace /> : <Navigate to="/login" replace />} />
      <Route
        path="/dashboard"
        element={
          <PrivateRoute>
            <AppLayout>
              <Dashboard />
            </AppLayout>
          </PrivateRoute>
        }
      />
      <Route
        path="/producao"
        element={
          <PrivateRoute>
            <AppLayout>
              <Producao />
            </AppLayout>
          </PrivateRoute>
        }
      />
      <Route
        path="/historico"
        element={
          <PrivateRoute>
            <AppLayout>
              <Historico />
            </AppLayout>
          </PrivateRoute>
        }
      />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <HashRouter>
        <AppRoutes />
      </HashRouter>
    </AuthProvider>
  )
}
