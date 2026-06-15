import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { LogIn, Building2, Shield, Users, Crown } from 'lucide-react'
import { useAuth } from '../auth/AuthContext'
import { useToast } from '../components/common/Toast'
import { getApiErrorMessage } from '../utils/format'
import './LoginPage.css'

const DEMO_USERS = [
  { email: 'employee@demo.com', password: 'password', role: 'EMPLOYEE', label: 'Employee', icon: Users },
  { email: 'manager@demo.com', password: 'password', role: 'MANAGER', label: 'Manager', icon: Crown },
  { email: 'leader@demo.com', password: 'password', role: 'LEADERSHIP', label: 'Leadership', icon: Building2 },
  { email: 'admin@demo.com', password: 'password', role: 'ADMIN', label: 'Admin', icon: Shield },
]

export default function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const { login, getDashboardPath } = useAuth()
  const navigate = useNavigate()
  const toast = useToast()

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const user = await login(email, password)
      toast.success(`Welcome back, ${user.name}!`)
      navigate(getDashboardPath(user.role), { replace: true })
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  const handleQuickLogin = async (demoUser) => {
    setEmail(demoUser.email)
    setPassword(demoUser.password)
    setLoading(true)
    try {
      const user = await login(demoUser.email, demoUser.password)
      toast.success(`Signed in as ${demoUser.label}`)
      navigate(getDashboardPath(user.role), { replace: true })
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-page">
      <div className="login-page__hero">
        <div className="login-page__brand">
          <div className="login-page__logo">EY</div>
          <h1>Enterprise Attendance Portal</h1>
          <p>Track WFH/WFO attendance, manage teams, and gain organizational insights.</p>
        </div>
        <ul className="login-page__features">
          <li>Geo-verified check-in &amp; check-out</li>
          <li>Real-time classification &amp; analytics</li>
          <li>Role-based dashboards &amp; notifications</li>
        </ul>
      </div>

      <div className="login-page__form-panel">
        <div className="login-page__form-card">
          <h2>Sign in</h2>
          <p className="login-page__subtitle">Enter your credentials or use a demo account</p>

          <form onSubmit={handleSubmit} className="login-page__form">
            <div className="form-group">
              <label htmlFor="email">Email</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="you@company.com"
                required
                autoComplete="username"
              />
            </div>
            <div className="form-group">
              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                autoComplete="current-password"
              />
            </div>
            <button type="submit" className="btn btn-primary btn-lg login-page__submit" disabled={loading}>
              <LogIn size={18} />
              {loading ? 'Signing in...' : 'Sign in'}
            </button>
          </form>

          <div className="login-page__divider">
            <span>Quick demo login</span>
          </div>

          <div className="login-page__demo-grid">
            {DEMO_USERS.map((demo) => (
              <button
                key={demo.role}
                type="button"
                className="login-page__demo-btn"
                onClick={() => handleQuickLogin(demo)}
                disabled={loading}
              >
                <demo.icon size={18} />
                <span>{demo.label}</span>
              </button>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
