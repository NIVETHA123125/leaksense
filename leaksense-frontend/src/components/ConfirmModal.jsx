import { LockIcon, UnlockIcon } from './icons/AppIcons'

function ConfirmModal({ open, tone, icon, title, message, confirmText, onConfirm, onCancel }) {
  if (!open) return null

  const danger = tone === 'danger'
  const color = danger ? 'var(--err)' : 'var(--ok)'

  return (
    <div className="modal-overlay" onClick={onCancel}>
      <div
        className="modal-card"
        style={{ borderTop: `4px solid ${color}` }}
        onClick={(e) => e.stopPropagation()}
      >
        <div style={{ display: 'flex', justifyContent: 'center', margin: '8px 0 16px' }}>
          {icon || (danger ? <LockIcon size={44} color={color} /> : <UnlockIcon size={44} color={color} />)}
        </div>

        <h2 style={{ margin: '12px 0 8px', fontSize: '20px', fontWeight: 800 }}>{title}</h2>

        <p style={{ color: 'var(--text-muted)', fontSize: '14px', marginBottom: '24px', lineHeight: 1.5 }}>
          {message}
        </p>

        <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
          <button
            onClick={onCancel}
            style={{
              flex: 1,
              background: 'transparent',
              color: 'var(--text)',
              border: '1px solid var(--border-input)',
              borderRadius: '8px',
              padding: '10px',
              fontWeight: '600',
              cursor: 'pointer',
              transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            }}
          >
            Cancel
          </button>

          <button
            onClick={onConfirm}
            style={{
              flex: 1,
              background: 'transparent',
              color: color,
              border: `1.5px solid ${color}`,
              borderRadius: '8px',
              padding: '10px',
              fontWeight: '700',
              cursor: 'pointer',
              transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            }}
            onMouseEnter={(e) => { e.currentTarget.style.background = danger ? 'rgba(239, 68, 68, 0.08)' : 'rgba(34, 197, 94, 0.08)' }}
            onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
          >
            {confirmText}
          </button>
        </div>
      </div>
    </div>
  )
}

export default ConfirmModal