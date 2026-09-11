# 💧 Smart Water Heater Controller (Android)

An Android app to remotely control an immersion water heater via a [Tuya Smart](https://iot.tuya.com) Wi-Fi relay switch. Calculates optimal heating duration based on a seasonal mains water temperature model — no external sensors or APIs required.

## Features

- **Remote control** — turn the heater on/off with configurable countdown timers via the Tuya OpenAPI
- **Thermal model** — calculates how long to heat based on target temperature, tank size, and element rating
- **Seasonal inlet temperature** — uses a UKWIR-calibrated sine-wave formula to estimate incoming cold water temperature by day-of-year (no sensor needed)
- **Quick presets** — 30 min, 1h, 1.5h, 2h, and Full Tank boosts dynamically adjusted to the season
- **Custom slider** — dial in exact duration with live energy (kWh) and shower-water estimates
- **Legionella-safe defaults** — 60 °C target temperature by default

## Screenshots

> See `ui_screenshot.jpg` for a preview of the dashboard.

---

## Setup

### Prerequisites

- Docker Desktop (for building — no local Java/Gradle/Android SDK needed)
- A Tuya IoT account with a Cloud Project and a Wi-Fi relay device

### 1. Clone the repo

```bash
git clone https://github.com/your-username/water-heater-app.git
cd water-heater-app
```

### 2. Configure credentials

Copy the example file and fill in your own values:

```bash
cp local.properties.example local.properties
```

Edit `local.properties`:

```properties
TUYA_CLIENT_ID=your_client_id_here
TUYA_CLIENT_SECRET=your_client_secret_here
TUYA_REGION_URL=https://openapi.tuyaeu.com
TUYA_WATER_TANK_DEVICE_ID=your_device_id_here
```

**Where to find these values:**

| Value | Where to find it |
|---|---|
| `TUYA_CLIENT_ID` | [iot.tuya.com](https://iot.tuya.com) → Cloud → Your Project → Overview |
| `TUYA_CLIENT_SECRET` | Same page, hidden by default |
| `TUYA_WATER_TANK_DEVICE_ID` | Tuya Console → Devices tab, or run `python -m tinytuya wizard` |
| `TUYA_REGION_URL` | `https://openapi.tuyaeu.com` (EU), `https://openapi.tuyaus.com` (US), etc. |

> ⚠️ **`local.properties` is in `.gitignore` and will never be committed.** It is only used at build time to inject secrets via Gradle `buildConfigField`.

### 3. Build the APK

```bash
./build_with_docker.sh
```

This builds a debug APK at `app-debug.apk` with no local Java/Gradle/Android SDK required.

### 4. Install on your phone

Send `app-debug.apk` to yourself (WhatsApp, email, or Google Drive) and tap to install. You may need to enable **Install from unknown sources** in Android Settings.

---

## Project Structure

```
water-heater-app/
├── app/src/main/java/com/example/waterheater/
│   ├── data/
│   │   ├── TuyaApiClient.kt        # HMAC-SHA256 Tuya OpenAPI client
│   │   ├── ThermalModel.kt         # Heating physics + UKWIR seasonal model
│   │   └── WaterHeaterRepository.kt
│   ├── ui/
│   │   ├── WaterHeaterViewModel.kt
│   │   └── screens/
│   │       ├── HomeScreen.kt       # Main dashboard
│   │       ├── SchedulesScreen.kt
│   │       ├── SettingsScreen.kt
│   │       └── RelayStatusScreen.kt
│   └── MainActivity.kt
├── local.properties.example        # ← copy this to local.properties
├── build_with_docker.sh            # Docker build script
├── Dockerfile
└── test_app_logic.py               # Physics + HMAC unit tests
```

## Seasonal Water Temperature Model

The app estimates incoming cold water temperature using a UKWIR-calibrated formula — no sensors or API calls needed:

```
T_mains = 11.5 + 7.5 × sin(2π × (day_of_year − 60) / 365)
```

This gives approximately **4 °C** in late February and **19 °C** in late August for a typical UK distribution system. The heating duration for all presets adjusts automatically every day.

## Tuya API Architecture

The app uses the **Tuya OpenAPI** directly (no third-party SDK), authenticating with HMAC-SHA256 signed requests. Credentials are injected at build time from `local.properties` via Gradle `buildConfigField` — they are never stored in source code.

---

## Security

| File | Committed? | Purpose |
|---|---|---|
| `local.properties` | ❌ No (gitignored) | Build-time credential injection |
| `local.properties.example` | ✅ Yes | Template for contributors |
| `.env` | ❌ No (gitignored) | Python script credentials |
| `.env.example` | ✅ Yes | Template for Python scripts |

