package com.example.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages Bluetooth pairing and bonding operations with a state machine for reliable
 * connection handling across various device types.
 */
public class PairingManager implements IPairingManager {
    private static final String TAG = "PairingManager";
    
    // Timeouts for various operations (in ms)
    private static final long PAIRING_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    private static final long BOND_STATE_CHANGE_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    
    private final BleHidManager bleHidManager;
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    
    private boolean isRegistered = false;
    private PairingCallback pairingCallback = null;
    private PairingState currentState = PairingState.IDLE;
    private BluetoothDevice pendingPairingDevice = null;
    private Map<String, Integer> devicePairingRetryCount = new HashMap<>();
    private int maxPairingRetries = 3;
    
    // Handler for timeouts
    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable = null;
    
    /**
     * BroadcastReceiver for Bluetooth pairing events.
     */
    private final BroadcastReceiver pairingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                BluetoothDevice device = BluetoothUtils.getBluetoothDeviceFromIntent(intent);
                int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
                int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.BOND_NONE);
                
                handleBondStateChanged(device, prevBondState, bondState);
            } 
            else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                BluetoothDevice device = BluetoothUtils.getBluetoothDeviceFromIntent(intent);
                int variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                
                // Cancel any current timeout since we're making progress
                cancelTimeouts();
                
                // Start a new timeout for the pairing request
                startTimeout(PAIRING_TIMEOUT, "Pairing request timed out", device);
                
                handlePairingRequest(device, variant);
            }
            // Handle additional pairing edge cases
            else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = BluetoothUtils.getBluetoothDeviceFromIntent(intent);
                Log.d(TAG, "Device connected: " + BluetoothUtils.getDeviceInfo(device));
                
                if (pendingPairingDevice != null && 
                    pendingPairingDevice.getAddress().equals(device.getAddress())) {
                    Log.i(TAG, "Pending device connected during pairing");
                    
                    // If we were waiting for a bond, this might signal we're making progress
                    if (currentState == PairingState.WAITING_FOR_BOND) {
                        // Reset the timeout since we're making progress
                        cancelTimeouts();
                        startTimeout(BOND_STATE_CHANGE_TIMEOUT, "Bonding timed out after ACL connection", device);
                    }
                }
            }
            else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = BluetoothUtils.getBluetoothDeviceFromIntent(intent);
                Log.d(TAG, "Device disconnected: " + BluetoothUtils.getDeviceInfo(device));
                
                // If this is our pending device and we're still trying to pair, this is likely a problem
                if (pendingPairingDevice != null && 
                    pendingPairingDevice.getAddress().equals(device.getAddress()) &&
                    (currentState == PairingState.PAIRING_STARTED || 
                     currentState == PairingState.PAIRING_REQUESTED || 
                     currentState == PairingState.WAITING_FOR_BOND)) {
                    
                    Log.w(TAG, "Device disconnected during pairing process");
                    
                    // Check if we should retry
                    if (shouldRetryPairing(device)) {
                        Log.i(TAG, "Will retry pairing after disconnect");
                        
                        // Wait briefly before retrying
                        final BluetoothDevice retryDevice = device;
                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Log.i(TAG, "Retrying pairing after disconnect");
                                // Only retry if we're still in a pairing state
                                if (currentState == PairingState.PAIRING_STARTED || 
                                    currentState == PairingState.PAIRING_REQUESTED || 
                                    currentState == PairingState.WAITING_FOR_BOND) {
                                    createBond(retryDevice);
                                }
                            }
                        }, 1000);
                    } else {
                        // No more retries, mark as failed
                        updatePairingState(PairingState.PAIRING_FAILED, device, "Device disconnected during pairing");
                        notifyPairingComplete(device, false, "Device disconnected during pairing");
                    }
                }
            }
        }
    };
    
    /**
     * Creates a new Pairing Manager.
     * 
     * @param bleHidManager The parent BLE HID manager
     */
    public PairingManager(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        this.context = bleHidManager.getContext();
        this.bluetoothAdapter = bleHidManager.getBluetoothAdapter();
    }
    
    /**
     * Registers the pairing receivers to monitor pairing events.
     */
    @Override
    public void registerReceiver() {
        if (isRegistered) {
            return;
        }
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        
        context.registerReceiver(pairingReceiver, filter);
        isRegistered = true;
        Log.d(TAG, "Pairing broadcast receiver registered");
    }
    
    /**
     * Unregisters the pairing receivers.
     */
    @Override
    public void unregisterReceiver() {
        if (!isRegistered) {
            return;
        }
        
        try {
            context.unregisterReceiver(pairingReceiver);
            isRegistered = false;
            Log.d(TAG, "Pairing broadcast receiver unregistered");
        } catch (Exception e) {
            Log.e(TAG, "Failed to unregister receiver", e);
        }
    }
    
    /**
     * Sets a callback for pairing events.
     * 
     * @param callback The callback to set, or null to remove
     */
    @Override
    public void setPairingCallback(PairingCallback callback) {
        this.pairingCallback = callback;
        
        // Register receiver when callback is set
        if (callback != null) {
            registerReceiver();
        }
    }
    
    /**
     * Start a timeout for the current operation.
     * 
     * @param timeoutMs Timeout in milliseconds
     * @param message Message to log if timeout occurs
     * @param device Device being paired
     */
    private void startTimeout(long timeoutMs, final String message, final BluetoothDevice device) {
        // Cancel any existing timeout
        cancelTimeouts();
        
        // Create new timeout
        timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "TIMEOUT: " + message + " for device " + BluetoothUtils.getDeviceInfo(device));
                handlePairingTimeout(device, message);
            }
        };
        
        timeoutHandler.postDelayed(timeoutRunnable, timeoutMs);
        Log.d(TAG, "Started " + (timeoutMs/1000) + "s timeout for " + BluetoothUtils.getDeviceInfo(device));
    }
    
    /**
     * Cancel any pending timeouts.
     */
    private void cancelTimeouts() {
        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
            Log.d(TAG, "Cancelled pending timeout");
        }
    }
    
    /**
     * Handle a pairing timeout.
     * 
     * @param device Device that timed out
     * @param reason Reason for the timeout
     */
    private void handlePairingTimeout(BluetoothDevice device, String reason) {
        // Check if we're still in a pairing state
        if (currentState == PairingState.PAIRING_STARTED || 
            currentState == PairingState.PAIRING_REQUESTED || 
            currentState == PairingState.WAITING_FOR_BOND) {
            
            // Check if we should retry
            if (shouldRetryPairing(device)) {
                Log.i(TAG, "Retrying pairing after timeout: " + reason);
                createBond(device);
            } else {
                // No more retries, mark as failed
                updatePairingState(PairingState.PAIRING_FAILED, device, "Pairing timed out: " + reason);
                notifyPairingComplete(device, false, "Pairing timed out: " + reason);
            }
        }
    }
    
    /**
     * Determine if we should retry pairing with a device.
     * 
     * @param device The device to check
     * @return true if pairing should be retried
     */
    private boolean shouldRetryPairing(BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        
        String address = device.getAddress();
        int retryCount = devicePairingRetryCount.containsKey(address) ? 
                          devicePairingRetryCount.get(address) : 0;
        
        if (retryCount < maxPairingRetries) {
            // Increment retry count
            devicePairingRetryCount.put(address, retryCount + 1);
            return true;
        }
        
        return false;
    }
    
    /**
     * Reset retry count for a device.
     * 
     * @param device The device to reset retry count for
     */
    private void resetRetryCount(BluetoothDevice device) {
        if (device != null) {
            devicePairingRetryCount.put(device.getAddress(), 0);
        }
    }
    
    /**
     * Update the current pairing state and notify callback.
     * 
     * @param newState The new pairing state
     * @param device The device involved
     * @param message A status message
     */
    private void updatePairingState(PairingState newState, BluetoothDevice device, String message) {
        if (currentState != newState) {
            Log.d(TAG, "Pairing state changed: " + currentState + " -> " + newState + 
                   (message != null ? " (" + message + ")" : ""));
            
            currentState = newState;
            
            // Update the pending device reference
            if (newState == PairingState.IDLE || newState == PairingState.PAIRING_FAILED) {
                pendingPairingDevice = null;
            } else if (device != null) {
                pendingPairingDevice = device;
            }
            
            // Notify callback of progress
            if (pairingCallback != null && device != null) {
                pairingCallback.onPairingProgress(device, newState, message);
            }
            
            // Special handling for certain state transitions
            if (newState == PairingState.BONDED) {
                // Reset retry count when bonding succeeds
                resetRetryCount(device);
                
                // Cancel any pending timeouts
                cancelTimeouts();
            }
        }
    }
    
    /**
     * Handles bond state changes.
     * 
     * @param device The device whose bond state changed
     * @param previousState The previous bond state
     * @param newState The new bond state
     */
    private void handleBondStateChanged(BluetoothDevice device, int previousState, int newState) {
        String deviceInfo = BluetoothUtils.getDeviceInfo(device);
        
        Log.d(TAG, "Bond state changed for " + deviceInfo + 
                ": " + BluetoothUtils.bondStateToString(previousState) + 
                " -> " + BluetoothUtils.bondStateToString(newState));
        
        // Cancel any timeouts since we got a bond state change
        cancelTimeouts();
        
        if (newState == BluetoothDevice.BOND_BONDING) {
            // Device is now bonding - update state and start timeout
            updatePairingState(PairingState.WAITING_FOR_BOND, device, "Bonding in progress");
            
            // Use a longer timeout for the actual bonding process
            startTimeout(BOND_STATE_CHANGE_TIMEOUT * 2, "Bonding process timed out", device);
        }
        else if (newState == BluetoothDevice.BOND_BONDED) {
            // Successfully bonded
            updatePairingState(PairingState.BONDED, device, "Device bonded successfully");
            
            Log.i(TAG, "Device bonded: " + deviceInfo);
            notifyPairingComplete(device, true, "Pairing completed successfully");
            
            // For Android-to-Android pairing, we need to ensure the device stays connected
            // so add a slight delay to make sure the bonding process completes fully
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override
                public void run() {
                    // If we're still bonded, we can consider this a successful pairing
                    if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                        Log.d(TAG, "Bond has remained stable for " + deviceInfo);
                    }
                }
            }, 1000);
        } 
        else if (previousState == BluetoothDevice.BOND_BONDING && 
                 newState == BluetoothDevice.BOND_NONE) {
            // Bonding failed
            Log.w(TAG, "Bonding failed for device: " + deviceInfo);
            
            // Check if we should retry
            if (shouldRetryPairing(device)) {
                Log.i(TAG, "Retrying pairing after bonding failure");
                
                // Wait a bit longer before retrying for Android devices
                final BluetoothDevice retryDevice = device;
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Executing pairing retry for: " + BluetoothUtils.getDeviceInfo(retryDevice));
                        // Only retry if we're still in a pairing state
                        if (currentState == PairingState.PAIRING_STARTED || 
                            currentState == PairingState.PAIRING_REQUESTED || 
                            currentState == PairingState.WAITING_FOR_BOND ||
                            currentState == PairingState.PAIRING_FAILED) {
                            createBond(retryDevice);
                        }
                    }
                }, 2000); // 2 seconds delay for Android device pairing
            } else {
                // No more retries, mark as failed
                updatePairingState(PairingState.PAIRING_FAILED, device, "Bonding failed");
                notifyPairingComplete(device, false, "Bonding failed after " + maxPairingRetries + " attempts");
            }
        }
        else if (previousState == BluetoothDevice.BOND_BONDED && 
                 newState == BluetoothDevice.BOND_NONE) {
            // Device was unpaired
            Log.i(TAG, "Device unpaired: " + deviceInfo);
            
            if (currentState == PairingState.UNPAIRING) {
                // This was an intentional unpairing
                updatePairingState(PairingState.IDLE, device, "Device unpaired successfully");
            }
        }
    }
    
    /**
     * Handles pairing requests.
     * 
     * @param device The device requesting pairing
     * @param variant The pairing variant type
     */
    private void handlePairingRequest(BluetoothDevice device, int variant) {
        String deviceInfo = BluetoothUtils.getDeviceInfo(device);
        Log.d(TAG, "Pairing request from " + deviceInfo + 
                ", variant: " + BluetoothUtils.pairingVariantToString(variant));
        
        // Update state to show we've received a pairing request
        updatePairingState(PairingState.PAIRING_STARTED, device, 
                          "Received pairing request: " + BluetoothUtils.pairingVariantToString(variant));
        
        // Just-works pairing can be auto-accepted
        if (variant == BluetoothUtils.PAIRING_VARIANT_CONSENT) {
            Log.i(TAG, "Auto-accepting just-works pairing for " + deviceInfo);
            BluetoothUtils.setPairingConfirmation(device, true);
        }
        
        // Notify through callback for UI handling of other variants
        if (pairingCallback != null) {
            pairingCallback.onPairingRequested(device, variant);
        }
    }
    
    /**
     * Notify the callback that pairing is complete.
     * 
     * @param device The paired device
     * @param success Whether pairing was successful
     * @param status Optional status message
     */
    private void notifyPairingComplete(BluetoothDevice device, boolean success, String status) {
        if (pairingCallback != null && device != null) {
            pairingCallback.onPairingComplete(device, success, status);
        }
    }
    
    /**
     * Initiates pairing with a device.
     * 
     * @param device The device to pair with
     * @return true if pairing was initiated, false otherwise
     */
    @Override
    public boolean createBond(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Cannot create bond with null device");
            return false;
        }
        
        String deviceInfo = BluetoothUtils.getDeviceInfo(device);
        
        try {
            registerReceiver(); // Ensure we're registered to receive events
            
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "Device already bonded: " + deviceInfo);
                updatePairingState(PairingState.BONDED, device, "Device already bonded");
                return true;
            }
            
            if (currentState == PairingState.PAIRING_REQUESTED || 
                currentState == PairingState.PAIRING_STARTED || 
                currentState == PairingState.WAITING_FOR_BOND) {
                
                // If we're already pairing with this device, that's fine
                if (pendingPairingDevice != null && 
                    pendingPairingDevice.getAddress().equals(device.getAddress())) {
                    Log.i(TAG, "Already pairing with this device: " + deviceInfo);
                    return true;
                }
                
                // If we're pairing with a different device, cancel that first
                Log.w(TAG, "Already pairing with another device, cancelling");
                cancelPairing();
            }
            
            // Update state and start pairing
            updatePairingState(PairingState.PAIRING_REQUESTED, device, "Initiating pairing");
            pendingPairingDevice = device;
            
            // Start timeout
            startTimeout(PAIRING_TIMEOUT, "Pairing process timed out", device);
            
            boolean result = BluetoothUtils.createBond(device);
            
            if (!result) {
                updatePairingState(PairingState.PAIRING_FAILED, device, "Failed to initiate pairing");
                cancelTimeouts();
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error creating bond", e);
            updatePairingState(PairingState.PAIRING_FAILED, device, "Error: " + e.getMessage());
            cancelTimeouts();
            return false;
        }
    }
    
    /**
     * Cancel any ongoing pairing process.
     * 
     * @return true if pairing was cancelled, false if no pairing was in progress
     */
    @Override
    public boolean cancelPairing() {
        if (currentState == PairingState.IDLE || 
            currentState == PairingState.BONDED || 
            currentState == PairingState.PAIRING_FAILED) {
            
            // No pairing to cancel
            return false;
        }
        
        cancelTimeouts();
        
        if (pendingPairingDevice != null) {
            Log.i(TAG, "Cancelling pairing with " + BluetoothUtils.getDeviceInfo(pendingPairingDevice));
            
            // Try to cancel using reflection if bonding has started
            if (pendingPairingDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                try {
                    java.lang.reflect.Method method = pendingPairingDevice.getClass().getMethod("cancelBondProcess");
                    boolean result = (Boolean) method.invoke(pendingPairingDevice);
                    Log.d(TAG, "Cancel bond process result: " + result);
                } catch (Exception e) {
                    Log.e(TAG, "Error cancelling bond process", e);
                }
            }
            
            BluetoothDevice device = pendingPairingDevice;
            updatePairingState(PairingState.PAIRING_FAILED, device, "Pairing cancelled");
            notifyPairingComplete(device, false, "Pairing cancelled by user");
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Removes pairing with a device.
     * 
     * @param device The device to unpair
     * @return true if unpairing was successful, false otherwise
     */
    @Override
    public boolean removeBond(BluetoothDevice device) {
        if (device == null) {
            Log.e(TAG, "Cannot remove bond with null device");
            return false;
        }
        
        try {
            String deviceInfo = BluetoothUtils.getDeviceInfo(device);
            
            if (device.getBondState() == BluetoothDevice.BOND_NONE) {
                Log.i(TAG, "Device already not bonded: " + deviceInfo);
                return true;
            }
            
            // Update state to unpairing
            updatePairingState(PairingState.UNPAIRING, device, "Removing bond");
            
            boolean result = BluetoothUtils.removeBond(device);
            
            if (!result) {
                updatePairingState(PairingState.IDLE, device, "Failed to remove bond");
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error removing bond", e);
            return false;
        }
    }
    
    /**
     * Sets the PIN code for pairing.
     * 
     * @param device The device to set PIN for
     * @param pin The PIN code as a string
     * @return true if the PIN was set, false otherwise
     */
    @Override
    public boolean setPin(BluetoothDevice device, String pin) {
        return BluetoothUtils.setPin(device, pin);
    }
    
    /**
     * Confirms a pairing request (for just-works or numeric comparison).
     * 
     * @param device The device to confirm pairing with
     * @param confirm true to accept, false to reject
     * @return true if confirmation was sent, false otherwise
     */
    @Override
    public boolean setPairingConfirmation(BluetoothDevice device, boolean confirm) {
        boolean result = BluetoothUtils.setPairingConfirmation(device, confirm);
        
        if (!confirm && result) {
            // If rejecting, update state to failed
            updatePairingState(PairingState.PAIRING_FAILED, device, "Pairing rejected by user");
            notifyPairingComplete(device, false, "Pairing rejected by user");
        }
        
        return result;
    }
    
    /**
     * Sets a passkey for pairing.
     * 
     * @param device The device to set passkey for
     * @param passkey The numeric passkey
     * @param confirm Whether to auto-confirm the passkey
     * @return true if the passkey was set, false otherwise
     */
    @Override
    public boolean setPasskey(BluetoothDevice device, int passkey, boolean confirm) {
        return BluetoothUtils.setPasskey(device, passkey, confirm);
    }
    
    /**
     * Set the maximum number of pairing retry attempts.
     * 
     * @param retries Number of retry attempts (default is 3)
     */
    @Override
    public void setMaxPairingRetries(int retries) {
        this.maxPairingRetries = Math.max(0, retries);
    }
    
    /**
     * Get the current pairing state.
     * 
     * @return The current pairing state
     */
    @Override
    public PairingState getPairingState() {
        return currentState;
    }
    
    /**
     * Checks if a device is currently bonded/paired.
     * 
     * @param device The device to check
     * @return true if bonded, false otherwise
     */
    @Override
    public boolean isBonded(BluetoothDevice device) {
        return device != null && device.getBondState() == BluetoothDevice.BOND_BONDED;
    }
    
    /**
     * Gets information about all bonded devices.
     * 
     * @return Information about all bonded devices
     */
    @Override
    public String getBondedDevicesInfo() {
        if (bluetoothAdapter == null) {
            return "Bluetooth adapter not available";
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Bonded devices:\n");
        
        for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
            info.append("- ").append(BluetoothUtils.getDeviceInfo(device)).append("\n");
        }
        
        if (bluetoothAdapter.getBondedDevices().isEmpty()) {
            info.append("(No bonded devices)");
        }
        
        return info.toString();
    }
    
    /**
     * Gets comprehensive information about the current pairing status.
     * 
     * @return Detailed information about pairing status
     */
    public String getPairingStatusInfo() {
        StringBuilder info = new StringBuilder();
        
        // Current state
        info.append("Pairing State: ").append(currentState).append("\n");
        
        // Pending device info
        if (pendingPairingDevice != null) {
            info.append("Pairing with: ").append(BluetoothUtils.getDeviceInfo(pendingPairingDevice)).append("\n");
            info.append("Bond state: ").append(BluetoothUtils.bondStateToString(pendingPairingDevice.getBondState())).append("\n");
        } else {
            info.append("No pending pairing\n");
        }
        
        // Add retry counts for debugging
        if (!devicePairingRetryCount.isEmpty()) {
            info.append("Retry counts:\n");
            for (Map.Entry<String, Integer> entry : devicePairingRetryCount.entrySet()) {
                info.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("/").append(maxPairingRetries).append("\n");
            }
        }
        
        // Add bonded devices info
        info.append("\n").append(getBondedDevicesInfo());
        
        return info.toString();
    }
    
    /**
     * Displays a toast message on the UI thread.
     */
    private void showToast(final String message) {
        try {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast", e);
        }
    }
    
    /**
     * Cleans up resources when no longer needed.
     */
    @Override
    public void close() {
        cancelPairing();
        unregisterReceiver();
        cancelTimeouts();
        pairingCallback = null;
        currentState = PairingState.IDLE;
        pendingPairingDevice = null;
        Log.i(TAG, "Pairing Manager closed");
    }
}
