import { NavLink } from 'react-router-dom'
import {
  LayoutDashboard,
  History,
  Bell,
  MapPin,
  FileText,
  Users,
  Building2,
  Shield,
} from 'lucide-react'
import { useAuth } from '../../auth/AuthContext'
import './Sidebar.css'

export default function Sidebar() {
  const { user, getDashboardPath } = useAuth()
  const role = user?.role

  const dashboardPath = getDashboardPath(role)

  const navItems = [
    { to: dashboardPath, label: 'Dashboard', icon: LayoutDashboard, roles: ['EMPLOYEE', 'MANAGER', 'LEADERSHIP', 'ADMIN'] },
    { to: '/attendance/history', label: 'My History', icon: History, roles: ['EMPLOYEE'] },
    { to: '/notifications', label: 'Notifications', icon: Bell, roles: ['EMPLOYEE', 'MANAGER', 'LEADERSHIP', 'ADMIN'] },
    { to: '/admin/employees', label: 'Employees', icon: Users, roles: ['ADMIN'] },
    { to: '/admin/offices', label: 'Office Locations', icon: MapPin, roles: ['ADMIN'] },
    { to: '/admin/policies', label: 'Policies', icon: FileText, roles: ['ADMIN'] },
  ]

  const visibleItems = navItems.filter((item) => item.roles.includes(role))

  const roleIcons = {
    EMPLOYEE: Users,
    MANAGER: Users,
    LEADERSHIP: Building2,
    ADMIN: Shield,
  }
  const RoleIcon = roleIcons[role] || Users

  return (
    <aside className="sidebar">
      <div className="sidebar__brand">
        <div className="sidebar__logo">EY</div>
        <div>
          <div className="sidebar__title">Attendance</div>
          <div className="sidebar__subtitle">Enterprise Portal</div>
        </div>
      </div>

      <nav className="sidebar__nav">
        {visibleItems.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) => `sidebar__link ${isActive ? 'active' : ''}`}
          >
            <item.icon size={18} />
            <span>{item.label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="sidebar__footer">
        <div className="sidebar__user">
          <div className="sidebar__avatar">
            <RoleIcon size={16} />
          </div>
          <div>
            <div className="sidebar__user-name">{user?.name}</div>
            <div className="sidebar__user-role">{role}</div>
          </div>
        </div>
      </div>
    </aside>
  )
}
