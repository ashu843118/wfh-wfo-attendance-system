import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts'
import './ChartCard.css'

export default function BarChartCard({
  title,
  data = [],
  bars = [
    { key: 'presentDays', label: 'Present', color: '#FFE600' },
    { key: 'wfoDays', label: 'WFO', color: '#4ade80' },
    { key: 'wfhDays', label: 'WFH', color: '#60a5fa' },
  ],
  xKey = 'employeeName',
}) {
  return (
    <div className="chart-card">
      <div className="card-header">
        <h3 className="card-title">{title}</h3>
      </div>
      {data.length === 0 ? (
        <div className="chart-card__empty">No chart data available</div>
      ) : (
        <ResponsiveContainer width="100%" height={280}>
          <BarChart data={data} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.06)" />
            <XAxis
              dataKey={xKey}
              tick={{ fill: 'var(--color-text-muted)', fontSize: 11 }}
              axisLine={{ stroke: 'var(--color-border)' }}
            />
            <YAxis
              tick={{ fill: 'var(--color-text-muted)', fontSize: 11 }}
              axisLine={{ stroke: 'var(--color-border)' }}
            />
            <Tooltip
              contentStyle={{
                background: 'var(--color-bg-elevated)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-sm)',
              }}
            />
            <Legend />
            {bars.map((bar) => (
              <Bar key={bar.key} dataKey={bar.key} name={bar.label} fill={bar.color} radius={[4, 4, 0, 0]} />
            ))}
          </BarChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}
