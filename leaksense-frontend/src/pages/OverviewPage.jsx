import PageHeader from '../components/PageHeader'
import SummaryCards from '../components/SummaryCards'
import AlertsPanel from '../components/AlertsPanel'
import SensorHealthPanel from '../components/SensorHealthPanel'
import AuditTrailPanel from '../components/AuditTrailPanel'
import GasChart from '../components/GasChart'
import { useData } from '../context/DataContext'

function OverviewPage() {
  const { summary, alerts, nodes, actions, readings, acknowledge, resolve } = useData()

  return (
    <>
      <PageHeader
        eyebrow="OVERVIEW"
        title="Gas Leak Monitoring Dashboard"
        subtitle="Real-time pipeline safety, alerts and valve activity"
      />
      {summary && <SummaryCards summary={summary} />}

      {/* Full-width Live Gas Reading Chart spanning the length of the page */}
      <div style={{ marginTop: '24px', width: '100%' }}>
        <GasChart readings={readings} height={380} />
      </div>

      {/* Three parallel panels side-by-side */}
      <div className="grid-3">
        <AlertsPanel alerts={alerts} onAcknowledge={acknowledge} onResolve={resolve} />
        <SensorHealthPanel nodes={nodes} />
        <AuditTrailPanel actions={actions} />
      </div>
    </>
  )
}

export default OverviewPage