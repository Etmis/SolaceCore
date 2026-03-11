import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import {
  getRoles, createRole, updateRole, deleteRole,
  getModerators, createModerator, updateModerator, deleteModerator,
  getModeratorRoles, assignRole, removeRole
} from '../api'
import type { Role } from '../types'
import { FaPlus, FaEdit, FaTrash, FaUserShield, FaUsers, FaKey, FaCheck, FaTimes } from 'react-icons/fa'
import './AdminPanel.css'

interface Moderator {
  id: number
  username: string
  is_active: boolean
}

export default function AdminPanel() {
  const { hasPermission } = useAuth()
  const [activeTab, setActiveTab] = useState<'moderators' | 'roles'>('moderators')
  
  // Moderators
  const [moderators, setModerators] = useState<Moderator[]>([])
  const [selectedModerator, setSelectedModerator] = useState<Moderator | null>(null)
  const [moderatorRoles, setModeratorRoles] = useState<Role[]>([])
  const [showCreateModerator, setShowCreateModerator] = useState(false)
  const [newModUsername, setNewModUsername] = useState('')
  const [newModPassword, setNewModPassword] = useState('')
  const [generatedPassword, setGeneratedPassword] = useState<string | null>(null)
  
  // Roles
  const [roles, setRoles] = useState<Role[]>([])
  const [showCreateRole, setShowCreateRole] = useState(false)
  const [editingRole, setEditingRole] = useState<Role | null>(null)
  const [newRoleName, setNewRoleName] = useState('')
  const [newRolePermissions, setNewRolePermissions] = useState({
    ban: false,
    unban: false,
    warn: false,
    kick: false,
    mute: false,
    unmute: false,
    manageRoles: false,
    viewActions: false
  })
  
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    if (!hasPermission('manageRoles')) return
    loadData()
  }, [hasPermission])

  const loadData = async () => {
    setLoading(true)
    setError(null)
    try {
      const [rolesData, modsData] = await Promise.all([getRoles(), getModerators()])
      setRoles(rolesData)
      setModerators(modsData)
    } catch (err: any) {
      setError(err.message || 'Failed to load data')
    } finally {
      setLoading(false)
    }
  }

  const loadModeratorRoles = async (modId: number) => {
    try {
      const roles = await getModeratorRoles(modId)
      setModeratorRoles(roles)
    } catch (err: any) {
      setError(err.message || 'Failed to load moderator roles')
    }
  }

  // ===== MODERATOR FUNCTIONS =====
  
  const generatePassword = (): string => {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%'
    let password = ''
    for (let i = 0; i < 12; i++) {
      password += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    return password
  }

  const handleCreateModerator = async () => {
    if (!newModUsername.trim()) {
      setError('Username is required')
      return
    }

    const password = newModPassword || generatedPassword
    if (!password) {
      setError('Password is required')
      return
    }

    try {
      await createModerator(newModUsername, password)
      setSuccess(`Moderator "${newModUsername}" created successfully. Password: ${password}`)
      setNewModUsername('')
      setNewModPassword('')
      setGeneratedPassword(null)
      setShowCreateModerator(false)
      loadData()
      setTimeout(() => setSuccess(null), 5000)
    } catch (err: any) {
      setError(err.message || 'Failed to create moderator')
    }
  }

  const handleToggleModerator = async (mod: Moderator) => {
    try {
      await updateModerator(mod.id, { is_active: !mod.is_active })
      loadData()
      setSuccess(`Moderator ${mod.is_active ? 'deactivated' : 'activated'} successfully`)
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      setError(err.message || 'Failed to update moderator')
    }
  }

  const handleDeleteModerator = async (mod: Moderator) => {
    if (!confirm(`Are you sure you want to delete moderator "${mod.username}"? This action cannot be undone.`)) {
      return
    }

    try {
      await deleteModerator(mod.id)
      loadData()
      if (selectedModerator?.id === mod.id) {
        setSelectedModerator(null)
        setModeratorRoles([])
      }
      setSuccess(`Moderator "${mod.username}" deleted successfully`)
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      setError(err.message || 'Failed to delete moderator')
    }
  }

  const handleAssignRole = async (roleId: number) => {
    if (!selectedModerator) return

    try {
      await assignRole(selectedModerator.id, roleId)
      loadModeratorRoles(selectedModerator.id)
      setSuccess('Role assigned successfully')
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      setError(err.message || 'Failed to assign role')
    }
  }

  const handleRemoveRole = async (roleId: number) => {
    if (!selectedModerator) return

    try {
      await removeRole(selectedModerator.id, roleId)
      loadModeratorRoles(selectedModerator.id)
      setSuccess('Role removed successfully')
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      setError(err.message || 'Failed to remove role')
    }
  }

  // ===== ROLE FUNCTIONS =====

  const handleCreateRole = async () => {
    if (!newRoleName.trim()) {
      setError('Role name is required')
      return
    }

    try {
      await createRole(newRoleName, newRolePermissions)
      setShowCreateRole(false)
      setNewRoleName('')
      setNewRolePermissions({
        ban: false,
        unban: false,
        warn: false,
        kick: false,
        mute: false,
        unmute: false,
        manageRoles: false,
        viewActions: false
      })
      loadData()
      setSuccess('Role created successfully')
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      setError(err.message || 'Failed to create role')
    }
  }

  const handleUpdateRole = async () => {
    if (!editingRole) return

    try {
      await updateRole(editingRole.id, editingRole.name, editingRole.permissions)
      setEditingRole(null)
      loadData()
      setSuccess('Role updated successfully')
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      setError(err.message || 'Failed to update role')
    }
  }

  const handleDeleteRole = async (id: number) => {
    if (!confirm('Are you sure you want to delete this role?')) return

    try {
      await deleteRole(id)
      loadData()
      setSuccess('Role deleted successfully')
      setTimeout(() => setSuccess(null), 3000)
    } catch (err: any) {
      setError(err.message || 'Failed to delete role')
    }
  }

  if (!hasPermission('manageRoles')) {
    return (
      <div className="admin-panel">
        <p className="error-message">You don't have permission to access the admin panel.</p>
      </div>
    )
  }

  if (loading) {
    return <div className="admin-panel"><div className="loading">Loading...</div></div>
  }

  return (
    <div className="admin-panel">
      <div className="admin-header">
        <h1><FaUserShield /> Administration Panel</h1>
        <div className="admin-tabs">
          <button
            className={`tab-btn ${activeTab === 'moderators' ? 'active' : ''}`}
            onClick={() => setActiveTab('moderators')}
          >
            <FaUsers /> Moderators ({moderators.length})
          </button>
          <button
            className={`tab-btn ${activeTab === 'roles' ? 'active' : ''}`}
            onClick={() => setActiveTab('roles')}
          >
            <FaKey /> Roles ({roles.length})
          </button>
        </div>
      </div>

      {error && (
        <div className="message error-message">
          {error}
          <button className="message-close" onClick={() => setError(null)}>×</button>
        </div>
      )}
      {success && (
        <div className="message success-message">
          {success}
          <button className="message-close" onClick={() => setSuccess(null)}>×</button>
        </div>
      )}

      {/* MODERATORS TAB */}
      {activeTab === 'moderators' && (
        <div className="admin-section">
          <div className="section-header">
            <h2>Moderator Management</h2>
            <button
              className="btn btn-primary"
              onClick={() => {
                setShowCreateModerator(!showCreateModerator)
                setGeneratedPassword(null)
                setNewModUsername('')
                setNewModPassword('')
              }}
            >
              <FaPlus /> Create Moderator
            </button>
          </div>

          {showCreateModerator && (
            <div className="create-form">
              <h3>Create New Moderator</h3>
              <div className="form-group">
                <label>Username *</label>
                <input
                  type="text"
                  placeholder="Enter moderator username (3-50 characters)"
                  value={newModUsername}
                  onChange={(e) => setNewModUsername(e.target.value)}
                  minLength={3}
                  maxLength={50}
                />
              </div>

              <div className="form-row">
                <div className="form-group">
                  <label>Password *</label>
                  <input
                    type="text"
                    placeholder="Enter password or use generated one"
                    value={newModPassword}
                    onChange={(e) => setNewModPassword(e.target.value)}
                    minLength={6}
                  />
                </div>
                <div className="form-group">
                  <label>&nbsp;</label>
                  <button
                    className="btn btn-secondary"
                    type="button"
                    onClick={() => setGeneratedPassword(generatePassword())}
                  >
                    <FaKey /> Generate Password
                  </button>
                </div>
              </div>

              {generatedPassword && (
                <div className="password-display">
                  <strong>Generated Password:</strong>
                  <code>{generatedPassword}</code>
                  <small>Copy this password and share it securely with the moderator</small>
                </div>
              )}

              <div className="form-actions">
                <button className="btn btn-primary" onClick={handleCreateModerator}>
                  Create Moderator
                </button>
                <button className="btn btn-secondary" onClick={() => setShowCreateModerator(false)}>
                  Cancel
                </button>
              </div>
            </div>
          )}

          <div className="moderators-container">
            <div className="moderators-list">
              <h3>All Moderators</h3>
              {moderators.length === 0 ? (
                <p className="empty-state">No moderators created yet</p>
              ) : (
                <div className="list-items">
                  {moderators.map((mod) => (
                    <div
                      key={mod.id}
                      className={`moderator-item ${selectedModerator?.id === mod.id ? 'selected' : ''} ${!mod.is_active ? 'inactive' : ''}`}
                      onClick={() => {
                        setSelectedModerator(mod)
                        loadModeratorRoles(mod.id)
                      }}
                    >
                      <div className="moderator-content">
                        <div className="moderator-name">{mod.username}</div>
                      </div>
                      <div className="moderator-status">
                        {mod.is_active ? (
                          <span className="status-badge active"><FaCheck /> Active</span>
                        ) : (
                          <span className="status-badge inactive"><FaTimes /> Inactive</span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {selectedModerator && (
              <div className="moderator-details">
                <h3>Manage: {selectedModerator.username}</h3>

                <div className="role-assignment">
                  <h4>Assigned Roles</h4>
                  {moderatorRoles.length === 0 ? (
                    <p className="empty-state">No roles assigned</p>
                  ) : (
                    <div className="assigned-roles-list">
                      {moderatorRoles.map((role) => (
                        <div key={role.id} className="assigned-role-item">
                          <div className="role-name">{role.name}</div>
                          <button
                            className="btn btn-danger btn-sm"
                            onClick={() => handleRemoveRole(role.id)}
                          >
                            <FaTrash />
                          </button>
                        </div>
                      ))}
                    </div>
                  )}

                  <div className="role-selector">
                    <select
                      onChange={(e) => {
                        const roleId = parseInt(e.target.value)
                        if (roleId) {
                          handleAssignRole(roleId)
                          e.target.value = ''
                        }
                      }}
                    >
                      <option value="">+ Assign a role...</option>
                      {roles
                        .filter((r) => !moderatorRoles.some((mr) => mr.id === r.id))
                        .map((role) => (
                          <option key={role.id} value={role.id}>
                            {role.name}
                          </option>
                        ))}
                    </select>
                  </div>
                </div>

                <div className="moderator-actions">
                  <button
                    className={`btn ${selectedModerator.is_active ? 'btn-warning' : 'btn-success'}`}
                    onClick={() => handleToggleModerator(selectedModerator)}
                  >
                    {selectedModerator.is_active ? 'Deactivate' : 'Activate'}
                  </button>
                  <button
                    className="btn btn-danger"
                    onClick={() => handleDeleteModerator(selectedModerator)}
                  >
                    <FaTrash /> Delete Moderator
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* ROLES TAB */}
      {activeTab === 'roles' && (
        <div className="admin-section">
          <div className="section-header">
            <h2>Role Management</h2>
            <button
              className="btn btn-primary"
              onClick={() => {
                setShowCreateRole(!showCreateRole)
                setNewRoleName('')
                setNewRolePermissions({
                  ban: false,
                  unban: false,
                  warn: false,
                  kick: false,
                  mute: false,
                  unmute: false,
                  manageRoles: false,
                  viewActions: false
                })
              }}
            >
              <FaPlus /> Create Role
            </button>
          </div>

          {showCreateRole && (
            <div className="create-form">
              <h3>Create New Role</h3>
              <div className="form-group">
                <label>Role Name *</label>
                <input
                  type="text"
                  placeholder="e.g., Moderator, Admin, Helper"
                  value={newRoleName}
                  onChange={(e) => setNewRoleName(e.target.value)}
                />
              </div>

              <div className="permissions-section">
                <h4>Permissions</h4>
                <div className="permissions-grid">
                  {Object.entries(newRolePermissions).map(([perm, checked]) => (
                    <div key={perm} className="permission-checkbox">
                      <label>
                        <input
                          type="checkbox"
                          checked={checked}
                          onChange={(e) =>
                            setNewRolePermissions({ ...newRolePermissions, [perm]: e.target.checked })
                          }
                        />
                        <span>{perm.replace(/([A-Z])/g, ' $1').trim()}</span>
                      </label>
                    </div>
                  ))}
                </div>
              </div>

              <div className="form-actions">
                <button className="btn btn-primary" onClick={handleCreateRole}>
                  Create Role
                </button>
                <button className="btn btn-secondary" onClick={() => setShowCreateRole(false)}>
                  Cancel
                </button>
              </div>
            </div>
          )}

          <div className="roles-grid">
            {roles.length === 0 ? (
              <p className="empty-state">No roles created yet</p>
            ) : (
              roles.map((role) => (
                <div key={role.id} className="role-card">
                  {editingRole?.id === role.id ? (
                    <div className="role-edit-form">
                      <h4>Edit Role</h4>
                      <div className="form-group">
                        <label>Role Name</label>
                        <input
                          type="text"
                          value={editingRole.name}
                          onChange={(e) => setEditingRole({ ...editingRole, name: e.target.value })}
                        />
                      </div>

                      <div className="permissions-section">
                        <h5>Permissions</h5>
                        <div className="permissions-grid">
                          {Object.entries(editingRole.permissions).map(([perm, checked]) => (
                            <div key={perm} className="permission-checkbox">
                              <label>
                                <input
                                  type="checkbox"
                                  checked={checked}
                                  onChange={(e) =>
                                    setEditingRole({
                                      ...editingRole,
                                      permissions: { ...editingRole.permissions, [perm]: e.target.checked }
                                    })
                                  }
                                />
                                <span>{perm.replace(/([A-Z])/g, ' $1').trim()}</span>
                              </label>
                            </div>
                          ))}
                        </div>
                      </div>

                      <div className="form-actions">
                        <button className="btn btn-primary btn-sm" onClick={handleUpdateRole}>
                          Save Changes
                        </button>
                        <button className="btn btn-secondary btn-sm" onClick={() => setEditingRole(null)}>
                          Cancel
                        </button>
                      </div>
                    </div>
                  ) : (
                    <>
                      <h3>{role.name}</h3>
                      <div className="role-permissions">
                        {Object.entries(role.permissions)
                          .filter(([_, value]) => value)
                          .map(([key]) => (
                            <span key={key} className="permission-badge">
                              {key.replace(/([A-Z])/g, ' $1').trim()}
                            </span>
                          ))}
                        {Object.values(role.permissions).every(v => !v) && (
                          <span className="permission-badge empty">No permissions</span>
                        )}
                      </div>
                      <div className="role-card-actions">
                        <button className="btn btn-primary btn-sm" onClick={() => setEditingRole(role)}>
                          <FaEdit /> Edit
                        </button>
                        <button className="btn btn-danger btn-sm" onClick={() => handleDeleteRole(role.id)}>
                          <FaTrash /> Delete
                        </button>
                      </div>
                    </>
                  )}
                </div>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  )
}
