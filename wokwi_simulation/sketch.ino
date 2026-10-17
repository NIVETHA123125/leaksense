/*
 * =========================================================================================
 * LeakSense — Autonomous Industrial IoT Pipeline Edge Node
 * Hardware Simulation & Integration Firmware for Wokwi (ESP32)
 * =========================================================================================
 *
 * Hardware Pinout:
 *   - 1. MQ-4 Gas Sensor (Potentiometer):       GPIO 34 (ADC1_CH6)
 *   - 2. MPX5700DP Pressure (Potentiometer):   GPIO 35 (ADC1_CH7)
 *   - 3. ME2-O2 Oxygen (Potentiometer):        GPIO 32 (ADC1_CH4)
 *   - 4. MA40S4R Ultrasonic Acoustic:          TRIG: GPIO 5, ECHO: GPIO 18
 *   - Solenoid Valve Control (Relay Module):   GPIO 25 (Digital Output)
 *   - Valve OPEN Indicator (Green LED):        GPIO 2  (Digital Output)
 *   - Valve CLOSED Indicator (Red LED):        GPIO 4  (Digital Output)
 *   - Siren / Piezo Alarm (Buzzer):            GPIO 26 (Digital Output)
 *   - Status Heartbeat LED (Cyan LED):         GPIO 27 (Digital Output)
 *   - I2C Display (LCD 1602):                  SDA: GPIO 21, SCL: GPIO 22
 * =========================================================================================
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <Wire.h>
#include <LiquidCrystal_I2C.h>

// ----------------- Pin Definitions -----------------
#define PIN_POT_GAS          34   // Simulates MQ-4 Methane transducer
#define PIN_POT_PRESSURE     35   // Simulates MPX5700DP line pressure transducer
#define PIN_POT_O2           32   // Simulates ME2-O2 electrochemical cell
#define PIN_TRIG              5   // Ultrasonic Trig (MA40S4R Acoustic simulation)
#define PIN_ECHO             18   // Ultrasonic Echo (MA40S4R Acoustic simulation)
#define PIN_VALVE_RELAY      25   // Relay driving 12V Solenoid Isolation Valve
#define PIN_VALVE_OPEN_LED    2   // Green LED: Valve OPEN / flowing
#define PIN_VALVE_CLOSED_LED  4   // Red LED: Valve CLOSED / isolated
#define PIN_BUZZER           26   // Piezo Siren
#define PIN_LED_STATUS       27   // Visual transmission heartbeat

// ----------------- Safety Thresholds (Autonomous Edge Trip) -----------------
#define EDGE_METHANE_CRIT_PPM   350.0  // Autonomous sub-second valve isolation threshold
#define EDGE_PRESSURE_LOW_KPA   380.0  // Rapid pressure collapse limit (nominal ~460 kPa)
#define EDGE_OXYGEN_LOW_PCT      19.5  // Asphyxiation / displacement limit (normal ~20.9%)
#define EDGE_ACOUSTIC_DIST_CM    15.0  // Ultrasonic micro-leak acoustic signature threshold

// ----------------- Network & Server Configuration -----------------
// In Wokwi simulator, WiFi SSID is "Wokwi-GUEST" with no password.
const char* WIFI_SSID     = "Wokwi-GUEST";
const char* WIFI_PASSWORD = "";

// Node Identity (Must match a registered node in Spring Boot DB, seeded as "NODE-A1")
const char* NODE_CODE     = "NODE-A1";

// Ingest API Security Key (Defined in application-local.properties)
const char* API_KEY       = "7478d022b168bb56aa669236bd21f1bb173086b3471dbc80";

// Backend Endpoint URL (active localtunnel URL)
String backendBaseUrl = "https://sweet-trees-wonder.loca.lt"; 

// Telemetry interval (milliseconds)
const unsigned long TELEMETRY_INTERVAL = 5000;
unsigned long lastTelemetryTime = 0;

// State Variables
bool valveOpen = true;        // Valve state (HIGH = Open / Energized, LOW = Closed / Isolated)
bool emergencyLatched = false;
int batteryLevel = 94;

LiquidCrystal_I2C lcd(0x27, 16, 2);

// ----------------- Hardware Actuation Helpers -----------------
void setValve(bool open, const char* reason) {
  valveOpen = open;
  digitalWrite(PIN_VALVE_RELAY, open ? HIGH : LOW);
  digitalWrite(PIN_VALVE_OPEN_LED, open ? HIGH : LOW);
  digitalWrite(PIN_VALVE_CLOSED_LED, open ? LOW : HIGH);
  Serial.print("[VALVE] Position changed to: ");
  Serial.print(open ? "OPEN (Green LED ON)" : "CLOSED / ISOLATED (Red LED ON)");
  Serial.print(" | Reason: ");
  Serial.println(reason);
}

void triggerAlarm(bool active) {
  if (active) {
    tone(PIN_BUZZER, 1800); // 1.8 kHz audible warble
  } else {
    noTone(PIN_BUZZER);
  }
}

// ----------------- Sensor Conversion Routines -----------------
// Maps ADC (0 - 4095) to Methane PPM (0 - 1500 PPM)
float readMethanePPM() {
  int raw = analogRead(PIN_POT_GAS);
  float ppm = (raw / 4095.0) * 1200.0;
  return ppm;
}

// Maps ADC (0 - 4095) to Pressure kPa (0 - 600 kPa, nominal 465 kPa)
float readPressureKPa() {
  int raw = analogRead(PIN_POT_PRESSURE);
  float kpa = (raw / 4095.0) * 600.0;
  return kpa;
}

// Maps ADC (0 - 4095) to Oxygen percentage (12.0% - 24.0%, nominal 20.9%)
float readOxygenPercent() {
  int raw = analogRead(PIN_POT_O2);
  float o2 = 12.0 + ((raw / 4095.0) * 12.0);
  return o2;
}

// Measures ultrasonic acoustic echo / distance (simulating acoustic emission from pinhole micro-leaks)
float readUltrasonicAcoustic() {
  digitalWrite(PIN_TRIG, LOW);
  delayMicroseconds(2);
  digitalWrite(PIN_TRIG, HIGH);
  delayMicroseconds(10);
  digitalWrite(PIN_TRIG, LOW);
  long duration = pulseIn(PIN_ECHO, HIGH, 25000); // 25ms timeout
  if (duration == 0) return 200.0; // Normal quiet baseline (~200 cm)
  float distCm = duration * 0.034 / 2.0;
  return distCm;
}

bool lcdFound = false;

// ----------------- Display Update Routine -----------------
void updateLcd(float ch4, float p, float o2, float risk, float acoustic) {
  if (!lcdFound) return;
  lcd.clear();
  lcd.setCursor(0, 0);
  if (valveOpen) {
    lcd.print("G:");
    lcd.print((int)ch4);
    lcd.print("P ");
    lcd.print("P:");
    lcd.print((int)p);
    lcd.print("k");
  } else {
    lcd.print("!VALVE CLOSED!");
  }

  lcd.setCursor(0, 1);
  lcd.print("O2:");
  lcd.print(o2, 1);
  lcd.print("% U:");
  lcd.print((int)acoustic);
  lcd.print("cm");
}

// ----------------- Setup -----------------
void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n==============================================");
  Serial.println("  LeakSense Autonomous Edge Safety Node (ESP32)");
  Serial.println("  4 Sensors: CH4 + Pressure + O2 + Ultrasonic");
  Serial.println("==============================================");

  // Pin modes
  pinMode(PIN_POT_GAS, INPUT);
  pinMode(PIN_POT_PRESSURE, INPUT);
  pinMode(PIN_POT_O2, INPUT);
  pinMode(PIN_TRIG, OUTPUT);
  pinMode(PIN_ECHO, INPUT);
  pinMode(PIN_VALVE_RELAY, OUTPUT);
  pinMode(PIN_VALVE_OPEN_LED, OUTPUT);
  pinMode(PIN_VALVE_CLOSED_LED, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_LED_STATUS, OUTPUT);

  // Initialize valve in safe OPEN state (Green LED ON, Relay energized)
  setValve(true, "System Initialization / Normal Standby");
  triggerAlarm(false);

  // Probe I2C bus safely - will not hang if LCD is not wired
  Wire.begin(21, 22);
  Wire.beginTransmission(0x27);
  byte i2cError = Wire.endTransmission();
  if (i2cError == 0) {
    lcdFound = true;
    lcd.init();
    lcd.backlight();
    lcd.setCursor(0, 0);
    lcd.print("LeakSense Edge");
    lcd.setCursor(0, 1);
    lcd.print("Booting Node A1");
    Serial.println("[SETUP] I2C LCD connected successfully.");
  } else {
    lcdFound = false;
    Serial.println("[SETUP] No LCD detected on I2C (0x27). Skipping LCD.");
  }

  // Connect to Wi-Fi
  Serial.print("[WIFI] Connecting to ");
  Serial.print(WIFI_SSID);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int retries = 0;
  while (WiFi.status() != WL_CONNECTED && retries < 20) {
    delay(500);
    Serial.print(".");
    retries++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[WIFI] Connected successfully!");
    Serial.print("[WIFI] Assigned IP: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\n[WIFI] Failed to connect! Edge node will operate autonomously.");
  }
  delay(1000);
}

// ----------------- Command Acknowledgment -----------------
void sendCommandAck(long commandId, bool success, const char* note) {
  if (WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  String url = backendBaseUrl + "/api/ingest/commands/" + String(commandId) + "/ack";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-API-Key", API_KEY);
  http.addHeader("Bypass-Tunnel-Reminder", "true");

  StaticJsonDocument<200> doc;
  doc["nodeCode"] = NODE_CODE;
  doc["success"] = success;
  doc["note"] = note;

  String requestBody;
  serializeJson(doc, requestBody);

  int httpCode = http.POST(requestBody);
  Serial.print("[COMMAND] Acknowledged ID ");
  Serial.print(commandId);
  Serial.print(" | HTTP Response: ");
  Serial.println(httpCode);

  http.end();
}

// ----------------- Ingest Telemetry Transmission -----------------
void transmitTelemetry(float ch4, float pressure, float o2, float acoustic) {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("[HTTP] Wi-Fi offline. Skipping telemetry transmission.");
    return;
  }

  // Heartbeat LED flash
  digitalWrite(PIN_LED_STATUS, HIGH);

  HTTPClient http;
  String url = backendBaseUrl + "/api/ingest/readings";
  http.begin(url);
  http.addHeader("Content-Type", "application/json");
  http.addHeader("X-API-Key", API_KEY);
  http.addHeader("Bypass-Tunnel-Reminder", "true");

  // Assemble JSON payload matching IngestRequest DTO
  StaticJsonDocument<300> doc;
  doc["nodeCode"] = NODE_CODE;
  doc["methanePpm"] = round(ch4 * 10.0) / 10.0;
  doc["pressureKpa"] = round(pressure * 10.0) / 10.0;
  doc["oxygenPercent"] = round(o2 * 10.0) / 10.0;
  doc["batteryLevel"] = batteryLevel;

  String jsonPayload;
  serializeJson(doc, jsonPayload);

  Serial.println("\n--- [INGEST] Sending Packet ---");
  Serial.print("Payload: ");
  Serial.println(jsonPayload);

  int httpCode = http.POST(jsonPayload);
  digitalWrite(PIN_LED_STATUS, LOW);

  if (httpCode == 200) {
    String response = http.getString();
    Serial.print("[INGEST] Server OK (200): ");
    Serial.println(response);

    StaticJsonDocument<512> respDoc;
    DeserializationError error = deserializeJson(respDoc, response);
    if (!error) {
      float riskScore = respDoc["riskScore"] | 0.0;
      updateLcd(ch4, pressure, o2, riskScore, acoustic);

      // Process any pending commands sent by the server
      JsonArray commands = respDoc["commands"].as<JsonArray>();
      for (JsonObject cmd : commands) {
        long cmdId = cmd["id"];
        const char* cmdType = cmd["type"];
        Serial.print("[COMMAND] Received remote action: ");
        Serial.println(cmdType);

        if (strcmp(cmdType, "CLOSE") == 0 || strcmp(cmdType, "CLOSE_VALVE") == 0) {
          setValve(false, "Remote Server Command");
          sendCommandAck(cmdId, true, "Valve closed per server request");
        } else if (strcmp(cmdType, "OPEN") == 0 || strcmp(cmdType, "OPEN_VALVE") == 0) {
          if (!emergencyLatched) {
            setValve(true, "Remote Server Command");
            sendCommandAck(cmdId, true, "Valve opened safely");
          } else {
            sendCommandAck(cmdId, false, "Refused: Local hazardous gas latch active");
          }
        } else if (strcmp(cmdType, "ALARM_ON") == 0) {
          triggerAlarm(true);
          sendCommandAck(cmdId, true, "Alarm siren activated per server command");
        } else if (strcmp(cmdType, "ALARM_OFF") == 0 || strcmp(cmdType, "SILENCE_ALARM") == 0) {
          triggerAlarm(false);
          sendCommandAck(cmdId, true, "Alarm siren silenced per server command");
        }
      }
    }
  } else if (httpCode > 0) {
    Serial.print("[INGEST] HTTP Error Code: ");
    Serial.println(httpCode);
  } else {
    Serial.print("[INGEST] HTTP Connection Failed: ");
    Serial.println(http.errorToString(httpCode));
  }

  http.end();
}

// ----------------- Main Loop -----------------
void loop() {
  // 1. Read all 4 calibrated sensors
  float ch4      = readMethanePPM();
  float pressure = readPressureKPa();
  float o2       = readOxygenPercent();
  float acoustic = readUltrasonicAcoustic();

  // 2. Autonomous Sub-Second Edge Protection
  // If any sensor (Methane, Pressure, Oxygen, or Ultrasonic acoustic micro-leak) breaches limit:
  bool gasHazard = (ch4 >= EDGE_METHANE_CRIT_PPM);
  bool pressureHazard = (pressure <= EDGE_PRESSURE_LOW_KPA);
  bool oxygenHazard = (o2 <= EDGE_OXYGEN_LOW_PCT);
  bool acousticHazard = (acoustic <= EDGE_ACOUSTIC_DIST_CM);

  if (gasHazard || pressureHazard || oxygenHazard || acousticHazard) {
    if (!emergencyLatched) {
      emergencyLatched = true;
      setValve(false, gasHazard ? "CRITICAL: Methane breach >= 350 ppm" :
                      pressureHazard ? "CRITICAL: Line pressure collapse" :
                      oxygenHazard ? "CRITICAL: Oxygen displacement" :
                      "CRITICAL: Ultrasonic acoustic micro-leak hiss detected");
      triggerAlarm(true);
    }
  } else {
    // Clear local buzzer if readings normalize to baseline
    if (emergencyLatched) {
      emergencyLatched = false;
      triggerAlarm(false);
      Serial.println("[EDGE] Physical readings normalized to baseline.");
    }
  }

  // 3. Periodic Telemetry Transmission
  unsigned long now = millis();
  if (now - lastTelemetryTime >= TELEMETRY_INTERVAL) {
    lastTelemetryTime = now;
    transmitTelemetry(ch4, pressure, o2, acoustic);
  }

  delay(100);
}
