package com.inventonater.blehid.unity;

/**
 * Interface for Unity to receive callbacks from the BLE HID plugin.
 * Unity code can implement this interface to handle connection and pairing events.
 */
public interface UnityCallback {
    /**
     * Called when a device requests pairing.
     * 
     * @param deviceAddress The MAC address of the device requesting pairing
     * @param variant The pairing variant (PIN, passkey, etc.)
     */
    void onPairingRequested(String deviceAddress, int variant);
    
    /**
     * Called when a device is connected.
     * 
     * @param deviceAddress The MAC address of the connected device
     */
    void onDeviceConnected(String deviceAddress);
    
    /**
     * Called when a device is disconnected.
     * 
     * @param deviceAddress The MAC address of the disconnected device
     */
    void onDeviceDisconnected(String deviceAddress);
    
    /**
     * Called when pairing with a device fails.
     * 
     * @param deviceAddress The MAC address of the device that failed to pair
     */
    void onPairingFailed(String deviceAddress);
    
    /**
     * Factory method to create an instance of UnityCallback from a Unity object.
     * 
     * @param impl The implementation provided by Unity
     * @return The UnityCallback instance
     */
    public static UnityCallback createInstance(final UnityCallback impl) {
        return impl;
    }
}
