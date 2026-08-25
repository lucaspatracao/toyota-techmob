import { HashRouter, Routes, Route, Navigate } from 'react-router-dom'
import Sidebar from './components/Sidebar.jsx'
import Dashboard from './pages/Dashboard.jsx'
import Producao from './pages/Producao.jsx'
import Historico from './pages/Historico.jsx'
import './styles/layout.css'

export default function App() {
  return (
    <HashRouter>
      <div className="app-shell">
        <Sidebar />
        <main className="app-content">
          <Routes>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="/dashboard" element={<Dashboard />} />
            <Route path="/producao" element={<Producao />} />
            <Route path="/historico" element={<Historico />} />
          </Routes>
        </main>
      </div>
    </HashRouter>
  )
}
