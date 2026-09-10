import { useState, useEffect } from 'react'
import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import Sidebar from './components/Sidebar.jsx'
import Topbar from './components/Topbar.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Producao from './pages/Producao.jsx'
import Historico from './pages/Historico.jsx'
import './styles/layout.css'

export default function App() {
  const [collapsed, setCollapsed] = useState(false)
  const [mobileOpen, setMobileOpen] = useState(false)
  const [theme, setTheme] = useState(() => localStorage.getItem('smart40-theme') || 'light')

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('smart40-theme', theme)
  }, [theme])

  const handleToggleTheme = () => setTheme((t) => (t === 'dark' ? 'light' : 'dark'))

  const handleToggleSidebar = () => {
    // Em telas pequenas o hambúrguer abre/fecha o menu off-canvas;
    // em telas grandes ele recolhe a sidebar para 64px (só ícones).
    if (window.innerWidth <= 1024) {
      setMobileOpen((v) => !v)
    } else {
      setCollapsed((v) => !v)
    }
  }

  return (
    <HashRouter>
      <div className="app-shell">
        <Sidebar
          collapsed={collapsed}
          mobileOpen={mobileOpen}
          onCloseMobile={() => setMobileOpen(false)}
          onToggleSidebar={handleToggleSidebar}
        />
        <div className="app-main">
          <Topbar mqttConnected theme={theme} onToggleTheme={handleToggleTheme} />
          <main className="app-content">
            <Routes>
              <Route path="/" element={<Navigate to="/dashboard" replace />} />
              <Route path="/dashboard" element={<Dashboard />} />
              <Route path="/producao" element={<Producao />} />
              <Route path="/historico" element={<Historico />} />
            </Routes>
          </main>
        </div>
      </div>
    </HashRouter>
  )
}
