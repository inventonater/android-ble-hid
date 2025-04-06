# BLE HID Unity Plugin

This Unity plugin provides Bluetooth Low Energy (BLE) Human Interface Device (HID) functionality for Android devices, allowing them to act as BLE peripherals such as keyboards, mice, and media remote controls.

## Setup Guide

1. **Import the Plugin**:
   - The plugin is automatically included in the `Assets/Plugins/Android` folder
   - The core functionality is in the `BleHidPlugin.aar` file

2. **Android Manifest Settings**:
   - Required permissions and features are defined in the AndroidManifest.xml
   - Check that Bluetooth permissions are properly declared

3. **Project Settings**:
   - Target Android API level 34 for optimal compatibility
   - Package name set to `com.example.blehid.unity`
   - IL2CPP scripting backend recommended for best performance

## Plugin Architecture

The plugin is organized into several components:

- **Core BLE Services**: Manages connections, advertisements, and GATT services
- **HID Services**: Specialized implementations for mouse, keyboard, and media controls
- **Unity Bridge**: Connects Java and C# through JNI
- **Event System**: Allows the plugin to notify Unity of important events

## Troubleshooting

If you encounter build issues:

1. **Namespace Conflicts**: Plugin namespace is now `com.example.blehid.plugin` to avoid conflicts with the Unity app package
2. **Duplicate Classes**: Unity's built-in classes are excluded from the plugin to prevent duplication
3. **Gradle Issues**: Custom Gradle settings are provided in `gradle.properties` file

## API Documentation

Check the C# scripts in the `BleHidTest` folder for usage examples. The main classes to interact with are:

- `BleHidManager`: Central manager for initializing and controlling all BLE HID functionality
- `BleHidEventRegistry`: System for receiving events from the plugin
- `BleHidProtocol`: Constants and protocol definitions
