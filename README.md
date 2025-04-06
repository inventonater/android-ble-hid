# Inventonater HID

This project implements an Android application that functions as a Bluetooth Low Energy (BLE) Human Interface Device (HID), allowing your Android device to be used as a wireless mouse or keyboard for other devices like computers, tablets, or smart TVs.

## Prerequisites

- Android device running Android 5.0 (API level 21) or higher
- Device with BLE hardware that supports peripheral/advertiser mode
- Host device (computer, tablet, etc.) with Bluetooth capabilities

## Features

- BLE HID Mouse functionality
- BLE HID Keyboard functionality (basic)
- BLE advertisement and GATT server management
- Simple touch interface for mouse movement and buttons
- Basic keyboard input

## Setup

1. Connect your Android device to your computer via USB
2. Enable USB debugging on your Android device
3. Clone this repository
4. Build and install the application:

```bash
./gradlew installDebug
```

5. Grant the necessary Bluetooth permissions when prompted

## Usage

1. On the Android device, open the BLE HID app
2. Choose either "Mouse" or "Keyboard" mode
3. Tap "Start Advertising" to begin advertising as a BLE HID device
4. On your host device (e.g., a computer), go to Bluetooth settings and look for "Android BLE Mouse" or "Android BLE Keyboard"
5. Pair with the device
6. Use the touchpad area to control the mouse cursor or the keyboard interface to send keystrokes

## Troubleshooting

If you're experiencing issues, try the following:

### Basic Troubleshooting

1. Make sure your Android device supports BLE peripheral mode (run `./ble-tools.sh check`)
2. Ensure Bluetooth is enabled on both devices
3. Try restarting both devices
4. Grant all requested permissions to the app

### Advanced Troubleshooting

We provide a unified tool with several commands for diagnosing and fixing issues:

```bash
./ble-tools.sh [command]
```

Available commands:

* `check` - Check if your device supports BLE peripheral mode
* `debug` - Basic BLE debugging information
* `debug --enhanced` - Comprehensive BLE HID debugging tool
* `run` - Build, install, and run the app in one step
* `test` - Test mouse connectivity on host devices
* `help` - Show available commands and options

### Mac-Specific Issues

If connecting to a Mac:

1. Make sure "Input Monitoring" permission is granted to Bluetooth in System Preferences > Security & Privacy > Privacy
2. Try using LightBlue Explorer (available on Mac App Store) to check if your device is advertising properly
3. If device appears in Bluetooth settings but pairing gets stuck, try these steps:
   - Ensure the Android device reports "Device connected" in the app UI
   - Check System Information > Bluetooth on Mac to see if the device is listed
   - Make sure notifications are enabled for HID report characteristics

### Common Issues

1. **Device pairs but doesn't work**
   - This often indicates notification issues. The HID reports may not be properly configured.

2. **Device appears in Bluetooth but won't connect**
   - Verify peripheral mode support on your Android device
   - Check advertising settings and permissions

3. **Mouse movement is detected but not sent to the host**
   - Check for GATT notifications in logs
   - Verify input monitoring permissions on Mac

## Debugging Log Analysis

When running the enhanced debugging tool, look for these key indicators:

1. Connection establishment:
   ```
   Device connected: XX:XX:XX:XX:XX:XX
   ```

2. HID Service registration:
   ```
   Added HID service to GATT server
   Service added: 00001812-0000-1000-8000-00805f9b34fb
   ```

3. Characteristic read requests:
   ```
   Read request for characteristic: 00002a4a-0000-1000-8000-00805f9b34fb
   ```

4. Notification status:
   ```
   Notification sent successfully: [...]
   ```

## Technical Components

- `BleHidManager`: Central coordinator for all BLE HID functionality
- `BleAdvertiser`: Handles BLE advertising
- `BleGattServerManager`: Manages GATT server and services
- `HidMouseService`: Implements the HID mouse service
- `HidKeyboardService`: Implements the HID keyboard service

## Development Guide

### Project Structure

The project is organized into three modules:
- `app`: Main Android application with UI and user interactions
- `core`: Core BLE HID functionality library that can be reused in other projects
- `unity-plugin`: Unity integration for using BLE HID in Unity games/apps

### Build Configuration

The build configuration has been consolidated for maintainability:
- `versions.gradle`: Central configuration file for SDK versions, dependencies, and other build settings
- Module build files reference this central configuration to ensure consistency

### Development Tools

Development tools have been consolidated into a single script:
- `ble-tools.sh`: Unified script with multiple commands for debugging, setup, and testing
  - `./ble-tools.sh check`: Check device BLE peripheral capability
  - `./ble-tools.sh debug [--enhanced]`: Debug BLE issues with optional enhanced mode
  - `./ble-tools.sh run`: Build, install, and run the app
  - `./ble-tools.sh test`: Test mouse connectivity
  - `./ble-tools.sh help`: Show usage information

This centralized approach simplifies development, ensures consistency, and reduces code duplication.
