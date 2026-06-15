import { useCallback, useState } from 'react'

export default function useGeolocation() {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const getLocation = useCallback(() => {
    return new Promise((resolve, reject) => {
      if (!navigator.geolocation) {
        const err = new Error('Geolocation is not supported by this browser')
        setError(err)
        reject(err)
        return
      }

      setLoading(true)
      setError(null)

      navigator.geolocation.getCurrentPosition(
        (position) => {
          setLoading(false)
          const payload = {
            latitude: position.coords.latitude,
            longitude: position.coords.longitude,
            accuracy: position.coords.accuracy,
            timestamp: new Date().toISOString().slice(0, 19),
          }
          resolve(payload)
        },
        (geoError) => {
          setLoading(false)
          const err = new Error(geoError.message || 'Unable to retrieve location')
          setError(err)
          reject(err)
        },
        { enableHighAccuracy: true, timeout: 15000, maximumAge: 0 },
      )
    })
  }, [])

  return { getLocation, loading, error }
}
