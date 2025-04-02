#!/bin/bash

# Android BLE HID app deployment and launch helper script
# This script builds, installs, grants permissions, and launches the BLE HID app

# Terminal colors
GREEN="\033[0;32m"
BLUE="\033[0;34m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
NC="\033[0m" # No Color
BOLD="\033[1m"

APP_ID="com.example.blehid.app"

echo -e "${BOLD}===== Android BLE HID App Runner =====${NC}"
echo ""

# Check for connected devices
DEVICE_CONNECTED=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
if [ "$DEVICE_CONNECTED" -eq 0 ]; then
    echo -e "${RED}❌ Error: No Android devices connected via ADB${NC}"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

echo -e "${GREEN}📱 Found connected Android device${NC}"

# Build and install the app
echo -e "${BLUE}🔨 Building and installing the app...${NC}"
./gradlew installDebug

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Build failed. Please check for errors.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ App installed successfully!${NC}"
echo ""

# Run the grant permissions script
echo -e "${BLUE}🔐 Setting up permissions...${NC}"
./grant_permissions.sh
echo ""

# Launch the app
echo -e "${BLUE}🚀 Launching the app...${NC}"
adb shell am start -n $APP_ID/.MainActivity
echo -e "${GREEN}✅ App launched!${NC}"
echo ""

# Instructions for the user
echo -e "${YELLOW}===== Next Steps =====${NC}"
echo -e "1. On the Android device, tap ${BOLD}Start Mouse${NC}"
echo -e "2. Then tap ${BOLD}Start Advertising${NC}"
echo -e "3. On your Mac, go to System Preferences → Bluetooth"
echo -e "4. Look for ${BOLD}Android BLE Mouse${NC} in the list of devices"
echo -e "5. Connect to it when it appears"
echo ""
echo -e "${YELLOW}===== Troubleshooting =====${NC}"
echo -e "• If the device doesn't appear in the Mac's Bluetooth list:"
echo -e "  - Keep both the app screen and Mac's Bluetooth settings screen open"
echo -e "  - Try restarting the advertising in the app"
echo -e "  - Try using LightBlue Explorer app on Mac to verify the device is advertising"
echo -e "• If connection fails:"
echo -e "  - Make sure both Bluetooth and Location services are enabled on the Android device"
echo -e "  - Restart Bluetooth on your Mac"
echo -e "  - Try rebooting both devices"
echo ""

echo -e "${GREEN}✨ Setup complete! Follow the instructions above to connect your Mac.${NC}"
