# Inventonater BLE HID for Unity

A Unity plugin that provides Bluetooth Low Energy Human Interface Device (HID) functionality for Android devices. This plugin allows your Unity applications to emulate keyboard, mouse, and media control inputs over Bluetooth.

## Features

- Emulate keyboard inputs (keys, combinations, text typing)
- Emulate mouse movements and button clicks
- Control media playback (play/pause, next/previous track, volume)
- Easy-to-use Unity C# API
- Android 8.0+ support
- Built-in permission handling for Android 12+

## Installation

### Option 1: Install via Unity Package Manager (Local Package)

1. Open your Unity project
2. Go to `Window > Package Manager`
3. Click the `+` button and select `Add package from disk...`
4. Navigate to and select the `package.json` file in the `com.inventonater.blehid` folder
5. Click `Open` to install the package

### Option 2: Import from .unitypackage File

1. Download the latest `.unitypackage` release
2. Open your Unity project
3. Go to `Assets > Import Package > Custom Package...`
4. Select the downloaded `.unitypackage` file
5. Ensure all items are selected and click `Import`

## Getting Started

### 1. Android Bluetooth Permissions

This package now includes a pre-configured `AndroidManifest.xml` with all necessary Bluetooth permissions for both older Android versions (using BLUETOOTH and LOCATION permissions) and newer Android 12+ devices (using the BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, and BLUETOOTH_CONNECT permissions).

No manual setup of permissions is required - they will be automatically merged into your project's final AndroidManifest.xml during the build process.

### 2. Add BleHidManager to Your Scene

```csharp
// Add a BleHidManager component to a GameObject in your scene
GameObject bleHidObject = new GameObject("BleHidManager");
BleHidManager bleHid = bleHidObject.AddComponent<BleHidManager>();
```

Or, drag the `BleHidManager` prefab from the package into your scene.

### 3. Initialize the Plugin

```csharp
// Initialize the BLE HID functionality
StartCoroutine(bleHid.Initialize());

// Register for the initialization completion event
bleHid.OnInitializeComplete += (success, message) => {
    if (success) {
        Debug.Log("BLE HID initialized successfully!");
        
        // Start advertising to make the device discoverable
        bleHid.StartAdvertising();
    } else {
        Debug.LogError("BLE HID initialization failed: " + message);
    }
};
```

### 4. Use the API to Send HID Commands

```csharp
// Type text
bleHid.TypeText("Hello, Bluetooth world!");

// Send a keyboard shortcut (Ctrl+C)
bleHid.SendKeyWithModifiers(
    BleHidConstants.KEY_C, 
    BleHidConstants.KEY_MOD_LCTRL
);

// Move the mouse and click
bleHid.MoveMouse(10, 5);  // Move right 10px, down 5px
bleHid.ClickMouseButton(BleHidConstants.BUTTON_LEFT);

// Control media playback
bleHid.PlayPause();
bleHid.VolumeUp();
```

## API Reference

### BleHidManager

The main class that handles BLE HID functionality.

#### Properties

- `IsInitialized` - Whether the plugin is initialized
- `IsAdvertising` - Whether BLE advertising is active
- `IsConnected` - Whether a device is connected
- `ConnectedDeviceName` - Name of the connected device
- `ConnectedDeviceAddress` - Bluetooth address of the connected device

#### Methods

- `Initialize()` - Initialize the BLE HID functionality (returns a coroutine)
- `StartAdvertising()` - Start BLE advertising to make the device discoverable
- `StopAdvertising()` - Stop BLE advertising
- `GetAdvertisingState()` - Get the current advertising state
- `SendKey(byte keyCode)` - Send a keyboard key press
- `SendKeyWithModifiers(byte keyCode, byte modifiers)` - Send a key with modifier keys
- `TypeText(string text)` - Type a string of text
- `MoveMouse(int deltaX, int deltaY)` - Move the mouse
- `ClickMouseButton(int button)` - Click a mouse button
- `PlayPause()` - Send media play/pause command
- `NextTrack()` - Send media next track command
- `PreviousTrack()` - Send media previous track command
- `VolumeUp()` - Send media volume up command
- `VolumeDown()` - Send media volume down command
- `Mute()` - Send media mute command
- `GetDiagnosticInfo()` - Get diagnostic information
- `Close()` - Close the plugin and release resources

