package com.example.blehid.unity.events;

import android.bluetooth.BluetoothDevice;

/**
 * Event fired when a device successfully connects.
 */
public class DeviceConnectedEvent extends DeviceEvent {
    
    /**
     * Creates a new device connected event.
     * 
     * @param device The connected device
     */
    public DeviceConnectedEvent(BluetoothDevice device) {
        super(EventType.DEVICE_CONNECTED, device);
    }
    
    /**
     * Creates a new device connected event using just the address.
     * 
     * @param deviceAddress MAC address of the connected device
     */
    public DeviceConnectedEvent(String deviceAddress) {
        super(EventType.DEVICE_CONNECTED, deviceAddress);
    }
}
