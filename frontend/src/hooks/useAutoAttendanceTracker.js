import { useCallback, useEffect, useRef, useState } from 'react'
import { postLocationSignal } from '../api/attendanceApi'
import { isWfhPromptDismissed } from '../utils/autoAttendanceSession'

const SIGNAL_INTERVAL_MS = 5000

export const TRACKING_STATE_LABELS = {
  WAITING_FOR_PERMISSION: 'Requesting location permission',
  LOCATION_PERMISSION_DENIED: 'Location permission denied',
  DETECTING_LOCATION: 'Detecting current location',
  INSIDE_OFFICE: 'Inside office geofence',
  OUTSIDE_OFFICE: 'Outside office geofence',
  AUTO_CHECKIN_PENDING: 'Inside office — auto check-in pending',
  AUTO_CHECKED_IN: 'Auto checked-in',
  CHECKED_IN_WFO: 'Checked in as WFO',
  CHECKED_IN_WFH: 'Checked in as WFH',
  AUTO_CHECKOUT_MONITORING_ACTIVE: 'Auto-checkout monitoring active',
  WFH_CONFIRMATION_REQUIRED: 'Outside office — WFH confirmation required',
  NOT_CHECKED_IN: 'Not checked in',
  AUTO_CHECKOUT_PENDING: 'Outside office — auto checkout pending',
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

/**
 * @param {'idle' | 'once' | 'watch'} mode
 *   idle  — day closed, WFH session, or not an employee; no location activity
 *   once  — eligible for check-in; getCurrentPosition once per status change
 *   watch — active WFO session; watchPosition for auto-checkout only
 */
export default function useAutoAttendanceTracker({
  mode = 'idle',
  userId,
  attendanceDate,
  evaluationKey,
  onSignalProcessed,
  onWfhConfirmationRequired,
}) {
  const watchIdRef = useRef(null)
  const lastSentRef = useRef(0)
  const lastEvaluatedKeyRef = useRef(null)
  const [trackingState, setTrackingState] = useState('NOT_CHECKED_IN')
  const [assignedOfficeName, setAssignedOfficeName] = useState(null)
  const [permissionState, setPermissionState] = useState('prompt')
  const [error, setError] = useState(null)

  const sendSignal = useCallback(
    async (coords, purpose) => {
      const now = Date.now()
      if (now - lastSentRef.current < SIGNAL_INTERVAL_MS) {
        return null
      }
      lastSentRef.current = now

      const isWatchSignal = purpose === 'watch'
      if (!isWatchSignal) {
        setTrackingState('DETECTING_LOCATION')
      }

      try {
        const response = await postLocationSignal(buildLocationPayload(coords))
        if (response?.assignedOfficeName) {
          setAssignedOfficeName(response.assignedOfficeName)
        }

        if (isWatchSignal) {
          if (response?.actionTaken) {
            if (response?.trackingState) {
              setTrackingState(response.trackingState)
            }
          } else if (response?.trackingState === 'AUTO_CHECKOUT_PENDING') {
            setTrackingState('AUTO_CHECKOUT_PENDING')
          }
        } else if (response?.trackingState) {
          setTrackingState(response.trackingState)
        }

        if (
          response?.requiresWfhConfirmation &&
          userId &&
          attendanceDate &&
          !isWfhPromptDismissed(userId, attendanceDate)
        ) {
          onWfhConfirmationRequired?.(response)
        }
        onSignalProcessed?.(response)
        setError(null)
        return response
      } catch (err) {
        setError(err)
        throw err
      }
    },
    [attendanceDate, onSignalProcessed, onWfhConfirmationRequired, userId],
  )

  const stopTracking = useCallback(() => {
    if (watchIdRef.current != null && navigator.geolocation) {
      navigator.geolocation.clearWatch(watchIdRef.current)
      watchIdRef.current = null
    }
  }, [])

  const evaluateOnce = useCallback(
    (evalKey) => {
      if (!navigator.geolocation) {
        setError(new Error('Geolocation is not supported by this browser'))
        setTrackingState('LOCATION_PERMISSION_DENIED')
        return
      }

      if (lastEvaluatedKeyRef.current === evalKey) {
        return
      }
      lastEvaluatedKeyRef.current = evalKey
      setTrackingState('WAITING_FOR_PERMISSION')

      navigator.geolocation.getCurrentPosition(
        (position) => {
          setPermissionState('granted')
          sendSignal(
            {
              latitude: position.coords.latitude,
              longitude: position.coords.longitude,
              accuracy: position.coords.accuracy,
            },
            'check-in',
          ).catch(() => {})
        },
        () => {
          setPermissionState('denied')
          setTrackingState('LOCATION_PERMISSION_DENIED')
          setError(new Error(PERMISSION_DENIED_MESSAGE))
        },
        { enableHighAccuracy: true, maximumAge: SIGNAL_INTERVAL_MS, timeout: 20000 },
      )
    },
    [sendSignal],
  )

  const startWatcher = useCallback(() => {
    if (!navigator.geolocation) {
      setError(new Error('Geolocation is not supported by this browser'))
      setTrackingState('LOCATION_PERMISSION_DENIED')
      return
    }

    if (watchIdRef.current != null) {
      return
    }

    watchIdRef.current = navigator.geolocation.watchPosition(
      (position) => {
        setPermissionState('granted')
        sendSignal(
          {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
          },
          'watch',
        ).catch(() => {})
      },
      () => {
        setPermissionState('denied')
        setTrackingState('LOCATION_PERMISSION_DENIED')
        setError(new Error(PERMISSION_DENIED_MESSAGE))
      },
      { enableHighAccuracy: true, maximumAge: SIGNAL_INTERVAL_MS, timeout: 20000 },
    )
  }, [sendSignal])

  useEffect(() => {
    if (mode === 'idle') {
      stopTracking()
      lastEvaluatedKeyRef.current = null
      return undefined
    }

    if (mode === 'once') {
      stopTracking()
      if (evaluationKey) {
        evaluateOnce(evaluationKey)
      }
      return undefined
    }

    if (mode === 'watch') {
      startWatcher()
      return () => stopTracking()
    }

    return undefined
  }, [mode, evaluationKey, evaluateOnce, startWatcher, stopTracking])

  return {
    trackingState,
    trackingStateLabel: TRACKING_STATE_LABELS[trackingState] || trackingState,
    assignedOfficeName,
    permissionState,
    error,
    stopTracking,
  }
}
