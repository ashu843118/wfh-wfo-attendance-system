import { useCallback, useEffect, useState } from 'react'
import { Plus, Pencil, RefreshCw, UserCheck, UserX } from 'lucide-react'
import Topbar from '../components/layout/Topbar'
import DataTable from '../components/tables/DataTable'
import LoadingSpinner from '../components/common/LoadingSpinner'
import { useToast } from '../components/common/Toast'
import {
  getEmployees,
  createEmployee,
  updateEmployee,
  updateEmployeeStatus,
  getManagers,
  getTeams,
  getOfficeLocations,
} from '../api/adminApi'
import { getApiErrorMessage } from '../utils/format'
import './DashboardPages.css'

const ROLES = ['EMPLOYEE', 'MANAGER', 'LEADERSHIP', 'ADMIN']

const EMPTY_FORM = {
  name: '',
  email: '',
  role: 'EMPLOYEE',
  teamId: '',
  managerId: '',
  assignedOfficeLocationId: '',
  temporaryPassword: '',
  active: true,
}

export default function EmployeesPage() {
  const toast = useToast()
  const [employees, setEmployees] = useState([])
  const [pagination, setPagination] = useState({ page: 0, size: 20, totalPages: 0, totalElements: 0 })
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(EMPTY_FORM)
  const [managers, setManagers] = useState([])
  const [teams, setTeams] = useState([])
  const [offices, setOffices] = useState([])
  const [filters, setFilters] = useState({
    search: '',
    role: '',
    teamId: '',
    active: '',
    page: 0,
  })

  const loadDropdowns = useCallback(async () => {
    try {
      const [managerList, teamList, officeList] = await Promise.all([
        getManagers(),
        getTeams(),
        getOfficeLocations(),
      ])
      setManagers(managerList || [])
      setTeams(teamList || [])
      setOffices((officeList || []).filter((office) => office.active))
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }, [toast])

  const loadEmployees = useCallback(async () => {
    setLoading(true)
    try {
      const activeFilter =
        filters.active === '' ? undefined : filters.active === 'true'
      const data = await getEmployees({
        page: filters.page,
        size: pagination.size,
        search: filters.search || undefined,
        role: filters.role || undefined,
        teamId: filters.teamId ? Number(filters.teamId) : undefined,
        active: activeFilter,
      })
      setEmployees(data?.content || [])
      setPagination((prev) => ({
        ...prev,
        page: data?.page ?? 0,
        totalPages: data?.totalPages ?? 0,
        totalElements: data?.totalElements ?? 0,
      }))
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setLoading(false)
    }
  }, [filters, pagination.size, toast])

  useEffect(() => {
    loadDropdowns()
  }, [loadDropdowns])

  useEffect(() => {
    loadEmployees()
  }, [loadEmployees])

  const handleFilterChange = (e) => {
    const { name, value } = e.target
    setFilters((prev) => ({ ...prev, [name]: value, page: 0 }))
  }

  const handleFormChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }))
  }

  const openCreateForm = () => {
    setEditingId(null)
    setForm(EMPTY_FORM)
    setShowForm(true)
  }

  const openEditForm = (row) => {
    setEditingId(row.id)
    setForm({
      name: row.name || '',
      email: row.email || '',
      role: row.role || 'EMPLOYEE',
      teamId: row.teamId ? String(row.teamId) : '',
      managerId: row.managerId ? String(row.managerId) : '',
      assignedOfficeLocationId: row.assignedOfficeLocationId ? String(row.assignedOfficeLocationId) : '',
      temporaryPassword: '',
      active: row.active ?? true,
    })
    setShowForm(true)
  }

  const closeForm = () => {
    setShowForm(false)
    setEditingId(null)
    setForm(EMPTY_FORM)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true)
    try {
      if (editingId) {
        await updateEmployee(editingId, {
          name: form.name,
          role: form.role,
          teamId: form.teamId ? Number(form.teamId) : null,
          managerId: form.managerId ? Number(form.managerId) : null,
          assignedOfficeLocationId: form.assignedOfficeLocationId
            ? Number(form.assignedOfficeLocationId)
            : null,
          active: form.active,
        })
        toast.success('Employee updated successfully')
      } else {
        await createEmployee({
          name: form.name,
          email: form.email,
          role: form.role,
          teamId: form.teamId ? Number(form.teamId) : null,
          managerId: form.managerId ? Number(form.managerId) : null,
          assignedOfficeLocationId: form.assignedOfficeLocationId
            ? Number(form.assignedOfficeLocationId)
            : null,
          temporaryPassword: form.temporaryPassword,
          active: form.active,
        })
        toast.success('Employee created. They can log in with the temporary password.')
      }
      closeForm()
      await loadEmployees()
      await loadDropdowns()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    } finally {
      setSaving(false)
    }
  }

  const handleToggleStatus = async (row) => {
    const nextActive = !row.active
    const action = nextActive ? 'activate' : 'deactivate'
    if (!window.confirm(`${action.charAt(0).toUpperCase()}${action.slice(1)} ${row.name}?`)) return
    try {
      await updateEmployeeStatus(row.id, nextActive)
      toast.success(`Employee ${action}d successfully`)
      await loadEmployees()
    } catch (err) {
      toast.error(getApiErrorMessage(err))
    }
  }

  const teamRequired = form.role === 'EMPLOYEE' || form.role === 'MANAGER'
  const officeRequired = form.role === 'EMPLOYEE' || form.role === 'MANAGER'

  const columns = [
    { key: 'name', label: 'Name' },
    { key: 'email', label: 'Email' },
    { key: 'role', label: 'Role' },
    { key: 'teamName', label: 'Team', render: (row) => row.teamName || '—' },
    {
      key: 'assignedOfficeName',
      label: 'Office',
      render: (row) => row.assignedOfficeName || '—',
    },
    { key: 'managerName', label: 'Manager', render: (row) => row.managerName || '—' },
    {
      key: 'active',
      label: 'Status',
      render: (row) => (
        <span className={`badge ${row.active ? 'badge-wfo' : 'badge-late'}`}>
          {row.active ? 'Active' : 'Inactive'}
        </span>
      ),
    },
    {
      key: 'actions',
      label: 'Actions',
      render: (row) => (
        <div className="table-actions">
          <button type="button" className="btn btn-secondary btn-sm" onClick={() => openEditForm(row)}>
            <Pencil size={14} />
            Edit
          </button>
          <button
            type="button"
            className="btn btn-secondary btn-sm"
            onClick={() => handleToggleStatus(row)}
          >
            {row.active ? <UserX size={14} /> : <UserCheck size={14} />}
            {row.active ? 'Deactivate' : 'Activate'}
          </button>
        </div>
      ),
    },
  ]

  return (
    <>
      <Topbar
        title="Employee Management"
        actions={
          <div className="topbar-actions">
            <button type="button" className="btn btn-secondary btn-sm" onClick={loadEmployees}>
              <RefreshCw size={16} />
              Refresh
            </button>
            <button type="button" className="btn btn-primary btn-sm" onClick={openCreateForm}>
              <Plus size={16} />
              Create Employee
            </button>
          </div>
        }
      />
      <div className="app-layout__content dashboard-page">
        <div className="page-header">
          <h1>Employees</h1>
          <p>Admin-controlled employee provisioning. No public self-registration.</p>
        </div>

        <div className="card filters-card">
          <div className="filters-grid">
            <div className="form-group">
              <label htmlFor="search">Search</label>
              <input
                id="search"
                name="search"
                type="text"
                placeholder="Name or email"
                value={filters.search}
                onChange={handleFilterChange}
              />
            </div>
            <div className="form-group">
              <label htmlFor="role">Role</label>
              <select id="role" name="role" value={filters.role} onChange={handleFilterChange}>
                <option value="">All roles</option>
                {ROLES.map((role) => (
                  <option key={role} value={role}>{role}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="teamId">Team</label>
              <select id="teamId" name="teamId" value={filters.teamId} onChange={handleFilterChange}>
                <option value="">All teams</option>
                {teams.map((team) => (
                  <option key={team.id} value={team.id}>{team.name}</option>
                ))}
              </select>
            </div>
            <div className="form-group">
              <label htmlFor="active">Status</label>
              <select id="active" name="active" value={filters.active} onChange={handleFilterChange}>
                <option value="">All</option>
                <option value="true">Active</option>
                <option value="false">Inactive</option>
              </select>
            </div>
          </div>
        </div>

        {showForm && (
          <div className="card form-card">
            <div className="card-header">
              <h3 className="card-title">{editingId ? 'Edit Employee' : 'Create Employee'}</h3>
            </div>
            <form className="admin-form" onSubmit={handleSubmit}>
              <div className="form-grid">
                <div className="form-group">
                  <label htmlFor="name">Name *</label>
                  <input id="name" name="name" value={form.name} onChange={handleFormChange} required />
                </div>
                <div className="form-group">
                  <label htmlFor="email">Email *</label>
                  <input
                    id="email"
                    name="email"
                    type="email"
                    value={form.email}
                    onChange={handleFormChange}
                    required
                    disabled={!!editingId}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="form-role">Role *</label>
                  <select id="form-role" name="role" value={form.role} onChange={handleFormChange} required>
                    {ROLES.map((role) => (
                      <option key={role} value={role}>{role}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="form-teamId">Team {teamRequired ? '*' : ''}</label>
                  <select
                    id="form-teamId"
                    name="teamId"
                    value={form.teamId}
                    onChange={handleFormChange}
                    required={teamRequired}
                  >
                    <option value="">Select team</option>
                    {teams.map((team) => (
                      <option key={team.id} value={team.id}>{team.name}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="form-managerId">Reporting Manager</label>
                  <select id="form-managerId" name="managerId" value={form.managerId} onChange={handleFormChange}>
                    <option value="">None</option>
                    {managers.map((manager) => (
                      <option key={manager.id} value={manager.id}>{manager.name}</option>
                    ))}
                  </select>
                </div>
                <div className="form-group">
                  <label htmlFor="form-officeId">Assigned Office {officeRequired ? '*' : ''}</label>
                  <select
                    id="form-officeId"
                    name="assignedOfficeLocationId"
                    value={form.assignedOfficeLocationId}
                    onChange={handleFormChange}
                    required={officeRequired}
                  >
                    <option value="">Select office</option>
                    {offices.map((office) => (
                      <option key={office.id} value={office.id}>
                        {office.officeName} — {office.address}
                      </option>
                    ))}
                  </select>
                </div>
                {!editingId && (
                  <div className="form-group">
                    <label htmlFor="temporaryPassword">Temporary Password *</label>
                    <input
                      id="temporaryPassword"
                      name="temporaryPassword"
                      type="password"
                      value={form.temporaryPassword}
                      onChange={handleFormChange}
                      required
                      minLength={6}
                    />
                  </div>
                )}
                <div className="form-group form-group--checkbox">
                  <label>
                    <input
                      type="checkbox"
                      name="active"
                      checked={form.active}
                      onChange={handleFormChange}
                    />
                    Active
                  </label>
                </div>
              </div>
              <div className="form-actions">
                <button type="button" className="btn btn-secondary" onClick={closeForm}>Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? 'Saving...' : editingId ? 'Update Employee' : 'Create Employee'}
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="card">
          {loading ? (
            <LoadingSpinner message="Loading employees..." />
          ) : (
            <>
              <DataTable columns={columns} data={employees} emptyMessage="No employees found" />
              <div className="pagination-bar">
                <span>
                  Page {pagination.page + 1} of {Math.max(pagination.totalPages, 1)}
                  {' '}({pagination.totalElements} total)
                </span>
                <div className="pagination-actions">
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={pagination.page <= 0}
                    onClick={() => setFilters((prev) => ({ ...prev, page: prev.page - 1 }))}
                  >
                    Previous
                  </button>
                  <button
                    type="button"
                    className="btn btn-secondary btn-sm"
                    disabled={pagination.page >= pagination.totalPages - 1}
                    onClick={() => setFilters((prev) => ({ ...prev, page: prev.page + 1 }))}
                  >
                    Next
                  </button>
                </div>
              </div>
            </>
          )}
        </div>
      </div>
    </>
  )
}
