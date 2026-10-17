# LeakSense — Autonomous Industrial IoT Pipeline Safety Platform (Software)

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4%2B-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![WebSockets](https://img.shields.io/badge/STOMP-WebSockets-010101?style=for-the-badge&logo=socketdotio&logoColor=white)](https://stomp.github.io/)
[![Tests](https://img.shields.io/badge/Tests-271%2F271%20Passing%20(100%25)-brightgreen?style=for-the-badge)]()

> **Notice:** This document covers the **Software Platform** of LeakSense (the Spring Boot Safety Core, React SCADA Operator Dashboard, AI Predictive Engine, Real-Time Event Bus, and Relational Database). For physical sensor hardware specifications, pinouts, circuit wiring, and 3D enclosures, refer to the hardware build documentation.

---

## 📖 Table of Contents
1. [Platform Overview](#-platform-overview)
2. [Software Architecture & Topology](#-software-architecture--topology)
3. [Key Software Capabilities](#-key-software-capabilities)
   - [Real-Time Telemetry & Multi-Tier Risk Engine](#1-real-time-telemetry--multi-tier-risk-engine)
   - [Autonomous Safety Watchdogs](#2-autonomous-safety-watchdogs)
   - [Valve Control & Emergency Interlocks](#3-valve-control--emergency-interlocks)
   - [Omni-Channel Alerting & Notification Engine](#4-omni-channel-alerting--notification-engine)
   - [AI Predictive Trajectory & Operator Copilot](#5-ai-predictive-trajectory--operator-copilot)
   - [Automated Compliance Auditing (PDF & Excel)](#6-automated-compliance-auditing-pdf--excel)
4. [Software Repository Structure](#-software-repository-structure)
5. [Prerequisites & System Requirements](#-prerequisites--system-requirements)
6. [Getting Started & Local Setup](#-getting-started--local-setup)
   - [Database Setup](#1-database-setup)
   - [Backend Service Setup](#2-backend-service-setup)
   - [Frontend Application Setup](#3-frontend-application-setup)
7. [Default Authentication Credentials](#-default-authentication-credentials)
8. [Configuration & Environment Variables](#-configuration--environment-variables)
9. [REST & WebSocket API Reference](#-rest--websocket-api-reference)
10. [Testing & Quality Assurance](#-testing--quality-assurance)
11. [Security & Session Architecture](#-security--session-architecture)

---

## 🌐 Platform Overview

**LeakSense** is an enterprise-grade pipeline safety and industrial monitoring platform designed to detect hazardous gas leaks (Methane $\text{CH}_4$), pressure anomalies ($\Delta P$), and oxygen displacement ($\text{O}_2$) with sub-second response times. 

The software system coordinates continuous telemetry ingestion from edge nodes, computes non-linear multi-variable risk scores, automatically trips isolation valves when limits are breached, maintains real-time SCADA operator consoles, and generates scheduled executive safety audit reports.

### Key Performance Metrics
* **Sub-Second Edge Command Actuation**: Valve trip dispatched in $< 1.2\text{s}$ upon critical limit breach.
* **100% Test Suite Coverage**: 271 unit, integration, and WebMvc slice tests passing across 27 test classes.
* **Resilient Watchdogs**: Built-in 180s stale node heartbeat timeout, sensor spike/jump filtering, analog freeze/flatline detection, and 120s valve-stuck monitoring.
* **Real-Time Data Streaming**: Dual-channel communication via RESTful ingestion and STOMP over WebSockets.

---

## 🏗 Software Architecture & Topology

The LeakSense software operates across a multi-tier decoupled architecture:

```
+───────────────────────────────────────────────────────────────────────────────────+
|                               TELEMETRY INGESTION                                 |
|          Edge Node Telemetry Packets (HTTP POST /api/ingest/reading)               |
+─────────────────────────────────────────┬─────────────────────────────────────────+
                                          │
                                          ▼
+───────────────────────────────────────────────────────────────────────────────────+
|                          SPRING BOOT 3.x SAFETY CORE                              |
|                                                                                   |
|  ┌─────────────────────────┐  ┌──────────────────────────┐  ┌──────────────────┐  |
|  │  Ingest & Preprocessing │  │  Risk Engine (0-100)     │  │  Watchdog Core   │  |
|  │  - Battery Clamping     │  │  - Non-linear derivative │  │  - 180s Stale    │  |
|  │  - Out-of-Range Traps   │  │  - ΔP / Δt & ΔCH4 / Δt   │  │  - Sensor Freeze │  |
|  └────────────┬────────────┘  └─────────────┬────────────┘  │  - Actuator Stuck│  |
|               │                             │               └────────┬─────────┘  |
|               ▼                             ▼                        │            |
|  ┌────────────────────────────────────────────────────────┐          │            |
|  │  Safety & Valve Interlock Services                     │◄─────────┘            |
|  │  - Autonomous Emergency Trip (leaksense.autoclose)    │                       |
|  │  - Role-based Manual Actuation (ADMIN Open, OPER Close)│                       |
|  └────────────────────────────┬───────────────────────────┘                       |
|                               │                                                   |
|  ┌────────────────────────────┴───────────────────────────┐                       |
|  │  Multi-Channel Dispatch Engine                         │                       |
|  │  - STOMP WebSockets Broker (/topic/readings, /alerts)  │                       |
|  │  - In-App Notifications Entity & Badge Counters        │                       |
|  │  - Twilio SMS Emergency Client (with sandbox fallback) │                       |
|  │  - OpenPDF Scheduled Weekly Safety Audit (Mondays 9 AM)│                       |
|  └────────────────────────────────────────────────────────┘                       |
+───────────────────────────────┬───────────────────────────┬───────────────────────+
                                │                           │
                 Persistence    │                           │  REST / STOMP
                                ▼                           ▼
+─────────────────────────────────────────+   +─────────────────────────────────────+
|           DATABASE STORAGE              |   |       SCADA OPERATOR DASHBOARD      |
|                                         |   |                                     |
|  MySQL 8.x + Hibernate JPA              |   |  React 19 + Vite 8 + Recharts       |
|  - Zones, Nodes, & Telemetry Readings   |   |  - Real-Time Gas & Pressure Graphs  |
|  - Alerts, Alarm Events, Valve Actions  |   |  - Audio Siren Alarms & Silence Bar |
|  - Threshold Audit Logs & User Accounts |   |  - Interactive Valve Control Matrix |
|  - In-App Persistent Notifications      |   |  - AI Operator Copilot Assistant    |
+─────────────────────────────────────────+   +─────────────────────────────────────+
```

---

## ⚡ Key Software Capabilities

### 1. Real-Time Telemetry & Multi-Tier Risk Engine
* **Telemetry Processing**: Ingests packets containing Methane ($\text{ppm}$), Differential Pressure ($\text{kPa}$), Oxygen concentration ($\% \text{ Vol}$), and battery level ($\%$) through [`IngestController`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/controller/IngestController.java).
* **Composite Risk Score ($0-100$)**: Evaluates readings using a weighted non-linear model:
  $$\text{Risk} = \min\left(100,\, \left(\frac{\text{CH}_4}{\text{CH}_{4,\text{crit}}} \times 60\right) + \left(\frac{|\Delta P|}{P_{\text{baseline}}} \times 25\right) + \left(\frac{\max(0, 20.9 - \text{O}_2)}{5} \times 15\right)\right)$$
* **Dynamic Threshold Evaluation**: Categorizes risk bands (`SAFE`, `LOW`, `MODERATE`, `HIGH`, `CRITICAL`) and dispatches warnings or emergency alerts based on configurable zone thresholds.

### 2. Autonomous Safety Watchdogs
* **Stale Node Sentinel** ([`NodeMonitorService`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/NodeMonitorService.java)): Scheduled 60s monitor flags nodes as `OFFLINE` if no heartbeat arrives within $180\text{s}$ (tolerating 3 missed 60s cycles to prevent alert flapping).
* **Sensor Spike Detection** ([`SensorHealthService`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/SensorHealthService.java)): Flags abnormal instantaneous derivative leaps ($\ge 100\%$ Methane jump, $\ge 15\%$ sudden pressure collapse, or $\ge 10\%$ oxygen drop).
* **Analog Flat-Line / Freeze Detector**: Detects failing analog transducers by monitoring consecutive identical floating-point values (triggers `SENSOR_FAULT` after 5 consecutive identical readings).
* **Valve-Stuck Protection** ([`CommandService`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/CommandService.java)): Generates critical actuator alarms if an edge valve fails to acknowledge open/close actuation within $120\text{s}$.
* **Battery Sentinel**: Issues progressive alerts at low ($\le 20\%$) and critical ($\le 10\%$) battery thresholds.

### 3. Valve Control & Emergency Interlocks
* **Autonomous Auto-Close**: If `leaksense.autoclose.enabled=true`, the safety core immediately commands valve isolation when `CRITICAL` leak thresholds are exceeded.
* **Role-Based Actuation Safeguards**:
  - `OPERATOR`: May initiate emergency closures and normal zone shutoffs.
  - `ADMIN`: Required to re-open valves once safe baseline readings are restored.
  - **Active Hazard Lock**: Prevents valve opening commands from executing while an unacknowledged critical hazard persists in that zone.
* **Plant-Wide Master Trip**: One-click emergency shutoff endpoint trips all pipeline valves simultaneously.

### 4. Omni-Channel Alerting & Notification Engine
* **STOMP WebSockets**: Broadcasts low-latency telemetry and events over `/topic/readings`, `/topic/alerts`, `/topic/alarms`, and `/topic/notifications`.
* **In-App Notification Center**: Persistent database notifications with unread counts and batch read/unread updates.
* **Audio Siren Alarm Lifecycle**: Dispatches synchronized audible alarm events to connected SCADA dashboards, supporting operator silencing with an automated 5-minute resound timer if conditions remain unsafe.
* **SMS & Email Broadcast**: Twilio REST SMS dispatch for critical alerts (with transparent sandbox simulation mode) and HTML mail alerts via JavaMail.

### 5. AI Predictive Trajectory & Operator Copilot
* **Pre-Rupture Trajectory Heuristics** ([`AiEarlyWarningService`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/AiEarlyWarningService.java)): Correlates negative pressure trends with low-level gas creep to identify micro-fractures before physical thresholds trip.
* **Context-Aware AI Chatbot Copilot** ([`AiController`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/controller/AiController.java)): An interactive assistant accessible via the dashboard with live visibility into active alarms, valve positions, offline nodes, and emergency operating procedures.
* **Automated AI Safety Assessment** ([`AiReportSummaryService`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/AiReportSummaryService.java)): Generates an algorithmic system safety grade ($0-100$), narrative summaries, and actionable maintenance advice.

### 6. Automated Compliance Auditing (PDF & Excel)
* **Weekly PDF Safety Audits**: Built with OpenPDF, generating branded reports complete with KPI cards, alarm event summaries, valve actuation logs, and zone status tables.
* **Scheduled Cron Dispatch**: Automatically compiles and emails the weekly audit report every Monday at 9:00 AM (`leaksense.report.weekly-cron`).
* **On-Demand Reporting**: Exportable PDF and Excel workbooks available directly from the SCADA console.

---

## 📂 Software Repository Structure

```
d:\Leaksense
│
├── leaksense-frontend/                      # React 19 + Vite SCADA Dashboard
│   ├── public/                              # Static public assets
│   ├── src/
│   │   ├── api/
│   │   │   └── leaksenseApi.js              # Axios HTTP client & API route definitions
│   │   ├── components/
│   │   │   ├── icons/AppIcons.jsx           # Clean SVG iconography suite
│   │   │   ├── layout/AppLayout.jsx         # Pinned sidebar & topbar layout shell
│   │   │   ├── AiChatWidget.jsx             # Floating AI Chatbot assistant modal
│   │   │   ├── AiReportSummaryCard.jsx      # AI safety score & auditor notes card
│   │   │   ├── AlarmBanner.jsx              # Pulsing emergency banner with siren audio
│   │   │   ├── AlertHistoryTable.jsx        # Paginated, multi-filter alert log table
│   │   │   ├── AlertsPanel.jsx              # Active alarm cards with Acknowledge/Resolve
│   │   │   ├── AuditTrailPanel.jsx          # Live log of operator valve actions
│   │   │   ├── ConfirmModal.jsx             # Confirmation dialog for critical operations
│   │   │   ├── GasChart.jsx                 # Recharts live streaming multi-sensor graph
│   │   │   ├── Login.jsx                    # Operator & Administrator authentication
│   │   │   ├── NotificationBell.jsx         # In-app notification center dropdown
│   │   │   ├── SensorHealthPanel.jsx        # Grid cards showing node voltage & link state
│   │   │   ├── SummaryCards.jsx             # SCADA KPI metric summary tiles
│   │   │   ├── ThemeToggle.jsx              # Dark / Light theme switcher
│   │   │   └── ValveControlPanel.jsx        # Solenoid valve actuation controls & trip switch
│   │   ├── context/
│   │   │   └── DataContext.jsx              # Global state (WebSockets, alerts, nodes, zones)
│   │   ├── pages/
│   │   │   ├── AlertsPage.jsx               # Dedicated safety alert investigation view
│   │   │   ├── AnalyticsPage.jsx            # Live telemetry stream & 5 historical charts
│   │   │   ├── LandingPage.jsx              # Public landing page with live heartbeat
│   │   │   ├── OverviewPage.jsx             # Primary operator control console
│   │   │   ├── ReportsPage.jsx              # Weekly PDF & Excel compliance export center
│   │   │   ├── SensorsPage.jsx              # Edge hardware inventory & ping simulation
│   │   │   ├── SettingsPage.jsx             # Dynamic threshold limit configuration
│   │   │   ├── UsersPage.jsx                # Role-based user account administration
│   │   │   └── ValvesPage.jsx               # Dedicated pipeline valve control & topology
│   │   ├── App.jsx                          # Main routing & sessionStorage auth guard
│   │   ├── index.css                        # Design system tokens & utility styling
│   │   ├── main.jsx                         # Application entry point
│   │   └── theme.css                        # Theme tokens for dark & light modes
│   ├── package.json                         # Frontend dependencies & scripts
│   └── vite.config.js                       # Vite build configuration
│
└── leaksense_backend/                       # Spring Boot 3.x Safety Core Backend
    ├── src/
    │   ├── main/
    │   │   ├── java/com/leaksense/leaksense_backend/
    │   │   │   ├── config/                  # SecurityConfig, JwtAuthFilter, WebSocketConfig, DataInitializer
    │   │   │   ├── controller/              # 16 REST Controllers (Ingest, Valves, Alerts, AI, etc.)
    │   │   │   ├── dto/                     # Request/Response Data Transfer Objects
    │   │   │   ├── entity/                  # 10 JPA Entities (Reading, Node, Zone, Alert, ValveAction, etc.)
    │   │   │   ├── repository/              # 11 Spring Data JPA Repositories
    │   │   │   ├── service/                 # 23 Core Engineering & Business Logic Services
    │   │   │   └── LeaksenseBackendApplication.java # Application main & scheduling enablement
    │   │   └── resources/
    │   │       ├── application.properties   # Core configuration & watchdog thresholds
    │   │       └── application-test.properties # In-memory H2 configuration for tests
    │   └── test/java/com/leaksense/leaksense_backend/ # 27 JUnit 5 & Mockito test suites (271 tests)
    └── pom.xml                              # Maven configuration & dependencies
```

---

## 💻 Prerequisites & System Requirements

Ensure the following tools are installed on your workstation:

| Requirement | Minimum Version | Recommended Version | Purpose |
| :--- | :--- | :--- | :--- |
| **Java (JDK)** | OpenJDK 21 | Eclipse Temurin 21 (LTS) | Backend runtime & compiler |
| **Node.js** | Node.js 18.x | Node.js 20.x or 22.x LTS | Frontend execution runtime |
| **npm** | npm 9.x | npm 10.x | Frontend package manager |
| **MySQL Server** | MySQL 8.0 | MySQL 8.0+ | Relational data store |
| **Build Tool** | Maven 3.9+ | Included Maven Wrapper (`./mvnw`) | Backend build & test execution |

---

## 🚀 Getting Started & Local Setup

### 1. Database Setup
Log in to your local MySQL instance (port `3306`) and create the database schema:

```sql
CREATE DATABASE IF NOT EXISTS leaksense_db;
```

> **Note:** Hibernate JPA is configured with `spring.jpa.hibernate.ddl-auto=update`. Tables, foreign keys, and indexes will be created automatically on initial backend startup.

### 2. Backend Service Setup
Navigate to the backend directory and launch the application using the Maven wrapper:

```bash
# On Linux / macOS:
cd leaksense_backend
./mvnw spring-boot:run

# On Windows PowerShell:
cd d:\Leaksense\leaksense_backend
.\mvnw.cmd spring-boot:run
```

* The backend service will start on **`http://localhost:8080`**.
* [`DataInitializer`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/config/DataInitializer.java) will automatically seed the database with initial pipeline zones, sensor nodes, threshold settings, and default operator/admin accounts.

### 3. Frontend Application Setup
In a new terminal window, navigate to the frontend directory, install dependencies, and launch the Vite development server:

```bash
cd d:\Leaksense\leaksense-frontend
npm install
npm run dev
```

* The SCADA operator console will be available at **`http://localhost:5173`**.
* Unauthenticated visitors are directed to the **Public Landing Page** showcasing real-time system heartbeat telemetry.

---

## 🔐 Default Authentication Credentials

The system initializes with two pre-configured accounts:

| Role | Username | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Full access: SCADA Console, User Management, Safety Threshold Tuning, Valve Opening |
| **Operator** | `operator` | `operator123` | Operational access: Telemetry Monitoring, Alert Acknowledgment, Valve Closures |

> [!WARNING]
> Seed credentials are provided solely for development and evaluation. In production environments, administrators must immediately change default passwords via the User Management panel (`/api/users`) or provide credentials through external identity providers.

---

## ⚙️ Configuration & Environment Variables

Key runtime properties defined in [`application.properties`](file:///d:/Leaksense/leaksense_backend/src/main/resources/application.properties) can be customized directly or overridden via environment variables:

| Property Key | Default Value | Environment Variable | Description |
| :--- | :--- | :--- | :--- |
| `server.port` | `8080` | `PORT` | Backend HTTP port |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/leaksense_db` | `SPRING_DATASOURCE_URL` | MySQL JDBC connection string |
| `spring.datasource.username` | `root` | `SPRING_DATASOURCE_USERNAME` | MySQL database username |
| `spring.datasource.password` | *(empty / local)* | `SPRING_DATASOURCE_PASSWORD` | MySQL database password |
| `leaksense.jwt.expiration-ms` | `86400000` (24h) | `LEAKSENSE_JWT_EXPIRATION` | JWT token validity in milliseconds |
| `leaksense.node.offline-after-seconds`| `180` | `NODE_OFFLINE_SECONDS` | Inactivity window before flagging node as OFFLINE |
| `leaksense.autoclose.enabled` | `false` | `AUTOCLOSE_ENABLED` | Set `true` to auto-trip valves on critical breaches |
| `leaksense.command.timeout-seconds` | `120` | `COMMAND_TIMEOUT` | Timeout before flagging valve actuator failure |
| `leaksense.alarm.resound-minutes` | `5` | `ALARM_RESOUND_MINUTES` | Cooldown period before silenced alarm re-triggers |
| `leaksense.sensor.stuck-count` | `5` | `SENSOR_STUCK_COUNT` | Consecutive identical readings indicating stuck sensor |
| `leaksense.sensor.spike.methane-percent`| `100` | `SPIKE_METHANE_PCT` | Percentage jump flagging a methane spike |
| `leaksense.sensor.spike.pressure-percent`| `25` | `SPIKE_PRESSURE_PCT`| Sudden drop percentage flagging a pressure anomaly |
| `leaksense.sms.enabled` | `false` | `TWILIO_ENABLED` | Set `true` to enable Twilio emergency SMS alerts |
| `leaksense.sms.account-sid` | `""` | `TWILIO_ACCOUNT_SID` | Twilio Account SID |
| `leaksense.sms.auth-token` | `""` | `TWILIO_AUTH_TOKEN` | Twilio Auth Token |
| `spring.mail.username` | `""` | `MAIL_USERNAME` | SMTP account email address |
| `spring.mail.password` | `""` | `MAIL_PASSWORD` | SMTP account app password |
| `leaksense.report.weekly-cron` | `0 0 9 * * MON` | `REPORT_CRON` | Scheduled cron expression for weekly PDF audit |

---

## 📡 REST & WebSocket API Reference

### Public Endpoints (No Token Required)
* `POST /api/auth/login` — Authenticate operator/admin credentials and receive a JWT Bearer token.
* `GET /api/public/landing` — System capabilities, technical specifications, and marketing metadata.
* `GET /api/public/status` — Live system status ribbon (online node count, active alert count, system health).
* `POST /api/public/demo-request` — Submit technical inquiries or enterprise demo requests.
* `POST /api/ingest/reading` — Ingestion endpoint for edge sensor node telemetry packets.

### Telemetry & Live Telemetry Stream
* `GET /api/readings/live` — Fetches the most recent 50 buffered sensor telemetry samples.
* `GET /api/readings/latest` — Fetches the latest sample per sensor node.
* `GET /api/readings/history` — Multi-filter time-series queries with node and date range selectors.

### Safety Alerts & Audible Alarms
* `GET /api/alerts/active` — Active unacknowledged safety alerts.
* `PUT /api/alerts/{id}/ack` — Acknowledge an alert with the operator's username.
* `PUT /api/alerts/{id}/resolve` — Mark an alert as resolved and safe.
* `GET /api/alerts/history` — Paginated and filterable historical alert records.
* `GET /api/alarms/active` — Currently sounding audible siren alarm events.
* `PUT /api/alarms/{id}/silence` — Silence an active siren (initiates 5-minute resound timer).
* `PUT /api/alarms/{id}/clear` — Clear an alarm after normal readings are restored.

### Pipeline Valves & Actuator Controls
* `GET /api/valves` — Comprehensive status of all pipeline zone isolation valves.
* `POST /api/valves/{zoneId}/open` — Command a specific zone valve to open (`ADMIN` required).
* `POST /api/valves/{zoneId}/close` — Command a specific zone valve to close (`OPERATOR` or `ADMIN`).
* `POST /api/valves/emergency-close-all` — Immediately trip and close all facility isolation valves.
* `GET /api/valve-actions/recent` — Audit log of recent automated and manual valve movements.

### Historical Analytics & Reporting
* `GET /api/analytics/sensor-trends` — Multi-sensor time-series (Methane, Pressure, Oxygen, Risk Index).
* `GET /api/analytics/risk-distribution` — Hazard distribution counts across risk categories.
* `GET /api/analytics/alerts-timeline` — Daily alert frequency classified by severity level.
* `GET /api/analytics/alerts-by-sensor` — Incident breakdown categorized by sensor type.
* `GET /api/analytics/valve-activity` — Metrics on manual vs automated actuations.
* `GET /api/reports/weekly/pdf` — Stream generated executive weekly compliance PDF.
* `GET /api/reports/excel` — Download structured historical telemetry and alert Excel workbook.

### AI Copilot & Anomaly Insights
* `POST /api/ai/chat` — Contextual natural language assistant with live database awareness.
* `GET /api/ai/report-summary` — Dynamic system safety score ($0-100$) and narrative recommendations.
* `GET /api/ai/insights` — Real-time predictive anomaly feed with confidence scores.

### Administration & User Management
* `GET /api/users` — List registered operator and administrator accounts (`ADMIN` only).
* `POST /api/users` — Provision a new user account (`ADMIN` only).
* `DELETE /api/users/{id}` — Deactivate and remove a user account (`ADMIN` only).
* `GET /api/settings/thresholds` — Retrieve active safety threshold boundaries (`ADMIN` only).
* `PUT /api/settings/thresholds` — Update zone safety thresholds with persistent audit trail (`ADMIN` only).

### Real-Time WebSocket Topics (STOMP over `/ws`)
* `/topic/readings` — Broadcasts live telemetry packets as they are ingested.
* `/topic/alerts` — Pushes newly created safety alerts and resolution events.
* `/topic/alarms` — Pushes audible siren alarm status changes (ACTIVE, SILENCED, CLEARED).
* `/topic/notifications` — Pushes notifications to the in-app notification center.

---

## 🧪 Testing & Quality Assurance

The LeakSense software is verified with automated testing suites for both backend and frontend layers:

### 1. Backend Automated Tests (JUnit 5 & Mockito)
The backend test suite executes against an in-memory H2 database using [`application-test.properties`](file:///d:/Leaksense/leaksense_backend/src/test/resources/application-test.properties):

```bash
cd d:\Leaksense\leaksense_backend
.\mvnw.cmd test
```

* **Current Status**: **271 of 271 Tests Passing (100%)**.
* **Coverage Scope**:
  - `ThresholdEvaluatorTest`: Multi-tier thresholds, auto-valve shutoff triggers, baseline safety boundaries.
  - `NodeMonitorServiceTest`: 180s stale node heartbeat timeout and 120s valve stuck watchdog.
  - `SensorHealthServiceTest`: Spike jump detection and 5-cycle sensor freeze detection.
  - `AiEarlyWarningServiceTest`: Multi-variable derivative trajectory calculations ($\frac{\Delta P}{\Delta t}$ vs $\frac{\Delta \text{CH}_4}{\Delta t}$).
  - `ValveServiceTest`: Fine-grained role permissions, safety locks, and emergency master trips.
  - `PdfReportServiceTest` & `WeeklyReportServiceTest`: PDF document construction and scheduled dispatch.
  - `WebMvc Slice Tests`: Auth, Ingest, Alerts, Analytics, Reports, AI, and Notification controllers.

### 2. Frontend Linting & Production Build
The frontend is validated using **ESLint 9** and **Vite**:

```bash
cd d:\Leaksense\leaksense-frontend

# Run ESLint validation:
npm run lint

# Build production bundle:
npm run build
```

* **Current Status**: **0 Errors, 0 Warnings**. Production bundle compiles cleanly into `dist/`.

---

## 🔒 Security & Session Architecture

* **Stateless JWT Authentication**: Tokens are signed using HMAC-SHA256 and verified on every request via [`JwtAuthFilter`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/config/JwtAuthFilter.java).
* **Session Storage Isolation**: In the frontend, JWT tokens and user profiles are stored strictly in `sessionStorage`. Closing the browser tab destroys the active session, automatically reverting to the Public Landing Page upon reopen to protect shared SCADA control room terminals.
* **Axios Interceptors**: Automatically inject Bearer authentication headers into outgoing requests and intercept `401 Unauthorized` responses to clear invalid sessions and redirect to login.
* **Audit Trails**: Every manual or automated valve operation is logged to the `valve_actions` table with timestamps, operator IDs, and triggering rationale. Threshold changes are tracked in `threshold_audit_logs`.

---

## 📄 License

This software is distributed under the terms of the **MIT License**.
