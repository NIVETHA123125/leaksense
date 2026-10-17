# LeakSense — Autonomous IoT Pipeline Safety & Leak Mitigation Platform

[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()
[![Java](https://img.shields.io/badge/Java-21-orange.svg)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4%2B-blue.svg)]()
[![Tests](https://img.shields.io/badge/Tests-268%20Passing-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-purple.svg)]()

> **Industrial IoT pipeline safety platform combining real-time multi-sensor telemetry, AI-driven predictive early-warning detection, autonomous sub-second solenoid valve isolation, omni-channel emergency alerts, and automated compliance auditing.**

---

## 🌟 System Architecture & Topology

```
   ┌──────────────────────────────────────────────────────────────┐
   │               INDUSTRIAL PIPELINE MONITORING ZONE            │
   │                                                              │
   │   [MQ-4 Methane]     [MPX5700 Pressure]     [ME2-O2 Oxygen]  │
   │          │                   │                     │         │
   │          └───────────┬───────┴─────────────┬───────┘         │
   │                      ▼                     ▼                 │
   │             [ESP32 / LoRa Node]   [12V Solenoid Valve]       │
   └──────────────────────┬─────────────────────▲─────────────────┘
                          │ (HTTPS / WS / JSON) │ (Auto-Isolation)
                          ▼                     │
   ┌────────────────────────────────────────────┴─────────────────┐
   │                 LEAKSENSE SPRING BOOT BACKEND                │
   │                                                              │
   │  • Telemetry Ingest & Real-Time Risk Engine (0-100)          │
   │  • Safety Core: 1-min Monitor, Spike & Stuck Sensor Detector │
   │  • Autonomous & Manual Valve Actuation Service               │
   │  • Multi-Channel Notifications: WebSocket, In-App, Mail, SMS │
   │  • Historical Analytics & Multi-Criteria Filter Query Engine │
   │  • Automated Weekly PDF & Excel Safety Auditing              │
   │  • AI Intelligence: Pre-Rupture Trajectory & Chatbot         │
   └──────────────────────┬───────────────────────────────────────┘
                          ▼
   ┌──────────────────────────────────────────────────────────────┐
   │              WEB DASHBOARD & OPERATOR CHANNELS               │
   │                                                              │
   │  • Responsive Vite/React Dashboard + Glassmorphism UI        │
   │  • Interactive Bell Notifications & Real-Time Popups         │
   │  • 5 Interactive Analytical Charts & Historical Trend Views  │
   │  • AI Operator Chatbot Assistant (Natural Language Q&A)      │
   │  • Executive PDF Compliance Download & Weekly Dispatch       │
   └──────────────────────────────────────────────────────────────┘
```

---

## 🚀 Key Feature Blocks (All 18 Components Complete)

### 1. Safety Core
* **1-Minute Node Monitor**: Automatically flags inactive nodes as `OFFLINE` if no heartbeat is received within 60 seconds.
* **Sensor Spike Detection**: Instant alerts if methane spikes $\ge 100\%$, pressure drops $\ge 15\%$, or oxygen drops $\ge 10\%$ in consecutive readings.
* **Sensor Stuck Detection**: Flags hardware faults when a sensor reports identical analog readings across 5 consecutive samples.
* **Battery Health Monitoring**: Raises `WARNING` at $\le 20\%$ battery and `CRITICAL` at $\le 10\%$.
* **Valve-Stuck Protection**: Automatically raises `FAULT` alert if a valve actuator does not reach confirmed state within 120 seconds.

### 2. Multi-Channel Notifications
* **In-App Bell Notifications**: Persistent `AppNotification` table with unread count (`GET /api/notifications/unread-count`) and mark-read actions (`PUT /api/notifications/{id}/read`, `PUT /api/notifications/mark-all-read`).
* **Real-Time WebSocket Push**: Broadcasts to `/topic/notifications`, `/topic/alerts`, `/topic/alarms`, and `/topic/readings`.
* **Expanded Email Events**:
  - Critical & Warning Leak Alerts
  - Critical Alarm Sounded
  - Alarm Silenced by Operator
  - Alarm Cleared (Safe Readings Restored)
  - Sensor Node Restored & Reconnected
  - Valve Actuation (Open / Close) with Reason
* **Emergency SMS Dispatch**: Integrated `SmsService` with Twilio API support for `CRITICAL` emergencies with graceful fallback simulation.

### 3. History & Analytical Graphs
* **Multi-Criteria Alert History Query**: Filter alerts dynamically with pagination via `GET /api/alerts/history` by:
  - `zoneId`, `nodeId`, `sensor`, `level`, `severity`, `status`, `from`, `to`, `page`, `size`, `direction`.
* **5 Rich Analytical Endpoints**:
  1. `GET /api/analytics/sensor-trends`: Historical time-series readings for methane, pressure, oxygen, and risk score.
  2. `GET /api/analytics/risk-distribution`: Zone & reading hazard counts across SAFE, LOW, MODERATE, HIGH, CRITICAL.
  3. `GET /api/analytics/alerts-timeline`: Daily breakdown of critical vs warning alerts over 7, 14, or 30 days.
  4. `GET /api/analytics/alerts-by-sensor`: Breakdown and percentage share of alerts by sensor type.
  5. `GET /api/analytics/valve-activity`: Metrics on manual vs automated closures and recent actuation history.

### 4. Automated Weekly Safety Audits
* **Executive PDF Generator**: Generates comprehensive PDF reports with KPI cards, alarms timeline, valve isolation history, and pipeline zone overviews.
* **Scheduled Cron Email Dispatch**: Automatically dispatches the PDF report to management every Monday morning (`0 0 9 * * MON`).
* **On-Demand PDF & Excel Downloads**: `GET /api/reports/weekly/pdf`, `GET /api/reports/alerts/excel`, `GET /api/reports/alerts/pdf`.

### 5. AI Features & Operator Assistant
* **AI Early-Warning Predictive Engine**:
  - Correlates negative pressure drift with gas creeping up to detect pre-rupture signatures before limits are breached.
  - Predicts rapid oxygen displacement trajectory.
  - Detects micro-turbulence pressure oscillations.
* **Interactive AI Chatbot Assistant (`POST /api/ai/chat`)**:
  - Natural language Q&A engine analyzing live real-time pipeline context (alerts, alarms, valve positions, low batteries, safety protocols).
* **AI Report Summary (`GET /api/ai/report-summary`)**:
  - Generates executive narrative assessment, overall safety score (0-100), key observations, and recommended actions.
* **AI Insights (`GET /api/ai/insights`)**:
  - High-priority anomaly dashboard feed with confidence scores and recommended field actions.

### 6. Public Landing Page & Showcase
* **Modern Dark-Mode Landing Page**: Glassmorphic UI with vibrant cyan/emerald gradients, live status pulse badge, interactive stats, hardware specifications table, and demo request form.
* **Public REST Endpoints**: `GET /api/public/landing`, `GET /api/public/status`, `POST /api/public/demo-request`.

### 7. Production Security
* **JWT Stateless Authentication**: Secure token verification with role-based access control (`ADMIN` vs `OPERATOR`).
* **CORS & CSRF Hardening**: Configured for dashboard clients (`http://localhost:5173`, `http://localhost:3000`).
* **Public Route Isolation**: Landing page and public APIs are accessible without authentication while sensitive valve and admin settings remain strictly guarded.

---

## 🛠️ Tech Stack & Requirements

| Layer | Technology |
|---|---|
| **Language** | Java 21 (with preview features enabled) |
| **Framework** | Spring Boot 3.4+ / 4.1.1 |
| **Database** | MySQL 8.0 (Hibernate DDL Auto update) |
| **Messaging** | Spring WebSocket (STOMP Broker) |
| **PDF Generation** | OpenPDF (LibrePDF 2.0.3) |
| **Excel Export** | Apache POI 5.3.0 |
| **Email** | Spring Mail (JavaMailSender / Gmail SMTP) |
| **Testing** | JUnit 5, Mockito, AssertJ |

---

## 📋 Configuration (`application.properties`)

```properties
# Server & Database
server.port=8080
spring.datasource.url=jdbc:mysql://localhost:3306/leaksense_db
spring.jpa.hibernate.ddl-auto=update
leaksense.jwt.expiration-ms=86400000

# Safety Thresholds
leaksense.node.offline-after-seconds=60
leaksense.sensor.stuck-count=5
leaksense.sensor.spike.methane-percent=100
leaksense.sensor.spike.pressure-percent=15
leaksense.sensor.spike.oxygen-percent=10
leaksense.node.low-battery-percent=20
leaksense.node.critical-battery-percent=10
leaksense.node.valve-stuck-seconds=120

# Notifications (Email & SMS)
leaksense.notify.enabled=true
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
leaksense.notify.to=operator@leaksense.com

# Twilio SMS (Optional)
leaksense.sms.enabled=false
leaksense.sms.account-sid=
leaksense.sms.auth-token=
leaksense.sms.from-number=
leaksense.sms.to-numbers=

# Weekly Report Schedule
leaksense.report.weekly-cron=0 0 9 * * MON
```

---

## 🧪 Testing & Verification

Run the complete test suite (260+ unit and integration tests):

```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

Start the application:

```bash
.\mvnw.cmd spring-boot:run
```

Access the landing page at `http://localhost:8080/`!
