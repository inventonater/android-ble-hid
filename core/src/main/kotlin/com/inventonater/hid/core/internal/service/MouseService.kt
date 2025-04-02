package com.inventonater.hid.core.internal.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import com.inventonater.hid.core.api.MouseButton
import com.inventonater.hid.core.api.NotificationManager
import com.inventonater.hid.core.api.ServiceCharacteristic
import com.inventonater.hid.core.api.ServiceDescriptor
import com.inventonater.hid.core.internal.diagnostics.LoggerFactory
import kotlinx.coroutines.runBlocking

/**
 * Implementation of a HID Mouse service.
 *
 * This service provides mouse functionality including movement,
 * button clicks, and scrolling.
 *
 * @property notificationManager Manager for sending notifications
 * @property loggerFactory Factory for creating loggers
 */
class MouseService(
    notificationManager: NotificationManager,
    loggerFactory: LoggerFactory
) : AbstractHidService(SERVICE_ID, notificationManager, loggerFactory) {
    
    companion object {
        const val SERVICE_ID = "mouse"
        private const val REPORT_ID = 0x01
        
        // Button masks
        private const val BUTTON_LEFT_MASK = 0x01
        private const val BUTTON_RIGHT_MASK = 0x02
        private const val BUTTON_MIDDLE_MASK = 0x04
        
        // Max movement values
        private const val MAX_MOVEMENT = 127
        private const val MIN_MOVEMENT = -127
    }
    
    // Report map for a standard 3-button mouse with scroll wheel
    override val reportMap: ByteArray = byteArrayOf(
        // Usage Page (Generic Desktop)
        0x05.toByte(), 0x01,
        // Usage (Mouse)
        0x09.toByte(), 0x02,
        // Collection (Application)
        0xA1.toByte(), 0x01,
        // Report ID (1)
        0x85.toByte(), REPORT_ID.toByte(),
        // Usage (Pointer)
        0x09.toByte(), 0x01,
        // Collection (Physical)
        0xA1.toByte(), 0x00,
        
        // Usage Page (Button)
        0x05.toByte(), 0x09,
        // Usage Minimum (Button 1)
        0x19.toByte(), 0x01,
        // Usage Maximum (Button 3)
        0x29.toByte(), 0x03,
        // Logical Minimum (0)
        0x15.toByte(), 0x00,
        // Logical Maximum (1)
        0x25.toByte(), 0x01,
        // Report Size (1)
        0x75.toByte(), 0x01,
        // Report Count (3)
        0x95.toByte(), 0x03,
        // Input (Data, Variable, Absolute)
        0x81.toByte(), 0x02,
        
        // Report Size (5)
        0x75.toByte(), 0x05,
        // Report Count (1)
        0x95.toByte(), 0x01,
        // Input (Constant) - padding
        0x81.toByte(), 0x01,
        
        // Usage Page (Generic Desktop)
        0x05.toByte(), 0x01,
        // Usage (X)
        0x09.toByte(), 0x30,
        // Usage (Y)
        0x09.toByte(), 0x31,
        // Logical Minimum (-127)
        0x15.toByte(), 0x81.toByte(),
        // Logical Maximum (127)
        0x25.toByte(), 0x7F,
        // Report Size (8)
        0x75.toByte(), 0x08,
        // Report Count (2)
        0x95.toByte(), 0x02,
        // Input (Data, Variable, Relative)
        0x81.toByte(), 0x06,
        
        // Usage (Wheel)
        0x09.toByte(), 0x38,
        // Logical Minimum (-127)
        0x15.toByte(), 0x81.toByte(),
        // Logical Maximum (127)
        0x25.toByte(), 0x7F,
        // Report Size (8)
        0x75.toByte(), 0x08,
        // Report Count (1)
        0x95.toByte(), 0x01,
        // Input (Data, Variable, Relative)
        0x81.toByte(), 0x06,
        
        // End Collection (Physical)
        0xC0.toByte(),
        // End Collection (Application)
        0xC0.toByte()
    )
    
    // Mouse report: [reportId, buttons, x, y, wheel]
    private val mouseReport = ByteArray(5)
    
    init {
        // Initialize report buffer with report ID
        mouseReport[0] = REPORT_ID.toByte()
    }
    
    override fun getCharacteristics(): List<ServiceCharacteristic> {
        // Standard HID characteristics
        return listOf(
            // HID Information characteristic
            object : ServiceCharacteristic {
                override val uuid = HID_INFORMATION_UUID.toString()
                override val properties = BluetoothGattCharacteristic.PROPERTY_READ
                override val permissions = BluetoothGattCharacteristic.PERMISSION_READ
                override val initialValue = byteArrayOf(
                    0x11, 0x01,     // HID 1.11
                    0x00,           // Not localized
                    0x03            // Can wake up host, normally connectable
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
            
            // Report characteristic (for mouse input reports)
            object : ServiceCharacteristic {
                override val uuid = HID_REPORT_UUID.toString()
                override val properties = BluetoothGattCharacteristic.PROPERTY_READ or
                        BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                        BluetoothGattCharacteristic.PROPERTY_WRITE
                override val permissions = BluetoothGattCharacteristic.PERMISSION_READ or
                        BluetoothGattCharacteristic.PERMISSION_WRITE
                override val initialValue = ByteArray(5) { 
                    if (it == 0) REPORT_ID.toByte() else 0 
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
                        override val value = byteArrayOf(REPORT_ID.toByte(), 0x01) // Input report
                    }
                )
            }
        )
    }
    
    override fun initializeInternal(): Boolean {
        logger.info("Initializing mouse service")
        
        // Clear the report to ensure a clean state
        mouseReport.fill(0)
        mouseReport[0] = REPORT_ID.toByte()
        
        return true
    }
    
    /**
     * Move the mouse pointer by the specified amount.
     *
     * @param x Horizontal movement (-127 to 127)
     * @param y Vertical movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    fun movePointer(x: Int, y: Int): Boolean {
        logger.debug("Moving mouse: x=$x, y=$y")
        
        // Clamp values to the allowed range
        val clampedX = x.coerceIn(MIN_MOVEMENT, MAX_MOVEMENT)
        val clampedY = y.coerceIn(MIN_MOVEMENT, MAX_MOVEMENT)
        
        // Create the report
        val reportData = ByteArray(4)  // buttons, x, y, wheel
        // Keep the current button state
        reportData[0] = mouseReport[1]
        reportData[1] = clampedX.toByte()
        reportData[2] = clampedY.toByte()
        reportData[3] = 0  // No scroll
        
        // Send the report and return the result
        return runBlocking {
            sendReport(REPORT_ID, reportData)
        }.also { 
            // Update the cached report if successful
            if (it) {
                System.arraycopy(reportData, 0, mouseReport, 1, reportData.size)
            }
        }
    }
    
    /**
     * Presses the specified mouse button.
     *
     * @param button The button to press
     * @return true if the report was sent successfully, false otherwise
     */
    fun pressButton(button: MouseButton): Boolean {
        logger.debug("Pressing button: $button")
        
        // Get the button mask
        val mask = when (button) {
            MouseButton.LEFT -> BUTTON_LEFT_MASK
            MouseButton.RIGHT -> BUTTON_RIGHT_MASK
            MouseButton.MIDDLE -> BUTTON_MIDDLE_MASK
        }
        
        // Create the report
        val reportData = ByteArray(4)  // buttons, x, y, wheel
        // Set the button bit
        reportData[0] = (mouseReport[1].toInt() or mask).toByte()
        reportData[1] = 0  // No X movement
        reportData[2] = 0  // No Y movement
        reportData[3] = 0  // No scroll
        
        // Send the report and return the result
        return runBlocking {
            sendReport(REPORT_ID, reportData)
        }.also { 
            // Update the cached report if successful
            if (it) {
                System.arraycopy(reportData, 0, mouseReport, 1, reportData.size)
            }
        }
    }
    
    /**
     * Releases all mouse buttons.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    fun releaseButtons(): Boolean {
        logger.debug("Releasing all buttons")
        
        // Create the report
        val reportData = ByteArray(4)  // buttons, x, y, wheel
        reportData[0] = 0  // No buttons pressed
        reportData[1] = 0  // No X movement
        reportData[2] = 0  // No Y movement
        reportData[3] = 0  // No scroll
        
        // Send the report and return the result
        return runBlocking {
            sendReport(REPORT_ID, reportData)
        }.also { 
            // Update the cached report if successful
            if (it) {
                System.arraycopy(reportData, 0, mouseReport, 1, reportData.size)
            }
        }
    }
    
    /**
     * Clicks the specified mouse button (press and release).
     *
     * @param button The button to click
     * @return true if both press and release were successful, false otherwise
     */
    fun click(button: MouseButton): Boolean {
        logger.debug("Clicking button: $button")
        
        // Press the button
        val pressResult = pressButton(button)
        if (!pressResult) {
            logger.error("Failed to press button: $button")
            return false
        }
        
        // Short delay between press and release
        try {
            Thread.sleep(10)
        } catch (e: InterruptedException) {
            logger.warn("Sleep interrupted during click")
        }
        
        // Release the button
        val releaseResult = releaseButtons()
        if (!releaseResult) {
            logger.error("Failed to release button: $button")
            return false
        }
        
        return true
    }
    
    /**
     * Scrolls the mouse wheel.
     *
     * @param amount Scroll amount (-127 to 127, positive for up, negative for down)
     * @return true if the report was sent successfully, false otherwise
     */
    fun scroll(amount: Int): Boolean {
        logger.debug("Scrolling: $amount")
        
        // Clamp values to the allowed range
        val clampedAmount = amount.coerceIn(MIN_MOVEMENT, MAX_MOVEMENT)
        
        // Create the report
        val reportData = ByteArray(4)  // buttons, x, y, wheel
        reportData[0] = mouseReport[1]  // Keep current button state
        reportData[1] = 0  // No X movement
        reportData[2] = 0  // No Y movement
        reportData[3] = clampedAmount.toByte()  // Scroll amount
        
        // Send the report and return the result
        return runBlocking {
            sendReport(REPORT_ID, reportData)
        }.also { 
            // Update the cached report if successful
            if (it) {
                System.arraycopy(reportData, 0, mouseReport, 1, reportData.size)
            }
        }
    }
    
    override fun shutdownInternal() {
        logger.info("Shutting down mouse service")
        
        // Release any pressed buttons before shutting down
        runBlocking {
            try {
                val reportData = ByteArray(4)
                reportData[0] = 0  // No buttons pressed
                sendReport(REPORT_ID, reportData)
            } catch (e: Exception) {
                logger.error("Error releasing buttons during shutdown", e)
            }
        }
    }
}
