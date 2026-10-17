# LeakSense — SCADA Operator Console (Frontend)

[![React](https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind_CSS-v4-38B2AC?style=for-the-badge&logo=tailwind-css&logoColor=white)](https://tailwindcss.com/)
[![Recharts](https://img.shields.io/badge/Recharts-3.x-22c55e?style=for-the-badge)](https://recharts.org/)

> **Real-time Industrial SCADA Operator Interface for the LeakSense Pipeline Safety Platform.**  
> Built with React 19, Vite, Recharts, and STOMP WebSockets over SockJS.

---

## 🖥 Overview & Views

1. **Public Showcase Landing Page (`/`)**: Public-facing platform introduction, live heartbeat telemetry status ribbon, hardware specs, and demonstration request submission.
2. **Operator Login Modal**: Secure authentication with role-based redirection (`OPERATOR` vs `ADMIN`).
3. **Overview SCADA Dashboard (`/dashboard`)**:
   - Pulsing emergency audible siren banner with quick-silence action.
   - Core KPI summary tiles (active leaks, safe zones, valve status, health score).
   - Real-time multi-sensor gas telemetry chart.
   - Active alert investigation panel with direct Acknowledge and Resolve actions.
   - Sensor node connectivity and battery level monitoring cards.
   - Live valve actuation audit trail.
4. **Analytics Suite (`/analytics`)**:
   - **Live Stream Tab**: Incoming packet buffer table, live streaming graph, and real-time risk gauges.
   - **Historical Analytics Tab**: Interactive multi-sensor trend lines (Methane, Pressure, Oxygen, Risk Index), risk band distribution histograms, stacked daily alert timelines, sensor failure share donut charts, and valve activity metrics.
5. **Field Valve Control Matrix (`/valves`)**:
   - Visual valve status per pipeline zone.
   - Individual zone open and close actions with double-confirmation dialogs.
   - Red "EMERGENCY SHUT DOWN ALL VALVES" facility master trip switch.
6. **Safety Alerts Center (`/alerts`)**: Multi-filter alert log supporting filters by zone, severity, resolution status, and date range.
7. **Sensor Inventory (`/sensors`)**: Hardware inventory displaying battery levels, last-seen timestamps, active transducers, and interactive ping test simulator.
8. **Compliance Reporting (`/reports`)**: AI narrative safety assessment card, on-demand PDF audit export, and raw Excel workbook download.
9. **Platform Administration (`/users` & `/settings`)**: Dynamic safety threshold tuning with audit logging and user account management (`ADMIN` only).
10. **Interactive AI Chatbot Copilot**: Floating drawer widget powered by `/api/ai/chat` for live questions regarding pipeline status and emergency procedures.

---

## 🛠 Tech Stack & Key Libraries

* **Core Framework**: React 19, React Router DOM 7
* **Build System**: Vite 8 with Hot Module Replacement (HMR)
* **Real-Time Streaming**: `@stomp/stompjs` for WebSocket subscriptions (`/topic/readings`, `/topic/alerts`, `/topic/alarms`, `/topic/notifications`)
* **Data Visualization**: `recharts` for interactive multi-line charts, bar charts, and donut graphs
* **HTTP Client**: `axios` with automatic Bearer token interceptor and 401 unauthenticated session clearance
* **Styling & Design System**: Tailwind CSS v4, custom CSS variables in `theme.css`, and glassmorphic SCADA tokens in `index.css`

---

## 🚀 Getting Started

### Prerequisites
* **Node.js**: v18.0.0 or higher
* **npm**: v9.0.0 or higher
* **LeakSense Backend**: Running on `http://localhost:8080`

### Installation & Run

```bash
# 1. Install dependencies
npm install

# 2. Start Vite development server
npm run dev
```

The frontend application will be served at `http://localhost:5173`.

### Available Scripts

* `npm run dev`: Runs the app in development mode with HMR.
* `npm run build`: Compiles and bundles production-ready static assets into `dist/`.
* `npm run lint`: Runs ESLint 9 checks across all source files.
* `npm run preview`: Locally previews the production build.

---

## 🔒 Session & Security Notes

* **`sessionStorage` Isolation**: Authentication tokens (`leaksense_token`) and user credentials are saved exclusively in `sessionStorage`. Closing the browser tab terminates the authenticated session, preventing unauthorized access on shared SCADA control room terminals.
* **Axios Interceptors**: Automatically appends the Bearer JWT to all protected API calls and flushes credentials upon `401 Unauthorized`.
