import { useState } from 'react'
import PageHeader from '../components/PageHeader'
import AlertsPanel from '../components/AlertsPanel'
import AlertHistoryTable from '../components/AlertHistoryTable'
import { AlertIcon, ArchiveIcon } from '../components/icons/AppIcons'
import { useData } from '../context/DataContext'

export default function AlertsPage() {
  const { alerts, acknowledge, resolve } = useData()
  const [activeTab, setActiveTab] = useState('active')

  return (
    <>
      <PageHeader
        eyebrow="SAFETY & ALERTS"
        title="Incident & Alert Management"
        subtitle="Real-time incident response and auditable historical alert logs"
      />

      {/* Tabs */}
      <div
        style={{
          display: 'flex',
          gap: '12px',
          borderBottom: '1px solid var(--border)',
          marginBottom: '20px',
        }}
      >
        <button
          onClick={() => setActiveTab('active')}
          style={{
            background: 'transparent',
            border: 'none',
            borderBottom: activeTab === 'active' ? '2px solid var(--accent)' : '2px solid transparent',
            color: activeTab === 'active' ? 'var(--accent)' : 'var(--text-muted)',
            fontWeight: activeTab === 'active' ? '700' : '500',
            fontSize: '15px',
            padding: '10px 16px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <AlertIcon size={16} />
          <span>Live Alerts</span>
          {alerts.length > 0 && (
            <span
              style={{
                background: 'var(--err)',
                color: '#fff',
                borderRadius: '9999px',
                padding: '1px 7px',
                fontSize: '11px',
                fontWeight: 700,
              }}
            >
              {alerts.length}
            </span>
          )}
        </button>

        <button
          onClick={() => setActiveTab('history')}
          style={{
            background: 'transparent',
            border: 'none',
            borderBottom: activeTab === 'history' ? '2px solid var(--accent)' : '2px solid transparent',
            color: activeTab === 'history' ? 'var(--accent)' : 'var(--text-muted)',
            fontWeight: activeTab === 'history' ? '700' : '500',
            fontSize: '15px',
            padding: '10px 16px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}
        >
          <ArchiveIcon size={16} />
          <span>Historical Archive</span>
        </button>
      </div>

      {activeTab === 'active' ? (
        <AlertsPanel alerts={alerts} onAcknowledge={acknowledge} onResolve={resolve} />
      ) : (
        <AlertHistoryTable />
      )}
    </>
  )
}