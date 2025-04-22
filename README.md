# Android BLE HID

A Bluetooth Low Energy (BLE) Human Interface Device (HID) library for Android that lets Android devices act as Bluetooth keyboards, mice, and media controllers.

## Project Structure

- **core**: Core BLE HID functionality implementation
- **unity-plugin**: Unity bridge that wraps the core module
- **com.inventonater.blehid**: Unity package (UPM)

## Building and Installation

### Building the Android Libraries

```bash
# Build the consolidated Android plugin
./build-aar-plugin.sh
```

This script:
1. Builds the core and unity-plugin modules as a single consolidated AAR
2. Copies the AAR file to the Unity package

### Creating the Unity Package

```bash
# Package the Unity plugin into a .tgz file
cd com.inventonater.blehid
./package_to_tgz.sh
```

This creates a `com.inventonater.blehid-x.x.x.tgz` file that can be imported into Unity using the Package Manager.

## Unity Integration

The package includes:

- BLE HID functionality (keyboard, mouse, media controls)
- Android permissions handling
- Input filtering options
- Connection management

All required Android permissions are automatically included when importing the package.

## Usage

1. Import the package in Unity using Package Manager > Add package from tarball
2. Use `BleHidManager` to access the core functionality
3. See included examples for implementation details

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

Copyright (c) 2025 Inventonater
