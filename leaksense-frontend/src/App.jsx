import { useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import Login from './components/Login'
import LandingPage from './pages/LandingPage'
import { DataProvider } from './context/DataContext'
import AppLayout from './components/layout/AppLayout'
import OverviewPage from './pages/OverviewPage'
import AlertsPage from './pages/AlertsPage'
import SensorsPage from './pages/SensorsPage'
import ValvesPage from './pages/ValvesPage'
import AnalyticsPage from './pages/AnalyticsPage'
import ReportsPage from './pages/ReportsPage'
import UsersPage from './pages/UsersPage'
import SettingsPage from './pages/SettingsPage'

function readSavedUser() {
  try {
    // Clear any lingering localStorage auth credentials from previous runs
    localStorage.removeItem('leaksense_user')
    localStorage.removeItem('leaksense_token')

    const saved = sessionStorage.getItem('leaksense_user')
    const token = sessionStorage.getItem('leaksense_token')
    return saved && token ? JSON.parse(saved) : null
  } catch {
    return null
  }
}

function App() {
  const [user, setUser] = useState(readSavedUser)
  const [showLogin, setShowLogin] = useState(false)

  const handleLogin = (data) => {
    const loggedIn = {
      username: data.username,
      fullName: data.fullName,
      role: data.role,
    }
    // SessionStorage automatically clears when the tab/browser is closed
    sessionStorage.setItem('leaksense_token', data.token)
    sessionStorage.setItem('leaksense_user', JSON.stringify(loggedIn))
    localStorage.removeItem('leaksense_token')
    localStorage.removeItem('leaksense_user')

    // Always reset path to root ('/') so new logins always open the Overview page
    if (window.location.pathname !== '/' && window.location.pathname !== '') {
      window.history.replaceState(null, '', '/')
    }

    setUser(loggedIn)
    setShowLogin(false)
  }

  const handleLogout = () => {
    sessionStorage.removeItem('leaksense_token')
    sessionStorage.removeItem('leaksense_user')
    localStorage.removeItem('leaksense_token')
    localStorage.removeItem('leaksense_user')

    // Reset path to root ('/') on logout so next login starts on Overview page
    if (window.location.pathname !== '/' && window.location.pathname !== '') {
      window.history.replaceState(null, '', '/')
    }

    setUser(null)
    setShowLogin(false)
  }

  if (!user) {
    if (showLogin) {
      return <Login onLogin={handleLogin} onBack={() => setShowLogin(false)} />
    }
    return <LandingPage onGoToLogin={() => setShowLogin(true)} />
  }

  const isAdmin = user.role === 'ADMIN'
  const adminOnly = (page) => (isAdmin ? page : <Navigate to="/" replace />)

  return (
    <DataProvider user={user}>
      <BrowserRouter>
        <Routes>
          <Route path="/landing" element={<LandingPage onGoToLogin={() => { window.location.href = '/' }} />} />
          <Route element={<AppLayout user={user} onLogout={handleLogout} />}>
            <Route index element={<OverviewPage />} />
            <Route path="alerts" element={<AlertsPage />} />
            <Route path="sensors" element={<SensorsPage />} />
            <Route path="valves" element={<ValvesPage />} />
            <Route path="analytics" element={<AnalyticsPage />} />
            <Route path="readings" element={<Navigate to="/analytics" replace />} />
            <Route path="reports" element={<ReportsPage />} />
            <Route path="users" element={adminOnly(<UsersPage />)} />
            <Route path="settings" element={adminOnly(<SettingsPage />)} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </DataProvider>
  )
}

export default App