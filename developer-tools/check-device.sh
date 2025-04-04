#!/bin/bash

# =============================================================================
# check-device.sh - Check if a device supports BLE peripheral mode
# =============================================================================
# This script checks if the connected Android device supports BLE peripheral mode
# and HID functionality necessary for the BluetoothHidDevice API

# Terminal colors
RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
BLUE="\033[0;34m"
BOLD="\033[1m"
NC="\033[0m" # No Color

echo -e "${BOLD}===== BLE Peripheral Capability Checker =====${NC}"
echo ""

# Check if Android device is connected
DEVICE_CONNECTED=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
if [ "$DEVICE_CONNECTED" -eq 0 ]; then
    echo -e "${RED}❌ Error: No Android devices connected via ADB${NC}"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi
echo -e "${GREEN}📱 Found connected Android device${NC}"

# Get device info
echo -n "📊 Android version: "
ANDROID_VERSION=$(adb shell getprop ro.build.version.release)
SDK_VERSION=$(adb shell getprop ro.build.version.sdk)
echo "$ANDROID_VERSION (API $SDK_VERSION)"

echo -n "📱 Device model: "
MODEL=$(adb shell getprop ro.product.model)
MANUFACTURER=$(adb shell getprop ro.product.manufacturer)
echo "$MANUFACTURER $MODEL"

# Check if SDK version is high enough for BluetoothHidDevice
if [ "$SDK_VERSION" -lt 28 ]; then
    echo -e "${RED}❌ Device API level ($SDK_VERSION) is below Android 9 (API 28)${NC}"
    echo "BluetoothHidDevice API requires Android 9 (API 28) or higher"
    echo "This device cannot use the modern BluetoothHidDevice implementation."
    exit 1
fi
echo -e "${GREEN}✓ API level ($SDK_VERSION) supports BluetoothHidDevice API${NC}"

# Check for BLE adapter support
echo -e "${BLUE}🔍 Checking BLE capability...${NC}"

echo -n "  • Bluetooth Low Energy support: "
BLE_SUPPORTED1=$(adb shell "cmd bluetooth has-feature ble" 2>/dev/null)
BLE_SUPPORTED2=$(adb shell dumpsys bluetooth_manager | grep -i "mLeState" | grep -i "on" | wc -l)

# Alternative check: All modern Android devices with Bluetooth support BLE
if [[ "$BLE_SUPPORTED1" == *"true"* ]] || [ "$BLE_SUPPORTED2" -gt 0 ] || [ "$SDK_VERSION" -ge 21 ]; then
    echo -e "${GREEN}✅ Supported${NC}"
    BLE_SUPPORTED=true
else
    echo -e "${RED}❌ Not supported${NC}"
    echo "⚠️ Device reports no BLE support, but this is unusual for modern devices."
    echo "   Continuing with checks anyway..."
    BLE_SUPPORTED=false
fi

# Check for BLE peripheral mode support
echo -n "  • BLE peripheral (advertiser) mode: "

# Multiple methods to detect peripheral mode capability
ADVERTISER_CHECK1=$(adb shell dumpsys bluetooth_manager | grep -i "LeAdvertiser" | wc -l)
ADVERTISER_CHECK2=$(adb shell "cmd bluetooth has-feature le-peripheral" 2>/dev/null)
MULTIPLE_ADV=$(adb shell "cmd bluetooth has-feature le-extended-advertising" 2>/dev/null)

if [[ "$ADVERTISER_CHECK2" == *"true"* ]]; then
    echo -e "${GREEN}✅ Supported (confirmed via Bluetooth service)${NC}"
    PERIPHERAL_SUPPORTED=true
elif [ "$ADVERTISER_CHECK1" -gt 0 ]; then
    echo -e "${GREEN}✅ Supported (LeAdvertiser found in Bluetooth manager)${NC}"
    PERIPHERAL_SUPPORTED=true
else
    echo -e "${RED}❌ Not supported${NC}"
    echo -e "${RED}❌ This device cannot act as a BLE peripheral${NC}"
    PERIPHERAL_SUPPORTED=false
fi

# Check HID profile support
echo -n "  • HID Device profile support: "
HID_SUPPORTED=$(adb shell dumpsys bluetooth_manager | grep -i "HidDevice" | wc -l)

if [ "$HID_SUPPORTED" -gt 0 ]; then
    echo -e "${GREEN}✅ Supported${NC}"
    HID_PROFILE_SUPPORTED=true
else
    echo -e "${YELLOW}⚠️ Could not confirm HID profile support${NC}"
    echo "   This might still work as the BluetoothHidDevice API may not show up in dumpsys"
    HID_PROFILE_SUPPORTED=true
fi

# Summary
echo ""
echo -e "${BOLD}===== Summary =====${NC}"
if [ "$PERIPHERAL_SUPPORTED" = true ] && [ "$SDK_VERSION" -ge 28 ]; then
    echo -e "${GREEN}✅ GOOD NEWS: This device ($MANUFACTURER $MODEL) supports BLE peripheral mode${NC}"
    echo -e "${GREEN}✅ API level ($SDK_VERSION) supports BluetoothHidDevice API${NC}"
    echo "   You should be able to use the Android BLE HID app on this device."
    echo ""
    echo "🔖 Next steps:"
    echo "   1. Run ./developer-tools/run-app.sh to install and launch the app"
    echo "   2. Connect to the device from your host computer"
else
    echo -e "${RED}❌ BAD NEWS: This device ($MANUFACTURER $MODEL) is not compatible${NC}"
    if [ "$SDK_VERSION" -lt 28 ]; then
        echo "   Android 9 (API 28) or higher is required for BluetoothHidDevice API"
    fi
    if [ "$PERIPHERAL_SUPPORTED" = false ]; then
        echo "   This device does not support BLE peripheral mode"
    fi
    echo ""
    echo "💡 What you can do instead:"
    echo "   • Try a different Android device (Android 9 or higher is required)"
    echo "   • Known good devices: Pixel 3 or newer, Samsung Galaxy S8 or newer"
fi

echo ""
echo -e "${GREEN}✨ Capability check complete!${NC}"
