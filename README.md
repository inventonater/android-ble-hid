# Android BLE HID

This project implements a Bluetooth Low Energy (BLE) Human Interface Device (HID) library for Android, allowing Android devices to act as Bluetooth keyboards, mice, and media controllers.

## Project Structure

The project is organized into multiple modules:

- **core**: Core BLE HID functionality (`com.inventonater.blehid.core`)
- **unity-plugin**: Unity-specific bridge for the core functionality (`com.inventonater.blehid.unity`)
- **app**: Android demo app that showcases the library's features (`com.inventonater.blehid.app`)
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

To build the Android libraries and copy them to the Unity package, use the dedicated build script:

```bash
./build-unity-package.sh
```

This script:
1. Builds only the necessary modules (core, unity-plugin)
2. Avoids building app module (which can have resource issues)
3. Copies both AAR files to the Unity package's Runtime/Plugins/Android directory
4. Creates the final Unity package (.tgz file)

Alternatively, you can build manually:

```bash
# Build only the required modules
./gradlew :core:assembleRelease :unity-plugin:assembleRelease

# Copy to Unity package
./gradlew :unity-plugin:copyToUnity
```

Note: The AAR files now live exclusively in the package structure, not in the Unity project's Assets folder. This is by design - when a Unity project uses the package, it automatically gets access to the plugin files without needing to copy them into the project's Assets.

#### Legacy Build Support

If you still need to copy the AAR files to a Unity project's Assets folder (legacy approach), you can use:

```bash
./gradlew copyPluginToUnityTest copyCoreToUnityTest
```

However, this is not recommended for new projects, which should use the package system instead.

### Building the Unity Package

After building the Android libraries, you can build the Unity package:

```bash
cd com.inventonater.blehid
./package_build.sh
```

## Migration from Previous Version

If you were using the previous version of this library that used the `unity-test` project structure, please see the [Migration Guide](com.inventonater.blehid/MIGRATION_GUIDE.md) for instructions on how to upgrade.

## Namespace

All code uses the `com.inventonater.blehid` namespace:

- Core: `com.inventonater.blehid.core`
- Unity Plugin: `com.inventonater.blehid.unity`
- App: `com.inventonater.blehid.app`

## Testing the Migration

If you've just completed the namespace migration, you can verify everything works correctly:

```bash
# Test the entire build pipeline with the new namespace
./test-migration.sh
```

## License

[Your license information here]
