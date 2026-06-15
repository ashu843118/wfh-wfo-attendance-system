import { useCallback, useState } from 'react'
import {
  RefreshCw,
  LogIn,
  LogOut,
  Calendar,
  Building2,
  Home,
  Clock,
  AlertTriangle,
  MapPin,
  Info,
} from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import KpiCard from '../components/cards/KpiCard'
import LineChartCard from '../components/charts/LineChartCard'
import DataTable, { StatusBadge } from '../components/tables/DataTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import { useToast } from '../components/common/Toast'
import { useAuth } from '../auth/AuthContext'
import usePolling from '../hooks/usePolling'
import useGeolocation from '../hooks/useGeolocation'
import useAutoAttendanceTracker, { PERMISSION_DENIED_MESSAGE } from '../hooks/useAutoAttendanceTracker'
import { getEmployeeDashboard } from '../api/dashboardApi'
import { checkIn, checkOut, confirmWfhCheckIn, dismissWfhPrompt, getTodayAttendance } from '../api/attendanceApi'
import { formatLastUpdated, getApiErrorMessage } from '../utils/format'
import './DashboardPages.css'

const AUTO_ATTENDANCE_INFO =
  'Auto attendance is active while this app is open. If you are inside your assigned office geofence, WFO check-in is recorded automatically. If you are outside office geofence, you can confirm WFH check-in manually. Auto checkout applies only to auto WFO check-ins. Manual or WFH check-ins require manual checkout or are closed at end of day. Location is used only for attendance and is not tracked when the app is closed.'

