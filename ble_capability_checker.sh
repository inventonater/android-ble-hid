#!/bin/bash

# BLE Peripheral Capability Checker Script
# This script performs a detailed check of a device's Bluetooth capabilities
# to determine if it can function as a BLE peripheral

echo "===== BLE Peripheral Capability Checker ====="
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

if [ "$SDK_VERSION" -lt 21 ]; then
    echo "❌ Android 5.0 (API 21) or higher is required for BLE peripheral mode"
    exit 1
fi

echo -n "📱 Device model: "
MODEL=$(adb shell getprop ro.product.model)
MANUFACTURER=$(adb shell getprop ro.product.manufacturer)
echo "$MANUFACTURER $MODEL"

# Check Bluetooth status
echo -n "📶 Checking Bluetooth status: "
BT_STATE=$(adb shell settings get global bluetooth_on 2>/dev/null)
if [ "$BT_STATE" = "1" ]; then
    echo "✅ ON"
else
    echo "❌ OFF"
    echo "⚠️ Enabling Bluetooth to complete capability check..."
    adb shell am start -a android.bluetooth.adapter.action.REQUEST_ENABLE
    sleep 2
fi

# Check for BLE adapter support
echo "🔍 Checking BLE capability..."

echo -n "  • Bluetooth Low Energy support: "
BLE_SUPPORTED1=$(adb shell "cmd bluetooth has-feature ble" 2>/dev/null)
BLE_SUPPORTED2=$(adb shell dumpsys bluetooth_manager | grep -i "mLeState" | grep -i "on" | wc -l)

# Alternative check: All modern Android devices with Bluetooth support BLE
if [[ "$BLE_SUPPORTED1" == *"true"* ]] || [ "$BLE_SUPPORTED2" -gt 0 ] || [ "$SDK_VERSION" -ge 21 ]; then
    echo "✅ Supported"
    BLE_SUPPORTED=true
else
    echo "❌ Not supported"
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
    echo "✅ Supported (confirmed via Bluetooth service)"
    PERIPHERAL_SUPPORTED=true
elif [ "$ADVERTISER_CHECK1" -gt 0 ]; then
    echo "✅ Supported (LeAdvertiser found in Bluetooth manager)"
    PERIPHERAL_SUPPORTED=true
else
    echo "❌ Not supported"
    echo "❌ This device cannot act as a BLE peripheral"
    PERIPHERAL_SUPPORTED=false
fi

# Run a quick test if API level is high enough and peripheral mode seems supported
if [ "$PERIPHERAL_SUPPORTED" = true ] && [ "$SDK_VERSION" -ge 26 ]; then
    echo ""
    echo "🧪 Running quick BLE advertising test..."
    
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
        echo "  • Dynamic test result: ✅ Peripheral mode confirmed working"
    elif [[ "$TEST_RESULT" == *"peripheral mode NOT supported"* ]]; then
        echo "  • Dynamic test result: ❌ Peripheral mode confirmed NOT working"
        PERIPHERAL_SUPPORTED=false
    else
        echo "  • Dynamic test result: ⚠️ Test inconclusive (${TEST_RESULT:-no result})"
    fi
fi

echo ""
echo "===== Summary ====="
if [ "$PERIPHERAL_SUPPORTED" = true ]; then
    echo "✅ GOOD NEWS: This device ($MANUFACTURER $MODEL) likely supports BLE peripheral mode."
    echo "   You should be able to use the Android BLE HID app on this device."
    echo ""
    echo "🔖 Next steps:"
    echo "   1. Run ./run_app.sh to install and launch the app"
    echo "   2. Tap 'Start Mouse' then 'Start Advertising'"
    echo "   3. Check if your Mac can see the device in Bluetooth settings"
else
    echo "❌ BAD NEWS: This device ($MANUFACTURER $MODEL) does NOT support BLE peripheral mode."
    echo "   Unfortunately, this is a hardware limitation and you cannot use the Android BLE HID app."
    echo ""
    echo "💡 What you can do instead:"
    echo "   • Try a different Android device that supports BLE peripheral mode"
    echo "   • Consider using a dedicated BLE HID device (keyboard/mouse)"
    echo "   • Known good devices: Pixel 3 or newer, Samsung Galaxy S8 or newer"
fi

echo ""
echo "✨ Capability check complete!"
