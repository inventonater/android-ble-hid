#!/bin/bash

# BLE HID Debug Script
# This script helps troubleshoot BLE advertising and connection issues

echo "===== Android BLE HID Debugging Tool ====="
echo ""

# Check for connected devices
DEVICE_CONNECTED=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
if [ "$DEVICE_CONNECTED" -eq 0 ]; then
    echo "❌ Error: No Android devices connected via ADB"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

echo "📱 Found connected Android device"

# Check Bluetooth status
echo -n "📶 Checking Bluetooth status: "
BT_STATE=$(adb shell settings get global bluetooth_on 2>/dev/null)
if [ "$BT_STATE" = "1" ]; then
    echo "✅ ON"
else
    echo "❌ OFF"
    echo "⚠️ Please enable Bluetooth on your Android device"
    adb shell am start -a android.bluetooth.adapter.action.REQUEST_ENABLE
    exit 1
fi

# Get the Bluetooth name
echo -n "📶 Bluetooth adapter name: "
BT_NAME=$(adb shell dumpsys bluetooth_manager | grep "name:" | head -n 1 | cut -d ":" -f 2- | xargs)
echo "$BT_NAME"

# Check if app is running, if not, start it
echo -n "🔍 Checking if app is running: "
APP_RUNNING=$(adb shell ps | grep com.example.blehid.app | wc -l)
if [ "$APP_RUNNING" -gt 0 ]; then
    echo "✅ Yes"
else
    echo "❌ No"
    echo "🚀 Starting app..."
    adb shell am start -n com.example.blehid.app/.MainActivity
fi

# Check Bluetooth permissions
echo "🔐 Checking Bluetooth permissions:"
adb shell dumpsys package com.example.blehid.app | grep -E "BLUETOOTH|ACCESS_FINE_LOCATION" | while read -r line; do
    PERM=$(echo "$line" | xargs)
    GRANTED=$(echo "$line" | grep "granted=true" | wc -l)
    if [ "$GRANTED" -gt 0 ]; then
        echo "  • $PERM ✅"
    else
        echo "  • $PERM ❌"
        echo "    ⚠️ Permission not granted!"
    fi
done

# Check if BLE advertising is supported
echo -n "📡 Checking if BLE advertising is supported: "
BLE_SUPPORTED=$(adb shell dumpsys bluetooth_manager | grep -i "LeAdvertiser" | wc -l)
if [ "$BLE_SUPPORTED" -gt 0 ]; then
    echo "✅ Yes"
else 
    echo "❌ No"
    echo "⚠️ This device might not support BLE peripheral mode"
fi

# Print recent logs related to advertising
echo ""
echo "📋 Recent BLE advertising logs:"
adb logcat -d | grep -E "BleAdvert|BLE Advertising" | tail -15

echo ""
echo "💡 Instructions for debugging:"
echo "  1. On the Android device, tap 'Start Mouse' button"
echo "  2. Then tap 'Start Advertising' button"
echo "  3. Check logcat to see if advertising starts successfully"
echo "  4. On your Mac, open System Preferences → Bluetooth"
echo "  5. Keep the Bluetooth panel open (this helps with discovery)"
echo ""
echo "🔍 Alternative debugging with LightBlue Explorer (Mac):"
echo "  1. Install LightBlue Explorer from Mac App Store"
echo "  2. Open it while the Android app is advertising"
echo "  3. Check if your Android device appears in the scanner"
echo "  4. Look for a device with HID service UUID (0x1812)"
echo ""
echo "✨ Debug complete!"
