#!/bin/bash
# Test the migration to the new namespace

echo "==== Testing migration to com.inventonater.blehid ===="

# Step 1: Build the migrated app
echo "Building the migrated app module..."
./gradlew :app:assembleDebug --console=plain || { echo "App build failed"; exit 1; }
echo "App build successful!"

# Step 2: Build the core and unity-plugin modules
echo "Building core and unity-plugin modules..."
./gradlew :core:assembleRelease :unity-plugin:assembleRelease --console=plain || { echo "Core/unity-plugin build failed"; exit 1; }
echo "Core and unity-plugin build successful!"

# Step 3: Build the Unity package
echo "Building Unity package..."
./build-unity-package.sh || { echo "Unity package build failed"; exit 1; }
echo "Unity package build successful!"

echo ""
echo "==== Migration Test Complete ===="
echo "All components built successfully using the new namespace."
echo ""
echo "Next steps:"
echo "1. Install the app on a device to verify functionality"
echo "2. Import the Unity package in a Unity project to verify integration"
echo "3. When you're confident everything works, you can delete the old namespace files:"
echo "   ./remove-legacy-code.sh"
echo ""
