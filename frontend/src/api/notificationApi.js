import apiClient, { unwrapResponse } from './apiClient'

export async function getNotifications(page = 0, size = 20) {
  const response = await apiClient.get('/api/notifications', {
    params: { page, size },
  })
  return unwrapResponse(response)
}

export async function getUnreadCount() {
  const response = await apiClient.get('/api/notifications/unread-count')
  return unwrapResponse(response)
}

export async function markAsRead(id) {
  const response = await apiClient.post(`/api/notifications/${id}/read`)
  return unwrapResponse(response)
}
