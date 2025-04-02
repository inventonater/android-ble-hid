package com.inventonater.hid.core

import android.content.Context
import com.inventonater.hid.core.api.BleHidManager
import com.inventonater.hid.core.api.ConnectionListener
import com.inventonater.hid.core.api.LogLevel
import com.inventonater.hid.core.api.MouseButton
import com.inventonater.hid.core.di.BleHidModule

/**
 * Main entry point for the BLE HID library.
 *
 * This class provides a simplified facade to the BLE HID functionality,
 * making it easy for client applications to use the library without
 * having to deal with the internal implementation details.
 *
 * Usage:
 * ```
 * // Initialize the BLE HID library
 * BleHid.initialize(context)
 *
 * // Start advertising
 * BleHid.startAdvertising()
 *
 * // Use mouse functionality
 * BleHid.moveMouse(10, 5)
 * BleHid.clickMouseButton(MouseButton.LEFT)
 *
 * // Shutdown when done
 * BleHid.shutdown()
 * ```
 */
object BleHid {
    private var manager: BleHidManager? = null
    
    /**
     * Initialize the BLE HID library.
     *
     * This method must be called before any other methods.
     *
     * @param context Application context
     * @param logLevel Log level for diagnostics (default: INFO)
     * @return true if initialization was successful, false otherwise
     */
    @JvmStatic
    fun initialize(context: Context, logLevel: LogLevel = LogLevel.INFO): Boolean {
        try {
            manager = BleHidModule.initialize(context, logLevel)
            
            // Initialize the manager
            val initSuccess = manager?.initialize(context) ?: false
            
            // Activate HID services
            if (initSuccess) {
                manager?.activateService("mouse")
                manager?.activateService("keyboard")
            }
            
            return initSuccess
        } catch (e: Exception) {
            // Initialization failed
            return false
        }
    }
    
    /**
     * Check if the BLE HID library is initialized.
     *
     * @return true if initialized, false otherwise
     */
    @JvmStatic
    fun isInitialized(): Boolean {
        return manager != null
    }
    
    /**
     * Start advertising the BLE HID device.
     *
     * @return true if advertising started successfully, false otherwise
     */
    @JvmStatic
    fun startAdvertising(): Boolean {
        return manager?.startAdvertising() ?: false
    }
    
    /**
     * Stop advertising the BLE HID device.
     */
    @JvmStatic
    fun stopAdvertising() {
        manager?.stopAdvertising()
    }
    
    /**
     * Check if the device is currently advertising.
     *
     * @return true if advertising, false otherwise
     */
    @JvmStatic
    fun isAdvertising(): Boolean {
        return manager?.isAdvertising() ?: false
    }
    
    /**
     * Check if a device is connected.
     *
     * @return true if a device is connected, false otherwise
     */
    @JvmStatic
    fun isConnected(): Boolean {
        return manager?.isConnected() ?: false
    }
    
    /**
     * Add a listener for connection events.
     *
     * @param listener The listener to add
     */
    @JvmStatic
    fun addConnectionListener(listener: ConnectionListener) {
        manager?.addConnectionListener(listener)
    }
    
    /**
     * Remove a connection listener.
     *
     * @param listener The listener to remove
     */
    @JvmStatic
    fun removeConnectionListener(listener: ConnectionListener) {
        manager?.removeConnectionListener(listener)
    }
    
    /**
     * Shutdown the BLE HID library and release all resources.
     */
    @JvmStatic
    fun shutdown() {
        manager?.close()
        manager = null
        BleHidModule.shutdown()
    }
    
    /**
     * Check if the device supports BLE peripheral mode.
     *
     * @return true if peripheral mode is supported, false otherwise
     */
    @JvmStatic
    fun isBlePeripheralSupported(): Boolean {
        return manager?.isBlePeripheralSupported() ?: false
    }
    
    // Mouse functionality
    
    /**
     * Move the mouse pointer.
     *
     * @param x Horizontal movement (-127 to 127)
     * @param y Vertical movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    @JvmStatic
    fun moveMouse(x: Int, y: Int): Boolean {
        return manager?.moveMouse(x, y) ?: false
    }
    
    /**
     * Click a mouse button.
     *
     * @param button Button to click (defined in [MouseButton])
     * @return true if the report was sent successfully, false otherwise
     */
    @JvmStatic
    fun clickMouseButton(button: MouseButton): Boolean {
        return manager?.clickMouseButton(button) ?: false
    }
    
    /**
     * Press a mouse button.
     *
     * @param button Button to press (defined in [MouseButton])
     * @return true if the report was sent successfully, false otherwise
     */
    @JvmStatic
    fun pressMouseButton(button: MouseButton): Boolean {
        return manager?.pressMouseButton(button) ?: false
    }
    
    /**
     * Release all mouse buttons.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    @JvmStatic
    fun releaseMouseButtons(): Boolean {
        return manager?.releaseMouseButtons() ?: false
    }
    
    /**
     * Scroll the mouse wheel.
     *
     * @param amount Scroll amount (-127 to 127, positive for up, negative for down)
     * @return true if the report was sent successfully, false otherwise
     */
    @JvmStatic
    fun scrollMouseWheel(amount: Int): Boolean {
        return manager?.scrollMouseWheel(amount) ?: false
    }
    
    // Keyboard functionality
    
    /**
     * Send a keyboard key.
     *
     * @param keyCode HID key code
     * @return true if the report was sent successfully, false otherwise
     */
    @JvmStatic
    fun sendKey(keyCode: Int): Boolean {
        return manager?.sendKey(keyCode) ?: false
    }
    
    /**
     * Send multiple keyboard keys simultaneously.
     *
     * @param keyCodes Array of HID key codes
     * @return true if the report was sent successfully, false otherwise
     */
    @JvmStatic
    fun sendKeys(keyCodes: IntArray): Boolean {
        return manager?.sendKeys(keyCodes) ?: false
    }
    
    /**
     * Releases all pressed keys.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    @JvmStatic
    fun releaseKeys(): Boolean {
        return manager?.releaseKeys() ?: false
    }
}
