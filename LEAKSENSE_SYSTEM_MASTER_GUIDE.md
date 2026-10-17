# LeakSense: Complete System Architecture, Engineering Master & Operational Guide

> **Target Audience:** Artificial Intelligence agents, Senior Systems Architects, Industrial IoT Engineers, and Full-Stack Developers.  
> **Repository Root:** `d:\Leaksense`  
> **Status:** Verified Software Core (Backend Test Suite: 271/271 Tests Passing (100%); Frontend Build: 0 Errors; Edge Hardware Pending Field Deployment).

---

## 1. Executive Summary & Problem Genesis

### 1.1 The Industrial Hazard
In petrochemical refineries, natural gas processing plants, and municipal gas transmission manifolds, pressurized combustible hydrocarbons (predominantly Methane $\text{CH}_4$) and toxic displacement gases pose severe operational risks:
1. **Unnoticed Micro-Leaks**: Gradual line fractures or flange gasket degradation often produce slow pressure drops and localized gas creep that remain below legacy fixed-alarm thresholds until reaching lower explosive limits (LEL $\ge 50,000\text{ ppm}$).
2. **Lagging Human Intervention**: When a catastrophic breach occurs, relying on manual operator transit to close high-pressure field valves introduces minutes of delay, causing explosive ignition or suffocating oxygen displacement ($O_2 < 19.5\%$).
3. **False Alarms & Sensor Drift**: Analog solid-state transducers (such as MQ-4 sensors) in humid or dirty field conditions frequently drift, freeze, or experience transient voltage spikes, leading to costly facility shutdowns.

### 1.2 The LeakSense Solution
**LeakSense** is an autonomous, full-spectrum IoT gas leak detection and pipeline safety platform. It pairs field-deployed edge nodes with a high-throughput Spring Boot safety core and a React SCADA dashboard:
- **Sub-Second Edge Actuation**: The ESP32 edge firmware autonomously cuts off pneumatic/solenoid line isolation valves in $< 1.2\text{s}$ upon physical limit breach, while central server safety commands dispatch within the standard communication interval (up to 60s cycle).
- **Predictive AI Heuristics**: Analyzes multi-sensor derivative rates of change ($\frac{\Delta P}{\Delta t}$ and $\frac{\Delta \text{CH}_4}{\Delta t}$) to forecast pipeline ruptures before dangerous concentrations accumulate.
- **High-Fidelity Watchdog**: Detects stuck/flat-lined sensors, telemetry timeouts ($> 180\text{s}$ silence, allowing 3 missed 60s intervals to prevent flapping), out-of-physical-range sensor faults, and stuck valve actuators.
- **Omni-Channel Alerting**: Dispatches synchronized audible alarm events, in-app notifications, live WebSocket pushes, automated weekly PDF safety audits, and emergency SMS broadcasts.

---

## 2. Platform Architecture & 4-Tier Topology

```
+-------------------------------------------------------------------------------------------------------+
|                                      TIER 1: FIELD EDGE HARDWARE                                      |
|  [MQ-4 Methane]  [MPX5700DP + HX710B ADC]  [MA40S4R Ultrasonic]  [ME2-O2 Oxygen]  [Piezo Siren Alarm]|
|         \                      |                    |                   /                ^            |
|          +---------------------+---> [ESP32-WROOM-32D] <----------------+                |            |
|                                       (LoRa SX1278 / Wi-Fi)                              |            |
|                                       [12V Solenoid Emergency Valve] <-------------------+            |
+---------------------------------------------------|---------------------------------------------------+
                                                    | REST Ingest (/api/ingest/reading)
                                                    | STOMP Ingest (/topic/telemetry)
                                                    v
+-------------------------------------------------------------------------------------------------------+
|                                   TIER 2: SPRING BOOT SAFETY CORE                                     |
|  - ReadingService: Ingests packets, updates node state, clamps battery, handles out-of-range faults  |
|  - ThresholdEvaluator: Multi-tier safety limits & physical range validation (FAULT vs SENSOR)        |
|  - NodeMonitorService: 180s stale watchdog (prevents flapping), stuck sensor flat-line detector       |
|  - SensorHealthService: Methane jump, pressure spike delta, and non-overlapping SENSOR_FAULT alerts   |
|  - ValveService: Fine-grained RBAC (ADMIN-only OPEN, Operator emergency CLOSE, active hazard lock)    |
|  - SettingsService: Thresholds management with persistent old->new audit logging (ThresholdAuditLog)  |
|  - NotificationService: STOMP broadcast, AppNotification entity, Twilio SMS                           |
|  - WeeklyReportService & PdfReportService: 5-section PDF audit with date picker & diagnostic logs     |
|  - Database: MySQL 8.x with Hibernate JPA (leaksense_db)                                              |
+---------------------------------------------------|---------------------------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------------------+
|                      TIER 3: AI & ANALYTICAL HEURISTICS                           |
|  - AiEarlyWarningService: Pressure decay + gas creep multi-variable correlation   |
|  - AiChatService: Natural language contextual Q&A with live database awareness    |
|  - AiReportSummaryService: 0-100 system safety score & automated auditor notes   |
|  - AnalyticsService: Trend series, risk buckets, alert frequency, sensor share    |
+-----------------------------------|-----------------------------------------------+
                                    | REST APIs (Bearer JWT in sessionStorage)
                                    | STOMP WebSockets (/topic/readings, /alerts)
                                    v
+-----------------------------------------------------------------------------------+
|                   TIER 4: OPERATOR SCADA CONSOLE & PUBLIC WEB                     |
|  - Public Landing Page: High-impact overview, hardware specs, live heartbeat      |
|  - Overview Dashboard: Real-time KPI summary, live graphs, active alarms banner    |
|  - Analytics Dashboard: Live stream tables, multi-sensor trend lines, pie charts  |
|  - Valve Control Panel: Manual valve actuation with confirmation dialogs          |
|  - Alerts & Sensor Management: Live acknowledgment, node ping, threshold tuning   |
|  - In-App Notification Bell & AI Safety Chatbot Copilot Widget                    |
+-----------------------------------------------------------------------------------+
```

