import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ReferenceLine,
  ResponsiveContainer,
} from 'recharts'
import { TrendingUpIcon } from './icons/AppIcons'

function GasChart({ readings, height = 360 }) {
  const data = [...readings].reverse().map((r) => ({
    time: new Date(r.recordedAt).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    }),
    methane: r.methanePpm,
    risk: r.riskScore,
  }))

  return (
    <div
      style={{
        background: 'var(--panel)',
        borderRadius: '12px',
        padding: '20px',
        border: '1px solid var(--border)',
        boxShadow: 'var(--shadow-sm)',
        width: '100%',
        boxSizing: 'border-box',
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '16px',
          flexWrap: 'wrap',
          gap: '8px',
        }}
      >
        <h2 style={{ fontSize: '18px', margin: 0, display: 'flex', alignItems: 'center', gap: '8px' }}>
          <TrendingUpIcon size={18} />
          <span>Live Gas Readings & Risk Telemetry</span>
        </h2>
        <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
          {data.length} telemetry points · Real-time pipeline stream
        </span>
      </div>

      {data.length === 0 ? (
        <div style={{ height: `${height}px`, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
          No readings yet. Awaiting sensor telemetry packets...
        </div>
      ) : (
        <ResponsiveContainer width="100%" height={height}>
          <LineChart data={data} margin={{ top: 10, right: 30, left: 10, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
            <XAxis dataKey="time" stroke="var(--text-muted)" minTickGap={30} />
            <YAxis yAxisId="left" stroke="#14b8a6" width={40} />
            <YAxis
                          yAxisId="right"
                          orientation="right"
                          domain={[0, 100]}
                          stroke="#f59e0b"
                          width={35}
                        />
            <Tooltip
              contentStyle={{ background: 'var(--panel-2)', border: '1px solid var(--border)' }}
            />
            <Legend />
            <ReferenceLine
              yAxisId="left"
              y={200}
              stroke="#f59e0b"
              strokeDasharray="4 4"
              label={{ value: 'Warn: 200', fill: '#f59e0b', fontSize: 10, position: 'insideTopLeft' }}
            />
            <ReferenceLine
              yAxisId="left"
              y={350}
              stroke="#ef4444"
              strokeDasharray="4 4"
              label={{ value: 'Crit: 350', fill: '#ef4444', fontSize: 10, position: 'insideTopLeft' }}
            />
            <Line
              yAxisId="left"
              type="monotone"
              dataKey="methane"
              name="Methane (ppm)"
              stroke="#14b8a6"
              strokeWidth={2}
            />
            <Line
              yAxisId="right"
              type="monotone"
              dataKey="risk"
              name="Risk score"
              stroke="#f59e0b"
              strokeWidth={2}
            />
          </LineChart>
        </ResponsiveContainer>
      )}
    </div>
  )
}

export default GasChart