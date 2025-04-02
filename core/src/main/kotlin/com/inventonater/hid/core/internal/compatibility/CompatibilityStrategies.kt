package com.inventonater.hid.core.internal.compatibility

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.inventonater.hid.core.api.CompatibilityStrategy
import com.inventonater.hid.core.api.LogManager
import java.util.UUID

/**
 * Base class for compatibility strategies.
 *
 * @property logManager The log manager for logging
 */
abstract class BaseCompatibilityStrategy(
    protected val logManager: LogManager
) : CompatibilityStrategy {
    
    protected val logger = logManager.getLogger("Compatibility.${javaClass.simpleName}")
    
    /**
     * Standard HID service UUID.
     */
    protected val HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
    
    /**
     * Standard HID Information UUID.
     */
    protected val HID_INFORMATION_UUID = UUID.fromString("00002A4A-0000-1000-8000-00805f9b34fb")
    
    /**
     * Standard HID Report Map UUID.
     */
    protected val HID_REPORT_MAP_UUID = UUID.fromString("00002A4B-0000-1000-8000-00805f9b34fb")
    
    /**
     * Standard HID Report UUID.
     */
    protected val HID_REPORT_UUID = UUID.fromString("00002A4D-0000-1000-8000-00805f9b34fb")
    
    /**
     * Standard HID Control Point UUID.
     */
    protected val HID_CONTROL_POINT_UUID = UUID.fromString("00002A4C-0000-1000-8000-00805f9b34fb")
    
    /**
     * Find a characteristic in a service by UUID.
     */
    protected fun findCharacteristic(service: BluetoothGattService, uuid: UUID): BluetoothGattCharacteristic? {
        return service.getCharacteristic(uuid)
    }
    
    /**
     * Convert a byte array to a hex string for logging.
     */
    protected fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }
}

/**
 * Generic compatibility strategy for unknown devices.
 * Provides a baseline implementation with minimal customization.
 *
 * @property logManager The log manager for logging
 */
class GenericCompatibilityStrategy(
    logManager: LogManager
) : BaseCompatibilityStrategy(logManager) {
    
    // Standard HID Information
    private val HID_INFORMATION = byteArrayOf(
        0x11, 0x01,     // HID 1.11
        0x00,           // Not localized
        0x01            // Not normally connectable
    )
    
    override fun adaptReportMap(reportMap: ByteArray): ByteArray {
        logger.debug("Using generic report map")
        return reportMap
    }
    
    override fun getHidInformation(): ByteArray {
        logger.debug("Using generic HID information")
        return HID_INFORMATION
    }
    
    override fun getDeviceName(): String {
        return "Inventonater HID Device"
    }
    
    override fun configureService(service: BluetoothGattService) {
        logger.debug("Configuring service with generic compatibility")
        
        // Find and update the HID Information characteristic
        val hidInfoChar = findCharacteristic(service, HID_INFORMATION_UUID)
        if (hidInfoChar != null) {
            hidInfoChar.setValue(getHidInformation())
            logger.debug("Updated HID Information characteristic")
        }
    }
    
    override fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic): ByteArray? {
        // Let the default implementation handle it
        return null
    }
    
    override fun adaptReport(reportId: Int, report: ByteArray): ByteArray {
        // No adaptation needed for generic devices
        return report
    }
}

/**
 * Apple compatibility strategy for macOS and iOS devices.
 * Provides specific adaptations for better compatibility with Apple devices.
 *
 * @property logManager The log manager for logging
 */
