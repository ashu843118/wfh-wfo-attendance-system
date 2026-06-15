import apiClient, { unwrapResponse } from './apiClient'

export async function getOfficeLocations() {
  const response = await apiClient.get('/api/admin/office-locations')
  return unwrapResponse(response)
}

export async function getOfficeLocation(id) {
  const response = await apiClient.get(`/api/admin/office-locations/${id}`)
  return unwrapResponse(response)
}

export async function createOfficeLocation(data) {
  const response = await apiClient.post('/api/admin/office-locations', data)
  return unwrapResponse(response)
}

export async function updateOfficeLocation(id, data) {
  const response = await apiClient.put(`/api/admin/office-locations/${id}`, data)
  return unwrapResponse(response)
}

export async function deleteOfficeLocation(id) {
  const response = await apiClient.delete(`/api/admin/office-locations/${id}`)
  return unwrapResponse(response)
}

export async function getPolicies() {
  const response = await apiClient.get('/api/admin/policies')
  return unwrapResponse(response)
}

export async function getPolicy(id) {
  const response = await apiClient.get(`/api/admin/policies/${id}`)
  return unwrapResponse(response)
}

export async function createPolicy(data) {
  const response = await apiClient.post('/api/admin/policies', data)
  return unwrapResponse(response)
}

export async function updatePolicy(id, data) {
  const response = await apiClient.put(`/api/admin/policies/${id}`, data)
  return unwrapResponse(response)
}

export async function deletePolicy(id) {
  const response = await apiClient.delete(`/api/admin/policies/${id}`)
  return unwrapResponse(response)
}

export async function getEmployees({ page = 0, size = 20, search, role, teamId, active } = {}) {
  const params = { page, size }
  if (search) params.search = search
  if (role) params.role = role
  if (teamId) params.teamId = teamId
  if (active !== undefined && active !== null && active !== '') params.active = active
  const response = await apiClient.get('/api/admin/employees', { params })
  return unwrapResponse(response)
}

export async function createEmployee(data) {
  const response = await apiClient.post('/api/admin/employees', data)
  return unwrapResponse(response)
}

export async function updateEmployee(id, data) {
  const response = await apiClient.put(`/api/admin/employees/${id}`, data)
  return unwrapResponse(response)
}

export async function updateEmployeeStatus(id, active) {
  const response = await apiClient.patch(`/api/admin/employees/${id}/status`, { active })
  return unwrapResponse(response)
}

export async function getManagers() {
  const response = await apiClient.get('/api/admin/managers')
  return unwrapResponse(response)
}

export async function getTeams() {
  const response = await apiClient.get('/api/admin/teams')
  return unwrapResponse(response)
}
