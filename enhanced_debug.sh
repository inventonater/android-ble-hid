#!/bin/bash

# Enhanced BLE HID Debugging Tool
# This script provides in-depth debugging for BLE HID peripheral issues

echo "===== Enhanced BLE HID Debugging Tool ====="
echo ""

# Check for connected devices
DEVICE_CONNECTED=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
if [ "$DEVICE_CONNECTED" -eq 0 ]; then
    echo "❌ Error: No Android devices connected via ADB"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

echo "📱 Found connected Android device"

# Get device information
echo -n "📊 Android version: "
ANDROID_VERSION=$(adb shell getprop ro.build.version.release)
SDK_VERSION=$(adb shell getprop ro.build.version.sdk)
echo "$ANDROID_VERSION (API $SDK_VERSION)"

echo -n "📱 Device model: "
MODEL=$(adb shell getprop ro.product.model)
MANUFACTURER=$(adb shell getprop ro.product.manufacturer)
echo "$MANUFACTURER $MODEL"

# Clear logcat to ensure clean logs for debugging
adb logcat -c
echo "📋 Cleared logcat buffer"

# Start logging in the background to capture events
adb logcat > ble_hid_debug_log.txt &
LOG_PID=$!
echo "📝 Started logging to ble_hid_debug_log.txt (PID: $LOG_PID)"

# Launch the app
echo "🚀 Launching app..."
adb shell am start -n com.example.blehid.app/.MainActivity

# Wait for app to start
sleep 3

# Navigate to mouse activity
echo "🐭 Navigating to mouse activity..."
adb shell input tap 540 650
sleep 2

# Start advertising
echo "📢 Starting advertising..."
adb shell input tap 540 850
sleep 5

echo ""
echo "🔎 Checking for connected devices..."
CONNECTED_DEVICES=$(adb logcat -d | grep "Device connected" | tail -5)
if [ -n "$CONNECTED_DEVICES" ]; then
    echo "✅ Connected device found:"
    echo "$CONNECTED_DEVICES"
else
    echo "❌ No connected devices detected in logs"
fi

echo ""
echo "📊 Checking descriptor and characteristic activity..."
DESCRIPTOR_ACTIVITY=$(adb logcat -d | grep -E "descriptor|characteristic|notification" | tail -10)
if [ -n "$DESCRIPTOR_ACTIVITY" ]; then
    echo "✅ Descriptor/characteristic activity found:"
    echo "$DESCRIPTOR_ACTIVITY"
else
    echo "❌ No descriptor/characteristic activity detected"
    echo "   This suggests the host is not configuring notifications or reading descriptors"
fi

echo ""
echo "📊 Checking for mouse report attempts..."
MOUSE_REPORTS=$(adb logcat -d | grep "Mouse report sent" | tail -5)
if [ -n "$MOUSE_REPORTS" ]; then
    echo "✅ Mouse reports are being sent:"
    echo "$MOUSE_REPORTS"
else
    echo "❌ No mouse reports detected"
    echo "   This suggests input is not being sent or logged"
fi

echo ""
echo "🧪 Testing mouse movement..."
adb shell input tap 540 400
sleep 1
adb shell input touchscreen swipe 300 400 600 400 500
sleep 2

# Check again for mouse reports after manual input
MOUSE_REPORTS_AFTER=$(adb logcat -d | grep "Mouse report sent" | tail -5)
if [ -n "$MOUSE_REPORTS_AFTER" ]; then
    echo "✅ Mouse reports detected after manual input:"
    echo "$MOUSE_REPORTS_AFTER"
else
    echo "❌ Still no mouse reports detected after manual input"
    echo "   This may indicate issues with HID report generation or GATT notification"
fi

echo ""
echo "🔍 Checking GATT notifications..."
NOTIFICATIONS=$(adb logcat -d | grep -E "Notification sent|notification" | tail -5)
if [ -n "$NOTIFICATIONS" ]; then
    echo "✅ GATT notifications detected:"
    echo "$NOTIFICATIONS"
else
    echo "❌ No GATT notifications detected"
    echo "   This suggests notifications are not being sent or logged"
fi

echo ""
echo "💡 Checking HID descriptor read activity..."
DESCRIPTOR_READS=$(adb logcat -d | grep -E "Read request for|descriptor|HID_REPORT" | tail -10)
if [ -n "$DESCRIPTOR_READS" ]; then
    echo "✅ HID descriptor read activity detected:"
    echo "$DESCRIPTOR_READS"
else
    echo "❌ No HID descriptor read activity detected"
    echo "   This suggests the host may not be reading HID descriptors properly"
fi

echo ""
echo "⚠️ Detailed Diagnosis:"

# Check if connected but not sending reports
if [[ "$CONNECTED_DEVICES" && -z "$MOUSE_REPORTS" ]]; then
    echo "  • Connected but not sending reports - possible CCCD (Client Characteristic Configuration Descriptor) issue"
    echo "  • Host may not have enabled notifications for HID report characteristic"
fi

# Check if advertising but not connecting
if [[ -z "$CONNECTED_DEVICES" ]]; then
    echo "  • Advertising but not connecting - check host Bluetooth settings"
    echo "  • Make sure host Bluetooth is in discovery mode"
fi

# Check if HID descriptor isn't being read
if [[ "$CONNECTED_DEVICES" && -z "$DESCRIPTOR_READS" ]]; then
    echo "  • Connected but HID descriptor not read - host may not recognize device as HID"
    echo "  • Check HID service and characteristic UUIDs"
fi

# Kill background logging
kill $LOG_PID
echo ""
echo "📝 Stopped logging (Log saved to ble_hid_debug_log.txt)"

echo ""
echo "🚀 Next Steps for Troubleshooting:"
echo "  1. Use LightBlue Explorer app on Mac to check if HID service is visible"
echo "  2. Enable 'Input Monitoring' permission for Bluetooth in Mac System Preferences"
echo "  3. Try modifying HID report descriptor to follow Apple's HID descriptor guidelines"
echo "  4. Test connecting to a different host device (Windows PC or another Mac)"
echo "  5. Try a different Android device that's known to support BLE peripheral mode"

echo ""
echo "✨ Enhanced debugging complete!"
