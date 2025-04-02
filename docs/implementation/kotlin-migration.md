# Kotlin Migration

This document outlines our approach to migrating from Java to Kotlin and the benefits this will bring to the Inventonater HID project.

## Migration Benefits

1. **Null Safety**: Kotlin's type system helps prevent null pointer exceptions
2. **Conciseness**: Less boilerplate code for more maintainable solutions
3. **Extension Functions**: Add functionality to existing classes without inheritance
4. **Coroutines**: Simplified asynchronous programming
5. **Higher-Order Functions**: Better callback and event handling
6. **Data Classes**: Simplified model classes with built-in utility methods
7. **Scope Functions**: More expressive code with let, apply, run, etc.
8. **Property Delegation**: Simplified common patterns like lazy initialization

## Conversion Approach

Rather than using automatic conversion tools, we'll rewrite the code from scratch to:

1. Fully utilize Kotlin's features
2. Apply modern design patterns
3. Create a cleaner, more maintainable codebase
4. Remove accumulated technical debt
5. Better adapt the code to Android's Kotlin-focused future

## Key Kotlin Features for BLE HID

### Sealed Classes for Connection States

```kotlin
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val device: BluetoothDevice) : ConnectionState()
    data class Failed(val reason: String) : ConnectionState()
}
```

### Extension Functions for Bluetooth Operations

```kotlin
// Example extension functions (not complete implementations)
fun BluetoothGattService.findCharacteristic(uuid: UUID): BluetoothGattCharacteristic?
fun BluetoothGattCharacteristic.enableNotifications(): Boolean
fun BluetoothDevice.isConnected(): Boolean
```

### Coroutines for Asynchronous Operations

```kotlin
suspend fun connectDevice(device: BluetoothDevice): Result<Unit>
suspend fun sendNotification(characteristic: BluetoothGattCharacteristic, data: ByteArray): Result<Unit>
```

### Data Classes for Report Data

```kotlin
data class MouseReport(
    val buttons: Int,
    val x: Int,
    val y: Int,
    val wheel: Int
)

data class KeyboardReport(
    val modifiers: Int,
    val keyCodes: List<Int>
)
```

### Flow for State Updates

```kotlin
val connectionState: StateFlow<ConnectionState>
val notificationResults: Flow<NotificationResult>
```

## Package Structure for Kotlin Code

```
com.inventonater.hid.core/
  |- api/                 # Public interfaces
  |- internal/            # Implementation details
     |- ble/              # BLE implementation
     |- service/          # Service implementations
     |- compatibility/    # Device compatibility
     |- util/             # Utility classes and extensions
```

## Coding Standards

1. **Naming Conventions**:
   - Interfaces: No "I" prefix (e.g., `HidService` not `IHidService`)
   - Classes: CamelCase (e.g., `BleHidManager`)
   - Functions: camelCase (e.g., `connectDevice()`)
   - Constants: UPPER_SNAKE_CASE (e.g., `MAX_REPORT_SIZE`)

2. **Function Style**:
   - Prefer expression functions for simple cases
   - Use named parameters for better readability
   - Keep functions focused and small

3. **Extension Usage**:
   - Organize extensions by the extended type
   - Document extensions thoroughly
   - Prefer extension functions over utility classes

4. **Null Safety**:
   - Minimize use of `!!` operator
   - Use `?.` and `?:` operators appropriately
   - Document nullability expectations

## Migration Challenges

1. **Interoperability with Java**:
   - Unity plugin may require Java compatibility
   - Handle Java nullability concerns at boundaries

2. **Platform-Specific Features**:
   - Ensure Kotlin features work with Android API levels
   - Handle backward compatibility appropriately

3. **Learning Curve**:
   - Team members may need to adjust to Kotlin idioms
   - Avoid overly complex Kotlin features initially

## Kotlin-Specific Testing

1. **MockK for Mocking**:
   - Use MockK instead of Mockito for better Kotlin support
   - Leverage coroutine testing utilities

2. **Property-Based Testing**:
   - Use Kotlin's built-in features for property-based tests
   - Test nullability contracts thoroughly

## Gradual Adoption Strategy

While we're doing a clean-slate rewrite, we'll approach Kotlin features with increasing sophistication:

1. **Phase 1**: Basic Kotlin syntax and null safety
2. **Phase 2**: Extension functions and higher-order functions
3. **Phase 3**: Coroutines for asynchronous operations
4. **Phase 4**: Flow for reactive programming
5. **Phase 5**: Advanced features (DSLs, delegates, etc.)

This approach allows the team to gradually adapt to Kotlin while delivering value early.
