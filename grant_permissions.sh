#!/bin/bash

# Android BLE HID app permissions granting script
# This script automatically grants all required permissions for the BLE HID app

APP_ID="com.example.blehid.app"

echo "===== BLE HID Permissions Helper ====="

# Check for connected devices
DEVICE_CONNECTED=$(adb devices | grep -v "List" | grep -v "^$" | wc -l)
if [ "$DEVICE_CONNECTED" -eq 0 ]; then
    echo "❌ Error: No Android devices connected via ADB"
    echo "Please connect your device and enable USB debugging"
    exit 1
fi

echo "📱 Found connected Android device"
echo "🔐 Setting up Bluetooth permissions..."

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
        echo "✅ Granted"
    else
        if [[ "$RESULT" == *"not a changeable permission type"* ]]; then
            echo "⚠️ Not a runtime permission (normal)"
        elif [[ "$RESULT" == *"has not requested permission"* ]]; then
            echo "⚠️ Not declared in manifest"
        else
            echo "❌ Failed: $RESULT"
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

# Enable permission dialogs
echo -n "🔄 Enabling permission dialogs: "
adb shell appops set $APP_ID REQUEST_INSTALL_PACKAGES allow > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Done"
else
    echo "⚠️ Not supported on this Android version"
fi

# Enable Bluetooth if not already enabled
echo -n "📶 Ensuring Bluetooth is enabled: "
adb shell am broadcast -a android.bluetooth.adapter.action.REQUEST_ENABLE > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "✅ Requested"
else
    echo "⚠️ Could not request Bluetooth enable"
fi

# Check if Bluetooth is enabled
BT_STATE=$(adb shell settings get global bluetooth_on 2>/dev/null)
if [ "$BT_STATE" = "1" ]; then
    echo "📶 Bluetooth is enabled: ✅"
else
    echo "📶 Bluetooth status: ⚠️ Might be disabled, check your device"
fi

echo ""
echo "✨ Permission setup complete!"
echo ""
echo "💡 If your app doesn't appear in the Mac's Bluetooth menu, try these steps:"
echo "  1. Make sure the app is running and 'Start Advertising' is active"
echo "  2. On your Mac, go to System Preferences → Bluetooth"
echo "  3. Keep the Bluetooth panel open while the app is advertising"
echo "  4. Try LightBlue Explorer app on Mac to verify BLE advertising is working"
echo "  5. Make sure your Mac's Bluetooth is turned on and set to discoverable"
echo "  6. Restart the Android app after granting permissions"
