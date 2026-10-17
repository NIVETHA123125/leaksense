import { useState } from 'react'
import { login } from '../api/leaksenseApi'
import { BrandIcon } from './icons/AppIcons'
import LoginScene from './LoginScene'

function Login({ onLogin, onBack }) {
  const [tab, setTab] = useState('OPERATOR')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const switchTab = (next) => {
    setTab(next)
    setPassword('')
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const res = await login(username, password)

      if (res.data.role !== tab) {
        setError(
          tab === 'ADMIN'
            ? 'This account is not an administrator. Use Operator Login.'
            : 'This is an administrator account. Use Admin Login.'
        )
        return
      }

      onLogin(res.data)
    } catch (err) {
      if (err.response?.status === 401) {
        setError('Invalid username or password')
      } else {
        setError('Cannot reach the backend. Is Spring Boot running?')
      }
    } finally {
      setLoading(false)
    }
  }

  const disabled = loading || !username || !password

  return (
    <div className="login-page">
      <section className="login-brand">
        {onBack && (
          <button
            onClick={onBack}
            style={{
              background: 'rgba(255, 255, 255, 0.08)',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              color: '#94a3b8',
              borderRadius: '8px',
              padding: '8px 14px',
              fontSize: '12px',
              fontWeight: 600,
              cursor: 'pointer',
              marginBottom: '28px',
              width: 'fit-content',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '6px',
              transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            }}
          >
            ← Return to Enterprise Landing Page
          </button>
        )}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '16px' }}>
          <BrandIcon size={38} />
          <div>
            <div style={{ fontSize: '22px', fontWeight: 800, color: '#ffffff', letterSpacing: '-0.02em' }}>
              LeakSense <span style={{ color: 'var(--primary)' }}>Enterprise</span>
            </div>
            <div style={{ fontSize: '11px', color: '#94a3b8', letterSpacing: '0.06em', textTransform: 'uppercase', fontWeight: 700 }}>
              Industrial Safety Console
            </div>
          </div>
        </div>
        <p className="login-tagline">
          Continuous telemetry ingestion, automated sub-second valve isolation, and cryptographically verified audit logs.
        </p>

        <LoginScene />

        <p className="login-steps" style={{ color: 'var(--primary)', letterSpacing: '2.5px', fontWeight: 800, fontSize: '12px' }}>
          DETECT • ISOLATE • AUDIT
        </p>
      </section>

      <section className="login-form-side">
        <div className="login-mobile-logo">
          {onBack && (
            <button
              onClick={onBack}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--accent)',
                fontSize: '12px',
                fontWeight: 600,
                cursor: 'pointer',
                marginBottom: '10px',
                display: 'block',
              }}
            >
              ← Back to Website
            </button>
          )}
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <BrandIcon size={28} />
            <span style={{ fontWeight: 800, color: 'var(--text)' }}>LeakSense</span>
          </div>
        </div>

        <div className="login-card">
          <div className="login-tabs">
            <button
              type="button"
              className={tab === 'OPERATOR' ? 'login-tab active' : 'login-tab'}
              onClick={() => switchTab('OPERATOR')}
            >
              OPERATOR LOGIN
            </button>
            <button
              type="button"
              className={tab === 'ADMIN' ? 'login-tab active' : 'login-tab'}
              onClick={() => switchTab('ADMIN')}
            >
              ADMIN LOGIN <span className="badge-admin">ADMIN</span>
            </button>
          </div>

          <form className="login-body" onSubmit={handleSubmit}>
            <h2>LeakSense Console</h2>
            <p className="login-sub">
              {tab === 'ADMIN' ? 'Administrator sign in' : 'Sign in to continue'}
            </p>

            <label className="login-label">Username *</label>
            <input
              className="login-input"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              autoFocus
            />

            <label className="login-label">Password *</label>
            <input
              className="login-input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            {error && <p className="login-error">{error}</p>}

            <button className="login-submit" type="submit" disabled={disabled}>
              {loading ? 'SIGNING IN...' : 'SIGN IN'}
            </button>
          </form>
        </div>
      </section>
    </div>
  )
}

export default Login