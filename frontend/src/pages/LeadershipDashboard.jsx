import { useCallback, useState } from 'react'
import { RefreshCw, Users, UserCheck, Building2, Home, TrendingUp } from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import KpiCard from '../components/cards/KpiCard'
import LineChartCard from '../components/charts/LineChartCard'
import DataTable from '../components/tables/DataTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import PaginationBar from '../components/common/PaginationBar'
import { useToast } from '../components/common/Toast'
import { useAuth } from '../auth/AuthContext'
import usePolling from '../hooks/usePolling'
import { getLeadershipDashboard, getLeadershipTeamSummary } from '../api/dashboardApi'
import { formatLastUpdated } from '../utils/format'
import './DashboardPages.css'

export default function LeadershipDashboard() {
  const toast = useToast()
  const { isAuthenticated } = useAuth()
  const [teamPage, setTeamPage] = useState(0)
  const [teamSize, setTeamSize] = useState(20)

  const fetchDashboard = useCallback(() => getLeadershipDashboard(), [])

  const fetchTeamSummary = useCallback(
    () => getLeadershipTeamSummary({ page: teamPage, size: teamSize }),
    [teamPage, teamSize]
  )

  const { data: dashboard, loading, lastUpdated, refresh } = usePolling(fetchDashboard, 60000, {
    enabled: isAuthenticated,
  })

  const {
    data: teamSummary,
    loading: teamLoading,
    refresh: refreshTeamSummary,
  } = usePolling(fetchTeamSummary, 60000, { enabled: isAuthenticated })

  const handleRefresh = async () => {
    await Promise.all([refresh(), refreshTeamSummary()])
    toast.info('Dashboard refreshed')
  }

  const kpis = dashboard?.kpis
  const teamRows = teamSummary?.content || []

  const teamColumns = [
    { key: 'teamName', label: 'Team' },
    {
      key: 'attendancePercentage',
      label: 'Attendance %',
      render: (row) => `${(row.attendancePercentage ?? 0).toFixed(1)}%`,
    },
  ]

  if (loading && !dashboard) {
    return <LoadingSpinner fullPage message="Loading leadership dashboard..." />
  }

  return (
    <>
      <Topbar
        title="Leadership Dashboard"
        actions={
          <button type="button" className="btn btn-secondary btn-sm" onClick={handleRefresh}>
            <RefreshCw size={16} />
            Refresh
          </button>
        }
      />
      <div className="app-layout__content dashboard-page">
        <div className="page-header">
          <h1>Organization Overview</h1>
          <p className="last-updated">Last updated: {formatLastUpdated(lastUpdated)}</p>
        </div>

        <div className="grid-kpi">
          <KpiCard label="Total Employees" value={kpis?.totalEmployees} icon={Users} accent />
          <KpiCard label="Present Today" value={kpis?.presentToday} icon={UserCheck} />
          <KpiCard label="WFO Today" value={kpis?.wfoToday} icon={Building2} />
          <KpiCard label="WFH Today" value={kpis?.wfhToday} icon={Home} />
          <KpiCard
            label="Avg WFO %"
            value={kpis?.averageWfoPercentage != null ? `${kpis.averageWfoPercentage.toFixed(1)}%` : '—'}
            icon={TrendingUp}
          />
        </div>

        <div className="grid-charts">
          <LineChartCard title="WFO / WFH Trend" data={dashboard?.wfoWfhTrend || []} />
        </div>

        <div className="card">
          <div className="card-header">
            <h3 className="card-title">Team Performance</h3>
          </div>
          {teamLoading && !teamSummary ? (
            <LoadingSpinner message="Loading team performance..." />
          ) : (
            <>
              <DataTable
                columns={teamColumns}
                data={teamRows}
                keyField="teamId"
                emptyMessage="No team performance data available"
              />
              <PaginationBar
                page={teamPage}
                size={teamSize}
                totalElements={teamSummary?.totalElements ?? 0}
                totalPages={teamSummary?.totalPages ?? 0}
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
      </div>
    </>
  )
}
