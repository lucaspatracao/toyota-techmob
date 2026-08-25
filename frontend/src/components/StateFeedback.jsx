import '../styles/statefeedback.css'

export function LoadingState({ label = 'Carregando dados...' }) {
  return (
    <div className="state-feedback">
      <div className="state-spinner" />
      <p>{label}</p>
    </div>
  )
}

export function ErrorState({ message = 'Não foi possível carregar os dados.', onRetry }) {
  return (
    <div className="state-feedback state-feedback-error">
      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="var(--accent-red)" strokeWidth="2">
        <circle cx="12" cy="12" r="10" />
        <path d="M12 8v5M12 16h.01" />
      </svg>
      <p>{message}</p>
      {onRetry && (
        <button className="state-retry-btn" onClick={onRetry}>
          Tentar novamente
        </button>
      )}
    </div>
  )
}
