import { useCallback, useEffect, useRef, useState } from 'react'

/** Intervalo padrão entre consultas, em ms (VITE_POLLING_MS; a bancada é lida a cada 1 s). */
export const POLLING_MS = Number(import.meta.env.VITE_POLLING_MS) || 2000

/**
 * Chama `fetchFn` agora e de novo `intervalMs` depois que cada resposta
 * chega (nunca duas consultas ao mesmo tempo, mesmo com a API lenta).
 *
 * Mantém o último dado enquanto recarrega: quando os `deps` mudam (ex.: um
 * filtro) ou uma consulta falha, a tela continua mostrando o que tinha.
 *   - `loading`: true até a primeira resposta dos `deps` atuais;
 *   - `error`: erro da última consulta (null quando ela deu certo);
 *   - `atualizadoEm`: Date da última resposta boa;
 *   - `recarregar()`: consulta de novo na hora (botão "Tentar novamente").
 *
 * Uso:
 *   const { data, loading, error } = usePolling(
 *     () => buscarDashboard(MAQUINA_ID).then(adaptDashboard),
 *     [MAQUINA_ID]
 *   )
 */
export function usePolling(fetchFn, deps = [], intervalMs = POLLING_MS) {
  const [estado, setEstado] = useState({ data: null, error: null, loading: true, atualizadoEm: null })
  const fetchFnRef = useRef(fetchFn)
  fetchFnRef.current = fetchFn
  const recarregarRef = useRef(() => {})

  useEffect(() => {
    let ativo = true
    let emAndamento = false
    let timer

    async function carregar() {
      if (emAndamento) return
      emAndamento = true
      clearTimeout(timer)
      try {
        const data = await fetchFnRef.current()
        if (ativo) setEstado({ data, error: null, loading: false, atualizadoEm: new Date() })
      } catch (error) {
        if (ativo) setEstado((atual) => ({ ...atual, error, loading: false }))
      } finally {
        emAndamento = false
        if (ativo) timer = setTimeout(carregar, intervalMs)
      }
    }

    setEstado((atual) => ({ ...atual, loading: true }))
    recarregarRef.current = carregar
    carregar()

    return () => {
      ativo = false
      clearTimeout(timer)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [...deps, intervalMs])

  const recarregar = useCallback(() => recarregarRef.current(), [])

  return { ...estado, recarregar }
}
