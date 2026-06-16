import { useCallback, useState } from 'react'
import { ChevronDown, ChevronRight, RefreshCw } from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import DataTable, { StatusBadge } from '../components/tables/DataTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import PaginationBar from '../components/common/PaginationBar'
import { useToast } from '../components/common/Toast'
import { useAuth } from '../auth/AuthContext'
import usePolling from '../hooks/usePolling'
import {
  getAttendanceHistory,
  getAttendanceEventsForDate,
  getAttendanceSessionsForDate,
} from '../api/attendanceApi'
import { formatDate, formatDuration, formatTime, formatLastUpdated, getApiErrorMessage } from '../utils/format'
import './DashboardPages.css'

function DayDetailPanel({ date, expanded, onToggle }) {
  const [eventsPage, setEventsPage] = useState({ data: null, page: 0, size: 10, loading: false, error: null })
  const [sessionsPage, setSessionsPage] = useState({ data: null, page: 0, size: 10, loading: false, error: null })

  const loadDetail = useCallback(async (eventsPg = 0, sessionsPg = 0, eventsSize = 10, sessionsSize = 10) => {
    if (!date) return
    setEventsPage((prev) => ({ ...prev, loading: true, error: null }))
    setSessionsPage((prev) => ({ ...prev, loading: true, error: null }))
    try {
      const [events, sessions] = await Promise.all([
        getAttendanceEventsForDate(date, { page: eventsPg, size: eventsSize }),
        getAttendanceSessionsForDate(date, { page: sessionsPg, size: sessionsSize }),
      ])
      setEventsPage({ data: events, page: eventsPg, size: eventsSize, loading: false, error: null })
      setSessionsPage({ data: sessions, page: sessionsPg, size: sessionsSize, loading: false, error: null })
    } catch (err) {
      const message = getApiErrorMessage(err)
      setEventsPage((prev) => ({ ...prev, loading: false, error: message }))
      setSessionsPage((prev) => ({ ...prev, loading: false, error: message }))
    }
  }, [date])

  const handleToggle = async () => {
    const next = !expanded
    onToggle(next)
    if (next && !eventsPage.data && !eventsPage.loading) {
      await loadDetail()
    }
  }

  if (!expanded) {
    return (
      <button type="button" className="btn btn-secondary btn-sm" onClick={handleToggle}>
        <ChevronRight size={14} />
        View sessions
      </button>
    )
  }

  const eventRows = eventsPage.data?.content || []
  const sessionRows = sessionsPage.data?.content || []

  return (
    <div className="history-day-detail">
      <button type="button" className="btn btn-secondary btn-sm" onClick={handleToggle}>
        <ChevronDown size={14} />
        Hide sessions
      </button>

      <h4>Sessions</h4>
      {sessionsPage.loading && !sessionsPage.data ? (
        <LoadingSpinner message="Loading sessions..." />
      ) : sessionRows.length === 0 ? (
        <p className="last-updated">No sessions recorded for this date.</p>
      ) : (
        <>
          <div className="history-detail-table">
            <table>
              <thead>
                <tr>
                  <th>Mode</th>
                  <th>Check-in</th>
                  <th>Check-out</th>
                  <th>Source</th>
                  <th>Auto checkout</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {sessionRows.map((session) => (
                  <tr key={session.id}>
                    <td>
                      <StatusBadge value={session.sessionMode} type="mode" />
                    </td>
                    <td>{formatTime(session.checkInTime)}</td>
                    <td>{formatTime(session.checkOutTime)}</td>
                    <td>{session.checkInEventType}</td>
                    <td>{session.autoCheckoutEligible ? 'Yes' : 'No'}</td>
                    <td>
                      <StatusBadge value={session.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <PaginationBar
            page={sessionsPage.page}
            size={sessionsPage.size}
            totalElements={sessionsPage.data?.totalElements ?? 0}
            totalPages={sessionsPage.data?.totalPages ?? 0}
            loading={sessionsPage.loading}
            error={sessionsPage.error}
            onPageChange={(nextPage) => loadDetail(eventsPage.page, nextPage, eventsPage.size, sessionsPage.size)}
            onSizeChange={(nextSize) => loadDetail(0, 0, eventsPage.size, nextSize)}
          />
        </>
      )}

      <h4>Events</h4>
      {eventsPage.loading && !eventsPage.data ? (
        <LoadingSpinner message="Loading events..." />
      ) : eventRows.length === 0 ? (
        <p className="last-updated">No events recorded for this date.</p>
      ) : (
        <>
          <div className="history-detail-table">
            <table>
              <thead>
                <tr>
                  <th>Time</th>
                  <th>Event</th>
                  <th>Trigger</th>
                  <th>Source</th>
                </tr>
              </thead>
              <tbody>
                {eventRows.map((event) => (
                  <tr key={event.id}>
                    <td>{formatTime(event.eventTime)}</td>
                    <td>
                      <StatusBadge value={event.eventType} />
                    </td>
                    <td>{event.triggerMode}</td>
                    <td>{event.source}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <PaginationBar
            page={eventsPage.page}
            size={eventsPage.size}
            totalElements={eventsPage.data?.totalElements ?? 0}
            totalPages={eventsPage.data?.totalPages ?? 0}
            loading={eventsPage.loading}
            error={eventsPage.error}
            onPageChange={(nextPage) => loadDetail(nextPage, sessionsPage.page, eventsPage.size, sessionsPage.size)}
            onSizeChange={(nextSize) => loadDetail(0, sessionsPage.page, nextSize, sessionsPage.size)}
          />
        </>
      )}
    </div>
  )
}

export default function AttendanceHistoryPage() {
  const toast = useToast()
  const { isAuthenticated } = useAuth()
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [expandedDate, setExpandedDate] = useState(null)

  const fetchHistory = useCallback(() => {
    const params = { page, size }
    if (from) params.from = from
    if (to) params.to = to
    return getAttendanceHistory(params)
  }, [page, size, from, to])

  const { data, loading, lastUpdated, refresh, error } = usePolling(fetchHistory, 60000, {
    enabled: isAuthenticated,
  })

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
    setExpandedDate(null)
    refresh()
  }

  const columns = [
    {
      key: 'attendanceDate',
      label: 'Date',
      render: (row) => formatDate(row.attendanceDate),
    },
    {
      key: 'attendanceMode',
      label: 'Final Mode',
      render: (row) => (
        <StatusBadge
          value={row.processingStatus === 'CLASSIFICATION_PENDING' ? 'Pending' : row.attendanceMode}
          type="mode"
        />
      ),
    },
    {
      key: 'checkInTime',
      label: 'First Check-in',
      render: (row) => formatTime(row.checkInTime),
    },
    {
      key: 'checkOutTime',
      label: 'Final Checkout',
      render: (row) => formatTime(row.checkOutTime),
    },
    {
      key: 'totalOfficeMinutes',
      label: 'Office Time',
      render: (row) => (row.totalOfficeMinutes != null ? formatDuration(row.totalOfficeMinutes) : '—'),
    },
    {
      key: 'status',
      label: 'Status',
      render: (row) => <StatusBadge value={row.status} />,
    },
    {
      key: 'flags',
      label: 'Flags',
      render: (row) => (
        <span className="history-flags">
          {row.late && <StatusBadge value="Late" type="late" />}
          {row.status === 'MISSING_CHECKOUT' && <StatusBadge value="Missing checkout" type="late" />}
          {!row.late && row.status !== 'MISSING_CHECKOUT' && '—'}
        </span>
      ),
    },
    {
      key: 'details',
      label: 'Details',
      render: (row) => (
        <DayDetailPanel
          date={row.attendanceDate}
          expanded={expandedDate === row.attendanceDate}
          onToggle={(open) => setExpandedDate(open ? row.attendanceDate : null)}
        />
      ),
    },
  ]

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
                className="btn btn-secondary btn-sm"
                onClick={() => {
                  setFrom('')
                  setTo('')
                  setPage(0)
                  setExpandedDate(null)
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
          ) : records.length === 0 ? (
            <p className="last-updated">No attendance records found</p>
          ) : (
            <>
              <DataTable
                columns={columns}
                data={records}
                keyField="id"
                emptyMessage="No attendance records found"
              />
              <PaginationBar
                page={page}
                size={size}
                totalElements={data?.totalElements ?? 0}
                totalPages={data?.totalPages ?? 0}
                loading={loading}
                error={error ? getApiErrorMessage(error) : null}
                onPageChange={setPage}
                onSizeChange={(nextSize) => {
                  setSize(nextSize)
                  setPage(0)
                  setExpandedDate(null)
                }}
              />
            </>
          )}
        </div>
      </div>
    </>
  )
}
