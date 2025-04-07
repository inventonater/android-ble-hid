package com.inventonater.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

/**
 * Handles BLE advertising of the HID peripheral device.
 * This class manages the Bluetooth LE advertising process for advertising 
 * the device as a HID peripheral.
 * 
 * DIAGNOSTIC VERSION: Enhanced with better logging and fallback options
 */
public class BleAdvertiser {
    private static final String TAG = "BleAdvertiser";
    
    // Human Interface Device service UUID
    private static final UUID HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    
    // Device Info service UUID
    private static final UUID DEVICE_INFO_UUID = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");
    
    // Error messages for UI feedback
    public static final String ERROR_NO_ADAPTER = "Bluetooth adapter not available";
    public static final String ERROR_NO_PERIPHERAL_MODE = "This device doesn't support BLE peripheral mode";
    public static final String ERROR_ANDROID_VERSION = "Android version doesn't support BLE advertising";
    public static final String ERROR_ALREADY_ADVERTISING = "Already advertising";
    public static final String ERROR_DATA_TOO_LARGE = "Advertising data too large";
    
    private final BleHidManager bleHidManager;
    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private boolean isAdvertising = false;
    private String lastErrorMessage = null;
    private Context context;
    
    // Diagnostic variables
    private int advertisingAttempts = 0;
    private int advertisingSuccess = 0;
    private int advertisingFailures = 0;
    private long lastAdvertisingStartTime = 0;
    
    // New options
    private boolean forceAdvertising = true; // Override capability check
    
    /**
     * Callback for BLE advertising operations.
     */
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            isAdvertising = true;
            advertisingSuccess++;
            long timeTaken = System.currentTimeMillis() - lastAdvertisingStartTime;
            
            Log.i(TAG, "✅ BLE Advertising started successfully (took " + timeTaken + "ms)");
            Log.i(TAG, "ℹ️ Settings in effect: mode=" + modeToString(settingsInEffect.getMode()) +
                  ", txPowerLevel=" + powerToString(settingsInEffect.getTxPowerLevel()) +
                  ", timeout=" + settingsInEffect.getTimeout() + "ms");
            
            showToast("Advertising started successfully!");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            isAdvertising = false;
            advertisingFailures++;
            String errorMessage = getAdvertiseErrorMessage(errorCode);
            Log.e(TAG, "❌ BLE Advertising failed to start: " + errorMessage + 
                  " (attempt #" + advertisingAttempts + ")");
            
