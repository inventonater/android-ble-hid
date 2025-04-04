#!/bin/bash
# Test script for Inventonater BLE HID debugging features
# This script helps verify all the debugging components are working correctly

echo "Starting Inventonater BLE HID debug test script"
echo "================================================"

# Check if Android SDK is available
if [ -z "$ANDROID_HOME" ]; then
  echo "Error: ANDROID_HOME environment variable not set."
  echo "Please set ANDROID_HOME to your Android SDK location."
  exit 1
fi

ADB="$ANDROID_HOME/platform-tools/adb"

# Check if device is connected
echo "Checking for connected Android devices..."
DEVICES=$($ADB devices | grep -v "List" | grep -v "^$" | wc -l)

if [ $DEVICES -eq 0 ]; then
  echo "Error: No Android devices connected."
  echo "Please connect a device and enable USB debugging."
  exit 1
fi

echo "Found $DEVICES device(s)"

# Build and install the app
echo "Building and installing app..."
./gradlew installDebug

if [ $? -ne 0 ]; then
  echo "Error: Failed to build and install the app."
  exit 1
fi

echo "App installed successfully."

# Start the ModernHidActivity
echo "Launching ModernHidActivity..."
$ADB shell am start -n "com.example.blehid.app/com.inventonater.blehid.ModernHidActivity"

echo "Waiting for app to start..."
sleep 3

# Test environment analysis
echo "Testing environment analysis..."
$ADB shell input tap 450 540  # Debug Options button position (approximate)
sleep 1
$ADB shell input tap 450 380  # Analyze Environment button position (approximate)
sleep 2

# Test HID descriptor analysis
echo "Testing HID descriptor analysis..."
$ADB shell input tap 450 540  # Debug Options button position
sleep 1
$ADB shell input tap 450 450  # Analyze HID Descriptor button position (approximate)
sleep 2

# Test report statistics
echo "Testing report statistics..."
$ADB shell input tap 450 540  # Debug Options button position
sleep 1
$ADB shell input tap 450 520  # Show Report Statistics button position (approximate)
sleep 2

# Test verbose logging
echo "Testing verbose logging..."
$ADB shell input tap 450 540  # Debug Options button position
sleep 1
$ADB shell input tap 510 340  # Verbose Logging switch position (approximate)
sleep 1

# Test file logging (requires permission)
echo "Testing file logging..."
$ADB shell input tap 450 540  # Debug Options button position
sleep 1
$ADB shell input tap 510 380  # File Logging switch position (approximate)
sleep 1

# Test registering HID device
echo "Testing HID device registration..."
$ADB shell input tap 450 180  # Register HID Device button position (approximate)
sleep 3

# Test mouse movement
echo "Testing mouse movement (if connected)..."
$ADB shell input tap 250 320  # Move Mouse button position (approximate)
sleep 3

# Test keyboard input
echo "Testing keyboard input (if connected)..."
$ADB shell input tap 250 400  # Type Text button position (approximate)
sleep 3

# Test media control
echo "Testing media control (if connected)..."
$ADB shell input tap 650 400  # Play/Pause button position (approximate)
sleep 3

# Pull logs
echo "Retrieving logs..."
$ADB shell "run-as com.example.blehid.app ls -l /data/data/com.example.blehid.app/files"
$ADB shell "run-as com.example.blehid.app ls -l /sdcard/Android/data/com.example.blehid.app/files/hid_logs"

echo "================================================"
echo "Debug test script completed."
echo "Please check the app's logs for detailed results."
echo "You may also need to grant storage permissions for file logging to work."
