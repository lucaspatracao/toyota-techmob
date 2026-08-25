import '../styles/progressbar.css'

export default function ProgressBar({ value = 0, color = 'var(--accent-green)' }) {
  return (
    <div className="progress-track">
      <div
        className="progress-fill"
        style={{ width: `${Math.min(100, Math.max(0, value))}%`, background: color }}
      />
    </div>
  )
}
