import { useCallback, useEffect, useState } from 'react'
import { RefreshCw, Users, UserCheck, Building2, Home, Clock, AlertTriangle, ShieldAlert, UserX, X } from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import KpiCard from '../components/cards/KpiCard'
import PieChartCard from '../components/charts/PieChartCard'
import BarChartCard from '../components/charts/BarChartCard'
import DataTable, { StatusBadge } from '../components/tables/DataTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import PaginationBar from '../components/common/PaginationBar'
import { useToast } from '../components/common/Toast'
import { useAuth } from '../auth/AuthContext'
import usePolling from '../hooks/usePolling'
import { getManagerDashboard, getManagerDashboardDrilldown } from '../api/dashboardApi'
import { formatDateTime, formatDuration, formatLastUpdated, formatTime, getApiErrorMessage } from '../utils/format'
import './DashboardPages.css'

const DRILLDOWN_TYPES = {
  TEAM_SIZE: 'TEAM_SIZE',
  PRESENT: 'PRESENT',
  WFO: 'WFO',
  WFH: 'WFH',
  ABSENT: 'ABSENT',
  OUTLIERS: 'OUTLIERS',
}

const CLICKABLE_KPIS = new Set(Object.values(DRILLDOWN_TYPES))

const DRILLDOWN_TITLES = {
  [DRILLDOWN_TYPES.TEAM_SIZE]: 'Team Members',
  [DRILLDOWN_TYPES.PRESENT]: 'Present Today',
  [DRILLDOWN_TYPES.WFO]: 'WFO Today',
  [DRILLDOWN_TYPES.WFH]: 'WFH Today',
  [DRILLDOWN_TYPES.ABSENT]: 'Absent Today',
  [DRILLDOWN_TYPES.OUTLIERS]: 'Open Outlier Alerts',
}

const KPI_CARD_CONFIG = [
  { type: DRILLDOWN_TYPES.TEAM_SIZE, label: 'Team Size', kpiKey: 'teamSize', icon: Users, accent: true },
  { type: DRILLDOWN_TYPES.PRESENT, label: 'Present Today', kpiKey: 'presentToday', icon: UserCheck },
  { type: DRILLDOWN_TYPES.ABSENT, label: 'Absent Today', kpiKey: 'absentToday', icon: UserX },
  { type: DRILLDOWN_TYPES.WFO, label: 'WFO Today', kpiKey: 'wfoToday', icon: Building2 },
  { type: DRILLDOWN_TYPES.WFH, label: 'WFH Today', kpiKey: 'wfhToday', icon: Home },
  { type: null, label: 'Late Today', kpiKey: 'lateToday', icon: Clock },
  { type: null, label: 'Pending Classification', kpiKey: 'pendingClassification', icon: AlertTriangle },
  { type: DRILLDOWN_TYPES.OUTLIERS, label: 'Open Outliers', kpiKey: 'openOutliers', icon: ShieldAlert },
]

const EMPTY_MESSAGES = {
  [DRILLDOWN_TYPES.TEAM_SIZE]: 'No active employees found in your team',
  [DRILLDOWN_TYPES.PRESENT]: 'No employees marked present for today',
  [DRILLDOWN_TYPES.WFO]: 'No WFO attendance records for today',
  [DRILLDOWN_TYPES.WFH]: 'No WFH attendance records for today',
  [DRILLDOWN_TYPES.ABSENT]: 'No absent employees for today',
  [DRILLDOWN_TYPES.OUTLIERS]: 'No open outlier alerts for your team',
}

function buildDrilldownTitle(type, totalElements) {
  const base = DRILLDOWN_TITLES[type] || 'Details'
  const unit = type === DRILLDOWN_TYPES.OUTLIERS ? 'alerts' : 'employees'
  return `${base} — ${totalElements ?? 0} ${unit}`
}

