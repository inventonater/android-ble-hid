package com.inventonater.hid.unity;

/**
 * Simplified connection listener interface for Unity.
 * This interface provides callbacks for connection events in a Unity-friendly way.
 */
public interface UnityConnectionListener {
    /**
     * Called when a device connects.
     *
     * @param deviceAddress The MAC address of the connected device
     */
    void onConnected(String deviceAddress);
    
    /**
     * Called when a device disconnects.
     *
     * @param deviceAddress The MAC address of the disconnected device
     */
    void onDisconnected(String deviceAddress);
}
