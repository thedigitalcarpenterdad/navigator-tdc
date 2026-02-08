# TDC Navigator Deployment Guide
## 2025 Lincoln Navigator Android Automotive OS

### Quick Deployment
For immediate testing on a 2025 Lincoln Navigator:

```bash
# Clone and build
git clone [repository]
cd navigator-tdc
./gradlew assembleDebug

# Install on Navigator SYNC 4A system
adb connect [vehicle-ip]:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Prerequisites

#### Development Environment
- **Android Studio**: Arctic Fox or later
- **Java**: 17 or higher
- **Kotlin**: 1.8+
- **Android SDK**: API 28+ (Android 9)
- **Target SDK**: API 34 (Android 14)

#### Vehicle Requirements
- **2025 Lincoln Navigator** with SYNC 4A
- **Android Automotive OS** enabled
- **Developer mode** activated on SYNC 4A
- **ADB debugging** enabled
- **USB or Wi-Fi connection** to vehicle

#### Hardware Requirements
- Lincoln Navigator with Revel premium audio system
- Multi-zone climate control
- Power seats with massage functionality
- Power windows and panoramic sunroof
- Power folding mirrors
- Adaptive cruise control and Co-Pilot360 Plus

### Build Instructions

#### 1. Environment Setup
```bash
# Install Android SDK
export ANDROID_HOME=/path/to/android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Verify automotive SDK components
sdkmanager "platforms;android-34" "build-tools;34.0.0"
sdkmanager "add-ons;addon-google_apis-google-34"
```

#### 2. Project Configuration
```bash
git clone [repository-url]
cd navigator-tdc

# Copy and configure local properties
cp gradle.properties.template gradle.properties
# Edit gradle.properties with your vehicle-specific settings

# Generate wake word model (if needed)
# Place tdc_wake_word_model.tflite in app/src/main/assets/
```

#### 3. Build Variants

**Debug Build** (for development):
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

**Release Build** (for production):
```bash
# Configure signing in app/build.gradle first
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

### Vehicle Deployment

#### 1. Enable Developer Mode on SYNC 4A
1. Navigate to **Settings > General > About SYNC**
2. Tap **Build Number** 7 times rapidly
3. Return to Settings - **Developer Options** should appear
4. Enable **USB Debugging** and **Stay Awake**
5. Enable **Allow mock locations** for testing

#### 2. Connect to Vehicle
**Via USB:**
```bash
# Connect USB cable to SYNC 4A USB-C port
adb devices
# Should show: [device-id] device
```

**Via Wi-Fi:**
```bash
# Enable Wi-Fi debugging in Developer Options
# Get vehicle IP from SYNC 4A network settings
adb connect [vehicle-ip]:5555
adb devices
```

#### 3. Install TDC Navigator
```bash
# Install application
adb install app/build/outputs/apk/debug/app-debug.apk

# Verify installation
adb shell pm list packages | grep com.tdc.navigator

# Launch application
adb shell am start -n com.tdc.navigator/.MainActivity
```

### Configuration

#### 1. Vehicle Permissions
The app requires these automotive permissions:
- `android.car.permission.CAR_CLIMATE`
- `android.car.permission.CAR_DOORS` 
- `android.car.permission.CAR_WINDOWS`
- `android.car.permission.CAR_SEATS`
- `android.car.permission.CAR_LIGHTS`
- `android.permission.RECORD_AUDIO`

Grant permissions via SYNC 4A settings or ADB:
```bash
adb shell pm grant com.tdc.navigator android.permission.RECORD_AUDIO
```

#### 2. Wake Word Model Setup
1. Ensure `tdc_wake_word_model.tflite` is in `/app/src/main/assets/`
2. Model should be trained for "Claw" wake word activation
3. Verify model compatibility with TensorFlow Lite 2.14.0

#### 3. Lincoln/Ford API Integration
Configure FordPass Connect integration:
1. Obtain API keys from Ford Developer Program
2. Add keys to `gradle.properties`:
   ```
   fordpass.api.key=your_api_key
   fordpass.api.secret=your_api_secret
   ```

### Testing

