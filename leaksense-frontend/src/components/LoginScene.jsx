const nodes = [
  { x: 90, label: 'NODE-A1', delay: 0, leak: false },
  { x: 220, label: 'NODE-A2', delay: 0.8, leak: false },
  { x: 430, label: 'NODE-B1', delay: 1.6, leak: true },
]

function LoginScene() {
  return (
    <svg
      className="login-scene"
      viewBox="0 0 520 280"
      role="img"
      aria-label="Animated illustration: a sensor detects a gas leak, an alert appears and the valve closes automatically"
    >
      <rect x="20" y="170" width="480" height="40" rx="20" className="scene-pipe" />
      <line x1="40" y1="190" x2="305" y2="190" className="scene-flow" />
      <line x1="355" y1="190" x2="480" y2="190" className="scene-flow scene-flow-after" />

      <rect x="312" y="166" width="36" height="48" rx="6" className="scene-valve-body" />
      <rect x="325" y="118" width="10" height="44" rx="2" className="scene-gate" />
      <circle cx="330" cy="230" r="5" className="scene-valve-light" />
      <text x="330" y="250" textAnchor="middle" className="scene-label">VALVE</text>

      {nodes.map((n) => (
        <g key={n.label}>
          <line x1={n.x} y1="132" x2={n.x} y2="170" className="scene-stem" />
          <line x1={n.x} y1="106" x2={n.x} y2="96" className="scene-stem" />
          <circle
            cx={n.x}
            cy="94"
            r="5"
            className="scene-ring"
            style={{ animationDelay: `${n.delay}s` }}
          />
          <circle
            cx={n.x}
            cy="94"
            r="5"
            className="scene-ring"
            style={{ animationDelay: `${n.delay + 1.2}s` }}
          />
          <rect
            x={n.x - 18}
            y="106"
            width="36"
            height="26"
            rx="6"
            className={n.leak ? 'scene-node scene-node-leak' : 'scene-node'}
          />
          <text x={n.x} y="236" textAnchor="middle" className="scene-label">
            {n.label}
          </text>
        </g>
      ))}

      <text x="155" y="262" textAnchor="middle" className="scene-label">ZONE A</text>
      <text x="430" y="262" textAnchor="middle" className="scene-label">ZONE B</text>

      <g className="scene-leak">
        <circle cx="424" cy="166" r="6" className="scene-puff" style={{ animationDelay: '0s' }} />
        <circle cx="436" cy="166" r="5" className="scene-puff" style={{ animationDelay: '0.6s' }} />
        <circle cx="430" cy="166" r="7" className="scene-puff" style={{ animationDelay: '1.2s' }} />
      </g>

      <g className="scene-alert">
        <rect x="270" y="28" width="240" height="34" rx="17" fill="#ef4444" />
        <text x="390" y="50">LEAK DETECTED</text>
      </g>

      <g className="scene-closed">
        <rect x="270" y="28" width="240" height="34" rx="17" fill="#22c55e" />
        <text x="390" y="50">VALVE CLOSED · ALERT SENT</text>
      </g>
    </svg>
  )
}

export default LoginScene