            showToast("Advertising failed: " + errorMessage);
            lastErrorMessage = errorMessage;
        }
    };
    
    /**
     * Converts advertising mode to string.
     */
    private String modeToString(int mode) {
        switch (mode) {
            case AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY:
                return "LOW_LATENCY";
            case AdvertiseSettings.ADVERTISE_MODE_BALANCED:
                return "BALANCED";
            case AdvertiseSettings.ADVERTISE_MODE_LOW_POWER:
                return "LOW_POWER";
            default:
                return "UNKNOWN(" + mode + ")";
        }
    }
    
    /**
     * Converts power level to string.
     */
    private String powerToString(int power) {
        switch (power) {
            case AdvertiseSettings.ADVERTISE_TX_POWER_HIGH:
                return "HIGH";
            case AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM:
                return "MEDIUM";
            case AdvertiseSettings.ADVERTISE_TX_POWER_LOW:
                return "LOW";
            case AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW:
                return "ULTRA_LOW";
            default:
                return "UNKNOWN(" + power + ")";
        }
    }
    
    /**
     * Creates a new BLE advertiser.
     * 
     * @param bleHidManager The parent BLE HID manager
     */
    public BleAdvertiser(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        this.bluetoothAdapter = bleHidManager.getBluetoothAdapter();
        this.context = bleHidManager.getContext();
        
        Log.i(TAG, "📱 BleAdvertiser initialized with forceAdvertising=" + forceAdvertising);
    }
    
    /**
     * Starts advertising the BLE HID service.
     * 
     * @return true if advertising started successfully, false otherwise
     */
    public boolean startAdvertising() {
        advertisingAttempts++;
        lastAdvertisingStartTime = System.currentTimeMillis();
        
        Log.i(TAG, "🔍 DIAGNOSTIC: startAdvertising() - Attempt #" + advertisingAttempts + 
              " (success=" + advertisingSuccess + ", failures=" + advertisingFailures + ")");
        
        if (isAdvertising) {
            lastErrorMessage = ERROR_ALREADY_ADVERTISING;
            Log.w(TAG, "⚠️ " + lastErrorMessage);
            showToast("Already advertising!");
            return true; // Already advertising is not a failure
        }
        
        if (bluetoothAdapter == null) {
            lastErrorMessage = ERROR_NO_ADAPTER;
            Log.e(TAG, "❌ " + lastErrorMessage);
            showToast("No Bluetooth adapter available!");
            advertisingFailures++;
            return false;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            lastErrorMessage = ERROR_ANDROID_VERSION;
            Log.e(TAG, "❌ " + lastErrorMessage);
            showToast("Android version too old for BLE advertising!");
            advertisingFailures++;
            return false;
        }
        
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            lastErrorMessage = "Bluetooth is disabled";
            Log.e(TAG, "❌ " + lastErrorMessage);
            showToast("Bluetooth is disabled!");
            advertisingFailures++;
            return false;
        }
        
        // Log device capabilities
        logDeviceCapabilities();
        
        // Set simplified device name for better compatibility
        try {
            String deviceName = "Android BLE Mouse";
            bluetoothAdapter.setName(deviceName);
            Log.i(TAG, "📱 Device name set to: " + deviceName);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to set device name", e);
        }
        
        // Get the advertiser
        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        
        // Check if the device supports advertising
        if (bluetoothLeAdvertiser == null && !forceAdvertising) {
            lastErrorMessage = ERROR_NO_PERIPHERAL_MODE;
            Log.e(TAG, "❌ " + lastErrorMessage);
            
            // Extra logging to help debug peripheral mode support
            Log.e(TAG, "❌ Device reports no peripheral mode support. Bluetooth adapter: " + 
                  (bluetoothAdapter != null ? "Available" : "Unavailable") + 
                  ", isMultipleAdvertisementSupported: " + 
                  (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && 
                   bluetoothAdapter != null && 
                   bluetoothAdapter.isMultipleAdvertisementSupported()));
            
            showToast("Device reports no BLE peripheral support");
            advertisingFailures++;
            return false;
        }
        
        // If forceAdvertising is true and advertiser is null, try to continue
        if (bluetoothLeAdvertiser == null && forceAdvertising) {
            Log.w(TAG, "⚠️ OVERRIDE: Attempting to force advertising despite no advertiser reported");
            // Try to get it again - sometimes it just needs another attempt
            bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
            if (bluetoothLeAdvertiser == null) {
                Log.e(TAG, "❌ Failed to force get advertiser");
                showToast("Force advertising failed - no advertiser available");
                advertisingFailures++;
                return false;
            }
        }
        
        Log.i(TAG, "✅ Got BluetoothLeAdvertiser instance: " + bluetoothLeAdvertiser);
        
        // Build advertising parameters
        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData advertiseData = buildSimplifiedAdvertiseData(); // Using simplified data
        AdvertiseData scanResponseData = buildSimplifiedScanResponseData(); // Using simplified response
        
        try {
            Log.i(TAG, "📢 Starting advertising with HID service UUID: " + HID_SERVICE_UUID);
            Log.i(TAG, "📋 Advertise data: " + advertiseDataToString(advertiseData));
            Log.i(TAG, "📋 Scan response: " + advertiseDataToString(scanResponseData));
            
            // Start advertising
            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback);
            
            // Return true but actual success will be determined in the callback
            return true;
        } catch (Exception e) {
            lastErrorMessage = "Failed to start advertising: " + e.getMessage();
            Log.e(TAG, "❌ " + lastErrorMessage, e);
            showToast("Failed to start advertising: " + e.getMessage());
            advertisingFailures++;
            return false;
        }
    }
    
    /**
     * Log device capabilities to help with debugging.
     */
    private void logDeviceCapabilities() {
        Log.i(TAG, "=== 📱 DEVICE CAPABILITIES ===");
        Log.i(TAG, "Device name: " + bluetoothAdapter.getName());
        Log.i(TAG, "Device address: " + bluetoothAdapter.getAddress());
        Log.i(TAG, "Android SDK: " + Build.VERSION.SDK_INT);
        Log.i(TAG, "Bluetooth enabled: " + bluetoothAdapter.isEnabled());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.i(TAG, "isMultipleAdvertisementSupported: " + bluetoothAdapter.isMultipleAdvertisementSupported());
            Log.i(TAG, "isOffloadedFilteringSupported: " + bluetoothAdapter.isOffloadedFilteringSupported());
            Log.i(TAG, "isOffloadedScanBatchingSupported: " + bluetoothAdapter.isOffloadedScanBatchingSupported());
            Log.i(TAG, "isLe2MPhySupported: " + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bluetoothAdapter.isLe2MPhySupported()));
            Log.i(TAG, "isLeCodedPhySupported: " + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bluetoothAdapter.isLeCodedPhySupported()));
            Log.i(TAG, "isLeExtendedAdvertisingSupported: " + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bluetoothAdapter.isLeExtendedAdvertisingSupported()));
            Log.i(TAG, "BluetoothLeAdvertiser: " + (bluetoothAdapter.getBluetoothLeAdvertiser() != null ? "Available" : "Not available"));
        }
        Log.i(TAG, "================================");
    }

    /**
     * Helper to convert AdvertiseData to a readable string for debugging.
     */
    private String advertiseDataToString(AdvertiseData data) {
        StringBuilder sb = new StringBuilder("AdvertiseData{");
        sb.append("includeTxPower=").append(data.getIncludeTxPowerLevel());
        sb.append(", includeDeviceName=").append(data.getIncludeDeviceName());
        sb.append(", numServiceUuids=").append(data.getServiceUuids() != null ? data.getServiceUuids().size() : 0);
        sb.append(", numServiceData=").append(data.getServiceData() != null ? data.getServiceData().size() : 0);
        sb.append(", numManufacturerSpecificData=").append(data.getManufacturerSpecificData() != null ? data.getManufacturerSpecificData().size() : 0);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Display a toast message on the UI thread.
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
     * Builds the advertising settings for the BLE peripheral.
     * 
     * @return The configured AdvertiseSettings
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        // Simple but effective settings for maximum visibility
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0) 
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();
                
        Log.d(TAG, "🔧 Using high-power advertising settings for maximum visibility");
        return settings;
    }

    /**
     * Builds a minimal advertising data payload that's guaranteed to fit within BLE size limits.
     * 
     * @return The configured AdvertiseData
     */
    private AdvertiseData buildSimplifiedAdvertiseData() {
        // Create the absolute minimal advertising data to avoid size issues
        // We'll use just the 16-bit HID service UUID instead of the full 128-bit one
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder()
                .setIncludeDeviceName(false) // Name is too large for adv packet
                .setIncludeTxPowerLevel(false); // Save bytes
        
        try {
            // This is a special abbreviated format for standard BLE services
            // Service UUIDs defined in the Bluetooth SIG can use the 16-bit format
            ParcelUuid shortHidUuid = ParcelUuid.fromString("0000" + "1812" + "-0000-1000-8000-00805f9b34fb");
            dataBuilder.addServiceUuid(shortHidUuid);
            
            Log.d(TAG, "📦 Added shortened HID service UUID to advertisement data");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error adding service UUID: " + e.getMessage());
        }
        
        return dataBuilder.build();
    }

    /**
     * Builds a minimal scan response data payload.
     * 
     * @return The configured AdvertiseData for scan response
     */
    private AdvertiseData buildSimplifiedScanResponseData() {
        // Minimal scan response - just include device name, no additional data
        AdvertiseData.Builder responseBuilder = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false); // Omit TX power level to save space
        
        Log.d(TAG, "📦 Created minimal scan response with only device name");
        return responseBuilder.build();
    }
    
    /**
     * Gets the last error message from an advertising attempt.
     * 
     * @return The last error message, or null if no error occurred
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }
    
    /**
     * Stops advertising if currently advertising.
     */
    public void stopAdvertising() {
        if (!isAdvertising || bluetoothLeAdvertiser == null) {
            return;
        }
        
        try {
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            isAdvertising = false;
            Log.i(TAG, "🛑 BLE Advertising stopped");
            showToast("Advertising stopped");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to stop advertising", e);
        }
    }
    
    /**
     * Converts an advertising error code to a human-readable message.
     * 
     * @param errorCode The error code from AdvertiseCallback
     * @return A human-readable error message
     */
    private String getAdvertiseErrorMessage(int errorCode) {
        switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                return "Advertising already started";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "Advertising data too large";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "Advertising feature unsupported";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                return "Advertising internal error";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "Too many advertisers";
            default:
                return "Unknown error: " + errorCode;
        }
    }
    
    /**
     * Checks if currently advertising.
     * 
     * @return true if advertising, false otherwise
     */
    public boolean isAdvertising() {
        return isAdvertising;
    }
    
    /**
     * Get device reports of peripheral mode support
     */
    public boolean getDeviceReportedPeripheralSupport() {
        return bluetoothAdapter != null && 
               bluetoothAdapter.isMultipleAdvertisementSupported() && 
               bluetoothAdapter.getBluetoothLeAdvertiser() != null;
    }
    
    /**
     * Returns diagnostic information about advertising attempts.
     */
    public String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("ADVERTISING DIAGNOSTICS:\n");
        info.append("- Attempts: ").append(advertisingAttempts).append("\n");
        info.append("- Successes: ").append(advertisingSuccess).append("\n");
        info.append("- Failures: ").append(advertisingFailures).append("\n");
        info.append("- Force advertising: ").append(forceAdvertising).append("\n");
        info.append("- Currently advertising: ").append(isAdvertising).append("\n");
        info.append("- Device reports peripheral support: ").append(getDeviceReportedPeripheralSupport()).append("\n");
        info.append("- Last error: ").append(lastErrorMessage != null ? lastErrorMessage : "None").append("\n");
        return info.toString();
    }
}
