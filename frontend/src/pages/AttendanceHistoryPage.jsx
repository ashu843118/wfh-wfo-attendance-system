import { useCallback, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import DataTable, { StatusBadge } from '../components/tables/DataTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import { useToast } from '../components/common/Toast'
import { useAuth } from '../auth/AuthContext'
import usePolling from '../hooks/usePolling'
import { getAttendanceHistory, getAttendanceEvents } from '../api/attendanceApi'
import { formatDate, formatTime, formatLastUpdated, getApiErrorMessage } from '../utils/format'
import './DashboardPages.css'

export default function AttendanceHistoryPage() {
  const toast = useToast()
  const { isAuthenticated } = useAuth()
  const [page, setPage] = useState(0)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')

  const [view, setView] = useState('summary')

  const fetchHistory = useCallback(() => {
    const params = { page, size: 20 }
    if (from) params.from = from
    if (to) params.to = to
    return view === 'summary' ? getAttendanceHistory(params) : getAttendanceEvents({ ...params, size: 50 })
  }, [page, from, to, view])

  const { data, loading, lastUpdated, refresh } = usePolling(fetchHistory, 60000, { enabled: isAuthenticated })

  const records = data?.content || []

  const handleRefresh = async () => {
    try {
      await refresh()
      toast.info('History refreshed')
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  const handleFilter = (e) => {
    e.preventDefault()
    setPage(0)
    refresh()
  }

  const summaryColumns = [
    {
      key: 'attendanceDate',
      label: 'Date',
      render: (row) => formatDate(row.attendanceDate),
    },
    {
      key: 'status',
      label: 'Status',
      render: (row) => <StatusBadge value={row.status} />,
    },
    {
      key: 'attendanceMode',
      label: 'Mode',
      render: (row) => (
        <StatusBadge
          value={row.processingStatus === 'CLASSIFICATION_PENDING' ? 'Pending' : row.attendanceMode}
          type="mode"
        />
      ),
    },
    {
      key: 'processingStatus',
      label: 'Processing',
      render: (row) =>
        row.processingStatus === 'CLASSIFICATION_PENDING' ? (
          <StatusBadge value="Pending" type="pending" />
        ) : (
          row.processingStatus || '—'
        ),
    },
    {
      key: 'late',
      label: 'Late',
      render: (row) => (row.late ? <StatusBadge value="Late" type="late" /> : '—'),
    },
    {
      key: 'checkInTime',
      label: 'Check In',
      render: (row) => formatTime(row.checkInTime),
    },
    {
      key: 'checkOutTime',
      label: 'Check Out',
      render: (row) => formatTime(row.checkOutTime),
    },
    { key: 'source', label: 'Source' },
  ]

  const eventColumns = [
    {
      key: 'attendanceDate',
      label: 'Date',
      render: (row) => formatDate(row.attendanceDate),
    },
    {
      key: 'eventType',
      label: 'Event',
      render: (row) => <StatusBadge value={row.eventType} />,
    },
    {
      key: 'eventTime',
      label: 'Time',
      render: (row) => formatTime(row.eventTime),
    },
    {
      key: 'triggerMode',
      label: 'Trigger',
    },
    { key: 'source', label: 'Source' },
  ]

  const columns = view === 'summary' ? summaryColumns : eventColumns

  return (
    <>
      <Topbar
        title="Attendance History"
        actions={
          <button type="button" className="btn btn-secondary btn-sm" onClick={handleRefresh}>
            <RefreshCw size={16} />
            Refresh
          </button>
        }
      />
      <div className="app-layout__content">
        <div className="page-header">
          <p className="last-updated">Last updated: {formatLastUpdated(lastUpdated)}</p>
        </div>

        <div className="card crud-form">
          <form onSubmit={handleFilter}>
            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="from">From</label>
                <input id="from" type="date" value={from} onChange={(e) => setFrom(e.target.value)} />
              </div>
              <div className="form-group">
                <label htmlFor="to">To</label>
                <input id="to" type="date" value={to} onChange={(e) => setTo(e.target.value)} />
              </div>
            </div>
            <div className="crud-form__actions">
              <button type="submit" className="btn btn-primary btn-sm">
                Apply Filter
              </button>
              <button
                type="button"
                className={`btn btn-sm ${view === 'summary' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => {
                  setView('summary')
                  setPage(0)
                }}
              >
                Daily Summary
              </button>
              <button
                type="button"
                className={`btn btn-sm ${view === 'events' ? 'btn-primary' : 'btn-secondary'}`}
                onClick={() => {
                  setView('events')
                  setPage(0)
                }}
              >
                Event Audit
              </button>
              <button
                type="button"
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  setFrom('')
                  setTo('')
                  setPage(0)
                }}
              >
                Clear
              </button>
            </div>
          </form>
        </div>

        <div className="card">
          {loading && !data ? (
            <LoadingSpinner message="Loading history..." />
          ) : (
            <>
              <DataTable
                columns={columns}
                data={records}
                keyField="id"
                emptyMessage={view === 'summary' ? 'No attendance records found' : 'No attendance events found'}
              />
              {data && data.totalPages > 1 && (
                <div className="pagination">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    Previous
                  </button>
                  <span className="last-updated">
                    Page {page + 1} of {data.totalPages}
                  </span>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </>
  )
}
