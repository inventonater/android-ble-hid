#!/bin/bash

# =============================================================================
# build-all.sh - Build all components of the Android BLE HID project
# =============================================================================
# This script builds all components (core, app, unity plugin) and helps 
# identify any build issues or package structure problems

echo "===== Building Android BLE HID with Modern BluetoothHidDevice API ====="

# Enable error reporting
set -e

# Clean the project first
echo "Cleaning project..."
cd "$(dirname "$0")/.." # Navigate to project root
./gradlew clean

# Build the core module
echo "Building core module..."
./gradlew :core:assembleDebug

# Build the app
echo "Building app module..."
./gradlew :app:assembleDebug

# Build the unity plugin
echo "Building Unity plugin..."
./gradlew :unity-plugin:assembleRelease

# Copy the Unity plugin to the Unity project
echo "Copying Unity plugin to Unity project..."
./gradlew :unity-plugin:copyToUnity

# Show dependency information
echo "Checking for dependency conflicts..."
./gradlew showDependencies

echo "===== Build completed successfully ====="
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"
echo "Unity plugin: unity-test/Assets/Plugins/Android/BleHidPlugin.aar"

# Print some package structure validations
echo "===== Verifying package structure ====="
echo "App package: $(grep -r "package" app/src/main/java/com/inventonater/blehid/app/MainActivity.java | head -1)"
echo "Unity plugin package: $(grep -r "package" unity-plugin/src/main/java/com/inventonater/blehid/unity/ModernBleHidPlugin.java | head -1)"
echo "===== Verification completed ====="
