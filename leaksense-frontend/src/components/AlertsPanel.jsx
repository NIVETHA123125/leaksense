import { CheckIcon, LocationIcon, SensorIcon, UserIcon } from './icons/AppIcons'

const severityBadgeStyles = {
  CRITICAL: { bg: 'var(--err-bg)', color: 'var(--err)', border: '1px solid var(--err)' },
  HIGH: { bg: 'rgba(249, 115, 22, 0.15)', color: '#ea580c', border: '1px solid rgba(249, 115, 22, 0.3)' },
  MEDIUM: { bg: 'var(--warn-bg)', color: 'var(--warn)', border: '1px solid var(--warn)' },
  LOW: { bg: 'var(--ok-bg)', color: 'var(--ok)', border: '1px solid var(--ok)' },
}

function AlertsPanel({ alerts, onAcknowledge, onResolve }) {
  return (
    <div className="panel">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 800 }}>Live Incident Alerts</h2>
        <span
          style={{
            fontSize: '11px',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
            color: 'var(--text-faint)',
          }}
        >
          {alerts.length} Active
        </span>
      </div>

      {alerts.length === 0 && (
        <div style={{ padding: '24px 16px', textAlign: 'center', background: 'var(--panel-2)', borderRadius: '8px', border: '1px solid var(--border-soft)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
          <CheckIcon size={16} color="var(--ok)" />
          <span style={{ color: 'var(--ok)', fontWeight: 600, fontSize: '14px' }}>All monitored pipeline zones operational. Zero active hazards.</span>
        </div>
      )}

      {alerts.map((alert) => {
        const badge = severityBadgeStyles[alert.severity] || { bg: 'var(--panel-3)', color: 'var(--text-muted)', border: '1px solid var(--border)' }
        return (
          <div
            key={alert.id}
            style={{
              background: 'var(--panel-2)',
              border: '1px solid var(--border-soft)',
              borderRadius: '8px',
              padding: '16px',
              marginBottom: '12px',
              transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '8px' }}>
              <strong style={{ fontSize: '14px', fontWeight: 700 }}>{alert.title}</strong>
              <span
                style={{
                  background: badge.bg,
                  color: badge.color,
                  border: badge.border,
                  borderRadius: '9999px',
                  padding: '2px 10px',
                  fontSize: '11px',
                  fontWeight: 700,
                  letterSpacing: '0.04em',
                }}
              >
                {alert.severity}
              </span>
            </div>

            <p style={{ color: 'var(--text-muted)', fontSize: '13px', margin: '8px 0', lineHeight: 1.5 }}>
              {alert.message}
            </p>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '8px', marginTop: '12px', paddingTop: '10px', borderTop: '1px solid var(--border-soft)' }}>
              <span style={{ color: 'var(--text-faint)', fontSize: '12px', display: 'inline-flex', alignItems: 'center', gap: '6px', flexWrap: 'wrap' }}>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}><LocationIcon size={13} /> {alert.zone?.name}</span>
                <span>·</span>
                <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}><SensorIcon size={13} /> {alert.node?.nodeCode}</span>
                {alert.sensor && <span>· {alert.sensor}</span>}
                {alert.acknowledgedBy && (
                  <>
                    <span>·</span>
                    <span style={{ display: 'inline-flex', alignItems: 'center', gap: '4px' }}><UserIcon size={13} /> {alert.acknowledgedBy}</span>
                  </>
                )}
              </span>

              <div style={{ display: 'flex', gap: '8px' }}>
                {alert.status === 'OPEN' && (
                  <button
                    onClick={() => onAcknowledge(alert.id)}
                    style={{
                      background: 'transparent',
                      color: 'var(--accent)',
                      border: '1.5px solid var(--accent)',
                      borderRadius: '8px',
                      padding: '6px 12px',
                      fontSize: '12px',
                      fontWeight: 600,
                      cursor: 'pointer',
                      transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                    }}
                    onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(79, 70, 229, 0.08)' }}
                    onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
                  >
                    Acknowledge
                  </button>
                )}
                <button
                  onClick={() => onResolve(alert.id)}
                  style={{
                    background: 'transparent',
                    color: 'var(--ok)',
                    border: '1.5px solid var(--ok)',
                    borderRadius: '8px',
                    padding: '6px 14px',
                    fontSize: '12px',
                    fontWeight: 600,
                    cursor: 'pointer',
                    transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                  }}
                  onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(34, 197, 94, 0.08)' }}
                  onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
                >
                  Resolve
                </button>
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default AlertsPanel