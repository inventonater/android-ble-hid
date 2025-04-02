package com.inventonater.hid.core.api

/**
 * Factory for creating and managing HID services.
 *
 * This interface is responsible for registering service types and
 * creating service instances on demand. It can also create composite
 * services that combine multiple service types.
 */
interface ServiceFactory {
    /**
     * Register a service type with a factory function.
     *
     * @param serviceId Unique identifier for the service
     * @param factory Factory function to create the service
     */
    fun register(serviceId: String, factory: () -> HidServiceBase)
    
    /**
     * Check if a service type is registered.
     *
     * @param serviceId ID of the service to check
     * @return true if the service is registered, false otherwise
     */
    fun isRegistered(serviceId: String): Boolean
    
    /**
     * Create a service instance.
     *
     * @param serviceId ID of the service to create
     * @return The created service, or null if the service is not registered
     */
    fun create(serviceId: String): HidServiceBase?
    
    /**
     * Create a composite service that combines multiple services.
     *
     * @param compositeId Unique identifier for the composite service
     * @param serviceIds List of service IDs to include in the composite
     * @return The created composite service, or null if any of the services are not registered
     */
    fun createComposite(compositeId: String, serviceIds: List<String>): HidServiceBase?
    
    /**
     * Get a list of registered service IDs.
     *
     * @return List of registered service IDs
     */
    fun getRegisteredServiceIds(): List<String>
    
    /**
     * Unregister a service type.
     *
     * @param serviceId ID of the service to unregister
     * @return true if the service was unregistered, false if it wasn't registered
     */
    fun unregister(serviceId: String): Boolean
}

/**
 * Information about a registered service.
 *
 * @property serviceId Unique identifier for the service
 * @property factory Factory function to create the service
 */
data class ServiceRegistration(
    val serviceId: String,
    val factory: () -> HidServiceBase
)

/**
 * Interface for a composite HID service that combines multiple services.
 */
interface CompositeHidService : HidServiceBase {
    /**
     * Get the services that are part of this composite.
     *
     * @return List of component services
     */
    fun getComponentServices(): List<HidServiceBase>
    
    /**
     * Add a service to the composite.
     *
     * @param service The service to add
     * @return true if the service was added, false otherwise
     */
    fun addService(service: HidServiceBase): Boolean
    
    /**
     * Remove a service from the composite.
     *
     * @param serviceId ID of the service to remove
     * @return true if the service was removed, false if it wasn't part of the composite
     */
    fun removeService(serviceId: String): Boolean
}
