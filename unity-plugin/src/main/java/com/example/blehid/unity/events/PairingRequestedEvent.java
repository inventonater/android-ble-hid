package com.example.blehid.unity.events;

import android.bluetooth.BluetoothDevice;

/**
 * Event fired when a device requests pairing.
 */
public class PairingRequestedEvent extends DeviceEvent {
    private final int pairingVariant;
    
    /**
     * Creates a new pairing requested event.
     * 
     * @param device The device requesting pairing
     * @param pairingVariant The pairing variant requested (PIN, passkey, etc.)
     */
    public PairingRequestedEvent(BluetoothDevice device, int pairingVariant) {
        super(EventType.PAIRING_REQUESTED, device);
        this.pairingVariant = pairingVariant;
    }
    
    /**
     * Creates a new pairing requested event.
     * 
     * @param deviceAddress The address of the device requesting pairing
     * @param pairingVariant The pairing variant requested (PIN, passkey, etc.)
     */
    public PairingRequestedEvent(String deviceAddress, int pairingVariant) {
        super(EventType.PAIRING_REQUESTED, deviceAddress);
        this.pairingVariant = pairingVariant;
    }
    
    /**
     * Gets the pairing variant requested.
     * 
     * @return Pairing variant
     */
    public int getPairingVariant() {
        return pairingVariant;
    }
    
    @Override
    public String toString() {
        return String.format("%s[pairingVariant=%d]", super.toString(), pairingVariant);
    }
}
