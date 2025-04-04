#!/bin/bash

# =============================================================================
# test-debug-features.sh - Test Inventonater BLE HID debugging features
# =============================================================================
# This script helps verify that all debugging components are working correctly
# by automating UI interactions with the app's debug interface

# Source common functions
source "$(dirname "$0")/common.sh"

# Navigate to project root
cd "$(dirname "$0")/.."

echo -e "${BOLD}===== BLE HID Debug Features Test =====${NC}"
echo "This script will test all debug functions of the application"
echo ""

# Check if Android device is connected
check_device_connected || exit 1

echo "Building and installing app..."
./gradlew installDebug

if [ $? -ne 0 ]; then
  echo -e "${RED}Error: Failed to build and install the app.${NC}"
  exit 1
fi

echo -e "${GREEN}✅ App installed successfully.${NC}"

# Start the ModernHidActivity
echo "Launching ModernHidActivity..."
adb shell am start -n "$APP_ID/com.inventonater.blehid.ModernHidActivity"

echo "Waiting for app to start..."
sleep 3

# Start logging
LOG_INFO=$(start_logging)
LOG_FILE=$(echo $LOG_INFO | cut -d' ' -f1)
LOG_PID=$(echo $LOG_INFO | cut -d' ' -f2)

echo -e "${BLUE}===== Testing Debug Features =====${NC}"

# Test environment analysis
echo -n "Testing environment analysis... "
adb shell input tap 450 540  # Debug Options button position (approximate)
sleep 1
adb shell input tap 450 380  # Analyze Environment button position (approximate)
sleep 2
ENV_LOG=$(adb logcat -d | grep -E "Environment|Analysis|System|Device" | tail -5)
if [ -n "$ENV_LOG" ]; then
    echo -e "${GREEN}✅${NC}"
    echo "  $ENV_LOG" | head -1
else
    echo -e "${RED}❌${NC}"
fi

# Test HID descriptor analysis
echo -n "Testing HID descriptor analysis... "
adb shell input tap 450 540  # Debug Options button position
sleep 1
adb shell input tap 450 450  # Analyze HID Descriptor button position (approximate)
sleep 2
DESC_LOG=$(adb logcat -d | grep -E "Descriptor|HID_REPORT|Report Map" | tail -5)
if [ -n "$DESC_LOG" ]; then
    echo -e "${GREEN}✅${NC}"
    echo "  $DESC_LOG" | head -1
else
    echo -e "${RED}❌${NC}"
fi

# Test report statistics
echo -n "Testing report statistics... "
adb shell input tap 450 540  # Debug Options button position
sleep 1
adb shell input tap 450 520  # Show Report Statistics button position (approximate)
sleep 2
STATS_LOG=$(adb logcat -d | grep -E "Statistics|Report sent|count" | tail -5)
if [ -n "$STATS_LOG" ]; then
    echo -e "${GREEN}✅${NC}"
    echo "  $STATS_LOG" | head -1
else
    echo -e "${RED}❌${NC}"
fi

# Test verbose logging
echo -n "Testing verbose logging... "
adb shell input tap 450 540  # Debug Options button position
sleep 1
adb shell input tap 510 340  # Verbose Logging switch position (approximate)
sleep 1
VERBOSE_LOG=$(adb logcat -d | grep -E "Verbose logging|enabled|debug" | tail -2)
if [ -n "$VERBOSE_LOG" ]; then
    echo -e "${GREEN}✅${NC}"
    echo "  $VERBOSE_LOG" | head -1
else
    echo -e "${RED}❌${NC}"
fi

# Test file logging (requires permission)
echo -n "Testing file logging... "
adb shell input tap 450 540  # Debug Options button position
sleep 1
adb shell input tap 510 380  # File Logging switch position (approximate)
sleep 1
FILE_LOG=$(adb logcat -d | grep -E "File logging|enabled|storage" | tail -2)
if [ -n "$FILE_LOG" ]; then
    echo -e "${GREEN}✅${NC}"
    echo "  $FILE_LOG" | head -1
else
    echo -e "${RED}❌${NC}"
fi

# Test registering HID device
echo -n "Testing HID device registration... "
adb shell input tap 450 180  # Register HID Device button position (approximate)
sleep 3
REG_LOG=$(adb logcat -d | grep -E "register|HID Device|BluetoothHidDevice" | tail -5)
if [ -n "$REG_LOG" ]; then
    echo -e "${GREEN}✅${NC}"
    echo "  $REG_LOG" | head -1
else
    echo -e "${RED}❌${NC}"
fi

# Test mouse movement
echo -n "Testing mouse movement (if connected)... "
adb shell input tap 250 320  # Move Mouse button position (approximate)
sleep 3
MOUSE_LOG=$(adb logcat -d | grep -E "Mouse|movement|report" | tail -3)
if [ -n "$MOUSE_LOG" ]; then
    echo -e "${GREEN}✅${NC}"
    echo "  $MOUSE_LOG" | head -1
else
    echo -e "${YELLOW}⚠️ No response (may need connection)${NC}"
fi

# Test keyboard input
echo -n "Testing keyboard input (if connected)... "
adb shell input tap 250 400  # Type Text button position (approximate)
sleep 3
KB_LOG=$(adb logcat -d | grep -E "Keyboard|typing|key press" | tail -3)
if [ -n "$KB_LOG" ]; then
    echo -e "${GREEN}✅${NC}"
    echo "  $KB_LOG" | head -1
else
    echo -e "${YELLOW}⚠️ No response (may need connection)${NC}"
fi

# Test media control
echo -n "Testing media control (if connected)... "
adb shell input tap 650 400  # Play/Pause button position (approximate)
sleep 3
MEDIA_LOG=$(adb logcat -d | grep -E "Media|play|pause|control" | tail -3)
if [ -n "$MEDIA_LOG" ]; then
    echo -e "${GREEN}✅${NC}"
    echo "  $MEDIA_LOG" | head -1
else
    echo -e "${YELLOW}⚠️ No response (may need connection)${NC}"
fi

# Pull logs
echo ""
echo -e "${BLUE}===== Checking Log Files =====${NC}"
echo -n "Checking app log files... "
APP_LOGS=$(adb shell "run-as $APP_ID ls -l /data/data/$APP_ID/files" 2>/dev/null)
if [ -n "$APP_LOGS" ]; then
    echo -e "${GREEN}✅ Found app logs${NC}"
    echo "$APP_LOGS" | head -3
else
    echo -e "${YELLOW}⚠️ No app logs found${NC}"
fi

echo -n "Checking external storage logs... "
STORAGE_LOGS=$(adb shell "run-as $APP_ID ls -l /sdcard/Android/data/$APP_ID/files/hid_logs" 2>/dev/null)
if [ -n "$STORAGE_LOGS" ]; then
    echo -e "${GREEN}✅ Found external storage logs${NC}"
    echo "$STORAGE_LOGS" | head -3
else
    echo -e "${YELLOW}⚠️ No external storage logs found${NC}"
    echo "  You may need to grant storage permissions for file logging to work."
fi

# Stop logging
stop_logging "$LOG_PID" "$LOG_FILE"

echo ""
echo -e "${GREEN}✨ Debug features test completed!${NC}"
echo "Full log available at: $LOG_FILE"
