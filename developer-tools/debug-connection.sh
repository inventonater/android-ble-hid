#!/bin/bash

# =============================================================================
# debug-connection.sh - Debug BLE advertising and connection issues
# =============================================================================
# This script provides diagnostics for BLE connectivity problems between
# Android and host devices.

# Terminal colors
RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
BLUE="\033[0;34m"
BOLD="\033[1m"
NC="\033[0m" # No Color

APP_ID="com.inventonater.blehid.app"

# Parse arguments for enhanced debugging
ENHANCED=false
if [ "$1" = "--enhanced" ]; then
    ENHANCED=true
    echo -e "${BOLD}===== Enhanced BLE HID Debugging Tool =====${NC}"
else
    echo -e "${BOLD}===== BLE HID Connection Debugger =====${NC}"
fi
echo ""

# Navigate to project root
cd "$(dirname "$0")/.."

# Check if Android device is connected
DEVICE_CONNECTED=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
if [ "$DEVICE_CONNECTED" -eq 0 ]; then
    echo -e "${RED}❌ Error: No Android devices connected via ADB${NC}"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi
echo -e "${GREEN}📱 Found connected Android device${NC}"

# If enhanced, get more device info and start logging
if [ "$ENHANCED" = true ]; then
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
    LOG_FILE="ble_hid_debug_$(date +%Y%m%d_%H%M%S).log"
    adb logcat > "$LOG_FILE" &
    LOG_PID=$!
    echo "📝 Started logging to $LOG_FILE (PID: $LOG_PID)"
fi

# Check Bluetooth status
echo -n "📶 Checking Bluetooth status: "
BT_STATE=$(adb shell settings get global bluetooth_on 2>/dev/null)
if [ "$BT_STATE" = "1" ]; then
    echo -e "${GREEN}✅ ON${NC}"
else
    echo -e "${RED}❌ OFF${NC}"
    echo "⚠️ Enabling Bluetooth to complete tests..."
    adb shell am start -a android.bluetooth.adapter.action.REQUEST_ENABLE
    sleep 2
fi

# Get the Bluetooth name
echo -n "📶 Bluetooth adapter name: "
BT_NAME=$(adb shell dumpsys bluetooth_manager | grep "name:" | head -n 1 | cut -d ":" -f 2- | xargs)
echo "$BT_NAME"

# Check if app is running, if not, start it
echo -n "🔍 Checking if app is running: "
APP_RUNNING=$(adb shell ps | grep $APP_ID | wc -l)
if [ "$APP_RUNNING" -gt 0 ]; then
    echo -e "${GREEN}✅ Yes${NC}"
else
    echo -e "${YELLOW}❌ No${NC}"
    echo "🚀 Starting app..."
    adb shell am start -n $APP_ID/.MainActivity
    sleep 2
fi

# Check Bluetooth permissions
echo "🔐 Checking Bluetooth permissions:"
adb shell dumpsys package $APP_ID | grep -E "BLUETOOTH|ACCESS_FINE_LOCATION" | while read -r line; do
    PERM=$(echo "$line" | xargs)
    GRANTED=$(echo "$line" | grep "granted=true" | wc -l)
    if [ "$GRANTED" -gt 0 ]; then
        echo -e "  • $PERM ${GREEN}✅${NC}"
    else
        echo -e "  • $PERM ${RED}❌${NC}"
        echo "    ⚠️ Permission not granted - this will prevent proper functionality!"
    fi
done

# Check if BLE advertising is supported
echo -n "📡 Checking if BLE advertising is supported: "
BLE_SUPPORTED=$(adb shell dumpsys bluetooth_manager | grep -i "LeAdvertiser" | wc -l)
if [ "$BLE_SUPPORTED" -gt 0 ]; then
    echo -e "${GREEN}✅ Yes${NC}"
else 
    echo -e "${RED}❌ No${NC}"
    echo "⚠️ This device might not support BLE peripheral mode"
fi

# Print recent logs related to advertising
echo ""
echo "📋 Recent BLE advertising logs:"
adb logcat -d | grep -E "BleAdvert|Bluetooth|HID|Gatt" | tail -15

