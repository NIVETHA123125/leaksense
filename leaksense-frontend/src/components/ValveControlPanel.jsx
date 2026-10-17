import { UnlockIcon, LockIcon, ClockIcon, AlertIcon } from './icons/AppIcons'

const getStateMeta = (valveState) => {
  switch (valveState) {
    case 'CLOSED':
      return {
        icon: <LockIcon size={12} color="var(--err)" />,
        text: 'VALVE ISOLATED / CLOSED',
        color: 'var(--err)',
        bg: 'var(--err-bg)',
      }
    case 'CLOSING':
      return {
        icon: <ClockIcon size={12} color="var(--warn)" />,
        text: 'ISOLATING... WAITING CONFIRMATION',
        color: 'var(--warn)',
        bg: 'var(--warn-bg)',
      }
    case 'OPENING':
      return {
        icon: <ClockIcon size={12} color="var(--warn)" />,
        text: 'OPENING... WAITING CONFIRMATION',
        color: 'var(--warn)',
        bg: 'var(--warn-bg)',
      }
    case 'PARTIAL':
      return {
        icon: <AlertIcon size={12} color="var(--warn)" />,
        text: 'PARTIAL STATE / DISCREPANCY',
        color: 'var(--warn)',
        bg: 'var(--warn-bg)',
      }
    case 'FAULT':
      return {
        icon: <AlertIcon size={12} color="var(--err)" />,
        text: 'HARDWARE FAULT: CHECK INTERLOCK',
        color: 'var(--err)',
        bg: 'var(--err-bg)',
      }
    case 'OPEN':
    default:
      return {
        icon: <UnlockIcon size={12} color="var(--ok)" />,
        text: 'VALVE ONLINE / OPEN',
        color: 'var(--ok)',
        bg: 'var(--ok-bg)',
      }
  }
}

function ValveControlPanel({ zones, onValve, canControl }) {
  return (
    <div className="panel">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <h2 style={{ margin: 0, fontSize: '18px', fontWeight: 800 }}>Zone Isolation Valves</h2>
        <span
          style={{
            fontSize: '11px',
            fontWeight: 700,
            textTransform: 'uppercase',
            letterSpacing: '0.05em',
            color: 'var(--text-faint)',
          }}
        >
          Dual-Lock Solenoid
        </span>
      </div>

      {!canControl && (
        <div style={{ padding: '10px 14px', background: 'var(--warn-bg)', border: '1px solid var(--warn)', borderRadius: '8px', color: 'var(--warn)', fontSize: '12px', fontWeight: 600, marginBottom: '16px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          <LockIcon size={14} color="var(--warn)" />
          <span>Operator mode: Valve actuators require Administrator privileges to toggle.</span>
        </div>
      )}

      {zones.length === 0 && (
        <div style={{ padding: '24px 16px', textAlign: 'center', background: 'var(--panel-2)', borderRadius: '8px', border: '1px solid var(--border-soft)' }}>
          <p style={{ color: 'var(--text-muted)', margin: 0, fontSize: '14px' }}>No pipeline zones registered.</p>
        </div>
      )}

      {zones.map((zone) => {
        const state = getStateMeta(zone.valveState)
        const current = zone.valveState || 'OPEN'
        const moving = current === 'CLOSING' || current === 'OPENING'
        const canClose = ['OPEN', 'PARTIAL', 'FAULT'].includes(current)
        const canOpen = ['CLOSED', 'PARTIAL', 'FAULT'].includes(current)

        return (
          <div
            key={zone.id}
            style={{
              background: 'var(--panel-2)',
              border: '1px solid var(--border-soft)',
              borderRadius: '8px',
              padding: '16px',
              marginBottom: '12px',
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: '12px',
              flexWrap: 'wrap',
              transition: 'background-color 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            }}
          >
            <div>
              <strong style={{ fontSize: '15px', fontWeight: 700, color: 'var(--text)' }}>{zone.name}</strong>
              <div style={{ marginTop: '6px' }}>
                <span
                  style={{
                    background: state.bg,
                    color: state.color,
                    border: `1px solid ${state.color}`,
                    borderRadius: '9999px',
                    padding: '3px 10px',
                    fontSize: '11px',
                    fontWeight: 700,
                    letterSpacing: '0.02em',
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}
                >
                  {state.icon}
                  <span>{state.text}</span>
                </span>
              </div>
            </div>

            <div style={{ display: 'flex', gap: '8px' }}>
              {canClose && (
                <button
                  type="button"
                  disabled={!canControl || moving}
                  onClick={() => onValve(zone, 'CLOSE')}
                  style={{
                    background: 'transparent',
                    color: 'var(--err)',
                    border: '1.5px solid var(--err)',
                    borderRadius: '8px',
                    padding: '8px 16px',
                    fontWeight: 600,
                    fontSize: '13px',
                    cursor: !canControl || moving ? 'not-allowed' : 'pointer',
                    opacity: !canControl || moving ? 0.5 : 1,
                    transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                  }}
                  onMouseEnter={(e) => { if (canControl && !moving) e.currentTarget.style.background = 'rgba(239, 68, 68, 0.08)' }}
                  onMouseLeave={(e) => { if (canControl && !moving) e.currentTarget.style.background = 'transparent' }}
                >
                  Emergency Close
                </button>
              )}

              {canOpen && (
                <button
                  type="button"
                  className="btn-open-valve"
                  disabled={!canControl || moving}
                  onClick={() => onValve(zone, 'OPEN')}
                  style={{
                    background: 'transparent',
                    color: '#16a34a',
                    border: '1.5px solid #16a34a',
                    borderRadius: '8px',
                    padding: '8px 16px',
                    fontWeight: 600,
                    fontSize: '13px',
                    cursor: !canControl || moving ? 'not-allowed' : 'pointer',
                    opacity: !canControl || moving ? 0.5 : 1,
                    transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                  }}
                  onMouseEnter={(e) => { if (canControl && !moving) e.currentTarget.style.background = 'rgba(22, 163, 74, 0.08)' }}
                  onMouseLeave={(e) => { if (canControl && !moving) e.currentTarget.style.background = 'transparent' }}
                >
                  Open Valve
                </button>
              )}
            </div>
          </div>
        )
      })}
    </div>
  )
}

export default ValveControlPanel