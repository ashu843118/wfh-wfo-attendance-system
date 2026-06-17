import './KpiCard.css'

export default function KpiCard({
  label,
  value,
  icon: Icon,
  trend,
  accent = false,
  clickable = false,
  selected = false,
  onClick,
}) {
  const className = [
    'kpi-card',
    accent ? 'kpi-card--accent' : '',
    clickable ? 'kpi-card--clickable' : '',
    selected ? 'kpi-card--selected' : '',
  ]
    .filter(Boolean)
    .join(' ')

  const handleKeyDown = (event) => {
    if (!clickable || !onClick) return
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      onClick()
    }
  }

  return (
    <div
      className={className}
      role={clickable ? 'button' : undefined}
      tabIndex={clickable ? 0 : undefined}
      onClick={clickable ? onClick : undefined}
      onKeyDown={handleKeyDown}
      aria-pressed={clickable ? selected : undefined}
    >
      <div className="kpi-card__header">
        <span className="kpi-card__label">{label}</span>
        {Icon && (
          <span className="kpi-card__icon">
            <Icon size={20} />
          </span>
        )}
      </div>
      <div className="kpi-card__value">{value ?? '—'}</div>
      {trend && <div className="kpi-card__trend">{trend}</div>}
    </div>
  )
}
