package com.inventonater.hid.core.internal.service

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import com.inventonater.hid.core.api.HidServiceBase
import com.inventonater.hid.core.api.NotificationManager
import com.inventonater.hid.core.api.ServiceCharacteristic
import com.inventonater.hid.core.api.ServiceDescriptor
import com.inventonater.hid.core.internal.diagnostics.LoggerFactory
import java.util.UUID

/**
 * Abstract base class for HID service implementations.
 *
 * This class provides common functionality for all HID services,
 * reducing the amount of code that needs to be implemented by
 * specific service classes.
 *
 * @property serviceId Unique identifier for this service
 * @property notificationManager Manager for sending notifications
 * @property loggerFactory Factory for creating loggers
 */
abstract class AbstractHidService(
    override val serviceId: String,
    private val notificationManager: NotificationManager,
    private val loggerFactory: LoggerFactory
) : HidServiceBase {

    // Logger for this service
    protected val logger = loggerFactory.getLogger("HidService.$serviceId")
    
    // HID Service UUIDs
    protected companion object {
        val HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
        val HID_INFORMATION_UUID = UUID.fromString("00002A4A-0000-1000-8000-00805f9b34fb")
        val HID_CONTROL_POINT_UUID = UUID.fromString("00002A4C-0000-1000-8000-00805f9b34fb")
        val HID_REPORT_MAP_UUID = UUID.fromString("00002A4B-0000-1000-8000-00805f9b34fb")
        val HID_REPORT_UUID = UUID.fromString("00002A4D-0000-1000-8000-00805f9b34fb")
        
        // Standard descriptor UUIDs
        val CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        val REPORT_REFERENCE_UUID = UUID.fromString("00002908-0000-1000-8000-00805f9b34fb")
        
        // HID Control Point values
        const val CONTROL_POINT_SUSPEND = 0x00
        const val CONTROL_POINT_EXIT_SUSPEND = 0x01
    }
    
    // Flag indicating if the service is initialized
    private var initialized = false
    
    // Cache of characteristic UUIDs to characteristic objects
    protected val characteristicCache = mutableMapOf<UUID, BluetoothGattCharacteristic>()
    
    /**
     * Create a standard HID service with all required characteristics.
     *
     * @return The configured BluetoothGattService
     */
    protected fun createHidService(): BluetoothGattService {
        logger.debug("Creating HID service")
        
        val service = BluetoothGattService(
            HID_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        
        // Add required characteristics
        val characteristics = getCharacteristics()
        
        for (serviceChrct in characteristics) {
            val characteristic = BluetoothGattCharacteristic(
                UUID.fromString(serviceChrct.uuid),
                serviceChrct.properties,
                serviceChrct.permissions
            )
            
            // Set initial value if provided
            serviceChrct.initialValue?.let { characteristic.setValue(it) }
            
            // Add descriptors
            for (serviceDesc in serviceChrct.descriptors) {
                val descriptor = BluetoothGattDescriptor(
                    UUID.fromString(serviceDesc.uuid),
                    serviceDesc.permissions
                )
                
                // Set initial value if provided
                serviceDesc.value?.let { descriptor.setValue(it) }
                
                characteristic.addDescriptor(descriptor)
            }
            
            // Add the characteristic to the service
            service.addCharacteristic(characteristic)
            
            // Cache the characteristic for quick lookup
            characteristicCache[UUID.fromString(serviceChrct.uuid)] = characteristic
        }
        
        return service
    }
    
    /**
     * Send a notification for an HID report.
     *
     * @param reportId The report ID
     * @param data The report data
     * @return true if the notification was sent successfully, false otherwise
     */
    protected suspend fun sendReport(reportId: Int, data: ByteArray): Boolean {
        val characteristic = characteristicCache[HID_REPORT_UUID]
        
        if (characteristic == null) {
            logger.error("Report characteristic not found")
            return false
        }
        
        if (!isInitialized()) {
            logger.error("Service not initialized")
            return false
        }
        
        // Create the full report with the report ID
        val report = ByteArray(data.size + 1)
        report[0] = reportId.toByte()
        System.arraycopy(data, 0, report, 1, data.size)
        
        // Log the report being sent for debugging
        logger.debug("Sending HID report: ${report.joinToString(" ") { "%02X".format(it) }}")
        
        // Send the notification
        val result = notificationManager.sendNotification(characteristic, report)
        
        return result.isSuccess
    }
    
    /**
     * Get a cached characteristic by UUID.
     *
     * @param uuid The UUID of the characteristic to get
     * @return The characteristic, or null if not found
     */
    protected fun getCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        return characteristicCache[uuid]
    }
    
    override fun initialize(): Boolean {
        if (initialized) {
            logger.warn("Service already initialized")
            return true
        }
        
        logger.info("Initializing HID service: $serviceId")
        
        // Perform service-specific initialization
        val success = initializeInternal()
        
        if (success) {
            initialized = true
            logger.info("HID service initialized successfully: $serviceId")
        } else {
            logger.error("Failed to initialize HID service: $serviceId")
        }
        
        return success
    }
    
    /**
     * Service-specific initialization logic.
     * Subclasses should override this to perform their specific initialization.
     *
     * @return true if initialization was successful, false otherwise
     */
    protected open fun initializeInternal(): Boolean {
        return true
    }
    
    override fun shutdown() {
        if (!initialized) {
            logger.warn("Service not initialized, nothing to shut down")
            return
        }
        
        logger.info("Shutting down HID service: $serviceId")
        
        // Perform service-specific shutdown
        shutdownInternal()
        
        // Clear caches
        characteristicCache.clear()
        
        initialized = false
        logger.info("HID service shut down: $serviceId")
    }
    
    /**
     * Service-specific shutdown logic.
     * Subclasses should override this to perform their specific cleanup.
     */
    protected open fun shutdownInternal() {
        // Default implementation does nothing
    }
    
    override fun isInitialized(): Boolean {
        return initialized
    }
    
    override fun handleReport(reportId: Int, data: ByteArray): Boolean {
        // Default implementation does nothing
        // Subclasses should override this for device-originated reports
        logger.warn("Received report with ID $reportId, but no handler implemented")
        return false
    }
    
    /**
     * Convert a byte array to a hex string for logging.
     *
     * @param bytes The byte array to convert
     * @return A hex string representation of the byte array
     */
    protected fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString(" ") { "%02X".format(it) }
    }
}
