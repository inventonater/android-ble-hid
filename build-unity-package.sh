#!/bin/bash
# Script to build the Unity package

echo "Building Unity Package Components ==="
echo "Building only core and unity-plugin modules..."

# Remove any remnants of old namespace files before building
echo "Checking for legacy namespace files..."
if [ -d "unity-plugin/src/main/java/com/example" ]; then
    echo "Removing unity-plugin/src/main/java/com/example..."
    rm -rf unity-plugin/src/main/java/com/example
    echo "Removed."
else
    echo "No legacy files found in unity-plugin (already cleaned)."
fi

if [ -d "core/src/main/java/com/example" ]; then
    echo "Removing core/src/main/java/com/example..."
    rm -rf core/src/main/java/com/example
    echo "Removed."
else
    echo "No legacy files found in core (already cleaned)."
fi
echo "Legacy namespace files check complete."

# Build the core and unity-plugin modules but not the app
./gradlew :core:assembleRelease :unity-plugin:assembleRelease --console=plain || { echo "Build failed"; exit 1; }

echo "Core and unity-plugin modules built successfully"
echo "Copying AAR files to Unity package..."

# Use the copy task from unity-plugin's build.gradle
./gradlew :unity-plugin:copyToUnity --console=plain || { echo "Failed to copy AAR files"; exit 1; }

echo "AAR files copied successfully"
echo "Building Unity package..."

# Run the package build script
cd com.inventonater.blehid
./package_build.sh || { echo "Package build failed"; exit 1; }

# Return to the original directory
cd ..

echo "=== Unity Package Build Complete ==="
echo "You can find the package at: com.inventonater.blehid/com.inventonater.blehid-1.0.0.tgz"
