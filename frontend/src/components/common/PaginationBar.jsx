import './PaginationBar.css'

const PAGE_SIZE_OPTIONS = [10, 20, 50]

export default function PaginationBar({
  page = 0,
  size = 20,
  totalElements = 0,
  totalPages = 0,
  onPageChange,
  onSizeChange,
  loading = false,
  error = null,
}) {
  if (error) {
    return <div className="pagination-bar pagination-bar--error">{error}</div>
  }

  const safeTotalPages = Math.max(totalPages, 1)
  const start = totalElements === 0 ? 0 : page * size + 1
  const end = Math.min((page + 1) * size, totalElements)

  return (
    <div className="pagination-bar">
      <div className="pagination-bar__info">
        {loading ? (
          <span>Loading...</span>
        ) : (
          <span>
            Showing {start}–{end} of {totalElements}
          </span>
        )}
      </div>

      <div className="pagination-actions">
        {onSizeChange && (
          <label className="pagination-bar__size">
            <span className="sr-only">Page size</span>
            <select
              value={size}
              disabled={loading}
              onChange={(e) => onSizeChange(Number(e.target.value))}
              aria-label="Page size"
            >
              {PAGE_SIZE_OPTIONS.map((option) => (
                <option key={option} value={option}>
                  {option} / page
                </option>
              ))}
            </select>
          </label>
        )}

        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={loading || page <= 0}
          onClick={() => onPageChange(page - 1)}
        >
          Previous
        </button>

        <span className="pagination-bar__page">
          Page {page + 1} of {safeTotalPages}
        </span>

        <button
          type="button"
          className="btn btn-secondary btn-sm"
          disabled={loading || page >= totalPages - 1 || totalPages === 0}
          onClick={() => onPageChange(page + 1)}
        >
          Next
        </button>
      </div>
    </div>
  )
}
