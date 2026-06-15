import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip, Legend } from 'recharts'
import './ChartCard.css'

const COLORS = ['#FFE600', '#4ade80', '#60a5fa', '#f87171', '#a78bfa']

export default function PieChartCard({ title, data = [], dataKey = 'value', nameKey = 'label' }) {
  const chartData = data.map((item) => ({
    name: item[nameKey] || item.label,
    value: item[dataKey] ?? item.value,
  }))

  return (
    <div className="chart-card">
      <div className="card-header">
        <h3 className="card-title">{title}</h3>
      </div>
      {chartData.length === 0 ? (
        <div className="chart-card__empty">No chart data available</div>
      ) : (
        <ResponsiveContainer width="100%" height={280}>
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={100}
              paddingAngle={3}
              dataKey="value"
            >
              {chartData.map((_, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip
              contentStyle={{
                background: 'var(--color-bg-elevated)',
                border: '1px solid var(--color-border)',
                borderRadius: 'var(--radius-sm)',
              }}
            />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}
