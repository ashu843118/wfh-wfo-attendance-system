import { Navigate, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'
import LoadingSpinner from '../components/common/LoadingSpinner'

export default function ProtectedRoute({ children }) {
  const { isAuthenticated, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return <LoadingSpinner fullPage message="Loading session..." />
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />
  }

  return children
}
