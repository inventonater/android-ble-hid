package com.inventonater.blehid.unity;

/**
 * Interface for BLE HID callback events from Java to Unity.
 * This interface defines methods that will be called by the Java plugin
 * to notify Unity of important events.
 */
public interface BleHidUnityCallback {
    /**
     * Called when initialization is complete.
     * @param success Whether initialization was successful
     * @param message Additional information about the result
     */
    void onInitializeComplete(boolean success, String message);
    
    /**
     * Called when the BLE advertising state changes.
     * @param advertising Whether advertising is now active
     * @param message Additional information or error details
     */
    void onAdvertisingStateChanged(boolean advertising, String message);
    
    /**
     * Called when a device connects or disconnects.
     * @param connected Whether a device is now connected
     * @param deviceName Name of the connected device, or null if disconnected
     * @param deviceAddress Address of the connected device, or null if disconnected
     */
    void onConnectionStateChanged(boolean connected, String deviceName, String deviceAddress);
    
    /**
     * Called when pairing status changes.
     * @param status Pairing status string (e.g., "REQUESTED", "SUCCESS", "FAILED", "BONDED", "NONE")
     * @param deviceAddress Address of the device being paired with, or null
     */
    void onPairingStateChanged(String status, String deviceAddress);
    
    /**
     * Called when connection parameters are updated.
     * @param interval Connection interval in milliseconds
     * @param latency Slave latency (number of connection events that slave can skip)
     * @param timeout Supervision timeout in milliseconds
     * @param mtu MTU size in bytes
     */
    void onConnectionParametersChanged(int interval, int latency, int timeout, int mtu);
    
    /**
     * Called when RSSI is read.
     * @param rssi RSSI value in dBm
     */
    void onRssiRead(int rssi);
    
    /**
     * Called when a connection parameter change request is completed.
     * @param parameterName Name of the parameter that was changed
     * @param success Whether the change was successful
     * @param actualValue The actual value that was applied by the remote device
     */
    void onConnectionParameterRequestComplete(String parameterName, boolean success, String actualValue);
    
    /**
     * Called when an error occurs.
     * @param errorCode Numeric error code
     * @param errorMessage Human-readable error message
     */
    void onError(int errorCode, String errorMessage);
    
    /**
     * Called for debug log messages.
     * @param message Debug message to log
     */
    void onDebugLog(String message);
}
