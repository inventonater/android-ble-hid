package com.example.blehid.unity.events;

import android.bluetooth.BluetoothDevice;

/**
 * Event fired when a device disconnects.
 */
public class DeviceDisconnectedEvent extends DeviceEvent {
    private final DisconnectReason reason;
    
    /**
     * Reasons why a device might disconnect
     */
    public enum DisconnectReason {
        NORMAL_DISCONNECT,
        CONNECTION_TIMEOUT,
        CONNECTION_LOST,
        REMOTE_DEVICE_TERMINATED,
        LOCAL_HOST_TERMINATED,
        UNKNOWN
    }
    
    /**
     * Creates a new device disconnected event.
     * 
     * @param device The disconnected device
     * @param reason The reason for disconnection
     */
    public DeviceDisconnectedEvent(BluetoothDevice device, DisconnectReason reason) {
        super(EventType.DEVICE_DISCONNECTED, device);
        this.reason = reason;
    }
    
    /**
     * Creates a new device disconnected event using just the address.
     * 
     * @param deviceAddress MAC address of the disconnected device
     * @param reason The reason for disconnection
     */
    public DeviceDisconnectedEvent(String deviceAddress, DisconnectReason reason) {
        super(EventType.DEVICE_DISCONNECTED, deviceAddress);
        this.reason = reason;
    }
    
    /**
     * Gets the reason for disconnection.
     * 
     * @return Disconnect reason
     */
    public DisconnectReason getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return String.format("%s[reason=%s]", super.toString(), reason);
    }
}
