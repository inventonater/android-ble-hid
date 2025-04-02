package com.inventonater.hid.core.internal

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.inventonater.hid.core.api.BleConnectionManager
import com.inventonater.hid.core.api.BleHidManager
import com.inventonater.hid.core.api.ConnectionListener
import com.inventonater.hid.core.api.ConnectionState
import com.inventonater.hid.core.api.DeviceCompatibilityManager
import com.inventonater.hid.core.api.DiagnosticsManager
import com.inventonater.hid.core.api.HidServiceBase
import com.inventonater.hid.core.api.MouseButton
import com.inventonater.hid.core.api.ServiceFactory
import com.inventonater.hid.core.internal.ble.BleAdvertiserImpl
import com.inventonater.hid.core.internal.service.MouseService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of the BleHidManager interface.
 *
 * This class is the central coordinator for the BLE HID system.
 *
 * @property connectionManager Manager for BLE connections
 * @property deviceCompatibilityManager Manager for device compatibility
 * @property serviceFactory Factory for creating HID services
 * @property diagnosticsManager Manager for diagnostics
 */
class BleHidManagerImpl(
    private val connectionManager: BleConnectionManager,
    private val deviceCompatibilityManager: DeviceCompatibilityManager,
    private val serviceFactory: ServiceFactory,
    private val diagnosticsManager: DiagnosticsManager
) : BleHidManager {
    
    private val logger = diagnosticsManager.logManager.getLogger("BleHidManager")
    
    // Coroutine scope for asynchronous operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // BLE advertiser
    private lateinit var advertiser: BleAdvertiserImpl
    
    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Application context
    private lateinit var context: Context
    
    // Bluetooth adapter
    private var bluetoothAdapter: BluetoothAdapter? = null
    
    // Flag indicating if initialized
    private var initialized = false
    
    // Active services
    private val activeServices = mutableMapOf<String, HidServiceBase>()
    
    // Connection listeners
    private val connectionListeners = CopyOnWriteArrayList<ConnectionListener>()
    
    override fun initialize(context: Context): Boolean {
        if (initialized) {
            logger.warn("Already initialized")
            return true
        }
        
        logger.info("Initializing BLE HID Manager")
        
        this.context = context.applicationContext
        
        // Get Bluetooth adapter
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?
        if (bluetoothManager == null) {
            logger.error("Bluetooth manager not found")
            return false
        }
        
        bluetoothAdapter = bluetoothManager.adapter
        if (bluetoothAdapter == null) {
            logger.error("Bluetooth adapter not found")
            return false
        }
        
        if (!bluetoothAdapter!!.isEnabled) {
            logger.error("Bluetooth is not enabled")
            return false
        }
        
        if (!isBlePeripheralSupported()) {
            logger.error("BLE peripheral mode not supported")
            return false
        }
        
        // Initialize connection manager
        if (!connectionManager.initialize(context)) {
            logger.error("Failed to initialize connection manager")
            return false
        }
        
        // Create advertiser
        advertiser = BleAdvertiserImpl(
            bluetoothAdapter!!,
            diagnosticsManager.logManager
        )
        
        // Subscribe to connection state changes
        connectionManager.addConnectionStateListener { newState ->
            _connectionState.value = newState
            
            // Notify connection listeners
            when (newState) {
                is ConnectionState.Connected -> {
                    for (listener in connectionListeners) {
                        listener.onDeviceConnected(newState.device)
                    }
                }
                is ConnectionState.Disconnected -> {
                    val device = (_connectionState.value as? ConnectionState.Connected)?.device
                    if (device != null) {
                        for (listener in connectionListeners) {
                            listener.onDeviceDisconnected(device)
                        }
                    }
                }
                else -> {
                    // No action for other states
                }
            }
            
            // Handle connection state changes
            when (newState) {
                is ConnectionState.Connected -> {
                    // Stop advertising when connected
                    stopAdvertising()
                }
                is ConnectionState.Disconnected -> {
                    // Restart advertising when disconnected
                    if (!isAdvertising()) {
                        startAdvertising()
                    }
                }
                else -> {
                    // No action for other states
                }
            }
        }
        
        // Register standard services
        registerServiceFactory(MouseService.SERVICE_ID) {
            MouseService(connectionManager, diagnosticsManager.logManager)
        }
        
        initialized = true
        logger.info("BLE HID Manager initialized successfully")
        return true
    }
    
    override fun startAdvertising(): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        logger.info("Starting BLE advertising")
        
        // Check if any services are active
        if (activeServices.isEmpty()) {
            logger.error("No active services")
            return false
        }
        
        // Get device name from compatibility manager
        val deviceName = deviceCompatibilityManager.getCurrentDeviceType().let { deviceType ->
            deviceCompatibilityManager.getStrategyForDevice(
                getConnectedDevice() ?: bluetoothAdapter!!.getRemoteDevice("00:00:00:00:00:00")
            ).getDeviceName()
        }
        
        // Set device name
        bluetoothAdapter!!.name = deviceName
        
        // Start advertising
        return advertiser.startAdvertising()
    }
    
    override fun stopAdvertising() {
        if (initialized) {
            logger.info("Stopping BLE advertising")
            advertiser.stopAdvertising()
        }
    }
    
    override fun isAdvertising(): Boolean {
        return initialized && advertiser.isAdvertising()
    }
    
    override fun isConnected(): Boolean {
        return connectionState.value is ConnectionState.Connected
    }
    
    override fun getConnectedDevice(): BluetoothDevice? {
        return (connectionState.value as? ConnectionState.Connected)?.device
    }
    
    override fun close() {
        if (!initialized) {
            return
        }
        
        logger.info("Closing BLE HID Manager")
        
        // Stop advertising
        stopAdvertising()
        
        // Shutdown all active services
        for (service in activeServices.values) {
            try {
                service.shutdown()
            } catch (e: Exception) {
                logger.error("Error shutting down service: ${service.serviceId}", e)
            }
        }
        activeServices.clear()
        
        // Close connection manager
        connectionManager.close()
        
        // Clear listeners
        connectionListeners.clear()
        
        initialized = false
        logger.info("BLE HID Manager closed")
    }
    
    override fun isBlePeripheralSupported(): Boolean {
        val adapter = bluetoothAdapter ?: return false
        
        // Check if multiple advertisement is supported
        return adapter.isMultipleAdvertisementSupported
    }
    
    override fun addConnectionListener(listener: ConnectionListener) {
        connectionListeners.add(listener)
    }
    
    override fun removeConnectionListener(listener: ConnectionListener) {
        connectionListeners.remove(listener)
    }
    
    override fun registerServiceFactory(serviceId: String, factory: () -> HidServiceBase) {
        logger.info("Registering service factory: $serviceId")
        serviceFactory.register(serviceId, factory)
    }
    
    override fun activateService(serviceId: String): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        logger.info("Activating service: $serviceId")
        
        // Check if already active
        if (activeServices.containsKey(serviceId)) {
            logger.warn("Service already active: $serviceId")
            return true
        }
        
        // Create the service
        val service = serviceFactory.create(serviceId)
        if (service == null) {
            logger.error("Failed to create service: $serviceId")
            return false
        }
        
        // Initialize the service
        if (!service.initialize()) {
            logger.error("Failed to initialize service: $serviceId")
            return false
        }
        
        // Add to active services
        activeServices[serviceId] = service
        logger.info("Service activated: $serviceId")
        
        return true
    }
    
    override fun deactivateService(serviceId: String) {
        if (!initialized) {
            logger.warn("Not initialized")
            return
        }
        
        logger.info("Deactivating service: $serviceId")
        
        // Get the service
        val service = activeServices[serviceId]
        if (service == null) {
            logger.warn("Service not active: $serviceId")
            return
        }
        
        // Shutdown the service
        service.shutdown()
        
        // Remove from active services
        activeServices.remove(serviceId)
        logger.info("Service deactivated: $serviceId")
    }
    
    override fun getAvailableServices(): List<String> {
        return serviceFactory.getRegisteredServiceIds()
    }
    
    override fun getActiveServices(): List<String> {
        return activeServices.keys.toList()
    }
    
    // Mouse functionality
    
    override fun moveMouse(x: Int, y: Int): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        // Get mouse service
        val mouseService = activeServices[MouseService.SERVICE_ID] as? MouseService
        if (mouseService == null) {
            logger.error("Mouse service not active")
            return false
        }
        
        // Move the mouse
        return mouseService.movePointer(x, y)
    }
    
    override fun clickMouseButton(button: MouseButton): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        // Get mouse service
        val mouseService = activeServices[MouseService.SERVICE_ID] as? MouseService
        if (mouseService == null) {
            logger.error("Mouse service not active")
            return false
        }
        
        // Click the button
        return mouseService.click(button)
    }
    
    override fun pressMouseButton(button: MouseButton): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        // Get mouse service
        val mouseService = activeServices[MouseService.SERVICE_ID] as? MouseService
        if (mouseService == null) {
            logger.error("Mouse service not active")
            return false
        }
        
        // Press the button
        return mouseService.pressButton(button)
    }
    
    override fun releaseMouseButtons(): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        // Get mouse service
        val mouseService = activeServices[MouseService.SERVICE_ID] as? MouseService
        if (mouseService == null) {
            logger.error("Mouse service not active")
            return false
        }
        
        // Release buttons
        return mouseService.releaseButtons()
    }
    
    override fun scrollMouseWheel(amount: Int): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        // Get mouse service
        val mouseService = activeServices[MouseService.SERVICE_ID] as? MouseService
        if (mouseService == null) {
            logger.error("Mouse service not active")
            return false
        }
        
        // Scroll the wheel
        return mouseService.scroll(amount)
    }
    
    // Keyboard functionality - these would be implemented similarly to the mouse functions
    // For now, we'll provide stub implementations since we haven't implemented KeyboardService yet
    
    override fun sendKey(keyCode: Int): Boolean {
        logger.warn("Keyboard service not implemented yet")
        return false
    }
    
    override fun sendKeys(keyCodes: IntArray): Boolean {
        logger.warn("Keyboard service not implemented yet")
        return false
    }
    
    override fun releaseKeys(): Boolean {
        logger.warn("Keyboard service not implemented yet")
        return false
    }
}
