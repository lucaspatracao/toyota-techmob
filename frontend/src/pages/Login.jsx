import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import '../styles/login.css'

export default function Login() {
  const navigate = useNavigate()
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      login(username, password)
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const handleQuickLogin = (user) => {
    setError('')
    setLoading(true)
    try {
      login(user, '1234')
      navigate('/dashboard')
    } catch (err) {
      setError(err.message)
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-wrap">
        <div className="brand-panel">
          <div>
            <div className="brand-logo">
              <span className="brand-mark">
                <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M12 2L21 7V17L12 22L3 17V7L12 2Z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
                  <path d="M12 12L21 7M12 12V22M12 12L3 7" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round" />
                </svg>
              </span>
              SMART 4.0
            </div>

            <div className="brand-copy">
              <h1>Monitoramento de produção e performance operacional.</h1>
              <p>
                Acompanhe OEE, disponibilidade, performance e qualidade da sua linha em tempo quase real,
                direto do chão de fábrica.
              </p>
            </div>

            <div className="brand-stats">
              <div className="brand-stat">
                <div className="brand-stat-num">78,4%</div>
                <div className="brand-stat-label">OEE consolidado</div>
              </div>
              <div className="brand-stat">
                <div className="brand-stat-num">92,1%</div>
                <div className="brand-stat-label">Disponibilidade</div>
              </div>
              <div className="brand-stat">
                <div className="brand-stat-num">99,6%</div>
                <div className="brand-stat-label">Qualidade</div>
              </div>
            </div>
          </div>

          <div className="brand-footer">SMART 4.0 © 2026 · Todos os direitos reservados.</div>
        </div>

        <div className="login-panel">
          <div className="panel-head">
            <span className="status-chip">
              <span className="status-dot" />
              MQTT conectado
            </span>
            <h2>Acesso ao sistema</h2>
            <p>Entre com suas credenciais para acompanhar a Bancada Smart em tempo real.</p>
          </div>

          {error && <div className="login-error">{error}</div>}

          <form onSubmit={handleSubmit} className="login-form">
            <div className="field">
              <label htmlFor="username">Usuário</label>
              <input
                id="username"
                type="text"
                placeholder="Digite seu usuário"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                disabled={loading}
                autoFocus
                required
              />
            </div>

            <div className="field">
              <label htmlFor="password">Senha</label>
              <input
                id="password"
                type="password"
                placeholder="Digite sua senha"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                required
              />
            </div>

            <div className="row-between">
              <label className="remember">
                <input type="checkbox" />
                Manter conectado
              </label>
              <a href="#">Esqueci minha senha</a>
            </div>

            <button type="submit" className="login-button" disabled={loading}>
              {loading ? 'Autenticando...' : 'Entrar na plataforma'}
            </button>
          </form>

          <div className="divider">ACESSO RÁPIDO</div>

          <div className="quick-grid">
            <button type="button" className="quick-btn" onClick={() => handleQuickLogin('supervisor-a')} disabled={loading}>
              <span className="quick-avatar">A</span>
              Supervisor Turno A
            </button>
            <button type="button" className="quick-btn" onClick={() => handleQuickLogin('supervisor-b')} disabled={loading}>
              <span className="quick-avatar">B</span>
              Supervisor Turno B
            </button>
            <button type="button" className="quick-btn" onClick={() => handleQuickLogin('supervisor-c')} disabled={loading}>
              <span className="quick-avatar">C</span>
              Supervisor Turno C
            </button>
            <button type="button" className="quick-btn" onClick={() => handleQuickLogin('manager')} disabled={loading}>
              <span className="quick-avatar">G</span>
              Gerente Geral
            </button>
          </div>

          <div className="demo-note">
            <b>Ambiente de demonstração.</b> Todos os usuários utilizam a senha <code>1234</code>. Use os
            botões acima para acesso rápido ou digite um usuário completo.
          </div>

          <div className="foot-note">
            © 2026 Toyota TechMob Brasil. Todos os direitos reservados. · <a href="#">Privacidade</a>
          </div>
        </div>
      </div>
    </div>
  )
}
