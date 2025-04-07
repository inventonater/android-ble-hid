#!/bin/bash

# Script to build only the Unity package components, excluding the app module
# This avoids the app resource errors when building for the Unity package

echo "=== Building Unity Package Components ==="
echo "Building only core and unity-plugin modules..."

# Build only the required modules
./gradlew :core:clean :core:assembleRelease :unity-plugin:clean :unity-plugin:assembleRelease

# Check if the build was successful
if [ $? -ne 0 ]; then
    echo "Error: Build failed"
    exit 1
fi

echo "Core and unity-plugin modules built successfully"

# Copy the AAR files to the Unity package
echo "Copying AAR files to Unity package..."
./gradlew :unity-plugin:copyToPackage

# Check if copying was successful
if [ $? -ne 0 ]; then
    echo "Error: Failed to copy AAR files to package"
    exit 1
fi

echo "AAR files copied successfully"

# Build the Unity package
echo "Building Unity package..."
cd com.inventonater.blehid
./package_build.sh

# Return to the original directory
cd ..

echo "=== Unity Package Build Complete ==="
echo "You can find the package at: com.inventonater.blehid/com.inventonater.blehid-1.0.0.tgz"
