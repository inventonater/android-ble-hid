# Android BLE HID Device

This project allows an Android device to act as a Bluetooth HID peripheral (keyboard, mouse, and media controller) using the BluetoothHidDevice API introduced in Android 9 (API 28).

## Features

- Emulate HID peripherals (keyboard, mouse, media controller)
- Built on the modern BluetoothHidDevice API
- Debug UI for testing and troubleshooting
- Unity integration for game developers

## Requirements

- Android 9.0 (API 28) or higher
- Device with Bluetooth HID Device profile support
- Bluetooth and location permissions

## Project Structure

The project is organized into several modules:

- **app**: Main Android application with UI
- **core**: Core BLE HID implementation using BluetoothHidDevice API
- **unity-plugin**: Java wrapper for Unity integration
- **unity-test**: Sample Unity project demonstrating integration

## Core Components

### HidManager

The central component that handles:
- Connecting to the HID Service
- Registering as a HID Device
- Connecting with host devices
- Sending HID reports

### HID Reporters

- **KeyboardReporter**: For keyboard input (keys, modifiers, typing text)
- **MouseReporter**: For mouse input (movement, buttons, scrolling)
- **MediaReporter**: For media control (play/pause, volume, etc.)

## Usage

### Basic Usage (Android App)

1. Launch the app
2. Grant necessary permissions
3. Click "Start BLE HID Device"
4. Pair with a host device (computer, tablet, etc.)
5. Use the UI to send keyboard, mouse, or media commands

### Unity Integration

To use in Unity projects:

1. Add the BleHidPlugin.aar file to your Unity project's Assets/Plugins/Android folder
2. Use the ModernBleHidManager component to access HID functionality:

```csharp
// Example: Type text via HID keyboard
ModernBleHidManager.Instance.TypeString("Hello from Unity!");

// Example: Move mouse
ModernBleHidManager.Instance.MoveMouseRelative(10, 5);

// Example: Click mouse button
ModernBleHidManager.Instance.ClickMouseButton(ModernBleHidManager.MouseButton.LEFT);

// Example: Send media key
ModernBleHidManager.Instance.SendMediaKey(ModernBleHidManager.MediaKey.PLAY_PAUSE);
```

## Building & Development

This project includes comprehensive developer tools in the `developer-tools/` directory 
to simplify building, testing, and debugging.

### Interactive Developer Tools Launcher

The easiest way to access all developer tools is through the interactive launcher:

```bash
./dev-hid.sh
```

This will present a menu with all available tools organized by category:
- Build tools (all components, app only, Unity plugin)
- Testing & debugging tools (device compatibility, connection diagnostics)
- Deployment tools (build, install, and run)

### Individual Tool Scripts

You can also run individual tool scripts directly:

```bash
./developer-tools/build-all.sh     # Build all components
./developer-tools/check-device.sh  # Check device compatibility
./developer-tools/run-app.sh       # Build, install and run the app
```

For more details, see the [Developer Tools README](developer-tools/README.md).

## Debugging

The ModernHidActivity includes extensive debugging capabilities:
- Connection state monitoring
- HID report logging
- Bluetooth event tracking
- Device compatibility analysis

## Permissions

The app requires the following permissions:
- Bluetooth (BLUETOOTH, BLUETOOTH_ADMIN) for Android < 12
- Bluetooth (BLUETOOTH_CONNECT, BLUETOOTH_ADVERTISE) for Android 12+
- Location (ACCESS_FINE_LOCATION) for Android 6-11 (required for BLE operations)
