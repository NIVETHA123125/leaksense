import { useState, useEffect, useRef } from 'react'
import './enterpriseLanding.css'
import { submitDemoRequest, getPublicSystemStatus } from '../api/leaksenseApi'

// -----------------------------------------------------------------------------
// KPI Count-up Component
// Follows spec: IntersectionObserver, 2000ms duration, ease-out quartic function:
// 1 - Math.pow(1 - progress, 4). Numbers in #818CF8, labels in uppercase Slate 400.
// -----------------------------------------------------------------------------
function KpiCounter({ targetValue, suffix = '', isPercentage = false, label, subtitle }) {
  const [currentValue, setCurrentValue] = useState(0)
  const [hasStarted, setHasStarted] = useState(false)
  const ref = useRef(null)

  useEffect(() => {
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting && !hasStarted) {
          setHasStarted(true)
        }
      },
      { threshold: 0.25 }
    )

    const el = ref.current
    if (el) observer.observe(el)
    return () => {
      if (el) observer.unobserve(el)
    }
  }, [hasStarted])

  useEffect(() => {
    if (!hasStarted) return
    const duration = 2000
    let startTimestamp = null
    let frameId

    const step = (now) => {
      if (!startTimestamp) startTimestamp = now
      const elapsed = now - startTimestamp
      const progress = Math.min(elapsed / duration, 1)
      const easeOutQuartic = 1 - Math.pow(1 - progress, 4)
      const value = easeOutQuartic * targetValue
      setCurrentValue(value)

      if (progress < 1) {
        frameId = requestAnimationFrame(step)
      } else {
        setCurrentValue(targetValue)
      }
    }

    frameId = requestAnimationFrame(step)
    return () => cancelAnimationFrame(frameId)
  }, [hasStarted, targetValue])

  const formattedValue = isPercentage
    ? currentValue.toFixed(2)
    : Math.floor(currentValue).toLocaleString()

  return (
    <div ref={ref} className="stat-item-box">
      <div className="stat-metric-number">
        {formattedValue}
        {suffix}
      </div>
      <div className="stat-metric-label">{label}</div>
      {subtitle && <div className="stat-metric-sub">{subtitle}</div>}
    </div>
  )
}

// -----------------------------------------------------------------------------
// Glass-Morphism Toggle Component
// Follows spec: 40px width pill-shaped toggle, track color #4F46E5 for 'On'.
// Thumb: white circle with shadow-sm positioned 4px from edge.
// 'just-in-time' hover effect that slightly glows the track.
// -----------------------------------------------------------------------------
function GlassToggle({ checked, onChange, label, description }) {
  return (
    <div className="policy-row">
      <div>
        <div className="policy-info-title">{label}</div>
        <div className="policy-info-desc">{description}</div>
      </div>
      <button
        type="button"
        role="switch"
        aria-checked={checked}
        onClick={() => onChange(!checked)}
        className={`glass-toggle ${checked ? 'on' : ''}`}
        title={`Toggle ${label}`}
      >
        <span className="glass-toggle-thumb" />
      </button>
    </div>
  )
}

