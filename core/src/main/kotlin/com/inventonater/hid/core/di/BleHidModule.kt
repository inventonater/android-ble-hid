package com.inventonater.hid.core.di

import android.content.Context
import com.inventonater.hid.core.api.BleConnectionManager
import com.inventonater.hid.core.api.BleHidManager
import com.inventonater.hid.core.api.DeviceCompatibilityManager
import com.inventonater.hid.core.api.DeviceDetector
import com.inventonater.hid.core.api.DiagnosticsConfig
import com.inventonater.hid.core.api.DiagnosticsManager
import com.inventonater.hid.core.api.LogLevel
import com.inventonater.hid.core.api.ServiceFactory
import com.inventonater.hid.core.internal.BleHidManagerImpl
import com.inventonater.hid.core.internal.ble.BleConnectionManagerImpl
import com.inventonater.hid.core.internal.compatibility.AppleCompatibilityStrategy
import com.inventonater.hid.core.internal.compatibility.DeviceCompatibilityManagerImpl
import com.inventonater.hid.core.internal.compatibility.DeviceDetectorImpl
import com.inventonater.hid.core.internal.compatibility.GenericCompatibilityStrategy
import com.inventonater.hid.core.internal.compatibility.WindowsCompatibilityStrategy
import com.inventonater.hid.core.internal.diagnostics.DiagnosticsManagerImpl
import com.inventonater.hid.core.internal.service.ServiceFactoryImpl

/**
 * Dependency injection module for the BLE HID system.
 * 
 * This class is responsible for creating and initializing all the components
 * needed for the BLE HID system to work properly.
 */
object BleHidModule {
    
    // Lazily initialized singleton instances
    private var diagnosticsManager: DiagnosticsManager? = null
    private var serviceFactory: ServiceFactory? = null
    private var deviceCompatibilityManager: DeviceCompatibilityManager? = null
    private var connectionManager: BleConnectionManager? = null
    private var bleHidManager: BleHidManager? = null
    
    /**
     * Initialize all components needed for the BLE HID system.
     * 
     * @param context Application context
     * @param logLevel Log level for diagnostics
     * @return BleHidManager instance that can be used to interact with the system
     */
    @Synchronized
    fun initialize(context: Context, logLevel: LogLevel = LogLevel.INFO): BleHidManager {
        // Check if already initialized
        if (bleHidManager != null) {
            return bleHidManager!!
        }
        
        // Create diagnostics manager first
        val diagManager = createDiagnosticsManager(context, logLevel)
        val logger = diagManager.logManager.getLogger("BleHidModule")
        
        logger.info("Initializing BLE HID Module")
        
        // Create components
        val factory = createServiceFactory(diagManager)
        val deviceManager = createDeviceCompatibilityManager(diagManager)
        val connManager = createConnectionManager(diagManager)
        
        // Create the BleHidManager
        val hidManager = BleHidManagerImpl(
            connectionManager = connManager,
            deviceCompatibilityManager = deviceManager,
            serviceFactory = factory,
            diagnosticsManager = diagManager
        )
        
        // Store instances
        diagnosticsManager = diagManager
        serviceFactory = factory
        deviceCompatibilityManager = deviceManager
        connectionManager = connManager
        bleHidManager = hidManager
        
        logger.info("BLE HID Module initialized successfully")
        
        return hidManager
    }
    
    /**
     * Create the diagnostics manager.
     * 
     * @param context Application context
     * @param logLevel Log level
     * @return DiagnosticsManager instance
     */
    private fun createDiagnosticsManager(
        context: Context,
        logLevel: LogLevel
    ): DiagnosticsManager {
        // Create and initialize diagnostics manager
        val manager = DiagnosticsManagerImpl(context)
        
        val config = DiagnosticsConfig(
            logLevel = logLevel,
            enableReportMonitoring = true,
            enableConnectionMonitoring = true,
            enablePerformanceTracking = true
        )
        
        manager.initialize(config)
        
        // Start a session
        manager.startSession("BleHid")
        
        return manager
    }
    
    /**
     * Create the service factory.
     * 
     * @param diagManager Diagnostics manager
     * @return ServiceFactory instance
     */
    private fun createServiceFactory(
        diagManager: DiagnosticsManager
    ): ServiceFactory {
        return ServiceFactoryImpl(diagManager.logManager)
    }
    
    /**
     * Create the device compatibility manager.
     * 
     * @param diagManager Diagnostics manager
     * @return DeviceCompatibilityManager instance
     */
    private fun createDeviceCompatibilityManager(
        diagManager: DiagnosticsManager
    ): DeviceCompatibilityManager {
        // Create device detector
        val detector = createDeviceDetector(diagManager)
        
        // Create default strategy
        val defaultStrategy = GenericCompatibilityStrategy(diagManager.logManager)
        
        // Create manager
        val manager = DeviceCompatibilityManagerImpl(
            logManager = diagManager.logManager,
            deviceDetector = detector,
            defaultStrategy = defaultStrategy
        )
        
        // Register strategies
        manager.registerStrategy(
            deviceType = com.inventonater.hid.core.api.DeviceType.APPLE,
            strategy = AppleCompatibilityStrategy(diagManager.logManager)
        )
        
        manager.registerStrategy(
            deviceType = com.inventonater.hid.core.api.DeviceType.WINDOWS,
            strategy = WindowsCompatibilityStrategy(diagManager.logManager)
        )
        
        return manager
    }
    
    /**
     * Create the device detector.
     * 
     * @param diagManager Diagnostics manager
     * @return DeviceDetector instance
     */
    private fun createDeviceDetector(
        diagManager: DiagnosticsManager
    ): DeviceDetector {
        return DeviceDetectorImpl(diagManager.logManager)
    }
    
    /**
     * Create the BLE connection manager.
     * 
     * @param diagManager Diagnostics manager
     * @return BleConnectionManager instance
     */
    private fun createConnectionManager(
        diagManager: DiagnosticsManager
    ): BleConnectionManager {
        return BleConnectionManagerImpl(diagManager.logManager)
    }
    
    /**
     * Get the BleHidManager instance.
     * 
     * @return BleHidManager instance, or null if not initialized
     */
    fun getBleHidManager(): BleHidManager? {
        return bleHidManager
    }
    
    /**
     * Shut down all components.
     */
    @Synchronized
    fun shutdown() {
        bleHidManager?.close()
        connectionManager = null
        deviceCompatibilityManager = null
        serviceFactory = null
        diagnosticsManager?.close()
        
        // Clear references
        bleHidManager = null
        diagnosticsManager = null
    }
}
