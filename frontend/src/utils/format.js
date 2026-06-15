export function formatDateTime(date) {
  if (!date) return '—'
  return new Date(date).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

export function formatDate(date) {
  if (!date) return '—'
  return new Date(date).toLocaleDateString(undefined, { dateStyle: 'medium' })
}

export function formatTime(date) {
  if (!date) return '—'
  return new Date(date).toLocaleTimeString(undefined, { timeStyle: 'short' })
}

export function formatLastUpdated(date) {
  if (!date) return 'Never'
  return date.toLocaleTimeString(undefined, { timeStyle: 'medium' })
}

export function getApiErrorMessage(error) {
  return error?.response?.data?.message || error?.message || 'An unexpected error occurred'
}
