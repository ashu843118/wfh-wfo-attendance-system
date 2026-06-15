import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { login as loginApi, getCurrentUser } from '../api/authApi'
import { clearStoredToken, getStoredToken, setStoredToken } from '../api/apiClient'

const USER_KEY = 'attendance_user'

const AuthContext = createContext(null)

const ROLE_DASHBOARD = {
  EMPLOYEE: '/dashboard/employee',
  MANAGER: '/dashboard/manager',
  LEADERSHIP: '/dashboard/leadership',
  ADMIN: '/dashboard/admin',
}

function loadStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

function persistUser(user) {
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  } else {
    localStorage.removeItem(USER_KEY)
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(loadStoredUser)
  const [loading, setLoading] = useState(true)

  const refreshUser = useCallback(async () => {
    const token = getStoredToken()
    if (!token) {
      setUser(null)
      persistUser(null)
      return null
    }
    const profile = await getCurrentUser()
    setUser(profile)
    persistUser(profile)
    return profile
  }, [])

  useEffect(() => {
    const init = async () => {
      try {
        if (getStoredToken()) {
          await refreshUser()
        }
      } catch {
        clearStoredToken()
        setUser(null)
        persistUser(null)
      } finally {
        setLoading(false)
      }
    }
    init()
  }, [refreshUser])

  const login = useCallback(async (email, password) => {
    const response = await loginApi(email, password)
    setStoredToken(response.token)
    const profile = {
      employeeId: response.employeeId,
      email: response.email,
      name: response.name,
      role: response.role,
      teamId: response.teamId,
      managerId: response.managerId,
    }
    setUser(profile)
    persistUser(profile)
    return profile
  }, [])

  const logout = useCallback(() => {
    clearStoredToken()
    setUser(null)
    persistUser(null)
  }, [])

  const getDashboardPath = useCallback((role) => {
    return ROLE_DASHBOARD[role] || '/login'
  }, [])

  const value = useMemo(
    () => ({
      user,
      loading,
      isAuthenticated: !!user,
      login,
      logout,
      refreshUser,
      getDashboardPath,
    }),
    [user, loading, login, logout, refreshUser, getDashboardPath],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return context
}

export { ROLE_DASHBOARD }
