import { useCallback, useEffect, useState } from 'react'
import { Plus, Pencil, Trash2, RefreshCw } from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import DataTable from '../components/tables/DataTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import PaginationBar from '../components/common/PaginationBar'
import { useToast } from '../components/common/Toast'
import { getPolicies, createPolicy, updatePolicy, deletePolicy } from '../api/adminApi'
import { getApiErrorMessage } from '../utils/format'
import '../pages/DashboardPages.css'

const EMPTY_FORM = {
  teamId: '',
  minimumWfoDaysPerWeek: '3',
  standardCheckInTime: '09:00',
  standardCheckOutTime: '18:00',
  lateThresholdMinutes: '15',
  requiredWfoMinutes: '180',
  active: true,
}

export default function PoliciesPage() {
  const toast = useToast()
  const [policies, setPolicies] = useState([])
  const [pagination, setPagination] = useState({ page: 0, size: 20, totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)

  const loadPolicies = useCallback(async (page = pagination.page, size = pagination.size) => {
    setLoading(true)
    try {
      const data = await getPolicies({ page, size })
      setPolicies(data?.content || [])
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
    loadPolicies()
  }, [loadPolicies])

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
      teamId: String(row.teamId ?? ''),
      minimumWfoDaysPerWeek: String(row.minimumWfoDaysPerWeek ?? ''),
      standardCheckInTime: row.standardCheckInTime?.slice(0, 5) || '09:00',
      standardCheckOutTime: row.standardCheckOutTime?.slice(0, 5) || '18:00',
      lateThresholdMinutes: String(row.lateThresholdMinutes ?? ''),
      requiredWfoMinutes: String(row.requiredWfoMinutes ?? '180'),
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
      teamId: parseInt(form.teamId, 10),
      minimumWfoDaysPerWeek: parseInt(form.minimumWfoDaysPerWeek, 10),
      standardCheckInTime: form.standardCheckInTime,
      standardCheckOutTime: form.standardCheckOutTime,
      lateThresholdMinutes: parseInt(form.lateThresholdMinutes, 10),
      requiredWfoMinutes: parseInt(form.requiredWfoMinutes, 10),
      active: form.active,
    }
    try {
      if (editingId) {
        await updatePolicy(editingId, payload)
        toast.success('Policy updated')
      } else {
        await createPolicy(payload)
        toast.success('Policy created')
      }
      handleCancel()
      await loadPolicies()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this policy?')) return
    try {
      await deletePolicy(id)
      toast.success('Policy deleted')
      await loadPolicies()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  const columns = [
    { key: 'teamId', label: 'Team ID' },
    { key: 'minimumWfoDaysPerWeek', label: 'Min WFO Days/Week' },
    {
      key: 'standardCheckInTime',
      label: 'Check-In',
      render: (row) => row.standardCheckInTime?.slice(0, 5) || '—',
    },
    {
      key: 'standardCheckOutTime',
      label: 'Check-Out',
      render: (row) => row.standardCheckOutTime?.slice(0, 5) || '—',
    },
    { key: 'lateThresholdMinutes', label: 'Late Threshold (min)' },
    { key: 'requiredWfoMinutes', label: 'Required WFO (min)' },
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
        title="Attendance Policies"
        actions={
          <button type="button" className="btn btn-secondary btn-sm" onClick={loadPolicies}>
            <RefreshCw size={16} />
            Refresh
          </button>
        }
      />
      <div className="app-layout__content">
        <div className="card crud-form">
          <div className="card-header">
            <h3 className="card-title">{editingId ? 'Edit Policy' : 'Add Policy'}</h3>
            {editingId && (
              <button type="button" className="btn btn-secondary btn-sm" onClick={handleCancel}>
                Cancel
              </button>
            )}
          </div>
          <form onSubmit={handleSubmit}>
            <div className="form-grid">
              <div className="form-group">
                <label htmlFor="teamId">Team ID</label>
                <input id="teamId" name="teamId" type="number" min="1" value={form.teamId} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label htmlFor="minimumWfoDaysPerWeek">Min WFO Days/Week</label>
                <input id="minimumWfoDaysPerWeek" name="minimumWfoDaysPerWeek" type="number" min="0" value={form.minimumWfoDaysPerWeek} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label htmlFor="standardCheckInTime">Standard Check-In</label>
                <input id="standardCheckInTime" name="standardCheckInTime" type="time" value={form.standardCheckInTime} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label htmlFor="standardCheckOutTime">Standard Check-Out</label>
                <input id="standardCheckOutTime" name="standardCheckOutTime" type="time" value={form.standardCheckOutTime} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label htmlFor="lateThresholdMinutes">Late Threshold (minutes)</label>
                <input id="lateThresholdMinutes" name="lateThresholdMinutes" type="number" min="0" value={form.lateThresholdMinutes} onChange={handleChange} required />
              </div>
              <div className="form-group">
                <label htmlFor="requiredWfoMinutes">Required WFO Minutes</label>
                <input id="requiredWfoMinutes" name="requiredWfoMinutes" type="number" min="1" value={form.requiredWfoMinutes} onChange={handleChange} required />
              </div>
              <div className="form-group checkbox">
                <input id="active" name="active" type="checkbox" checked={form.active} onChange={handleChange} />
                <label htmlFor="active">Active</label>
              </div>
            </div>
            <div className="crud-form__actions">
              <button type="submit" className="btn btn-primary" disabled={saving}>
                <Plus size={16} />
                {saving ? 'Saving...' : editingId ? 'Update Policy' : 'Add Policy'}
              </button>
            </div>
          </form>
        </div>

        <div className="card">
          <div className="card-header">
            <h3 className="card-title">All Policies</h3>
          </div>
          {loading ? (
            <LoadingSpinner message="Loading policies..." />
          ) : (
            <>
              <DataTable columns={columns} data={policies} keyField="id" emptyMessage="No policies configured" />
              <PaginationBar
                page={pagination.page}
                size={pagination.size}
                totalElements={pagination.totalElements}
                totalPages={pagination.totalPages}
                loading={loading}
                onPageChange={(nextPage) => loadPolicies(nextPage, pagination.size)}
                onSizeChange={(nextSize) => loadPolicies(0, nextSize)}
              />
            </>
          )}
        </div>
      </div>
    </>
  )
}
