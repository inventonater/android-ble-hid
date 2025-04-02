package com.inventonater.hid.core.internal.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.inventonater.hid.core.api.NotificationManager
import com.inventonater.hid.core.api.ServiceCharacteristic
import com.inventonater.hid.core.api.ServiceDescriptor
import com.inventonater.hid.core.internal.diagnostics.LoggerFactory
import kotlinx.coroutines.runBlocking
import java.util.Arrays

/**
 * Implementation of a HID Keyboard service.
 *
 * This service provides keyboard functionality including key presses,
 * modifiers, and handling of LED state from the host.
 *
 * @property notificationManager Manager for sending notifications
 * @property loggerFactory Factory for creating loggers
 */
class KeyboardService(
    notificationManager: NotificationManager,
    loggerFactory: LoggerFactory
) : AbstractHidService(SERVICE_ID, notificationManager, loggerFactory) {
    
    companion object {
        const val SERVICE_ID = "keyboard"
        
        // Report IDs
        private const val KEYBOARD_REPORT_ID = 0x01
        
        // Report types
        private const val REPORT_TYPE_INPUT = 0x01
        private const val REPORT_TYPE_OUTPUT = 0x02
        
        // Report sizes
        private const val KEYBOARD_REPORT_SIZE = 8
        
    // Keyboard modifier byte bit masks (byte 0 of report)
    const val KEYBOARD_MODIFIER_LEFT_CTRL = 0x01.toByte()
    const val KEYBOARD_MODIFIER_LEFT_SHIFT = 0x02.toByte()
    const val KEYBOARD_MODIFIER_LEFT_ALT = 0x04.toByte()
    const val KEYBOARD_MODIFIER_LEFT_GUI = 0x08.toByte()
    const val KEYBOARD_MODIFIER_RIGHT_CTRL = 0x10.toByte()
    const val KEYBOARD_MODIFIER_RIGHT_SHIFT = 0x20.toByte()
    const val KEYBOARD_MODIFIER_RIGHT_ALT = 0x40.toByte()
    const val KEYBOARD_MODIFIER_RIGHT_GUI = 0x80.toByte()
        
        // Maximum number of keys to report at once (6 is standard for boot protocol)
        private const val MAX_KEYS = 6
    }
    
    // Report map for a standard keyboard
    override val reportMap: ByteArray = byteArrayOf(
        // Usage Page (Generic Desktop)
        0x05.toByte(), 0x01.toByte(),
        // Usage (Keyboard)
        0x09.toByte(), 0x06.toByte(),
        // Collection (Application)
        0xA1.toByte(), 0x01.toByte(),
        
        // Report ID
        0x85.toByte(), KEYBOARD_REPORT_ID.toByte(),
        
        // Usage Page (Key Codes)
        0x05.toByte(), 0x07.toByte(),
        // Usage Minimum (Keyboard Left Control)
        0x19.toByte(), 0xE0.toByte(),
        // Usage Maximum (Keyboard Right GUI)
        0x29.toByte(), 0xE7.toByte(),
        // Logical Minimum (0)
        0x15.toByte(), 0x00.toByte(),
        // Logical Maximum (1)
        0x25.toByte(), 0x01.toByte(),
        // Report Size (1)
        0x75.toByte(), 0x01.toByte(),
        // Report Count (8)
        0x95.toByte(), 0x08.toByte(),
        // Input (Data, Variable, Absolute): Modifier byte
        0x81.toByte(), 0x02.toByte(),
        
        // Report Count (1)
        0x95.toByte(), 0x01.toByte(),
        // Report Size (8)
        0x75.toByte(), 0x08.toByte(),
        // Input (Constant): Reserved byte
        0x81.toByte(), 0x01.toByte(),
        
        // Report Count (5)
        0x95.toByte(), 0x05.toByte(),
        // Report Size (1)
        0x75.toByte(), 0x01.toByte(),
        // Usage Page (LEDs)
        0x05.toByte(), 0x08.toByte(),
        // Usage Minimum (Num Lock)
        0x19.toByte(), 0x01.toByte(),
        // Usage Maximum (Kana)
        0x29.toByte(), 0x05.toByte(),
        // Output (Data, Variable, Absolute): LED report
        0x91.toByte(), 0x02.toByte(),
        
        // Report Count (1)
        0x95.toByte(), 0x01.toByte(),
        // Report Size (3)
        0x75.toByte(), 0x03.toByte(),
        // Output (Constant): LED report padding
        0x91.toByte(), 0x01.toByte(),
        
        // Report Count (6)
        0x95.toByte(), 0x06.toByte(),
        // Report Size (8)
        0x75.toByte(), 0x08.toByte(),
        // Logical Minimum (0)
        0x15.toByte(), 0x00.toByte(),
        // Logical Maximum (255)
        0x25.toByte(), 0xFF.toByte(),
        // Usage Page (Key Codes)
        0x05.toByte(), 0x07.toByte(),
        // Usage Minimum (0)
        0x19.toByte(), 0x00.toByte(),
        // Usage Maximum (255)
        0x29.toByte(), 0xFF.toByte(),
        // Input (Data, Array): Key array (6 keys)
        0x81.toByte(), 0x00.toByte(),
        
        // End Collection
        0xC0.toByte()
    )
    
    // Keyboard state
    private val keyboardState = ByteArray(KEYBOARD_REPORT_SIZE)
    
    // Flag indicating if the service is suspended
    private var suspended = false
    
    init {
        // Initialize report buffer with report ID
        keyboardState.fill(0)
        keyboardState[0] = KEYBOARD_REPORT_ID.toByte()
    }
    
    override fun getCharacteristics(): List<ServiceCharacteristic> {
        return listOf(
            // HID Information characteristic
            object : ServiceCharacteristic {
                override val uuid = HID_INFORMATION_UUID.toString()
                override val properties = BluetoothGattCharacteristic.PROPERTY_READ
                override val permissions = BluetoothGattCharacteristic.PERMISSION_READ
                override val initialValue = byteArrayOf(
                    0x11, 0x01,     // HID 1.11
                    0x00,           // Not localized
                    0x01            // Not normally connectable
                )
                override val descriptors = emptyList<ServiceDescriptor>()
            },
            
            // Report Map characteristic
            object : ServiceCharacteristic {
                override val uuid = HID_REPORT_MAP_UUID.toString()
                override val properties = BluetoothGattCharacteristic.PROPERTY_READ
                override val permissions = BluetoothGattCharacteristic.PERMISSION_READ
                override val initialValue = reportMap
                override val descriptors = emptyList<ServiceDescriptor>()
            },
            
            // HID Control Point characteristic
            object : ServiceCharacteristic {
                override val uuid = HID_CONTROL_POINT_UUID.toString()
                override val properties = BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                override val permissions = BluetoothGattCharacteristic.PERMISSION_WRITE
                override val initialValue = null
                override val descriptors = emptyList<ServiceDescriptor>()
            },
            
            // Input report characteristic
            object : ServiceCharacteristic {
                override val uuid = HID_REPORT_UUID.toString()
                override val properties = BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY
                override val permissions = BluetoothGattCharacteristic.PERMISSION_READ
                override val initialValue = ByteArray(KEYBOARD_REPORT_SIZE) { 
                    if (it == 0) KEYBOARD_REPORT_ID.toByte() else 0 
                }
                override val descriptors = listOf(
                    // Client Characteristic Configuration descriptor
                    object : ServiceDescriptor {
                        override val uuid = CLIENT_CONFIG_UUID.toString()
                        override val permissions = BluetoothGattDescriptor.PERMISSION_READ or
                                BluetoothGattDescriptor.PERMISSION_WRITE
                        override val value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    },
                    
                    // Report Reference descriptor
                    object : ServiceDescriptor {
                        override val uuid = REPORT_REFERENCE_UUID.toString()
                        override val permissions = BluetoothGattDescriptor.PERMISSION_READ
                        override val value = byteArrayOf(KEYBOARD_REPORT_ID.toByte(), REPORT_TYPE_INPUT.toByte())
                    }
                )
            },
            
            // Output report characteristic (for LED status)
            object : ServiceCharacteristic {
                override val uuid = HID_REPORT_UUID.toString()
                override val properties = BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_WRITE or
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
                override val permissions = BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE
                override val initialValue = byteArrayOf(KEYBOARD_REPORT_ID.toByte(), 0)
                override val descriptors = listOf(
                    // Report Reference descriptor
                    object : ServiceDescriptor {
                        override val uuid = REPORT_REFERENCE_UUID.toString()
                        override val permissions = BluetoothGattDescriptor.PERMISSION_READ
                        override val value = byteArrayOf(KEYBOARD_REPORT_ID.toByte(), REPORT_TYPE_OUTPUT.toByte())
                    }
                )
            }
        )
    }
    
    override fun initializeInternal(): Boolean {
        logger.info("Initializing keyboard service")
        
        // Reset report state
        keyboardState.fill(0)
        keyboardState[0] = KEYBOARD_REPORT_ID.toByte()
        
        return true
    }
    
    /**
     * Sends a single key press.
     *
     * @param keyCode The HID key code to send
     * @return true if the report was sent successfully, false otherwise
     */
    fun sendKey(keyCode: Int): Boolean {
        if (suspended) {
            logger.warn("Keyboard is suspended, cannot send key")
            return false
        }
        
        // Reset report state
        keyboardState.fill(0)
        keyboardState[0] = KEYBOARD_REPORT_ID.toByte()
        
        // Set the key in the first key slot
        keyboardState[2] = (keyCode and 0xFF).toByte()
        
        // Send the report
        return sendKeyboardReport(keyboardState)
    }
    
    /**
     * Sends a key press with modifier keys.
     *
     * @param keyCode The HID key code to send
     * @param modifiers Modifier byte (ctrl, shift, alt, gui)
     * @return true if the report was sent successfully, false otherwise
     */
    fun sendKeyWithModifiers(keyCode: Int, modifiers: Int): Boolean {
        if (suspended) {
            logger.warn("Keyboard is suspended, cannot send key")
            return false
        }
        
        // Reset report state
        keyboardState.fill(0)
        keyboardState[0] = KEYBOARD_REPORT_ID.toByte()
        
        // Set modifiers and key
        keyboardState[1] = modifiers.toByte()
        keyboardState[2] = (keyCode and 0xFF).toByte()
        
        // Send the report
        return sendKeyboardReport(keyboardState)
    }
    
    /**
     * Sends multiple key presses simultaneously.
     *
     * @param keyCodes Array of HID key codes to send
     * @return true if the report was sent successfully, false otherwise
     */
    fun sendKeys(keyCodes: IntArray): Boolean {
        if (suspended) {
            logger.warn("Keyboard is suspended, cannot send keys")
            return false
        }
        
        if (keyCodes.isEmpty()) {
            return releaseAllKeys()
        }
        
        // Reset report state
        keyboardState.fill(0)
        keyboardState[0] = KEYBOARD_REPORT_ID.toByte()
        
        // Add keys (up to 6)
        val numKeys = minOf(keyCodes.size, MAX_KEYS)
        for (i in 0 until numKeys) {
            keyboardState[i + 2] = (keyCodes[i] and 0xFF).toByte()
        }
        
        // Send the report
        return sendKeyboardReport(keyboardState)
    }
    
    /**
     * Sends key presses with modifier keys.
     *
     * @param keyCodes Array of HID key codes to send
     * @param modifiers Modifier byte (ctrl, shift, alt, gui)
     * @return true if the report was sent successfully, false otherwise
     */
    fun sendKeysWithModifiers(keyCodes: IntArray, modifiers: Int): Boolean {
        if (suspended) {
            logger.warn("Keyboard is suspended, cannot send keys")
            return false
        }
        
        // Reset report state
        keyboardState.fill(0)
        keyboardState[0] = KEYBOARD_REPORT_ID.toByte()
        
        // Set modifiers
        keyboardState[1] = modifiers.toByte()
        
        if (keyCodes.isNotEmpty()) {
            // Add keys (up to 6)
            val numKeys = minOf(keyCodes.size, MAX_KEYS)
            for (i in 0 until numKeys) {
                keyboardState[i + 2] = (keyCodes[i] and 0xFF).toByte()
            }
        }
        
        // Send the report
        return sendKeyboardReport(keyboardState)
    }
    
    /**
     * Releases all pressed keys.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    fun releaseAllKeys(): Boolean {
        if (suspended) {
            logger.warn("Keyboard is suspended, cannot release keys")
            return false
        }
        
        // Reset report state (all zeros means no keys pressed)
        keyboardState.fill(0)
        keyboardState[0] = KEYBOARD_REPORT_ID.toByte()
        
        // Send the report
        return sendKeyboardReport(keyboardState)
    }
    
    /**
     * Send a keyboard report.
     *
     * @param report The report data to send
     * @return true if the report was sent successfully, false otherwise
     */
    private fun sendKeyboardReport(report: ByteArray): Boolean {
        logger.debug("Sending keyboard report: ${bytesToHex(report)}")
        
        return runBlocking {
            sendReport(KEYBOARD_REPORT_ID, report.sliceArray(1 until report.size))
        }
    }
    
    override fun handleReport(reportId: Int, data: ByteArray): Boolean {
        if (reportId != KEYBOARD_REPORT_ID) {
            return false
        }
        
        // This is an output report from the host (LED status)
        if (data.isNotEmpty()) {
            val ledState = data[0]
            logger.debug("Received LED state: $ledState")
            
            // Process LED state if needed
            // bit 0: Num Lock
            // bit 1: Caps Lock
            // bit 2: Scroll Lock
            // bit 3: Compose
            // bit 4: Kana
            
            return true
        }
        
        return false
    }
    
    override fun shutdown() {
        if (!isInitialized()) {
            logger.warn("Service not initialized, nothing to shut down")
            return
        }
        
        logger.info("Shutting down keyboard service")
        
        // Release all keys before shutting down
        runBlocking {
            try {
                val reportData = ByteArray(KEYBOARD_REPORT_SIZE - 1)
                sendReport(KEYBOARD_REPORT_ID, reportData)
            } catch (e: Exception) {
                logger.error("Error releasing keys during shutdown", e)
            }
        }
        
        super.shutdown()
    }
}
