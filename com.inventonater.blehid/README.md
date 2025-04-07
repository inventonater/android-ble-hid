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

### 1. Add BleHidManager to Your Scene

```csharp
// Add a BleHidManager component to a GameObject in your scene
GameObject bleHidObject = new GameObject("BleHidManager");
BleHidManager bleHid = bleHidObject.AddComponent<BleHidManager>();
```

Or, drag the `BleHidManager` prefab from the package into your scene.

### 2. Initialize the Plugin

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

### 3. Use the API to Send HID Commands

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

## Building from Source

To build the plugin from source:

1. Clone the repository
2. Open a terminal in the project root directory
3. Run `./gradlew :unity-plugin:copyToUnity`
4. The `.aar` files will be built and copied to the Unity package

## License

© 2025 Inventonater. All rights reserved.
