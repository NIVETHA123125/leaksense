import { createContext, useContext, useEffect, useState } from 'react'
import { Client } from '@stomp/stompjs'
import {
  getSummary, getActiveAlerts, getNodes, getLatestReadings,
  getRecentValveActions, getZones, getActiveAlarms, setValve,
  acknowledgeAlert, resolveAlert, silenceAlarm, silenceAllAlarms,
  getNotifications, getUnreadNotificationCount, markNotificationRead, markAllNotificationsRead
} from '../api/leaksenseApi'

const DataContext = createContext(null)

export function DataProvider({ user, children }) {
  const [summary, setSummary] = useState(null)
  const [alerts, setAlerts] = useState([])
  const [nodes, setNodes] = useState([])
  const [readings, setReadings] = useState([])
  const [actions, setActions] = useState([])
  const [zones, setZones] = useState([])
  const [alarms, setAlarms] = useState([])
  const [notifications, setNotifications] = useState([])
  const [unreadCount, setUnreadCount] = useState(0)
  const [error, setError] = useState('')
  const [live, setLive] = useState(false)
  const [zoneChoice, setZoneChoice] = useState(
    () => localStorage.getItem('leaksense_zone') || 'all'
  )

  const loadNotificationsData = async () => {
    try {
      const [notifsRes, countRes] = await Promise.all([
        getNotifications(true),
        getUnreadNotificationCount(),
      ])
      setNotifications(notifsRes.data || [])
      setUnreadCount(countRes.data?.unreadCount || 0)
    } catch {
      // Notifications failure shouldn't break whole dashboard
    }
  }

  const loadData = async () => {
    try {
      const [s, a, n, r, v, z, al] = await Promise.all([
        getSummary(), getActiveAlerts(), getNodes(),
        getLatestReadings(), getRecentValveActions(), getZones(),
        getActiveAlarms(),
      ])
      setSummary(s.data)
      setAlerts(a.data)
      setNodes(n.data)
      setReadings(r.data)
      setActions(v.data)
      setZones(z.data)
      setAlarms(al.data)
      setError('')
    } catch {
      setError('Cannot reach the backend. Is Spring Boot running?')
    }
  }

  useEffect(() => {
    loadData()
    loadNotificationsData()
  }, [])

  const notifyBrowser = (title, options = {}) => {
    if (typeof window !== 'undefined' && 'Notification' in window) {
      if (Notification.permission === 'granted') {
        try {
          new Notification(title, { icon: '/vite.svg', ...options })
        } catch {
          // ignore restriction on mobile or unsupported devices
        }
      }
    }
  }

  useEffect(() => {
    if (typeof window !== 'undefined' && 'Notification' in window) {
      if (Notification.permission === 'default') {
        Notification.requestPermission().catch(() => {})
      }
    }
  }, [])

  useEffect(() => {
    const client = new Client({
      brokerURL: 'ws://localhost:8080/ws',
      reconnectDelay: 5000,
      onConnect: () => {
        setLive(true)
        client.subscribe('/topic/readings', (message) => {
          const reading = JSON.parse(message.body)
          setReadings((prev) => [reading, ...prev].slice(0, 50))
        })
        client.subscribe('/topic/alerts', (message) => {
          try {
            const alert = JSON.parse(message.body)
            notifyBrowser(`LeakSense [${alert.severity || 'ALERT'}]: ${alert.title}`, {
              body: alert.message || 'Pipeline safety event detected.',
            })
          } catch {
            notifyBrowser('LeakSense Safety Alert', {
              body: 'New alert received from pipeline sensor stream.',
            })
          }
          loadData()
          loadNotificationsData()
        })
        client.subscribe('/topic/valves', () => {
          loadData()
          loadNotificationsData()
        })
        client.subscribe('/topic/alarms', (message) => {
          try {
            const alarm = JSON.parse(message.body)
            notifyBrowser(`LeakSense SIREN ALARM: ${alarm.sensor || 'Hazard'}`, {
              body: alarm.message || 'Critical pipeline emergency siren triggered!',
            })
          } catch {
            // ignore malformed alarm payload
          }
          loadData()
          loadNotificationsData()
        })
        client.subscribe('/topic/notifications', (message) => {
          try {
            const newNotif = JSON.parse(message.body)
            notifyBrowser(`LeakSense: ${newNotif.title}`, {
              body: newNotif.message,
            })
            setNotifications((prev) => [newNotif, ...prev.filter(n => n.id !== newNotif.id)].slice(0, 50))
            setUnreadCount((c) => c + 1)
          } catch {
            loadNotificationsData()
          }
        })
      },
      onWebSocketClose: () => setLive(false),
    })
    client.activate()
    return () => {
      client.deactivate()
    }
  }, [])

  const acknowledge = async (id) => {
    await acknowledgeAlert(id, user.username)
    loadData()
  }

  const resolve = async (id) => {
    await resolveAlert(id)
    loadData()
  }

  const changeValve = async (zoneId, action) => {
    await setValve(zoneId, action)
    loadData()
  }

  const silence = async (id) => {
    await silenceAlarm(id)
    loadData()
  }

  const silenceAll = async () => {
    await silenceAllAlarms()
    loadData()
  }

  const setZoneFilter = (value) => {
    setZoneChoice(value)
    localStorage.setItem('leaksense_zone', value)
  }

  // ---- zone filter: pages receive already-filtered data (alarms are never filtered) ----
  const zoneFilter = zones.some((z) => String(z.id) === zoneChoice) ? zoneChoice : 'all'
  const all = zoneFilter === 'all'

  const inZone = (item) => all || String(item?.zone?.id) === zoneFilter
  const readingInZone = (r) => all || String(r?.node?.zone?.id) === zoneFilter

  const shownAlerts = alerts.filter(inZone)
  const shownNodes = nodes.filter(inZone)
  const shownReadings = readings.filter(readingInZone)
  const shownActions = actions.filter(inZone)
  const shownZones = zones.filter((z) => all || String(z.id) === zoneFilter)

  const shownSummary =
    all || !summary
      ? summary
      : {
          totalZones: shownZones.length,
          totalNodes: shownNodes.length,
          onlineNodes: shownNodes.filter((n) => n.status === 'ONLINE').length,
          offlineNodes: shownNodes.filter((n) => n.status !== 'ONLINE').length,
          activeAlerts: shownAlerts.length,
          criticalAlerts: shownAlerts.filter((a) => a.severity === 'CRITICAL').length,
        }

  const markNotificationAsRead = async (id) => {
    try {
      await markNotificationRead(id)
      setNotifications((prev) => prev.filter((n) => n.id !== id))
      setUnreadCount((c) => Math.max(0, c - 1))
    } catch {
      loadNotificationsData()
    }
  }

  const markAllNotificationsAsRead = async () => {
    try {
      await markAllNotificationsRead()
      setNotifications([])
      setUnreadCount(0)
    } catch {
      loadNotificationsData()
    }
  }

  const value = {
    user,
    summary: shownSummary,
    alerts: shownAlerts,
    nodes: shownNodes,
    readings: shownReadings,
    actions: shownActions,
    zones: shownZones,
    allZones: zones,
    alarms,
    notifications,
    unreadCount,
    loadNotifications: loadNotificationsData,
    markNotificationAsRead,
    markAllNotificationsAsRead,
    zoneFilter,
    setZoneFilter,
    error,
    live,
    acknowledge,
    resolve,
    changeValve,
    silence,
    silenceAll,
  }

  return <DataContext.Provider value={value}>{children}</DataContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useData() {
  return useContext(DataContext)
}