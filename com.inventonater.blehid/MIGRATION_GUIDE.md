# Migration Guide

This guide will help you migrate from the older unity-test project approach to the new Unity Package Manager (UPM) package.

## Why Migrate?

The new `com.inventonater.blehid` package offers several advantages:

- Easier installation and updates through Unity's Package Manager
- Cleaner project structure following Unity's package conventions
- Better isolation from your project's code
- Simplified version management

## Migration Steps

### 1. Remove Old Files

If you have already integrated the BLE HID functionality using the `unity-test` approach, you should first remove the existing files from your project:

1. Delete the `Assets/BleHidTest` directory if it exists in your project
2. Delete the `Assets/Plugins/Android/BleHidPlugin.aar` and `Assets/Plugins/Android/BleHidCore.aar` files

### 2. Install the Package

You can install the package using one of these methods:

#### Option A: Using the .tgz Package File

1. Download the `com.inventonater.blehid-1.0.0.tgz` package file
2. In Unity, go to Window > Package Manager
3. Click the "+" button in the top-left corner
4. Choose "Add package from tarball..."
5. Select the downloaded .tgz file

#### Option B: Using Git URL (if hosted in a Git repository)

1. In Unity, go to Window > Package Manager
2. Click the "+" button in the top-left corner
3. Choose "Add package from git URL..."
4. Enter the Git URL for the package: `https://github.com/yourusername/android-ble-hid.git?path=com.inventonater.blehid`

#### Option C: Using a Local Folder Reference

If you have the package locally (for development or testing), you can reference it directly:

1. Open your project's `Packages/manifest.json` file
2. Add a reference to the local package:
   ```json
   {
     "dependencies": {
       "com.inventonater.blehid": "file:/path/to/com.inventonater.blehid",
       ...
     }
   }
   ```
3. For relative paths, use:
   ```json
   "com.inventonater.blehid": "file:../../com.inventonater.blehid"
   ```
   (This assumes the package is two directory levels up from the Packages folder)

### 3. Update Your Code

The package contains the same functionality as the old approach, but with a different namespace. You'll need to update your code:

#### Namespace Changes

- Old: `com.example.blehid.*`
- New: `com.inventonater.blehid.*`

#### C# Script References

Update your C# scripts to use the new namespace and reference the package classes:

```csharp
// Old
using BleHidTest;

// New
using Inventonater.BleHid;
```

#### GameObject Names

If you were referencing specific GameObject names in your code (like the BleHidManager), make sure to update those references.

### 4. Understanding Native Plugin Locations

One important change with the package approach is where the native plugins (AAR files) are stored:

- **Old approach**: Files were placed in your project's `Assets/Plugins/Android/` directory
- **New approach**: Files are included in the package's `Runtime/Plugins/Android/` directory

The key advantage is that you no longer need to manage these files directly in your project. When you import the package, Unity automatically recognizes and includes these plugin files during the build process.

### 5. Android Manifest Settings

**New in version 1.0.0**: The package now includes its own `AndroidManifest.xml` with all required Bluetooth permissions. You no longer need to manually add these permissions to your project's manifest.

The package's manifest includes:
- `BLUETOOTH`, `BLUETOOTH_ADMIN` (for Android 11 and below)
- `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (for Android 11 and below)
- `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` (for Android 12+)
- Required Bluetooth feature declarations

These permissions will be automatically merged into your final application manifest during the build process. If your project has its own `AndroidManifest.xml`, you can safely remove any Bluetooth-related permissions from it to avoid duplication.

If you previously had a custom `gradle.properties` file in your `Assets/Plugins/Android` directory with Bluetooth-related settings, you can safely remove it as well. The package now handles all necessary Bluetooth configurations.

#### Verifying The Manifest Merging

To verify that the Bluetooth permissions are correctly being included in your final build:

1. After building your Android APK, you can examine the final `AndroidManifest.xml` using Android's APK Analyzer:
   - In Android Studio, select "Build > Analyze APK..." and select your built APK
   - Navigate to "AndroidManifest.xml" in the APK Analyzer
   - Verify that all Bluetooth permissions are present

2. Alternatively, you can use the `aapt` tool from the Android SDK:
   ```bash
   aapt dump xmltree YOUR_APP.apk AndroidManifest.xml | grep -i bluetooth
   ```

3. If the permissions aren't appearing in your final build, see the troubleshooting section in the README.md for solutions.

#### Manual Permission Setup (If Needed)

In rare cases where manifest merging doesn't work correctly with your specific Unity version or configuration, you can manually copy the manifest to your project:

1. Copy the `AndroidManifest.xml` file from:
   `Packages/com.inventonater.blehid/Runtime/Plugins/Android/AndroidManifest.xml`
   
2. Paste it into your project's:
   `Assets/Plugins/Android/AndroidManifest.xml`
   
3. If you already have an AndroidManifest.xml file, merge the permission entries from our file into yours.

### 6. Testing

After migration, test your Bluetooth functionality to ensure everything works as expected:

1. Test device discovery and connection
2. Test all HID functions (keyboard, mouse, media controls)
3. Test connection stability and edge cases

## Troubleshooting

If you encounter issues during migration:

- Check the Unity console for error messages
- Ensure all references to old namespaces have been updated
- Verify Android manifest settings and permissions
- Make sure the AAR files are properly included in the build (they should be in the package's `Runtime/Plugins/Android` directory)

## Need Help?

If you encounter any issues during migration, please contact support at support@inventonater.com or open an issue in the GitHub repository.
