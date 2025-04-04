#!/bin/bash

# =============================================================================
# test-mouse.sh - Test mouse connectivity and movement
# =============================================================================
# This script helps test mouse movement functionality and verify the HID
# connection is working properly with the host device.

# Terminal colors
RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
BLUE="\033[0;34m"
BOLD="\033[1m"
NC="\033[0m" # No Color

APP_ID="com.inventonater.blehid.app"

echo -e "${BOLD}===== BLE HID Mouse Connectivity Test =====${NC}"
echo "This script helps test and diagnose mouse functionality"
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

# Check if the app is running
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

# Check for connected host devices
echo -n "🔍 Checking for connected devices: "
CONNECTED_DEVICES=$(adb logcat -d | grep -E "Device connected|CONNECTION_STATE_CONNECTED" | tail -5)
if [ -n "$CONNECTED_DEVICES" ]; then
    echo -e "${GREEN}✅ Found connected device${NC}"
    echo "   $CONNECTED_DEVICES"
else
    echo -e "${YELLOW}⚠️ No connected devices found${NC}"
    echo "   Please make sure your device is paired and connected to a host"
    echo "   You can run ./developer-tools/debug-connection.sh for assistance"
    
    read -p "Would you like to continue without a connected device? (y/n): " CONTINUE
    if [[ $CONTINUE != "y" && $CONTINUE != "Y" ]]; then
        echo "Exiting test. Please connect a host device first."
        exit 0
    fi
fi

# Clear logcat to ensure clean logs for testing
adb logcat -c
echo "📋 Cleared logcat buffer for testing"

# Start automated mouse movement tests
echo ""
echo -e "${BLUE}===== Starting Mouse Movement Tests =====${NC}"

# Testing a circle movement pattern
echo "⚙️ Test 1: Moving mouse in a circular pattern..."
for i in {1..36}; do
    # Calculate x, y coordinates for a circle
    X=$(echo "s($i*10*3.14159/180)*50" | bc -l | xargs printf "%.0f")
    Y=$(echo "c($i*10*3.14159/180)*50" | bc -l | xargs printf "%.0f")
    
    echo -n "  Moving to point ($X, $Y)..."
    
    # Use adb shell input tap to simulate a touch at this position
    adb shell input tap $((X + 400)) $((Y + 400))
    
    # Check logs for mouse reports
    REPORT_FOUND=$(adb logcat -d | grep -E "Mouse|report|movement" | tail -1)
    if [ -n "$REPORT_FOUND" ]; then
        echo -e "${GREEN}✅${NC}"
    else
        echo -e "${RED}❌ No HID report detected${NC}"
    fi
    
    # Small delay between movements
    sleep 0.2
done

# Test mouse buttons
echo ""
echo "⚙️ Test 2: Testing mouse buttons..."

# Left button
echo -n "  Left button click..."
adb shell input tap 300 500
sleep 0.5
LEFT_CLICK=$(adb logcat -d | grep -E "button|click|left" | tail -1)
if [ -n "$LEFT_CLICK" ]; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌ No left button event detected${NC}"
fi

# Right button (if available)
echo -n "  Right button click..."
adb shell input tap 500 500
sleep 0.5
RIGHT_CLICK=$(adb logcat -d | grep -E "button|click|right" | tail -1)
if [ -n "$RIGHT_CLICK" ]; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌ No right button event detected${NC}"
fi

# Test for scrolling (if available)
echo -n "  Scroll wheel..."
adb shell input swipe 400 300 400 400 300
sleep 0.5
SCROLL=$(adb logcat -d | grep -E "scroll|wheel" | tail -1)
if [ -n "$SCROLL" ]; then
    echo -e "${GREEN}✅${NC}"
else
    echo -e "${RED}❌ No scroll event detected${NC}"
fi

# Analyze overall performance
echo ""
echo -e "${BLUE}===== Test Results Analysis =====${NC}"

# Count total events
TOTAL_EVENTS=$(adb logcat -d | grep -E "Mouse|report|click|scroll|movement" | wc -l)
echo "📊 Total HID events detected: $TOTAL_EVENTS"

if [ "$TOTAL_EVENTS" -gt 10 ]; then
    echo -e "${GREEN}✅ Mouse HID functionality appears to be working properly${NC}"
elif [ "$TOTAL_EVENTS" -gt 0 ]; then
    echo -e "${YELLOW}⚠️ Mouse HID functionality is working but may have issues${NC}"
else
    echo -e "${RED}❌ No mouse HID events detected - functionality is not working${NC}"
fi

# Gather feedback on host device behavior
echo ""
echo -e "${BLUE}===== Host Device Feedback =====${NC}"
echo "Please check if the mouse cursor on your host device moved in a circular pattern"
echo "and responded to button clicks and scrolling:"

echo ""
echo "1. Did you see mouse pointer movement on the host? (yes/no)"
read MOVEMENT_OBSERVED

echo "2. Did mouse clicks register on the host? (yes/no)"
read CLICKS_OBSERVED

echo "3. Did scrolling register on the host? (yes/no)"
read SCROLL_OBSERVED

echo ""
echo -e "${BLUE}===== Final Diagnosis =====${NC}"

if [[ "$MOVEMENT_OBSERVED" == "yes" && "$CLICKS_OBSERVED" == "yes" ]]; then
    echo -e "${GREEN}✅ Mouse functionality is working properly!${NC}"
    echo "   Both logs and observed behavior confirm HID mouse is functioning"
elif [[ "$TOTAL_EVENTS" -gt 10 && "$MOVEMENT_OBSERVED" != "yes" ]]; then
    echo -e "${YELLOW}⚠️ Mixed results: Events are being sent but not registering on host${NC}"
    echo "   Possible issues:"
    echo "   • Host not properly interpreting HID reports"
    echo "   • HID descriptor format not compatible with host"
    echo "   • Connection is established but data transfer has issues"
else
    echo -e "${RED}❌ Mouse functionality is not working properly${NC}"
    echo "   Please run ./developer-tools/debug-connection.sh --enhanced for detailed diagnostics"
fi

echo ""
echo -e "${GREEN}✨ Mouse connectivity test complete!${NC}"
