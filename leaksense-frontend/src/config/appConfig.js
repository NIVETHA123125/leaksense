export const APP_CONFIG = {
  brand: {
    name: 'LeakSense',
    subtitle: 'Gas Leak Monitoring',
    tagline: 'Industrial Gas & Pipeline Safety Intelligence',
  },
  workspace: {
    rootName: 'Workspace',
  },
  nav: [
    { to: '/', label: 'Overview', iconName: 'overview', title: 'Overview' },
    { to: '/alerts', label: 'Alerts', iconName: 'alerts', title: 'Alerts & History' },
    { to: '/analytics', label: 'Analytics', iconName: 'analytics', title: 'Telemetry & Risk Analytics' },
    { to: '/sensors', label: 'Sensors', iconName: 'sensors', title: 'Sensor Health' },
    { to: '/valves', label: 'Valves', iconName: 'valves', title: 'Valve Control' },
    { to: '/reports', label: 'Reports', iconName: 'reports', title: 'Safety Reports & AI Audits' },
  ],
  adminNav: [
    { to: '/users', label: 'Users', iconName: 'users', title: 'User Management' },
    { to: '/settings', label: 'Settings', iconName: 'settings', title: 'Alert Settings' },
  ],
}
