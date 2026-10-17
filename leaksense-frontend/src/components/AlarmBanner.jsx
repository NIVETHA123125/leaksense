import { useEffect, useRef, useState } from 'react'
import { useData } from '../context/DataContext'
import { AlertIcon, BellOffIcon, VolumeIcon, VolumeOffIcon } from './icons/AppIcons'

const timeOf = (value) =>
  value ? new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''

function AlarmBanner() {
  const { alarms, silence, silenceAll } = useData()
  const [soundOn, setSoundOn] = useState(
    () => localStorage.getItem('leaksense_sound') !== 'off'
  )
  const [error, setError] = useState('')
  const audioRef = useRef(null)

  const active = alarms.filter((a) => a.status === 'ACTIVE')
  const silenced = alarms.filter((a) => a.status === 'SILENCED')
  const activeCount = active.length

  useEffect(() => {
    if (!soundOn || activeCount === 0) return undefined

    const beep = () => {
      try {
        const AudioContextClass = window.AudioContext || window.webkitAudioContext
        if (!audioRef.current) audioRef.current = new AudioContextClass()
        const ctx = audioRef.current
        if (ctx.state === 'suspended') ctx.resume()

        const oscillator = ctx.createOscillator()
        const gain = ctx.createGain()
        oscillator.type = 'square'
        oscillator.frequency.value = 880
        gain.gain.value = 0.05
        oscillator.connect(gain)
        gain.connect(ctx.destination)
        oscillator.start()
        oscillator.stop(ctx.currentTime + 0.25)
      } catch {
        // sound is optional: the banner still works without it
      }
    }

    beep()
    const timer = setInterval(beep, 1500)
    return () => clearInterval(timer)
  }, [soundOn, activeCount])

  const toggleSound = () => {
    const next = !soundOn
    setSoundOn(next)
    localStorage.setItem('leaksense_sound', next ? 'on' : 'off')
  }

  const run = async (action) => {
    setError('')
    try {
      await action()
    } catch (err) {
      setError(err.response?.data?.message || 'Could not silence the alarm.')
    }
  }

  if (alarms.length === 0) return null

  return (
    <div className={activeCount > 0 ? 'alarm-banner' : 'alarm-banner silenced-only'}>
      <div className={activeCount > 0 ? 'alarm-head pulse' : 'alarm-head'}>
        <span style={{ display: 'inline-flex', alignItems: 'center', gap: '8px' }}>
          {activeCount > 0 ? (
            <>
              <AlertIcon size={16} /> ALARM: {activeCount} active
            </>
          ) : (
            <>
              <BellOffIcon size={16} /> Alarm silenced: the danger may still be present
            </>
          )}
        </span>

        <div className="alarm-actions">
          <button
            className="btn btn-small btn-light"
            onClick={toggleSound}
            title="Turn the dashboard beep on or off"
            style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
          >
            {soundOn ? (
              <>
                <VolumeIcon size={14} /> Sound on
              </>
            ) : (
              <>
                <VolumeOffIcon size={14} /> Sound off
              </>
            )}
          </button>
          {activeCount > 1 && (
            <button
              className="btn btn-small btn-light"
              onClick={() => run(silenceAll)}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              <BellOffIcon size={14} /> Silence all
            </button>
          )}
        </div>
      </div>

      <div className="alarm-body">
        {error && <div className="notice error" style={{ margin: '10px 14px' }}>{error}</div>}

        {active.map((a) => (
          <div className="alarm-row" key={a.id}>
            <div>
              <strong style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
                <AlertIcon size={14} color="var(--err)" /> {a.zone?.name} · {a.node?.nodeCode} · {a.sensor}
              </strong>
              <p>{a.message}</p>
              <small>Since {timeOf(a.startedAt)}</small>
            </div>
            <button
              className="btn btn-warn"
              onClick={() => run(() => silence(a.id))}
              style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}
            >
              <BellOffIcon size={13} /> Silence
            </button>
          </div>
        ))}

        {silenced.map((a) => (
          <div className="alarm-row muted" key={a.id}>
            <span style={{ display: 'inline-flex', alignItems: 'center', gap: '6px' }}>
              <BellOffIcon size={14} color="var(--warn)" />
              <span>
                {a.zone?.name} · {a.node?.nodeCode} · {a.sensor}: silenced by{' '}
                <strong>{a.silencedBy}</strong> at {timeOf(a.silencedAt)}. It sounds again if the danger continues.
              </span>
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

export default AlarmBanner