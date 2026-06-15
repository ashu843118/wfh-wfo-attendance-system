import { LogOut } from 'lucide-react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import './Topbar.css'

export default function Topbar({ title, actions }) {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="topbar">
      <div className="topbar__left">
        {title && <h1 className="topbar__title">{title}</h1>}
      </div>
      <div className="topbar__right">
        {actions}
        <div className="topbar__user">
          <span className="topbar__email">{user?.email}</span>
        </div>
        <button type="button" className="topbar__logout btn btn-secondary btn-sm" onClick={handleLogout}>
          <LogOut size={16} />
          Logout
        </button>
      </div>
    </header>
  )
}
