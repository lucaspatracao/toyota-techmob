import { api } from './api.js'

/**
 * Uma função por endpoint da API (contrato completo, com exemplos, em
 * backend/README.md). Todas são GET e devolvem o JSON exatamente como a
 * API envia; a conversão para as telas fica em `adapters.js`.
 *
 *   GET /api/maquinas
 *   GET /api/dashboard/{maquinaId}
 *   GET /api/planta/{maquinaId}/status
 *   GET /api/pedidos/{maquinaId}?de=&ate=&origem=
 *   GET /api/pedidos/detalhe/{pedidoId}
 *   GET /api/indicadores/historico/{maquinaId}
 */

/** Máquina consultada pelas telas (seed do schema.sql: 1, a Bancada Smart 4.0). */
export const MAQUINA_ID = Number(import.meta.env.VITE_MAQUINA_ID) || 1

/** Lista de máquinas cadastradas. */
export async function listarMaquinas() {
  const { data } = await api.get('/api/maquinas')
  return data
}

/** Máquina, OEE do último pedido, estado ao vivo, resumo de hoje e meta. */
export async function buscarDashboard(maquinaId) {
  const { data } = await api.get(`/api/dashboard/${maquinaId}`)
  return data
}

/** Estado ao vivo da bancada: estações, pedido em andamento, fonte e conexão. */
export async function buscarStatusPlanta(maquinaId) {
  const { data } = await api.get(`/api/planta/${maquinaId}/status`)
  return data
}

/**
 * Pedidos do período, do mais recente ao mais antigo, com o OEE de cada um.
 * Filtros opcionais: `de` e `ate` (Date; filtro pelo início do pedido, em
 * [de, ate)) e `origem` (BANCADA, REPLAY ou SINTETICO).
 */
export async function listarPedidos(maquinaId, { de, ate, origem } = {}) {
  const params = {}
  if (de) params.de = de.toISOString()
  if (ate) params.ate = ate.toISOString()
  if (origem) params.origem = origem
  const { data } = await api.get(`/api/pedidos/${maquinaId}`, { params })
  return data
}

/** Um pedido com as etapas (estoque, manipulação, separação) e o tempo padrão de cada uma. */
export async function buscarPedido(pedidoId) {
  const { data } = await api.get(`/api/pedidos/detalhe/${pedidoId}`)
  return data
}

/** Série de OEE pedido a pedido, do mais recente ao mais antigo. */
export async function buscarHistoricoIndicadoresOEE(maquinaId) {
  const { data } = await api.get(`/api/indicadores/historico/${maquinaId}`)
  return data
}
