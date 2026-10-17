import { useEffect, useState } from 'react'
import PageHeader from '../components/PageHeader'
import { getThresholds, updateThresholds } from '../api/leaksenseApi'
import { FlameIcon, GaugeIcon, WindIcon, WrenchIcon, CpuIcon } from '../components/icons/AppIcons'

const defaults = {
  methaneWarningPpm: 200,
  methaneLimitPpm: 350,
  pressureLowCriticalKpa: 420,
  pressureLowWarningKpa: 440,
  pressureNormalKpa: 460,
  pressureHighWarningKpa: 480,
  pressureHighCriticalKpa: 500,
  pressureToleranceKpa: 40,
  oxygenLowWarning: 20,
  minSafeOxygen: 19.5,
  methaneMinValid: 0,
  methaneMaxValid: 10000,
  pressureMinValid: 15,
  pressureMaxValid: 700,
  oxygenMinValid: 0,
  oxygenMaxValid: 25,
  warningScore: 30,
  criticalScore: 60,
}

const groups = [
  {
    short: 'Methane',
    Icon: FlameIcon,
    title: 'Methane limits (ppm)',
    note: 'MQ-4 sensor. A leak alert is raised when methane goes above these levels.',
    fields: [
      { key: 'methaneWarningPpm', label: 'Warning above', hint: 'Must be below the critical limit.' },
      { key: 'methaneLimitPpm', label: 'Critical above', hint: 'Allowed: 1 to 5000.' },
    ],
  },
  {
    short: 'Pressure',
    Icon: GaugeIcon,
    title: 'Pressure limits (kPa)',
    note: 'MPX5700 sensor. A pressure drop can mean a leak, and a rise can mean a blockage.',
    fields: [
      { key: 'pressureLowCriticalKpa', label: 'Critical below', hint: 'Lowest of all the limits.' },
      { key: 'pressureLowWarningKpa', label: 'Warning below', hint: 'Above the critical-low level.' },
      { key: 'pressureNormalKpa', label: 'Normal pressure', hint: 'Between the two warnings.' },
      { key: 'pressureHighWarningKpa', label: 'Warning above', hint: 'Above the normal pressure.' },
      { key: 'pressureHighCriticalKpa', label: 'Critical above', hint: 'Highest of all the limits.' },
      { key: 'pressureToleranceKpa', label: 'Tolerance (risk score)', hint: 'Drift that counts fully toward the risk score. Above 0.' },
    ],
  },
  {
    short: 'Oxygen',
    Icon: WindIcon,
    title: 'Oxygen limits (%)',
    note: 'MIX8410 sensor. Gas pushes oxygen out, so a falling level is dangerous.',
    fields: [
      { key: 'oxygenLowWarning', label: 'Warning below', hint: 'Above the critical level, at most 20.8.' },
      { key: 'minSafeOxygen', label: 'Critical below', hint: 'Allowed: 10 to 20.8.' },
    ],
  },
  {
    short: 'Sensor range',
    Icon: WrenchIcon,
    title: 'Sensor failure ranges',
    note: 'A reading outside these ranges is treated as a faulty sensor, not as a leak.',
    fields: [
      { key: 'methaneMinValid', label: 'Methane minimum (ppm)', hint: 'Below the maximum.' },
      { key: 'methaneMaxValid', label: 'Methane maximum (ppm)', hint: 'Above the critical limit.' },
      { key: 'pressureMinValid', label: 'Pressure minimum (kPa)', hint: 'Below every pressure limit.' },
      { key: 'pressureMaxValid', label: 'Pressure maximum (kPa)', hint: 'Above every pressure limit.' },
      { key: 'oxygenMinValid', label: 'Oxygen minimum (%)', hint: 'Below the critical oxygen level.' },
      { key: 'oxygenMaxValid', label: 'Oxygen maximum (%)', hint: 'Above normal air (20.9).' },
    ],
  },
  {
    short: 'Risk score',
    Icon: CpuIcon,
    title: 'Zone risk score',
    note: 'The warning and critical scores decide when a zone turns WARNING or CRITICAL.',
    fields: [
      { key: 'warningScore', label: 'Warning score', hint: 'Above 0 and below the critical score.' },
      { key: 'criticalScore', label: 'Critical score', hint: 'Maximum 100.' },
    ],
  },
]

const allFields = groups.flatMap((g) => g.fields.map((f) => ({ ...f, group: g.short })))

