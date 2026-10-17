# LeakSense — Wokwi ESP32 Hardware Simulation & Integration Guide

This directory contains the simulation files and edge firmware to simulate the **LeakSense IoT Edge Node** in the [Wokwi ESP32 Simulator](https://wokwi.com/) and stream live telemetry into the Spring Boot safety backend and React SCADA console.

---

## 🎛 Simulated Components & Pin Wiring

| Component | Simulated By | ESP32 Pin | Parameter / Range | Nominal Baseline |
| :--- | :--- | :--- | :--- | :--- |
| **MQ-4 Gas Sensor** | Potentiometer (`pot_gas`) | `GPIO 34` (ADC1_CH6) | Methane $\text{CH}_4$ ($0 - 1,200\text{ ppm}$) | $100 - 150\text{ ppm}$ |
| **MPX5700DP Pressure** | Potentiometer (`pot_pressure`) | `GPIO 35` (ADC1_CH7) | Line Pressure ($0 - 600\text{ kPa}$) | $450 - 470\text{ kPa}$ |
| **ME2-O2 Oxygen Cell** | Potentiometer (`pot_o2`) | `GPIO 32` (ADC1_CH4) | Concentration ($12.0\% - 24.0\%$) | $20.9\%$ |
| **Solenoid Valve** | Relay Module (`relay`) | `GPIO 25` | Digital Out (HIGH = Open, LOW = Closed) | OPEN |
| **Piezo Siren Alarm** | Buzzer (`buzzer`) | `GPIO 26` | 1.8 kHz audible warble on hazard breach | SILENT |
| **Telemetry Heartbeat** | Green LED (`led_status`) | `GPIO 27` | Flashes during HTTP packet transmission | Blinking |
| **Diagnostic Display** | LCD 1602 I2C (`lcd`) | `GPIO 21` (SDA), `GPIO 22` (SCL) | Real-time sensor readout & valve position | Active |

---

## 🚀 Step 1: Open Simulation in Wokwi

You can run the simulation using either the **Wokwi Web Browser** or the **VS Code Wokwi Extension**:

### Option A: Wokwi Web (Recommended & Easiest)
1. Open [https://wokwi.com](https://wokwi.com) in your web browser.
2. Select **ESP32** $\rightarrow$ **ESP32 (Arduino)**.
3. In the project editor tabs:
   - Replace the default code in **`sketch.ino`** with the content of [`sketch.ino`](file:///d:/Leaksense/wokwi_simulation/sketch.ino).
   - Click the **`diagram.json`** tab and replace its content with [`diagram.json`](file:///d:/Leaksense/wokwi_simulation/diagram.json).
   - Click the **Library Manager** tab (or create `libraries.txt`) and add:
     ```
     ArduinoJson
     LiquidCrystal I2C
     ```
4. Click the green **Play** button to compile and start the simulation!

---

## 🌐 Step 2: Bridge Wokwi to Local Backend (`localhost:8080`)

> [!IMPORTANT]
> Because Wokwi runs inside a sandboxed virtual network in your browser, the ESP32 in Wokwi **cannot** access `http://localhost:8080` directly. You must expose your local Spring Boot backend using a temporary secure tunnel so Wokwi can POST telemetry to it.

### Method 1: LocalTunnel (Free, No signup required)
Open a PowerShell terminal on your PC and run:
```powershell
npx localtunnel --port 8080
```
This will output a public tunnel URL, for example:
```
your url is: https://bright-zebra-12.loca.lt
```

### Method 2: Ngrok
If you have `ngrok` installed:
```powershell
ngrok http 8080
```
Copy the generated `https://xxxx.ngrok-free.app` URL.

### Update `sketch.ino` with your Tunnel URL
In `sketch.ino` (line 49):
```cpp
// Replace with your active tunnel URL:
String backendBaseUrl = "https://bright-zebra-12.loca.lt";
```

---

## 🔗 Step 3: Run Full-Stack Integration

1. **Start MySQL & Spring Boot Backend**:
   ```powershell
   cd d:\Leaksense\leaksense_backend
   .\mvnw.cmd spring-boot:run
   ```
   *(Backend starts on `http://localhost:8080`, database is auto-seeded with node `NODE-A1`)*

2. **Start SCADA React Frontend**:
   ```powershell
   cd d:\Leaksense\leaksense-frontend
   npm run dev
   ```
   *(Visit `http://localhost:5173` and log in with username: `operator` / password: `operator123`)*

3. **Start the Wokwi Simulation**:
   - Click the green **Start** button in Wokwi.
   - In the Wokwi Serial Monitor, observe:
     ```
     [WIFI] Connected to Wokwi-GUEST
     [WIFI] Assigned IP: 10.0.2.15
     --- [INGEST] Sending Packet ---
     Payload: {"nodeCode":"NODE-A1","methanePpm":140.2,"pressureKpa":462.5,"oxygenPercent":20.9,"batteryLevel":94}
     [INGEST] Server OK (200): {"status":"ok","riskScore":8.2,"commands":[]}
     ```
   - In the React SCADA Dashboard (`http://localhost:5173`):
     - The **Overview** and **Analytics** pages will start updating live with the sensor values from Wokwi!

---

## 🧪 Interactive Integration Scenarios to Test

### Scenario 1: Autonomous Edge Gas Leak Trip
1. Turn the **MQ-4 Potentiometer** in Wokwi to the right so Methane reaches $\ge 350\text{ ppm}$.
2. **Immediate Result**:
   - The ESP32 autonomously trips the Relay (valve closes) in $< 1.2\text{s}$ and sounds the buzzer.
   - The LCD displays `!VALVE CLOSED!`.
   - On the next telemetry packet, Spring Boot records the breach, initiates a `CRITICAL` alert, and pushes an audible alarm event to the React frontend.
   - The React Dashboard displays a flashing red **Alarm Banner** with sound!

### Scenario 2: Remote Operator Valve Control
1. In the React Dashboard, navigate to the **Valves** page (`/valves`).
2. Click **Close Valve** for Zone A.
3. Spring Boot queues a `CLOSE` command for `NODE-A1`.
4. On the next telemetry polling cycle, Wokwi receives:
   ```
   [COMMAND] Received remote action: CLOSE
   [VALVE] Position changed to: CLOSED (ISOLATED)
   [COMMAND] Acknowledged ID 1 | HTTP Response: 200
   ```
5. The relay in Wokwi trips and sends an acknowledgment back to the backend.
