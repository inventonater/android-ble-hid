package com.inventonater.hid.core.internal.compatibility

import android.bluetooth.BluetoothDevice
import android.os.Build
import com.inventonater.hid.core.api.CompatibilityStrategy
import com.inventonater.hid.core.api.DeviceCompatibilityManager
import com.inventonater.hid.core.api.DeviceDetector
import com.inventonater.hid.core.api.DeviceType
import com.inventonater.hid.core.api.LogManager

/**
 * Implementation of DeviceCompatibilityManager that manages compatibility strategies for different devices.
 *
 * @property logManager The log manager for logging
 * @property deviceDetector The device detector for detecting device types
 * @property defaultStrategy The default compatibility strategy
 */
class DeviceCompatibilityManagerImpl(
    private val logManager: LogManager,
    private val deviceDetector: DeviceDetector,
    private val defaultStrategy: CompatibilityStrategy
) : DeviceCompatibilityManager {
    
    private val logger = logManager.getLogger("DeviceCompatibilityManager")
    
    // Compatibility strategies for different device types
    private val strategies = mutableMapOf<DeviceType, CompatibilityStrategy>()
    
    // Current device type override
    private var deviceTypeOverride: DeviceType? = null
    
    init {
        // Register the default strategy for UNKNOWN device type
        strategies[DeviceType.UNKNOWN] = defaultStrategy
    }
    
    override fun getStrategyForDevice(device: BluetoothDevice): CompatibilityStrategy {
        // If there's an override, use that
        if (deviceTypeOverride != null) {
            logger.info("Using device type override: $deviceTypeOverride")
            return strategies[deviceTypeOverride] ?: defaultStrategy
        }
        
        // Otherwise, detect the device type
        val deviceType = deviceDetector.detectDeviceType(device)
        logger.info("Detected device type: $deviceType")
        
        // Return the appropriate strategy
        return strategies[deviceType] ?: defaultStrategy
    }
    
    override fun setDeviceTypeOverride(deviceType: DeviceType): CompatibilityStrategy {
        logger.info("Setting device type override: $deviceType")
        deviceTypeOverride = deviceType
        return strategies[deviceType] ?: defaultStrategy
    }
    
    override fun clearDeviceTypeOverride() {
        logger.info("Clearing device type override")
        deviceTypeOverride = null
    }
    
    override fun registerStrategy(deviceType: DeviceType, strategy: CompatibilityStrategy) {
        logger.info("Registering compatibility strategy for device type: $deviceType")
        strategies[deviceType] = strategy
    }
    
    override fun getCurrentDeviceType(): DeviceType {
        return deviceTypeOverride ?: DeviceType.UNKNOWN
    }
}

/**
 * Implementation of DeviceDetector that detects device types.
 *
 * @property logManager The log manager for logging
 */
class DeviceDetectorImpl(private val logManager: LogManager) : DeviceDetector {
    
    private val logger = logManager.getLogger("DeviceDetector")
    
    override fun detectDeviceType(device: BluetoothDevice): DeviceType {
        logger.debug("Detecting device type for: ${device.address}")
        
        // Try to detect the device type based on available information
        val deviceName = device.name ?: ""
        
        // Check for Apple devices
        if (deviceName.contains("Mac") || deviceName.contains("iPhone") || deviceName.contains("iPad")) {
            logger.info("Detected Apple device: $deviceName")
            return DeviceType.APPLE
        }
        
        // Check for Windows devices
        if (deviceName.contains("Windows") || deviceName.contains("PC")) {
            logger.info("Detected Windows device: $deviceName")
            return DeviceType.WINDOWS
        }
        
        // Check for Android devices
        if (deviceName.contains("Android")) {
            logger.info("Detected Android device: $deviceName")
            return DeviceType.ANDROID
        }
        
        // Unable to determine device type
        logger.info("Unable to determine device type for: $deviceName")
        return DeviceType.UNKNOWN
    }
}
