import '../styles/panel.css'

export default function Panel({ title, subtitle, right, children, className = '', style }) {
  return (
    <section className={`panel ${className}`} style={style}>
      {(title || right) && (
        <div className="panel-header">
          <div>
            {title && <h2>{title}</h2>}
            {subtitle && <p>{subtitle}</p>}
          </div>
          {right && <div className="panel-header-right">{right}</div>}
        </div>
      )}
      <div className="panel-body">{children}</div>
    </section>
  )
}
