#!/bin/bash

echo "Building Consolidated AAR Unity Plugin ==="
echo "Building unity-plugin module (with integrated core)..."

# Build only the unity-plugin module (core is now integrated)
./gradlew :unity-plugin:assembleRelease --console=plain || { echo "Build failed"; exit 1; }

echo "Unity plugin module built successfully"
echo "Copying consolidated AAR to Unity package..."

# Use the copy task from unity-plugin's build.gradle
./gradlew :unity-plugin:copyToUnity --console=plain || { echo "Failed to copy consolidated AAR"; exit 1; }

echo "Consolidated AAR copied successfully"
echo "Building Unity package..."

# Run the package build script
# cd com.inventonater.blehid
# ./package_build.sh || { echo "Package build failed"; exit 1; }
# cd ..

echo "=== Consolidated AAR Unity Plugin Build Complete ==="
