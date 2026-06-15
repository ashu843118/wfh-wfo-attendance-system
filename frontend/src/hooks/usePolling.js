import { useCallback, useEffect, useRef, useState } from 'react'

export default function usePolling(fetchFn, intervalMs, { enabled = true, immediate = true } = {}) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(immediate && enabled)
  const [error, setError] = useState(null)
  const [lastUpdated, setLastUpdated] = useState(null)
  const fetchRef = useRef(fetchFn)

  fetchRef.current = fetchFn

  const refresh = useCallback(async () => {
    if (!enabled) {
      return null
    }
    try {
      setError(null)
      const result = await fetchRef.current()
      setData(result)
      setLastUpdated(new Date())
      return result
    } catch (err) {
      setError(err)
      throw err
    } finally {
      setLoading(false)
    }
  }, [enabled])

  useEffect(() => {
    if (!enabled) {
      setLoading(false)
      return undefined
    }

    let active = true

    const run = async () => {
      if (!active) return
      try {
        await refresh()
      } catch {
        /* error stored in state */
      }
    }

    if (immediate) {
      run()
    }

    const id = setInterval(() => {
      if (active && document.visibilityState === 'visible') {
        run()
      }
    }, intervalMs)

    return () => {
      active = false
      clearInterval(id)
    }
  }, [enabled, intervalMs, immediate, refresh])

  return { data, loading, error, lastUpdated, refresh }
}
