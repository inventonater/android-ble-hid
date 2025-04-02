package com.inventonater.hid.core.internal.compatibility

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import com.inventonater.hid.core.api.CompatibilityStrategy
import com.inventonater.hid.core.api.DeviceCompatibilityManager
import com.inventonater.hid.core.api.DeviceDetector
import com.inventonater.hid.core.api.DeviceType
import com.inventonater.hid.core.api.LogManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Implementation of the DeviceCompatibilityManager interface.
 *
 * This class manages compatibility strategies for different device types.
 */
class DeviceCompatibilityManagerImpl(
    private val logManager: LogManager,
    private val deviceDetector: DeviceDetector,
    private val defaultStrategy: CompatibilityStrategy
) : DeviceCompatibilityManager {
    
    private val logger = logManager.getLogger("DeviceCompatibilityManager")
    
    // Map of device types to compatibility strategies
    private val strategies = ConcurrentHashMap<DeviceType, CompatibilityStrategy>()
    
    // Cache of device addresses to compatibility strategies
    private val deviceCache = ConcurrentHashMap<String, CompatibilityStrategy>()
    
    // Device type override (if set)
    private val deviceTypeOverride = AtomicReference<DeviceType?>(null)
    
    override fun getStrategyForDevice(device: BluetoothDevice): CompatibilityStrategy {
        // Check cache first
        val cachedStrategy = deviceCache[device.address]
        if (cachedStrategy != null) {
            return cachedStrategy
        }
        
        // If there's an override, use that
        val override = deviceTypeOverride.get()
        if (override != null) {
            val strategy = strategies[override] ?: defaultStrategy
            deviceCache[device.address] = strategy
            return strategy
        }
        
        // Detect device type
        val deviceType = deviceDetector.detectDeviceType(device)
        
        // Get strategy for device type
        val strategy = strategies[deviceType] ?: defaultStrategy
        
        // Cache the strategy
        deviceCache[device.address] = strategy
        
        logger.info("Using compatibility strategy for ${device.address}: ${strategy.getDeviceName()}")
        return strategy
    }
    
    override fun setDeviceTypeOverride(deviceType: DeviceType): CompatibilityStrategy {
        logger.info("Setting device type override to $deviceType")
        deviceTypeOverride.set(deviceType)
        
        // Clear device cache
        deviceCache.clear()
        
        return strategies[deviceType] ?: defaultStrategy
    }
    
    override fun clearDeviceTypeOverride() {
        logger.info("Clearing device type override")
        deviceTypeOverride.set(null)
        
        // Clear device cache
        deviceCache.clear()
    }
    
    override fun registerStrategy(deviceType: DeviceType, strategy: CompatibilityStrategy) {
        if (strategies.containsKey(deviceType)) {
            logger.warn("Replacing existing strategy for device type: $deviceType")
        }
        
        strategies[deviceType] = strategy
        logger.info("Registered compatibility strategy for $deviceType: ${strategy.getDeviceName()}")
        
        // Clear device cache
        deviceCache.clear()
    }
    
    override fun getCurrentDeviceType(): DeviceType {
        val override = deviceTypeOverride.get()
        if (override != null) {
            return override
        }
        
        // Just return UNKNOWN since we don't have a specific device to check
        return DeviceType.UNKNOWN
    }
}
