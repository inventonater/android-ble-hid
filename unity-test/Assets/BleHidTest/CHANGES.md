# BleHidPlugin Changes

## Fixes for Duplicate Class Issues

The following changes were made to address the `Duplicate class com.unity3d.player.UnityPlayer` issue:

1. **Removed UnityPlayer.java**:
   - Deleted the original UnityPlayer.java file that was conflicting with Unity's built-in version
   - Created a stub version in a different package (`com.example.blehid.unity.stubs.UnityPlayerStub`)

2. **Updated UnityEventBridge**:
   - Changed imports to use the stub class instead of the real Unity class
   - Modified the code to call the stub methods that will be replaced at runtime

3. **Modified Build Settings**:
   - Changed namespace to `com.example.blehid.plugin` to avoid package conflicts
   - Added ProGuard rules to remove Unity classes from the AAR
   - Set `minifyEnabled` to true to enable ProGuard/R8 processing
   - Added explicit exclusions for Unity classes in the packagingOptions

4. **Added Dependency Configuration**:
   - Used compileOnly for Unity dependencies to prevent them from being included in the AAR
   - Added explicit Kotlin dependency versions to ensure compatibility

## Additional Fixes

### Android Manifest Structure

We updated the AndroidManifest.xml to properly follow Unity's manifest requirements:

- Changed the package name to `com.example.blehid.unity`
- Added proper application configuration with hardcoded labels instead of resource references
- Fixed the activity element structure and attributes
- Added LEANBACK_LAUNCHER category for TV support
- Set proper SDK version requirements for Bluetooth permissions
- Organized permissions by Android version compatibility

### Android Resources

Removed direct Android resources in favor of including them in the AAR file:
- Removed `res/values/strings.xml` and associated meta files
- Updated manifest to use hardcoded strings instead of resource references

## How to Test

1. The modified AAR has been placed in the Assets/Plugins/Android directory
2. In Unity Editor, go to File > Build Settings
3. Select Android as the target platform
4. Click "Build" or "Build And Run"

The app should now build successfully without any duplicate class errors and launch properly on the device.
