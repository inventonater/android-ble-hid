#!/bin/bash

echo "Building Consolidated AAR Unity Plugin ==="
echo "Building unity-plugin module (with integrated core)..."

# Change to unity-plugin directory where the gradle files now live
cd unity-plugin || { echo "Failed to change to unity-plugin directory"; exit 1; }

# Make gradlew executable and ensure it has proper permissions
chmod +x ./gradlew

# Use the local gradlew script to build the project
./gradlew clean assembleRelease --info --stacktrace || { echo "Build failed"; exit 1; }

echo "Unity plugin module built successfully"
echo "Creating plugin directory if it doesn't exist..."
mkdir -p ../com.inventonater.blehid/Runtime/Plugins/Android

echo "Copying consolidated AAR to Unity package..."
cp build/outputs/aar/blehid-unity-plugin-release.aar ../com.inventonater.blehid/Runtime/Plugins/Android/BleHid.aar || { echo "Failed to copy AAR file"; exit 1; }

# Add timestamp to AAR for Git tracking
echo "Adding timestamp to AAR file..."
TIMESTAMP=$(date)
TEMP_FILE=$(mktemp)
echo "Build timestamp: $TIMESTAMP - Consolidated AAR with core and unity plugin" > $TEMP_FILE
zip -u ../com.inventonater.blehid/Runtime/Plugins/Android/BleHid.aar $TEMP_FILE || { echo "Failed to update AAR with timestamp"; exit 1; }
rm $TEMP_FILE

echo "Consolidated AAR copied and stamped successfully"

# Return to the root directory
cd ..

echo "Consolidated AAR copied successfully"
echo "Building Unity package..."

# Run the package build script if needed
# cd com.inventonater.blehid
# ./package_to_tgz.sh || { echo "Package build failed"; exit 1; }
# cd ..

echo "=== Consolidated AAR Unity Plugin Build Complete ==="
