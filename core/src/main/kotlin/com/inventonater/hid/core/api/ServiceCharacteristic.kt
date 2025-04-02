package com.inventonater.hid.core.api

/**
 * Represents a Bluetooth GATT characteristic used by a HID service.
 */
interface ServiceCharacteristic {
    /**
     * The UUID of the characteristic.
     */
    val uuid: String
    
    /**
     * The properties of the characteristic.
     * 
     * For example: BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE
     */
    val properties: Int
    
    /**
     * The permissions of the characteristic.
     * 
     * For example: BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
     */
    val permissions: Int
    
    /**
     * The initial value of the characteristic, or null if no initial value.
     */
    val initialValue: ByteArray?
    
    /**
     * The descriptors of the characteristic.
     */
    val descriptors: List<ServiceDescriptor>
}

/**
 * Represents a Bluetooth GATT descriptor used by a characteristic.
 */
interface ServiceDescriptor {
    /**
     * The UUID of the descriptor.
     */
    val uuid: String
    
    /**
     * The permissions of the descriptor.
     * 
     * For example: BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE
     */
    val permissions: Int
    
    /**
     * The value of the descriptor, or null if no value.
     */
    val value: ByteArray?
}
