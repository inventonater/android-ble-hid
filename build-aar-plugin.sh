#!/bin/bash

echo "Building AAR Unity Plugin Components ==="
echo "Building only core and unity-plugin modules..."

# Build the core and unity-plugin modules but not the app
./gradlew :core:assembleRelease :unity-plugin:assembleRelease --console=plain || { echo "Build failed"; exit 1; }

echo "Core and unity-plugin modules built successfully"
echo "Copying AAR files to Unity package..."

# Use the copy task from unity-plugin's build.gradle
./gradlew :unity-plugin:copyToUnity --console=plain || { echo "Failed to copy AAR files"; exit 1; }

echo "AAR files copied successfully"
echo "Building Unity package..."

# Run the package build script
# cd com.inventonater.blehid
# ./package_build.sh || { echo "Package build failed"; exit 1; }
# cd ..

echo "=== AAR Unity Plugin Build Complete ==="
