#!/bin/bash

# =============================================================================
# run-app.sh - Build, install, and run the Android BLE HID app
# =============================================================================
# This script builds the Android app, installs it on a connected device,
# grants necessary permissions, and launches it.

# Terminal colors
RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
BLUE="\033[0;34m"
BOLD="\033[1m"
NC="\033[0m" # No Color

APP_ID="com.inventonater.blehid.app"

# Navigate to project root
cd "$(dirname "$0")/.."

echo -e "${BOLD}===== Android BLE HID App Launcher =====${NC}"
echo ""

# Check if Android device is connected
DEVICE_CONNECTED=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
if [ "$DEVICE_CONNECTED" -eq 0 ]; then
    echo -e "${RED}❌ Error: No Android devices connected via ADB${NC}"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi
echo -e "${GREEN}📱 Found connected Android device${NC}"

# Build and install the app
echo -e "${BLUE}🔨 Building and installing the app...${NC}"
./gradlew :app:installDebug

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed. Please check for errors.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ App installed successfully!${NC}"
echo ""

# Grant necessary permissions
echo -e "${BLUE}🔐 Setting up Bluetooth permissions...${NC}"

# Check Android version
ANDROID_VERSION=$(adb shell getprop ro.build.version.sdk)
echo "📊 Android SDK version: $ANDROID_VERSION"

# Grant each permission and report status
grant_permission() {
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
    grant_permission "android.permission.BLUETOOTH_ADVERTISE"
    grant_permission "android.permission.BLUETOOTH_CONNECT"
    grant_permission "android.permission.BLUETOOTH_SCAN"
else
    echo "🔒 Granting legacy permissions for Android < 12:"
    grant_permission "android.permission.ACCESS_FINE_LOCATION"
fi

# Enable Bluetooth if not already enabled
echo -n "📶 Ensuring Bluetooth is enabled: "
adb shell am broadcast -a android.bluetooth.adapter.action.REQUEST_ENABLE > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Requested${NC}"
else
    echo -e "${YELLOW}⚠️ Could not request Bluetooth enable${NC}"
fi

# Launch the app
echo -e "${BLUE}🚀 Launching the app...${NC}"
adb shell am start -n $APP_ID/.MainActivity
echo -e "${GREEN}✅ App launched!${NC}"
echo ""

# Instructions for the user
echo -e "${YELLOW}===== Next Steps =====${NC}"
echo -e "1. On the Android device, tap ${BOLD}Start BLE HID Device${NC}"
echo -e "2. Use the app interface to send HID commands (keyboard, mouse, media)"
echo -e "3. Connect to the Android device from your host computer's Bluetooth settings"
echo ""
echo -e "${YELLOW}===== Troubleshooting =====${NC}"
echo -e "• If the device doesn't appear in the host's Bluetooth list:"
echo -e "  - Ensure Bluetooth is enabled on both devices"
echo -e "  - Check the app logs for any errors"
echo -e "  - Run ./developer-tools/check-device.sh to verify device compatibility"
echo -e "  - Run ./developer-tools/debug-connection.sh for more detailed diagnostics"
echo ""

echo -e "${GREEN}✨ Setup complete! Follow the instructions above to use your Android device as an HID peripheral.${NC}"
