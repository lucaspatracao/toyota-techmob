import axios from 'axios'

// Base URL configurável via variável de ambiente (Vite expõe apenas
// variáveis prefixadas com VITE_). Em dev, aponta para o Spring Boot local
// (porta 8080, confirmada no README do backend); em produção, para a URL
// do serviço no Render.
export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  timeout: 10000,
})

// Interceptor simples de log de erro — útil durante a integração inicial
// com o back-end. Pode ser removido/expandido conforme a necessidade
// (ex.: toast de erro, retry, etc.).
api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('[API] Erro na requisição:', error?.config?.url, error?.message)
    return Promise.reject(error)
  }
)
