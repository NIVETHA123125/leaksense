import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8080/api',
})

api.interceptors.request.use((config) => {
  const token = sessionStorage.getItem('leaksense_token') || localStorage.getItem('leaksense_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (res) => res,
  (err) => {
    const status = err.response?.status
    const isLogin = err.config?.url?.includes('/auth/login')
    if ((status === 401) && !isLogin) {
      sessionStorage.removeItem('leaksense_token')
      sessionStorage.removeItem('leaksense_user')
      localStorage.removeItem('leaksense_token')
      localStorage.removeItem('leaksense_user')
      window.location.href = '/'
    }
    return Promise.reject(err)
  }
)

export const login = (username, password) =>
  api.post('/auth/login', { username, password })

export const getSummary = () => api.get('/dashboard/summary')

export const getActiveAlerts = () => api.get('/alerts/active')

export const acknowledgeAlert = (id, username) =>
  api.put(`/alerts/${id}/acknowledge`, null, { params: { username } })

export const resolveAlert = (id) => api.put(`/alerts/${id}/resolve`)

export const getNodes = () => api.get('/nodes')

export const getLatestReadings = () => api.get('/readings/latest')

export const getRecentValveActions = () => api.get('/valve-actions/recent')

export const getZones = () => api.get('/zones')

export const setValve = (id, action) =>
  api.post(`/zones/${id}/valve`, null, { params: { action } })

export const downloadAlertsReport = (type) =>
api.get(`/reports/alerts/${type}`, { responseType: 'blob' })

export const getUsers = () => api.get('/users')

export const createUser = (data) => api.post('/users', data)

export const resetUserPassword = (id, password) =>
  api.put(`/users/${id}/password`, { password })

export const deleteUser = (id) => api.delete(`/users/${id}`)

export const getThresholds = () => api.get('/settings/thresholds')

export const updateThresholds = (data) => api.put('/settings/thresholds', data)

export const getActiveAlarms = () => api.get('/alarms/active')

export const silenceAlarm = (id) => api.put(`/alarms/${id}/silence`)

export const silenceAllAlarms = () => api.put('/alarms/silence-all')

// --- In-App Notifications ---
export const getNotifications = (unreadOnly = false) =>
  api.get('/notifications', { params: { unreadOnly } })

export const getUnreadNotificationCount = () =>
  api.get('/notifications/unread-count')

export const markNotificationRead = (id) =>
  api.put(`/notifications/${id}/read`)

export const markAllNotificationsRead = () =>
  api.put('/notifications/mark-all-read')

// --- Alert History ---
export const getAlertHistory = (params = {}) =>
  api.get('/alerts/history', { params })

// --- Analytics ---
export const getSensorTrends = (nodeId, hours = 24) =>
  api.get('/analytics/sensor-trends', { params: { nodeId: nodeId || undefined, hours } })

export const getRiskDistribution = () =>
  api.get('/analytics/risk-distribution')

export const getAlertTimeline = (days = 7) =>
  api.get('/analytics/alerts-timeline', { params: { days } })

export const getAlertsBySensor = () =>
  api.get('/analytics/alerts-by-sensor')

export const getValveActivityStats = () =>
  api.get('/analytics/valve-activity')

// --- Reports ---
export const downloadWeeklyReport = (from, to) =>
  api.get('/reports/weekly/pdf', {
    responseType: 'blob',
    params: { from: from || undefined, to: to || undefined },
  })

export const sendWeeklyEmail = (from, to) =>
  api.post('/reports/weekly/send-email', null, {
    params: { from: from || undefined, to: to || undefined },
  })

// --- AI Engine & Chatbot ---
export const sendAiChat = (message, conversationId) =>
  api.post('/ai/chat', { message, conversationId })

export const getAiReportSummary = () =>
  api.get('/ai/report-summary')

export const getAiInsights = () =>
  api.get('/ai/insights')

// --- Public Landing Endpoints ---
export const getPublicLandingContent = () =>
  api.get('/public/landing')

export const getPublicSystemStatus = () =>
  api.get('/public/status')

export const submitDemoRequest = (data) =>
  api.post('/public/demo-request', data)

