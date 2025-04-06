package com.example.blehid.unity.events;

import android.bluetooth.BluetoothDevice;

/**
 * Event fired when pairing with a device fails.
 */
public class PairingFailedEvent extends DeviceEvent {
    private final PairingFailureReason reason;
    
    /**
     * Reasons why pairing might fail
     */
    public enum PairingFailureReason {
        CANCELLED,
        TIMEOUT,
        AUTHENTICATION_FAILED,
        PROTOCOL_ERROR,
        REJECTED_BY_REMOTE,
        UNKNOWN
    }
    
    /**
     * Creates a new pairing failed event.
     * 
     * @param device The device that failed to pair
     * @param reason The reason for the pairing failure
     */
    public PairingFailedEvent(BluetoothDevice device, PairingFailureReason reason) {
        super(EventType.PAIRING_FAILED, device);
        this.reason = reason;
    }
    
    /**
     * Creates a new pairing failed event using just the address.
     * 
     * @param deviceAddress MAC address of the device that failed to pair
     * @param reason The reason for the pairing failure
     */
    public PairingFailedEvent(String deviceAddress, PairingFailureReason reason) {
        super(EventType.PAIRING_FAILED, deviceAddress);
        this.reason = reason;
    }
    
    /**
     * Gets the reason for the pairing failure.
     * 
     * @return Pairing failure reason
     */
    public PairingFailureReason getReason() {
        return reason;
    }
    
    @Override
    public String toString() {
        return String.format("%s[reason=%s]", super.toString(), reason);
    }
}
