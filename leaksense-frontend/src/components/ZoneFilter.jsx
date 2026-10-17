import { useData } from '../context/DataContext'

function ZoneFilter() {
  const { allZones, zoneFilter, setZoneFilter } = useData()

  return (
    <select
      className="zone-select"
      value={zoneFilter}
      onChange={(e) => setZoneFilter(e.target.value)}
      aria-label="Filter by zone"
    >
      <option value="all">All zones</option>
      {allZones.map((z) => (
        <option key={z.id} value={String(z.id)}>
          {z.name}
        </option>
      ))}
    </select>
  )
}

export default ZoneFilter