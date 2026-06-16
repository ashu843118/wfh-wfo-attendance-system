/**
 * Parses API date/time values. Backend stores instants in UTC; LocalDateTime fields
 * are serialized without a zone suffix and should be interpreted as UTC.
 */
export function parseApiDateTime(value) {
  if (value == null || value === '') return null
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : value
  }

  const str = String(value).trim()
  if (!str) return null

  // Calendar date only — display as local calendar day
  if (/^\d{4}-\d{2}-\d{2}$/.test(str)) {
    const [year, month, day] = str.split('-').map(Number)
    return new Date(year, month - 1, day)
  }

  // Already includes timezone offset or Z
  if (/[zZ]$/.test(str) || /[+-]\d{2}:\d{2}$/.test(str)) {
    const parsed = new Date(str)
    return Number.isNaN(parsed.getTime()) ? null : parsed
  }

  // ISO local datetime from backend (UTC stored) — append Z
  const normalized = str.includes('T') ? `${str}Z` : `${str}T00:00:00Z`
  const parsed = new Date(normalized)
  return Number.isNaN(parsed.getTime()) ? null : parsed
}

export function formatDate(dateTime) {
  const date = parseApiDateTime(dateTime)
  if (!date) return '—'
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(date)
}

export function formatTime(dateTime) {
  const date = parseApiDateTime(dateTime)
  if (!date) return '—'
  return new Intl.DateTimeFormat(undefined, { timeStyle: 'short' }).format(date)
}

export function formatDateTime(dateTime) {
  const date = parseApiDateTime(dateTime)
  if (!date) return '—'
  return new Intl.DateTimeFormat(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export function formatDuration(minutes) {
  if (minutes == null || minutes < 0) return '—'
  if (minutes < 60) return `${minutes} min`
  const hours = Math.floor(minutes / 60)
  const remainder = minutes % 60
  return remainder > 0 ? `${hours}h ${remainder}m` : `${hours}h`
}