export default function EmployeeDashboard() {
  const toast = useToast()
  const { isAuthenticated } = useAuth()
  const { getLocation, loading: geoLoading } = useGeolocation()
  const [actionLoading, setActionLoading] = useState(null)
  const [showWfhModal, setShowWfhModal] = useState(false)
  const [showInfo, setShowInfo] = useState(false)
  const [wfhLoading, setWfhLoading] = useState(false)

  const fetchDashboard = useCallback(() => getEmployeeDashboard(), [])
  const fetchToday = useCallback(() => getTodayAttendance(), [])

  const {
    data: dashboard,
    loading: dashLoading,
    lastUpdated: dashUpdated,
    refresh: refreshDash,
  } = usePolling(fetchDashboard, 60000, { enabled: isAuthenticated })

  const {
    data: todayRecord,
    loading: todayLoading,
    lastUpdated: todayUpdated,
    refresh: refreshToday,
  } = usePolling(fetchToday, 30000, { enabled: isAuthenticated })

  const handleSignalProcessed = useCallback(
    async (response) => {
      if (response?.actionTaken) {
        await Promise.all([refreshDash(), refreshToday()])
        if (response.userMessage) {
          toast.success(response.userMessage)
        } else if (response.actionTaken.status === 'CHECKED_IN') {
          toast.success('Auto check-in recorded')
        } else if (response.actionTaken.status === 'CHECKED_OUT') {
          toast.success('Auto check-out recorded')
        }
      } else if (response?.todaySummary) {
        await refreshToday()
      }
    },
    [refreshDash, refreshToday, toast],
  )

  const handleWfhConfirmationRequired = useCallback(() => {
    setShowWfhModal(true)
  }, [])

  const { trackingStateLabel, assignedOfficeName, error: autoError, resetWfhPrompt } = useAutoAttendanceTracker({
    enabled: isAuthenticated,
    onSignalProcessed: handleSignalProcessed,
    onWfhConfirmationRequired: handleWfhConfirmationRequired,
  })

  const handleRefresh = async () => {
    await Promise.all([refreshDash(), refreshToday()])
    toast.info('Dashboard refreshed')
  }

  const handleCheckIn = async () => {
    setActionLoading('in')
    try {
      const location = await getLocation()
      const result = await checkIn(location)
      toast.success('Check-in recorded successfully')
      if (result.processingStatus === 'CLASSIFICATION_PENDING') {
        toast.info('Classification is pending — your attendance mode will update shortly')
      }
      setShowWfhModal(false)
      resetWfhPrompt()
      await Promise.all([refreshDash(), refreshToday()])
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setActionLoading(null)
    }
  }

  const handleWfhCheckIn = async () => {
    setWfhLoading(true)
    try {
      const location = await getLocation()
      await confirmWfhCheckIn(location)
      toast.success('WFH check-in recorded successfully.')
      setShowWfhModal(false)
      resetWfhPrompt()
      await Promise.all([refreshDash(), refreshToday()])
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setWfhLoading(false)
    }
  }

  const handleCheckOut = async () => {
    setActionLoading('out')
    try {
      const location = await getLocation()
      const result = await checkOut(location)
      toast.success('Check-out recorded successfully')
      if (result.processingStatus === 'CLASSIFICATION_PENDING') {
        toast.info('Classification is pending — your attendance mode will update shortly')
      }
      await Promise.all([refreshDash(), refreshToday()])
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setActionLoading(null)
    }
  }

  const kpis = dashboard?.kpis
  const isPending = todayRecord?.processingStatus === 'CLASSIFICATION_PENDING'
  const hasOpenSession = todayRecord?.currentSessionStatus === 'OPEN' || todayRecord?.status === 'CHECKED_IN'
  const isDayClosed =
    todayRecord?.status === 'SYSTEM_CLOSED' ||
    todayRecord?.status === 'MISSING_CHECKOUT'

  const permissionDenied = autoError?.message === PERMISSION_DENIED_MESSAGE

  const columns = [
    { key: 'date', label: 'Date' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
    { key: 'mode', label: 'Mode', render: (row) => <StatusBadge value={row.mode} type="mode" /> },
    { key: 'late', label: 'Late', render: (row) => (row.late ? <StatusBadge value="Late" type="late" /> : '—') },
    { key: 'checkInTime', label: 'Check In' },
    { key: 'checkOutTime', label: 'Check Out' },
  ]

  if (dashLoading && !dashboard) {
    return <LoadingSpinner fullPage message="Loading dashboard..." />
  }

  return (
    <>
      <Topbar
        title="Employee Dashboard"
        actions={
          <button type="button" className="btn btn-secondary btn-sm" onClick={handleRefresh}>
            <RefreshCw size={16} />
            Refresh
          </button>
        }
      />
      <div className="app-layout__content dashboard-page">
        <div className="page-header">
          <p className="last-updated">
            Last updated: {formatLastUpdated(dashUpdated)} · Attendance status: {formatLastUpdated(todayUpdated)}
          </p>
        </div>

        <div className="checkin-panel card">
          <div className="checkin-panel__header">
            <h2>Today&apos;s Attendance</h2>
            <div className="checkin-panel__header-actions">
              <button
                type="button"
                className="btn btn-secondary btn-sm info-btn"
                onClick={() => setShowInfo((prev) => !prev)}
                aria-label="Auto attendance information"
              >
                <Info size={16} />
              </button>
              {isPending && (
                <span className="badge badge-pending">
                  <AlertTriangle size={12} style={{ marginRight: 4 }} />
                  Classification Pending
                </span>
              )}
            </div>
          </div>

          {showInfo && <p className="auto-track-panel__hint">{AUTO_ATTENDANCE_INFO}</p>}

          <div className="auto-track-panel">
            <div className="auto-track-panel__state">
              <MapPin size={14} />
              <span>{trackingStateLabel}</span>
            </div>
            {assignedOfficeName && (
              <p className="auto-track-panel__office">
                Assigned office: <strong>{assignedOfficeName}</strong>
              </p>
            )}
            {permissionDenied && (
              <p className="auto-track-panel__error">{PERMISSION_DENIED_MESSAGE}</p>
            )}
            {autoError && !permissionDenied && (
              <p className="auto-track-panel__error">{autoError.message}</p>
            )}
          </div>

          {showWfhModal && !hasOpenSession && (
            <div className="wfh-modal card">
              <p>You are currently outside office geofence. Do you want to check in as Work From Home?</p>
              <div className="form-actions">
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={async () => {
                    setShowWfhModal(false)
                    try {
                      await dismissWfhPrompt()
                    } catch (err) {
                      toast.error(getApiErrorMessage(err))
                    }
                    resetWfhPrompt()
                  }}
                >
                  Not now
                </button>
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleWfhCheckIn}
                  disabled={wfhLoading || geoLoading}
                >
                  {wfhLoading ? 'Checking in...' : 'Check in as WFH'}
                </button>
              </div>
            </div>
          )}

          {todayLoading && !todayRecord ? (
            <LoadingSpinner message="Loading status..." />
          ) : (
            <div className="checkin-panel__body">
              <div className="checkin-panel__status">
                <div className="checkin-panel__stat">
                  <span className="label">Status</span>
                  <StatusBadge value={todayRecord?.status || 'Not checked in'} />
                </div>
                <div className="checkin-panel__stat">
                  <span className="label">Mode</span>
                  <StatusBadge value={todayRecord?.attendanceMode || (isPending ? 'Pending' : '—')} type="mode" />
                </div>
                <div className="checkin-panel__stat">
                  <span className="label">Office Time</span>
                  <span>
                    {todayRecord?.totalOfficeMinutes != null
                      ? `${todayRecord.totalOfficeMinutes} min`
                      : '—'}
                  </span>
                </div>
                <div className="checkin-panel__stat">
                  <span className="label">Session</span>
                  <StatusBadge value={todayRecord?.currentSessionStatus || 'NONE'} />
                </div>
                <div className="checkin-panel__stat">
                  <span className="label">Check In</span>
                  <span>{todayRecord?.checkInTime ? new Date(todayRecord.checkInTime).toLocaleTimeString() : '—'}</span>
                </div>
                <div className="checkin-panel__stat">
                  <span className="label">Check Out</span>
                  <span>{todayRecord?.checkOutTime ? new Date(todayRecord.checkOutTime).toLocaleTimeString() : '—'}</span>
                </div>
              </div>

              <div className="checkin-panel__actions">
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={handleCheckIn}
                  disabled={actionLoading || geoLoading || hasOpenSession || isDayClosed}
                >
                  <LogIn size={18} />
                  {actionLoading === 'in' ? 'Checking in...' : 'Check In (Manual)'}
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={handleCheckOut}
                  disabled={actionLoading || geoLoading || !hasOpenSession || isDayClosed}
                >
                  <LogOut size={18} />
                  {actionLoading === 'out' ? 'Checking out...' : 'Check Out (Manual)'}
                </button>
              </div>
            </div>
          )}
        </div>

        <div className="grid-kpi">
          <KpiCard label="Present Days" value={kpis?.presentDaysThisMonth} icon={Calendar} accent />
          <KpiCard label="WFO Days" value={kpis?.wfoDaysThisMonth} icon={Building2} />
          <KpiCard label="WFH Days" value={kpis?.wfhDaysThisMonth} icon={Home} />
          <KpiCard label="Late Days" value={kpis?.lateDaysThisMonth} icon={Clock} />
          <KpiCard label="Missing Checkout" value={kpis?.missingCheckoutDaysThisMonth} icon={AlertTriangle} />
          <KpiCard label="Pending Classification" value={kpis?.pendingClassification} icon={AlertTriangle} />
        </div>

        <div className="grid-charts">
          <LineChartCard title="Last 30 Days — WFO / WFH Trend" data={dashboard?.wfoWfhTrend || []} />
        </div>

        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Recent Attendance</h3>
          </div>
          <DataTable
            columns={columns}
            data={dashboard?.recentAttendance || []}
            keyField="date"
            emptyMessage="No recent attendance records"
          />
        </div>
      </div>
    </>
  )
}
