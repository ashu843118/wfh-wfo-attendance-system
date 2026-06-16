import { useCallback, useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, RefreshCw } from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import DataTable from '../components/tables/DataTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import PaginationBar from '../components/common/PaginationBar'
import { useToast } from '../components/common/Toast'
import {
  getOfficeLocations,
  createOfficeLocation,
  updateOfficeLocation,
  deleteOfficeLocation,
} from '../api/adminApi'
import { getApiErrorMessage } from '../utils/format'
import '../pages/DashboardPages.css'

const EMPTY_FORM = {
  officeName: '',
  address: '',
  latitude: '',
  longitude: '',
  radiusMeters: '100',
  active: true,
}

export default function OfficeLocationsPage() {
  const toast = useToast()
  const [locations, setLocations] = useState([])
  const [pagination, setPagination] = useState({ page: 0, size: 20, totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)

  const loadLocations = useCallback(async (page = pagination.page, size = pagination.size) => {
    setLoading(true)
    try {
      const data = await getOfficeLocations({ page, size })
      setLocations(data?.content || [])
      setPagination({
        page: data?.page ?? page,
        size: data?.size ?? size,
        totalPages: data?.totalPages ?? 0,
        totalElements: data?.totalElements ?? 0,
      })
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [pagination.page, pagination.size, toast])

  useEffect(() => {
    loadLocations()
  }, [loadLocations])

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const handleEdit = (row) => {
    setEditingId(row.id)
    setForm({
      officeName: row.officeName || '',
      address: row.address || '',
      latitude: String(row.latitude ?? ''),
      longitude: String(row.longitude ?? ''),
      radiusMeters: String(row.radiusMeters ?? ''),
      active: row.active ?? true,
    })
  }

  const handleCancel = () => {
    setEditingId(null)
    setForm(EMPTY_FORM)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    const payload = {
      officeName: form.officeName,
      address: form.address,
      latitude: parseFloat(form.latitude),
      longitude: parseFloat(form.longitude),
      radiusMeters: parseInt(form.radiusMeters, 10),
      active: form.active,
    }
    try {
      if (editingId) {
        await updateOfficeLocation(editingId, payload)
        toast.success('Office location updated')
      } else {
        await createOfficeLocation(payload)
        toast.success('Office location created')
      }
      handleCancel()
      await loadLocations()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this office location?')) return
    try {
      await deleteOfficeLocation(id)
      toast.success('Office location deleted')
      await loadLocations()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  const columns = [
    { key: 'officeName', label: 'Name' },
    { key: 'address', label: 'Address' },
    { key: 'latitude', label: 'Latitude' },
    { key: 'longitude', label: 'Longitude' },
    { key: 'radiusMeters', label: 'Radius (m)' },
    {
      key: 'active',
      label: 'Active',
      render: (row) => (row.active ? 'Yes' : 'No'),
    },
    {
      key: 'actions',
      label: 'Actions',
      render: (row) => (
        <div className="page-actions">
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => handleEdit(row)}>
            <Pencil size={14} />
          </button>
          <button type="button" className="btn btn-danger btn-sm" onClick={() => handleDelete(row.id)}>
            <Trash2 size={14} />
          </button>
        </div>
      ),
    },
  ]

  return (
    <>
      <Topbar
        title="Office Locations"
        actions={
          <button type="button" className="btn btn-secondary btn-sm" onClick={loadLocations}>
            <RefreshCw size={16} />
            Refresh
          </button>
        }
      />
      <div className="app-layout__content">
        <div className="card crud-form">
          <div className="card-header">
            <h3 className="card-title">{editingId ? 'Edit Office Location' : 'Add Office Location'}</h3>
            {editingId && (
              <button type="button" className="btn btn-secondary btn-sm" onClick={handleCancel}>
                Cancel
              </button>
            )}
          </div>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="officeName">Office Name</label>
                <input id="officeName" name="officeName" value={form.officeName} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label htmlFor="address">Address</label>
                <input id="address" name="address" value={form.address} onChange={handleChange} />
              </div>
              <div className="form-group">
                <label htmlFor="latitude">Latitude</label>
                <input id="latitude" name="latitude" type="number" step="any" value={form.latitude} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label htmlFor="longitude">Longitude</label>
                <input id="longitude" name="longitude" type="number" step="any" value={form.longitude} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label htmlFor="radiusMeters">Radius (meters)</label>
                <input id="radiusMeters" name="radiusMeters" type="number" min="1" value={form.radiusMeters} onChange={handleChange} required />
              </div>
              <div className="form-group checkbox">
                <input id="active" name="active" type="checkbox" checked={form.active} onChange={handleChange} />
                <label htmlFor="active">Active</label>
              </div>
            </div>
            <div className="crud-form__actions">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                <Plus size={16} />
                {saving ? 'Saving...' : editingId ? 'Update Location' : 'Add Location'}
              </button>
            </div>
          </form>
        </div>

        <div className="card">
          <div className="card-header">
            <h3 className="card-title">All Office Locations</h3>
          </div>
          {loading ? (
            <LoadingSpinner message="Loading locations..." />
          ) : (
            <>
              <DataTable columns={columns} data={locations} keyField="id" emptyMessage="No office locations configured" />
              <PaginationBar
                page={pagination.page}
                size={pagination.size}
                totalElements={pagination.totalElements}
                totalPages={pagination.totalPages}
                loading={loading}
                onPageChange={(nextPage) => loadLocations(nextPage, pagination.size)}
                onSizeChange={(nextSize) => loadLocations(0, nextSize)}
              />
            </>
          )}
        </div>
      </div>
    </>
  )
}