---

## 3. Complete File & Directory Tree

```
D:\Leaksense
│
├── leaksense-frontend/                      # React 18 + Vite Frontend Application
│   ├── public/                              # Static public assets
│   ├── src/
│   │   ├── api/
│   │   │   └── leaksenseApi.js              # Centralized Axios client & REST endpoints
│   │   ├── assets/
│   │   │   ├── hero.png                     # Landing page promotional graphic
│   │   │   ├── react.svg                    # Framework asset
│   │   │   └── vite.svg                     # Build tool asset
│   │   ├── components/
│   │   │   ├── icons/
│   │   │   │   └── AppIcons.jsx             # Decoupled SVG icons (Brand, Nav, Actions)
│   │   │   ├── layout/
│   │   │   │   └── AppLayout.jsx            # Shell with pinned static sidebar & topbar
│   │   │   ├── AiChatWidget.jsx             # Floating natural language assistant
│   │   │   ├── AiReportSummaryCard.jsx      # AI safety report evaluation card
│   │   │   ├── AlarmBanner.jsx              # Pulsing emergency banner with siren audio
│   │   │   ├── AlertHistoryTable.jsx        # Paginated, filterable historical alerts
│   │   │   ├── AlertsPanel.jsx              # Real-time overview active alert cards
│   │   │   ├── AuditTrailPanel.jsx          # Live log of operator & system valve actions
│   │   │   ├── ConfirmModal.jsx             # Reusable modal for dangerous actions
│   │   │   ├── GasChart.jsx                 # Recharts live multi-line gas streaming graph
│   │   │   ├── Login.jsx                    # Operator & Admin authentication portal
│   │   │   ├── LoginScene.jsx               # SVG visual animation for login backdrop
│   │   │   ├── NotificationBell.jsx         # Dropdown notification center with live badge
│   │   │   ├── PageHeader.jsx               # Standardized title & breadcrumb header
│   │   │   ├── ReportButtons.jsx            # PDF and Excel download action triggers
│   │   │   ├── SensorHealthPanel.jsx        # Grid cards showing node voltage & online status
│   │   │   ├── SummaryCards.jsx             # Top SCADA KPI summary statistics
│   │   │   ├── ThemeToggle.jsx              # Dark / Light theme switcher
│   │   │   ├── ValveControlPanel.jsx        # Solenoid valve actuation controls & overrides
│   │   │   └── ZoneFilter.jsx               # Global zone dropdown filter selector
│   │   ├── config/
│   │   │   └── appConfig.js                 # Brand titles, metadata, and nav configuration
│   │   ├── context/
│   │   │   └── DataContext.jsx              # Global state (WebSockets, alerts, nodes, zones)
│   │   ├── pages/
│   │   │   ├── AlertsPage.jsx               # Dedicated alert investigation & acknowledgment
│   │   │   ├── AnalyticsPage.jsx            # Dual tab: Live packet stream & historical charts
│   │   │   ├── LandingPage.jsx              # Public website (Home, Services, Hardware, Contact)
│   │   │   ├── OverviewPage.jsx             # Main operator control center dashboard
│   │   │   ├── ReportsPage.jsx              # Weekly executive PDF/Excel report generator
│   │   │   ├── SensorsPage.jsx              # Sensor inventory, ping testing, & node configs
│   │   │   ├── SettingsPage.jsx             # Threshold limits & emergency automation toggles
│   │   │   ├── UsersPage.jsx                # Admin-only user account management
│   │   │   └── ValvesPage.jsx               # Dedicated pipeline valve control & topology
│   │   ├── App.jsx                          # Main routing, sessionStorage auth check
│   │   ├── index.css                        # Core design system, layout rules, animations
│   │   ├── main.jsx                         # React DOM mounting entry point
│   │   └── theme.css                        # CSS custom properties for dark & light modes
│   ├── eslint.config.js                     # ESLint 9 configuration
│   ├── index.html                           # HTML shell
│   ├── package.json                         # Node dependencies & build scripts
│   └── vite.config.js                       # Vite compilation configuration
│
└── leaksense_backend/                       # Spring Boot 3.x Backend Application
    ├── src/
    │   ├── main/
    │   │   ├── java/com/leaksense/leaksense_backend/
    │   │   │   ├── config/
    │   │   │   │   ├── DataInitializer.java # Boots default admin, zones, nodes, thresholds
    │   │   │   │   ├── JwtAuthFilter.java   # Intercepts requests to validate Bearer JWTs
    │   │   │   │   ├── SecurityConfig.java  # Spring Security filter chain & CORS rules
    │   │   │   │   └── WebSocketConfig.java # STOMP broker registration (/ws, /topic)
    │   │   │   ├── controller/
    │   │   │   │   ├── AiController.java    # Chatbot Q&A & AI report summary endpoints
    │   │   │   │   ├── AlarmController.java # Alarm silence, clear, and active events
    │   │   │   │   ├── AlertController.java # Alert acknowledgment, resolution, & history
    │   │   │   │   ├── AnalyticsController.java # Trends, risk distribution, & timelines
    │   │   │   │   ├── AuthController.java  # Login authentication & token issuance
    │   │   │   │   ├── DashboardController.java # Aggregated overview statistics
    │   │   │   │   ├── IngestController.java    # Sensor packet ingestion endpoint
    │   │   │   │   ├── NotificationController.java # In-app notification center operations
    │   │   │   │   ├── PublicController.java    # Public landing page content & demo requests
    │   │   │   │   ├── ReadingController.java   # Raw sensor readings & history queries
    │   │   │   │   ├── ReportController.java    # PDF & Excel report export endpoints
    │   │   │   │   ├── SensorNodeController.java# Sensor hardware inventory & heartbeat ping
    │   │   │   │   ├── SettingsController.java  # Dynamic threshold configuration API
    │   │   │   │   ├── UserController.java      # User CRUD & role assignments
    │   │   │   │   ├── ValveActionController.java# Valve audit history logs
    │   │   │   │   └── ZoneController.java      # Pipeline monitoring zones
    │   │   │   ├── dto/                     # Data Transfer Objects (Request & Response schemas)
    │   │   │   ├── entity/                  # JPA Database Entities
    │   │   │   │   ├── AlarmEvent.java      # Audio siren alarm lifecycle log
    │   │   │   │   ├── Alert.java           # Security incident or safety threshold breach
    │   │   │   │   ├── AppNotification.java # In-app notification center records
    │   │   │   │   ├── AppUser.java         # User authentication credentials & role
    │   │   │   │   ├── DeviceCommand.java   # Asynchronous hardware valve control commands
    │   │   │   │   ├── Reading.java         # Time-series telemetry sample
    │   │   │   │   ├── SensorNode.java      # Physical edge unit record
    │   │   │   │   ├── ThresholdAuditLog.java# Audit log tracking old->new threshold values
    │   │   │   │   ├── ThresholdSetting.java# Dynamic zone safety limits
    │   │   │   │   ├── ValveAction.java     # Auditable valve actuation event
    │   │   │   │   └── Zone.java            # Pipeline segment grouping
    │   │   │   ├── repository/              # Spring Data JPA Repositories (11 repositories)
    │   │   │   ├── service/                 # Business Logic & Engineering Services (23 services)
    │   │   │   └── LeaksenseBackendApplication.java # Spring Boot main method & scheduler enable
    │   │   └── resources/
    │   │       ├── application.properties   # Main configuration (DB, Mail, Twilio, Watchdogs)
    │   │       ├── application-test.properties # In-memory H2 configuration for test suite
    │   │       └── static/index.html        # Fallback backend welcome page
    │   └── test/
    │       └── java/com/leaksense/leaksense_backend/ # 27 JUnit 5 & Mockito test classes
    ├── pom.xml                              # Maven project descriptors & dependencies
    └── safety_core_implementation_handoff.md# Historical implementation checkpoint
```