#### 1. Simulation Mode
For testing without vehicle hardware:
```bash
# Enable simulation in gradle.properties
vehicle.simulation.enabled=true

# Run on Android Automotive emulator
./gradlew installDebug
```

#### 2. Vehicle Integration Testing
```bash
# Run automated tests
./gradlew connectedAndroidTest

# Test wake word detection
adb shell am broadcast -a com.tdc.navigator.TEST_WAKE_WORD --es wake_word "hey tdc"

# Test vehicle commands
adb shell am broadcast -a com.tdc.navigator.TEST_COMMAND --es command "set temperature to 72 degrees"
```

#### 3. Voice Command Testing
Essential commands to test:

**Climate Control:**
- "Claw, set temperature to 72 degrees"
- "Claw, turn on air conditioning"
- "Claw, increase fan speed"

**Windows:**
- "Claw, open sunroof"
- "Claw, roll down driver window"
- "Claw, close all windows"

**Seats:**
- "Claw, turn on seat heating"
- "Claw, start driver massage"
- "Claw, adjust seat position"

**Lighting:**
- "Claw, turn on ambient lighting"
- "Claw, set ambient to blue"
- "Claw, turn on dome lights"

### Production Deployment

#### 1. Code Signing
Configure release signing in `app/build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("path/to/keystore.jks")
            storePassword "keystore_password"
            keyAlias "release_key_alias"
            keyPassword "key_password"
        }
    }
}
```

#### 2. Optimization
```bash
# Build optimized release
./gradlew assembleRelease
./gradlew bundleRelease  # For Android App Bundle

# Verify APK
aapt dump badging app/build/outputs/apk/release/app-release.apk
```

#### 3. Distribution
**Direct Installation:**
```bash
# Install on multiple vehicles
for vehicle_ip in vehicle1_ip vehicle2_ip; do
    adb connect $vehicle_ip:5555
    adb install app/build/outputs/apk/release/app-release.apk
done
```

**OTA Updates** (future):
- Integration with Lincoln Connect services
- Remote deployment capabilities
- Automatic update mechanisms

### Security Considerations

#### 1. Vehicle Security
- All vehicle control commands include safety checks
- Emergency stop functionality always available
- Limited operation during vehicle motion
- Encryption for sensitive vehicle data

#### 2. Voice Security
- Wake word detection runs locally (no cloud dependency)
- Voice commands processed on-device
- No persistent audio storage
- User consent required for microphone access

#### 3. Network Security
- HTTPS for all external API calls
- Certificate pinning for Ford/Lincoln APIs
- No sensitive data logging in production builds

### Troubleshooting

#### Common Issues

**Wake Word Not Detected:**
```bash
# Check microphone permissions
adb shell dumpsys package com.tdc.navigator | grep permission

# Verify model loading
adb logcat -s TDC_Navigator_WakeWordService
```

**Vehicle Control Not Working:**
```bash
# Check automotive permissions
adb shell dumpsys car

# Verify vehicle API connection
adb logcat -s TDC_Navigator_NavigatorVehicleManager
```

**App Crashes:**
```bash
# Get crash logs
adb logcat -s AndroidRuntime TDC_Navigator*

# Check system resources
adb shell dumpsys meminfo com.tdc.navigator
```

#### Debug Commands
```bash
# Enable verbose logging
adb shell setprop log.tag.TDC_Navigator VERBOSE

# Test vehicle simulation
adb shell am start -n com.tdc.navigator/.MainActivity --ez enable_simulation true

# Force wake word detection
adb shell am broadcast -a com.tdc.navigator.DEBUG_WAKE_WORD
```

### Support

#### Resources
- **Ford Developer Program**: [developer.ford.com](https://developer.ford.com)
- **Android Automotive Documentation**: [developer.android.com/cars](https://developer.android.com/cars)
- **Lincoln Connect Services**: [lincolnway.com](https://lincolnway.com)

#### Contact
- **Technical Issues**: [support@tdc-navigator.com](mailto:support@tdc-navigator.com)
- **Vehicle Integration**: [integration@tdc-navigator.com](mailto:integration@tdc-navigator.com)
- **Security Concerns**: [security@tdc-navigator.com](mailto:security@tdc-navigator.com)