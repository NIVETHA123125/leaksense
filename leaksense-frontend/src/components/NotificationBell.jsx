import { useState, useRef, useEffect } from 'react'
import { useData } from '../context/DataContext'
import { BellIcon, CheckIcon } from './icons/AppIcons'

export default function NotificationBell() {
  const {
    notifications,
    unreadCount,
    markNotificationAsRead,
    markAllNotificationsAsRead,
    loadNotifications,
  } = useData()

  const [isOpen, setIsOpen] = useState(false)
  const dropdownRef = useRef(null)

  // Close on outside click
  useEffect(() => {
    function handleClickOutside(event) {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
        setIsOpen(false)
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside)
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside)
    }
  }, [isOpen])

  const toggleOpen = () => {
    setIsOpen(!isOpen)
    if (!isOpen) {
      loadNotifications()
    }
  }

  const displayedNotifications = notifications

  const getSeverityStyle = (severity) => {
    switch (severity?.toUpperCase()) {
      case 'CRITICAL':
        return { bg: 'rgba(239, 68, 68, 0.15)', color: '#ef4444', border: '#ef4444' }
      case 'WARNING':
        return { bg: 'rgba(245, 158, 11, 0.15)', color: '#f59e0b', border: '#f59e0b' }
      default:
        return { bg: 'var(--primary-tint)', color: 'var(--primary)', border: 'var(--primary-border)' }
    }
  }

  const formatTime = (dateStr) => {
    if (!dateStr) return ''
    try {
      const d = new Date(dateStr)
      return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) + ' · ' + d.toLocaleDateString([], { month: 'short', day: 'numeric' })
    } catch {
      return dateStr
    }
  }

  return (
    <div className="notif-wrapper" ref={dropdownRef} style={{ position: 'relative' }}>
      <button
        onClick={toggleOpen}
        className="notif-bell-btn"
        title="Notifications"
        aria-label="Open notifications"
        style={{
          position: 'relative',
          background: isOpen ? 'var(--hover)' : 'transparent',
          border: '1px solid var(--border)',
          borderRadius: '8px',
          width: '36px',
          height: '36px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          cursor: 'pointer',
          color: 'var(--text)',
          fontSize: '16px',
          transition: 'all 0.2s ease',
        }}
      >
        <BellIcon size={18} color="var(--text)" />
        {unreadCount > 0 && (
          <span
            style={{
              position: 'absolute',
              top: '-4px',
              right: '-4px',
              background: '#ef4444',
              color: '#ffffff',
              borderRadius: '999px',
              fontSize: '10px',
              fontWeight: '700',
              padding: '1px 5px',
              minWidth: '16px',
              height: '16px',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              boxShadow: '0 0 8px rgba(239, 68, 68, 0.6)',
              animation: 'pulse 2s infinite',
            }}
          >
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div
          className="notif-dropdown"
          style={{
            position: 'absolute',
            top: 'calc(100% + 8px)',
            right: 0,
            width: '380px',
            maxWidth: '90vw',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            boxShadow: '0 12px 36px rgba(0, 0, 0, 0.4)',
            zIndex: 100,
            overflow: 'hidden',
            display: 'flex',
            flexDirection: 'column',
            maxHeight: '520px',
            animation: 'popIn 0.15s ease',
          }}
        >
          {/* Header */}
          <div
            style={{
              padding: '12px 16px',
              borderBottom: '1px solid var(--border-soft)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              background: 'var(--panel-2)',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <strong style={{ fontSize: '15px' }}>Notifications</strong>
              {unreadCount > 0 && (
                <span
                  style={{
                    background: 'var(--accent-bg)',
                    color: 'var(--accent)',
                    borderRadius: '10px',
                    padding: '2px 8px',
                    fontSize: '11px',
                    fontWeight: 'bold',
                  }}
                >
                  {unreadCount} new
                </span>
              )}
            </div>

            <div style={{ display: 'flex', gap: '6px' }}>
              {notifications.length > 0 && (
                <button
                  onClick={markAllNotificationsAsRead}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--accent)',
                    fontSize: '12px',
                    fontWeight: '600',
                    cursor: 'pointer',
                    padding: '4px 8px',
                    borderRadius: '4px',
                    transition: 'all 0.15s ease',
                  }}
                  title="Mark all as read and clear"
                >
                  Read all
                </button>
              )}
            </div>
          </div>

          {/* Notifications List */}
          <div
            style={{
              overflowY: 'auto',
              flex: 1,
            }}
          >
            {displayedNotifications.length === 0 ? (
              <div
                style={{
                  padding: '36px 16px',
                  textAlign: 'center',
                  color: 'var(--text-muted)',
                  fontSize: '13px',
                }}
              >
                <div style={{ marginBottom: '8px', display: 'flex', justifyContent: 'center' }}>
                  <CheckIcon size={24} color="var(--text-faint)" />
                </div>
                All caught up! No notifications.
              </div>
            ) : (
              displayedNotifications.map((notif) => {
                const isRead = notif.isRead || notif.read
                const sev = getSeverityStyle(notif.severity)
                return (
                  <div
                    key={notif.id}
                    onClick={() => !isRead && markNotificationAsRead(notif.id)}
                    style={{
                      padding: '12px 14px',
                      borderBottom: '1px solid var(--border-soft)',
                      background: isRead ? 'transparent' : 'var(--hover)',
                      cursor: isRead ? 'default' : 'pointer',
                      transition: 'background 0.15s ease',
                      display: 'flex',
                      gap: '10px',
                      alignItems: 'flex-start',
                    }}
                  >
                    <div
                      style={{
                        width: '8px',
                        height: '8px',
                        borderRadius: '50%',
                        background: isRead ? 'transparent' : '#ef4444',
                        marginTop: '6px',
                        flexShrink: 0,
                      }}
                    />

                    <div style={{ flex: 1, minWidth: 0 }}>
                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          gap: '6px',
                          marginBottom: '3px',
                        }}
                      >
                        <span
                          style={{
                            fontSize: '13px',
                            fontWeight: isRead ? '500' : '700',
                            color: 'var(--text)',
                          }}
                        >
                          {notif.title}
                        </span>
                        <span
                          style={{
                            fontSize: '10px',
                            fontWeight: '600',
                            padding: '1px 6px',
                            borderRadius: '4px',
                            background: sev.bg,
                            color: sev.color,
                            border: `1px solid ${sev.border}`,
                            flexShrink: 0,
                          }}
                        >
                          {(notif.severity === 'INFO' ? 'NORMAL' : notif.severity) || 'NORMAL'}
                        </span>
                      </div>

                      <p
                        style={{
                          fontSize: '12px',
                          color: 'var(--text-muted)',
                          margin: '0 0 6px',
                          lineHeight: 1.4,
                        }}
                      >
                        {notif.message}
                      </p>

                      <div
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          gap: '8px',
                          fontSize: '11px',
                          color: 'var(--text-faint)',
                        }}
                      >
                        <span>{formatTime(notif.createdAt)}</span>
                        {notif.zoneId && <span>• Zone #{notif.zoneId}</span>}
                        {notif.nodeId && <span>• Node #{notif.nodeId}</span>}
                        {!isRead && (
                          <span
                            style={{
                              marginLeft: 'auto',
                              color: 'var(--accent)',
                              fontSize: '11px',
                              cursor: 'pointer',
                            }}
                          >
                            Mark read
                          </span>
                        )}
                      </div>
                    </div>
                  </div>
                )
              })
            )}
          </div>
        </div>
      )}
    </div>
  )
}
