import { Link } from 'react-router-dom'
import { MapPin, FileText, Shield, Settings, Users } from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import './DashboardPages.css'

const ADMIN_LINKS = [
  {
    to: '/admin/employees',
    title: 'Employee Management',
    description: 'Create and manage employees, roles, teams, and reporting managers',
    icon: Users,
  },
  {
    to: '/admin/offices',
    title: 'Office Locations',
    description: 'Manage geo-fenced office locations for WFO verification',
    icon: MapPin,
  },
  {
    to: '/admin/policies',
    title: 'Attendance Policies',
    description: 'Configure WFO minimums, check-in times, and late thresholds',
    icon: FileText,
  },
]

export default function AdminDashboard() {
  return (
    <>
      <Topbar title="Admin Dashboard" />
      <div className="app-layout__content dashboard-page">
        <div className="page-header">
          <h1>Administration</h1>
          <p>Manage office locations, attendance policies, and system configuration</p>
        </div>

        <div className="admin-links-grid">
          {ADMIN_LINKS.map((link) => (
            <Link key={link.to} to={link.to} className="admin-link-card">
              <div className="admin-link-card__icon">
                <link.icon size={24} />
              </div>
              <div>
                <h3>{link.title}</h3>
                <p>{link.description}</p>
              </div>
            </Link>
          ))}

          <div className="admin-link-card admin-link-card--disabled">
            <div className="admin-link-card__icon">
              <Settings size={24} />
            </div>
            <div>
              <h3>System Settings</h3>
              <p>Additional configuration options coming soon</p>
            </div>
          </div>

          <div className="admin-link-card admin-link-card--info">
            <div className="admin-link-card__icon">
              <Shield size={24} />
            </div>
            <div>
              <h3>Security &amp; Access</h3>
              <p>Role-based access control is enforced across all portal routes</p>
            </div>
          </div>
        </div>
      </div>
    </>
  )
}
