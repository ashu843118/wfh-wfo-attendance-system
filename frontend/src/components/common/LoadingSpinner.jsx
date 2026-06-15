import { Loader2 } from 'lucide-react'
import './LoadingSpinner.css'

export default function LoadingSpinner({ message = 'Loading...', fullPage = false, size = 32 }) {
  return (
    <div className={`loading-spinner ${fullPage ? 'loading-spinner--full' : ''}`}>
      <Loader2 size={size} className="loading-spinner__icon" />
      {message && <p className="loading-spinner__message">{message}</p>}
    </div>
  )
}
