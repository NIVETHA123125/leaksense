import PageHeader from '../components/PageHeader'
import SensorHealthPanel from '../components/SensorHealthPanel'
import { useData } from '../context/DataContext'

function SensorsPage() {
  const { nodes } = useData()

  return (
    <>
      <PageHeader
        eyebrow="SENSORS"
        title="Sensor Health"
        subtitle="Node status, battery level and last heartbeat"
      />
      <SensorHealthPanel nodes={nodes} />
    </>
  )
}

export default SensorsPage