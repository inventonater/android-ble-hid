package com.inventonater.hid.core.api

import android.bluetooth.BluetoothDevice

/**
 * Interface for receiving BLE connection events.
 * Implement this interface to be notified of connection and
 * disconnection events from remote BLE central devices.
 */
interface ConnectionListener {
    /**
     * Called when a central device connects to our peripheral.
     *
     * @param device The Bluetooth device that connected
     */
    fun onDeviceConnected(device: BluetoothDevice)
    
    /**
     * Called when a central device disconnects from our peripheral.
     *
     * @param device The Bluetooth device that disconnected
     */
    fun onDeviceDisconnected(device: BluetoothDevice)
}
