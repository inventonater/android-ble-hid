package com.inventonater.hid.core.internal.ble

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import com.inventonater.hid.core.api.BleConnectionManager
import com.inventonater.hid.core.api.ConnectionState
import com.inventonater.hid.core.api.ConnectionStateListener
import com.inventonater.hid.core.api.GattOperation
import com.inventonater.hid.core.api.LogManager
import com.inventonater.hid.core.api.NotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of BleConnectionManager that handles BLE GATT server operations.
 *
 * @property logManager The log manager for logging
 */
class BleConnectionManagerImpl(
    private val logManager: LogManager
) : BleConnectionManager, NotificationManager {
    
    private val logger = logManager.getLogger("BleConnectionManager")
    
    // Coroutine scope for asynchronous operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // GATT server
    private var gattServer: BluetoothGattServer? = null
    
    // Connection state
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // GATT operations
    private val _gattOperations = MutableSharedFlow<GattOperation>(extraBufferCapacity = 10)
    override val gattOperations: Flow<GattOperation> = _gattOperations.asSharedFlow()
    
    // Connection state listeners
    private val connectionStateListeners = CopyOnWriteArrayList<ConnectionStateListener>()
    
    // Connected device
    private var connectedDevice: BluetoothDevice? = null
    
    // Context
    private lateinit var context: Context
    
    // Bluetooth manager
    private lateinit var bluetoothManager: BluetoothManager
    
    // Notification queuing
    private val notificationQueue = ArrayDeque<NotificationRequest>()
    private var notificationInProgress = false
    
    // Characteristic cache for notification enablement tracking
    private val notificationEnabledCharacteristics = mutableSetOf<UUID>()
    
    // Flag indicating if initialized
    private var initialized = false
    
    override fun initialize(context: Context): Boolean {
        if (initialized) {
            logger.warn("Already initialized")
            return true
        }
        
        logger.info("Initializing BLE Connection Manager")
        
        this.context = context.applicationContext
        
        // Get Bluetooth manager
        bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            ?: run {
                logger.error("Bluetooth manager not found")
                return false
            }
        
        try {
            // Open GATT server
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
            if (gattServer == null) {
                logger.error("Failed to open GATT server")
                return false
            }
            
            initialized = true
            logger.info("BLE Connection Manager initialized successfully")
            return true
        } catch (e: Exception) {
            logger.error("Failed to initialize BLE Connection Manager", e)
            return false
        }
    }
    
    override fun addService(service: BluetoothGattService): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        logger.info("Adding service: ${service.uuid}")
        
        try {
            val result = gattServer?.addService(service) ?: false
            if (result) {
                logger.info("Service added successfully: ${service.uuid}")
                
                // Emit service added operation
                scope.launch {
                    _gattOperations.emit(GattOperation.ServiceAdded(service))
                }
            } else {
                logger.error("Failed to add service: ${service.uuid}")
            }
            
            return result
        } catch (e: Exception) {
            logger.error("Error adding service: ${service.uuid}", e)
            return false
        }
    }
    
    override fun removeService(serviceUuid: String): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        logger.info("Removing service: $serviceUuid")
        
        try {
            // Find the service
            val serviceUuidObj = UUID.fromString(serviceUuid)
            val service = gattServer?.getService(serviceUuidObj)
            
            if (service == null) {
                logger.warn("Service not found: $serviceUuid")
                return false
            }
            
            // Remove the service
            gattServer?.removeService(service)
            
            logger.info("Service removed: $serviceUuid")
            
            // Emit service removed operation
            scope.launch {
                _gattOperations.emit(GattOperation.ServiceRemoved(serviceUuid))
            }
            
            return true
        } catch (e: Exception) {
            logger.error("Error removing service: $serviceUuid", e)
            return false
        }
    }
    
    override suspend fun sendNotification(characteristicUuid: String, value: ByteArray): Result<Unit> {
        if (!initialized) {
            logger.error("Not initialized")
            return Result.failure(IllegalStateException("Not initialized"))
        }
        
        val characteristic = findCharacteristicByUuid(UUID.fromString(characteristicUuid))
            ?: return Result.failure(IllegalArgumentException("Characteristic not found: $characteristicUuid"))
        
        return sendNotification(characteristic, value)
    }
    
    override suspend fun sendNotification(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray
    ): Result<Unit> {
        if (!initialized) {
            logger.error("Not initialized")
            return Result.failure(IllegalStateException("Not initialized"))
        }
        
        val device = connectedDevice
        if (device == null) {
            logger.error("No connected device")
            return Result.failure(IllegalStateException("No connected device"))
        }
        
        // Check if notifications are enabled for this characteristic
        if (!areNotificationsEnabled(characteristic)) {
            logger.warn("Notifications not enabled for characteristic: ${characteristic.uuid}")
            // Try to enable notifications
            if (!enableNotifications(characteristic)) {
                logger.error("Failed to enable notifications for characteristic: ${characteristic.uuid}")
                return Result.failure(IllegalStateException("Notifications not enabled for characteristic: ${characteristic.uuid}"))
            }
        }
        
        logger.debug("Sending notification for characteristic: ${characteristic.uuid}")
        
        try {
            // Set the value
            val valueSet = characteristic.setValue(value)
            if (!valueSet) {
                logger.error("Failed to set characteristic value")
                return Result.failure(IllegalStateException("Failed to set characteristic value"))
            }
            
            // Queue the notification
            val request = NotificationRequest(
                device = device,
                characteristic = characteristic,
                value = value.clone() // Clone to avoid modifications
            )
            
            notificationQueue.add(request)
            
            // Process the queue if not already processing
            if (!notificationInProgress) {
                processNotificationQueue()
            }
            
            // For now, assume success - real result will come from the callback
            return Result.success(Unit)
        } catch (e: Exception) {
            logger.error("Error sending notification", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Process the notification queue.
     */
    private fun processNotificationQueue() {
        if (notificationQueue.isEmpty() || notificationInProgress) {
            return
        }
        
        notificationInProgress = true
        
        val request = notificationQueue.removeFirst()
        
        try {
            val result = gattServer?.notifyCharacteristicChanged(
                request.device,
                request.characteristic,
                false // Not indicating
            ) ?: false
            
            if (result) {
                logger.debug("Notification request sent for characteristic: ${request.characteristic.uuid}")
            } else {
                logger.error("Failed to send notification for characteristic: ${request.characteristic.uuid}")
                
                // Log the failure
                scope.launch {
                    _gattOperations.emit(
                        GattOperation.NotificationSent(
                            request.characteristic,
                            request.value,
                            false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error sending notification", e)
            
            // Log the failure
            scope.launch {
                _gattOperations.emit(
                    GattOperation.NotificationSent(
                        request.characteristic,
                        request.value,
                        false
                    )
                )
            }
            
            // Continue processing the queue
            notificationInProgress = false
            processNotificationQueue()
        }
    }
    
    override fun areNotificationsEnabled(characteristic: BluetoothGattCharacteristic): Boolean {
        return notificationEnabledCharacteristics.contains(characteristic.uuid)
    }
    
    override fun enableNotifications(characteristic: BluetoothGattCharacteristic): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        // For now, we'll just track notification enablement in memory
        // In a real implementation, we'd check the Client Characteristic Configuration descriptor
        notificationEnabledCharacteristics.add(characteristic.uuid)
        return true
    }
    
    override fun disableNotifications(characteristic: BluetoothGattCharacteristic): Boolean {
        if (!initialized) {
            logger.error("Not initialized")
            return false
        }
        
        // Remove from enabled set
        return notificationEnabledCharacteristics.remove(characteristic.uuid)
    }
    
    /**
     * Find a characteristic by UUID.
     *
     * @param uuid The UUID of the characteristic to find
     * @return The characteristic, or null if not found
     */
    private fun findCharacteristicByUuid(uuid: UUID): BluetoothGattCharacteristic? {
        if (!initialized) {
            logger.error("Not initialized")
            return null
        }
        
        // Search all services for the characteristic
        gattServer?.services?.forEach { service ->
            service.characteristics?.forEach { characteristic ->
                if (characteristic.uuid == uuid) {
                    return characteristic
                }
            }
        }
        
        return null
    }
    
    override fun close() {
        if (!initialized) {
            return
        }
        
        logger.info("Closing BLE Connection Manager")
        
        try {
            // Close GATT server
            gattServer?.close()
            gattServer = null
            
            // Clear state
            _connectionState.value = ConnectionState.Disconnected
            connectedDevice = null
            connectionStateListeners.clear()
            notificationEnabledCharacteristics.clear()
            notificationQueue.clear()
            notificationInProgress = false
            
            initialized = false
            logger.info("BLE Connection Manager closed")
        } catch (e: Exception) {
            logger.error("Error closing BLE Connection Manager", e)
        }
    }
    
    override fun addConnectionStateListener(listener: ConnectionStateListener) {
        connectionStateListeners.add(listener)
    }
    
    override fun removeConnectionStateListener(listener: ConnectionStateListener) {
        connectionStateListeners.remove(listener)
    }
    
    /**
     * Update the connection state.
     *
     * @param newState The new connection state
     */
    private fun updateConnectionState(newState: ConnectionState) {
        // Update state
        _connectionState.value = newState
        
        // Notify listeners
        for (listener in connectionStateListeners) {
            listener.onConnectionStateChanged(newState)
        }
    }
    
    /**
     * GATT server callback.
     */
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        logger.info("Device connected: ${device.address}")
                        connectedDevice = device
                        updateConnectionState(ConnectionState.Connected(device))
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        logger.info("Device disconnected: ${device.address}")
                        connectedDevice = null
                        updateConnectionState(ConnectionState.Disconnected)
                    }
                    BluetoothProfile.STATE_CONNECTING -> {
                        logger.info("Device connecting: ${device.address}")
                        updateConnectionState(ConnectionState.Connecting)
                    }
                    BluetoothProfile.STATE_DISCONNECTING -> {
                        logger.info("Device disconnecting: ${device.address}")
                    }
                }
            } else {
                logger.error("Connection state change failed with status: $status")
                connectedDevice = null
                updateConnectionState(ConnectionState.Failed("Connection state change failed with status: $status"))
            }
        }
        
        override fun onServiceAdded(status: Int, service: BluetoothGattService) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                logger.info("Service added successfully: ${service.uuid}")
                
                // Emit service added operation
                scope.launch {
                    _gattOperations.emit(GattOperation.ServiceAdded(service))
                }
            } else {
                logger.error("Failed to add service: ${service.uuid}, status: $status")
            }
        }
        
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            logger.debug("Characteristic read request: ${characteristic.uuid}, offset: $offset")
            
            // Emit characteristic read operation
            scope.launch {
                _gattOperations.emit(
                    GattOperation.CharacteristicRead(
                        device,
                        characteristic,
                        requestId
                    )
                )
            }
            
            // TODO: This should be handled by the services
            // For now, just return the current value
            val value = characteristic.value ?: ByteArray(0)
            val responseValue = if (offset > value.size) {
                ByteArray(0)
            } else {
                value.copyOfRange(offset, value.size)
            }
            
            gattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                responseValue
            )
        }
        
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            logger.debug("Characteristic write request: ${characteristic.uuid}")
            
            // Emit characteristic write operation
            scope.launch {
                _gattOperations.emit(
                    GattOperation.CharacteristicWrite(
                        device,
                        characteristic,
                        value,
                        requestId
                    )
                )
            }
            
            // TODO: This should be handled by the services
            // For now, just update the value and send a success response
            characteristic.setValue(value)
            
            if (responseNeeded) {
                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            }
        }
        
        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            logger.debug("Descriptor read request: ${descriptor.uuid}")
            
            // TODO: This should be handled by the services
            // For now, just return the current value
            val value = descriptor.value ?: ByteArray(0)
            
            gattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                offset,
                value
            )
        }
        
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            logger.debug("Descriptor write request: ${descriptor.uuid}")
            
            // Check if this is a Client Configuration descriptor
            if (descriptor.uuid == UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) {
                // This is a request to enable/disable notifications
                val notificationsEnabled = value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                
                if (notificationsEnabled) {
                    logger.info("Notifications enabled for characteristic: ${descriptor.characteristic.uuid}")
                    notificationEnabledCharacteristics.add(descriptor.characteristic.uuid)
                } else {
                    logger.info("Notifications disabled for characteristic: ${descriptor.characteristic.uuid}")
                    notificationEnabledCharacteristics.remove(descriptor.characteristic.uuid)
                }
            }
            
            // Update the descriptor value
            descriptor.setValue(value)
            
            if (responseNeeded) {
                gattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    offset,
                    value
                )
            }
        }
        
        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            val success = status == BluetoothGatt.GATT_SUCCESS
            
            if (success) {
                logger.debug("Notification sent successfully")
            } else {
                logger.error("Notification failed with status: $status")
            }
            
            // Continue processing the queue
            notificationInProgress = false
            processNotificationQueue()
        }
    }
    
    /**
     * Data class for notification requests.
     */
    private data class NotificationRequest(
        val device: BluetoothDevice,
        val characteristic: BluetoothGattCharacteristic,
        val value: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as NotificationRequest
            
            if (device != other.device) return false
            if (characteristic != other.characteristic) return false
            if (!value.contentEquals(other.value)) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = device.hashCode()
            result = 31 * result + characteristic.hashCode()
            result = 31 * result + value.contentHashCode()
            return result
        }
    }
}
