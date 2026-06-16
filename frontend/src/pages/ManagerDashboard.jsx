import { useCallback, useState } from 'react'
import { RefreshCw, Users, UserCheck, Building2, Home, Clock, AlertTriangle, ShieldAlert, UserX } from 'lucide-react'
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
import { getManagerDashboard, getManagerOutliers, getManagerTeamAttendance } from '../api/dashboardApi'
import { formatDateTime, formatLastUpdated, formatTime, getApiErrorMessage } from '../utils/format'
import './DashboardPages.css'

export default function ManagerDashboard() {
  const toast = useToast()
  const { isAuthenticated } = useAuth()
  const [teamPage, setTeamPage] = useState(0)
  const [teamSize, setTeamSize] = useState(20)
  const [outlierPage, setOutlierPage] = useState(0)
  const [outlierSize, setOutlierSize] = useState(20)

  const fetchDashboard = useCallback(() => getManagerDashboard(), [])

  const fetchTeamAttendance = useCallback(
    () => getManagerTeamAttendance({ page: teamPage, size: teamSize }),
    [teamPage, teamSize]
  )

  const fetchOutliers = useCallback(
    () => getManagerOutliers(outlierPage, outlierSize),
    [outlierPage, outlierSize]
  )

  const {
    data: dashboard,
    loading,
    lastUpdated,
    refresh,
  } = usePolling(fetchDashboard, 60000, { enabled: isAuthenticated })

  const {
    data: teamPageData,
    loading: teamLoading,
    refresh: refreshTeam,
  } = usePolling(fetchTeamAttendance, 60000, { enabled: isAuthenticated })

  const {
    data: outliersPage,
    loading: outliersLoading,
    refresh: refreshOutliers,
  } = usePolling(fetchOutliers, 60000, { enabled: isAuthenticated })

  const handleRefresh = async () => {
    await Promise.all([refresh(), refreshTeam(), refreshOutliers()])
    toast.info('Dashboard refreshed')
  }

  const kpis = dashboard?.kpis
  const teamRows = teamPageData?.content || []
  const outliers = outliersPage?.content || []

  const teamColumns = [
    { key: 'employeeName', label: 'Employee' },
    { key: 'status', label: 'Status', render: (row) => <StatusBadge value={row.status} /> },
    { key: 'mode', label: 'Mode', render: (row) => <StatusBadge value={row.mode} type="mode" /> },
    { key: 'late', label: 'Late', render: (row) => (row.late ? <StatusBadge value="Late" type="late" /> : '—') },
    { key: 'checkInTime', label: 'Check In', render: (row) => formatTime(row.checkInTime) },
    { key: 'checkOutTime', label: 'Check Out', render: (row) => formatTime(row.checkOutTime) },
  ]

  const outlierColumns = [
    { key: 'employeeId', label: 'Employee ID' },
    { key: 'outlierType', label: 'Type' },
    { key: 'severity', label: 'Severity', render: (row) => <StatusBadge value={row.severity} /> },
    { key: 'description', label: 'Description' },
    { key: 'status', label: 'Status' },
    {
      key: 'detectedAt',
      label: 'Detected',
      render: (row) => formatDateTime(row.detectedAt),
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
          <KpiCard label="Team Size" value={kpis?.teamSize} icon={Users} accent />
          <KpiCard label="Present Today" value={kpis?.presentToday} icon={UserCheck} />
          <KpiCard label="Absent Today" value={kpis?.absentToday} icon={UserX} />
          <KpiCard label="WFO Today" value={kpis?.wfoToday} icon={Building2} />
          <KpiCard label="WFH Today" value={kpis?.wfhToday} icon={Home} />
          <KpiCard label="Late Today" value={kpis?.lateToday} icon={Clock} />
          <KpiCard label="Pending Classification" value={kpis?.pendingClassification} icon={AlertTriangle} />
          <KpiCard label="Open Outliers" value={kpis?.openOutliers} icon={ShieldAlert} />
        </div>

        <div className="grid-charts">
          <PieChartCard title="Today's Mode Distribution" data={dashboard?.modePieChart || []} />
          <BarChartCard title="Monthly Attendance by Employee" data={dashboard?.monthlyBarChart || []} />
        </div>

        <div className="card" style={{ marginBottom: '1.5rem' }}>
          <div className="card-header">
            <h3 className="card-title">Team Attendance Today</h3>
          </div>
          {teamLoading && !teamPageData ? (
            <LoadingSpinner message="Loading team attendance..." />
          ) : (
            <>
              <DataTable
                columns={teamColumns}
                data={teamRows}
                keyField="employeeId"
                emptyMessage="No team attendance data for today"
              />
              <PaginationBar
                page={teamPage}
                size={teamSize}
                totalElements={teamPageData?.totalElements ?? 0}
                totalPages={teamPageData?.totalPages ?? 0}
                loading={teamLoading}
                onPageChange={setTeamPage}
                onSizeChange={(nextSize) => {
                  setTeamSize(nextSize)
                  setTeamPage(0)
                }}
              />
            </>
          )}
        </div>

        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Open Outliers</h3>
          </div>
          {outliersLoading && !outliersPage ? (
            <LoadingSpinner message="Loading outliers..." />
          ) : (
            <>
              <DataTable
                columns={outlierColumns}
                data={outliers}
                keyField="id"
                emptyMessage="No open outliers detected"
              />
              <PaginationBar
                page={outlierPage}
                size={outlierSize}
                totalElements={outliersPage?.totalElements ?? 0}
                totalPages={outliersPage?.totalPages ?? 0}
                loading={outliersLoading}
                onPageChange={setOutlierPage}
                onSizeChange={(nextSize) => {
                  setOutlierSize(nextSize)
                  setOutlierPage(0)
                }}
              />
            </>
          )}
        </div>
      </div>
    </>
  )
}
