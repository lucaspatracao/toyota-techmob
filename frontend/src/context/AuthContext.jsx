import { createContext, useState, useCallback, useEffect } from 'react'

export const AuthContext = createContext(null)

/**
 * Tipos de usuários no sistema:
 * - supervisor-a, supervisor-b, supervisor-c: veem dados do seu turno
 * - manager: vê todas as informações de todos os turnos + acesso a metas/recados
 */
export const USER_TYPES = {
  SUPERVISOR_A: 'supervisor-a',
  SUPERVISOR_B: 'supervisor-b',
  SUPERVISOR_C: 'supervisor-c',
  MANAGER: 'manager',
}

/**
 * Mock de usuários cadastrados
 * Em produção, isso vem do backend via autenticação real
 */
const MOCK_USERS = {
  'supervisor-a': {
    id: 1,
    name: 'João Silva',
    email: 'joao.silva@toyota.com',
    type: USER_TYPES.SUPERVISOR_A,
    shift: 'A',
    password: '1234', // Mock apenas — em produção vem token JWT
  },
  'supervisor-b': {
    id: 2,
    name: 'Maria Santos',
    email: 'maria.santos@toyota.com',
    type: USER_TYPES.SUPERVISOR_B,
    shift: 'B',
    password: '1234',
  },
  'supervisor-c': {
    id: 3,
    name: 'Pedro Costa',
    email: 'pedro.costa@toyota.com',
    type: USER_TYPES.SUPERVISOR_C,
    shift: 'C',
    password: '1234',
  },
  manager: {
    id: 99,
    name: 'Carlos Gerente',
    email: 'carlos.gerente@toyota.com',
    type: USER_TYPES.MANAGER,
    shift: null,
    password: '1234',
  },
}

export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // Recupera usuário armazenado ao montar o componente
  useEffect(() => {
    const storedUser = localStorage.getItem('smart40-user')
    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser))
      } catch (e) {
        console.error('Erro ao recuperar usuário:', e)
      }
    }
    setLoading(false)
  }, [])

  const login = useCallback(
    (username, password) => {
      const userData = MOCK_USERS[username]

      if (!userData || userData.password !== password) {
        throw new Error('Credenciais inválidas')
      }

      const userToStore = {
        id: userData.id,
        name: userData.name,
        email: userData.email,
        type: userData.type,
        shift: userData.shift,
      }

      setUser(userToStore)
      localStorage.setItem('smart40-user', JSON.stringify(userToStore))
      return userToStore
    },
    []
  )

  const logout = useCallback(() => {
    setUser(null)
    localStorage.removeItem('smart40-user')
  }, [])

  const isAuthenticated = !!user
  const isManager = user?.type === USER_TYPES.MANAGER
  const isSupervisor = [USER_TYPES.SUPERVISOR_A, USER_TYPES.SUPERVISOR_B, USER_TYPES.SUPERVISOR_C].includes(
    user?.type
  )

  const value = {
    user,
    loading,
    login,
    logout,
    isAuthenticated,
    isManager,
    isSupervisor,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
