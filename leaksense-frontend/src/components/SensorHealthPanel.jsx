function batteryColor(level) {
  if (level >= 50) return 'var(--ok)'
  if (level >= 20) return 'var(--warn)'
  return 'var(--err)'
}

function SensorHealthPanel({ nodes }) {
  return (
    <div className="panel">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 800 }}>Sensor Edge Nodes</h2>
        <span
          style={{
            fontSize: '11px',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
            color: 'var(--text-faint)',
          }}
        >
          {nodes.length} Registered
        </span>
      </div>

      {nodes.length === 0 && (
        <div style={{ padding: '24px 16px', textAlign: 'center', background: 'var(--panel-2)', borderRadius: '8px', border: '1px solid var(--border-soft)' }}>
          <p style={{ color: 'var(--text-muted)', margin: 0, fontSize: '14px' }}>No edge sensor nodes registered in system.</p>
        </div>
      )}

      {nodes.map((node) => {
        const online = node.status === 'ONLINE'
        return (
          <div
            key={node.id}
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
                <span style={{ fontFamily: 'monospace', fontWeight: 700, fontSize: '14px', color: 'var(--accent)' }}>
                  {node.nodeCode}
                </span>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>• {node.zone?.name}</span>
              </div>
              <span
                style={{
                  background: online ? 'var(--ok-bg)' : 'var(--panel-3)',
                  color: online ? 'var(--ok)' : 'var(--text-faint)',
                  border: `1px solid ${online ? 'var(--ok)' : 'var(--border)'}`,
                  borderRadius: '9999px',
                  padding: '2px 10px',
                  fontSize: '11px',
                  fontWeight: 700,
                  letterSpacing: '0.02em',
                }}
              >
                {node.status}
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', fontSize: '12px', color: 'var(--text-faint)', margin: '8px 0 10px 0' }}>
              <span>Telemetry: Wi-Fi 6 + LoRa (433MHz)</span>
              <span>Last ping: {node.lastSeen ? new Date(node.lastSeen).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Never'}</span>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <span style={{ fontSize: '11px', fontWeight: 600, color: 'var(--text-muted)', minWidth: '70px' }}>
                BATTERY {node.batteryLevel}%
              </span>
              <div style={{ flex: 1, background: 'var(--border)', borderRadius: '9999px', height: '6px', overflow: 'hidden' }}>
                <div
                  style={{
                    width: `${node.batteryLevel}%`,
                    background: batteryColor(node.batteryLevel),
                    height: '100%',
                    borderRadius: '9999px',
                    transition: 'width 0.3s ease',
                  }}
                />
              </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default SensorHealthPanel