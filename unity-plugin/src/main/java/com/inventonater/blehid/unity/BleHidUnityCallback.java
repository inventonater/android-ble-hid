package com.inventonater.blehid.unity;

/**
 * Callback interface for the BLE HID Unity plugin.
 * This interface is used by the plugin to send events back to Unity.
 */
public interface BleHidUnityCallback {
    /**
     * Called when initialization is complete.
     *
     * @param success Whether initialization succeeded
     * @param message Success or error message
     */
    void onInitializeComplete(boolean success, String message);
    
    /**
     * Called when the advertising state changes.
     *
     * @param advertising Whether advertising is active
     * @param message Additional information about the state change
     */
    void onAdvertisingStateChanged(boolean advertising, String message);
    
    /**
     * Called when the connection state changes.
     *
     * @param connected Whether a device is connected
     * @param deviceName Name of the connected device (null if disconnected)
     * @param deviceAddress Address of the connected device (null if disconnected)
     */
    void onConnectionStateChanged(boolean connected, String deviceName, String deviceAddress);
    
    /**
     * Called when the pairing state changes.
     *
     * @param status Pairing status (REQUESTED, SUCCESS, FAILED, etc.)
     * @param deviceAddress Address of the device being paired
     */
    void onPairingStateChanged(String status, String deviceAddress);
    
    /**
     * Called when an error occurs.
     *
     * @param errorCode Error code
     * @param errorMessage Error message
     */
    void onError(int errorCode, String errorMessage);
    
    /**
     * Called for debug log messages.
     *
     * @param message Debug message
     */
    void onDebugLog(String message);
}
