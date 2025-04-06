#!/bin/bash
# Script to ensure Git recognizes AAR file changes
# This adds a timestamp to the AAR file after building

AAR_FILE="$(realpath ../../unity-test/Assets/Plugins/Android/BleHidPlugin.aar)"

if [ -f "$AAR_FILE" ]; then
    echo "Stamping AAR file with timestamp to ensure Git recognizes changes"
    # Create a temporary file with timestamp
    TEMP_FILE=$(mktemp)
    echo "Build timestamp: $(date)" > $TEMP_FILE
    
    # Add the timestamp file to the AAR (which is just a ZIP file)
    zip -u "$AAR_FILE" $TEMP_FILE
    
    # Clean up
    rm $TEMP_FILE
    
    echo "AAR file successfully stamped"
else
    echo "Error: AAR file not found at $AAR_FILE"
    exit 1
fi
