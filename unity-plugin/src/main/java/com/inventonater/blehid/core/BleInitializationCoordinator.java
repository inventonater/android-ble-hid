package com.inventonater.blehid.core;

import android.content.Context;
import android.util.Log;

/**
 * Coordinates the initialization process for BLE HID functionality.
 * This class manages the initialization sequence, ensuring that all
 * required components are properly initialized in the correct order.
 */
public class BleInitializationCoordinator {
    private static final String TAG = "BleInitCoordinator";
    private final BluetoothEnvironmentValidator environmentValidator;
    private final BleGattServerManager gattServerManager;
    private final HidMediaService hidMediaService;
    
    private boolean isInitialized = false;
    
    /**
     * Creates a new BleInitializationCoordinator.
     * 
     * @param context The application context
     * @param gattServerManager The GATT server manager to initialize
     * @param hidMediaService The HID service to initialize
     */
    public BleInitializationCoordinator(Context context, 
                                        BleGattServerManager gattServerManager,
                                        HidMediaService hidMediaService) {
        this.environmentValidator = new BluetoothEnvironmentValidator(context);
        this.gattServerManager = gattServerManager;
        this.hidMediaService = hidMediaService;
    }
    
    /**
     * Performs the full initialization sequence.
     * 
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {

        if (!checkPrerequisites()) {
            Log.e(TAG, "Failed to meet initialization prerequisites");
            return false;
        }
        
        if (!initializeGattServer()) {
            Log.e(TAG, "Failed to initialize GATT server");
            cleanupOnFailure();
            return false;
        }
        
        if (!initializeHidService()) {
            Log.e(TAG, "Failed to initialize HID service");
            cleanupOnFailure();
            return false;
        }
        
        isInitialized = true;
        Log.i(TAG, "BLE HID initialization completed successfully");
        return true;
    }
    
    /**
     * Checks if all prerequisites are met for initialization.
     * 
     * @return true if all prerequisites are met, false otherwise
     */
    private boolean checkPrerequisites() {
        // Delegate to environment validator
        return environmentValidator.validateAll();
    }
    
    /**
     * Initializes the GATT server.
     * 
     * @return true if initialization was successful, false otherwise
     */
    private boolean initializeGattServer() {
        try {
            boolean result = gattServerManager.initialize();
            if (!result) {
                Log.e(TAG, "GATT server initialization failed");
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error during GATT server initialization: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Initializes the HID service.
     * 
     * @return true if initialization was successful, false otherwise
     */
    private boolean initializeHidService() {
        try {
            boolean result = hidMediaService.initialize();
            if (!result) {
                Log.e(TAG, "HID service initialization failed");
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error during HID service initialization: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Cleans up resources if initialization fails.
     */
    private void cleanupOnFailure() {
        Log.d(TAG, "Cleaning up after failed initialization");
        
        try {
            // Close GATT server if it was initialized
            if (gattServerManager != null) {
                gattServerManager.close();
            }
            
            // Reset initialization state
            isInitialized = false;
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage(), e);
        }
    }
    
    /**
     * Checks if initialization has been completed successfully.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Gets the environment validator used by this coordinator.
     * 
     * @return The BluetoothEnvironmentValidator instance
     */
    public BluetoothEnvironmentValidator getEnvironmentValidator() {
        return environmentValidator;
    }
}
