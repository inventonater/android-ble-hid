#!/bin/bash

# =============================================================================
# build-app.sh - Build the Android BLE HID app module
# =============================================================================
# This script focuses on building just the Android app component

echo "===== Building Android BLE HID App ====="

# Enable error reporting
set -e

# Clean the project first
echo "Cleaning app module..."
cd "$(dirname "$0")/.." # Navigate to project root
./gradlew :app:clean

# Build the app
echo "Building app module..."
./gradlew :app:assembleDebug

# Build output information
echo "===== Build completed successfully ====="
echo "APK location: app/build/outputs/apk/debug/app-debug.apk"

# Prompt for installation
echo ""
read -p "Would you like to install the app on a connected device? (y/n): " INSTALL_CHOICE
if [[ $INSTALL_CHOICE == "y" || $INSTALL_CHOICE == "Y" ]]; then
    echo "Installing app on connected device..."
    ./gradlew :app:installDebug
    echo "Installation complete."
fi
