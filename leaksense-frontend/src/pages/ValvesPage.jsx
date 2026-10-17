import { useState } from 'react'
import PageHeader from '../components/PageHeader'
import ValveControlPanel from '../components/ValveControlPanel'
import AuditTrailPanel from '../components/AuditTrailPanel'
import ConfirmModal from '../components/ConfirmModal'
import { useData } from '../context/DataContext'

function ValvesPage() {
  const { user, zones, actions, changeValve } = useData()
  const [pending, setPending] = useState(null)
  const [message, setMessage] = useState(null)

  const confirmValve = async () => {
    const { zone, action } = pending
    setPending(null)

    try {
      await changeValve(zone.id, action)
      setMessage({
        type: 'ok',
        text: `${action === 'CLOSE' ? 'Close' : 'Open'} command sent to ${zone.name}. Waiting for the device to confirm.`,
      })
    } catch (err) {
      setMessage({
        type: 'error',
        text: err.response?.data?.message || 'Could not send the valve command.',
      })
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="VALVES"
        title="Valve Control"
        subtitle="Open or close the solenoid valve of each zone"
      />

      {message && <div className={`notice ${message.type}`}>{message.text}</div>}

      <div className="grid-2" style={{ marginTop: 0 }}>
        <ValveControlPanel
          zones={zones}
          onValve={(zone, action) => setPending({ zone, action })}
          canControl={user.role === 'ADMIN'}
        />
        <AuditTrailPanel actions={actions} />
      </div>

      <ConfirmModal
        open={pending !== null}
        tone={pending?.action === 'CLOSE' ? 'danger' : 'safe'}
        title={pending?.action === 'CLOSE' ? 'Emergency Close Valve?' : 'Open Valve?'}
        message={
          pending?.action === 'CLOSE'
            ? `This will shut off gas flow in ${pending?.zone.name}.`
            : `This will restore gas flow in ${pending?.zone.name}. Make sure the zone is safe.`
        }
        confirmText={pending?.action === 'CLOSE' ? 'Close Valve' : 'Open Valve'}
        onConfirm={confirmValve}
        onCancel={() => setPending(null)}
      />
    </>
  )
}

export default ValvesPage