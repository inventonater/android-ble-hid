#!/bin/bash

# Script to test BLE HID mouse connectivity and help diagnose issues
# Created to address mouse event responsiveness problems

# ANSI color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m' # No Color

echo -e "${BOLD}BLE HID Mouse Connectivity Test${NC}"
echo "===============================
This script will help diagnose BLE HID mouse connectivity issues
and provide diagnostics for troubleshooting.
"

echo -e "${BLUE}== Testing Bluetooth status ==${NC}"
if ! command -v bluetoothctl &> /dev/null; then
    echo -e "${RED}Error: bluetoothctl not found. This script requires bluetoothctl to be installed.${NC}"
    exit 1
fi

# Check if Bluetooth is enabled
BT_STATUS=$(bluetoothctl show | grep "Powered" | awk '{print $2}')
if [ "$BT_STATUS" != "yes" ]; then
    echo -e "${RED}Bluetooth is not enabled. Please enable Bluetooth and try again.${NC}"
    echo "You can enable it with: bluetoothctl power on"
    exit 1
else
    echo -e "${GREEN}✓ Bluetooth is enabled${NC}"
fi

# Check for BLE advertising capability
echo -e "\n${BLUE}== Testing BLE advertising capability ==${NC}"
ADVTEST=$(sudo hciconfig hci0 lestates 2>&1)
if [[ $ADVTEST == *"Not supported"* ]]; then
    echo -e "${RED}BLE advertising not supported on this device${NC}"
    echo "This may prevent the device from being discovered"
else
    echo -e "${GREEN}✓ BLE advertising appears to be supported${NC}"
fi

# Check for GATT server capability
echo -e "\n${BLUE}== Testing GATT server capability ==${NC}"
if [ -f "$ANDROID_SDK_ROOT/platform-tools/adb" ]; then
    ADB="$ANDROID_SDK_ROOT/platform-tools/adb"
elif command -v adb &> /dev/null; then
    ADB="adb"
else
    echo -e "${YELLOW}ADB not found. Skipping GATT server test.${NC}"
    ADB=""
fi

if [ -n "$ADB" ]; then
    # Check for connected device
    if $ADB devices | grep -q "device$"; then
        echo -e "${GREEN}✓ Android device connected${NC}"
        
        # Check if app is installed
        if $ADB shell pm list packages | grep -q "com.example.blehid"; then
            echo -e "${GREEN}✓ BLE HID app is installed${NC}"
            
            # Check for BLE peripheral mode support
            BLE_SUPPORTED=$($ADB shell dumpsys bluetooth_manager | grep -c "isMultipleAdvertisementSupported=true")
            if [ "$BLE_SUPPORTED" -gt 0 ]; then
                echo -e "${GREEN}✓ BLE peripheral mode appears to be supported${NC}"
            else
                echo -e "${RED}BLE peripheral mode may not be supported${NC}"
                echo "This is required for the app to function properly."
            fi
            
            # Check Android version for HID profile support
            ANDROID_VER=$($ADB shell getprop ro.build.version.sdk)
            if [ "$ANDROID_VER" -ge 24 ]; then
                echo -e "${GREEN}✓ Android API level $ANDROID_VER supports HID profile${NC}"
            else
                echo -e "${RED}Android API level $ANDROID_VER may not fully support HID profile${NC}"
                echo "Android 7.0 (API level 24) or higher is recommended."
            fi
        else
            echo -e "${YELLOW}BLE HID app not installed on the device${NC}"
        fi
    else
        echo -e "${YELLOW}No Android device connected. Skipping app tests.${NC}"
    fi
fi

# Check for nearby Bluetooth devices
echo -e "\n${BLUE}== Scanning for nearby Bluetooth devices ==${NC}"
echo "Starting a brief scan for nearby devices..."
bluetoothctl scan on &
sleep 5
bluetoothctl scan off
DEVICES=$(bluetoothctl devices | wc -l)
echo "Found $DEVICES nearby Bluetooth devices"

# Notification and connection diagnostics
echo -e "\n${BLUE}== Connection Diagnostics ==${NC}"

echo -e "${YELLOW}Potential reasons for the \"Nearby Devices section still has a spinning icon\" issue:${NC}"
echo "1. Advertising service not being found by hosts (fixed with new advertising data)"
echo "2. Notification system not properly configured (fixed with improved notification handling)"
echo "3. GATT service not correctly registered (improved in GATT server manager)" 

echo -e "\n${YELLOW}Potential reasons for mouse events not responding:${NC}"
echo "1. Notifications not enabled or acknowledged properly (fixed in HidMouseService)"
echo "2. Pairing but not completing connection (addressed with better initial reports)"
echo "3. Mouse report format not compatible with hosts (improved Apple compatibility)" 

# Log helper
echo -e "\n${BLUE}== Improved Logging ==${NC}"
echo "The updated code now provides more detailed logs to diagnose connection issues."
echo "To check logs in real-time while using the app, run:"
echo -e "${BOLD}adb logcat | grep -E \"BleHidManager|HidMouseService|BleGattServerManager|BleAdvertiser\"${NC}"

echo -e "\n${BOLD}Next steps:${NC}"
echo "1. Rebuild and reinstall the app with the latest changes"
echo "2. When using the app, try the following sequence:"
echo "   a. Start advertising"
echo "   b. Connect from host device" 
echo "   c. If connection succeeds but mouse isn't responding, try disconnecting and reconnecting"
echo "3. If still having issues, check the log for error messages"

echo -e "\n${GREEN}Test completed.${NC}"
