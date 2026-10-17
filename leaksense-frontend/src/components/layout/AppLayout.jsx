import { useEffect, useState } from 'react'
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useData } from '../../context/DataContext'
import { APP_CONFIG } from '../../config/appConfig'
import { BrandIcon, RailEdgeToggleIcon, MobileMenuIcon, NavIcon, LogoutIcon } from '../icons/AppIcons'
import ZoneFilter from '../ZoneFilter'
import AlarmBanner from '../AlarmBanner'
import NotificationBell from '../NotificationBell'
import AiChatWidget from '../AiChatWidget'

function useIsMobile() {
  const query = '(max-width: 800px)'
  const [isMobile, setIsMobile] = useState(() => window.matchMedia(query).matches)

  useEffect(() => {
    const media = window.matchMedia(query)
    const handler = (e) => setIsMobile(e.matches)
    media.addEventListener('change', handler)
    return () => media.removeEventListener('change', handler)
  }, [])

  return isMobile
}

function AppLayout({ user, onLogout }) {
  const navigate = useNavigate()
  const { live, error } = useData()
  const { pathname } = useLocation()
  const isMobile = useIsMobile()

  const handleLogoutClick = () => {
    navigate('/', { replace: true })
    if (window.location.pathname !== '/' && window.location.pathname !== '') {
      window.history.replaceState(null, '', '/')
    }
    onLogout()
  }

  const [collapsed, setCollapsed] = useState(
    () => localStorage.getItem('leaksense_sidebar') === 'collapsed'
  )
  const [drawerOpen, setDrawerOpen] = useState(false)

  const menu = user.role === 'ADMIN'
    ? [...APP_CONFIG.nav, ...APP_CONFIG.adminNav]
    : APP_CONFIG.nav

  const current = menu.find((l) => l.to === pathname) || menu[0]
  const showFilter = ['/', '/alerts', '/analytics', '/sensors', '/valves'].includes(pathname)

  const toggleMenu = () => {
    if (isMobile) {
      setDrawerOpen(!drawerOpen)
      return
    }
    const next = !collapsed
    setCollapsed(next)
    localStorage.setItem('leaksense_sidebar', next ? 'collapsed' : 'expanded')
  }

  const shellClass = [
    'app-shell',
    !isMobile && collapsed ? 'collapsed' : '',
    isMobile && drawerOpen ? 'drawer-open' : '',
  ]
    .filter(Boolean)
    .join(' ')

  return (
    <div className={shellClass}>
      <div className="sidebar-backdrop" onClick={() => setDrawerOpen(false)} />

      <aside className="sidebar">
        {/* Option 2: Floating Rail Edge Toggle Button */}
        {!isMobile && (
          <button
            className="sidebar-edge-toggle"
            onClick={toggleMenu}
            aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
            title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
          >
            <RailEdgeToggleIcon collapsed={collapsed} size={14} />
          </button>
        )}

        {/* Clean, uncrowded brand header */}
        <div
          className="sidebar-brand"
          onClick={() => collapsed && toggleMenu()}
          title={collapsed ? 'Click to expand sidebar' : undefined}
          style={{ cursor: collapsed ? 'pointer' : 'default' }}
        >
          <BrandIcon size={28} />
          <div className="brand-text">
            <strong>{APP_CONFIG.brand.name}</strong>
            <small>{APP_CONFIG.brand.subtitle}</small>
          </div>
        </div>

        <p className="sidebar-section">{APP_CONFIG.workspace.rootName.toUpperCase()}</p>
        <nav>
          {menu.map((l) => (
            <NavLink
              key={l.to}
              to={l.to}
              end
              title={l.label}
              onClick={() => setDrawerOpen(false)}
              className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
            >
              <NavIcon name={l.iconName} size={18} />
              <span className="nav-label">{l.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="sidebar-user">
          <div className="sidebar-user-row">
            <div className="avatar">{user.username.slice(0, 2).toUpperCase()}</div>
            <div className="user-text">
              <div style={{ fontSize: '14px', fontWeight: 'bold' }}>{user.fullName}</div>
              <div style={{ fontSize: '11px', color: 'var(--text-faint)' }}>ROLE_{user.role}</div>
            </div>
          </div>
          <button className="logout-btn" onClick={handleLogoutClick} title="Log out" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px' }}>
            <LogoutIcon size={16} />
            <span className="nav-label">Log out</span>
          </button>
        </div>
      </aside>

      <div className="main-area">
        <header className="topbar">
          <div className="topbar-left">
            {/* Mobile-only menu toggle */}
            {isMobile && (
              <button
                className="hamburger mobile-toggle"
                onClick={toggleMenu}
                aria-label="Toggle navigation drawer"
                aria-expanded={drawerOpen}
              >
                <MobileMenuIcon size={20} />
              </button>
            )}

            {/* Clean, un-cluttered breadcrumb */}
            <div className="breadcrumb">
              <span className="crumb-root">{APP_CONFIG.workspace.rootName} / </span>
              <strong>{current.title}</strong>
            </div>
          </div>

          <div className="topbar-right" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            {showFilter && <ZoneFilter />}
            <NotificationBell />
            <span
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '6px',
                background: live ? 'var(--ok-bg)' : 'var(--panel-2)',
                border: `1px solid ${live ? 'var(--ok)' : 'var(--border)'}`,
                color: live ? 'var(--ok)' : 'var(--text-faint)',
                borderRadius: '9999px',
                padding: '4px 12px',
                fontSize: '11px',
                fontWeight: 700,
                letterSpacing: '0.02em',
              }}
            >
              <span
                style={{
                  width: '6px',
                  height: '6px',
                  borderRadius: '50%',
                  backgroundColor: live ? '#16a34a' : 'var(--text-faint)',
                  boxShadow: live ? '0 0 0 2px rgba(22, 163, 74, 0.25)' : 'none',
                }}
              />
              {live ? 'System Online' : 'Telemetry Offline'}
            </span>
            <span
              style={{
                background: 'var(--primary-dark, #1E1B4B)',
                color: '#ffffff',
                border: '1px solid var(--primary-border, #C7D2FE)',
                borderRadius: '9999px',
                padding: '4px 10px',
                fontSize: '11px',
                fontWeight: 700,
                letterSpacing: '0.04em',
              }}
            >
              {user.role}
            </span>
          </div>
        </header>

        <main className="page">
          <AlarmBanner />
          {error && <p style={{ color: '#ef4444', marginBottom: '16px' }}>{error}</p>}
          <Outlet />
        </main>
      </div>

      {/* Floating AI Safety Assistant Widget */}
      <AiChatWidget />
    </div>
  )
}

export default AppLayout