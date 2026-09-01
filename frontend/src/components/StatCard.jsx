import '../styles/statcard.css'

/**
 * Card de indicador numérico usado no topo das telas Produção/Histórico.
 * icon: 'total' | 'good' | 'rejected' | 'rate'
 * trend: { direction: 'up' | 'down', value: '6,2%', label: 'vs ontem' } (opcional)
 */
const ICONS = {
  total: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2">
      <path d="M13 2 4 14h7l-1 8 9-12h-7l1-8Z" />
    </svg>
  ),
  good: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2">
      <path d="M20 6 9 17l-5-5" />
    </svg>
  ),
  rejected: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2">
      <path d="M18 6 6 18M6 6l12 12" />
    </svg>
  ),
  rate: (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#fff" strokeWidth="2">
      <circle cx="12" cy="12" r="9" />
      <path d="M9.5 9.5h.01M14.5 14.5h.01M9 15l6-6" />
    </svg>
  ),
}

const ICON_BG = {
  total: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
  good: 'linear-gradient(135deg, #16A34A, #22C55E)',
  rejected: 'linear-gradient(135deg, #E11D48, #F43F5E)',
  rate: 'linear-gradient(135deg, #D97706, #F59E0B)',
}

export default function StatCard({ icon = 'total', label, value, unit, caption, trend }) {
  return (
    <div className="stat-card">
      <div className="stat-card-icon" style={{ background: ICON_BG[icon] }}>
        {ICONS[icon]}
      </div>
      <div className="stat-card-body">
        <div className="stat-card-label">{label}</div>
        <div className="stat-card-value">
          {value}
          {unit && <span className="stat-card-unit"> {unit}</span>}
        </div>
        <div className="stat-card-caption">
          {caption}
          {trend && (
            <span className={`stat-trend ${trend.direction}`}>
              {trend.direction === 'up' ? '↑' : '↓'} {trend.value}
              {trend.label && <span className="stat-trend-label"> {trend.label}</span>}
            </span>
          )}
        </div>
      </div>
    </div>
  )
}
