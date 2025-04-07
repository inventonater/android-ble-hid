# Android BLE HID

This project implements a Bluetooth Low Energy (BLE) Human Interface Device (HID) library for Android, allowing Android devices to act as Bluetooth keyboards, mice, and media controllers.

## Project Structure

The project is organized into multiple modules:

- **core**: Core BLE HID functionality
- **unity-plugin**: Unity-specific bridge for the core functionality
- **app**: Android demo app that showcases the library's features
- **com.inventonater.blehid**: Unity package for easy integration into Unity projects

## Unity Package

The project now uses a Unity Package Manager (UPM) package for Unity integration. This makes it easy for Unity developers to include the BLE HID functionality in their projects.

### Package Location

The Unity package is located in the `com.inventonater.blehid` directory and can be built into a distributable `.tgz` file using the included `package_build.sh` script:

```bash
cd com.inventonater.blehid
./package_build.sh
```

This will create a `com.inventonater.blehid-1.0.0.tgz` file that can be imported into Unity using the Package Manager.

### Package Contents

- **Runtime/**: Contains the C# scripts and Android plugins
  - `BleHidManager.cs`: Main interface for Unity developers
  - `BleHidConstants.cs`: Constants for key codes, etc.
  - `BleHidDemo.cs`: Example implementation
  - `Plugins/Android/`: Contains the compiled Android libraries

- **Editor/**: Contains Unity Editor scripts
  - `BleHidBuildHelper.cs`: Helps with Android build settings

## Building the Project

### Building the Android Libraries

To build the Android libraries and copy them to the Unity package:

```bash
./gradlew :unity-plugin:copyToUnity
```

This will:
1. Build the core library
2. Build the Unity plugin
3. Copy both AAR files to the Unity package's Plugins/Android directory

### Building the Unity Package

After building the Android libraries, you can build the Unity package:

```bash
cd com.inventonater.blehid
./package_build.sh
```

## Migration from Previous Version

If you were using the previous version of this library that used the `unity-test` project structure, please see the [Migration Guide](com.inventonater.blehid/MIGRATION_GUIDE.md) for instructions on how to upgrade.

## License

[Your license information here]
