package com.inventonater.hid.core.api

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Central manager interface for BLE HID functionality.
 * 
 * This is the main entry point for applications to use the BLE HID functionality.
 * It coordinates all the components needed for BLE HID operation.
 */
interface BleHidManager {
    /**
     * Current connection state as an observable flow.
     */
    val connectionState: StateFlow<ConnectionState>
    
    /**
     * Initialize the BLE HID manager.
     *
     * @param context Application context
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(context: Context): Boolean
    
    /**
     * Start advertising the BLE HID device.
     *
     * @return true if advertising started successfully, false otherwise
     */
    fun startAdvertising(): Boolean
    
    /**
     * Stop advertising the BLE HID device.
     */
    fun stopAdvertising()
    
    /**
     * Check if the device is currently advertising.
     *
     * @return true if advertising, false otherwise
     */
    fun isAdvertising(): Boolean
    
    /**
     * Check if a device is connected.
     *
     * @return true if a device is connected, false otherwise
     */
    fun isConnected(): Boolean
    
    /**
     * Get the connected device.
     *
     * @return The connected BluetoothDevice, or null if not connected
     */
    fun getConnectedDevice(): BluetoothDevice?
    
    /**
     * Close the BLE HID manager and release all resources.
     */
    fun close()
    
    /**
     * Check if the device supports BLE peripheral mode.
     *
     * @return true if peripheral mode is supported, false otherwise
     */
    fun isBlePeripheralSupported(): Boolean
    
    /**
     * Add a listener for connection events.
     *
     * @param listener The listener to add
     */
    fun addConnectionListener(listener: ConnectionListener)
    
    /**
     * Remove a connection listener.
     *
     * @param listener The listener to remove
     */
    fun removeConnectionListener(listener: ConnectionListener)
    
    /**
     * Register a service factory.
     *
     * @param serviceId Unique identifier for the service
     * @param factory Factory function to create the service
     */
    fun registerServiceFactory(serviceId: String, factory: () -> HidServiceBase)
    
    /**
     * Activate a specific service.
     *
     * @param serviceId ID of the service to activate
     * @return true if the service was activated successfully, false otherwise
     */
    fun activateService(serviceId: String): Boolean
    
    /**
     * Deactivate a specific service.
     *
     * @param serviceId ID of the service to deactivate
     */
    fun deactivateService(serviceId: String)
    
    /**
     * Get available service IDs.
     *
     * @return List of available service IDs
     */
    fun getAvailableServices(): List<String>
    
    /**
     * Get active service IDs.
     *
     * @return List of active service IDs
     */
    fun getActiveServices(): List<String>
    
    // Mouse functionality
    
    /**
     * Move the mouse pointer.
     *
     * @param x Horizontal movement (-127 to 127)
     * @param y Vertical movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    fun moveMouse(x: Int, y: Int): Boolean
    
    /**
     * Click a mouse button.
     *
     * @param button Button to click (defined in [MouseButton])
     * @return true if the report was sent successfully, false otherwise
     */
    fun clickMouseButton(button: MouseButton): Boolean
    
    /**
     * Press a mouse button.
     *
     * @param button Button to press (defined in [MouseButton])
     * @return true if the report was sent successfully, false otherwise
     */
    fun pressMouseButton(button: MouseButton): Boolean
    
    /**
     * Release all mouse buttons.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    fun releaseMouseButtons(): Boolean
    
    /**
     * Scroll the mouse wheel.
     *
     * @param amount Scroll amount (-127 to 127, positive for up, negative for down)
     * @return true if the report was sent successfully, false otherwise
     */
    fun scrollMouseWheel(amount: Int): Boolean
    
    // Keyboard functionality
    
    /**
     * Send a keyboard key.
     *
     * @param keyCode HID key code
     * @return true if the report was sent successfully, false otherwise
     */
    fun sendKey(keyCode: Int): Boolean
    
    /**
     * Send multiple keyboard keys simultaneously.
     *
     * @param keyCodes Array of HID key codes
     * @return true if the report was sent successfully, false otherwise
     */
    fun sendKeys(keyCodes: IntArray): Boolean
    
    /**
     * Releases all pressed keys.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    fun releaseKeys(): Boolean
}

/**
 * Listener for connection events.
 */
interface ConnectionListener {
    /**
     * Called when a device connects.
     *
     * @param device The connected device
     */
    fun onDeviceConnected(device: BluetoothDevice)
    
    /**
     * Called when a device disconnects.
     *
     * @param device The disconnected device
     */
    fun onDeviceDisconnected(device: BluetoothDevice)
}

/**
 * Connection states for the BLE HID manager.
 */
sealed class ConnectionState {
    /**
     * Not connected to any device.
     */
    object Disconnected : ConnectionState()
    
    /**
     * Connection in progress.
     */
    object Connecting : ConnectionState()
    
    /**
     * Connected to a device.
     *
     * @param device The connected device
     */
    data class Connected(val device: BluetoothDevice) : ConnectionState()
    
    /**
     * Connection failed.
     *
     * @param reason Reason for the failure
     */
    data class Failed(val reason: String) : ConnectionState()
}

/**
 * Mouse buttons.
 */
enum class MouseButton(val value: Int) {
    LEFT(1),
    RIGHT(2),
    MIDDLE(4)
}