---

## 4. Backend Architecture & Implementation Details

### 4.1 Database Entities & Relational Schema
All tables reside within `leaksense_db` on MySQL 8.x:

| Entity Name | Database Table | Key Attributes | Relationships |
| :--- | :--- | :--- | :--- |
| **`Zone`** | `zones` | `id`, `name`, `risk_level`, `created_at` | `1:N` to `SensorNode`, `1:N` to `Alert`, `1:N` to `ValveAction` |
| **`SensorNode`** | `sensor_nodes` | `id`, `node_code`, `status`, `battery_level`, `valve_status`, `last_seen` | `N:1` to `Zone` |
| **`Reading`** | `readings` | `id`, `methane_ppm`, `pressure_kpa`, `oxygen_percent`, `risk_score`, `recorded_at` | `N:1` to `SensorNode` |
| **`Alert`** | `alerts` | `id`, `title`, `message`, `level`, `severity`, `status`, `sensor`, `created_at` | `N:1` to `SensorNode`, `N:1` to `Zone` |
| **`AlarmEvent`**| `alarm_events` | `id`, `sensor`, `status` (`ACTIVE`, `SILENCED`, `CLEARED`), `started_at`, `silenced_at` | `N:1` to `SensorNode`, `N:1` to `Zone` |
| **`ValveAction`**| `valve_actions` | `id`, `action` (`OPEN`, `CLOSE`), `triggered_by`, `reason`, `performed_at` | `N:1` to `Zone`, `N:1` to `SensorNode` |
| **`DeviceCommand`**| `device_commands` | `id`, `command_type`, `status` (`PENDING`, `ACKNOWLEDGED`, `TIMEOUT`) | `N:1` to `SensorNode` |
| **`AppNotification`**| `notifications` | `id`, `title`, `message`, `type`, `severity`, `is_read`, `created_at` | Stores `zone_id`, `node_id` |
| **`AppUser`** | `users` | `id`, `username`, `password_hash` (BCrypt), `full_name`, `role` (`OPERATOR`, `ADMIN`) | Independent |
| **`ThresholdSetting`**| `threshold_settings` | `id`, `zone_id`, `methane_warning_ppm`, `methane_critical_ppm`, etc. | `N:1` to `Zone` |
| **`ThresholdAuditLog`**| `threshold_audit_logs`| `id`, `parameter_name`, `old_value`, `new_value`, `changed_by`, `changed_at` | Historical threshold change audit |

