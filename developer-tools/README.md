# HID Developer Tools

This directory contains utilities and scripts for development, testing, and debugging of the Android BLE HID project.

## Available Tools

### Build Tools
- `build-all.sh` - Builds all project components (app, core, unity plugin) and performs validation
- `build-app.sh` - Builds and installs just the Android app
- `build-unity.sh` - Builds and copies the Unity plugin

### Testing Tools
- `check-device.sh` - Checks if a device supports BLE peripheral mode
- `debug-connection.sh` - Debug BLE advertising and connection issues
- `test-mouse.sh` - Test mouse connectivity
- `test-debug-features.sh` - Test the app's debugging features and UI

### Deployment Tools
- `run-app.sh` - Build, install, and run the app on a connected device

## Usage

Most scripts can be run directly from the developer-tools directory:

```bash
cd developer-tools
./build-all.sh
```

Or from the project root with:

```bash
./developer-tools/build-all.sh
```

See individual script help messages for more details on their usage.
