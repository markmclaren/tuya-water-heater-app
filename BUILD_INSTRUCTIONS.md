# 📱 Android Water Heater Control App

A standalone native Android application built in Kotlin with Jetpack Compose (Material 3) for controlling Tuya-based water heaters with thermodynamics-based duration estimation.

---

## 🎨 App UI Interface Screenshot

![Android Water Heater App Interface](ui_screenshot.jpg)

---

## ⚡ Quick Boost Presets (Configured for 266L Cylinder & Power Shower)

| Preset | Heating Boost Time | Hot Water Produced (at 40°C) | Shower / Usage Duration |
| :--- | :--- | :--- | :--- |
| **30 Min Boost** | 30 Minutes | ~49 Liters | **4 min Quick Shower** |
| **1 Hour Boost** | 60 Minutes (1h) | ~98 Liters | **8 min Full Shower** |
| **1.5 Hour Boost** | 90 Minutes (1.5h) | ~147 Liters | **Full Deep Bath** |
| **2 Hour Boost** | 120 Minutes (2h) | ~196 Liters | **2 Full Showers** (~16 mins) |
| **Full Tank Heat** | ~4 Hours 50 Mins | ~478 Liters | **40 mins showering / full capacity** |

---

## 🛠️ Build & Installation Instructions

### 🐳 Option 1: Zero-Installation Docker Build (Recommended - No Java, SDK, or Gradle Needed!)

Since Docker is already installed on your Mac, you can build the Android APK inside an isolated container with a single command:

1. Open Terminal in this folder:
   ```bash
   cd /Users/ismjml/Experiments/tuya-experiments/water-heater-app
   ```
2. Run the Docker build script:
   ```bash
   ./build_with_docker.sh
   ```
3. When the build finishes, your compiled APK will be created right inside this folder:
   `water-heater-app/app-debug.apk`

#### 📲 How to Install the APK on Your Phone Without USB Cables/ADB:
1. Start a temporary local web server in your terminal:
   ```bash
   python3 -m http.server 8000
   ```
2. Open your Android phone's web browser and go to your Mac's local IP address (e.g. `http://192.168.1.50:8000/app-debug.apk`).
3. Tap **Download & Install**!

---

### Option 2: Using Android Studio (GUI)
1. Launch **Android Studio**.
2. Select **Open** and select this directory:
   `/Users/ismjml/Experiments/tuya-experiments/water-heater-app`
3. Connect your Android device via USB or launch an emulator, then click **Run ▶️**.

---

## ⚙️ Key Project Files

- **[`app/src/main/java/com/example/waterheater/data/ThermalModel.kt`](app/src/main/java/com/example/waterheater/data/ThermalModel.kt)**: Thermodynamics calculations & 266L presets.
- **[`app/src/main/java/com/example/waterheater/data/TuyaApiClient.kt`](app/src/main/java/com/example/waterheater/data/TuyaApiClient.kt)**: HMAC-SHA256 direct Tuya Cloud API driver.
- **[`app/src/main/java/com/example/waterheater/ui/screens/HomeScreen.kt`](app/src/main/java/com/example/waterheater/ui/screens/HomeScreen.kt)**: Jetpack Compose dashboard UI.
- **[`app/src/main/java/com/example/waterheater/ui/screens/RelayStatusScreen.kt`](app/src/main/java/com/example/waterheater/ui/screens/RelayStatusScreen.kt)**: Power outage restore behavior setting (`power_off` / `power_on` / `last`).
- **[`test_app_logic.py`](test_app_logic.py)**: Python unit test script verifying physics math and HMAC signatures.
