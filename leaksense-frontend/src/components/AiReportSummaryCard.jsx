import { useState, useEffect } from 'react'
import { getAiReportSummary } from '../api/leaksenseApi'
import { CpuIcon, RefreshIcon, SearchIcon, ClipboardIcon } from './icons/AppIcons'

export default function AiReportSummaryCard() {
  const [summary, setSummary] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const loadSummary = async () => {
    setLoading(true)
    setError('')
    try {
      const res = await getAiReportSummary()
      setSummary(res.data)
    } catch {
      setError('Could not generate AI Safety Summary.')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadSummary()
  }, [])

  const getRiskBadge = (level) => {
    const l = level?.toUpperCase()
    if (l === 'CRITICAL_HAZARD') {
      return { bg: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', border: '#ef4444', label: 'CRITICAL HAZARD' }
    }
    if (l === 'ELEVATED_RISK') {
      return { bg: 'rgba(249, 115, 22, 0.15)', color: '#f97316', border: '#f97316', label: 'ELEVATED RISK' }
    }
    if (l === 'LOW_RISK') {
      return { bg: 'rgba(245, 158, 11, 0.15)', color: '#f59e0b', border: '#f59e0b', label: 'LOW RISK' }
    }
    return { bg: 'rgba(34, 197, 94, 0.15)', color: '#22c55e', border: '#22c55e', label: 'OPTIMAL' }
  }

  const badge = getRiskBadge(summary?.riskLevel)

  return (
    <div
      className="panel"
      style={{
        borderRadius: '12px',
        padding: '24px',
        border: '1px solid var(--border-soft)',
        background: 'var(--panel)',
        marginBottom: '20px',
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
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <CpuIcon size={24} color="#020617" />
          <div>
            <h2 style={{ fontSize: '18px', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
              AI Executive Safety Evaluation
              <span
                style={{
                  background: 'var(--accent-bg)',
                  color: 'var(--accent)',
                  fontSize: '11px',
                  fontWeight: 700,
                  padding: '2px 8px',
                  borderRadius: '999px',
                }}
              >
                LIVE ANALYSIS
              </span>
            </h2>
            <p style={{ color: 'var(--text-muted)', fontSize: '13px', margin: '2px 0 0' }}>
              Real-time heuristic & multi-sensor synthesis by LeakSense Safety Engine.
            </p>
          </div>
        </div>

        <button
          onClick={loadSummary}
          disabled={loading}
          style={{
            background: 'var(--panel-2)',
            color: 'var(--text)',
            border: '1px solid var(--border)',
            borderRadius: '8px',
            padding: '6px 12px',
            fontSize: '12px',
            cursor: 'pointer',
          }}
        >
          <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
            <RefreshIcon size={13} />
            <span>{loading ? 'Refreshing...' : 'Refresh Analysis'}</span>
          </span>
        </button>
      </div>

      {loading ? (
        <div style={{ textAlign: 'center', padding: '36px', color: 'var(--text-muted)' }}>
          Analyzing sensor metrics, valve history, and zone conditions...
        </div>
      ) : error ? (
        <div style={{ color: '#ef4444', fontSize: '13px' }}>{error}</div>
      ) : summary ? (
        <div>
          {/* Top Score Banner */}
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              flexWrap: 'wrap',
              gap: '16px',
              padding: '16px',
              background: 'var(--panel-2)',
              borderRadius: '10px',
              border: '1px solid var(--border-soft)',
              marginBottom: '16px',
            }}
          >
            <div>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)', fontWeight: 600 }}>
                OVERALL PIPELINE SAFETY SCORE
              </div>
              <div
                style={{
                  fontSize: '28px',
                  fontWeight: 900,
                  color: summary.overallSafetyScore >= 80 ? '#22c55e' : summary.overallSafetyScore >= 50 ? '#f59e0b' : '#ef4444',
                  marginTop: '4px',
                }}
              >
                {summary.overallSafetyScore?.toFixed(0)} <span style={{ fontSize: '16px', fontWeight: 500, color: 'var(--text-faint)' }}>/ 100</span>
              </div>
            </div>

            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: '12px', color: 'var(--text-muted)', marginBottom: '4px' }}>
                SAFETY ASSESSMENT
              </div>
              <span
                style={{
                  background: badge.bg,
                  color: badge.color,
                  border: `1px solid ${badge.border}`,
                  padding: '4px 12px',
                  borderRadius: '999px',
                  fontWeight: 800,
                  fontSize: '13px',
                }}
              >
                {badge.label}
              </span>
            </div>
          </div>

          {/* Executive Summary Narrative */}
          <div style={{ marginBottom: '16px' }}>
            <h4 style={{ fontSize: '13px', textTransform: 'uppercase', color: 'var(--text-muted)', marginBottom: '6px' }}>
              Executive Summary
            </h4>
            <p
              style={{
                fontSize: '14px',
                lineHeight: 1.6,
                color: 'var(--text)',
                background: 'var(--panel-3)',
                padding: '14px',
                borderRadius: '8px',
                borderLeft: '4px solid var(--accent)',
              }}
            >
              {summary.executiveSummary}
            </p>
          </div>

          {/* Observations & Recommendations Grid */}
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
              gap: '16px',
            }}
          >
            {/* Key Observations */}
            <div
              style={{
                background: 'var(--panel-2)',
                padding: '16px',
                borderRadius: '8px',
                border: '1px solid var(--border-soft)',
              }}
            >
              <h4 style={{ fontSize: '13px', color: 'var(--text)', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <SearchIcon size={14} color="#020617" />
                <span>Key Observations ({summary.keyObservations?.length || 0})</span>
              </h4>
              <ul style={{ paddingLeft: '18px', margin: 0, fontSize: '13px', color: 'var(--text-muted)', lineHeight: 1.5 }}>
                {summary.keyObservations?.map((obs, i) => (
                  <li key={i} style={{ marginBottom: '6px' }}>
                    {obs}
                  </li>
                ))}
              </ul>
            </div>

            {/* Recommended Actions */}
            <div
              style={{
                background: 'var(--panel-2)',
                padding: '16px',
                borderRadius: '8px',
                border: '1px solid var(--border-soft)',
              }}
            >
              <h4 style={{ fontSize: '13px', color: 'var(--text)', marginBottom: '10px', display: 'flex', alignItems: 'center', gap: '6px' }}>
                <ClipboardIcon size={14} color="#020617" />
                <span>Recommended Field Actions ({summary.recommendedActions?.length || 0})</span>
              </h4>
              <ul style={{ paddingLeft: '18px', margin: 0, fontSize: '13px', color: 'var(--text-muted)', lineHeight: 1.5 }}>
                {summary.recommendedActions?.map((act, i) => (
                  <li key={i} style={{ marginBottom: '6px' }}>
                    {act}
                  </li>
                ))}
              </ul>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}
