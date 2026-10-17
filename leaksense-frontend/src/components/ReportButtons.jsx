import { useState } from 'react'
import {
  downloadAlertsReport,
  downloadWeeklyReport,
  sendWeeklyEmail,
} from '../api/leaksenseApi'
import { ShieldIcon, PdfIcon, EmailIcon, ExcelIcon } from './icons/AppIcons'

export default function ReportButtons() {
  const [busy, setBusy] = useState('')
  const [message, setMessage] = useState({ text: '', type: '' })
  const [fromDate, setFromDate] = useState('')
  const [toDate, setToDate] = useState('')

  const handleDownloadAlerts = async (type) => {
    setBusy(`alerts-${type}`)
    setMessage({ text: '', type: '' })
    try {
      const res = await downloadAlertsReport(type)
      const extension = type === 'excel' ? 'xlsx' : 'pdf'
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const link = document.createElement('a')
      link.href = url
      link.download = `leaksense-alerts.${extension}`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      setMessage({ text: `Successfully generated alerts ${type.toUpperCase()} report.`, type: 'ok' })
    } catch {
      setMessage({ text: 'Could not download the alert report. Please try again.', type: 'error' })
    } finally {
      setBusy('')
    }
  }

  const handleDownloadWeeklyPdf = async () => {
    setBusy('weekly-pdf')
    setMessage({ text: '', type: '' })
    try {
      const fromIso = fromDate ? new Date(fromDate).toISOString() : undefined
      const toIso = toDate ? new Date(toDate).toISOString() : undefined
      const res = await downloadWeeklyReport(fromIso, toIso)
      const url = window.URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }))
      const link = document.createElement('a')
      link.href = url
      link.download = `leaksense-weekly-safety-report.pdf`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
      setMessage({ text: 'Weekly Executive Safety PDF downloaded successfully.', type: 'ok' })
    } catch {
      setMessage({ text: 'Failed to generate weekly safety PDF. Verify backend logs.', type: 'error' })
    } finally {
      setBusy('')
    }
  }

  const handleSendWeeklyEmail = async () => {
    setBusy('weekly-email')
    setMessage({ text: '', type: '' })
    try {
      const fromIso = fromDate ? new Date(fromDate).toISOString() : undefined
      const toIso = toDate ? new Date(toDate).toISOString() : undefined
      const res = await sendWeeklyEmail(fromIso, toIso)
      if (res.data?.success) {
        setMessage({ text: 'Weekly safety report email dispatched to safety personnel.', type: 'ok' })
      } else {
        setMessage({ text: res.data?.message || 'Email delivery skipped or notification service disabled.', type: 'error' })
      }
    } catch {
      setMessage({ text: 'Failed to dispatch safety email. Please check SMTP settings.', type: 'error' })
    } finally {
      setBusy('')
    }
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {/* Weekly Executive Safety Report Card */}
      <div
        className="panel"
        style={{
          borderRadius: '12px',
          padding: '20px',
          border: '1px solid var(--border-soft)',
          background: 'var(--panel)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
          <ShieldIcon size={24} color="#020617" />
          <div>
            <h2 style={{ fontSize: '18px', margin: 0 }}>Executive Weekly Safety Report</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '13px', margin: '2px 0 0' }}>
              Comprehensive LibrePDF audit containing KPI summaries, leak occurrences, valve actuations, and zone status.
            </p>
          </div>
        </div>

        {/* Date Filter Bar */}
        <div
          style={{
            display: 'flex',
            gap: '12px',
            alignItems: 'center',
            flexWrap: 'wrap',
            margin: '16px 0',
            padding: '12px',
            background: 'var(--panel-2)',
            borderRadius: '8px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>From:</span>
            <input
              type="datetime-local"
              value={fromDate}
              onChange={(e) => setFromDate(e.target.value)}
              style={inputStyle}
            />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>To:</span>
            <input
              type="datetime-local"
              value={toDate}
              onChange={(e) => setToDate(e.target.value)}
              style={inputStyle}
            />
          </div>

          <span style={{ fontSize: '11px', color: 'var(--text-faint)' }}>
            (Defaults to past 7 days if left blank)
          </span>
        </div>

        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
          <button
            onClick={handleDownloadWeeklyPdf}
            disabled={busy !== ''}
            style={actionButtonStyle('var(--accent)', busy === 'weekly-pdf')}
          >
            <PdfIcon size={16} /> {busy === 'weekly-pdf' ? 'Generating PDF...' : 'Download Weekly PDF Report'}
          </button>

          <button
            onClick={handleSendWeeklyEmail}
            disabled={busy !== ''}
            style={actionButtonStyle('var(--primary)', busy === 'weekly-email')}
          >
            <EmailIcon size={16} /> {busy === 'weekly-email' ? 'Dispatching Email...' : 'Email Report to Safety Team'}
          </button>
        </div>
      </div>

      {/* Alert History Data Exports */}
      <div
        className="panel"
        style={{
          borderRadius: '12px',
          padding: '20px',
          border: '1px solid var(--border-soft)',
          background: 'var(--panel)',
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
          <ExcelIcon size={24} color="#020617" />
          <div>
            <h2 style={{ fontSize: '18px', margin: 0 }}>Raw Alert History Export</h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '13px', margin: '2px 0 0' }}>
              Export entire alert history logs with timestamps, sensors, operator acknowledgment, and resolution records.
            </p>
          </div>
        </div>

        <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', marginTop: '16px' }}>
          <button
            onClick={() => handleDownloadAlerts('excel')}
            disabled={busy !== ''}
            style={actionButtonStyle('#22c55e', busy === 'alerts-excel')}
          >
            <ExcelIcon size={16} /> {busy === 'alerts-excel' ? 'Exporting...' : 'Export Excel (.xlsx)'}
          </button>

          <button
            onClick={() => handleDownloadAlerts('pdf')}
            disabled={busy !== ''}
            style={actionButtonStyle('#ef4444', busy === 'alerts-pdf')}
          >
            <PdfIcon size={16} /> {busy === 'alerts-pdf' ? 'Exporting...' : 'Export PDF (.pdf)'}
          </button>
        </div>
      </div>

      {/* Status Feedback */}
      {message.text && (
        <div
          style={{
            padding: '12px 16px',
            borderRadius: '8px',
            background: message.type === 'ok' ? 'rgba(34, 197, 94, 0.15)' : 'rgba(239, 68, 68, 0.15)',
            color: message.type === 'ok' ? '#22c55e' : '#ef4444',
            border: `1px solid ${message.type === 'ok' ? '#22c55e' : '#ef4444'}`,
            fontSize: '13px',
            fontWeight: 500,
          }}
        >
          {message.text}
        </div>
      )}
    </div>
  )
}

const inputStyle = {
  background: 'var(--panel-3)',
  color: 'var(--text)',
  border: '1px solid var(--border-input)',
  borderRadius: '6px',
  padding: '6px 10px',
  fontSize: '12px',
}

const actionButtonStyle = (color, active) => ({
  background: 'transparent',
  color: color,
  border: `1.5px solid ${color}`,
  borderRadius: '8px',
  padding: '10px 20px',
  fontWeight: '600',
  fontSize: '13px',
  cursor: active ? 'not-allowed' : 'pointer',
  opacity: active ? 0.65 : 1,
  display: 'inline-flex',
  alignItems: 'center',
  gap: '8px',
  transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
})