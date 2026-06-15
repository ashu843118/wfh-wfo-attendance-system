import { Navigate } from 'react-router-dom'
import { useAuth } from './AuthContext'

export default function RoleBasedRoute({ allowedRoles, children }) {
  const { user, getDashboardPath } = useAuth()

  if (!user) {
    return <Navigate to="/login" replace />
  }

  if (!allowedRoles.includes(user.role)) {
    return <Navigate to={getDashboardPath(user.role)} replace />
  }

  return children
}
