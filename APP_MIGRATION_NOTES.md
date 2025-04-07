# App Module Migration Guide

This document outlines how to update the app module to use the new `com.inventonater.blehid` namespace instead of the legacy `com.example.blehid` namespace.

## Current Status

The core and unity-plugin modules have been fully migrated to the new namespace structure:
- `com.inventonater.blehid.core.*`
- `com.inventonater.blehid.unity.*`

However, the app module still uses the old namespace:
- `com.example.blehid.app.*` 
- And imports from `com.example.blehid.core.*`

Since the app module is just for testing and not part of the Unity package, it hasn't been migrated yet.

## Migration Steps

If you want to update the app module:

1. **Update package declarations**:
   - Change all `package com.example.blehid.app;` to `package com.inventonater.blehid.app;`
   - Update all sub-packages accordingly (ui, etc.)

2. **Update imports**:
   - Replace all imports of `com.example.blehid.core.*` with `com.inventonater.blehid.core.*`
   - Update all internal imports to use the new app package structure

3. **Move Java files**:
   - Move all Java files from `app/src/main/java/com/example/blehid/app/` to `app/src/main/java/com/inventonater/blehid/app/`
   - Ensure directory structure matches the package declaration

4. **Update AndroidManifest.xml**:
   - Update package name in AndroidManifest.xml
   - Update any explicit activity class names that include the full package

5. **Update build.gradle**:
   - Modify the namespace in app/build.gradle to `com.inventonater.blehid.app`

## Files to Update

The following files need imports updated:

- `MainActivity.java`
- UI Package:
  - `MousePanelManager.java`
  - `KeyboardPanelManager.java`
  - `MediaPanelManager.java`
  - `TabManager.java`
  - `DiagnosticsManager.java`
  - `SimpleMediaActivity.java`
  - `LogManager.java`
  - `BluetoothReceiver.java`

## Command Line Migration

You could use these commands to perform the migration:

```bash
# Create new directory structure
mkdir -p app/src/main/java/com/inventonater/blehid/app
mkdir -p app/src/main/java/com/inventonater/blehid/app/ui

# Move files to new locations
mv app/src/main/java/com/example/blehid/app/*.java app/src/main/java/com/inventonater/blehid/app/
mv app/src/main/java/com/example/blehid/app/ui/*.java app/src/main/java/com/inventonater/blehid/app/ui/

# Update package and import statements in all Java files
find app/src/main/java/com/inventonater/blehid/app -name "*.java" -exec sed -i '' 's/package com.example.blehid/package com.inventonater.blehid/g' {} \;
find app/src/main/java/com/inventonater/blehid/app -name "*.java" -exec sed -i '' 's/import com.example.blehid/import com.inventonater.blehid/g' {} \;

# Update the AndroidManifest.xml
sed -i '' 's/package="com.example.blehid.app"/package="com.inventonater.blehid.app"/g' app/src/main/AndroidManifest.xml

# Update build.gradle
sed -i '' 's/namespace "com.example.blehid.app"/namespace "com.inventonater.blehid.app"/g' app/build.gradle
```

## Note

This migration is optional. The app module can continue to function with the old namespace since it's not part of the Unity package. The build script has been updated to only use the new namespace files for the Unity package build.
