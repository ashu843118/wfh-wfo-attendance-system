import { useCallback, useState } from 'react'
import { RefreshCw } from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import LoadingSpinner from '../components/common/LoadingSpinner'
import EmptyState from '../components/common/EmptyState'
import { useToast } from '../components/common/Toast'
import { useAuth } from '../auth/AuthContext'
import usePolling from '../hooks/usePolling'
import { getNotifications, markAsRead } from '../api/notificationApi'
import { formatDateTime, formatLastUpdated, getApiErrorMessage } from '../utils/format'
import './DashboardPages.css'

export default function NotificationsPage() {
  const toast = useToast()
  const { isAuthenticated } = useAuth()
  const [page, setPage] = useState(0)

  const fetchNotifications = useCallback(() => getNotifications(page, 20), [page])

  const { data, loading, lastUpdated, refresh } = usePolling(fetchNotifications, 30000, {
    enabled: isAuthenticated,
  })

  const notifications = data?.content || []

  const handleRefresh = async () => {
    await refresh()
    toast.info('Notifications refreshed')
  }

  const handleMarkRead = async (id) => {
    try {
      await markAsRead(id)
      await refresh()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  return (
    <>
      <Topbar
        title="Notifications"
        actions={
          <button type="button" className="btn btn-secondary btn-sm" onClick={handleRefresh}>
            <RefreshCw size={16} />
            Refresh
          </button>
        }
      />
      <div className="app-layout__content">
        <div className="page-header">
          <p className="last-updated">Last updated: {formatLastUpdated(lastUpdated)}</p>
        </div>

        <div className="card">
          {loading && !data ? (
            <LoadingSpinner message="Loading notifications..." />
          ) : notifications.length === 0 ? (
            <EmptyState title="No notifications" description="You're all caught up!" />
          ) : (
            <>
              {notifications.map((n) => (
                <div
                  key={n.id}
                  className={`notification-item ${n.read ? '' : 'unread'}`}
                  role="button"
                  tabIndex={0}
                  onClick={() => !n.read && handleMarkRead(n.id)}
                  onKeyDown={(e) => e.key === 'Enter' && !n.read && handleMarkRead(n.id)}
                >
                  <div className="notification-item__header">
                    <span className="notification-item__title">{n.title}</span>
                    <span className="notification-item__time">{formatDateTime(n.createdAt)}</span>
                  </div>
                  <p className="notification-item__message">{n.message}</p>
                  {!n.read && (
                    <span className="badge badge-pending" style={{ marginTop: '0.5rem' }}>
                      Unread — click to mark read
                    </span>
                  )}
                </div>
              ))}

              {data && data.totalPages > 1 && (
                <div className="pagination">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={page === 0}
                    onClick={() => setPage((p) => p - 1)}
                  >
                    Previous
                  </button>
                  <span className="last-updated">
                    Page {page + 1} of {data.totalPages}
                  </span>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={data.last}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    Next
                  </button>
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </>
  )
}
