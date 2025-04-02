package com.inventonater.hid.core.api

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages BLE connections and the GATT server.
 *
 * This interface is responsible for setting up the BLE GATT server,
 * handling device connections and disconnections, and managing
 * BLE operations like notifications.
 */
interface BleConnectionManager {
    /**
     * Current connection state as an observable flow.
     */
    val connectionState: StateFlow<ConnectionState>
    
    /**
     * Observable flow of GATT operations.
     */
    val gattOperations: Flow<GattOperation>
    
    /**
     * Initialize the connection manager.
     *
     * @param context Application context
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(context: Context): Boolean
    
    /**
     * Add a service to the GATT server.
     *
     * @param service The service to add
     * @return true if the service was added successfully, false otherwise
     */
    fun addService(service: BluetoothGattService): Boolean
    
    /**
     * Remove a service from the GATT server.
     *
     * @param serviceUuid UUID of the service to remove
     * @return true if the service was removed, false otherwise
     */
    fun removeService(serviceUuid: String): Boolean
    
    /**
     * Send a notification for a characteristic.
     *
     * @param characteristicUuid UUID of the characteristic
     * @param value The value to send
     * @return Result object containing success or failure information
     */
    suspend fun sendNotification(characteristicUuid: String, value: ByteArray): Result<Unit>
    
    /**
     * Close the connection manager and release all resources.
     */
    fun close()
    
    /**
     * Add a connection state listener.
     *
     * @param listener The listener to add
     */
    fun addConnectionStateListener(listener: ConnectionStateListener)
    
    /**
     * Remove a connection state listener.
     *
     * @param listener The listener to remove
     */
    fun removeConnectionStateListener(listener: ConnectionStateListener)
}

/**
 * Listener for connection state changes.
 */
interface ConnectionStateListener {
    /**
     * Called when the connection state changes.
     *
     * @param newState The new connection state
     */
    fun onConnectionStateChanged(newState: ConnectionState)
}

/**
 * Represents a GATT operation.
 */
sealed class GattOperation {
    /**
     * Service added to GATT server.
     *
     * @property service The service that was added
     */
    data class ServiceAdded(val service: BluetoothGattService) : GattOperation()
    
    /**
     * Service removed from GATT server.
     *
     * @property serviceUuid UUID of the service that was removed
     */
    data class ServiceRemoved(val serviceUuid: String) : GattOperation()
    
    /**
     * Characteristic read request.
     *
     * @property device The device that made the request
     * @property characteristic The characteristic being read
     * @property requestId The request ID
     */
    data class CharacteristicRead(
        val device: BluetoothDevice,
        val characteristic: BluetoothGattCharacteristic,
        val requestId: Int
    ) : GattOperation()
    
    /**
     * Characteristic write request.
     *
     * @property device The device that made the request
     * @property characteristic The characteristic being written
     * @property value The value being written
     * @property requestId The request ID
     */
    data class CharacteristicWrite(
        val device: BluetoothDevice,
        val characteristic: BluetoothGattCharacteristic,
        val value: ByteArray,
        val requestId: Int
    ) : GattOperation()
    
    /**
     * Notification sent.
     *
     * @property characteristic The characteristic for which the notification was sent
     * @property value The value that was sent
     * @property success Whether the notification was sent successfully
     */
    data class NotificationSent(
        val characteristic: BluetoothGattCharacteristic,
        val value: ByteArray,
        val success: Boolean
    ) : GattOperation()
}

/**
 * Manager for handling notifications to connected devices.
 */
interface NotificationManager {
    /**
     * Send a notification for a characteristic.
     *
     * @param characteristic The characteristic to notify
     * @param value The value to send
     * @return Result object containing success or failure information
     */
    suspend fun sendNotification(characteristic: BluetoothGattCharacteristic, value: ByteArray): Result<Unit>
    
    /**
     * Check if notifications are enabled for a characteristic.
     *
     * @param characteristic The characteristic to check
     * @return true if notifications are enabled, false otherwise
     */
    fun areNotificationsEnabled(characteristic: BluetoothGattCharacteristic): Boolean
    
    /**
     * Enable notifications for a characteristic.
     *
     * @param characteristic The characteristic to enable notifications for
     * @return true if notifications were enabled, false otherwise
     */
    fun enableNotifications(characteristic: BluetoothGattCharacteristic): Boolean
    
    /**
     * Disable notifications for a characteristic.
     *
     * @param characteristic The characteristic to disable notifications for
     * @return true if notifications were disabled, false otherwise
     */
    fun disableNotifications(characteristic: BluetoothGattCharacteristic): Boolean
}

/**
 * Result of a notification operation.
 */
sealed class NotificationResult {
    /**
     * Notification was sent successfully.
     *
     * @property characteristic The characteristic that was notified
     * @property value The value that was sent
     */
    data class Success(
        val characteristic: BluetoothGattCharacteristic,
        val value: ByteArray
    ) : NotificationResult()
    
    /**
     * Notification failed.
     *
     * @property characteristic The characteristic that was being notified
     * @property reason Reason for the failure
     */
    data class Failure(
        val characteristic: BluetoothGattCharacteristic,
        val reason: String
    ) : NotificationResult()
}
