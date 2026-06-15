import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import { CheckCircle, AlertCircle, Info, X } from 'lucide-react'
import './Toast.css'

const ToastContext = createContext(null)

let toastId = 0

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([])

  const removeToast = useCallback((id) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  const addToast = useCallback((message, type = 'info', duration = 4000) => {
    const id = ++toastId
    setToasts((prev) => [...prev, { id, message, type }])
    if (duration > 0) {
      setTimeout(() => removeToast(id), duration)
    }
    return id
  }, [removeToast])

  const value = useMemo(
    () => ({
      showToast: addToast,
      success: (msg) => addToast(msg, 'success'),
      error: (msg) => addToast(msg, 'error'),
      info: (msg) => addToast(msg, 'info'),
    }),
    [addToast],
  )

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className="toast-container" aria-live="polite">
        {toasts.map((toast) => (
          <ToastItem key={toast.id} toast={toast} onClose={() => removeToast(toast.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  )
}

function ToastItem({ toast, onClose }) {
  const icons = {
    success: CheckCircle,
    error: AlertCircle,
    info: Info,
  }
  const Icon = icons[toast.type] || Info

  return (
    <div className={`toast toast-${toast.type}`} role="alert">
      <Icon size={18} />
      <span className="toast-message">{toast.message}</span>
      <button type="button" className="toast-close" onClick={onClose} aria-label="Dismiss">
        <X size={16} />
      </button>
    </div>
  )
}

export function useToast() {
  const context = useContext(ToastContext)
  if (!context) {
    throw new Error('useToast must be used within ToastProvider')
  }
  return context
}
