import { Navigate, Route, Routes } from 'react-router-dom'
import { useAuth } from './auth/AuthContext'
import ProtectedRoute from './auth/ProtectedRoute'
import RoleBasedRoute from './auth/RoleBasedRoute'
import AppLayout from './components/layout/AppLayout'
import LoadingSpinner from './components/common/LoadingSpinner'
import LoginPage from './pages/LoginPage'
import EmployeeDashboard from './pages/EmployeeDashboard'
import ManagerDashboard from './pages/ManagerDashboard'
import LeadershipDashboard from './pages/LeadershipDashboard'
import AdminDashboard from './pages/AdminDashboard'
import OfficeLocationsPage from './pages/OfficeLocationsPage'
import PoliciesPage from './pages/PoliciesPage'
import EmployeesPage from './pages/EmployeesPage'
import NotificationsPage from './pages/NotificationsPage'
import AttendanceHistoryPage from './pages/AttendanceHistoryPage'

function RootRedirect() {
  const { user, loading, getDashboardPath } = useAuth()

  if (loading) {
    return <LoadingSpinner fullPage message="Loading..." />
  }

  if (!user) {
    return <Navigate to="/login" replace />
  }

  return <Navigate to={getDashboardPath(user.role)} replace />
}

function PublicRoute({ children }) {
  const { isAuthenticated, loading, user, getDashboardPath } = useAuth()

  if (loading) {
    return <LoadingSpinner fullPage message="Loading..." />
  }

  if (isAuthenticated) {
    return <Navigate to={getDashboardPath(user.role)} replace />
  }

  return children
}

export default function App() {
  return (
    <Routes>
      <Route
        path="/login"
        element={
          <PublicRoute>
            <LoginPage />
          </PublicRoute>
        }
      />

      <Route
        element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }
      >
        <Route path="/" element={<RootRedirect />} />

        <Route
          path="/dashboard/employee"
          element={
            <RoleBasedRoute allowedRoles={['EMPLOYEE']}>
              <EmployeeDashboard />
            </RoleBasedRoute>
          }
        />
        <Route
          path="/dashboard/manager"
          element={
            <RoleBasedRoute allowedRoles={['MANAGER']}>
              <ManagerDashboard />
            </RoleBasedRoute>
          }
        />
        <Route
          path="/dashboard/leadership"
          element={
            <RoleBasedRoute allowedRoles={['LEADERSHIP']}>
              <LeadershipDashboard />
            </RoleBasedRoute>
          }
        />
        <Route
          path="/dashboard/admin"
          element={
            <RoleBasedRoute allowedRoles={['ADMIN']}>
              <AdminDashboard />
            </RoleBasedRoute>
          }
        />

        <Route
          path="/attendance/history"
          element={
            <RoleBasedRoute allowedRoles={['EMPLOYEE']}>
              <AttendanceHistoryPage />
            </RoleBasedRoute>
          }
        />

        <Route path="/notifications" element={<NotificationsPage />} />

        <Route
          path="/admin/employees"
          element={
            <RoleBasedRoute allowedRoles={['ADMIN']}>
              <EmployeesPage />
            </RoleBasedRoute>
          }
        />
        <Route
          path="/admin/offices"
          element={
            <RoleBasedRoute allowedRoles={['ADMIN']}>
              <OfficeLocationsPage />
            </RoleBasedRoute>
          }
        />
        <Route
          path="/admin/policies"
          element={
            <RoleBasedRoute allowedRoles={['ADMIN']}>
              <PoliciesPage />
            </RoleBasedRoute>
          }
        />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
