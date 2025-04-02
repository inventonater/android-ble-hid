# Inventonater HID - Modernization Project

This document set outlines the comprehensive modernization plan for the Inventonater HID project - an Android application that functions as a Bluetooth Low Energy (BLE) Human Interface Device (HID), allowing Android devices to be used as wireless mice, keyboards, and other input devices.

## Project Goals

1. **Maintain Feature Set**: Preserve existing functionality while improving implementation
2. **Modernize Technology**: Move from Java to Kotlin and incorporate modern Android practices
3. **Improve Architecture**: Create a more maintainable, extensible architecture
4. **Enhance Reliability**: Address connection stability and compatibility issues
5. **Enable Extensibility**: Make it easier to add new controller types

## Documentation Structure

- [Architecture Overview](architecture/overview.md) - High-level architecture design
- [Core Components](architecture/core-components.md) - Key components and their responsibilities
- [Service Design](architecture/service-design.md) - HID service abstraction design
- [Migration Strategy](implementation/migration-strategy.md) - Steps for code migration
- [Kotlin Migration](implementation/kotlin-migration.md) - Java to Kotlin conversion approach
- [BLE Connectivity](implementation/ble-connectivity.md) - BLE connection management
- [Device Compatibility](implementation/device-compatibility.md) - Device compatibility strategy
- [Diagnostics](implementation/diagnostics.md) - Logging and diagnostic tools
- [Controller Types](new-features/controller-types.md) - Guide for adding new controller types

## Project Structure

```
android-ble-hid/
├── .old/                            # Original Java implementation 
│   ├── app/                         # Original app module
│   ├── core/                        # Original core module
│   └── unity-plugin/                # Original Unity plugin
├── app/                             # New app module
├── core/                            # New core module with Kotlin implementation
├── unity-plugin/                    # New Unity plugin module
└── buildSrc/                        # Centralized build logic and dependencies
```

## Implementation Approach

Our rewrite will maintain a clean-slate approach:
1. Preserve the original codebase in a `.old` directory
2. Build new modules from scratch with Kotlin
3. Design with extension and maintainability in mind
4. Incorporate modern Android architecture patterns
5. Improve diagnostics and error handling throughout

## Package Structure

All new code will use the `com.inventonater.hid` package namespace:

- `com.inventonater.hid.core.api` - Public interfaces
- `com.inventonater.hid.core.service` - HID service implementations
- `com.inventonater.hid.core.ble` - BLE connection management
- `com.inventonater.hid.core.compatibility` - Device-specific adaptations
- `com.inventonater.hid.core.diagnostics` - Logging and diagnostics utilities