class AppleCompatibilityStrategy(
    logManager: LogManager
) : BaseCompatibilityStrategy(logManager) {
    
    // Apple-specific HID Information with RemoteWake and NormallyConnectable flags
    private val APPLE_HID_INFORMATION = byteArrayOf(
        0x11, 0x01,     // HID 1.11
        0x00,           // Not localized
        0x03            // RemoteWake + NormallyConnectable
    )
    
    // Apple-specific service UUID used alongside the standard HID service
    private val APPLE_HID_IDENTIFIER_UUID = UUID.fromString("9FA480E0-4967-4542-9390-D343DC5D04AE")
    
    override fun adaptReportMap(reportMap: ByteArray): ByteArray {
        logger.debug("Adapting report map for Apple compatibility")
        
        // If this is a mouse report map, apply specific modifications
        if (isMouseReportMap(reportMap)) {
            return adaptMouseReportMap(reportMap)
        }
        
        return reportMap
    }
    
    /**
     * Determines if the report map is for a mouse device.
     */
    private fun isMouseReportMap(reportMap: ByteArray): Boolean {
        // Simple heuristic: look for mouse usage page and usage
        // This is not comprehensive but works for our standard report maps
        for (i in 0 until reportMap.size - 3) {
            // Check for Usage Page (Generic Desktop) followed by Usage (Mouse)
            if (reportMap[i] == 0x05.toByte() && reportMap[i + 1] == 0x01.toByte() &&
                reportMap[i + 2] == 0x09.toByte() && reportMap[i + 3] == 0x02.toByte()) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Adapts a mouse report map for better compatibility with Apple devices.
     */
    private fun adaptMouseReportMap(reportMap: ByteArray): ByteArray {
        logger.debug("Adapting mouse report map for Apple compatibility")
        
        // For now, just return the original report map
        // A real implementation would modify specific parts of the report map
        // based on known Apple compatibility requirements
        
        // Example modifications (not implemented):
        // 1. Change constant padding INPUT from 0x03 to 0x01
        // 2. Adjust report IDs if needed
        // 3. Add any Apple-specific page or usage references
        
        return reportMap
    }
    
    override fun getHidInformation(): ByteArray {
        logger.debug("Using Apple-specific HID information")
        return APPLE_HID_INFORMATION
    }
    
    override fun getDeviceName(): String {
        return "Inventonater HID Mouse"
    }
    
    override fun configureService(service: BluetoothGattService) {
        logger.debug("Configuring service with Apple compatibility")
        
        // Find and update the HID Information characteristic
        val hidInfoChar = findCharacteristic(service, HID_INFORMATION_UUID)
        if (hidInfoChar != null) {
            hidInfoChar.setValue(getHidInformation())
            logger.debug("Updated HID Information characteristic")
        }
        
        // Find and update the Report Map characteristic
        val reportMapChar = findCharacteristic(service, HID_REPORT_MAP_UUID)
        if (reportMapChar != null) {
            val originalReportMap = reportMapChar.value
            if (originalReportMap != null) {
                val adaptedReportMap = adaptReportMap(originalReportMap)
                reportMapChar.setValue(adaptedReportMap)
                logger.debug("Updated Report Map characteristic for Apple compatibility")
            }
        }
    }
    
    override fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic): ByteArray? {
        // Special handling for certain characteristics on Apple devices
        
        when (characteristic.uuid) {
            HID_INFORMATION_UUID -> {
                return getHidInformation()
            }
            else -> {
                // Let the default implementation handle it
                return null
            }
        }
    }
    
    override fun adaptReport(reportId: Int, report: ByteArray): ByteArray {
        // For now, no report adaptation needed for Apple devices
        return report
    }
}

/**
 * Windows compatibility strategy for Windows PCs.
 * Provides specific adaptations for better compatibility with Windows devices.
 *
 * @property logManager The log manager for logging
 */
class WindowsCompatibilityStrategy(
    logManager: LogManager
) : BaseCompatibilityStrategy(logManager) {
    
    // Standard HID Information with RemoteWake flag
    private val WINDOWS_HID_INFORMATION = byteArrayOf(
        0x11, 0x01,     // HID 1.11
        0x00,           // Not localized
        0x02            // RemoteWake
    )
    
    override fun adaptReportMap(reportMap: ByteArray): ByteArray {
        logger.debug("Using standard report map for Windows")
        return reportMap
    }
    
    override fun getHidInformation(): ByteArray {
        logger.debug("Using Windows-specific HID information")
        return WINDOWS_HID_INFORMATION
    }
    
    override fun getDeviceName(): String {
        return "Inventonater HID Device"
    }
    
    override fun configureService(service: BluetoothGattService) {
        logger.debug("Configuring service with Windows compatibility")
        
        // Find and update the HID Information characteristic
        val hidInfoChar = findCharacteristic(service, HID_INFORMATION_UUID)
        if (hidInfoChar != null) {
            hidInfoChar.setValue(getHidInformation())
            logger.debug("Updated HID Information characteristic")
        }
    }
    
    override fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic): ByteArray? {
        // Let the default implementation handle it
        return null
    }
    
    override fun adaptReport(reportId: Int, report: ByteArray): ByteArray {
        // No adaptation needed for Windows devices
        return report
    }
}
