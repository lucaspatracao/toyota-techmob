import { useEffect, useRef, useState } from 'react'

/**
 * Hook genérico de polling: chama `fetchFn` imediatamente e depois a
 * cada `intervalMs` (default 5000ms, conforme "Dados atualizados a cada
 * 5 segundos via MQTT" mencionado nas 3 telas de referência).
 *
 * Uso:
 *   const { data, loading, error } = usePolling(
 *     () => buscarDashboard(maquinaId).then(adaptDashboard),
 *     [maquinaId],
 *     5000
 *   )
 */
export function usePolling(fetchFn, deps = [], intervalMs = 5000) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const fetchFnRef = useRef(fetchFn)
  fetchFnRef.current = fetchFn

  useEffect(() => {
    let cancelled = false
    let intervalId

    async function load(isFirst) {
      try {
        if (isFirst) setLoading(true)
        const result = await fetchFnRef.current()
        if (!cancelled) {
          setData(result)
          setError(null)
        }
      } catch (err) {
        if (!cancelled) setError(err)
      } finally {
        if (!cancelled && isFirst) setLoading(false)
      }
    }

    load(true)
    intervalId = setInterval(() => load(false), intervalMs)

    return () => {
      cancelled = true
      clearInterval(intervalId)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)

  return { data, loading, error }
}
