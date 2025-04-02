# Inventonater BLE HID Library

A modern Android Bluetooth Low Energy HID (Human Interface Device) library that enables Android devices to function as HID peripherals such as keyboards, mice, and other input devices.

## Features

- **Peripheral Mode Support**: Act as a BLE HID device from your Android application
- **Multiple Controller Types**: 
  - 🖱️ Mouse - Movement, buttons, and scrolling
  - ⌨️ Keyboard - Key presses with modifiers
- **Compatibility Layer**: Built-in strategies for different host platforms
- **Unity Integration**: Ready-to-use plugin for Unity games and applications
- **Modern Architecture**:
  - Written in Kotlin with modern language features
  - Clean API design with proper abstraction
  - Coroutines for asynchronous operations
  - Comprehensive error handling

## Project Structure

The project is organized into three main modules:

- **core**: Core BLE HID functionality library that can be used in any Android project
- **app**: Sample Android application demonstrating the library's capabilities
- **unity-plugin**: Unity integration for using the BLE HID features in Unity projects

## Requirements

- Android 8.0 (API level 26) or higher
- Device with BLE peripheral mode support
- Bluetooth permissions

## Getting Started

### Building the Project

1. Clone the repository
2. Open in Android Studio 
3. Sync Gradle files
4. Build the project

### Using the Core Library

```kotlin
// Initialize
BleHid.initialize(context)

// Activate services you need
BleHid.activateService("mouse")
BleHid.activateService("keyboard")

// Start advertising
BleHid.startAdvertising()

// Use mouse functions
BleHid.moveMouse(10, 5)
BleHid.clickMouseButton(MouseButton.LEFT)
BleHid.scrollMouseWheel(10)

// Use keyboard functions
BleHid.sendKey(HidKeyCode.A)
BleHid.sendKeys(intArrayOf(HidKeyCode.SHIFT, HidKeyCode.H))
BleHid.releaseKeys()

// Cleanup when done
BleHid.shutdown()
```

### Using the Unity Plugin

1. Build the Unity plugin AAR:
   ```
   ./gradlew :unity-plugin:assembleRelease :unity-plugin:copyToUnity
   ```

2. The AAR will be automatically copied to `unity-test/Assets/Plugins/Android/BleHidPlugin.aar`

3. In your Unity C# script:
   ```csharp
   // Initialize
   UnityBleHid.initialize(context);
   
   // Set connection listener
   UnityBleHid.setConnectionListener(new MyConnectionListener());
   
   // Start advertising
   UnityBleHid.startAdvertising();
   
   // Use HID functions
   UnityBleHid.sendKey(keyCode);
   UnityBleHid.moveMouse(x, y);
   
   // Clean up
   UnityBleHid.shutdown();
   ```

## Architecture

The library follows a clean architecture approach with distinct layers:

- **API Layer**: Public interfaces and entry points (`com.inventonater.hid.core.api`)
- **Implementation Layer**: Internal implementations of the API interfaces (`com.inventonater.hid.core.internal`)
- **Service Layer**: Specific HID service implementations (mouse, keyboard)

### Key Components

- **BleHid**: Main entry point facade providing simplified access to all functionality
- **BleHidManager**: Manages BLE connections and service activation
- **HidServiceBase**: Base class for all HID services (mouse, keyboard)
- **ConnectionManager**: Handles BLE connection state and notifications
- **DeviceCompatibility**: Manages compatibility with different host platforms

## Recent Improvements

- **Complete Kotlin Migration**: Fully rewritten in Kotlin with modern language features
- **Improved Architecture**: Clean separation between API and implementation
- **Enhanced Type Safety**: Proper enums and sealed classes for type-safe operations
- **Coroutine Support**: Asynchronous operations using Kotlin coroutines
- **Unity Plugin**: Updated Unity integration with Java-Kotlin interoperability
- **Resource Management**: Better lifecycle management and resource cleanup
- **Improved Diagnostics**: Structured logging and error reporting

## Documentation

Detailed documentation is available in the `docs` directory:

- [Architecture Overview](docs/architecture/overview.md)
- [Core Components](docs/architecture/core-components.md)
- [Service Design](docs/architecture/service-design.md)
- [Device Compatibility](docs/implementation/device-compatibility.md)
- [BLE Connectivity](docs/implementation/ble-connectivity.md)
- [Diagnostics](docs/implementation/diagnostics.md)
- [Kotlin Migration](docs/implementation/kotlin-migration.md)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a pull request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request