function validate(values) {
  const v = {}
  for (const f of allFields) {
    const n = Number(values[f.key])
    if (values[f.key] === '' || Number.isNaN(n)) {
      return `${f.group}: "${f.label}" must be a number`
    }
    v[f.key] = n
  }

  if (v.methaneLimitPpm < 1 || v.methaneLimitPpm > 5000) {
    return 'Methane limit must be between 1 and 5000 ppm'
  }
  if (v.pressureNormalKpa <= 0) return 'Normal pressure must be above 0 kPa'
  if (v.pressureToleranceKpa <= 0) return 'Pressure tolerance must be above 0 kPa'
  if (v.minSafeOxygen < 10 || v.minSafeOxygen > 20.8) {
    return 'Minimum safe oxygen must be between 10 and 20.8 percent'
  }
  if (v.warningScore <= 0 || v.criticalScore > 100 || v.warningScore >= v.criticalScore) {
    return 'Scores must satisfy 0 < warning < critical <= 100'
  }
  if (v.methaneWarningPpm <= 0 || v.methaneWarningPpm >= v.methaneLimitPpm) {
    return 'Methane warning must be above 0 and below the critical methane limit'
  }
  if (
    !(
      v.pressureLowCriticalKpa < v.pressureLowWarningKpa &&
      v.pressureLowWarningKpa < v.pressureNormalKpa &&
      v.pressureNormalKpa < v.pressureHighWarningKpa &&
      v.pressureHighWarningKpa < v.pressureHighCriticalKpa
    )
  ) {
    return 'Pressure limits must satisfy: low critical < low warning < normal < high warning < high critical'
  }
  if (v.oxygenLowWarning <= v.minSafeOxygen || v.oxygenLowWarning > 20.8) {
    return 'Oxygen warning must be above the critical oxygen level and at most 20.8 percent'
  }
  if (v.methaneMinValid >= v.methaneMaxValid || v.methaneMaxValid <= v.methaneLimitPpm) {
    return 'Methane sensor range must be valid and above the critical limit'
  }
  if (v.pressureMinValid >= v.pressureLowCriticalKpa || v.pressureMaxValid <= v.pressureHighCriticalKpa) {
    return 'Pressure sensor range must contain all the pressure limits'
  }
  if (v.oxygenMinValid >= v.minSafeOxygen || v.oxygenMaxValid <= 20.9) {
    return 'Oxygen sensor range must contain the oxygen limits and normal air (20.9 percent)'
  }
  return ''
}

function SettingsPage() {
  const [form, setForm] = useState(null)
  const [info, setInfo] = useState(null)
  const [message, setMessage] = useState(null)
  const [saving, setSaving] = useState(false)

  const applyServerValues = (data) => {
    const next = {}
    for (const f of allFields) next[f.key] = String(data[f.key])
    setForm(next)
    setInfo({ updatedBy: data.updatedBy, updatedAt: data.updatedAt })
  }

  useEffect(() => {
    getThresholds()
      .then((res) => applyServerValues(res.data))
      .catch(() => setMessage({ type: 'error', text: 'Could not load the settings.' }))
  }, [])

  const setField = (key) => (e) => setForm({ ...form, [key]: e.target.value })

  const restoreDefaults = () => {
    const next = {}
    for (const f of allFields) next[f.key] = String(defaults[f.key])
    setForm(next)
    setMessage({ type: 'ok', text: 'Default values filled in. Click Save to apply them.' })
  }

  const handleSave = async (e) => {
    e.preventDefault()

    const problem = validate(form)
    if (problem) {
      setMessage({ type: 'error', text: problem })
      window.scrollTo({ top: 0, behavior: 'smooth' })
      return
    }

    const payload = {}
    for (const f of allFields) payload[f.key] = Number(form[f.key])

    setSaving(true)
    try {
      const res = await updateThresholds(payload)
      applyServerValues(res.data)
      setMessage({ type: 'ok', text: 'Settings saved. New readings use these limits right away.' })
    } catch (err) {
      setMessage({
        type: 'error',
        text: err.response?.data?.message || 'Could not save the settings.',
      })
    } finally {
      setSaving(false)
      window.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="ADMINISTRATION"
        title="Alert Settings"
        subtitle="Set the limits that raise alerts, flag faulty sensors and decide each zone's risk level"
      />

      {message && <div className={`notice ${message.type}`}>{message.text}</div>}

      {!form ? (
        <div className="panel">
          <p style={{ color: 'var(--text-muted)' }}>Loading...</p>
        </div>
      ) : (
        <form onSubmit={handleSave}>
          {groups.map((g) => {
            const GroupIcon = g.Icon
            return (
              <div className="panel" key={g.short} style={{ marginBottom: '16px' }}>
                <h2 style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  {GroupIcon && <GroupIcon size={18} />}
                  <span>{g.title}</span>
                </h2>
                <p style={{ color: 'var(--text-muted)', fontSize: '13px', marginBottom: '14px' }}>
                  {g.note}
                </p>

              <div className="form-grid">
                {g.fields.map((f) => (
                  <div key={f.key}>
                    <label className="login-label">{f.label}</label>
                    <input
                      className="login-input"
                      type="number"
                      step="any"
                      value={form[f.key]}
                      onChange={setField(f.key)}
                    />
                    <p className="field-hint">{f.hint}</p>
                  </div>
                ))}
              </div>
              </div>
            )
          })}

          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap' }}>
            <button className="btn btn-primary" type="submit" disabled={saving}>
              {saving ? 'Saving...' : 'Save settings'}
            </button>
            <button className="btn btn-ghost" type="button" onClick={restoreDefaults}>
              Restore defaults
            </button>
          </div>

          {info && (
            <p style={{ color: 'var(--text-faint)', fontSize: '12px', marginTop: '14px' }}>
              Last updated by {info.updatedBy} on {new Date(info.updatedAt).toLocaleString()}
            </p>
          )}
        </form>
      )}
    </>
  )
}

export default SettingsPage