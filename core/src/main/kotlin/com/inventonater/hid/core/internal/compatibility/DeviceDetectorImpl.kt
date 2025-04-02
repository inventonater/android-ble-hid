package com.inventonater.hid.core.internal.compatibility

import android.bluetooth.BluetoothDevice
import com.inventonater.hid.core.api.DeviceDetector
import com.inventonater.hid.core.api.DeviceType
import com.inventonater.hid.core.api.LogManager
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * Implementation of the DeviceDetector interface.
 * 
 * This class is responsible for detecting device types based on
 * Bluetooth device information.
 */
class DeviceDetectorImpl(private val logManager: LogManager) : DeviceDetector {
    private val logger = logManager.getLogger("DeviceDetector")
    
    // Registry of detection strategies
    private data class StrategyEntry(
        val strategy: (BluetoothDevice) -> DeviceType?,
        val priority: Int
    )
    
    private val strategies = ConcurrentHashMap<String, StrategyEntry>()
    
    // Current device type
    private val currentDeviceType = AtomicReference(DeviceType.UNKNOWN)
    
    init {
        // Register default detection strategies
        registerStrategyInternal("apple", { device ->
            if (isAppleDevice(device)) DeviceType.APPLE else null
        }, 100)
        
        registerStrategyInternal("windows", { device ->
            if (isWindowsDevice(device)) DeviceType.WINDOWS else null
        }, 90)
        
        registerStrategyInternal("linux", { device ->
            if (isLinuxDevice(device)) DeviceType.LINUX else null
        }, 80)
        
        registerStrategyInternal("android", { device ->
            if (isAndroidDevice(device)) DeviceType.ANDROID else null
        }, 70)
        
        registerStrategyInternal("chromeos", { device ->
            if (isChromeOSDevice(device)) DeviceType.CHROMEOS else null
        }, 60)
    }
    
    override fun detectDeviceType(device: BluetoothDevice): DeviceType {
        logger.debug("Detecting device type for ${device.address}")
        
        // Sort strategies by priority (higher first)
        val sortedStrategies = strategies.values.sortedByDescending { it.priority }
        
        // Try each strategy
        for (entry in sortedStrategies) {
            try {
                val deviceType = entry.strategy(device)
                if (deviceType != null) {
                    logger.info("Detected device type: $deviceType for ${device.address}")
                    currentDeviceType.set(deviceType)
                    return deviceType
                }
            } catch (e: Exception) {
                logger.error("Error in detection strategy", e)
            }
        }
        
        // Default to unknown
        logger.info("No device type detected, using UNKNOWN for ${device.address}")
        currentDeviceType.set(DeviceType.UNKNOWN)
        return DeviceType.UNKNOWN
    }
    
    private fun registerStrategyInternal(
        strategyId: String,
        strategy: (BluetoothDevice) -> DeviceType?,
        priority: Int
    ): Boolean {
        if (strategies.containsKey(strategyId)) {
            logger.warn("Strategy already registered: $strategyId")
            return false
        }
        
        strategies[strategyId] = StrategyEntry(strategy, priority)
        logger.info("Registered detection strategy: $strategyId with priority $priority")
        return true
    }
    
    // Detection helpers
    
    private fun isAppleDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.lowercase() ?: ""
        return name.contains("mac") || 
               name.contains("iphone") || 
               name.contains("ipad") || 
               name.contains("apple")
    }
    
    private fun isWindowsDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.lowercase() ?: ""
        return name.contains("windows") || 
               name.contains("pc") || 
               name.contains("surface") || 
               name.contains("microsoft")
    }
    
    private fun isLinuxDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.lowercase() ?: ""
        return name.contains("linux") || 
               name.contains("ubuntu") || 
               name.contains("fedora") || 
               name.contains("debian")
    }
    
    private fun isAndroidDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.lowercase() ?: ""
        return name.contains("android") || 
               name.contains("pixel") || 
               name.contains("samsung") || 
               name.contains("galaxy")
    }
    
    private fun isChromeOSDevice(device: BluetoothDevice): Boolean {
        val name = device.name?.lowercase() ?: ""
        return name.contains("chromebook") || 
               name.contains("chrome") || 
               name.contains("chromeos") || 
               name.contains("google")
    }
}
