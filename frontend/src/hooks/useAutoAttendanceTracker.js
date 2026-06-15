import { useCallback, useEffect, useRef, useState } from 'react'
import { postLocationSignal } from '../api/attendanceApi'

const SIGNAL_INTERVAL_MS = 5000

export const TRACKING_STATE_LABELS = {
  WAITING_FOR_PERMISSION: 'Requesting location permission',
  LOCATION_PERMISSION_DENIED: 'Location permission denied',
  DETECTING_LOCATION: 'Detecting location',
  INSIDE_OFFICE: 'Inside office geofence',
  OUTSIDE_OFFICE: 'Outside office geofence',
  AUTO_CHECKIN_PENDING: 'Auto check-in pending',
  AUTO_CHECKED_IN: 'Auto checked-in',
  CHECKED_IN_WFO: 'Checked in as WFO',
  CHECKED_IN_WFH: 'Checked in as WFH',
  WFH_CONFIRMATION_REQUIRED: 'Outside office — WFH confirmation required',
  NOT_CHECKED_IN: 'Not checked in',
  AUTO_CHECKOUT_PENDING: 'Auto checkout pending',
  AUTO_CHECKED_OUT: 'Auto checked-out',
  CHECKED_OUT: 'Checked out',
  SYSTEM_CLOSED: 'System closed',
  MISSING_CHECKOUT: 'Missing checkout',
}

export const PERMISSION_DENIED_MESSAGE =
  'Location permission is required for automatic attendance. You can enable it in browser settings or use manual check-in/check-out.'

function buildLocationPayload(coords) {
  return {
    latitude: coords.latitude,
    longitude: coords.longitude,
    accuracy: coords.accuracy ?? 10,
    timestamp: new Date().toISOString().slice(0, 19),
  }
}

export default function useAutoAttendanceTracker({ enabled, onSignalProcessed, onWfhConfirmationRequired }) {
  const watchIdRef = useRef(null)
  const intervalRef = useRef(null)
  const lastSentRef = useRef(0)
  const wfhPromptShownRef = useRef(false)
  const [trackingState, setTrackingState] = useState(
    enabled ? 'WAITING_FOR_PERMISSION' : 'NOT_CHECKED_IN',
  )
  const [assignedOfficeName, setAssignedOfficeName] = useState(null)
  const [permissionState, setPermissionState] = useState('prompt')
  const [error, setError] = useState(null)

  const sendSignal = useCallback(
    async (coords) => {
      const now = Date.now()
      if (now - lastSentRef.current < SIGNAL_INTERVAL_MS) {
        return null
      }
      lastSentRef.current = now

      try {
        setTrackingState('DETECTING_LOCATION')
        const response = await postLocationSignal(buildLocationPayload(coords))
        if (response?.trackingState) {
          setTrackingState(response.trackingState)
        }
        if (response?.assignedOfficeName) {
          setAssignedOfficeName(response.assignedOfficeName)
        }
        if (response?.requiresWfhConfirmation && !wfhPromptShownRef.current) {
          wfhPromptShownRef.current = true
          onWfhConfirmationRequired?.(response)
        }
        if (response?.actionTaken) {
          wfhPromptShownRef.current = false
        }
        onSignalProcessed?.(response)
        setError(null)
        return response
      } catch (err) {
        setError(err)
        throw err
      }
    },
    [onSignalProcessed, onWfhConfirmationRequired],
  )

  const stopTracking = useCallback(() => {
    if (watchIdRef.current != null && navigator.geolocation) {
      navigator.geolocation.clearWatch(watchIdRef.current)
      watchIdRef.current = null
    }
    if (intervalRef.current) {
      clearInterval(intervalRef.current)
      intervalRef.current = null
    }
  }, [])

  useEffect(() => {
    if (!enabled) {
      stopTracking()
      setTrackingState('NOT_CHECKED_IN')
      return undefined
    }

    if (!navigator.geolocation) {
      setError(new Error('Geolocation is not supported by this browser'))
      setTrackingState('LOCATION_PERMISSION_DENIED')
      return undefined
    }

    setTrackingState('WAITING_FOR_PERMISSION')

    watchIdRef.current = navigator.geolocation.watchPosition(
      (position) => {
        setPermissionState('granted')
        sendSignal({
          latitude: position.coords.latitude,
          longitude: position.coords.longitude,
          accuracy: position.coords.accuracy,
        }).catch(() => {})
      },
      () => {
        setPermissionState('denied')
        setTrackingState('LOCATION_PERMISSION_DENIED')
        setError(new Error(PERMISSION_DENIED_MESSAGE))
      },
      { enableHighAccuracy: true, maximumAge: SIGNAL_INTERVAL_MS, timeout: 20000 },
    )

    return () => stopTracking()
  }, [enabled, sendSignal, stopTracking])

  return {
    trackingState,
    trackingStateLabel: TRACKING_STATE_LABELS[trackingState] || trackingState,
    assignedOfficeName,
    permissionState,
    error,
    resetWfhPrompt: () => {
      wfhPromptShownRef.current = false
    },
  }
}
