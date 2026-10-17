import { useState, useEffect, useCallback } from 'react'
import { getAlertHistory } from '../api/leaksenseApi'
import { useData } from '../context/DataContext'
import { CheckIcon, ArchiveIcon, DownloadIcon, UserIcon } from './icons/AppIcons'

export default function AlertHistoryTable() {
  const { allZones } = useData()

  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [historyData, setHistoryData] = useState({
    content: [],
    totalElements: 0,
    totalPages: 0,
    number: 0,
    size: 20,
  })

  // Filter States
  const [zoneId, setZoneId] = useState('')
  const [sensor, setSensor] = useState('')
  const [level, setLevel] = useState('')
  const [severity, setSeverity] = useState('')
  const [status, setStatus] = useState('')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [direction, setDirection] = useState('desc')

  const fetchHistory = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const params = {
        page,
        size,
        direction,
      }
      if (zoneId) params.zoneId = zoneId
      if (sensor) params.sensor = sensor
      if (level) params.level = level
      if (severity) params.severity = severity
      if (status) params.status = status
      if (from) params.from = new Date(from).toISOString()
      if (to) params.to = new Date(to).toISOString()

      const res = await getAlertHistory(params)
      setHistoryData(res.data)
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load alert history records.')
    } finally {
      setLoading(false)
    }
  }, [page, size, direction, zoneId, sensor, level, severity, status, from, to])

  useEffect(() => {
    fetchHistory()
  }, [fetchHistory])

  const handleApply = (e) => {
    e?.preventDefault()
    setPage(0)
    fetchHistory()
  }

  const handleReset = () => {
    setZoneId('')
    setSensor('')
    setLevel('')
    setSeverity('')
    setStatus('')
    setFrom('')
    setTo('')
    setDirection('desc')
    setPage(0)
  }

  const handleExportCsv = () => {
    if (!historyData.content || historyData.content.length === 0) return
    const headers = ['ID', 'Title', 'Severity', 'Level', 'Status', 'Sensor', 'Zone', 'Node', 'Created At', 'Resolved At', 'Acknowledged By']
    const rows = historyData.content.map((a) => [
      a.id,
      `"${(a.title || '').replace(/"/g, '""')}"`,
      a.severity === 'INFO' ? 'NORMAL' : (a.severity || ''),
      a.level || '',
      a.status || '',
      a.sensor || '',
      `"${(a.zone?.name || '').replace(/"/g, '""')}"`,
      a.node?.nodeCode || '',
      a.createdAt || '',
      a.resolvedAt || '',
      a.acknowledgedBy || '',
    ])
    const csvContent = [headers.join(','), ...rows.map((r) => r.join(','))].join('\n')
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `leaksense-alerts-${new Date().toISOString().slice(0, 10)}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
  }

  const formatDuration = (startStr, endStr) => {
    if (!endStr) return <span style={{ color: '#ef4444', fontWeight: 600 }}>Active</span>
    const diffMs = new Date(endStr) - new Date(startStr)
    if (diffMs < 0) return '—'
    const minutes = Math.floor(diffMs / (1000 * 60))
    if (minutes < 1) return '< 1 min'
    if (minutes < 60) return `${minutes} min`
    const hours = Math.floor(minutes / 60)
    const remMins = minutes % 60
    return `${hours}h ${remMins}m`
  }

  const formatDateTime = (dateStr) => {
    if (!dateStr) return '—'
    try {
      const d = new Date(dateStr)
      return d.toLocaleString([], {
        month: 'short',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      })
    } catch {
      return dateStr
    }
  }

  const getSeverityBadge = (sev) => {
    const s = sev?.toUpperCase()
    if (s === 'CRITICAL') {
      return (
        <span
          style={{
            background: 'rgba(239, 68, 68, 0.15)',
            color: '#ef4444',
            border: '1px solid #ef4444',
            borderRadius: '999px',
            padding: '2px 8px',
            fontSize: '11px',
            fontWeight: 700,
          }}
        >
          CRITICAL
        </span>
      )
    }
    if (s === 'WARNING') {
      return (
        <span
          style={{
            background: 'rgba(245, 158, 11, 0.15)',
            color: '#f59e0b',
            border: '1px solid #f59e0b',
            borderRadius: '999px',
            padding: '2px 8px',
            fontSize: '11px',
            fontWeight: 700,
          }}
        >
          WARNING
        </span>
      )
    }
    return (
      <span
        style={{
          background: 'var(--primary-tint)',
          color: 'var(--primary)',
          border: '1px solid var(--primary-border)',
          borderRadius: '999px',
          padding: '2px 8px',
          fontSize: '11px',
          fontWeight: 700,
        }}
      >
        NORMAL
      </span>
    )
  }

  const getStatusBadge = (st) => {
    const s = st?.toUpperCase()
    if (s === 'RESOLVED') {
      return (
        <span
          style={{
            background: 'rgba(34, 197, 94, 0.15)',
            color: '#22c55e',
            borderRadius: '6px',
            padding: '2px 8px',
            fontSize: '11px',
            fontWeight: 600,
            display: 'inline-flex',
            alignItems: 'center',
            gap: '4px',
          }}
        >
          <CheckIcon size={12} color="#22c55e" />
          <span>RESOLVED</span>
        </span>
      )
    }
    if (s === 'ACKNOWLEDGED') {
      return (
        <span
          style={{
            background: 'var(--primary-tint)',
            color: 'var(--primary)',
            borderRadius: '6px',
            padding: '2px 8px',
            fontSize: '11px',
            fontWeight: 600,
          }}
        >
          ACKNOWLEDGED
        </span>
      )
    }
    return (
      <span
        style={{
          background: 'rgba(239, 68, 68, 0.15)',
          color: '#ef4444',
          borderRadius: '6px',
          padding: '2px 8px',
          fontSize: '11px',
          fontWeight: 600,
        }}
      >
        ● OPEN
      </span>
    )
  }

  return (
    <div
      style={{
        background: 'var(--panel)',
        borderRadius: '12px',
        padding: '20px',
        border: '1px solid var(--border-soft)',
        marginTop: '16px',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px',
          marginBottom: '16px',
        }}
      >
        <div>
          <h2 style={{ fontSize: '18px', display: 'flex', alignItems: 'center', gap: '8px' }}>
            <ArchiveIcon size={18} />
            <span>Alert History & Audit Archive</span>
          </h2>
          <p style={{ color: 'var(--text-muted)', fontSize: '13px', margin: '4px 0 0' }}>
            Search historical records, resolve logs, duration tracking, and operator acknowledgments.
          </p>
        </div>
        <div style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
          Total Logged: <strong>{historyData.totalElements || 0}</strong>
        </div>
      </div>

      {/* Filter Controls Bar */}
      <form
        onSubmit={handleApply}
        style={{
          background: 'var(--panel-2)',
          padding: '14px 16px',
          borderRadius: '10px',
          display: 'flex',
          flexWrap: 'wrap',
          gap: '12px',
          alignItems: 'flex-end',
          marginBottom: '18px',
          border: '1px solid var(--border-soft)',
          boxSizing: 'border-box',
          width: '100%',
        }}
      >
        {/* Zone Filter */}
        <div style={{ flex: '1 1 120px', minWidth: '110px' }}>
          <label style={{ display: 'block', fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px' }}>
            Zone
          </label>
          <select
            value={zoneId}
            onChange={(e) => setZoneId(e.target.value)}
            style={selectStyle}
          >
            <option value="">All Zones</option>
            {allZones?.map((z) => (
              <option key={z.id} value={z.id}>
                {z.name}
              </option>
            ))}
          </select>
        </div>

        {/* Severity */}
        <div style={{ flex: '1 1 120px', minWidth: '110px' }}>
          <label style={{ display: 'block', fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px' }}>
            Severity
          </label>
          <select
            value={severity}
            onChange={(e) => setSeverity(e.target.value)}
            style={selectStyle}
          >
            <option value="">All Severities</option>
            <option value="CRITICAL">CRITICAL</option>
            <option value="WARNING">WARNING</option>
            <option value="INFO">NORMAL</option>
          </select>
        </div>

        {/* Status */}
        <div style={{ flex: '1 1 120px', minWidth: '110px' }}>
          <label style={{ display: 'block', fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px' }}>
            Status
          </label>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            style={selectStyle}
          >
            <option value="">All Statuses</option>
            <option value="OPEN">OPEN</option>
            <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
            <option value="RESOLVED">RESOLVED</option>
          </select>
        </div>

        {/* Level */}
        <div style={{ flex: '1 1 120px', minWidth: '110px' }}>
          <label style={{ display: 'block', fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px' }}>
            Level
          </label>
          <select
            value={level}
            onChange={(e) => setLevel(e.target.value)}
            style={selectStyle}
          >
            <option value="">All Levels</option>
            <option value="SENSOR">SENSOR</option>
            <option value="DEVICE">DEVICE</option>
            <option value="FAULT">FAULT</option>
            <option value="SYSTEM">SYSTEM</option>
          </select>
        </div>

        {/* Sensor */}
        <div style={{ flex: '1 1 120px', minWidth: '110px' }}>
          <label style={{ display: 'block', fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px' }}>
            Sensor
          </label>
          <select
            value={sensor}
            onChange={(e) => setSensor(e.target.value)}
            style={selectStyle}
          >
            <option value="">All Sensors</option>
            <option value="METHANE">METHANE</option>
            <option value="PRESSURE">PRESSURE</option>
            <option value="OXYGEN">OXYGEN</option>
            <option value="BATTERY">BATTERY</option>
          </select>
        </div>

        {/* From Date */}
        <div style={{ flex: '1 1 150px', minWidth: '140px' }}>
          <label style={{ display: 'block', fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px' }}>
            From
          </label>
          <input
            type="datetime-local"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            style={inputStyle}
          />
        </div>

        {/* To Date */}
        <div style={{ flex: '1 1 150px', minWidth: '140px' }}>
          <label style={{ display: 'block', fontSize: '11px', color: 'var(--text-muted)', marginBottom: '4px' }}>
            To
          </label>
          <input
            type="datetime-local"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            style={inputStyle}
          />
        </div>

        {/* Actions */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            flex: '0 0 auto',
            minWidth: 'fit-content',
            marginLeft: 'auto',
          }}
        >
          <button
            type="submit"
            style={{
              background: 'var(--accent)',
              color: 'var(--on-accent)',
              border: 'none',
              borderRadius: '8px',
              padding: '9px 14px',
              fontSize: '12px',
              fontWeight: '700',
              cursor: 'pointer',
              height: '36px',
              whiteSpace: 'nowrap',
            }}
          >
            Filter
          </button>
          <button
            type="button"
            onClick={handleReset}
            style={{
              background: 'transparent',
              color: 'var(--text-muted)',
              border: '1px solid var(--border-input)',
              borderRadius: '8px',
              padding: '9px 12px',
              fontSize: '12px',
              cursor: 'pointer',
              height: '36px',
              whiteSpace: 'nowrap',
            }}
          >
            Reset
          </button>
          <button
            type="button"
            onClick={handleExportCsv}
            disabled={!historyData.content || historyData.content.length === 0}
            style={{
              background: 'rgba(20, 184, 166, 0.15)',
              color: '#14b8a6',
              border: '1px solid rgba(20, 184, 166, 0.35)',
              borderRadius: '8px',
              padding: '9px 14px',
              fontSize: '12px',
              fontWeight: '700',
              cursor: (!historyData.content || historyData.content.length === 0) ? 'not-allowed' : 'pointer',
              height: '36px',
              display: 'flex',
              alignItems: 'center',
              gap: '6px',
              whiteSpace: 'nowrap',
            }}
            title="Download filtered alert archive as CSV file"
          >
            <DownloadIcon size={14} />
            <span>Export CSV</span>
          </button>
        </div>
      </form>

      {error && (
        <div style={{ color: '#ef4444', fontSize: '13px', marginBottom: '12px' }}>
          {error}
        </div>
      )}

      {/* Table */}
      <div style={{ overflowX: 'auto' }}>
        <table
          className="data-table"
          style={{
            width: '100%',
            borderCollapse: 'collapse',
            fontSize: '13px',
            textAlign: 'left',
          }}
        >
          <thead>
            <tr style={{ background: 'var(--panel-2)' }}>
              <th style={thStyle}>Date & Time</th>
              <th style={thStyle}>Severity</th>
              <th style={thStyle}>Title & Description</th>
              <th style={thStyle}>Zone / Node</th>
              <th style={thStyle}>Type / Sensor</th>
              <th style={thStyle}>Status</th>
              <th style={thStyle}>Ack By</th>
              <th style={thStyle}>Resolved / Duration</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center', padding: '32px', color: 'var(--text-muted)' }}>
                  Loading history records...
                </td>
              </tr>
            ) : historyData.content.length === 0 ? (
              <tr>
                <td colSpan="8" style={{ textAlign: 'center', padding: '36px', color: 'var(--text-muted)' }}>
                  No alert records match the selected filter criteria.
                </td>
              </tr>
            ) : (
              historyData.content.map((item) => (
                <tr
                  key={item.id}
                  style={{
                    borderBottom: '1px solid var(--border-soft)',
                    transition: 'background 0.15s ease',
                  }}
                >
                  <td style={tdStyle}>
                    <div style={{ fontWeight: 600 }}>{formatDateTime(item.createdAt)}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>ID #{item.id}</div>
                  </td>
                  <td style={tdStyle}>{getSeverityBadge(item.severity)}</td>
                  <td style={{ ...tdStyle, maxWidth: '240px' }}>
                    <div style={{ fontWeight: 600, color: 'var(--text)' }}>{item.title}</div>
                    <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>
                      {item.message}
                    </div>
                  </td>
                  <td style={tdStyle}>
                    <div style={{ fontWeight: 500 }}>{item.zone?.name || '—'}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>
                      {item.node?.nodeCode || 'All Nodes'}
                    </div>
                  </td>
                  <td style={tdStyle}>
                    <div>{item.sensor || item.level || 'SYSTEM'}</div>
                    {item.level && item.sensor && (
                      <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>{item.level}</div>
                    )}
                  </td>
                  <td style={tdStyle}>{getStatusBadge(item.status)}</td>
                  <td style={tdStyle}>
                    {item.acknowledgedBy ? (
                      <span style={{ color: 'var(--text)', fontWeight: 500, display: 'inline-flex', alignItems: 'center', gap: '4px' }}>
                        <UserIcon size={13} />
                        <span>{item.acknowledgedBy}</span>
                      </span>
                    ) : (
                      <span style={{ color: 'var(--text-faint)' }}>—</span>
                    )}
                  </td>
                  <td style={tdStyle}>
                    <div>{formatDateTime(item.resolvedAt)}</div>
                    <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>
                      {formatDuration(item.createdAt, item.resolvedAt)}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination Bar */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          flexWrap: 'wrap',
          gap: '12px',
          marginTop: '16px',
          paddingTop: '12px',
          borderTop: '1px solid var(--border-soft)',
          fontSize: '12px',
          color: 'var(--text-muted)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span>Rows per page:</span>
          <select
            value={size}
            onChange={(e) => {
              setSize(Number(e.target.value))
              setPage(0)
            }}
            style={{
              background: 'var(--panel-2)',
              color: 'var(--text)',
              border: '1px solid var(--border-input)',
              borderRadius: '6px',
              padding: '4px 8px',
              fontSize: '12px',
            }}
          >
            <option value="10">10</option>
            <option value="20">20</option>
            <option value="50">50</option>
          </select>
          <span>
            Page {historyData.number + 1} of {Math.max(1, historyData.totalPages)}
          </span>
        </div>

        <div style={{ display: 'flex', gap: '6px' }}>
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={historyData.number <= 0 || loading}
            style={paginationBtnStyle(historyData.number > 0)}
          >
            ◀ Previous
          </button>
          <button
            onClick={() => setPage((p) => p + 1)}
            disabled={historyData.number + 1 >= historyData.totalPages || loading}
            style={paginationBtnStyle(historyData.number + 1 < historyData.totalPages)}
          >
            Next ▶
          </button>
        </div>
      </div>
    </div>
  )
}

const selectStyle = {
  width: '100%',
  boxSizing: 'border-box',
  padding: '7px 10px',
  borderRadius: '6px',
  background: 'var(--panel-3)',
  color: 'var(--text)',
  border: '1px solid var(--border-input)',
  fontSize: '12px',
}

const inputStyle = {
  width: '100%',
  boxSizing: 'border-box',
  padding: '6px 8px',
  borderRadius: '6px',
  background: 'var(--panel-3)',
  color: 'var(--text)',
  border: '1px solid var(--border-input)',
  fontSize: '12px',
}

const thStyle = {
  padding: '10px 12px',
  fontWeight: '600',
  fontSize: '12px',
  color: 'var(--text-faint)',
  borderBottom: '1px solid var(--border)',
}

const tdStyle = {
  padding: '12px',
  verticalAlign: 'middle',
}

const paginationBtnStyle = (enabled) => ({
  background: enabled ? 'var(--hover)' : 'transparent',
  color: enabled ? 'var(--text)' : 'var(--text-faint)',
  border: '1px solid var(--border-input)',
  borderRadius: '6px',
  padding: '6px 12px',
  cursor: enabled ? 'pointer' : 'not-allowed',
  fontSize: '12px',
  fontWeight: '500',
})
