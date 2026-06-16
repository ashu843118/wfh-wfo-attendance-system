export {
  formatDate,
  formatTime,
  formatDateTime,
  formatDuration,
  parseApiDateTime,
} from './dateTimeUtils'

export function formatLastUpdated(date) {
  if (!date) return 'Never'
  return date.toLocaleTimeString(undefined, { timeStyle: 'medium' })
}

export function getApiErrorMessage(error) {
  return error?.response?.data?.message || error?.message || 'An unexpected error occurred'
}

export function getLoginErrorMessage(error) {
  const status = error?.response?.status
  const errorCode = error?.response?.data?.errorCode

  if (status === 401 || errorCode === 'INVALID_CREDENTIALS' || errorCode === 'AUTH_INVALID_CREDENTIALS') {
    return 'Invalid email or password.'
  }

  if (!error?.response || status >= 500) {
    return 'Login service is currently unavailable. Please try again.'
  }

  return error?.response?.data?.message || 'Login service is currently unavailable. Please try again.'
}
