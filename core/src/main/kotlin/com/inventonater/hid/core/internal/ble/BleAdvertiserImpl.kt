package com.inventonater.hid.core.internal.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.os.ParcelUuid
import com.inventonater.hid.core.api.LogManager
import java.util.UUID

/**
 * Implementation of BLE advertising functionality.
 *
 * This class handles advertising the device as a BLE HID peripheral.
 *
 * @property bluetoothAdapter The Bluetooth adapter
 * @property logManager The log manager for logging
 */
class BleAdvertiserImpl(
    private val bluetoothAdapter: BluetoothAdapter,
    private val logManager: LogManager
) {
    private val logger = logManager.getLogger("BleAdvertiser")
    
    // HID service UUID
    private val HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb")
    
    // Advertiser
    private var advertiser: BluetoothLeAdvertiser? = null
    
    // Flag indicating if advertising
    private var advertising = false
    
    /**
     * Starts advertising the device as a BLE HID peripheral.
     *
     * @return true if advertising started successfully, false otherwise
     */
    fun startAdvertising(): Boolean {
        if (advertising) {
            logger.warn("Already advertising")
            return true
        }
        
        logger.info("Starting BLE advertising")
        
        // Get the advertiser
        advertiser = bluetoothAdapter.bluetoothLeAdvertiser
        if (advertiser == null) {
            logger.error("Bluetooth LE advertising not supported")
            return false
        }
        
        try {
            // Build advertising settings
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build()
            
            // Build advertising data
            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(HID_SERVICE_UUID))
                .build()
            
            // Start advertising
            advertiser?.startAdvertising(settings, data, advertiseCallback)
            logger.debug("Advertise request sent")
            
            // For now, assume success until callback
            advertising = true
            return true
        } catch (e: Exception) {
            logger.error("Failed to start advertising", e)
            advertising = false
            return false
        }
    }
    
    /**
     * Stops advertising.
     */
    fun stopAdvertising() {
        if (!advertising) {
            return
        }
        
        logger.info("Stopping BLE advertising")
        
        try {
            advertiser?.stopAdvertising(advertiseCallback)
        } catch (e: Exception) {
            logger.error("Error stopping advertising", e)
        }
        
        advertising = false
    }
    
    /**
     * Checks if advertising is currently active.
     *
     * @return true if advertising, false otherwise
     */
    fun isAdvertising(): Boolean {
        return advertising
    }
    
    /**
     * Callback for advertising operations.
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            logger.info("Advertising started successfully")
            advertising = true
        }
        
        override fun onStartFailure(errorCode: Int) {
            advertising = false
            
            val reason = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "already started"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "data too large"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "feature unsupported"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "internal error"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "too many advertisers"
                else -> "unknown error $errorCode"
            }
            
            logger.error("Failed to start advertising: $reason")
        }
    }
}
