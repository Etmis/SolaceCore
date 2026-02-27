import { useState, useEffect } from 'react'
import { useAuth } from '../contexts/AuthContext'
import { getRoles, createRole, updateRole, deleteRole, getModerators, getModeratorRoles, assignRole, removeRole } from '../api'
import type { Role } from '../types'
import { FaPlus, FaEdit, FaTrash, FaUserShield } from 'react-icons/fa'

export default function RoleManagement() {
  const { hasPermission } = useAuth()
  const [roles, setRoles] = useState<Role[]>([])
  const [moderators, setModerators] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [showCreateRole, setShowCreateRole] = useState(false)
  const [editingRole, setEditingRole] = useState<Role | null>(null)
  const [selectedModerator, setSelectedModerator] = useState<any | null>(null)
  const [moderatorRoles, setModeratorRoles] = useState<Role[]>([])

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
    } catch (err: any) {
      setError(err.message || 'Failed to update role')
    }
  }

  const handleDeleteRole = async (id: number) => {
    if (!confirm('Are you sure you want to delete this role?')) return

    try {
      await deleteRole(id)
      loadData()
    } catch (err: any) {
      setError(err.message || 'Failed to delete role')
    }
  }

  const handleAssignRole = async (modId: number, roleId: number) => {
    try {
      await assignRole(modId, roleId)
      if (selectedModerator?.id === modId) {
        loadModeratorRoles(modId)
      }
    } catch (err: any) {
      setError(err.message || 'Failed to assign role')
    }
  }

  const handleRemoveRole = async (modId: number, roleId: number) => {
    try {
      await removeRole(modId, roleId)
      if (selectedModerator?.id === modId) {
        loadModeratorRoles(modId)
      }
    } catch (err: any) {
      setError(err.message || 'Failed to remove role')
    }
  }

  if (!hasPermission('manageRoles')) {
    return (
      <div className="role-management">
        <p className="error">You don't have permission to manage roles.</p>
      </div>
    )
  }

  if (loading) {
    return <div className="role-management">Loading...</div>
  }

  return (
    <div className="role-management">
      <h2><FaUserShield /> Role Management</h2>

      {error && <div className="error-message">{error}</div>}

      <div className="role-sections">
        {/* Roles Section */}
        <section className="roles-section">
          <div className="section-header">
            <h3>Roles</h3>
            <button className="btn btn-primary btn-sm" onClick={() => setShowCreateRole(true)}>
              <FaPlus /> Create Role
            </button>
          </div>

          {showCreateRole && (
            <div className="role-form">
              <h4>Create New Role</h4>
              <input
                type="text"
                placeholder="Role name"
                value={newRoleName}
                onChange={(e) => setNewRoleName(e.target.value)}
              />
              <div className="permissions-grid">
                {Object.keys(newRolePermissions).map((perm) => (
                  <label key={perm}>
                    <input
                      type="checkbox"
                      checked={newRolePermissions[perm as keyof typeof newRolePermissions]}
                      onChange={(e) =>
                        setNewRolePermissions({ ...newRolePermissions, [perm]: e.target.checked })
                      }
                    />
                    {perm}
                  </label>
                ))}
              </div>
              <div className="form-actions">
                <button className="btn btn-primary" onClick={handleCreateRole}>Create</button>
                <button className="btn btn-secondary" onClick={() => setShowCreateRole(false)}>Cancel</button>
              </div>
            </div>
          )}

          <div className="roles-list">
            {roles.map((role) => (
              <div key={role.id} className="role-item">
                {editingRole?.id === role.id ? (
                  <div className="role-form">
                    <input
                      type="text"
                      value={editingRole.name}
                      onChange={(e) => setEditingRole({ ...editingRole, name: e.target.value })}
                    />
                    <div className="permissions-grid">
                      {Object.keys(editingRole.permissions).map((perm) => (
                        <label key={perm}>
                          <input
                            type="checkbox"
                            checked={editingRole.permissions[perm]}
                            onChange={(e) =>
                              setEditingRole({
                                ...editingRole,
                                permissions: { ...editingRole.permissions, [perm]: e.target.checked }
                              })
                            }
                          />
                          {perm}
                        </label>
                      ))}
                    </div>
                    <div className="form-actions">
                      <button className="btn btn-primary btn-sm" onClick={handleUpdateRole}>Save</button>
                      <button className="btn btn-secondary btn-sm" onClick={() => setEditingRole(null)}>Cancel</button>
                    </div>
                  </div>
                ) : (
                  <>
                    <div className="role-info">
                      <h4>{role.name}</h4>
                      <div className="role-permissions">
                        {Object.entries(role.permissions)
                          .filter(([_, value]) => value)
                          .map(([key]) => (
                            <span key={key} className="permission-badge">{key}</span>
                          ))}
                      </div>
                    </div>
                    <div className="role-actions">
                      <button className="btn btn-sm" onClick={() => setEditingRole(role)}>
                        <FaEdit />
                      </button>
                      <button className="btn btn-danger btn-sm" onClick={() => handleDeleteRole(role.id)}>
                        <FaTrash />
                      </button>
                    </div>
                  </>
                )}
              </div>
            ))}
          </div>
        </section>

        {/* Moderators Section */}
        <section className="moderators-section">
          <h3>Moderators</h3>
          <div className="moderators-list">
            {moderators.map((mod) => (
              <div
                key={mod.id}
                className={`moderator-item ${selectedModerator?.id === mod.id ? 'active' : ''}`}
                onClick={() => {
                  setSelectedModerator(mod)
                  loadModeratorRoles(mod.id)
                }}
              >
                <div className="moderator-info">
                  <strong>{mod.username}</strong>
                  <span className={`status ${mod.is_active ? 'active' : 'inactive'}`}>
                    {mod.is_active ? 'Active' : 'Inactive'}
                  </span>
                </div>
              </div>
            ))}
          </div>

          {selectedModerator && (
            <div className="moderator-roles">
              <h4>Roles for {selectedModerator.username}</h4>
              <div className="assigned-roles">
                {moderatorRoles.map((role) => (
                  <div key={role.id} className="assigned-role">
                    <span>{role.name}</span>
                    <button
                      className="btn btn-danger btn-sm"
                      onClick={() => handleRemoveRole(selectedModerator.id, role.id)}
                    >
                      <FaTrash />
                    </button>
                  </div>
                ))}
              </div>
              <div className="assign-role">
                <select
                  onChange={(e) => {
                    const roleId = parseInt(e.target.value)
                    if (roleId) {
                      handleAssignRole(selectedModerator.id, roleId)
                      e.target.value = ''
                    }
                  }}
                >
                  <option value="">Assign role...</option>
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
          )}
        </section>
      </div>
    </div>
  )
}
