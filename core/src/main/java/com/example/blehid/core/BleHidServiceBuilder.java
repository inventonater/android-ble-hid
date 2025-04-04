package com.example.blehid.core;

import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.example.blehid.core.gatt.GattServiceFactory;
import com.example.blehid.core.manager.BleGattServiceRegistry;

/**
 * Builder for BleHidService.
 * Allows flexible configuration of the HID service.
 */
public class BleHidServiceBuilder {
    private static final String TAG = "BleHidServiceBuilder";
    
    private final BleHidManager bleHidManager;
    private String deviceName = "BLE HID Device";
    private boolean enableMouse = true;
    private boolean enableKeyboard = true;
    private boolean enableConsumer = true;
    private BluetoothGattService customHidService = null;
    
    /**
     * Creates a new BleHidService builder.
     *
     * @param bleHidManager The BLE HID manager
     */
    public BleHidServiceBuilder(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
    }
    
    /**
     * Sets the device name.
     *
     * @param deviceName The device name
     * @return This builder for chaining
     */
    public BleHidServiceBuilder withDeviceName(String deviceName) {
        this.deviceName = deviceName;
        return this;
    }
    
    /**
     * Enables or disables mouse functionality.
     *
     * @param enable Whether to enable mouse functionality
     * @return This builder for chaining
     */
    public BleHidServiceBuilder withMouseSupport(boolean enable) {
        this.enableMouse = enable;
        return this;
    }
    
    /**
     * Enables or disables keyboard functionality.
     *
     * @param enable Whether to enable keyboard functionality
     * @return This builder for chaining
     */
    public BleHidServiceBuilder withKeyboardSupport(boolean enable) {
        this.enableKeyboard = enable;
        return this;
    }
    
    /**
     * Enables or disables consumer control functionality.
     *
     * @param enable Whether to enable consumer control functionality
     * @return This builder for chaining
     */
    public BleHidServiceBuilder withConsumerSupport(boolean enable) {
        this.enableConsumer = enable;
        return this;
    }
    
    /**
     * Uses a custom HID service instead of generating one based on configuration.
     *
     * @param hidService The custom HID service
     * @return This builder for chaining
     */
    public BleHidServiceBuilder withCustomHidService(BluetoothGattService hidService) {
        this.customHidService = hidService;
        return this;
    }
    
    /**
     * Builds a simplified mouse-only BleHidService for better compatibility.
     *
     * @return A new BleHidService with mouse functionality only
     */
    public static BleHidService createMouseService(BleHidManager bleHidManager) {
        // Turn on verbose logging
        Log.i(TAG, "Enabling verbose logging for debugging");
        
        // Generate a unique device name with timestamp to avoid conflicts
        String deviceName = "Android BLE Mouse " + System.currentTimeMillis() % 1000;
        
        Log.i(TAG, "Creating mouse-only service with name: " + deviceName);
        
        return new BleHidServiceBuilder(bleHidManager)
                .withMouseSupport(true)
                .withKeyboardSupport(false)
                .withConsumerSupport(false)
                .withDeviceName(deviceName)
                .build();
    }
    
    /**
     * Builds a keyboard-only BleHidService.
     *
     * @return A new BleHidService with keyboard functionality only
     */
    public static BleHidService createKeyboardService(BleHidManager bleHidManager) {
        return new BleHidServiceBuilder(bleHidManager)
                .withMouseSupport(false)
                .withKeyboardSupport(true)
                .withConsumerSupport(false)
                .withDeviceName("BLE Keyboard")
                .build();
    }
    
    /**
     * Builds a keyboard with media controls BleHidService.
     *
     * @return A new BleHidService with keyboard and consumer functionality
     */
    public static BleHidService createKeyboardWithMediaService(BleHidManager bleHidManager) {
        return new BleHidServiceBuilder(bleHidManager)
                .withMouseSupport(false)
                .withKeyboardSupport(true)
                .withConsumerSupport(true)
                .withDeviceName("BLE Multimedia Keyboard")
                .build();
    }
    
    /**
     * Builds a combined BleHidService with all functionality.
     *
     * @return A new BleHidService with all functionality
     */
    public static BleHidService createCombinedService(BleHidManager bleHidManager) {
        return new BleHidServiceBuilder(bleHidManager)
                .withMouseSupport(true)
                .withKeyboardSupport(true)
                .withConsumerSupport(true)
                .withDeviceName("BLE HID Device")
                .build();
    }
    
    /**
     * Builds the BleHidService based on configuration.
     *
     * @return A new BleHidService instance
     */
    public BleHidService build() {
        BluetoothGattService hidService;
        
        if (customHidService != null) {
            // Use custom service if provided
            hidService = customHidService;
            Log.d(TAG, "Using custom HID service");
        } else {
            // Generate service based on configuration
            hidService = GattServiceFactory.createHidService(
                    enableMouse, enableKeyboard, enableConsumer);
            Log.d(TAG, "Generated HID service with: " + 
                  "mouse=" + enableMouse + ", " +
                  "keyboard=" + enableKeyboard + ", " +
                  "consumer=" + enableConsumer);
        }
        
        // Get the GATT server manager from the HID manager
        BleGattServiceRegistry gattServiceRegistry = bleHidManager.getGattServiceRegistry();
        
        // Create the BLE notifier for characteristic notifications
        BleNotifier notifier = new BleNotifier(gattServiceRegistry);
        
        // Create the BleHidService
        return new BleHidService(
                bleHidManager,
                gattServiceRegistry,
                hidService,
                notifier,
                deviceName,
                enableMouse,
                enableKeyboard,
                enableConsumer);
    }
}
