import './DataTable.css'

export default function DataTable({ columns, data = [], keyField = 'id', emptyMessage = 'No records found' }) {
  if (!data.length) {
    return (
      <div className="data-table data-table--empty">
        <p>{emptyMessage}</p>
      </div>
    )
  }

  return (
    <div className="data-table-wrapper">
      <table className="data-table">
        <thead>
          <tr>
            {columns.map((col) => (
              <th key={col.key}>{col.label}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {data.map((row, idx) => (
            <tr key={row[keyField] ?? idx}>
              {columns.map((col) => (
                <td key={col.key}>
                  {col.render ? col.render(row) : row[col.key]}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export function StatusBadge({ value, type }) {
  const normalized = (value || '').toString().toUpperCase()
  let className = 'badge badge-default'

  if (type === 'mode' || normalized === 'WFO') className = 'badge badge-wfo'
  else if (normalized === 'WFH') className = 'badge badge-wfh'
  else if (normalized === 'PENDING' || normalized === 'CLASSIFICATION_PENDING' || type === 'pending') className = 'badge badge-pending'
  else if (normalized === 'LATE' || type === 'late') className = 'badge badge-late'

  return <span className={className}>{value || '—'}</span>
}
