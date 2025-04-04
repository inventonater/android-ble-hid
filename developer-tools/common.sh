#!/bin/bash

# =============================================================================
# common.sh - Shared utility functions for Android BLE HID developer tools
# =============================================================================
# This file contains common functions used across multiple developer tools scripts.
# Source this file at the beginning of each script to use these functions.

# Package ID
APP_ID="com.inventonater.blehid.app"

# Terminal colors
RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
BLUE="\033[0;34m"
BOLD="\033[1m"
NC="\033[0m" # No Color

# Check if Android device is connected
check_device_connected() {
    DEVICE_CONNECTED=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
    if [ "$DEVICE_CONNECTED" -eq 0 ]; then
        echo -e "${RED}❌ Error: No Android devices connected via ADB${NC}"
        echo "Please connect your device and enable USB debugging"
        return 1
    fi
    echo -e "${GREEN}📱 Found connected Android device${NC}"
    return 0
}

# Get device information
get_device_info() {
    echo -n "📊 Android version: "
    ANDROID_VERSION=$(adb shell getprop ro.build.version.release)
    SDK_VERSION=$(adb shell getprop ro.build.version.sdk)
    echo "$ANDROID_VERSION (API $SDK_VERSION)"

    echo -n "📱 Device model: "
    MODEL=$(adb shell getprop ro.product.model)
    MANUFACTURER=$(adb shell getprop ro.product.manufacturer)
    echo "$MANUFACTURER $MODEL"
    
    return 0
}

# Check Bluetooth status
check_bluetooth_status() {
    echo -n "📶 Checking Bluetooth status: "
    BT_STATE=$(adb shell settings get global bluetooth_on 2>/dev/null)
    if [ "$BT_STATE" = "1" ]; then
        echo -e "${GREEN}✅ ON${NC}"
        return 0
    else
        echo -e "${RED}❌ OFF${NC}"
        echo "⚠️ Enabling Bluetooth to complete tests..."
        adb shell am start -a android.bluetooth.adapter.action.REQUEST_ENABLE
        sleep 2
        return 1
    fi
}

# Grant the necessary permissions
grant_permissions() {
    echo -e "${BLUE}🔐 Setting up Bluetooth permissions...${NC}"

    # Check Android version
    ANDROID_VERSION=$(adb shell getprop ro.build.version.sdk)
    echo "📊 Android SDK version: $ANDROID_VERSION"

    # Grant each permission and report status
    grant_single_permission() {
        PERM=$1
        echo -n "  • $PERM: "
        
        # Try to grant the permission
        RESULT=$(adb shell pm grant $APP_ID $PERM 2>&1)
        
        # Check if successful
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✅ Granted${NC}"
        else
            if [[ "$RESULT" == *"not a changeable permission type"* ]]; then
                echo -e "${YELLOW}⚠️ Not a runtime permission (normal)${NC}"
            elif [[ "$RESULT" == *"has not requested permission"* ]]; then
                echo -e "${YELLOW}⚠️ Not declared in manifest${NC}"
            else
                echo -e "${RED}❌ Failed: $RESULT${NC}"
            fi
        fi
    }

    # Only try to grant the runtime permissions
    if [ "$ANDROID_VERSION" -ge 31 ]; then  # Android 12+
        echo "🔒 Granting Android 12+ specific permissions:"
        grant_single_permission "android.permission.BLUETOOTH_ADVERTISE"
        grant_single_permission "android.permission.BLUETOOTH_CONNECT"
        grant_single_permission "android.permission.BLUETOOTH_SCAN"
    else
        echo "🔒 Granting legacy permissions for Android < 12:"
        grant_single_permission "android.permission.ACCESS_FINE_LOCATION"
    fi

    # Enable Bluetooth if not already enabled
    echo -n "📶 Ensuring Bluetooth is enabled: "
    adb shell am broadcast -a android.bluetooth.adapter.action.REQUEST_ENABLE > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Requested${NC}"
    else
        echo -e "${YELLOW}⚠️ Could not request Bluetooth enable${NC}"
    fi

    # Check if Bluetooth is enabled
    BT_STATE=$(adb shell settings get global bluetooth_on 2>/dev/null)
    if [ "$BT_STATE" = "1" ]; then
        echo -e "📶 Bluetooth is enabled: ${GREEN}✅${NC}"
    else
        echo -e "📶 Bluetooth status: ${YELLOW}⚠️ Might be disabled, check your device${NC}"
    fi
    
    echo -e "${GREEN}✅ Permission setup complete!${NC}"
}

# Navigate to project root (call this from any script)
navigate_to_project_root() {
    cd "$(dirname "$0")/.."
}

# Set executable permissions for all scripts
ensure_scripts_executable() {
    chmod +x developer-tools/*.sh
    echo "Made all scripts executable"
}

# Start logging to a file
start_logging() {
    LOG_FILE="ble_hid_$(date +%Y%m%d_%H%M%S).log"
    adb logcat -c
    echo "📋 Cleared logcat buffer"
    adb logcat > "$LOG_FILE" &
    LOG_PID=$!
    echo "📝 Started logging to $LOG_FILE (PID: $LOG_PID)"
    
    # Return the log file and PID for later reference
    echo "$LOG_FILE $LOG_PID"
}

# Stop logging
stop_logging() {
    LOG_PID=$1
    LOG_FILE=$2
    
    if [ -n "$LOG_PID" ]; then
        kill $LOG_PID 2>/dev/null
        echo "📝 Stopped logging (Log saved to $LOG_FILE)"
    fi
}
