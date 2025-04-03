package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;

/**
 * Interface defining the contract for Bluetooth pairing manager implementations.
 */
public interface IPairingManager {
    /**
     * Pairing state enum defining the various states in the pairing process.
     */
    enum PairingState {
        IDLE,               // No pairing activity
        PAIRING_REQUESTED,  // Pairing has been requested
        PAIRING_STARTED,    // Pairing process has started
        WAITING_FOR_BOND,   // Waiting for bonding to complete
        BONDED,             // Device is bonded
        PAIRING_FAILED,     // Pairing failed
        UNPAIRING           // Currently removing bond
    }
    
    /**
     * Callback interface for pairing events.
     */
    interface PairingCallback {
        /**
         * Called when a device requests pairing.
         * 
         * @param device The device requesting pairing
         * @param variant The pairing variant (PIN, passkey, etc.)
         */
        void onPairingRequested(BluetoothDevice device, int variant);
        
        /**
         * Called when the pairing process is complete.
         * 
         * @param device The paired device
         * @param success Whether pairing was successful
         * @param status Optional status message
         */
        void onPairingComplete(BluetoothDevice device, boolean success, String status);
        
        /**
         * Called when pairing progress is updated.
         * 
         * @param device The device being paired
         * @param state The current pairing state
         * @param message A human-readable status message
         */
        void onPairingProgress(BluetoothDevice device, PairingState state, String message);
    }
    
    /**
     * Registers receivers to listen for pairing events.
     */
    void registerReceiver();
    
    /**
     * Unregisters receivers for pairing events.
     */
    void unregisterReceiver();
    
    /**
     * Sets a callback for pairing events.
     * 
     * @param callback The callback to set, or null to remove
     */
    void setPairingCallback(PairingCallback callback);
    
    /**
     * Initiates pairing with a device.
     * 
     * @param device The device to pair with
     * @return true if pairing was initiated, false otherwise
     */
    boolean createBond(BluetoothDevice device);
    
    /**
     * Removes pairing with a device.
     * 
     * @param device The device to unpair
     * @return true if unpairing was successful, false otherwise
     */
    boolean removeBond(BluetoothDevice device);
    
    /**
     * Cancel any ongoing pairing process.
     * 
     * @return true if pairing was cancelled, false otherwise
     */
    boolean cancelPairing();
    
    /**
     * Sets the PIN code for pairing.
     * 
     * @param device The device to set PIN for
     * @param pin The PIN code as a string
     * @return true if the PIN was set, false otherwise
     */
    boolean setPin(BluetoothDevice device, String pin);
    
    /**
     * Confirms a pairing request (for just-works or numeric comparison).
     * 
     * @param device The device to confirm pairing with
     * @param confirm true to accept, false to reject
     * @return true if confirmation was sent, false otherwise
     */
    boolean setPairingConfirmation(BluetoothDevice device, boolean confirm);
    
    /**
     * Sets a passkey for pairing.
     * 
     * @param device The device to set passkey for
     * @param passkey The numeric passkey
     * @param confirm Whether to auto-confirm the passkey
     * @return true if the passkey was set, false otherwise
     */
    boolean setPasskey(BluetoothDevice device, int passkey, boolean confirm);
    
    /**
     * Checks if a device is currently bonded/paired.
     * 
     * @param device The device to check
     * @return true if bonded, false otherwise
     */
    boolean isBonded(BluetoothDevice device);
    
    /**
     * Gets information about all bonded devices.
     * 
     * @return Information about all bonded devices
     */
    String getBondedDevicesInfo();
    
    /**
     * Gets the current pairing state.
     * 
     * @return The current pairing state
     */
    PairingState getPairingState();
    
    /**
     * Set the maximum number of pairing retry attempts.
     * 
     * @param retries Number of retry attempts
     */
    void setMaxPairingRetries(int retries);
    
    /**
     * Cleans up resources when no longer needed.
     */
    void close();
}