# Enhanced debugging features
if [ "$ENHANCED" = true ]; then
    # Navigate to HID activity if not already there
    echo "🎮 Navigating to HID activity..."
    adb shell input tap 540 650
    sleep 2

    # Start advertising (by clicking the main button)
    echo "📢 Testing HID functionality..."
    adb shell input tap 540 850
    sleep 5

    echo ""
    echo "🔎 Checking for connected devices..."
    CONNECTED_DEVICES=$(adb logcat -d | grep -E "Device connected|CONNECTION_STATE_CONNECTED" | tail -5)
    if [ -n "$CONNECTED_DEVICES" ]; then
        echo -e "${GREEN}✅ Connected device found:${NC}"
        echo "$CONNECTED_DEVICES"
    else
        echo -e "${RED}❌ No connected devices detected in logs${NC}"
    fi

    echo ""
    echo "📊 Checking HID report activity..."
    REPORT_ACTIVITY=$(adb logcat -d | grep -E "report|HID_REPORT|sendReport" | tail -10)
    if [ -n "$REPORT_ACTIVITY" ]; then
        echo -e "${GREEN}✅ HID report activity found:${NC}"
        echo "$REPORT_ACTIVITY"
    else
        echo -e "${RED}❌ No HID report activity detected${NC}"
        echo "   This suggests reports aren't being sent or logged"
    fi

    echo ""
    echo "🧪 Testing input events..."
    if [ -n "$CONNECTED_DEVICES" ]; then
        echo "Sending test events to verify HID functionality..."
        # Click on the mouse move area to test mouse reports
        adb shell input tap 400 400
        sleep 1
        # Swipe to test movement
        adb shell input touchscreen swipe 300 400 600 400 500
        sleep 2
    else
        echo -e "${YELLOW}⚠️ Skipping input tests as no device is connected${NC}"
    fi

    # Check again for reports after manual input
    if [ -n "$CONNECTED_DEVICES" ]; then
        REPORTS_AFTER=$(adb logcat -d | grep -E "report|sendReport" | tail -5)
        if [ -n "$REPORTS_AFTER" ]; then
            echo -e "${GREEN}✅ Reports detected after input:${NC}"
            echo "$REPORTS_AFTER"
        else
            echo -e "${RED}❌ No reports detected after input${NC}"
            echo "   This indicates an issue with HID report generation"
        fi
    fi

    # Detailed diagnosis
    echo ""
    echo -e "${BOLD}===== Detailed Diagnosis =====${NC}"

    # Check for common issues
    if [ -z "$CONNECTED_DEVICES" ]; then
        echo -e "${YELLOW}⚠️ No connected devices detected${NC}"
        echo "  Possible causes:"
        echo "  • Host not discovering the Android device (check host Bluetooth settings)"
        echo "  • Bluetooth advertising not correctly configured"
        echo "  • HID service not properly advertised"
    elif [ -z "$REPORT_ACTIVITY" ]; then
        echo -e "${YELLOW}⚠️ Connected but no HID reports detected${NC}"
        echo "  Possible causes:"
        echo "  • Connection established but HID protocol not initialized"
        echo "  • Report characteristic notifications not enabled by host"
        echo "  • HID descriptor not compatible with host device"
    fi

    # Check for HID descriptor issues
    HID_DESCRIPTOR_ISSUES=$(adb logcat -d | grep -E "descriptor|READ_DESCRIPTOR|REPORT_MAP" | tail -5)
    if [ -n "$HID_DESCRIPTOR_ISSUES" ]; then
        echo -e "${BLUE}💡 HID descriptor activity:${NC}"
        echo "$HID_DESCRIPTOR_ISSUES"
    fi

    # Kill background logging
    kill $LOG_PID
    echo ""
    echo "📝 Stopped logging (Log saved to $LOG_FILE)"

    echo ""
    echo "🚀 Next Steps for Troubleshooting:"
    echo "  1. Check the generated log file for more details: $LOG_FILE"
    echo "  2. Look for Bluetooth connection state changes in the logs"
    echo "  3. If not connecting, try to connect from a different host device"
    echo "  4. If connecting but not receiving HID events, check HID descriptor"
    echo "  5. Ensure host device supports HID over Bluetooth"
else
    echo ""
    echo "💡 Debugging instructions:"
    echo "  1. After launching the app, tap the 'Start BLE HID Device' button"
    echo "  2. On your host device, open Bluetooth settings and search for new devices"
    echo "  3. Look for 'Android BLE HID' in the device list"
    echo "  4. Try to connect from the host device"
    echo ""
    echo "📊 For more detailed diagnostics:"
    echo "  Run: ./developer-tools/debug-connection.sh --enhanced"
fi

echo ""
echo -e "${GREEN}✨ Debugging session complete!${NC}"
