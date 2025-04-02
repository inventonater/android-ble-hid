package com.inventonater.hid.core.internal.compatibility

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.inventonater.hid.core.api.CompatibilityStrategy
import com.inventonater.hid.core.api.LogManager

/**
 * Base class for compatibility strategies.
 *
 * @property logManager Log manager for logging
 * @property deviceName Device name to use for advertising
 */
abstract class BaseCompatibilityStrategy(
    protected val logManager: LogManager,
    private val deviceName: String
) : CompatibilityStrategy {
    
    protected val logger = logManager.getLogger("CompatibilityStrategy")
    
    override fun getDeviceName(): String = deviceName
    
    override fun getHidInformation(): ByteArray {
        // Default HID information
        return byteArrayOf(
            0x11, 0x01,     // HID 1.11
            0x00,           // Not localized
            0x01            // Not normally connectable
        )
    }
    
    override fun configureService(service: BluetoothGattService) {
        // Default implementation does nothing
    }
    
    override fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic): ByteArray? {
        // Default implementation returns null (use default value)
        return null
    }
    
    override fun adaptReport(reportId: Int, report: ByteArray): ByteArray {
        // Default implementation returns the original report
        return report
    }
}

/**
 * Generic compatibility strategy that works with most devices.
 */
class GenericCompatibilityStrategy(logManager: LogManager) : 
    BaseCompatibilityStrategy(logManager, "BLE HID Device") {
    
    init {
        logger.info("Initializing generic compatibility strategy")
    }
    
    override fun adaptReportMap(reportMap: ByteArray): ByteArray {
        // Generic devices typically work with standard HID report descriptors
        return reportMap
    }
}

/**
 * Apple compatibility strategy for macOS, iOS, and iPadOS devices.
 */
class AppleCompatibilityStrategy(logManager: LogManager) : 
    BaseCompatibilityStrategy(logManager, "BLE Keyboard/Mouse") {
    
    init {
        logger.info("Initializing Apple compatibility strategy")
    }
    
    override fun adaptReportMap(reportMap: ByteArray): ByteArray {
        // Apple sometimes requires specific report descriptors
        // For now, just return the original map
        return reportMap
    }
    
    override fun getHidInformation(): ByteArray {
        // Apple-specific HID information
        return byteArrayOf(
            0x01, 0x01,     // HID 1.1
            0x00,           // Not localized
            0x01            // Not normally connectable
        )
    }
}

/**
 * Windows compatibility strategy for Windows devices.
 */
class WindowsCompatibilityStrategy(logManager: LogManager) : 
    BaseCompatibilityStrategy(logManager, "BLE Input Device") {
    
    init {
        logger.info("Initializing Windows compatibility strategy")
    }
    
    override fun adaptReportMap(reportMap: ByteArray): ByteArray {
        // Windows might require specific report descriptors
        // For now, just return the original map
        return reportMap
    }
}
