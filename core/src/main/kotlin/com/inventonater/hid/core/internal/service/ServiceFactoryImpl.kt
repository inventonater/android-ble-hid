package com.inventonater.hid.core.internal.service

import com.inventonater.hid.core.api.CompositeHidService
import com.inventonater.hid.core.api.HidServiceBase
import com.inventonater.hid.core.api.ServiceFactory
import com.inventonater.hid.core.api.ServiceRegistration
import com.inventonater.hid.core.internal.diagnostics.LoggerFactory

/**
 * Implementation of the ServiceFactory interface.
 *
 * This class is responsible for registering service types and
 * creating service instances on demand.
 *
 * @property loggerFactory Factory for creating loggers
 */
class ServiceFactoryImpl(private val loggerFactory: LoggerFactory) : ServiceFactory {
    
    private val logger = loggerFactory.getLogger("ServiceFactory")
    
    // Registry of service factories
    private val serviceRegistry = mutableMapOf<String, () -> HidServiceBase>()
    
    override fun register(serviceId: String, factory: () -> HidServiceBase) {
        logger.info("Registering service: $serviceId")
        serviceRegistry[serviceId] = factory
    }
    
    override fun isRegistered(serviceId: String): Boolean {
        return serviceRegistry.containsKey(serviceId)
    }
    
    override fun create(serviceId: String): HidServiceBase? {
        logger.debug("Creating service: $serviceId")
        
        val factory = serviceRegistry[serviceId]
        if (factory == null) {
            logger.warn("Service not registered: $serviceId")
            return null
        }
        
        return try {
            factory()
        } catch (e: Exception) {
            logger.error("Failed to create service: $serviceId", e)
            null
        }
    }
    
    override fun createComposite(compositeId: String, serviceIds: List<String>): HidServiceBase? {
        logger.debug("Creating composite service: $compositeId with services: $serviceIds")
        
        if (serviceIds.isEmpty()) {
            logger.warn("Cannot create composite service with no components")
            return null
        }
        
        // Create all component services first
        val services = serviceIds.mapNotNull { create(it) }
        
        // If any service failed to create, return null
        if (services.size != serviceIds.size) {
            logger.error("Failed to create all component services for composite: $compositeId")
            return null
        }
        
        // Create the composite service
        return CompositeHidServiceImpl(compositeId, services, loggerFactory)
    }
    
    override fun getRegisteredServiceIds(): List<String> {
        return serviceRegistry.keys.toList()
    }
    
    override fun unregister(serviceId: String): Boolean {
        logger.info("Unregistering service: $serviceId")
        return serviceRegistry.remove(serviceId) != null
    }
}

/**
 * Implementation of CompositeHidService that combines multiple HID services.
 *
 * @property serviceId Unique identifier for this composite service
 * @property services List of component services
 * @property loggerFactory Factory for creating loggers
 */
class CompositeHidServiceImpl(
    override val serviceId: String,
    private val services: List<HidServiceBase>,
    loggerFactory: LoggerFactory
) : AbstractHidService(serviceId, NoOpNotificationManager(), loggerFactory), CompositeHidService {
    
    // Combined report map
    override val reportMap: ByteArray by lazy {
        combineReportMaps()
    }
    
    override fun getComponentServices(): List<HidServiceBase> {
        return services.toList()
    }
    
    override fun addService(service: HidServiceBase): Boolean {
        // If the service is already in the composite, do nothing
        if (services.contains(service)) {
            logger.warn("Service already in composite: ${service.serviceId}")
            return false
        }
        
        // Add the service to the list
        (services as MutableList).add(service)
        logger.info("Added service to composite: ${service.serviceId}")
        return true
    }
    
    override fun removeService(serviceId: String): Boolean {
        val removed = (services as MutableList).removeIf { it.serviceId == serviceId }
        if (removed) {
            logger.info("Removed service from composite: $serviceId")
        } else {
            logger.warn("Service not found in composite: $serviceId")
        }
        return removed
    }
    
    override fun getCharacteristics(): List<ServiceCharacteristic> {
        // Combine characteristics from all services
        return services.flatMap { it.getCharacteristics() }
    }
    
    override fun initializeInternal(): Boolean {
        logger.info("Initializing composite service with ${services.size} components")
        
        // Initialize all component services
        var success = true
        for (service in services) {
            if (!service.initialize()) {
                logger.error("Failed to initialize component service: ${service.serviceId}")
                success = false
                break
            }
        }
        
        // If any service failed to initialize, shut down the ones that succeeded
        if (!success) {
            for (service in services) {
                if (service.isInitialized()) {
                    service.shutdown()
                }
            }
        }
        
        return success
    }
    
    override fun shutdownInternal() {
        // Shut down all component services
        for (service in services) {
            try {
                if (service.isInitialized()) {
                    service.shutdown()
                }
            } catch (e: Exception) {
                logger.error("Error shutting down component service: ${service.serviceId}", e)
            }
        }
    }
    
    override fun handleReport(reportId: Int, data: ByteArray): Boolean {
        // Find the service that handles this report ID
        for (service in services) {
            if (service.handleReport(reportId, data)) {
                return true
            }
        }
        
        logger.warn("No service handled report with ID: $reportId")
        return false
    }
    
    /**
     * Combine report maps from all services.
     * This is a simplistic implementation that just concatenates the report maps.
     * In a real implementation, you'd need to handle report ID conflicts and
     * create a properly structured combined report descriptor.
     */
    private fun combineReportMaps(): ByteArray {
        // This is just a placeholder implementation
        // In a real implementation, you'd need to properly combine the report maps
        logger.debug("Combining report maps from ${services.size} services")
        
        // For now, just concatenate the report maps
        val combined = mutableListOf<Byte>()
        for (service in services) {
            combined.addAll(service.reportMap.toList())
        }
        
        return combined.toByteArray()
    }
}

/**
 * No-op implementation of NotificationManager for the composite service.
 * The composite service itself doesn't send notifications; it delegates to its components.
 */
private class NoOpNotificationManager : com.inventonater.hid.core.api.NotificationManager {
    override suspend fun sendNotification(
        characteristic: android.bluetooth.BluetoothGattCharacteristic, 
        value: ByteArray
    ): Result<Unit> {
        return Result.failure(UnsupportedOperationException("CompositeHidService does not support sending notifications directly"))
    }
    
    override fun areNotificationsEnabled(characteristic: android.bluetooth.BluetoothGattCharacteristic): Boolean {
        return false
    }
    
    override fun enableNotifications(characteristic: android.bluetooth.BluetoothGattCharacteristic): Boolean {
        return false
    }
    
    override fun disableNotifications(characteristic: android.bluetooth.BluetoothGattCharacteristic): Boolean {
        return false
    }
}
