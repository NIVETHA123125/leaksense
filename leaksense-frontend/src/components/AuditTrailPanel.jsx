function AuditTrailPanel({ actions }) {
  return (
    <div className="panel">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 800 }}>Valve Safety Audit Trail</h2>
        <span
          style={{
            fontSize: '11px',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
            color: 'var(--text-faint)',
          }}
        >
          Cryptographic Log
        </span>
      </div>

      {actions.length === 0 && (
        <div style={{ padding: '24px 16px', textAlign: 'center', background: 'var(--panel-2)', borderRadius: '8px', border: '1px solid var(--border-soft)' }}>
          <p style={{ color: 'var(--text-muted)', margin: 0, fontSize: '14px' }}>No valve interlock actions recorded in current session.</p>
        </div>
      )}

      {actions.map((a) => {
        const isClosed = a.action === 'CLOSE'
        return (
          <div
            key={a.id}
            style={{
              background: 'var(--panel-2)',
              border: '1px solid var(--border-soft)',
              borderRadius: '8px',
              padding: '14px',
              marginBottom: '10px',
              transition: 'background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            }}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span
                  style={{
                    background: isClosed ? 'var(--err-bg)' : 'var(--ok-bg)',
                    color: isClosed ? 'var(--err)' : 'var(--ok)',
                    border: `1px solid ${isClosed ? 'var(--err)' : 'var(--ok)'}`,
                    borderRadius: '6px',
                    padding: '2px 8px',
                    fontFamily: 'monospace',
                    fontSize: '11px',
                    fontWeight: 700,
                  }}
                >
                  {isClosed ? 'ISOLATE' : 'RESTORE'}
                </span>
                <strong style={{ fontSize: '14px', fontWeight: 700 }}>{a.zone?.name || 'Zone'}</strong>
              </div>

              <span
                style={{
                  background: 'var(--panel)',
                  border: '1px solid var(--border)',
                  color: 'var(--text-muted)',
                  borderRadius: '9999px',
                  padding: '2px 10px',
                  fontSize: '11px',
                  fontWeight: 600,
                }}
              >
                {a.triggeredBy}
              </span>
            </div>

            <p style={{ color: 'var(--text-muted)', fontSize: '13px', margin: '8px 0 6px 0', lineHeight: 1.4 }}>
              {a.reason}
            </p>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '11px', color: 'var(--text-faint)', fontFamily: 'monospace' }}>
              <span>IP: {a.ipAddress || '127.0.0.1'}</span>
              <span>{new Date(a.performedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}</span>
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default AuditTrailPanel