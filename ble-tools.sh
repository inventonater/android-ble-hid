#!/bin/bash

# ===========================================================================
# Inventonater HID Tools - Consolidated script for Inventonater HID project management
# ===========================================================================

# Terminal colors
RED="\033[0;31m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
BLUE="\033[0;34m"
BOLD="\033[1m"
NC="\033[0m" # No Color

APP_ID="com.inventonater.hid.app"

# ===========================================================================
# Common utility functions
# ===========================================================================

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

# Grant permissions needed for the app
grant_permissions() {
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

    # Grant Android 12+ specific permissions (required for all our supported devices)
    echo "🔒 Granting Bluetooth permissions:"
    grant_permission "android.permission.BLUETOOTH_ADVERTISE"
    grant_permission "android.permission.BLUETOOTH_CONNECT"
    grant_permission "android.permission.BLUETOOTH_SCAN"

    # Enable permission dialogs
    echo -n "🔄 Enabling permission dialogs: "
    adb shell appops set $APP_ID REQUEST_INSTALL_PACKAGES allow > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Done${NC}"
    else
        echo -e "${YELLOW}⚠️ Not supported on this Android version${NC}"
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

# ===========================================================================
# Main command functions
# ===========================================================================

# Show help information
show_help() {
    echo -e "${BOLD}Inventonater HID Tools - Android BLE HID project helper${NC}"
    echo "==============================================="
    echo ""
    echo "Usage: $0 [command] [options]"
    echo ""
    echo "Commands:"
    echo "  check         Check if device supports BLE peripheral mode"
    echo "  debug         Debug BLE advertising and connection issues"
    echo "    Options:"
    echo "      --enhanced  Run enhanced debugging with more tests"
    echo "  run           Build, install, and run the app"
    echo "  test          Test mouse connectivity"
    echo "  help          Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0 check           # Check if your device supports BLE peripheral mode"
    echo "  $0 debug           # Basic debugging for BLE issues"
    echo "  $0 debug --enhanced # Enhanced debugging with more tests"
    echo "  $0 run             # Build, install, and run the app"
    echo "  $0 test            # Test mouse connectivity"
    echo ""
}

# Check device BLE peripheral capability
cmd_check() {
    echo -e "${BOLD}===== BLE Peripheral Capability Checker =====${NC}"
    echo ""

    # Check device connection
    check_device_connected || exit 1
    
    # Get device info
    get_device_info
    
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

    # Run a quick test if API level is high enough and peripheral mode seems supported
    if [ "$PERIPHERAL_SUPPORTED" = true ] && [ "$SDK_VERSION" -ge 26 ]; then
        echo ""
        echo -e "${BLUE}🧪 Running quick BLE advertising test...${NC}"
        
        # Create a small test app using shell
        TEST_PKG="com.example.bletest"
        
        adb shell "am broadcast -a android.bluetooth.adapter.action.REQUEST_ENABLE"
        echo "  • Waiting for Bluetooth to fully initialize..."
        sleep 2
        
        # Use the Android Bluetooth API directly through adb shell to test advertising
        TEST_CMD="
        var BluetoothAdapter = android.bluetooth.BluetoothAdapter;
        var AdvertiseSettings = android.bluetooth.le.AdvertiseSettings;
        var AdvertiseData = android.bluetooth.le.AdvertiseData;
        var ParcelUuid = android.os.ParcelUuid;
        
        var adapter = BluetoothAdapter.getDefaultAdapter();
        var advertiser = adapter.getBluetoothLeAdvertiser();
        
        if (advertiser == null) {
            print('Advertiser null: peripheral mode NOT supported');
            false;
        } else {
            print('Advertiser available: peripheral mode IS supported');
            true;
        }
        "
        
        TEST_RESULT=$(adb shell "cmd activity exec-app $TEST_PKG android.util.jshell '$TEST_CMD'" 2>/dev/null)
        
        if [[ "$TEST_RESULT" == *"peripheral mode IS supported"* ]]; then
            echo -e "  • Dynamic test result: ${GREEN}✅ Peripheral mode confirmed working${NC}"
        elif [[ "$TEST_RESULT" == *"peripheral mode NOT supported"* ]]; then
            echo -e "  • Dynamic test result: ${RED}❌ Peripheral mode confirmed NOT working${NC}"
            PERIPHERAL_SUPPORTED=false
        else
            echo -e "  • Dynamic test result: ${YELLOW}⚠️ Test inconclusive (${TEST_RESULT:-no result})${NC}"
        fi
    fi

    echo ""
    echo -e "${BOLD}===== Summary =====${NC}"
    if [ "$PERIPHERAL_SUPPORTED" = true ]; then
        echo -e "${GREEN}✅ GOOD NEWS: This device ($MANUFACTURER $MODEL) likely supports BLE peripheral mode.${NC}"
        echo "   You should be able to use the Android BLE HID app on this device."
        echo ""
        echo "🔖 Next steps:"
        echo "   1. Run ./ble-tools.sh run to install and launch the app"
        echo "   2. Tap 'Start Mouse' then 'Start Advertising'"
        echo "   3. Check if your Mac can see the device in Bluetooth settings"
    else
        echo -e "${RED}❌ BAD NEWS: This device ($MANUFACTURER $MODEL) does NOT support BLE peripheral mode.${NC}"
        echo "   Unfortunately, this is a hardware limitation and you cannot use the Android BLE HID app."
        echo ""
        echo "💡 What you can do instead:"
        echo "   • Try a different Android device that supports BLE peripheral mode"
        echo "   • Consider using a dedicated BLE HID device (keyboard/mouse)"
        echo "   • Known good devices: Pixel 3 or newer, Samsung Galaxy S8 or newer"
    fi

    echo ""
    echo -e "${GREEN}✨ Capability check complete!${NC}"
}

# Debug BLE issues
cmd_debug() {
    ENHANCED=false
    if [ "$1" = "--enhanced" ]; then
        ENHANCED=true
        echo -e "${BOLD}===== Enhanced BLE HID Debugging Tool =====${NC}"
    else
        echo -e "${BOLD}===== Android BLE HID Debugging Tool =====${NC}"
    fi
    echo ""

    # Check device connection
    check_device_connected || exit 1
    
    # If enhanced, get more device info
    if [ "$ENHANCED" = true ]; then
        get_device_info
    
        # Clear logcat to ensure clean logs for debugging
        adb logcat -c
        echo "📋 Cleared logcat buffer"

        # Start logging in the background to capture events
        adb logcat > ble_hid_debug_log.txt &
        LOG_PID=$!
        echo "📝 Started logging to ble_hid_debug_log.txt (PID: $LOG_PID)"
    fi

    # Check Bluetooth status
    check_bluetooth_status

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
            echo "    ⚠️ Permission not granted!"
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
    adb logcat -d | grep -E "BleAdvert|BLE Advertising" | tail -15

    # Enhanced debugging features
    if [ "$ENHANCED" = true ]; then
        # Navigate to mouse activity if not already there
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
            echo -e "${GREEN}✅ Connected device found:${NC}"
            echo "$CONNECTED_DEVICES"
        else
            echo -e "${RED}❌ No connected devices detected in logs${NC}"
        fi

        echo ""
        echo "📊 Checking descriptor and characteristic activity..."
        DESCRIPTOR_ACTIVITY=$(adb logcat -d | grep -E "descriptor|characteristic|notification" | tail -10)
        if [ -n "$DESCRIPTOR_ACTIVITY" ]; then
            echo -e "${GREEN}✅ Descriptor/characteristic activity found:${NC}"
            echo "$DESCRIPTOR_ACTIVITY"
        else
            echo -e "${RED}❌ No descriptor/characteristic activity detected${NC}"
            echo "   This suggests the host is not configuring notifications or reading descriptors"
        fi

        echo ""
        echo "📊 Checking for mouse report attempts..."
        MOUSE_REPORTS=$(adb logcat -d | grep "Mouse report sent" | tail -5)
        if [ -n "$MOUSE_REPORTS" ]; then
            echo -e "${GREEN}✅ Mouse reports are being sent:${NC}"
            echo "$MOUSE_REPORTS"
        else
            echo -e "${RED}❌ No mouse reports detected${NC}"
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
            echo -e "${GREEN}✅ Mouse reports detected after manual input:${NC}"
            echo "$MOUSE_REPORTS_AFTER"
        else
            echo -e "${RED}❌ Still no mouse reports detected after manual input${NC}"
            echo "   This may indicate issues with HID report generation or GATT notification"
        fi

        echo ""
        echo "🔍 Checking GATT notifications..."
        NOTIFICATIONS=$(adb logcat -d | grep -E "Notification sent|notification" | tail -5)
        if [ -n "$NOTIFICATIONS" ]; then
            echo -e "${GREEN}✅ GATT notifications detected:${NC}"
            echo "$NOTIFICATIONS"
        else
            echo -e "${RED}❌ No GATT notifications detected${NC}"
            echo "   This suggests notifications are not being sent or logged"
        fi

        echo ""
        echo "💡 Checking HID descriptor read activity..."
        DESCRIPTOR_READS=$(adb logcat -d | grep -E "Read request for|descriptor|HID_REPORT" | tail -10)
        if [ -n "$DESCRIPTOR_READS" ]; then
            echo -e "${GREEN}✅ HID descriptor read activity detected:${NC}"
            echo "$DESCRIPTOR_READS"
        else
            echo -e "${RED}❌ No HID descriptor read activity detected${NC}"
            echo "   This suggests the host may not be reading HID descriptors properly"
        fi

        echo ""
        echo -e "${YELLOW}⚠️ Detailed Diagnosis:${NC}"

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
    else
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
        echo "📊 For more detailed diagnostics:"
        echo "  Run: ./ble-tools.sh debug --enhanced"
    fi

    echo ""
    echo -e "${GREEN}✨ Debug complete!${NC}"
}

# Build, install, and run the app
cmd_run() {
    echo -e "${BOLD}===== Android BLE HID App Runner =====${NC}"
    echo ""

    # Check device connection
    check_device_connected || exit 1

    # Build and install the app
    echo -e "${BLUE}🔨 Building and installing the app...${NC}"
    ./gradlew installDebug

    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ Build failed. Please check for errors.${NC}"
        exit 1
    fi

    echo -e "${GREEN}✅ App installed successfully!${NC}"
    echo ""

    # Run the grant permissions function
    grant_permissions
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
}

# Test mouse connectivity
cmd_test() {
    echo -e "${BOLD}BLE HID Mouse Connectivity Test${NC}"
    echo "==============================="
    echo "This script will help diagnose BLE HID mouse connectivity issues"
    echo "and provide diagnostics for troubleshooting."
    echo ""

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
            if $ADB shell pm list packages | grep -q "com.inventonater.hid"; then
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
}

# ===========================================================================
# Main script logic
# ===========================================================================

# If no arguments, show help
if [ $# -eq 0 ]; then
    show_help
    exit 0
fi

# Parse first argument as command
COMMAND=$1
shift

case "$COMMAND" in
    check)
        cmd_check "$@"
        ;;
    debug)
        cmd_debug "$@"
        ;;
    run)
        cmd_run "$@"
        ;;
    test)
        cmd_test "$@"
        ;;
    help)
        show_help
        ;;
    *)
        echo -e "${RED}Error: Unknown command '$COMMAND'${NC}"
        show_help
        exit 1
        ;;
esac
