import { Outlet } from 'react-router-dom'
import NotificationBell from '../common/NotificationBell'
import Sidebar from './Sidebar'
import './AppLayout.css'

export default function AppLayout() {
  return (
    <div className="app-layout">
      <Sidebar />
      <div className="app-layout__main">
        <div className="app-layout__global-actions">
          <NotificationBell />
        </div>
        <Outlet />
      </div>
    </div>
  )
}
