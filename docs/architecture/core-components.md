# Core Components

This document details the primary components of the Inventonater HID architecture, their responsibilities, and their interactions.

## BleHidManager

The central coordinator for the BLE HID functionality.

**Responsibilities:**
- Initialize and manage the BLE HID system
- Coordinate between services, connection management, and device compatibility
- Expose the public API for client applications
- Manage the lifecycle of BLE components

**Key Interfaces:**
- `BleHidManager`: Central interface for controlling BLE HID functionality
- `BleHidManagerListener`: Interface for clients to receive state changes and events

## ServiceFactory

Factory for creating and managing HID services.

**Responsibilities:**
- Register service types (mouse, keyboard, etc.)
- Create service instances on demand
- Combine multiple services into composite services
- Manage service dependencies and lifecycle

**Key Interfaces:**
- `ServiceFactory`: Service registration and creation interface
- `ServiceRegistration`: Information needed to register a service

## HidServiceBase

Base interface for all HID services.

**Responsibilities:**
- Define the common contract for all HID services
- Standardize service initialization and configuration
- Provide report descriptor and characteristic definitions
- Handle reports for specific HID functionality

**Key Interfaces:**
- `HidServiceBase`: Common interface for all services
- `ReportDescriptor`: Interface for HID report descriptors
- `ServiceCharacteristic`: Interface for service characteristics

## BleConnectionManager

Manages BLE connections and the GATT server.

**Responsibilities:**
- Initialize the BLE GATT server
- Handle device connections and disconnections
- Manage BLE advertising
- Coordinate pairing and security

**Key Interfaces:**
- `BleConnectionManager`: Interface for connection management
- `ConnectionState`: Enum of possible connection states
- `ConnectionStateListener`: Interface for connection state changes

## NotificationManager

Handles BLE notifications to connected devices.

**Responsibilities:**
- Send notifications for HID reports
- Track notification delivery and failures
- Manage notification queuing and retry
- Handle notification enabling/disabling

**Key Interfaces:**
- `NotificationManager`: Interface for sending notifications
- `NotificationResult`: Result of notification attempts
- `NotificationListener`: Interface for notification events

## DeviceCompatibility

Provides device-specific compatibility adaptations.

**Responsibilities:**
- Detect the type of connected host device
- Adapt HID services for specific platforms
- Customize report maps for better compatibility
- Apply device-specific workarounds

**Key Interfaces:**
- `DeviceCompatibility`: Interface for compatibility strategies
- `DeviceType`: Enum of supported device types
- `CompatibilityAdapter`: Interface for adapting services for specific devices

## DiagnosticsManager

Manages logging, monitoring, and diagnostics.

**Responsibilities:**
- Provide structured logging at different verbosity levels
- Monitor HID reports for debugging
- Track connection state transitions
- Record and report errors

**Key Interfaces:**
- `DiagnosticsManager`: Interface for diagnostic functionality
- `LogLevel`: Enum of log levels
- `LoggingStrategy`: Interface for different logging backends
- `ReportMonitor`: Interface for monitoring HID reports

## Service Implementations

Concrete implementations of various HID services.

### MouseService

**Responsibilities:**
- Implement HID mouse functionality
- Define mouse report descriptor
- Handle mouse movement and button reports
- Support different mouse features (buttons, wheel, etc.)

### KeyboardService

**Responsibilities:**
- Implement HID keyboard functionality
- Define keyboard report descriptor
- Handle key press and release reports
- Support modifier keys and key combinations

### MediaControlService

**Responsibilities:**
- Implement media control functionality
- Define media control report descriptor
- Handle media key reports (play, pause, volume, etc.)
- Support consumer control functions

### CompositeService

**Responsibilities:**
- Combine multiple services into a unified device
- Manage report IDs across services
- Coordinate service initialization and shutdown
- Handle routing reports to appropriate services
