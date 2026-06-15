import apiClient, { unwrapResponse } from './apiClient'

export async function login(email, password) {
  const response = await apiClient.post('/api/auth/login', { email, password })
  return unwrapResponse(response)
}

export async function getCurrentUser() {
  const response = await apiClient.get('/api/auth/me')
  return unwrapResponse(response)
}
