#!/bin/bash

# =============================================================================
# build-unity.sh - Build the Unity plugin for Android BLE HID
# =============================================================================
# This script builds and copies the Unity plugin to the Unity project

echo "===== Building Unity Plugin for Android BLE HID ====="

# Enable error reporting
set -e

# Clean and build the Unity plugin
echo "Cleaning and building Unity plugin..."
cd "$(dirname "$0")/.." # Navigate to project root
./gradlew :unity-plugin:clean :unity-plugin:assembleRelease

# Copy the plugin to the Unity project
echo "Copying Unity plugin to Unity project..."
./gradlew :unity-plugin:copyToUnity

echo "===== Build completed successfully ====="
echo "Unity plugin: unity-test/Assets/Plugins/Android/BleHidPlugin.aar"

# Print package validation info
echo "Plugin package: $(grep -r "package" unity-plugin/src/main/java/com/inventonater/blehid/unity/ModernBleHidPlugin.java | head -1)"

echo ""
echo "To use in Unity:"
echo "1. Open your Unity project"
echo "2. The BleHidPlugin.aar file is already copied to Assets/Plugins/Android/"
echo "3. Add ModernBleHidManager.cs to your Unity scripts"
echo "4. Create a GameObject and attach the ModernBleHidManager component"
echo "5. Use ModernBleHidManager.Instance to access HID functionality"
