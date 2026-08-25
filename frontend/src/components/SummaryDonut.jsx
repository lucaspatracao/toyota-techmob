/**
 * Anel com dois segmentos (peças boas / rejeitadas) usado no card
 * "Resumo (Últimas 24 horas)".
 */
export default function SummaryDonut({ good, rejected, size = 140, stroke = 16 }) {
  const total = good + rejected
  const r = (size - stroke) / 2
  const c = 2 * Math.PI * r
  const goodRatio = total ? good / total : 0
  const goodLen = c * goodRatio
  const rejectedLen = c - goodLen

  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="var(--border-subtle)" strokeWidth={stroke} />
      {/* Rejeitadas (base vermelha) */}
      <circle
        cx={size / 2}
        cy={size / 2}
        r={r}
        fill="none"
        stroke="var(--accent-red)"
        strokeWidth={stroke}
        strokeDasharray={`${rejectedLen} ${c - rejectedLen}`}
        strokeDashoffset={0}
        transform={`rotate(-90 ${size / 2} ${size / 2})`}
      />
      {/* Boas (por cima, mesmo ponto de partida) */}
      <circle
        cx={size / 2}
        cy={size / 2}
        r={r}
        fill="none"
        stroke="var(--accent-green)"
        strokeWidth={stroke}
        strokeDasharray={`${goodLen} ${c - goodLen}`}
        strokeDashoffset={-rejectedLen}
        transform={`rotate(-90 ${size / 2} ${size / 2})`}
      />
      <text x="50%" y="46%" textAnchor="middle" fontSize={size * 0.16} fontWeight="700" fill="var(--text-primary)">
        {total.toLocaleString('pt-BR')}
      </text>
      <text x="50%" y="60%" textAnchor="middle" fontSize={size * 0.09} fill="var(--text-tertiary)">
        peças
      </text>
    </svg>
  )
}
