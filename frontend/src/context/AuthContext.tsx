import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import { api } from '../lib/api'
import type { AuthResponse, User } from '../types'

type AuthValue = {
  user: User | null
  ready: boolean
  login: (response: AuthResponse) => void
  refresh: () => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [ready, setReady] = useState(false)

  async function refresh() {
    const token = localStorage.getItem('shofe_token')
    if (!token) {
      setUser(null)
      setReady(true)
      return
    }
    try {
      setUser(await api<User>('/api/auth/me'))
    } catch {
      localStorage.removeItem('shofe_token')
      setUser(null)
    } finally {
      setReady(true)
    }
  }

  useEffect(() => {
    void refresh()
  }, [])

  const value = useMemo<AuthValue>(() => ({
    user,
    ready,
    login: (response) => {
      localStorage.setItem('shofe_token', response.token)
      setUser(response.user)
      setReady(true)
    },
    refresh,
    logout: () => {
      localStorage.removeItem('shofe_token')
      setUser(null)
    },
  }), [user, ready])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('AuthProvider missing')
  return ctx
}
