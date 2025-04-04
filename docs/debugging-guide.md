# Inventonater BLE HID Debugging Guide

This document explains the debug tools and capabilities added to the Inventonater BLE HID implementation.

## Overview

The Inventonater BLE HID implementation now includes advanced debugging capabilities through the new `BleHidDebugger` class and its integration with the core component classes. These tools provide detailed insights into the BLE HID device operation, helping to diagnose issues with connections, report delivery, and compatibility.

## Debug Features

### 1. UI Debug Controls

The ModernHidActivity now includes several debug controls:
- **Debug Options** button - Opens a dialog with additional debugging features
- **Clear Log** button - Clears the current debug log
- **Report Stats** button - Shows current report statistics

### 2. Logging Options

#### Verbose Logging
Enables detailed logging of all HID operations, including byte-level report data. This is useful for diagnosing report format issues.

#### File Logging
Saves all debug logs to a file in the device's external storage. This allows for post-mortem analysis and sharing logs for support. Log files are stored in the app's external files directory under `hid_logs/`.

### 3. Diagnostics

#### Environment Analysis
Analyzes the device environment to determine:
- Android version and compatibility
- Bluetooth support level
- Available Bluetooth profiles
- HID Device profile support

Example usage:
```java
hidDebugger.analyzeEnvironment(context);
```

#### HID Descriptor Analysis
Examines the HID report descriptor to verify it's properly formatted and includes all necessary reports (keyboard, mouse, consumer control). This helps identify descriptor issues that might cause incompatibility with certain host devices.

Example usage:
```java
hidDebugger.analyzeHidDescriptor();
```

#### Connection State Analysis
Automatically analyzes connection state changes and provides diagnostics about potential issues:
- Failed connection attempts
- Pairing failures
- Connection timing statistics

### 4. Statistics

The debugger tracks various statistics about HID operations:
- Number of mouse reports sent
- Number of keyboard reports sent
- Number of consumer reports sent
- Number of failed report transmissions
- Success rate for each report type

Example usage:
```java
String stats = hidDebugger.getReportStatistics();
```

### 5. Performance Measurement

The debugger includes tools for measuring performance:
- Timing connection establishment
- Measuring report delivery latency
- Tracking operation durations

Example usage:
```java
// Mark the start time
hidDebugger.markTimestamp("operation_start");

// ... perform operation ...

// Report the elapsed time
hidDebugger.logElapsedTime("operation_start", "Operation duration");
```

## Using the Debugger

### Basic Usage

The debugger is automatically initialized as part of the HidManager. To access it:

```java
BleHidDebugger debugger = hidManager.getDebugger();
```

### Enabling File Logging

File logging requires storage permission:

```java
// Check and request permission first
if (hasStoragePermission) {
    debugger.enableFileLogging(context, true);
}
```

### Common Debugging Scenarios

#### Troubleshooting Connection Issues
1. Enable verbose logging
2. Use environment analysis to check device capabilities
3. Attempt to register and connect
4. Check the logs for connection state changes and errors

#### Diagnosing Report Delivery Problems
1. Enable verbose logging
2. Send reports using the UI controls
3. Check report statistics to see if reports are being delivered
4. Review logs for any errors in report sending

#### Investigating Host Compatibility Issues
1. Run the HID descriptor analysis
2. Check if all required report types are included
3. Look for any warnings about descriptor formatting
4. Compare with known-good descriptor formats

## Code Integration

The debugging system is integrated with all core components:

1. **HidManager** - Main integration point
2. **HidDeviceCallback** - Captures BLE events for analysis
3. **Report classes** - Track report delivery success

## Implementation Notes

- The debugger is implemented as a singleton to ensure a single instance is used across the application
- It provides file logging capabilities with proper lifecycle management
- Performance tracking uses atomic operations to ensure thread safety
- All UI interactions are performed on the main thread to avoid concurrency issues
