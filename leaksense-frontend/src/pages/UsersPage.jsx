import { useEffect, useState } from 'react'
import PageHeader from '../components/PageHeader'
import ConfirmModal from '../components/ConfirmModal'
import { useData } from '../context/DataContext'
import { getUsers, createUser, resetUserPassword, deleteUser } from '../api/leaksenseApi'
import { PlusIcon, UsersIcon, KeyIcon, TrashIcon } from '../components/icons/AppIcons'

const emptyForm = { username: '', password: '', fullName: '', role: 'OPERATOR' }

const errorText = (err) =>
  err.response?.data?.message || 'Something went wrong. Please try again.'

function UsersPage() {
  const { user } = useData()
  const [users, setUsers] = useState([])
  const [form, setForm] = useState(emptyForm)
  const [message, setMessage] = useState(null)
  const [toDelete, setToDelete] = useState(null)
  const [resetFor, setResetFor] = useState(null)
  const [newPassword, setNewPassword] = useState('')
  const [resetError, setResetError] = useState('')

  const load = async () => {
    try {
      const res = await getUsers()
      setUsers(res.data)
    } catch (err) {
      setMessage({ type: 'error', text: errorText(err) })
    }
  }

  useEffect(() => {
    load()
  }, [])

  const setField = (field) => (e) => setForm({ ...form, [field]: e.target.value })

  const handleCreate = async (e) => {
    e.preventDefault()
    try {
      await createUser(form)
      setForm(emptyForm)
      setMessage({ type: 'ok', text: 'User created successfully.' })
      load()
    } catch (err) {
      setMessage({ type: 'error', text: errorText(err) })
    }
  }

  const closeReset = () => {
    setResetFor(null)
    setNewPassword('')
    setResetError('')
  }

  const handleReset = async () => {
    try {
      await resetUserPassword(resetFor.id, newPassword)
      setMessage({ type: 'ok', text: `Password updated for ${resetFor.username}.` })
      closeReset()
    } catch (err) {
      setResetError(errorText(err))
    }
  }

  const handleDelete = async () => {
    try {
      await deleteUser(toDelete.id)
      setMessage({ type: 'ok', text: `User ${toDelete.username} deleted.` })
      load()
    } catch (err) {
      setMessage({ type: 'error', text: errorText(err) })
    }
    setToDelete(null)
  }

  return (
    <>
      <PageHeader
        eyebrow="ADMINISTRATION"
        title="User Management"
        subtitle="Add users, reset passwords and remove access"
      />

      {message && <div className={`notice ${message.type}`}>{message.text}</div>}

      <div className="panel">
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <PlusIcon size={18} />
          <span>Add user</span>
        </h2>
        <form onSubmit={handleCreate}>
          <div className="form-grid">
            <div>
              <label className="login-label">Username *</label>
              <input
                className="login-input"
                value={form.username}
                onChange={setField('username')}
              />
            </div>
            <div>
              <label className="login-label">Full name</label>
              <input
                className="login-input"
                value={form.fullName}
                onChange={setField('fullName')}
              />
            </div>
            <div>
              <label className="login-label">Password *</label>
              <input
                className="login-input"
                type="password"
                value={form.password}
                onChange={setField('password')}
              />
            </div>
            <div>
              <label className="login-label">Role *</label>
              <select
                className="login-input"
                value={form.role}
                onChange={setField('role')}
              >
                <option value="OPERATOR">Operator</option>
                <option value="ADMIN">Admin</option>
              </select>
            </div>
          </div>
          <p className="field-hint">
            Password: at least 8 characters with a letter and a number.
          </p>
          <button
            className="btn btn-primary"
            type="submit"
            disabled={!form.username || !form.password}
          >
            Create user
          </button>
        </form>
      </div>

      <div className="panel" style={{ marginTop: '16px' }}>
        <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <UsersIcon size={18} />
          <span>Users ({users.length})</span>
        </h2>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>USERNAME</th>
                <th>FULL NAME</th>
                <th>ROLE</th>
                <th>ACTIONS</th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => {
                const isMe = u.username === user.username
                return (
                  <tr key={u.id}>
                    <td>
                      {u.username}
                      {isMe && <span style={{ color: 'var(--text-faint)' }}> (you)</span>}
                    </td>
                    <td>{u.fullName}</td>
                    <td>
                      <span
                        style={{
                          background: u.role === 'ADMIN' ? '#020617' : 'var(--accent)',
                          color: '#ffffff',
                          border: `1px solid ${u.role === 'ADMIN' ? '#334155' : 'var(--accent)'}`,
                          borderRadius: '9999px',
                          padding: '2px 10px',
                          fontSize: '11px',
                          fontWeight: 700,
                          letterSpacing: '0.04em',
                          display: 'inline-block',
                        }}
                      >
                        {u.role}
                      </span>
                    </td>
                    <td>
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <button
                          className="btn btn-ghost btn-small"
                          onClick={() => setResetFor(u)}
                          style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                        >
                          <KeyIcon size={14} />
                          <span>Reset password</span>
                        </button>
                        <button
                          className="btn btn-danger btn-small"
                          disabled={isMe}
                          onClick={() => setToDelete(u)}
                          style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
                        >
                          <TrashIcon size={14} />
                          <span>Delete</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      </div>

      {resetFor && (
        <div className="modal-overlay" onClick={closeReset}>
          <div
            className="modal-card"
            style={{ borderTop: '4px solid #14b8a6' }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'center', margin: '8px 0 16px' }}>
              <KeyIcon size={44} color="#14b8a6" />
            </div>
            <h2 style={{ margin: '12px 0 8px', fontSize: '22px' }}>Reset password</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '16px' }}>
              Set a new password for <strong>{resetFor.username}</strong>.
            </p>
            <input
              className="login-input"
              type="password"
              placeholder="New password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoFocus
            />
            {resetError && <p className="login-error">{resetError}</p>}
            <div style={{ display: 'flex', gap: '12px' }}>
              <button className="btn btn-ghost" style={{ flex: 1 }} onClick={closeReset}>
                Cancel
              </button>
              <button
                className="btn btn-primary"
                style={{ flex: 1 }}
                disabled={!newPassword}
                onClick={handleReset}
              >
                Save
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmModal
        open={toDelete !== null}
        tone="danger"
        icon={<TrashIcon size={44} color="var(--err)" />}
        title="Delete user?"
        message={`This permanently removes ${toDelete?.username} and their access.`}
        confirmText="Delete"
        onConfirm={handleDelete}
        onCancel={() => setToDelete(null)}
      />
    </>
  )
}

export default UsersPage