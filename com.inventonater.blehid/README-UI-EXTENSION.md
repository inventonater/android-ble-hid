# BLE HID Local and Remote Control UI Extension

This extension adds a completely redesigned UI system with both local and remote device control capabilities. The new system provides a cleaner, more modular interface that's easier to maintain and extend.

## New Features

- **Tabbed Interface**: Clean separation between Local and Remote controls
- **Modular UI Architecture**: Each panel is a separate class with its own responsibility
- **Local Device Control**: Control the device running the app via Accessibility Services
- **Media Session Controls**: Control media playback on the local device
- **Extended Keyboard Controls**: Added directional keys and special keys
- **Improved Touch Controls**: Gesture-based input for both local and remote control

## Implementation Details

### Architecture Overview

The UI system is organized into the following components:

#### Core Framework
- `IBleHidPanel`: Interface defining the contract for all control panels
- `BaseBleHidPanel`: Abstract base implementation with shared functionality
- `BleHidLogger`: Centralized logging system for better debugging
- `BleHidUIController`: Main controller managing tabs and permissions

#### Local Device Control
- `LocalControlPanel`: Main panel with subtabs for local controls
- `LocalMediaPanel`: Media playback controls with app-specific features
- `LocalNavigationPanel`: Directional and system navigation controls
- `LocalSystemPanel`: System toggles and quick settings access
- `LocalTouchPanel`: Touch gestures and virtual touchpad

#### Remote Device Control
- `RemoteControlPanel`: Main panel with subtabs for BLE HID controls
- `RemoteMediaPanel`: Media control over BLE HID
- `RemoteMousePanel`: Mouse and pointer control over BLE HID
- `RemoteKeyboardPanel`: Keyboard input over BLE HID

### Required Permissions

The following permissions have been added to support local device control:

```xml
<!-- Media control permissions -->
<uses-permission android:name="android.permission.MEDIA_CONTENT_CONTROL" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
```

### Accessibility Service

The system uses Android's Accessibility Service to provide local device control:

```xml
<service
    android:name="com.inventonater.blehid.core.LocalAccessibilityService"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

## Usage

### Basic Usage

Simply add the `BleHidSimpleUI` component to a GameObject in your scene. It will automatically instantiate the new UI system including all panels.

```csharp
// Create and add BleHidSimpleUI component
GameObject uiObj = new GameObject("BleHidUI");
uiObj.AddComponent<BleHidSimpleUI>();
```

### Advanced Integration

For more control over the UI, you can work directly with the `BleHidUIController`:

```csharp
// Get the UI controller
BleHidUIController controller = FindObjectOfType<BleHidUIController>();

// Register custom panels
controller.RegisterPanel("Local", new MyCustomLocalPanel());
controller.RegisterPanel("Remote", new MyCustomRemotePanel());
```

## Custom Panels

To create custom panels, extend the `BaseBleHidPanel` class:

```csharp
public class MyCustomPanel : BaseBleHidPanel
{
    public override bool RequiresConnectedDevice => false;
    
    protected override void DrawPanelContent()
    {
        GUILayout.Label("My Custom Panel", titleStyle);
        
        if (GUILayout.Button("Custom Action", GUILayout.Height(60)))
        {
            // Do something
            logger.Log("Custom action performed");
        }
    }
}
```

## BleHidLocalControl API

The `BleHidLocalControl` class provides methods for controlling the local device:

```csharp
// Media controls
BleHidLocalControl.Instance.PlayPause();
BleHidLocalControl.Instance.NextTrack();
BleHidLocalControl.Instance.PreviousTrack();
BleHidLocalControl.Instance.VolumeUp();
BleHidLocalControl.Instance.VolumeDown();
BleHidLocalControl.Instance.Mute();

// Navigation controls
BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Up);
BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Down);
BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Left);
BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Right);
BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Back);
BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Home);
BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Recents);

// Touch controls
BleHidLocalControl.Instance.Tap(100, 200);
BleHidLocalControl.Instance.Swipe(100, 200, 300, 400);
