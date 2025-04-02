package com.inventonater.hid.core.api

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

/**
 * Base interface for all HID services.
 * 
 * This interface defines the contract that all HID services must implement.
 * Each HID service represents a specific type of HID device functionality,
 * such as mouse, keyboard, etc.
 */
interface HidServiceBase {
    /**
     * Unique identifier for this service type.
     */
    val serviceId: String
    
    /**
     * The HID report descriptor for this service.
     * This descriptor defines the format of the HID reports.
     */
    val reportMap: ByteArray
    
    /**
     * Initialize the service.
     * 
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(): Boolean
    
    /**
     * Shutdown the service and release any resources.
     */
    fun shutdown()
    
    /**
     * Get the Bluetooth GATT characteristics required by this service.
     * 
     * @return List of ServiceCharacteristic objects
     */
    fun getCharacteristics(): List<ServiceCharacteristic>
    
    /**
     * Process a received report.
     * 
     * @param reportId Report ID
     * @param data Report data
     * @return true if the report was processed successfully, false otherwise
     */
    fun handleReport(reportId: Int, data: ByteArray): Boolean
    
    /**
     * Check if the service is initialized.
     * 
     * @return true if the service is initialized, false otherwise
     */
    fun isInitialized(): Boolean
}

/**
 * Represents a Bluetooth GATT characteristic used by a HID service.
 */
interface ServiceCharacteristic {
    val uuid: String
    val properties: Int
    val permissions: Int
    val initialValue: ByteArray?
    val descriptors: List<ServiceDescriptor>
}

/**
 * Represents a Bluetooth GATT descriptor used by a HID service characteristic.
 */
interface ServiceDescriptor {
    val uuid: String
    val permissions: Int
    val value: ByteArray?
}
