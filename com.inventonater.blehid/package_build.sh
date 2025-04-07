#!/bin/bash

# Script to build a Unity package from the com.inventonater.blehid directory
# This creates a .tgz file that can be imported using the Unity Package Manager

# Ensure we're in the correct directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Get version from package.json
VERSION=$(grep -o '"version": *"[^"]*"' package.json | cut -d'"' -f4)
PACKAGE_NAME="com.inventonater.blehid"
OUTPUT_FILE="${PACKAGE_NAME}-${VERSION}.tgz"

echo "Building $PACKAGE_NAME version $VERSION..."

# Ensure the Android plugin directory exists
mkdir -p Runtime/Plugins/Android

# Check if AAR files exist
if [ ! -f "Runtime/Plugins/Android/BleHidPlugin.aar" ] || [ ! -f "Runtime/Plugins/Android/BleHidCore.aar" ]; then
    echo "Warning: AAR files not found in Runtime/Plugins/Android/"
    echo "You may need to build the Android plugin first using:"
    echo "  ./gradlew :unity-plugin:copyToUnity"
    
    # Ask if we should continue
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Build cancelled."
        exit 1
    fi
fi

# Create a temporary directory for package creation
TMP_DIR=$(mktemp -d)
echo "Using temporary directory: $TMP_DIR"

# Copy the package contents to the temp directory
cp -r package.json README.md MIGRATION_GUIDE.md Runtime Editor "$TMP_DIR"

# Create the tgz package
echo "Creating package..."
cd "$TMP_DIR"
npm pack

# Move the created package to the original directory
mv "$OUTPUT_FILE" "$SCRIPT_DIR"

# Clean up
cd "$SCRIPT_DIR"
rm -rf "$TMP_DIR"

echo "Package created: $OUTPUT_FILE"
echo "You can now import this package in Unity using the Package Manager > + > Add package from tarball"
