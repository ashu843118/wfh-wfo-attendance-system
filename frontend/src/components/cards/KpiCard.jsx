import './KpiCard.css'

export default function KpiCard({ label, value, icon: Icon, trend, accent = false }) {
  return (
    <div className={`kpi-card ${accent ? 'kpi-card--accent' : ''}`}>
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
