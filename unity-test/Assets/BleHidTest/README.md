# Android BLE HID Unity Plugin

This plugin allows you to use your Android device as a Bluetooth LE HID device from Unity, providing both media control and mouse functionality.

## Quick Setup with Bootstrapper

The easiest way to get started is to use the `BleHidBootstrapper` script which automatically creates all the necessary UI components:

1. Import the required files:
   - `Assets/Plugins/Android/BleHidPlugin.aar` - The Android plugin
   - `Assets/BleHidTest/BleHidManager.cs` - The Unity wrapper for the plugin
   - `Assets/BleHidTest/BleHidDemoController.cs` - Demo controller script
   - `Assets/BleHidTest/MouseTouchpad.cs` - Touchpad component
   - `Assets/BleHidTest/BleHidBootstrapper.cs` - UI bootstrapper script

2. Create a new scene or open an existing one

3. Create an empty GameObject in your scene

4. Add the `BleHidBootstrapper` script to it

5. Adjust settings in the Inspector if needed (all settings are optional with reasonable defaults)

6. Run your scene - all UI components will be created automatically

The bootstrapper will create a canvas with three panels:
- Connection panel with initialization and advertising controls
- Media control panel with play/pause, next/previous track, and volume buttons
- Mouse control panel with touchpad, click buttons, and scroll slider

## Manual Setup (Alternative Approach)

If you prefer more control over your UI layout or need to integrate with an existing UI, you can set up components manually:

1. Import the required files as listed above

2. Create a new scene or open an existing one

3. Create a UI Canvas with the following elements:

#### Connection Controls
- Initialize Button
- Advertise Button
- Status Text
- Connection Indicator Image

#### Media Controls
- Play/Pause Button
- Next Track Button
- Previous Track Button
- Volume Up Button
- Volume Down Button
- Mute Button

#### Mouse Controls
- MouseTouchpad component
- Left Click Button
- Right Click Button
- Scroll Slider

4. Create an empty GameObject in your scene

5. Add the `BleHidDemoController` script to it

6. Assign all your UI elements to the script's public fields in the Inspector

## Android Manifest Settings

Ensure your `AndroidManifest.xml` has the following permissions:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

For Android 12+ compatibility, also add:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
```

### 5. Build and Run

1. Build for Android and install on a device that supports BLE peripheral mode
2. Press the initialize button, then start advertising
3. Connect from a host device (computer, tablet, etc.)
4. Use the media and mouse controls

## API Reference

### BleHidManager

The main class provides the following methods:

#### Connection Management
- `InitializePlugin()` - Initialize the BLE HID plugin
- `StartAdvertising()` - Start advertising the BLE HID device
- `StopAdvertising()` - Stop advertising the BLE HID device
- `IsConnected()` - Check if connected to a host
- `ClosePlugin()` - Clean up resources

#### Media Controls
- `PlayPause()` - Send play/pause command
- `NextTrack()` - Send next track command
- `PreviousTrack()` - Send previous track command
- `VolumeUp()` - Send volume up command
- `VolumeDown()` - Send volume down command
- `Mute()` - Send mute command

#### Mouse Controls
- `MoveMouse(int x, int y)` - Move mouse pointer
- `MoveMouse(Vector2 movement, float sensitivity)` - Move mouse with Vector2 input
- `PressMouseButton(int button)` - Press a mouse button
- `ReleaseMouseButtons()` - Release all mouse buttons
- `ClickMouseButton(int button)` - Click a mouse button
- `ScrollMouseWheel(int amount)` - Scroll the mouse wheel

#### Advanced
- `SendCombinedReport(int mediaButtons, int mouseButtons, int x, int y)` - Send combined report

#### Events
- `OnConnected` - Called when a device connects
- `OnDisconnected` - Called when a device disconnects
- `OnConnectionStateChanged` - Called when connection state changes
- `OnPairingRequested` - Called when pairing is requested

### Button Constants

Use these constants for button flags:

```csharp
// Media buttons
BleHidManager.HidConstants.BUTTON_PLAY_PAUSE
BleHidManager.HidConstants.BUTTON_NEXT_TRACK
BleHidManager.HidConstants.BUTTON_PREVIOUS_TRACK
BleHidManager.HidConstants.BUTTON_VOLUME_UP
BleHidManager.HidConstants.BUTTON_VOLUME_DOWN
BleHidManager.HidConstants.BUTTON_MUTE

// Mouse buttons
BleHidManager.HidConstants.BUTTON_LEFT
BleHidManager.HidConstants.BUTTON_RIGHT
BleHidManager.HidConstants.BUTTON_MIDDLE
```

## Troubleshooting

1. **BLE not supported message**: Your device doesn't support Bluetooth Low Energy in peripheral mode
2. **Initialization failed**: Check Android logs for more details
3. **Connection issues**: 
   - Ensure Bluetooth is enabled on both devices
   - Check your device supports BLE peripheral mode
   - Verify you have the necessary permissions
4. **Commands not working**:
   - Verify you're connected to the host
   - Check Android logs for errors
   - Some hosts may not support all HID commands

## Sample Scene Layout

Here's an example layout for your Unity scene:

```
Canvas
├── Panel - Connection
│   ├── Initialize Button
│   ├── Advertise Button
│   ├── Status Text
│   └── Connection Indicator
├── Panel - Media Controls
│   ├── Play/Pause Button
│   ├── Next Button
│   ├── Previous Button
│   ├── Volume Up Button
│   ├── Volume Down Button
│   └── Mute Button
└── Panel - Mouse Controls
    ├── Joystick
    ├── Left Click Button
    ├── Right Click Button
    └── Scroll Slider
```

This layout makes it easy to organize your UI and provides a good starting point for your BLE HID controller interface.
