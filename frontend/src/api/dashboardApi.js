import apiClient, { unwrapResponse } from './apiClient'

export async function getEmployeeDashboard() {
  const response = await apiClient.get('/api/employee/dashboard-summary')
  return unwrapResponse(response)
}

export async function getManagerDashboard(date) {
  const params = date ? { date } : {}
  const response = await apiClient.get('/api/manager/dashboard-summary', { params })
  return unwrapResponse(response)
}

export async function getManagerTeamAttendance({ page = 0, size = 20, date } = {}) {
  const params = { page, size }
  if (date) params.date = date
  const response = await apiClient.get('/api/manager/team-attendance', { params })
  return unwrapResponse(response)
}

export async function getManagerDashboardDrilldown({ type, page = 0, size = 10, date } = {}) {
  const params = { type, page, size }
  if (date) params.date = date
  const response = await apiClient.get('/api/manager/dashboard/drilldown', { params })
  return unwrapResponse(response)
}

export async function getManagerOutliers(page = 0, size = 20) {
  const response = await apiClient.get('/api/manager/outliers', {
    params: { page, size },
  })
  return unwrapResponse(response)
}

export async function getLeadershipTeamSummary({ page = 0, size = 20, date } = {}) {
  const params = { page, size }
  if (date) params.date = date
  const response = await apiClient.get('/api/leadership/team-summary', { params })
  return unwrapResponse(response)
}

export async function getLeadershipDashboard(date) {
  const params = date ? { date } : {}
  const response = await apiClient.get('/api/leadership/dashboard', { params })
  return unwrapResponse(response)
}
