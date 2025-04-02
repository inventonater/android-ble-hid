/**
 * Project Settings for Inventonater HID
 * 
 * This project is structured into three modules:
 * - app: Main Android application module with UI and user interactions
 * - core: Core BLE HID functionality library (can be reused in other projects)
 * - unity-plugin: Unity integration for using BLE HID in Unity games/apps
 */
rootProject.name = "inventonater-hid"
include(":app")
include(":core")
include(":unity-plugin")

// Enable buildSrc for shared build logic
includeBuild("buildSrc")

// Repository setup for the settings script itself
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
