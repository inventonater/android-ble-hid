# HID Service Design

This document details the design of the HID service abstraction layer, a key component in making the Inventonater HID system extensible for new controller types.

## Service Architecture

```mermaid
graph TD
    A[HidServiceBase] <|-- B[AbstractHidService]
    B <|-- C[MouseService]
    B <|-- D[KeyboardService]
    B <|-- E[MediaControlService]
    B <|-- F[GamepadService]
    A <|-- G[CompositeHidService]
    G --> H[Service 1]
    G --> I[Service 2]
    G --> J[Service N]
    K[ServiceFactory] --> A
```

## Key Interfaces

### HidServiceBase

The fundamental interface that all HID services must implement:

```kotlin
interface HidServiceBase {
    val serviceId: String
    val reportMap: ByteArray
    fun initialize(): Boolean
    fun shutdown()
    fun getCharacteristics(): List<ServiceCharacteristic>
    fun handleReport(reportId: Int, data: ByteArray): Boolean
    fun isInitialized(): Boolean
}
```

### AbstractHidService

Abstract base class providing common implementation details:

- Default initialization behavior
- Standard GATT characteristic creation
- Common error handling
- Logging integration

### ServiceCharacteristic

Models a BLE characteristic needed by a service:

- UUID
- Properties (read, write, notify)
- Permissions
- Initial value
- Descriptors

### ReportDescriptor

Encapsulates HID report descriptor details:

- Report ID
- Report type (input, output, feature)
- Report size
- Format definition

### ServiceFactory

Manages service registration and creation:

- Register service types
- Create service instances
- Combine services
- Resolve service dependencies

## Service Design Principles

1. **Single Responsibility**: Each service focuses on one type of HID functionality
2. **Composition**: Complex devices are composed of multiple simple services
3. **Configuration**: Services are configurable through parameters
4. **Independence**: Services operate independently of other services
5. **Consistent Initialization**: All services follow the same initialization pattern
6. **Resource Management**: Services properly manage their own resources

## Service Lifecycle

1. **Registration**: Services register with the ServiceFactory
2. **Creation**: ServiceFactory creates instances when requested
3. **Initialization**: BleHidManager initializes services
4. **Operation**: Services respond to API calls and generate reports
5. **Shutdown**: Services clean up resources when no longer needed

## Report Handling

Report handling follows a consistent pattern:

1. Client code calls specific API method (e.g., `moveMouse`, `pressKey`)
2. Service translates API call to appropriate HID report
3. Service sends the report through the notification system
4. Report monitor traces the report for diagnostics (if enabled)

## Adding New Services

To add a new HID service type:

1. Implement the `HidServiceBase` interface (typically by extending `AbstractHidService`)
2. Define the service's report descriptor
3. Implement the specific HID functionality
4. Register the service with the ServiceFactory
5. Create an appropriate API surface in BleHidManager

## Composite Services

Composite services allow the combination of multiple individual services:

1. `CompositeHidService` implements `HidServiceBase`
2. It contains multiple child services
3. Report IDs are managed to avoid conflicts
4. A composite report map combines all service report maps
5. Initialization and shutdown are coordinated across all child services

## Example: Media Control Service

A media control service would:

1. Extend `AbstractHidService`
2. Define a consumer control report descriptor for media keys
3. Implement methods like `playPause()`, `volumeUp()`, `next()`
4. Generate the appropriate HID reports for each action
5. Register with the ServiceFactory with a unique service ID

## Service Discovery and Selection

Client applications can:

1. Query available service types
2. Select which services to activate
3. Create custom service combinations
4. Save and restore service configurations
