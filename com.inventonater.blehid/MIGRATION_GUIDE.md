# Migration Guide: Moving from BleHidTest to com.inventonater.blehid

This guide explains how to migrate from using the directly embedded `BleHidTest` Unity assets to the new `com.inventonater.blehid` Unity package format.

## Package Structure

The new Unity package follows the standard UPM (Unity Package Manager) format:

```
com.inventonater.blehid/
├── package.json             # Package metadata
├── README.md                # Package documentation
├── MIGRATION_GUIDE.md       # This guide
├── Runtime/                 # Runtime scripts
│   ├── Inventonater.BleHid.asmdef  # Assembly definition
│   ├── BleHidManager.cs     # Main manager script
│   ├── BleHidConstants.cs   # Constants for HID codes
│   └── Plugins/             # Native plugins
│       └── Android/         # Android-specific plugins
│           ├── BleHidPlugin.aar  # Unity plugin AAR
│           └── BleHidCore.aar    # Core BLE HID functionality AAR
└── Editor/                  # Editor scripts
    ├── Inventonater.BleHid.Editor.asmdef  # Editor assembly definition
    └── BleHidBuildHelper.cs  # Build helper utilities

```

## Building the Plugin

The Android plugin can be built from the gradle project:

1. From the root of the repository, run:
   ```
   ./gradlew :unity-plugin:copyToUnity
   ```

2. This will:
   - Build the Android AAR files
   - Copy them to both:
     - `unity-test/Assets/Plugins/Android/` (for backward compatibility)
     - `com.inventonater.blehid/Runtime/Plugins/Android/` (for the new package)

3. Alternatively, you can use the Unity Editor helper:
   - Import the package into your Unity project
   - Go to `Inventonater > BLE HID > Build Android Plugin`

## Using the New Package

### Option 1: Local Package

1. Open your Unity project
2. Go to `Window > Package Manager`
3. Click the `+` button and select `Add package from disk...`
4. Navigate to and select the `package.json` file in the `com.inventonater.blehid` folder
5. Click `Open` to install the package

### Option 2: Git URL

If you host this package in a Git repository:

1. Open your Unity project
2. Go to `Window > Package Manager`
3. Click the `+` button and select `Add package from git URL...`
4. Enter the Git URL to your repository, with the path to the package folder, e.g.:
   ```
   https://github.com/yourusername/android-ble-hid.git?path=/com.inventonater.blehid
   ```

## API Changes

The API remains largely the same, with the main change being the namespace:

- Old: `BleHid`
- New: `Inventonater.BleHid`

Example code change:

```csharp
// Old code
using BleHid;

BleHidManager manager = GetComponent<BleHidManager>();

// New code
using Inventonater.BleHid;

BleHidManager manager = GetComponent<BleHidManager>();
```

## Migrating an Existing Project

1. Remove any existing `BleHidTest` assets from your project
2. Import the new `com.inventonater.blehid` package
3. Update any namespace references from `BleHid` to `Inventonater.BleHid`
4. Replace any direct references to `Assets/BleHidTest/...` with references to the package

## Android Java Changes

The Java code has also been reorganized:

- Old namespace: `com.example.blehid`
- New namespace: `com.inventonater.blehid`

This should be transparent to Unity users unless you have custom Java code that interfaces with the plugin.
