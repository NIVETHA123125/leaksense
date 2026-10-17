import { LocationIcon, SensorIcon, CheckIcon, MinusIcon, AlertIcon, BoltIcon } from './icons/AppIcons'

function SummaryCards({ summary }) {
  const cards = [
    { label: 'MONITORED ZONES', value: summary.totalZones, icon: <LocationIcon size={16} />, color: 'var(--primary)' },
    { label: 'ACTIVE NODES', value: summary.totalNodes, icon: <SensorIcon size={16} />, color: 'var(--primary-hover)' },
    { label: 'ONLINE STATUS', value: summary.onlineNodes, icon: <CheckIcon size={16} />, color: '#16a34a' },
    { label: 'OFFLINE NODES', value: summary.offlineNodes, icon: <MinusIcon size={16} />, color: 'var(--text-faint)' },
    { label: 'ACTIVE ALERTS', value: summary.activeAlerts, icon: <AlertIcon size={16} />, color: '#d97706' },
    { label: 'CRITICAL HAZARDS', value: summary.criticalAlerts, icon: <BoltIcon size={16} />, color: '#dc2626' },
  ]

  return (
    <div className="summary-grid">
      {cards.map((card) => (
        <div
          key={card.label}
          className="summary-card"
          style={{
            borderLeft: `3px solid ${card.color}`,
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px' }}>
            <span className="summary-label">{card.label}</span>
            <span style={{ color: card.color, display: 'inline-flex' }}>{card.icon}</span>
          </div>
          <div className="summary-value" style={{ color: card.color }}>
            {card.value}
          </div>
        </div>
      ))}
    </div>
  )
}

export default SummaryCards