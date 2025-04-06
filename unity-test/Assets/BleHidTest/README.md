# BLE HID Unity Test App

This Unity application provides a simple test interface for the Android BLE HID functionality. It allows you to send media controls, mouse movements, and keyboard input to paired Bluetooth devices.

## Implementation

The implementation uses Unity's immediate mode GUI (IMGUI) system for a simple and reliable UI, especially for touch input on mobile devices. This approach was chosen over the more complex Canvas-based UI for its reliability and simplicity.

## Files

- **BleHidSimpleUI.cs**: Main script that implements the user interface and creates/manages the BleHidManager
- **BleHidManager.cs**: Core class that handles the communication with the native Android plugin
- **BleHidConstants.cs**: Contains constant values for key codes, button codes, etc.
- **BleHidSimpleScene.unity**: Unity scene file with the BleHidSimpleUI script attached

## Usage

1. Open the BleHidSimpleScene in Unity
2. Build the project for Android
3. Launch the app on an Android device
4. Tap "Start Advertising" to make the device discoverable as a Bluetooth HID device
5. Pair with the device from another Bluetooth host (computer, tablet, etc.)
6. Use the tabs to switch between Media, Mouse, and Keyboard controls
7. Send commands to the paired device

## UI Features

- **Status Area**: Shows initialization status, connection status, and advertising control
- **Tab System**: Switch between Media, Mouse, and Keyboard functionality
- **Media Controls**: Play/Pause, Next/Previous Track, Volume controls
- **Mouse Controls**: Touchpad for movement, buttons for clicks
- **Keyboard Controls**: Text input field and common keys
- **Log Display**: Shows real-time debugging information

## Technical Notes

This implementation uses the classic Unity Input system and OnGUI for better compatibility and simpler implementation. The UI is designed with large touch targets suitable for mobile devices.

The BleHidManager handles all the low-level communication with the Android-side plugin through the Unity-to-Java bridge.
