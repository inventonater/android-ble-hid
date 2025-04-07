#!/bin/bash
# Remove legacy code in the old namespace

echo "==== Removing legacy com.example code ===="
echo "This script will permanently remove old namespace files that have been migrated to com.inventonater"

# Check if the user wants to proceed
read -p "Are you sure you want to proceed? (y/n) " -n 1 -r
echo    # move to a new line
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "Operation cancelled"
    exit 1
fi

# Remove unity-plugin old namespace files
echo "Removing unity-plugin old namespace files..."
if [ -d "unity-plugin/src/main/java/com/example" ]; then
    rm -rf unity-plugin/src/main/java/com/example
    echo "Removed unity-plugin/src/main/java/com/example"
else
    echo "unity-plugin/src/main/java/com/example not found (already removed)"
fi

# Remove core old namespace files 
echo "Removing core old namespace files..."
if [ -d "core/src/main/java/com/example" ]; then
    rm -rf core/src/main/java/com/example
    echo "Removed core/src/main/java/com/example"
else
    echo "core/src/main/java/com/example not found (already removed)"
fi

# App module files 
echo "Removing app module old namespace files..."
read -p "Do you want to remove app module legacy files too? This should only be done after confirming the migrated app works. (y/n) " -n 1 -r
echo    # move to a new line
if [[ $REPLY =~ ^[Yy]$ ]]
then
    if [ -d "app/src/main/java/com/example" ]; then
        rm -rf app/src/main/java/com/example
        echo "Removed app/src/main/java/com/example"
    else
        echo "app/src/main/java/com/example not found (already removed)"
    fi
else
    echo "Keeping app module legacy files for now"
fi

echo "==== Legacy code removal complete ===="
echo "All com.example namespace files have been removed from the core and unity-plugin modules."
echo "App module is now using the new namespace: com.inventonater.blehid.app"