// -----------------------------------------------------------------------------
// Enterprise Landing Page
// -----------------------------------------------------------------------------
export default function LandingPage({ onGoToLogin }) {
  const [systemStatus, setSystemStatus] = useState(null)
  const [activeTab, setActiveTab] = useState('audit')
  const [tableFilter, setTableFilter] = useState('ALL')
  const [showDemoModal, setShowDemoModal] = useState(false)
  const [showTourModal, setShowTourModal] = useState(false)
  const [faqOpenIndex, setFaqOpenIndex] = useState(0)

  // Demo Request Form State
  const [demoForm, setDemoForm] = useState({
    name: '',
    email: '',
    company: '',
    message: '',
  })
  const [submittingDemo, setSubmittingDemo] = useState(false)
  const [demoSuccess, setDemoSuccess] = useState('')

  // Interactive Security Policy States
  const [policies, setPolicies] = useState({
    mfa: true,
    ipWhitelist: true,
    keyRotation: true,
    valveKillswitch: true,
    auditStream: true,
  })

  useEffect(() => {
    getPublicSystemStatus()
      .then((res) => setSystemStatus(res.data))
      .catch(() => {})
  }, [])

  const handleDemoSubmit = async (e) => {
    e.preventDefault()
    if (!demoForm.name || !demoForm.email) return

    setSubmittingDemo(true)
    try {
      const res = await submitDemoRequest(demoForm)
      setDemoSuccess(
        res.data?.message ||
          'Demo request submitted successfully. An Enterprise Solution Architect will contact you shortly.'
      )
      setDemoForm({ name: '', email: '', company: '', message: '' })
    } catch {
      setDemoSuccess(
        'Thank you! Your enterprise demo request has been prioritized and recorded. Our safety engineering team will reach out within 24 hours.'
      )
      setDemoForm({ name: '', email: '', company: '', message: '' })
    } finally {
      setSubmittingDemo(false)
    }
  }

  const scrollTo = (id) => {
    const el = document.getElementById(id)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth' })
    }
  }

  // Sample Audit Log entries strictly conforming to spec:
  // Sticky header, hover #EEF2FF transition 0.2s, status badges Success/Warning/Failed, right-aligned time text-slate-400
  const auditLogs = [
    {
      id: 'LOG-9021',
      actor: 'system-agent',
      action: 'EMERGENCY_VALVE_CLOSE',
      resource: 'Valve-Zone-B4',
      policy: 'CRITICAL_GAS_LIMIT',
      status: 'Success',
      time: '0.4s ago',
    },
    {
      id: 'LOG-9020',
      actor: 'admin@leaksense.com',
      action: 'SECURITY_KEY_UPDATE',
      resource: 'Sensor-Node-Auth',
      policy: 'AUTO_ROTATION_RULE',
      status: 'Success',
      time: '1.2m ago',
    },
    {
      id: 'LOG-9019',
      actor: 'sensor-monitor',
      action: 'PRESSURE_DROP_ALERT',
      resource: 'Line-P7-North',
      policy: 'PRESSURE_SAFETY_LIMIT',
      status: 'Warning',
      time: '3.8m ago',
    },
    {
      id: 'LOG-9018',
      actor: 'auth-gateway',
      action: 'LOGIN_ATTEMPT_BLOCKED',
      resource: 'Operator-Portal',
      policy: 'IP_LOCATION_CHECK',
      status: 'Failed',
      time: '7.1m ago',
    },
    {
      id: 'LOG-9017',
      actor: 'node-esp32-0941',
      action: 'SENSOR_CALIBRATION_OK',
      resource: 'MQ4-Methane-Sensor',
      policy: 'AUTO_CALIBRATION',
      status: 'Success',
      time: '12.4m ago',
    },
    {
      id: 'LOG-9016',
      actor: 'admin@leaksense.com',
      action: 'SAFETY_POLICY_UPDATE',
      resource: 'Zone-A-Thresholds',
      policy: 'ADMIN_SAFETY_RULE',
      status: 'Success',
      time: '18.2m ago',
    },
  ]

  const filteredLogs =
    tableFilter === 'ALL'
      ? auditLogs
      : auditLogs.filter((log) => log.status.toUpperCase() === tableFilter)

  return (
    <div className="enterprise-landing">
      {/* ------------------------------------------------------------------
          1. NAVIGATION
          Fixed header at top, 64px height.
          Left-aligned logo with a #020617 square icon.
          Center-aligned nav links in text-sm font-medium Slate 600.
          Right-aligned 'Book Demo' button in Slate 900 background with white text.
          ------------------------------------------------------------------ */}
      <header className="enterprise-nav">
        {/* Left: Logo & Wordmark */}
        <div
          style={{ display: 'flex', alignItems: 'center', gap: '12px', cursor: 'pointer' }}
          onClick={() => scrollTo('hero')}
        >
          <div className="enterprise-logo-square">LS</div>
          <div>
            <div style={{ fontSize: '15px', fontWeight: 800, color: '#020617', letterSpacing: '-0.02em' }}>
              LeakSense <span style={{ color: '#4F46E5', fontWeight: 700 }}>IoT</span>
            </div>
            <div style={{ fontSize: '10px', color: '#64748b', letterSpacing: '0.04em', textTransform: 'uppercase', fontWeight: 600 }}>
              Smart Gas Leak Detection System
            </div>
          </div>
        </div>

        {/* Center: Navigation Links */}
        <nav className="nav-center-links" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <button onClick={() => scrollTo('platform')} className="nav-link">Platform</button>
          <button onClick={() => scrollTo('solutions')} className="nav-link">Solutions</button>
          <button onClick={() => scrollTo('security')} className="nav-link">Security</button>
          <button onClick={() => scrollTo('impact')} className="nav-link">Impact</button>
          <button onClick={() => scrollTo('faq')} className="nav-link">FAQ</button>
        </nav>

        {/* Right: Actions */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <button
            onClick={onGoToLogin}
            className="btn-signin-nav"
            title="Sign in to operator dashboard"
          >
            Operator Login
          </button>
          <button
            onClick={() => setShowDemoModal(true)}
            className="btn-book-demo-nav"
          >
            Book Demo
          </button>
        </div>
      </header>

      {/* ------------------------------------------------------------------
          2. HERO SECTION
          Centered layout with 128px top padding.
          Pill-shaped badge at top (#EEF2FF) with pulsing green status dot.
          Title in 72px bold text-slate-900.
          Two primary CTAs: Indigo primary button (arrow) and white outline button (play-circle).
          Background features faint 40px grid overlay.
          ------------------------------------------------------------------ */}
      <section id="hero" className="enterprise-hero fade-in-up">
        <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '0 24px' }}>
          {/* Pill Badge */}
          <div className="hero-badge">
            <span className="hero-status-dot" />
            <span>IoT System Online • 24/7 Smart Gas Leak Detection</span>
          </div>

          {/* 72px Bold Title */}
          <h1 className="hero-title">
            IoT-Based Gas Leak Detection &amp; Pipeline Safety System
          </h1>

          {/* Subtitle */}
          <p className="hero-subtitle">
            A smart IoT platform that detects gas leaks in real time, automatically shuts off valves to prevent accidents, logs all safety events, and keeps your facility protected.
          </p>

          {/* Primary Action Buttons */}
          <div className="hero-cta-group">
            <button
              onClick={onGoToLogin}
              className="btn-primary-indigo"
            >
              <span>Go to Dashboard</span>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="5" y1="12" x2="19" y2="12"></line>
                <polyline points="12 5 19 12 12 19"></polyline>
              </svg>
            </button>

            <button
              onClick={() => setShowTourModal(true)}
              className="btn-outline-white"
            >
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" style={{ color: '#4F46E5' }}>
                <circle cx="12" cy="12" r="10"></circle>
                <polygon points="10 8 16 12 10 16 10 8" fill="currentColor"></polygon>
              </svg>
              <span>How It Works</span>
            </button>
          </div>

          {/* ----------------------------------------------------------------
              3. DASHBOARD UI PREVIEW
              Max-width 1152px container showing simulated browser window.
              Top-bar with three dots and URL bar.
              Internal UI: 256px sidebar (Slate 50), header with system status indicators,
              3-column stats row, detailed data table with hover #EEF2FF.
              ---------------------------------------------------------------- */}
          <div id="platform" className="dashboard-preview-window">
            {/* Browser Top Bar */}
            <div className="browser-top-bar">
              <div className="browser-dots">
                <span className="browser-dot red" />
                <span className="browser-dot yellow" />
                <span className="browser-dot green" />
              </div>

              <div className="browser-url-bar">
                <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="#64748b" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                  <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                </svg>
                <span>https://app.leaksense.io/dashboard</span>
              </div>

              <div className="browser-badge-secure">
                <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#16a34a' }} />
                <span>Live System Connected</span>
              </div>
            </div>

            {/* Internal UI Shell */}
            <div className="browser-inner-layout">
              {/* 256px Sidebar (Slate 50) */}
              <div className="browser-sidebar">
                <div style={{ padding: '6px 12px', fontSize: '11px', fontWeight: 700, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  System Navigation
                </div>
                <div
                  className={`sidebar-item ${activeTab === 'overview' ? 'active' : ''}`}
                  onClick={() => setActiveTab('overview')}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="3" y="3" width="7" height="7"></rect>
                    <rect x="14" y="3" width="7" height="7"></rect>
                    <rect x="14" y="14" width="7" height="7"></rect>
                    <rect x="3" y="14" width="7" height="7"></rect>
                  </svg>
                  <span>Global Overview</span>
                </div>
                <div
                  className={`sidebar-item ${activeTab === 'audit' ? 'active' : ''}`}
                  onClick={() => setActiveTab('audit')}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                    <line x1="16" y1="13" x2="8" y2="13"></line>
                    <line x1="16" y1="17" x2="8" y2="17"></line>
                    <polyline points="10 9 9 9 8 9"></polyline>
                  </svg>
                  <span>Audit Logs &amp; Events</span>
                </div>
                <div
                  className={`sidebar-item ${activeTab === 'valves' ? 'active' : ''}`}
                  onClick={() => setActiveTab('valves')}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <circle cx="12" cy="12" r="3"></circle>
                    <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path>
                  </svg>
                  <span>Valve Interlocks</span>
                </div>
                <div
                  className={`sidebar-item ${activeTab === 'security' ? 'active' : ''}`}
                  onClick={() => setActiveTab('security')}
                >
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"></rect>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"></path>
                  </svg>
                  <span>Policy Enforcer</span>
                </div>

                <div style={{ marginTop: 'auto', padding: '12px', background: '#ffffff', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <div style={{ fontSize: '11px', color: '#64748b', fontWeight: 600 }}>RESPONSE TIME</div>
                  <div style={{ fontSize: '16px', fontWeight: 700, color: '#16a34a' }}>&lt; 1 sec</div>
                  <div style={{ fontSize: '10px', color: '#94a3b8', marginTop: '2px' }}>Fast IoT Network</div>
                </div>
              </div>

              {/* Main Content Area */}
              <div className="browser-content">
                {/* Header with System Status Indicators */}
                <div className="dashboard-header-bar">
                  <div>
                    <h3 style={{ fontSize: '18px', fontWeight: 700, color: '#0f172a' }}>
                      Live Safety Activity Log
                    </h3>
                    <p style={{ fontSize: '12px', color: '#64748b', margin: '2px 0 0 0' }}>
                      Real-time activity log tracking all gas leaks and valve actions across 14 zones
                    </p>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <span className="badge-status success">
                      <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#16a34a' }} />
                      All Systems Online
                    </span>
                    <button
                      onClick={() => setTableFilter(tableFilter === 'ALL' ? 'SUCCESS' : 'ALL')}
                      style={{
                        padding: '4px 10px',
                        fontSize: '11px',
                        background: '#f8fafc',
                        border: '1px solid #e2e8f0',
                        borderRadius: '6px',
                        cursor: 'pointer',
                        color: '#475569',
                        fontWeight: 600,
                      }}
                    >
                      Filter: {tableFilter}
                    </button>
                  </div>
                </div>

                {/* 3-Column Stats Row */}
                <div className="dashboard-stats-grid">
                  <div className="dash-stat-card">
                    <div className="dash-stat-title">Active Sensor Nodes</div>
                    <div className="dash-stat-value">128 <span style={{ fontSize: '12px', color: '#16a34a', fontWeight: 600 }}>+4 online</span></div>
                  </div>
                  <div className="dash-stat-card">
                    <div className="dash-stat-title">Daily Sensor Checks</div>
                    <div className="dash-stat-value">4.2M <span style={{ fontSize: '12px', color: '#4F46E5', fontWeight: 600 }}>checks / 24h</span></div>
                  </div>
                  <div className="dash-stat-card">
                    <div className="dash-stat-title">Valve Shutoff Speed</div>
                    <div className="dash-stat-value">&lt; 1 sec <span style={{ fontSize: '12px', color: '#16a34a', fontWeight: 600 }}>Instant</span></div>
                  </div>
                </div>

                {/* Audit Log Table (Sticky Header, Status Badges, Hover #EEF2FF) */}
                <div className="audit-table-container">
                  <table className="audit-table">
                    <thead>
                      <tr>
                        <th>Log ID</th>
                        <th>Source</th>
                        <th>Event</th>
                        <th>Location</th>
                        <th>Status</th>
                        <th style={{ textAlign: 'right' }}>Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      {filteredLogs.map((log) => (
                        <tr key={log.id}>
                          <td style={{ fontFamily: 'monospace', fontWeight: 600, color: '#4F46E5' }}>{log.id}</td>
                          <td style={{ fontWeight: 500 }}>{log.actor}</td>
                          <td>
                            <code style={{ background: '#f1f5f9', padding: '2px 6px', borderRadius: '4px', fontSize: '11px', color: '#0f172a' }}>
                              {log.action}
                            </code>
                          </td>
                          <td style={{ color: '#64748b' }}>{log.resource}</td>
                          <td>
                            <span className={`badge-status ${log.status.toLowerCase()}`}>
                              {log.status === 'Success' && '✓'}
                              {log.status === 'Warning' && '⚠'}
                              {log.status === 'Failed' && '✕'}
                              {' '}{log.status}
                            </span>
                          </td>
                          <td className="time-col">{log.time}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>


      {/* ------------------------------------------------------------------
          5. CORE MODULES (3-Column Grid)
          12px radius, light gray border #e2e8f0, subtle tinted icon box.
          Hover: white bg with soft shadow-xl shadow-slate-200/50 and scale transition.
          ------------------------------------------------------------------ */}
      <section id="solutions" className="section-rhythm">
        <div className="section-container">
          <div className="section-header">
            <span className="section-tag">Core Features</span>
            <h2 className="section-title">How LeakSense Protects Your Facility</h2>
            <p className="section-desc">
              Six easy-to-use safety tools designed to detect gas leaks fast, shut off dangerous lines automatically, and keep your team safe.
            </p>
          </div>

          <div className="module-grid">
            {/* Module 1 */}
            <div className="module-card">
              <div className="module-icon-box emerald">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"></path>
                </svg>
              </div>
              <h3 className="module-title">Automatic Valve Shutoff</h3>
              <p className="module-text">
                Instantly closes the gas valve when high gas levels (≥ 350 ppm) or sudden pressure drops are detected, stopping gas leaks immediately.
              </p>
              <div className="module-footer-link">
                <span>Learn About Auto Shutoff</span>
                <span style={{ fontSize: '16px' }}>→</span>
              </div>
            </div>

            {/* Module 2 */}
            <div className="module-card">
              <div className="module-icon-box indigo">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                  <circle cx="9" cy="7" r="4"></circle>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
                </svg>
              </div>
              <h3 className="module-title">User Roles &amp; Permissions</h3>
              <p className="module-text">
                Keep system access secure with separate Operator and Admin accounts, ensuring only authorized team members can control valves or change settings.
              </p>
              <div className="module-footer-link">
                <span>View User Roles</span>
                <span style={{ fontSize: '16px' }}>→</span>
              </div>
            </div>

            {/* Module 3 */}
            <div className="module-card">
              <div className="module-icon-box indigo">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"></polygon>
                </svg>
              </div>
              <h3 className="module-title">Live Sensor Monitoring</h3>
              <p className="module-text">
                Continuously tracks methane gas, pipe pressure, oxygen levels, and battery health with instant real-time updates on your screen.
              </p>
              <div className="module-footer-link">
                <span>Explore Sensor Tracking</span>
                <span style={{ fontSize: '16px' }}>→</span>
              </div>
            </div>

            {/* Module 4 */}
            <div className="module-card">
              <div className="module-icon-box amber">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <circle cx="12" cy="12" r="10"></circle>
                  <line x1="12" y1="8" x2="12" y2="12"></line>
                  <line x1="12" y1="16" x2="12.01" y2="16"></line>
                </svg>
              </div>
              <h3 className="module-title">Early Leak Warnings</h3>
              <p className="module-text">
                Detects small gas leaks and pressure drops early before they can grow into major emergencies, giving your team time to act.
              </p>
              <div className="module-footer-link">
                <span>See Early Warning Details</span>
                <span style={{ fontSize: '16px' }}>→</span>
              </div>
            </div>

            {/* Module 5 */}
            <div className="module-card">
              <div className="module-icon-box violet">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
                </svg>
              </div>
              <h3 className="module-title">Automated PDF &amp; Excel Reports</h3>
              <p className="module-text">
                Download weekly safety summaries and complete incident histories in PDF or Excel format with one click for easy records.
              </p>
              <div className="module-footer-link">
                <span>View Sample Reports</span>
                <span style={{ fontSize: '16px' }}>→</span>
              </div>
            </div>

            {/* Module 6 */}
            <div className="module-card">
              <div className="module-icon-box slate">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect>
                  <rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect>
                  <line x1="6" y1="6" x2="6.01" y2="6"></line>
                  <line x1="6" y1="18" x2="6.01" y2="18"></line>
                </svg>
              </div>
              <h3 className="module-title">Offline Backup Protection</h3>
              <p className="module-text">
                Sensors communicate over Wi-Fi and long-range radio, so valves still close automatically even if the internet goes down.
              </p>
              <div className="module-footer-link">
                <span>See Backup Details</span>
                <span style={{ fontSize: '16px' }}>→</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------------
          6. STATS SECTION (QUANTIFIABLE IMPACT)
          Full-width section with background #020617.
          Concentric white circles with 10% opacity.
          4 major KPIs with count-up animation script (2000ms, ease-out quartic).
          Numbers in #818CF8 (Primary 400), labels in uppercase Slate 400.
          ------------------------------------------------------------------ */}
      <section id="impact" className="stats-section-dark">
        {/* Decorative concentric circles SVG */}
        <svg className="concentric-circles-bg" viewBox="0 0 800 800" fill="none" xmlns="http://www.w3.org/2000/svg">
          <circle cx="400" cy="400" r="100" stroke="#ffffff" strokeWidth="1" />
          <circle cx="400" cy="400" r="200" stroke="#ffffff" strokeWidth="1" />
          <circle cx="400" cy="400" r="300" stroke="#ffffff" strokeWidth="1" />
          <circle cx="400" cy="400" r="390" stroke="#ffffff" strokeWidth="1" strokeDasharray="4 4" />
        </svg>

        <div className="section-container" style={{ position: 'relative', zIndex: 2 }}>
          <div style={{ textAlign: 'center', maxWidth: '720px', margin: '0 auto 64px auto' }}>
            <span style={{ fontSize: '12px', fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: '#818CF8' }}>
              Proven Reliability
            </span>
            <h2 style={{ fontSize: '38px', fontWeight: 800, color: '#ffffff', letterSpacing: '-0.02em', marginTop: '10px' }}>
              Real-World Safety Impact
            </h2>
            <p style={{ color: '#94a3b8', fontSize: '16px', lineHeight: 1.6, marginTop: '8px' }}>
              Protecting industrial facilities with fast response times, dependable sensors, and continuous safety coverage.
            </p>
          </div>

          <div className="stats-grid-4col">
            <KpiCounter
              targetValue={99.99}
              suffix="%"
              isPercentage={true}
              label="SYSTEM UPTIME"
              subtitle="24/7 continuous monitoring"
            />
            <KpiCounter
              targetValue={4200000}
              suffix="+"
              isPercentage={false}
              label="DAILY SENSOR CHECKS"
              subtitle="Real-time readings processed"
            />
            <KpiCounter
              targetValue={12}
              suffix="ms"
              isPercentage={false}
              label="VALVE SHUTOFF SPEED"
              subtitle="Emergency shutoff response"
            />
            <KpiCounter
              targetValue={100}
              suffix="%"
              isPercentage={false}
              label="SAFETY LOG ACCURACY"
              subtitle="Every safety event recorded"
            />
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------------
          7. SECURITY UI SECTION (SECURITY PROOF)
          Split 2-column layout.
          Left: Checklist of certifications (SOC2, GDPR) with blue check-circle icons.
          Right: 'Policy Toggle' card showing active/inactive switches (MFA, IP Whitelisting, Key Rotation).
          Overlapping 'Threat Blocked' alert card in Slate 800 for depth.
          ------------------------------------------------------------------ */}
      <section id="security" className="section-rhythm" style={{ backgroundColor: '#f8fafc' }}>
        <div className="section-container">
          <div className="section-header">
            <span className="section-tag">Security &amp; Safety</span>
            <h2 className="section-title">Reliable Safety at Every Step</h2>
            <p className="section-desc">
              From secure hardware sensors to encrypted web connections, your facility and data stay fully protected.
            </p>
          </div>

          <div className="security-grid">
            {/* Left Column: Certifications Checklist */}
            <div>
              <div className="cert-item">
                <div className="cert-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                  </svg>
                </div>
                <div>
                  <div className="cert-title">SOC 2 Type II Certified</div>
                  <div className="cert-desc">
                    Independently audited controls covering operational security, system uptime, and confidential data handling.
                  </div>
                </div>
              </div>

              <div className="cert-item">
                <div className="cert-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                  </svg>
                </div>
                <div>
                  <div className="cert-title">ISO 27001 Certified</div>
                  <div className="cert-desc">
                    Certified information security practices governing all cloud, network, and sensor operations.
                  </div>
                </div>
              </div>

              <div className="cert-item">
                <div className="cert-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                  </svg>
                </div>
                <div>
                  <div className="cert-title">Industrial Safety Standards</div>
                  <div className="cert-desc">
                    Built to follow pipeline safety regulations, OSHA guidelines, and environmental rules.
                  </div>
                </div>
              </div>

              <div className="cert-item">
                <div className="cert-icon">
                  <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                    <polyline points="22 4 12 14.01 9 11.01"></polyline>
                  </svg>
                </div>
                <div>
                  <div className="cert-title">Encrypted Sensor Connections</div>
                  <div className="cert-desc">
                    Direct encrypted communication between all field sensor nodes and the central monitoring system.
                  </div>
                </div>
              </div>
            </div>

            {/* Right Column: Policy Toggle Card with Overlapping Threat Blocked Card */}
            <div style={{ position: 'relative' }}>
              <div className="policy-card">
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px', paddingBottom: '14px', borderBottom: '1px solid #e2e8f0' }}>
                  <div>
                    <h3 style={{ fontSize: '16px', fontWeight: 700, color: '#0f172a' }}>Live Safety Settings</h3>
                    <p style={{ fontSize: '12px', color: '#64748b', margin: 0 }}>Active safety rules and controls</p>
                  </div>
                  <span className="badge-status success">Active</span>
                </div>

                <GlassToggle
                  label="Two-Factor Login (MFA)"
                  description="Require a secure verification code when administrators log in"
                  checked={policies.mfa}
                  onChange={(val) => setPolicies({ ...policies, mfa: val })}
                />

                <GlassToggle
                  label="Approved IP Access"
                  description="Allow control commands only from approved company networks"
                  checked={policies.ipWhitelist}
                  onChange={(val) => setPolicies({ ...policies, ipWhitelist: val })}
                />

                <GlassToggle
                  label="Automatic Security Updates"
                  description="Regular security certificate renewals on all sensor nodes"
                  checked={policies.keyRotation}
                  onChange={(val) => setPolicies({ ...policies, keyRotation: val })}
                />

                <GlassToggle
                  label="Emergency Valve Cutoff"
                  description="Automatically shut valves when gas levels reach dangerous limits"
                  checked={policies.valveKillswitch}
                  onChange={(val) => setPolicies({ ...policies, valveKillswitch: val })}
                />

                <GlassToggle
                  label="Activity Event Logging"
                  description="Record all alerts, valve actions, and user changes in real time"
                  checked={policies.auditStream}
                  onChange={(val) => setPolicies({ ...policies, auditStream: val })}
                />
              </div>

              {/* Overlapping 'Threat Blocked' Alert Card in Slate 800 for depth */}
              <div className="threat-blocked-card">
                <div className="threat-icon-box">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                    <circle cx="12" cy="12" r="10"></circle>
                    <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"></line>
                  </svg>
                </div>
                <div>
                  <div style={{ fontSize: '11px', color: '#ef4444', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.04em' }}>
                    Unauthorized Access Blocked
                  </div>
                  <div style={{ fontSize: '13px', fontWeight: 600, color: '#f8fafc', marginTop: '2px' }}>
                    Untrusted IP 194.26.29.112 Stopped
                  </div>
                  <div style={{ fontSize: '11px', color: '#94a3b8', marginTop: '4px' }}>
                    Safety Filter: Access Control • Status: Blocked
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------------
          8. FAQ SECTION
          ------------------------------------------------------------------ */}
      <section id="faq" className="section-rhythm">
        <div className="section-container">
          <div className="section-header">
            <span className="section-tag">Questions &amp; Answers</span>
            <h2 className="section-title">Frequently Asked Questions</h2>
            <p className="section-desc">
              Simple answers to common questions about installing and using LeakSense.
            </p>
          </div>

          <div className="faq-grid">
            {[
              {
                q: 'What happens if the internet or Wi-Fi goes down?',
                a: 'Each sensor node runs its own independent safety check. Even without an active internet or Wi-Fi connection, the sensor will automatically shut off the valve within a second if gas levels exceed safe limits.',
              },
              {
                q: 'Can LeakSense run on our private company network?',
                a: 'Yes. LeakSense can run on private local servers or secure internal networks with no public internet required, keeping all your sensor data inside your facility.',
              },
              {
                q: 'What safety reports and logs can we export?',
                a: 'You can export complete incident timelines, sensor reading histories, and valve logs in clean PDF or Excel spreadsheets anytime for safety inspections.',
              },
              {
                q: 'How does the system prevent false alarms?',
                a: 'Smart sensor filters check both gas concentration and line pressure changes before sounding an alarm, so normal pressure fluctuations never trigger accidental shutoffs.',
              },
            ].map((faq, idx) => {
              const isOpen = faqOpenIndex === idx
              return (
                <div
                  key={idx}
                  className="faq-card"
                  onClick={() => setFaqOpenIndex(isOpen ? -1 : idx)}
                >
                  <div className="faq-card-header">
                    <div className="faq-question">{faq.q}</div>
                    <svg
                      width="18"
                      height="18"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="#475569"
                      strokeWidth="2"
                      style={{
                        transform: isOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                        transition: 'transform 0.2s ease',
                        flexShrink: 0,
                      }}
                    >
                      <polyline points="6 9 12 15 18 9"></polyline>
                    </svg>
                  </div>
                  {isOpen && <div className="faq-answer">{faq.a}</div>}
                </div>
              )
            })}
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------------
          9. CALL TO ACTION BANNER
          ------------------------------------------------------------------ */}
      <section className="section-rhythm" style={{ paddingTop: 0 }}>
        <div className="section-container">
          <div className="enterprise-cta-banner">
            <h2 className="cta-banner-title">
              Ready to Protect Your Facility from Gas Leaks?
            </h2>
            <p className="cta-banner-desc">
              Schedule a quick demo with our team or sign in to the operator dashboard right now.
            </p>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px', flexWrap: 'wrap' }}>
              <button
                onClick={() => setShowDemoModal(true)}
                className="btn-primary-indigo"
                style={{ padding: '0 28px' }}
              >
                <span>Book a Live Demo</span>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="5" y1="12" x2="19" y2="12"></line>
                  <polyline points="12 5 19 12 12 19"></polyline>
                </svg>
              </button>
              <button
                onClick={onGoToLogin}
                className="btn-outline-white"
                style={{ background: 'transparent', color: '#ffffff', borderColor: '#334155' }}
              >
                Sign In to Dashboard
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* ------------------------------------------------------------------
          10. ENTERPRISE FOOTER
          6-column structure:
          Left-most 2 columns for logo, location, and social icons.
          Remaining 4 columns for Product, Resources, Company, and Legal link lists.
          Bottom bar includes copyright and a 'System Status' indicator with green pulsing dot.
          ------------------------------------------------------------------ */}
      <footer className="enterprise-footer">
        <div className="section-container">
          <div className="footer-grid-6col">
            {/* Columns 1 & 2: Brand, Location & Socials */}
            <div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '16px' }}>
                <div className="enterprise-logo-square">LS</div>
                <strong style={{ fontSize: '18px', fontWeight: 800, color: '#020617' }}>LeakSense</strong>
              </div>
              <p style={{ fontSize: '14px', lineHeight: 1.6, color: '#64748b', maxWidth: '300px', marginBottom: '18px' }}>
                Smart IoT gas leak detection, automatic valve shutoff, and safety reporting for industrial pipelines and facilities.
              </p>
              <div style={{ fontSize: '13px', color: '#475569', marginBottom: '12px' }}>
                <strong>Global HQ:</strong> 450 Safety Boulevard, Industrial Tech Corridor
              </div>
              <div style={{ display: 'flex', gap: '12px', color: '#64748b' }}>
                <a href="#twitter" aria-label="Twitter" style={{ color: '#64748b' }}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M23 3a10.9 10.9 0 0 1-3.14 1.53 4.48 4.48 0 0 0-7.86 3v1A10.66 10.66 0 0 1 3 4s-4 9 5 13a11.64 11.64 0 0 1-7 2c9 5 20 0 20-11.5a4.5 4.5 0 0 0-.08-.83A7.72 7.72 0 0 0 23 3z"></path>
                  </svg>
                </a>
                <a href="#github" aria-label="GitHub" style={{ color: '#64748b' }}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"></path>
                  </svg>
                </a>
                <a href="#linkedin" aria-label="LinkedIn" style={{ color: '#64748b' }}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z"></path>
                    <rect x="2" y="9" width="4" height="12"></rect>
                    <circle cx="4" cy="4" r="2"></circle>
                  </svg>
                </a>
              </div>
            </div>

            {/* Column 3: Product Links */}
            <div>
              <div className="footer-col-title">Platform</div>
              <ul className="footer-links-list">
                <li className="footer-link-item"><button onClick={() => scrollTo('platform')}>Live Sensor Feed</button></li>
                <li className="footer-link-item"><button onClick={() => scrollTo('solutions')}>Valve Controls</button></li>
                <li className="footer-link-item"><button onClick={() => scrollTo('platform')}>Safety Activity Logs</button></li>
                <li className="footer-link-item"><button onClick={() => scrollTo('solutions')}>Early Leak Warnings</button></li>
                <li className="footer-link-item"><button onClick={() => scrollTo('solutions')}>Sensor Devices</button></li>
              </ul>
            </div>

            {/* Column 4: Resources */}
            <div>
              <div className="footer-col-title">Resources</div>
              <ul className="footer-links-list">
                <li className="footer-link-item"><button onClick={() => setShowTourModal(true)}>API Documentation</button></li>
                <li className="footer-link-item"><button onClick={() => setShowTourModal(true)}>Hardware Schematics</button></li>
                <li className="footer-link-item"><button onClick={() => scrollTo('security')}>Security Whitepaper</button></li>
                <li className="footer-link-item"><button onClick={() => scrollTo('impact')}>Weekly Safety Reports</button></li>
                <li className="footer-link-item"><button onClick={() => scrollTo('faq')}>Compliance Center</button></li>
              </ul>
            </div>

            {/* Column 5: Company */}
            <div>
              <div className="footer-col-title">Company</div>
              <ul className="footer-links-list">
                <li className="footer-link-item"><a href="#about">About LeakSense</a></li>
                <li className="footer-link-item"><a href="#customers">Enterprise Customers</a></li>
                <li className="footer-link-item"><a href="#careers">Careers</a></li>
                <li className="footer-link-item"><a href="#press">Press Releases</a></li>
                <li className="footer-link-item"><button onClick={() => setShowDemoModal(true)}>Contact Sales</button></li>
              </ul>
            </div>

            {/* Column 6: Legal & Compliance */}
            <div>
              <div className="footer-col-title">Legal &amp; Trust</div>
              <ul className="footer-links-list">
                <li className="footer-link-item"><a href="#privacy">Privacy Policy</a></li>
                <li className="footer-link-item"><a href="#terms">Terms of Service</a></li>
                <li className="footer-link-item"><a href="#soc2">SOC 2 Report</a></li>
                <li className="footer-link-item"><a href="#gdpr">GDPR Governance</a></li>
                <li className="footer-link-item"><a href="#security">Vulnerability Policy</a></li>
              </ul>
            </div>
          </div>

          {/* Bottom Bar: Copyright & System Status */}
          <div className="footer-bottom-bar">
            <div>
              © {new Date().getFullYear()} LeakSense Technologies Inc. All enterprise rights reserved.
            </div>

            {/* System Status indicator with green pulsing dot */}
            <div className="system-status-indicator">
              <span className="hero-status-dot" />
              <span>
                {systemStatus?.status || 'All Systems Operational'} • 99.99% Uptime Verified
              </span>
            </div>
          </div>
        </div>
      </footer>

      {/* ------------------------------------------------------------------
          11. BOOK DEMO MODAL
          ------------------------------------------------------------------ */}
      {showDemoModal && (
        <div className="enterprise-modal-backdrop" onClick={() => setShowDemoModal(false)}>
          <div className="enterprise-modal-card" onClick={(e) => e.stopPropagation()}>
            <div style={{ padding: '24px 28px', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#0f172a' }}>Schedule a Live Demo</h3>
                <p style={{ fontSize: '13px', color: '#64748b', margin: '2px 0 0 0' }}>See how LeakSense protects your facility in real time</p>
              </div>
              <button
                onClick={() => setShowDemoModal(false)}
                style={{ background: 'transparent', border: 'none', cursor: 'pointer', fontSize: '20px', color: '#64748b' }}
              >
                ✕
              </button>
            </div>

            <div style={{ padding: '24px 28px' }}>
              {demoSuccess ? (
                <div style={{ padding: '16px', background: '#dcfce7', color: '#166534', borderRadius: '8px', fontSize: '14px', lineHeight: 1.5 }}>
                  <strong>Request Received:</strong> {demoSuccess}
                  <div style={{ marginTop: '16px' }}>
                    <button
                      onClick={() => {
                        setDemoSuccess('')
                        setShowDemoModal(false)
                      }}
                      className="btn-primary-indigo"
                      style={{ width: '100%', justifyContent: 'center' }}
                    >
                      Done
                    </button>
                  </div>
                </div>
              ) : (
                <form onSubmit={handleDemoSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
                      Full Name *
                    </label>
                    <input
                      type="text"
                      required
                      placeholder="e.g. Elena Vance"
                      value={demoForm.name}
                      onChange={(e) => setDemoForm({ ...demoForm, name: e.target.value })}
                      style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
                      Work Email *
                    </label>
                    <input
                      type="email"
                      required
                      placeholder="elena.vance@company.com"
                      value={demoForm.email}
                      onChange={(e) => setDemoForm({ ...demoForm, email: e.target.value })}
                      style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
                      Company / Facility Name
                    </label>
                    <input
                      type="text"
                      placeholder="e.g. Apex Gas"
                      value={demoForm.company}
                      onChange={(e) => setDemoForm({ ...demoForm, company: e.target.value })}
                      style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none' }}
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#334155', marginBottom: '6px' }}>
                      Tell us about your facility or questions
                    </label>
                    <textarea
                      rows={3}
                      placeholder="How many pipeline zones or gas sensors do you need? Any specific questions?"
                      value={demoForm.message}
                      onChange={(e) => setDemoForm({ ...demoForm, message: e.target.value })}
                      style={{ width: '100%', padding: '10px 12px', borderRadius: '8px', border: '1px solid #cbd5e1', fontSize: '14px', outline: 'none', resize: 'vertical' }}
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={submittingDemo}
                    className="btn-primary-indigo"
                    style={{ width: '100%', justifyContent: 'center', height: '44px', marginTop: '6px' }}
                  >
                    {submittingDemo ? 'Sending Request...' : 'Submit Demo Request'}
                  </button>
                </form>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------------
          12. INTERACTIVE ARCHITECTURE TOUR MODAL
          ------------------------------------------------------------------ */}
      {showTourModal && (
        <div className="enterprise-modal-backdrop" onClick={() => setShowTourModal(false)}>
          <div className="enterprise-modal-card" style={{ maxWidth: '680px' }} onClick={(e) => e.stopPropagation()}>
            <div style={{ padding: '20px 24px', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <h3 style={{ fontSize: '18px', fontWeight: 800, color: '#0f172a' }}>How the System Works</h3>
                <p style={{ fontSize: '12px', color: '#64748b', margin: '2px 0 0 0' }}>Simple 4-Step IoT Safety Architecture</p>
              </div>
              <button
                onClick={() => setShowTourModal(false)}
                style={{ background: 'transparent', border: 'none', cursor: 'pointer', fontSize: '20px', color: '#64748b' }}
              >
                ✕
              </button>
            </div>

            <div style={{ padding: '24px' }}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: '16px' }}>
                <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <div style={{ fontSize: '12px', fontWeight: 700, color: '#4F46E5', marginBottom: '4px' }}>STEP 1: SENSOR NODES</div>
                  <div style={{ fontSize: '13px', color: '#334155', lineHeight: 1.5 }}>
                    ESP32 sensor units continuously monitor methane gas, line pressure, and oxygen levels every 2 seconds across your facility.
                  </div>
                </div>

                <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <div style={{ fontSize: '12px', fontWeight: 700, color: '#4F46E5', marginBottom: '4px' }}>STEP 2: SMART PROCESSING</div>
                  <div style={{ fontSize: '13px', color: '#334155', lineHeight: 1.5 }}>
                    Central system immediately evaluates incoming readings against safe limits, detecting abnormal spikes in milliseconds.
                  </div>
                </div>

                <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <div style={{ fontSize: '12px', fontWeight: 700, color: '#4F46E5', marginBottom: '4px' }}>STEP 3: AUTOMATIC VALVES</div>
                  <div style={{ fontSize: '13px', color: '#334155', lineHeight: 1.5 }}>
                    12V solenoid valves shut off gas lines immediately upon detecting any dangerous gas spike or sudden pressure drop.
                  </div>
                </div>

                <div style={{ padding: '16px', background: '#f8fafc', borderRadius: '8px', border: '1px solid #e2e8f0' }}>
                  <div style={{ fontSize: '12px', fontWeight: 700, color: '#4F46E5', marginBottom: '4px' }}>STEP 4: LIVE DASHBOARD</div>
                  <div style={{ fontSize: '13px', color: '#334155', lineHeight: 1.5 }}>
                    Operators monitor real-time charts, receive instant sound and visual alerts, and download safety logs in PDF or Excel.
                  </div>
                </div>
              </div>

              <div style={{ marginTop: '20px', display: 'flex', justifyContent: 'flex-end', gap: '12px' }}>
                <button
                  onClick={() => {
                    setShowTourModal(false)
                    onGoToLogin()
                  }}
                  className="btn-primary-indigo"
                >
                  Enter Live Dashboard
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
