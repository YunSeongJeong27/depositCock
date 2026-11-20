import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react'
import { api } from './api'
import type { Me } from './types'

interface AuthState {
  me: Me | null
  loading: boolean
  refresh: () => Promise<void>
  setMe: (me: Me | null) => void
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState>(null as unknown as AuthState)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null>(null)
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    try {
      setMe(await api.get<Me>('/api/me'))
    } catch {
      setMe(null)
    } finally {
      setLoading(false)
    }
  }, [])

  const logout = useCallback(async () => {
    await api.post('/api/auth/logout')
    setMe(null)
  }, [])

  useEffect(() => {
    refresh()
  }, [refresh])

  return <AuthContext.Provider value={{ me, loading, refresh, setMe, logout }}>{children}</AuthContext.Provider>
}

export function useAuth() {
  return useContext(AuthContext)
}

/** 새 회원가입을 받는지. 관리자가 끄면 false, 불러오는 동안은 null (2026-11-09). 막는 것은 서버가 한다 */
export function useSignupOpen(): boolean | null {
  const [open, setOpen] = useState<boolean | null>(null)
  useEffect(() => {
    api.get<{ open: boolean }>('/api/auth/signup-open').then((s) => setOpen(s.open)).catch(() => setOpen(true))
  }, [])
  return open
}
