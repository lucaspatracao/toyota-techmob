import { api } from './api.js'

/**
 * Camada de serviço que espelha, 1 para 1, os 4 endpoints confirmados no
 * back-end (com.toyota.techmob.backend.controller):
 *
 *   MaquinaController          → GET /api/maquinas
 *   DashboardController        → GET /api/dashboard/{maquinaId}
 *   HistoricoProducaoController→ GET /api/historico-producao/{maquinaId}
 *   IndicadorOEEController     → GET /api/indicadores/historico/{maquinaId}
 *
 * As funções retornam o JSON exatamente como a API envia. O "de-para"
 * para o formato das telas fica em `src/services/adapters.js` (lá está
 * documentado o JSON de cada endpoint).
 */

/** GET /api/maquinas — lista de máquinas cadastradas. */
export async function listarMaquinas() {
  const { data } = await api.get('/api/maquinas')
  return data
}

/** GET /api/dashboard/{maquinaId} — indicadores da tela Dashboard. */
export async function buscarDashboard(maquinaId) {
  const { data } = await api.get(`/api/dashboard/${maquinaId}`)
  return data
}

/** GET /api/historico-producao/{maquinaId} — linhas da tela Histórico. */
export async function buscarHistoricoProducao(maquinaId) {
  const { data } = await api.get(`/api/historico-producao/${maquinaId}`)
  return data
}

/** GET /api/indicadores/historico/{maquinaId} — série do gráfico "OEE ao longo do tempo". */
export async function buscarHistoricoIndicadoresOEE(maquinaId) {
  const { data } = await api.get(`/api/indicadores/historico/${maquinaId}`)
  return data
}
