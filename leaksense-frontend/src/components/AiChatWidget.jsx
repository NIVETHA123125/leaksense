import { useState, useRef, useEffect } from 'react'
import { sendAiChat, getAiInsights } from '../api/leaksenseApi'
import { AiIcon, ShieldIcon, ChatIcon, BoltIcon, RefreshIcon, CpuIcon } from './icons/AppIcons'

const INITIAL_SUGGESTIONS = [
  'Are there any active gas leaks?',
  'What is the current valve status?',
  'Check sensor battery levels',
  'What emergency protocol applies?',
]

export default function AiChatWidget() {
  const [isOpen, setIsOpen] = useState(false)
  const [activeTab, setActiveTab] = useState('chat') // 'chat' | 'insights'
  const [conversationId] = useState(() => 'conv_' + Math.random().toString(36).substring(2, 9))

  // Chat state
  const [messages, setMessages] = useState([
    {
      role: 'assistant',
      text: 'Hello! I am your LeakSense AI Safety Assistant. I monitor real-time gas telemetry, valve positions, and pre-rupture patterns. How can I assist you?',
      suggestions: INITIAL_SUGGESTIONS,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    },
  ])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const messagesEndRef = useRef(null)

  // Insights state
  const [insights, setInsights] = useState([])
  const [loadingInsights, setLoadingInsights] = useState(false)

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }

  useEffect(() => {
    if (isOpen && activeTab === 'chat') {
      scrollToBottom()
    }
  }, [messages, isOpen, activeTab])

  const loadInsights = async () => {
    setLoadingInsights(true)
    try {
      const res = await getAiInsights()
      setInsights(res.data || [])
    } catch {
      // Ignore or empty
    } finally {
      setLoadingInsights(false)
    }
  }

  useEffect(() => {
    if (isOpen && activeTab === 'insights') {
      loadInsights()
    }
  }, [isOpen, activeTab])

  const handleSend = async (messageText) => {
    const textToSend = (messageText || input).trim()
    if (!textToSend || loading) return

    const userMsg = {
      role: 'user',
      text: textToSend,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    }

    setMessages((prev) => [...prev, userMsg])
    setInput('')
    setLoading(true)

    try {
      const res = await sendAiChat(textToSend, conversationId)
      const aiMsg = {
        role: 'assistant',
        text: res.data?.reply || 'Received no response from safety engine.',
        intent: res.data?.intent,
        suggestions: res.data?.suggestions || [],
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      }
      setMessages((prev) => [...prev, aiMsg])
    } catch {
      const errorMsg = {
        role: 'assistant',
        text: 'Sorry, I encountered an error connecting to the AI Safety backend service.',
        suggestions: ['Retry again', 'Check system status'],
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      }
      setMessages((prev) => [...prev, errorMsg])
    } finally {
      setLoading(false)
    }
  }

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div style={{ position: 'fixed', bottom: '24px', right: '24px', zIndex: 999 }}>
      {/* Floating Launcher Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          style={{
            background: 'var(--panel)',
            color: 'var(--primary)',
            border: '1.5px solid var(--primary)',
            borderRadius: '8px',
            padding: '12px 18px',
            fontWeight: '600',
            fontSize: '13px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            cursor: 'pointer',
            boxShadow: '0 8px 20px rgba(79, 70, 229, 0.15)',
            transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          }}
          title="Open AI Safety Assistant"
          onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(79, 70, 229, 0.08)' }}
          onMouseLeave={(e) => { e.currentTarget.style.background = 'var(--panel)' }}
        >
          <AiIcon size={16} color="var(--primary)" />
          <span>AI Safety Sentinel</span>
        </button>
      )}

      {/* Chat Window / Drawer */}
      {isOpen && (
        <div
          style={{
            width: '380px',
            maxWidth: 'calc(100vw - 32px)',
            height: '560px',
            maxHeight: 'calc(100vh - 48px)',
            background: 'var(--panel)',
            border: '1px solid var(--border)',
            borderRadius: '12px',
            boxShadow: '0 20px 50px rgba(0, 0, 0, 0.45)',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden',
            animation: 'popIn 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
          }}
        >
          {/* Header */}
          <div
            style={{
              padding: '14px 16px',
              background: '#020617',
              borderBottom: '1px solid #1e293b',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
              <div
                style={{
                  width: '32px',
                  height: '32px',
                  borderRadius: '6px',
                  background: 'var(--primary)',
                  color: '#ffffff',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '16px',
                }}
              >
                <AiIcon size={18} color="#ffffff" />
              </div>
              <div>
                <strong style={{ fontSize: '13px', display: 'block', color: '#ffffff' }}>
                  LeakSense AI Sentinel
                </strong>
                <small style={{ color: '#22c55e', fontSize: '11px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                  <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#22c55e' }} />
                  Online & Telemetry Aware
                </small>
              </div>
            </div>

            <button
              onClick={() => setIsOpen(false)}
              style={{
                background: 'transparent',
                border: 'none',
                color: 'var(--text-muted)',
                fontSize: '18px',
                cursor: 'pointer',
                padding: '4px 8px',
                borderRadius: '6px',
              }}
              title="Close Assistant"
            >
              ✕
            </button>
          </div>

          {/* Tab Selector */}
          <div
            style={{
              display: 'flex',
              borderBottom: '1px solid var(--border-soft)',
              background: 'var(--panel-2)',
            }}
          >
            <button
              onClick={() => setActiveTab('chat')}
              style={{
                flex: 1,
                padding: '8px 12px',
                background: 'transparent',
                border: 'none',
                borderBottom: activeTab === 'chat' ? '2px solid var(--accent)' : '2px solid transparent',
                color: activeTab === 'chat' ? 'var(--accent)' : 'var(--text-muted)',
                fontSize: '12px',
                fontWeight: '600',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
              }}
            >
              <ChatIcon size={14} />
              <span>Copilot Chat</span>
            </button>
            <button
              onClick={() => setActiveTab('insights')}
              style={{
                flex: 1,
                padding: '8px 12px',
                background: 'transparent',
                border: 'none',
                borderBottom: activeTab === 'insights' ? '2px solid var(--accent)' : '2px solid transparent',
                color: activeTab === 'insights' ? 'var(--accent)' : 'var(--text-muted)',
                fontSize: '12px',
                fontWeight: '600',
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '6px',
              }}
            >
              <BoltIcon size={14} />
              <span>Early Warnings</span>
              {insights.length > 0 && (
                <span
                  style={{
                    background: '#ef4444',
                    color: '#fff',
                    borderRadius: '999px',
                    padding: '1px 5px',
                    fontSize: '10px',
                  }}
                >
                  {insights.length}
                </span>
              )}
            </button>
          </div>

          {/* Content Area */}
          {activeTab === 'chat' ? (
            <>
              {/* Message History */}
              <div
                style={{
                  flex: 1,
                  overflowY: 'auto',
                  padding: '14px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '12px',
                  background: 'var(--panel-3)',
                }}
              >
                {messages.map((m, idx) => {
                  const isUser = m.role === 'user'
                  return (
                    <div
                      key={idx}
                      style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: isUser ? 'flex-end' : 'flex-start',
                      }}
                    >
                      <div
                        style={{
                          maxWidth: '85%',
                          padding: '10px 14px',
                          borderRadius: isUser ? '14px 14px 2px 14px' : '14px 14px 14px 2px',
                          background: isUser ? 'var(--accent)' : 'var(--panel)',
                          color: isUser ? 'var(--on-accent)' : 'var(--text)',
                          border: isUser ? 'none' : '1px solid var(--border-soft)',
                          fontSize: '13px',
                          lineHeight: 1.45,
                          whiteSpace: 'pre-wrap',
                          wordBreak: 'break-word',
                          boxShadow: '0 2px 8px rgba(0, 0, 0, 0.1)',
                        }}
                      >
                        {m.text}
                      </div>

                      <span style={{ fontSize: '10px', color: 'var(--text-faint)', marginTop: '3px' }}>
                        {m.time}
                      </span>

                      {/* Suggestions Pills */}
                      {!isUser && m.suggestions && m.suggestions.length > 0 && (
                        <div
                          style={{
                            display: 'flex',
                            flexWrap: 'wrap',
                            gap: '6px',
                            marginTop: '8px',
                          }}
                        >
                          {m.suggestions.map((sug, i) => (
                            <button
                              key={i}
                              onClick={() => handleSend(sug)}
                              style={{
                                background: 'var(--panel-2)',
                                border: '1px solid var(--border)',
                                color: 'var(--text)',
                                borderRadius: '12px',
                                padding: '4px 10px',
                                fontSize: '11px',
                                cursor: 'pointer',
                                transition: 'all 0.15s ease',
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '6px',
                              }}
                            >
                              <BoltIcon size={12} color="var(--accent)" />
                              <span>{sug}</span>
                            </button>
                          ))}
                        </div>
                      )}
                    </div>
                  )
                })}

                {loading && (
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', color: 'var(--text-muted)', fontSize: '12px' }}>
                    <span style={{ display: 'inline-flex', animation: 'spin 1s linear infinite' }}><CpuIcon size={14} /></span> Analyzing pipeline database...
                  </div>
                )}
                <div ref={messagesEndRef} />
              </div>

              {/* Chat Input Bar */}
              <div
                style={{
                  padding: '12px',
                  background: 'var(--panel)',
                  borderTop: '1px solid var(--border-soft)',
                  display: 'flex',
                  gap: '8px',
                }}
              >
                <input
                  type="text"
                  placeholder="Ask AI about leaks, valves, or safety..."
                  value={input}
                  onChange={(e) => setInput(e.target.value)}
                  onKeyDown={handleKeyDown}
                  disabled={loading}
                  style={{
                    flex: 1,
                    background: 'var(--panel-2)',
                    color: 'var(--text)',
                    border: '1px solid var(--border-input)',
                    borderRadius: '8px',
                    padding: '8px 12px',
                    fontSize: '13px',
                    outline: 'none',
                  }}
                />
                <button
                  onClick={() => handleSend()}
                  disabled={loading || !input.trim()}
                  style={{
                    background: 'transparent',
                    color: 'var(--accent)',
                    border: '1.5px solid var(--accent)',
                    borderRadius: '8px',
                    padding: '8px 14px',
                    fontWeight: '700',
                    cursor: loading || !input.trim() ? 'not-allowed' : 'pointer',
                    opacity: loading || !input.trim() ? 0.5 : 1,
                  }}
                  onMouseEnter={(e) => { if (!loading && input.trim()) e.currentTarget.style.background = 'rgba(79, 70, 229, 0.08)' }}
                  onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent' }}
                >
                  Send
                </button>
              </div>
            </>
          ) : (
            /* Insights / Early Warnings Tab */
            <div
              style={{
                flex: 1,
                overflowY: 'auto',
                padding: '14px',
                display: 'flex',
                flexDirection: 'column',
                gap: '12px',
                background: 'var(--panel-3)',
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <span style={{ fontSize: '12px', color: 'var(--text-muted)' }}>
                  Continuous heuristic & micro-trend evaluation
                </span>
                <button
                  onClick={loadInsights}
                  disabled={loadingInsights}
                  style={{
                    background: 'transparent',
                    border: 'none',
                    color: 'var(--accent)',
                    fontSize: '11px',
                    cursor: 'pointer',
                    fontWeight: 600,
                    display: 'inline-flex',
                    alignItems: 'center',
                    gap: '4px',
                  }}
                >
                  {loadingInsights ? 'Scanning...' : (
                    <>
                      <RefreshIcon size={12} />
                      <span>Rescan</span>
                    </>
                  )}
                </button>
              </div>

              {loadingInsights ? (
                <div style={{ textAlign: 'center', padding: '36px', color: 'var(--text-muted)', fontSize: '13px' }}>
                  Analyzing pressure decay, oxygen drift, and variance...
                </div>
              ) : insights.length === 0 ? (
                <div
                  style={{
                    textAlign: 'center',
                    padding: '36px 16px',
                    color: 'var(--text-muted)',
                    background: 'var(--panel)',
                    borderRadius: '10px',
                    fontSize: '13px',
                  }}
                >
                  <div style={{ marginBottom: '8px', display: 'flex', justifyContent: 'center' }}>
                    <ShieldIcon size={28} color="var(--accent)" />
                  </div>
                  <strong>All Pre-Rupture Patterns Normal</strong>
                  <p style={{ margin: '4px 0 0', fontSize: '12px', color: 'var(--text-faint)' }}>
                    No creeping gas or micro-pressure decay anomalies detected.
                  </p>
                </div>
              ) : (
                insights.map((ins, i) => (
                  <div
                    key={ins.id || i}
                    style={{
                      background: 'var(--panel)',
                      border: '1px solid var(--border-soft)',
                      borderRadius: '10px',
                      padding: '12px',
                    }}
                  >
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '8px' }}>
                      <strong style={{ fontSize: '13px', color: ins.severity === 'CRITICAL' ? '#ef4444' : '#f59e0b' }}>
                        {ins.title}
                      </strong>
                      <span
                        style={{
                          fontSize: '10px',
                          fontWeight: 700,
                          padding: '2px 6px',
                          borderRadius: '4px',
                          background: ins.severity === 'CRITICAL' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                          color: ins.severity === 'CRITICAL' ? '#ef4444' : '#f59e0b',
                        }}
                      >
                        {ins.confidenceScore ? `${Math.round(ins.confidenceScore * 100)}% Confidence` : ins.severity}
                      </span>
                    </div>

                    <p style={{ fontSize: '12px', color: 'var(--text-muted)', margin: '6px 0', lineHeight: 1.4 }}>
                      {ins.description}
                    </p>

                    {ins.recommendation && (
                      <div
                        style={{
                          fontSize: '11px',
                          background: 'var(--panel-2)',
                          padding: '6px 8px',
                          borderRadius: '6px',
                          color: 'var(--text)',
                          marginTop: '6px',
                        }}
                      >
                        <strong>Mitigation:</strong> {ins.recommendation}
                      </div>
                    )}

                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '10px', color: 'var(--text-faint)', marginTop: '8px' }}>
                      <span>{ins.zoneName} · {ins.nodeCode}</span>
                      <span>{ins.detectedAt ? new Date(ins.detectedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : ''}</span>
                    </div>
                  </div>
                ))
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