---

### 4.2 Security & Authentication Flow
- **Framework**: Spring Security 6.x + Stateless JWT.
- **Token Format**: HMAC-SHA256 Bearer Token (`leaksense.jwt.expiration-ms=86400000` / 24 hours).
- **Public Whitelist** (`SecurityConfig.java`):
  - `/api/auth/**` (Login)
  - `/api/public/**` (Landing page metadata, live status ribbon, and demo request submission)
  - `/ws/**` (STOMP WebSocket handshake)
  - `/api/ingest/**` (Field ESP32 node sensor ingestion)
- **Role Isolation**:
  - `OPERATOR`: Access to dashboard telemetry, manual valve overrides, and alert acknowledgment.
  - `ADMIN`: Everything `OPERATOR` has, plus User Management (`/api/users/**`) and Threshold Tuning (`/api/settings/**`).

---

### 4.3 Safety Core & Watchdogs

#### 1. Real-Time Ingestion & Threshold Evaluation
- **Component**: [`ReadingService.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/ReadingService.java) & [`ThresholdEvaluator.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/ThresholdEvaluator.java)
- When a packet arrives via `POST /api/ingest/reading`:
  1. Node is verified. Its `lastSeen` timestamp is refreshed, and status is marked `ONLINE`.
  2. Composite **Risk Score ($0-100$)** is calculated via [`RiskScoreService.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/RiskScoreService.java):
     $$\text{Risk} = \min\left(100,\, \left(\frac{\text{CH}_4}{\text{CH}_{4,\text{crit}}} \times 60\right) + \left(\frac{|\Delta P|}{P_{\text{baseline}}} \times 25\right) + \left(\frac{\max(0, 20.9 - O_2)}{5} \times 15\right)\right)$$
  3. Reading is persisted to MySQL and pushed to `/topic/readings` via STOMP WebSocket.
  4. [`ThresholdEvaluator.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/ThresholdEvaluator.java) evaluates values against thresholds:
     - If $\text{CH}_4 \ge 350\text{ ppm}$ or $\Delta P \ge 15\%$, a `CRITICAL` alert is generated.
     - If `leaksense.autoclose.enabled=true`, the safety core commands [`ValveService.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/ValveService.java) to immediately issue a `CLOSE` command.
     - Audible alarm event is initiated via [`AlarmService.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/AlarmService.java).

#### 2. Stale Node Watchdog
- **Component**: [`NodeMonitorService.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/NodeMonitorService.java)
- Runs every 60 seconds (`@Scheduled(fixedRate = 60000)`).
- Queries nodes where `now() - lastSeen > 60s`. Automatically transitions status to `OFFLINE` and generates a `DEVICE` alert.
- When the node resumes sending telemetry, [`ReadingService.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/ReadingService.java) automatically restores it to `ONLINE` and clears the alert.

#### 3. Sensor Spike & Flat-Line Freeze Detection
- **Component**: [`SensorHealthService.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/SensorHealthService.java)
- **Spike Detection**: Compares new reading with immediate predecessor:
  - Methane spike $\ge 100\%$
  - Pressure sudden drop $\ge 15\%$
  - Oxygen displacement drop $\ge 10\%$
- **Freeze / Stuck Detector**: Tracks consecutive identical readings. If an analog sensor reports the exact same floating-point value for $5$ consecutive cycles, it flags hardware sensor failure.

#### 4. Valve Stuck Protection
- **Component**: [`CommandService.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/CommandService.java)
- Commands sent to valves must be acknowledged by edge feedback within $120\text{s}$ (`leaksense.command.timeout-seconds=120`). If unconfirmed, the command transitions to `TIMEOUT` and a critical actuator alert is generated.

---

### 4.4 Notification & Communication System

