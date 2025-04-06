package com.example.blehid.unity.events;

import android.bluetooth.BluetoothDevice;

/**
 * Base class for all device-related events.
 * Provides common device properties.
 */
public abstract class DeviceEvent extends BaseEvent {
    private final String deviceAddress;
    private final String deviceName;
    
    /**
     * Creates a new device event.
     * 
     * @param eventType Type of this event
     * @param device The Bluetooth device associated with the event
     */
    protected DeviceEvent(EventType eventType, BluetoothDevice device) {
        super(eventType);
        
        if (device == null) {
            this.deviceAddress = null;
            this.deviceName = null;
        } else {
            this.deviceAddress = device.getAddress();
            this.deviceName = device.getName();
        }
    }
    
    /**
     * Creates a new device event using just the address.
     * 
     * @param eventType Type of this event
     * @param deviceAddress MAC address of the device
     */
    protected DeviceEvent(EventType eventType, String deviceAddress) {
        super(eventType);
        this.deviceAddress = deviceAddress;
        this.deviceName = null; // Name unknown when only address is provided
    }
    
    /**
     * Gets the MAC address of the device.
     * 
     * @return Device MAC address
     */
    public String getDeviceAddress() {
        return deviceAddress;
    }
    
    /**
     * Gets the name of the device, if available.
     * 
     * @return Device name, or null if not available
     */
    public String getDeviceName() {
        return deviceName;
    }
    
    @Override
    public String toString() {
        return String.format("%s[deviceAddress=%s, deviceName=%s]", 
                             super.toString(), deviceAddress, deviceName);
    }
}