#### Events

- `OnInitializeComplete` - Called when initialization is complete
- `OnAdvertisingStateChanged` - Called when advertising state changes
- `OnConnectionStateChanged` - Called when connection state changes
- `OnPairingStateChanged` - Called when pairing state changes
- `OnError` - Called when an error occurs
- `OnDebugLog` - Called for debug log messages

### BleHidConstants

Contains constants for HID key codes, modifiers, and error codes.

## Requirements

- Unity 2021.3 or higher
- Android 8.0 (API level 26) or higher
- Bluetooth support on the device

## Android Manifest Configuration

The plugin includes a pre-configured `AndroidManifest.xml` file with all necessary Bluetooth permissions. You don't need to manually add these permissions to your project's manifest. If your project already has an AndroidManifest.xml, the permissions will be automatically merged during build.

The included permissions are:
- BLUETOOTH, BLUETOOTH_ADMIN (for Android 11 and below)
- ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION (for Android 11 and below)
- BLUETOOTH_SCAN, BLUETOOTH_ADVERTISE, BLUETOOTH_CONNECT (for Android 12+)

### How Unity's Manifest Merging Works

When building an Android application, Unity performs a manifest merge process that combines:

1. Unity's base AndroidManifest.xml template
2. Any manifests found in Assets/Plugins/Android/ directory
3. Manifests from package plugins (like ours in com.inventonater.blehid/Runtime/Plugins/Android/)
4. Gradle template manifests (if using custom Gradle templates)

> **Important**: If you're having issues with the permissions not being included in your final build, try these troubleshooting steps:
> 
> 1. Make sure the package is properly imported (try removing and re-adding it in the Packages/manifest.json)
> 2. Restart Unity to ensure all plugin files are properly detected
> 3. Check the Unity console for any import errors related to the package
> 4. If needed, manually copy the AndroidManifest.xml from com.inventonater.blehid/Runtime/Plugins/Android/ to your project's Assets/Plugins/Android/ directory

#### Merging Order and Priority

The merging process follows these rules:

- If an element with the same identifier appears in multiple manifests, the higher priority manifest's version is used
- Unique elements from all manifests are preserved
- For permissions, each unique permission is included only once, regardless of how many manifests request it
- Conflicting attributes on the same elements typically use the higher priority version

#### Compatibility with Other Packages

This approach is compatible with other packages that have their own AndroidManifest.xml files:

- If multiple packages request the same permission, it will appear only once in the final manifest
- If packages have unique permissions, all will be included
- If packages have conflicting configurations for the same component, Unity's build system usually follows a documented priority order

For most permission-related requirements, you won't encounter conflicts since Android permissions are simply merged rather than overwritten.

##### Example Scenario

If you have:
- Our package requesting Bluetooth permissions
- Another AR package requesting camera permissions
- Your main app requesting internet permissions

The final manifest will include all three sets of permissions without conflicts.

### Manually Adding Permissions (If Needed)

In rare cases where the automatic manifest merging doesn't work with your specific Unity version or build configuration, you can manually add the required permissions to your project's AndroidManifest.xml:

```xml
<!-- Bluetooth permissions for Android 12+ -->
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

<!-- Bluetooth permissions for Android 11 and below -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" android:maxSdkVersion="30" />

<!-- Bluetooth feature requirements -->
<uses-feature android:name="android.hardware.bluetooth" android:required="true" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="true" />
```

## Building from Source

To build the plugin from source:

1. Clone the repository
2. Open a terminal in the project root directory
3. Run `./gradlew :unity-plugin:copyToUnity`
4. The `.aar` files will be built and copied to the Unity package

## License

© 2025 Inventonater. All rights reserved.