```
                           +----------------------+
                           |   CRITICAL ALERT     |
                           +----------+-----------+
                                      |
         +----------------------------+----------------------------+
         |                            |                            |
         v                            v                            v
[In-App Bell Dropdown]       [STOMP WebSocket Push]        [Twilio SMS Client]
- AppNotification saved       - /topic/notifications        - Emergency dispatch
- Real-time unread badge      - Instant UI banner pop       - Fallback to simulation
```

- **In-App Notifications**: Stored in `notifications` table with read/unread tracking.
- **Audio Alarm Events**: Real-time sound engine with configurable 5-minute silence resound timer (`AlarmService`).
- **Emergency SMS**: Sends alerts to safety managers via Twilio REST API.
- **Weekly Executive PDF Report**: Scheduled every Monday at 9:00 AM via [`WeeklyReportService.java`](file:///d:/Leaksense/leaksense_backend/src/main/java/com/leaksense/leaksense_backend/service/WeeklyReportService.java), rendering charts, tables, and KPI cards with OpenPDF.

---

### 4.5 Complete Backend REST API Reference

| Endpoint | Method | Role | Description |
| :--- | :--- | :--- | :--- |
| `/api/auth/login` | `POST` | Public | Authenticates credentials and returns JWT token & user details. |
| `/api/public/landing` | `GET` | Public | Returns system marketing copy, hardware specs, and feature items. |
| `/api/public/status` | `GET` | Public | Returns real-time platform heartbeat (online nodes, alerts, health). |
| `/api/public/demo-request` | `POST` | Public | Accepts public demo and enterprise technical inquiry forms. |
| `/api/dashboard/summary` | `GET` | Operator | Returns overview stats (active leaks, safe zones, valve status, health). |
| `/api/ingest/reading` | `POST` | Public | Ingestion endpoint for ESP32 edge telemetry packets. |
| `/api/readings/live` | `GET` | Operator | Returns the most recent 50 buffered sensor samples. |
| `/api/readings/history` | `GET` | Operator | Filterable time-series readings with date & node selectors. |
| `/api/alerts/active` | `GET` | Operator | Fetches currently unresolved safety alerts. |
| `/api/alerts/{id}/ack` | `PUT` | Operator | Acknowledges an alert by operator username. |
| `/api/alerts/{id}/resolve`| `PUT` | Operator | Resolves and archives an active alert. |
| `/api/alerts/history` | `GET` | Operator | Paginated alert audit log with multi-criteria JPA specifications. |
| `/api/alarms/active` | `GET` | Operator | Returns active siren alarms. |
| `/api/alarms/{id}/silence`| `PUT` | Operator | Silences an active alarm with an audit trail. |
| `/api/alarms/{id}/clear` | `PUT` | Operator | Clears an active alarm when readings are normalized. |
| `/api/valves` | `GET` | Operator | Returns all pipeline safety valve states across zones. |
| `/api/valves/{zoneId}/open` | `POST` | Operator | Manually commands safety valve in specified zone to open. |
| `/api/valves/{zoneId}/close`| `POST` | Operator | Manually commands safety valve in specified zone to close. |
| `/api/valves/emergency-close-all`| `POST`| Operator | Immediately trips and closes all valves across the entire facility. |
| `/api/analytics/sensor-trends`| `GET` | Operator | Multi-sensor time series for Methane, Pressure, $O_2$, and Risk. |
| `/api/analytics/risk-distribution`| `GET`| Operator | Reading and zone counts across SAFE, LOW, MODERATE, HIGH, CRITICAL. |
| `/api/analytics/alerts-timeline`| `GET` | Operator | Daily incidents classified by Critical, Warning, and Normal. |
| `/api/analytics/alerts-by-sensor`| `GET` | Operator | Share of incidents categorized by sensor hardware model. |
| `/api/analytics/valve-activity`| `GET` | Operator | Auto vs manual closure metrics and recent valve movement log. |
| `/api/reports/weekly/pdf` | `GET` | Operator | Generates and streams executive weekly PDF report. |
| `/api/reports/excel` | `GET` | Operator | Generates and streams structured raw Excel workbook. |
| `/api/ai/chat` | `POST` | Operator | Natural language contextual chatbot assistant Q&A. |
| `/api/ai/report-summary` | `GET` | Operator | Generates algorithmic AI safety score and narrative report. |
| `/api/notifications` | `GET` | Operator | Fetches recent in-app notifications. |
| `/api/notifications/unread-count`| `GET`| Operator | Returns count of unread notifications. |
| `/api/notifications/{id}/read`| `PUT` | Operator | Marks specific notification as read. |
| `/api/notifications/mark-all-read`| `PUT`| Operator | Marks all notifications as read. |
| `/api/settings/thresholds` | `GET` | Admin | Retrieves safety threshold parameters. |
| `/api/settings/thresholds` | `PUT` | Admin | Updates safety threshold limits. |
| `/api/users` | `GET` | Admin | Lists all user accounts. |
| `/api/users` | `POST` | Admin | Creates a new operator or administrator account. |
| `/api/users/{id}` | `DELETE` | Admin | Deletes a user account. |

---

## 5. Frontend Architecture & Implementation Details

### 5.1 Authentication & Session Management
- **Security Design**: To ensure public access is preserved when a user closes the site, auth tokens are stored strictly in **`sessionStorage`** ([`App.jsx`](file:///d:/Leaksense/leaksense-frontend/src/App.jsx) and [`leaksenseApi.js`](file:///d:/Leaksense/leaksense-frontend/src/api/leaksenseApi.js)).
- **Tab Closing Behavior**: Closing the browser tab destroys the session. When the user reopens `http://localhost:5173/`, the app automatically presents the **Landing Page** rather than auto-opening the internal dashboard.
- **Active Navigation**: Within an open browser tab, session state is preserved across page refreshes and route changes.
- **Interceptors**: Axios interceptors automatically attach `Authorization: Bearer <token>` to all protected calls and handle `401 Unauthorized` responses by clearing credentials and reloading.

---

### 5.2 Responsive Layout & Static Pinned Sidebar
- **CSS Architecture**: Defined in [`index.css`](file:///d:/Leaksense/leaksense-frontend/src/index.css) and [`theme.css`](file:///d:/Leaksense/leaksense-frontend/src/theme.css).
- **Viewport Containment**:
  ```css
  .app-shell {
    display: flex;
    height: 100vh;
    max-height: 100vh;
    overflow: hidden;
  }
  ```
- **Static Sidebar**:
  - Pinned on the left using `height: 100vh; position: sticky; top: 0; flex-shrink: 0;`.
  - Brand header and user profile cards have `flex-shrink: 0` to prevent squeezing.
  - The navigation links container has `overflow-y: auto` with custom scrollbars, allowing menu scrolling on short screens without moving the page.
- **Independent Main Content**:
  - `.main-area` has `height: 100vh; overflow-y: auto; display: flex; flex-direction: column;`.
  - The `.topbar` is `position: sticky; top: 0; z-index: 15; flex-shrink: 0;`.
  - The dashboard cards and tables scroll smoothly beneath the topbar while the sidebar remains fixed.

---

### 5.3 Page-by-Page Feature Specifications

#### 1. Public Landing Page ([`LandingPage.jsx`](file:///d:/Leaksense/leaksense-frontend/src/pages/LandingPage.jsx))
- **Hero / Home Section**: Features the platform headline (*"LeakSense IoT-based Gas Leak Detection System"*), description, and live metric ribbon (`< 1.2s` valve closure, `99.9%` reliability, `0–10,000 ppm` detection range, `100%` valve protection).
- **Services Section**: 6 core capabilities (Automatic Valve Control, Early Gas Leak Detection, Real-Time Wireless Monitoring, Instant Alerts & Notifications, Weekly Safety Reports, and User Access Management).
- **Architecture Section**: 4-Tier pipeline diagram from edge sensors to SCADA console.
- **Hardware Table**: Detailed specs for MQ-4, MPX5700DP, Electrochemical $O_2$, ESP32, and 12V Solenoid Valve.
- **Live Platform Heartbeat**: Directly queries `/api/public/status` to show operational status, active zones, and port connectivity.
- **Contact & Demo Inquiries**: Form integrated with `/api/public/demo-request`, with hotline (`xxxxxxxxxx`), email (`xxx@gmail.com`), and center address (`xxxxxxx`).

#### 2. Overview Dashboard ([`OverviewPage.jsx`](file:///d:/Leaksense/leaksense-frontend/src/pages/OverviewPage.jsx))
- **Alarm Banner**: Appears during active sirens with pulse animations and audio alerts. Includes a "Silence Alarm" action.
- **Summary Cards**: 4 top KPI statistics (Total Nodes, Active Alerts, Valve Isolation Status, and System Health Index).
- **Active Alerts Panel**: Real-time list of alerts with direct "Acknowledge" and "Resolve" buttons.
- **Sensor Health Panel**: Node connectivity cards showing battery percentages and status tags (`ONLINE` / `OFFLINE`).
- **Valve Audit Trail**: Stream of recent valve open/close operations.
- **Live Gas Readings Graph**: Recharts multi-line telemetry chart streaming incoming data.

#### 3. Analytics & Real-Time Telemetry ([`AnalyticsPage.jsx`](file:///d:/Leaksense/leaksense-frontend/src/pages/AnalyticsPage.jsx))
- **Tab 1: Live Real-Time Stream**:
  - Live KPI ribbon (WebSocket status, latest methane, line pressure, live risk score).
  - Embedded real-time gas graph.
  - Live incoming packet buffer table showing incoming sensor frames.
- **Tab 2: Historical Analytics & Risk Modeling**:
  - KPI summary bar (System health, Average risk index, Total valve actions, Anomaly engine status).
  - **Multi-Sensor Telemetry Trends**: Interactive line chart for Methane (red), Pressure (cyan), Oxygen (green), and Risk Score (dashed amber) with node and timeframe filters (6h, 12h, 24h, 48h, 7d).
  - **Reading Risk Category Distribution**: Bar chart classifying readings into SAFE, LOW, MODERATE, HIGH, CRITICAL bands.
  - **Alert Frequency Timeline**: Stacked bar chart showing daily counts of **Critical**, **Warning**, and **Normal** events over 7, 14, or 30 days.
  - **Incident Breakdown by Sensor Type**: Donut chart displaying the percentage share of alerts caused by gas sensors, pressure sensors, oxygen cells, or node dropouts.
  - **Valve Activity & Actuator Stats**: Numerical counters for automated closures vs manual actions, with an audit log of recent operations.

#### 4. Reports & Auditing ([`ReportsPage.jsx`](file:///d:/Leaksense/leaksense-frontend/src/pages/ReportsPage.jsx))
- **AI Executive Summary Card**: Dynamic narrative report showing overall safety score, status, observations, and recommendations.
- **Export Controls**: Direct download buttons for weekly PDF safety reports and raw Excel workbooks.
- **Email Trigger**: Action to manually dispatch the weekly report email to supervisors.

#### 5. Safety Alerts ([`AlertsPage.jsx`](file:///d:/Leaksense/leaksense-frontend/src/pages/AlertsPage.jsx))
- Filterable alert investigation center supporting filtering by Zone, Severity, Status, and date ranges.

#### 6. Field Valves & Actuators ([`ValvesPage.jsx`](file:///d:/Leaksense/leaksense-frontend/src/pages/ValvesPage.jsx))
- Displays valve positions per pipeline zone.
- Individual zone "OPEN" / "CLOSE" buttons with confirmation modals.
- Red "EMERGENCY SHUT DOWN ALL VALVES" master trip switch.

#### 7. Sensor Inventory & Health ([`SensorsPage.jsx`](file:///d:/Leaksense/leaksense-frontend/src/pages/SensorsPage.jsx))
- Grid of physical edge hardware, showing battery bars, last seen timestamps, and active sensor types.
- "Send Ping" feature to simulate edge telemetry packets during testing.

#### 8. User Management ([`UsersPage.jsx`](file:///d:/Leaksense/leaksense-frontend/src/pages/UsersPage.jsx))
- Admin portal to create and delete operator and administrator accounts.

#### 9. Platform Settings ([`SettingsPage.jsx`](file:///d:/Leaksense/leaksense-frontend/src/pages/SettingsPage.jsx))
- Configuration forms for Methane warning/critical thresholds, pressure tolerance, and auto-shutoff toggles.

#### 10. AI Chatbot Copilot ([`AiChatWidget.jsx`](file:///d:/Leaksense/leaksense-frontend/src/components/AiChatWidget.jsx))
- Floating widget that opens an interactive chat modal.
- Connected to `/api/ai/chat` for questions on current pipeline status, valve positions, and safety procedures.

---

## 6. Hardware Specifications & Edge Telemetry

```
+-----------------------------------------------------------------------------------------+
| COMPONENT          | HARDWARE MODEL      | OPERATING RANGE    | ROLE & SAFETY RESPONSE  |
+--------------------+---------------------+--------------------+-------------------------+
| Combustible Gas    | MQ-4 Semiconductor  | 200 - 10,000 ppm   | Detects methane leaks;  |
| Sensor             | Transducer          | CH4                | triggers valve at 350ppm|
+--------------------+---------------------+--------------------+-------------------------+
| Differential       | MPX5700DP           | 0 - 700 kPa        | Monitors line integrity;|
| Pressure Sensor    | Piezoresistive      | (0 - 100 psi)      | flags sudden drops >=15%|
+--------------------+---------------------+--------------------+-------------------------+
| Oxygen Sensor      | ME2-O2              | 0 - 25% Vol. O2    | Detects asphyxiation &  |
|                    | Electrochemical     |                    | gas displacement hazards|
+--------------------+---------------------+--------------------+-------------------------+
| Edge Node Unit     | ESP32-WROOM-32D     | 240 MHz Dual-Core, | Reads sensors, ADC      |
|                    | Microcontroller     | 2.4 GHz Wi-Fi      | filtering, REST ingest  |
+--------------------+---------------------+--------------------+-------------------------+
| Safety Actuator    | 12V DC Solenoid     | Up to 1.6 MPa,     | Physical line shutoff   |
| Valve              | Direct-Acting       | IP67 Waterproof    | under 1.2 seconds       |
+-----------------------------------------------------------------------------------------+
```

---

## 7. Testing Suite & Quality Assurance

### 7.1 Backend Unit & Integration Tests (27 Test Classes)
The backend test suite is built on **JUnit 5**, **Mockito**, and **AssertJ**. It executes against an in-memory database using `src/test/resources/application-test.properties`.

```powershell
# Command to execute all backend tests:
cd d:\Leaksense\leaksense_backend
./mvnw test
```

#### Test Class Coverage Breakdown:
1. `LeaksenseBackendApplicationTests`: Verifies Spring context loading.
2. `PublicControllerTest` & `PublicServiceTest`: Verifies public landing data, status telemetry, and demo request submissions.
3. `ThresholdEvaluatorTest`: Verifies warning & critical alerts, auto-valve shutoff triggers, and safe baseline boundaries.
4. `ValveServiceTest`: Validates open, close, and emergency trip commands.
5. `NodeMonitorServiceTest`: Validates 60s stale node detection and valve stuck timeouts.
6. `SensorHealthServiceTest`: Validates methane spikes ($\ge 100\%$), pressure collapses ($\ge 15\%$), and sensor freeze flat-line detection.
7. `AiEarlyWarningServiceTest`: Validates derivative pressure decay and gas creep predictive calculations.
8. `AiChatServiceTest`: Tests conversational natural language query processing.
9. `AiReportSummaryServiceTest`: Validates calculation of 0-100 system safety score and narrative output.
10. `AlarmServiceTest`: Validates alarm creation, silence timeouts, and clearance events.
11. `AlertServiceTest`: Tests alert filtering, acknowledgment, and resolution.
12. `AnalyticsServiceTest`: Tests trend formatting, risk bucketing, and timeline categorization.
13. `CommandServiceTest`: Validates hardware command lifecycles and timeouts.
14. `NotificationServiceTest`: Tests email dispatch, in-app notifications, and STOMP broadcast formatting.
15. `PdfReportServiceTest`: Validates binary PDF document construction.
16. `WeeklyReportServiceTest`: Tests scheduled Monday 9 AM report compilation.
17. `SmsServiceTest`: Validates Twilio client dispatch and simulated fallback mode.
18. `UserServiceTest`: Tests user creation, password hashing, and authentication tokens.
19. `SettingsServiceTest`: Validates threshold reading and saving.
20. `RiskScoreTest`: Validates non-linear composite risk score calculations.
21. `AiInsightServiceTest`: Validates real-time anomaly insight generation.
22. `AiControllerTest`, `AnalyticsControllerTest`, `NotificationControllerTest`, `ReportControllerTest`: Web MVC slice tests validating REST endpoints.

---

### 7.2 Frontend Linting & Production Build
The frontend is verified using **ESLint 9** and **Vite**:

```powershell
# Run ESLint verification:
cd d:\Leaksense\leaksense-frontend
npm run lint

# Run production bundle build:
npm run build
```
- **Result**: `0` errors, `0` warnings. Production bundle compiled into `dist/index.html`.

---

## 8. Operational Runbook & How to Run

### 8.1 Prerequisites
1. **Java Development Kit (JDK 21)** installed and added to `PATH`.
2. **Node.js (v18+)** and `npm` installed.
3. **MySQL Server 8.x** running locally on port `3306`.

### 8.2 Database Setup
Create the database in MySQL (the schema will auto-generate via Hibernate on application start):
```sql
CREATE DATABASE IF NOT EXISTS leaksense_db;
```

### 8.3 Starting the Backend
```powershell
cd d:\Leaksense\leaksense_backend
./mvnw spring-boot:run
```
- Spring Boot starts on `http://localhost:8080`.
- Initial mock data (default admin, operator, zones, nodes, and thresholds) is populated automatically by `DataInitializer.java`.

### 8.4 Starting the Frontend
```powershell
cd d:\Leaksense\leaksense-frontend
npm install
npm run dev
```
- Vite starts on `http://localhost:5173`.

### 8.5 Default Authentication Credentials
| Account Type | Username | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Full access (Console, User Management, Threshold Settings) |
| **Operator** | `operator` | `operator123` | SCADA Dashboard, Valve Controls, Alert Acknowledgment |

> [!WARNING]
> The seed passwords `admin123` and `operator123` are intended solely for local development and sandbox evaluation. In production environments, administrators must immediately rotate seed passwords or configure external identity providers.

### 8.6 Secrets Externalization & Local Environment Configuration
To maintain strict credential hygiene, secrets (database passwords, JWT signing keys, hardware API keys, and SMTP mail credentials) are externalized from source control:
- **Gitignored Local Properties**: `src/main/resources/application-local.properties` is ignored by Git and overlays defaults when running with the active `local` profile.
- **Reference Template**: Refer to `src/main/resources/application-local.properties.example` for required configuration fields.
- **Production Environment Variables**: In containerized or production deployments, supply secrets via environment variables (`SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `LEAKSENSE_JWT_SECRET`, `LEAKSENSE_DEVICE_API_KEY`, `MAIL_USERNAME`, `MAIL_PASSWORD`).


---

## 9. Recommendations for Future Development

1. **LoRaWAN & Cellular Fallback**: Add LoRaWAN gateway drivers for pipeline sections beyond 2.4 GHz Wi-Fi coverage.
2. **Mobile Push Notifications**: Integrate Firebase Cloud Messaging (FCM) or Apple Push Notification Service (APNs) for mobile push alerts.
3. **3D Digital Twin Visualizer**: Enhance `OverviewPage.jsx` with a Three.js 3D pipeline manifold reflecting real-time valve positions and localized gas dispersion clouds.
4. **Machine Learning Model Integration**: Transition the heuristic early-warning engine to an ONNX runtime running an LSTM neural network trained on pipeline pressure transient waveforms.
