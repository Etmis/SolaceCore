import { createContext, useContext, useEffect, useState } from 'react'
import type { ReactNode } from 'react'
import { getCurrentModerator, isAuthenticated, logout as apiLogout } from '../api'
import type { Moderator } from '../types'

interface AuthContextType {
  moderator: Moderator | null
  loading: boolean
  login: () => Promise<void>
  logout: () => void
  hasPermission: (permission: string) => boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [moderator, setModerator] = useState<Moderator | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const initAuth = async () => {
      if (isAuthenticated()) {
        try {
          const mod = await getCurrentModerator()
          setModerator(mod)
        } catch (error) {
          console.error('Failed to load moderator:', error)
          apiLogout()
        }
      }
      setLoading(false)
    }

    initAuth()
  }, [])

  const login = async () => {
    try {
      const mod = await getCurrentModerator()
      setModerator(mod)
    } catch (error) {
      throw error
    }
  }

  const logout = () => {
    apiLogout()
    setModerator(null)
  }

  const hasPermission = (permission: string): boolean => {
    return moderator?.permissions?.[permission] === true
  }

  return (
    <AuthContext.Provider value={{ moderator, loading, login, logout, hasPermission }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
