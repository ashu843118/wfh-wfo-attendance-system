import apiClient, { unwrapResponse } from './apiClient'

export async function postLocationSignal(location) {
  const response = await apiClient.post('/api/attendance/location-signal', location)
  return unwrapResponse(response)
}

export async function checkIn(location, source = 'web-app') {
  const response = await apiClient.post('/api/attendance/check-in', {
    location,
    source,
  })
  return unwrapResponse(response)
}

export async function confirmWfhCheckIn(location, source = 'web-app') {
  const response = await apiClient.post('/api/attendance/wfh-check-in', {
    location,
    source,
  })
  return unwrapResponse(response)
}

export async function dismissWfhPrompt() {
  const response = await apiClient.post('/api/attendance/dismiss-wfh-prompt')
  return unwrapResponse(response)
}

export async function checkOut(location, source = 'web-app') {
  const response = await apiClient.post('/api/attendance/check-out', {
    location,
    source,
  })
  return unwrapResponse(response)
}

export async function getTodayAttendance() {
  const response = await apiClient.get('/api/attendance/me/today')
  return unwrapResponse(response)
}

export async function getAttendanceHistory(params = {}) {
  const response = await apiClient.get('/api/attendance/history', { params })
  return unwrapResponse(response)
}

export async function getAttendanceEvents(params = {}) {
  const response = await apiClient.get('/api/attendance/events', { params })
  return unwrapResponse(response)
}

export async function getAttendanceEventsForDate(date, params = {}) {
  const response = await apiClient.get('/api/attendance/events', {
    params: { date, ...params },
  })
  return unwrapResponse(response)
}

export async function getAttendanceSessionsForDate(date, params = {}) {
  const response = await apiClient.get('/api/attendance/sessions', {
    params: { date, ...params },
  })
  return unwrapResponse(response)
}

export async function recordAutoGeofenceEvent(eventType, location, source = 'AUTO_PWA') {
  const response = await apiClient.post('/api/attendance/events/auto', {
    eventType,
    location,
    source,
  })
  return unwrapResponse(response)
}
