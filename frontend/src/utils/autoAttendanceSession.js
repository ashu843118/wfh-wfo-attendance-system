const WFH_PROMPT_KEY_PREFIX = 'wfhPromptDismissed:'

export function wfhPromptDismissedKey(userId, attendanceDate) {
  return `${WFH_PROMPT_KEY_PREFIX}${userId}:${attendanceDate}`
}

export function isWfhPromptDismissed(userId, attendanceDate) {
  if (!userId || !attendanceDate) return false
  return sessionStorage.getItem(wfhPromptDismissedKey(userId, attendanceDate)) === 'true'
}

export function markWfhPromptDismissed(userId, attendanceDate) {
  if (!userId || !attendanceDate) return
  sessionStorage.setItem(wfhPromptDismissedKey(userId, attendanceDate), 'true')
}

export function clearWfhPromptDismissed(userId, attendanceDate) {
  if (!userId || !attendanceDate) return
  sessionStorage.removeItem(wfhPromptDismissedKey(userId, attendanceDate))
}

export function clearAllAutoAttendanceSessionKeys() {
  Object.keys(sessionStorage).forEach((key) => {
    if (key.startsWith(WFH_PROMPT_KEY_PREFIX)) {
      sessionStorage.removeItem(key)
    }
  })
}
