# Modern Android Permissions for BLE HID

This document outlines the simplified permission model used in our BLE HID library now that we target modern Android devices (Android 12+) exclusively.

## Simplified Permission Model

As of our update to target only Android 12 (API level 31) and above, we've simplified the permission model to remove legacy permissions and focus only on the new Bluetooth permissions introduced in Android 12.

### Required Permissions

Our app now requires only these permissions:

```xml
<uses-permission android:name="android.permission.BLUETOOTH_ADVERTISE" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
```

- **BLUETOOTH_ADVERTISE**: Allows the app to advertise as a BLE peripheral device
- **BLUETOOTH_CONNECT**: Allows the app to connect to paired Bluetooth devices and accept connections
- **BLUETOOTH_SCAN**: Allows the app to discover and pair Bluetooth devices

We use the `neverForLocation` flag on BLUETOOTH_SCAN to indicate that we don't use scanning for location purposes, which can improve the user experience during permission requests.

### Removed Legacy Permissions

The following legacy permissions are no longer required and have been removed:

```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

These permissions were required for different Android versions:
- BLUETOOTH and BLUETOOTH_ADMIN for all pre-Android 12 devices
- ACCESS_FINE_LOCATION for Android 6.0 - 11 due to BLE scanning giving location information

## Permission Handling

### Runtime Permission Requests

In the `MainActivity`, we now request only the modern permissions:

```kotlin
private fun getRequiredPermissions(): List<String> {
    val permissionsToRequest = mutableListOf<String>()
    
    // Check for Android 12+ permissions
    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
            != PackageManager.PERMISSION_GRANTED) {
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    
    // Check for other required permissions...
    
    return permissionsToRequest
}
```

### Testing Tool Support

Our `ble-tools.sh` script has been updated to grant only the modern permissions:

```bash
# Grant Android 12+ specific permissions
echo "🔒 Granting Bluetooth permissions:"
grant_permission "android.permission.BLUETOOTH_ADVERTISE"
grant_permission "android.permission.BLUETOOTH_CONNECT"
grant_permission "android.permission.BLUETOOTH_SCAN"
```

## BLE Framework Impact

The simplified permission model has several advantages:

1. **Cleaner Code**: Removed conditional permission logic based on Android version
2. **Better Security**: Explicit permission declarations make security requirements clearer
3. **Improved User Experience**: More specific permissions that clearly communicate what the app needs
4. **Fewer Edge Cases**: No special handling for different Android versions

## Unity Integration

When using the Unity plugin, the same permissions are required. The Unity plugin obtains these permissions through its Android manifest merging process.

## Troubleshooting

If you encounter permission-related issues:

1. Verify that all three permissions are declared in the manifest
2. Check that runtime permission requests are being made in MainActivity
3. For testing, you can use `ble-tools.sh run` which will automatically grant the required permissions
4. Use `adb shell pm grant <package> <permission>` to manually grant permissions if needed
