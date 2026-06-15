import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { getNotifications, getUnreadCount, markAsRead } from '../../api/notificationApi'
import { useAuth } from '../../auth/AuthContext'
import usePolling from '../../hooks/usePolling'
import './NotificationBell.css'

export default function NotificationBell() {
  const { isAuthenticated } = useAuth()
  const [open, setOpen] = useState(false)
  const ref = useRef(null)

  const { data: unreadCount = 0, refresh: refreshCount } = usePolling(getUnreadCount, 30000, {
    enabled: isAuthenticated,
  })
  const { data: notificationsPage, refresh: refreshList } = usePolling(
    () => getNotifications(0, 8),
    30000,
    { enabled: isAuthenticated },
  )

  const notifications = notificationsPage?.content || []

  useEffect(() => {
    const handleClick = (e) => {
      if (ref.current && !ref.current.contains(e.target)) {
        setOpen(false)
      }
    }
    document.addEventListener('mousedown', handleClick)
    return () => document.removeEventListener('mousedown', handleClick)
  }, [])

  const handleMarkRead = async (id) => {
    await markAsRead(id)
    refreshCount()
    refreshList()
  }

  return (
    <div className="notification-bell" ref={ref}>
      <button
        type="button"
        className="notification-bell__trigger"
        onClick={() => setOpen(!open)}
        aria-label={`Notifications${unreadCount ? `, ${unreadCount} unread` : ''}`}
      >
        <Bell size={20} />
        {unreadCount > 0 && (
          <span className="notification-bell__badge">{unreadCount > 99 ? '99+' : unreadCount}</span>
        )}
      </button>

      {open && (
        <div className="notification-bell__dropdown">
          <div className="notification-bell__header">
            <span>Notifications</span>
            <Link to="/notifications" onClick={() => setOpen(false)}>
              View all
            </Link>
          </div>
          <div className="notification-bell__list">
            {notifications.length === 0 ? (
              <p className="notification-bell__empty">No notifications</p>
            ) : (
              notifications.map((n) => (
                <button
                  key={n.id}
                  type="button"
                  className={`notification-bell__item ${n.read ? '' : 'unread'}`}
                  onClick={() => !n.read && handleMarkRead(n.id)}
                >
                  <span className="notification-bell__item-title">{n.title}</span>
                  <span className="notification-bell__item-msg">{n.message}</span>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}