export default function ManagerDashboard() {
  const toast = useToast()
  const { isAuthenticated } = useAuth()
  const [selectedDrilldownType, setSelectedDrilldownType] = useState(DRILLDOWN_TYPES.TEAM_SIZE)
  const [drilldownPage, setDrilldownPage] = useState(0)
  const [drilldownSize, setDrilldownSize] = useState(10)
  const [drilldownData, setDrilldownData] = useState(null)
  const [drilldownLoading, setDrilldownLoading] = useState(false)
  const [drilldownError, setDrilldownError] = useState(null)
  const [drilldownRefreshKey, setDrilldownRefreshKey] = useState(0)

  const fetchDashboard = useCallback(() => getManagerDashboard(), [])

  const {
    data: dashboard,
    loading,
    lastUpdated,
    refresh,
  } = usePolling(fetchDashboard, 60000, { enabled: isAuthenticated })

  useEffect(() => {
    if (!isAuthenticated) {
      return undefined
    }

    let cancelled = false

    const loadDrilldown = async () => {
      setDrilldownLoading(true)
      setDrilldownError(null)

      console.debug('Fetching manager drilldown', {
        type: selectedDrilldownType,
        page: drilldownPage,
        size: drilldownSize,
      })

      try {
        const data = await getManagerDashboardDrilldown({
          type: selectedDrilldownType,
          page: drilldownPage,
          size: drilldownSize,
        })
        if (!cancelled) {
          setDrilldownData(data)
        }
      } catch (error) {
        if (!cancelled) {
          setDrilldownError(getApiErrorMessage(error, 'Failed to load drill-down details'))
          setDrilldownData(null)
        }
      } finally {
        if (!cancelled) {
          setDrilldownLoading(false)
        }
      }
    }

    loadDrilldown()

    const intervalId = setInterval(() => {
      if (document.visibilityState === 'visible') {
        loadDrilldown()
      }
    }, 60000)

    return () => {
      cancelled = true
      clearInterval(intervalId)
    }
  }, [selectedDrilldownType, drilldownPage, drilldownSize, isAuthenticated, drilldownRefreshKey])

  const handleRefresh = async () => {
    await refresh()
    setDrilldownRefreshKey((key) => key + 1)
    toast.info('Dashboard refreshed')
  }

  const handleKpiClick = (type) => {
    if (!type || !CLICKABLE_KPIS.has(type)) return
    setSelectedDrilldownType(type)
    setDrilldownPage(0)
    setDrilldownData(null)
    setDrilldownError(null)
  }

  const handleClearFilter = () => {
    handleKpiClick(DRILLDOWN_TYPES.TEAM_SIZE)
  }

  const kpis = dashboard?.kpis
  const drilldownRows = drilldownData?.content || []
  const isOutlierView = selectedDrilldownType === DRILLDOWN_TYPES.OUTLIERS

  const employeeColumns = [
    { key: 'employeeName', label: 'Employee Name' },
    { key: 'email', label: 'Email' },
    { key: 'assignedOfficeName', label: 'Assigned Office', render: (row) => row.assignedOfficeName || '—' },
    { key: 'todayStatus', label: 'Today Status', render: (row) => <StatusBadge value={row.todayStatus} /> },
    {
      key: 'attendanceMode',
      label: 'Attendance Mode',
      render: (row) => (row.attendanceMode ? <StatusBadge value={row.attendanceMode} type="mode" /> : '—'),
    },
    { key: 'firstCheckInTime', label: 'First Check-in', render: (row) => formatTime(row.firstCheckInTime) },
    { key: 'finalCheckOutTime', label: 'Final Checkout', render: (row) => formatTime(row.finalCheckOutTime) },
    {
      key: 'totalOfficeMinutes',
      label: 'Total Office Minutes',
      render: (row) => (row.totalOfficeMinutes != null ? formatDuration(row.totalOfficeMinutes) : '—'),
    },
    {
      key: 'currentSessionStatus',
      label: 'Session Status',
      render: (row) => (row.currentSessionStatus ? <StatusBadge value={row.currentSessionStatus} /> : '—'),
    },
    { key: 'outlierCount', label: 'Outlier Count', render: (row) => row.outlierCount ?? 0 },
  ]

  const outlierColumns = [
    { key: 'employeeName', label: 'Employee Name' },
    { key: 'outlierType', label: 'Outlier Type' },
    { key: 'severity', label: 'Severity', render: (row) => <StatusBadge value={row.severity} /> },
    { key: 'description', label: 'Description' },
    {
      key: 'detectedAt',
      label: 'Detected Date',
      render: (row) => formatDateTime(row.detectedAt),
    },
    {
      key: 'outlierStatus',
      label: 'Status',
      render: (row) => <StatusBadge value={row.outlierStatus} />,
    },
  ]

  if (loading && !dashboard) {
    return <LoadingSpinner fullPage message="Loading manager dashboard..." />
  }

  return (
    <>
      <Topbar
        title="Manager Dashboard"
        actions={
          <button type="button" className="btn btn-secondary btn-sm" onClick={handleRefresh}>
            <RefreshCw size={16} />
            Refresh
          </button>
        }
      />
      <div className="app-layout__content dashboard-page">
        <div className="page-header">
          <p className="last-updated">Last updated: {formatLastUpdated(lastUpdated)}</p>
        </div>

        <div className="grid-kpi">
          {KPI_CARD_CONFIG.map(({ type, label, kpiKey, icon, accent }) => (
            <KpiCard
              key={label}
              label={label}
              value={kpis?.[kpiKey]}
              icon={icon}
              accent={accent}
              clickable={Boolean(type)}
              selected={type === selectedDrilldownType}
              onClick={type ? () => handleKpiClick(type) : undefined}
            />
          ))}
        </div>

        <div className="grid-charts">
          <PieChartCard title="Today's Mode Distribution" data={dashboard?.modePieChart || []} />
          <BarChartCard title="Monthly Attendance by Employee" data={dashboard?.monthlyBarChart || []} />
        </div>

        <div className="card drilldown-panel">
          <div className="card-header drilldown-panel__header">
            <h3 className="card-title">
              {buildDrilldownTitle(selectedDrilldownType, drilldownData?.totalElements)}
            </h3>
            {selectedDrilldownType !== DRILLDOWN_TYPES.TEAM_SIZE && (
              <button type="button" className="btn btn-secondary btn-sm" onClick={handleClearFilter}>
                <X size={16} />
                Show all team members
              </button>
            )}
          </div>

          {drilldownError && (
            <div className="drilldown-panel__error" role="alert">
              {drilldownError}
            </div>
          )}

          <div className="drilldown-panel__body">
            {drilldownLoading && (
              <div className="drilldown-panel__loading">
                <LoadingSpinner message="Loading drill-down details..." />
              </div>
            )}

            {!drilldownLoading && (
              <>
                <DataTable
                  key={selectedDrilldownType}
                  columns={isOutlierView ? outlierColumns : employeeColumns}
                  data={drilldownRows}
                  keyField={isOutlierView ? 'outlierId' : 'employeeId'}
                  emptyMessage={EMPTY_MESSAGES[selectedDrilldownType]}
                />
                <PaginationBar
                  page={drilldownPage}
                  size={drilldownSize}
                  totalElements={drilldownData?.totalElements ?? 0}
                  totalPages={drilldownData?.totalPages ?? 0}
                  loading={drilldownLoading}
                  error={drilldownError}
                  onPageChange={setDrilldownPage}
                  onSizeChange={(nextSize) => {
                    setDrilldownSize(nextSize)
                    setDrilldownPage(0)
                  }}
                />
              </>
            )}
          </div>
        </div>
      </div>
    </>
  )
}
