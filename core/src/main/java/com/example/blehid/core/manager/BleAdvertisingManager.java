package com.example.blehid.core.manager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import com.example.blehid.core.HidConstants;
import com.example.blehid.core.config.BleHidConfig;

import java.util.UUID;

/**
 * Manages BLE advertising operations.
 * This class is responsible for starting and stopping advertising, and configuring
 * advertising parameters.
 */
public class BleAdvertisingManager {
    private static final String TAG = "BleAdvertisingManager";
    
    private final BleLifecycleManager lifecycleManager;
    private final BleConnectionManager connectionManager;
    private BluetoothLeAdvertiser advertiser;
    private boolean isAdvertising = false;
    
    /**
     * Creates a new BLE advertising manager.
     *
     * @param lifecycleManager The BLE lifecycle manager
     * @param connectionManager The BLE connection manager
     */
    public BleAdvertisingManager(BleLifecycleManager lifecycleManager, BleConnectionManager connectionManager) {
        this.lifecycleManager = lifecycleManager;
        this.connectionManager = connectionManager;
    }
    
    /**
     * Starts advertising.
     *
     * @return true if successful, false otherwise
     */
    public boolean startAdvertising() {
        if (isAdvertising) {
            Log.w(TAG, "Already advertising");
            return true;
        }
        
        if (connectionManager.isConnected()) {
            Log.w(TAG, "Device already connected, not starting advertising");
            return false;
        }
        
        BluetoothAdapter bluetoothAdapter = lifecycleManager.getBluetoothAdapter();
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth adapter not available");
            return false;
        }
        
        advertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (advertiser == null) {
            Log.e(TAG, "Bluetooth LE advertiser not available");
            return false;
        }
        
        // Build advertising settings
        AdvertiseSettings settings = buildAdvertiseSettings();
        
        // Build advertising data
        AdvertiseData advertiseData = buildAdvertiseData();
        
        // Build scan response data
        AdvertiseData scanResponseData = buildScanResponseData();
        
        // Start advertising
        try {
            advertiser.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback);
            isAdvertising = true;
            Log.i(TAG, "BLE advertising started");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to start advertising", e);
            return false;
        }
    }
    
    /**
     * Stops advertising.
     */
    public void stopAdvertising() {
        if (!isAdvertising) {
            Log.d(TAG, "Not advertising");
            return;
        }
        
        if (advertiser != null) {
            try {
                advertiser.stopAdvertising(advertiseCallback);
                Log.i(TAG, "BLE advertising stopped");
            } catch (Exception e) {
                Log.e(TAG, "Failed to stop advertising", e);
            }
        }
        
        isAdvertising = false;
    }
    
    /**
     * Checks if advertising is currently active.
     *
     * @return true if advertising, false otherwise
     */
    public boolean isAdvertising() {
        return isAdvertising;
    }
    
    /**
     * Builds advertising settings.
     *
     * @return The advertising settings
     */
    private AdvertiseSettings buildAdvertiseSettings() {
        BleHidConfig config = lifecycleManager.getConfig();
        BleHidConfig.AdvertisingConfig advertisingConfig = config.getAdvertisingConfig();
        
        AdvertiseSettings.Builder builder = new AdvertiseSettings.Builder();
        
        // Set advertising mode
        int mode = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
        if (advertisingConfig.getAdvertisingMode() >= 0 && 
                advertisingConfig.getAdvertisingMode() <= 2) {
            mode = advertisingConfig.getAdvertisingMode();
        }
        builder.setAdvertiseMode(mode);
        
        // Set TX power level
        int txPowerLevel = config.getDeviceConfig().getTxPowerLevel();
        if (txPowerLevel >= 0 && txPowerLevel <= 3) {
            builder.setTxPowerLevel(txPowerLevel);
        } else {
            builder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM);
        }
        
        // Set connectable
        builder.setConnectable(true);
        
        // Set timeout
        int timeout = advertisingConfig.getAdvertisingTimeout();
        if (timeout > 0) {
            builder.setTimeout(timeout);
        }
        
        return builder.build();
    }
    
    /**
     * Builds advertising data.
     *
     * @return The advertising data
     */
    private AdvertiseData buildAdvertiseData() {
        BleHidConfig config = lifecycleManager.getConfig();
        BleHidConfig.AdvertisingConfig advertisingConfig = config.getAdvertisingConfig();
        
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        
        // Add HID service UUID
        builder.addServiceUuid(new ParcelUuid(HidConstants.HID_SERVICE_UUID));
        
        // Include TX power level if configured
        builder.setIncludeTxPowerLevel(advertisingConfig.isIncludeTxPower());
        
        // Include device name if configured
        builder.setIncludeDeviceName(advertisingConfig.isIncludeName());
        
        return builder.build();
    }
    
    /**
     * Builds scan response data.
     *
     * @return The scan response data
     */
    private AdvertiseData buildScanResponseData() {
        AdvertiseData.Builder builder = new AdvertiseData.Builder();
        
        // We don't include any extra data in the scan response for now,
        // but this could be extended in the future.
        
        return builder.build();
    }
    
    /**
     * Advertise callback.
     */
    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "Advertising started successfully");
            isAdvertising = true;
        }
        
        @Override
        public void onStartFailure(int errorCode) {
            isAdvertising = false;
            String reason;
            switch (errorCode) {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    reason = "already started";
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    reason = "data too large";
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    reason = "feature unsupported";
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    reason = "internal error";
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    reason = "too many advertisers";
                    break;
                default:
                    reason = "unknown error: " + errorCode;
                    break;
            }
            Log.e(TAG, "Advertising failed to start: " + reason);
        }
    };
    
    /**
     * Creates an advertising builder.
     *
     * @return A new builder
     */
    public AdvertisingBuilder createBuilder() {
        return new AdvertisingBuilder();
    }
    
    /**
     * Builder for advertising settings.
     */
    public class AdvertisingBuilder {
        private boolean includeTxPower = true;
        private boolean includeName = true;
        private int advertisingMode = AdvertiseSettings.ADVERTISE_MODE_LOW_POWER;
        private int txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM;
        private int timeout = 0; // No timeout
        
        /**
         * Sets whether to include TX power level in advertising data.
         *
         * @param includeTxPower Whether to include TX power level
         * @return This builder for chaining
         */
        public AdvertisingBuilder setIncludeTxPower(boolean includeTxPower) {
            this.includeTxPower = includeTxPower;
            return this;
        }
        
        /**
         * Sets whether to include device name in advertising data.
         *
         * @param includeName Whether to include device name
         * @return This builder for chaining
         */
        public AdvertisingBuilder setIncludeName(boolean includeName) {
            this.includeName = includeName;
            return this;
        }
        
        /**
         * Sets the advertising mode.
         *
         * @param advertisingMode The advertising mode
         * @return This builder for chaining
         */
        public AdvertisingBuilder setAdvertisingMode(int advertisingMode) {
            this.advertisingMode = advertisingMode;
            return this;
        }
        
        /**
         * Sets the TX power level.
         *
         * @param txPowerLevel The TX power level
         * @return This builder for chaining
         */
        public AdvertisingBuilder setTxPowerLevel(int txPowerLevel) {
            this.txPowerLevel = txPowerLevel;
            return this;
        }
        
        /**
         * Sets the advertising timeout.
         *
         * @param timeout The timeout in milliseconds
         * @return This builder for chaining
         */
        public AdvertisingBuilder setTimeout(int timeout) {
            this.timeout = timeout;
            return this;
        }
        
        /**
         * Applies the settings to the configuration.
         */
        public void apply() {
            BleHidConfig config = lifecycleManager.getConfig();
            BleHidConfig.AdvertisingConfig advertisingConfig = config.getAdvertisingConfig();
            
            advertisingConfig.setIncludeTxPower(includeTxPower);
            advertisingConfig.setIncludeName(includeName);
            advertisingConfig.setAdvertisingMode(advertisingMode);
            config.getDeviceConfig().setTxPowerLevel(txPowerLevel);
            advertisingConfig.setAdvertisingTimeout(timeout);
        }
    }
    
    /**
     * Cleans up resources.
     */
    public void close() {
        stopAdvertising();
    }
    
    /**
     * Gets the last error message from advertising operations.
     * 
     * @return The last error message, or null if no error
     */
    public String getLastErrorMessage() {
        if (isAdvertising) {
            return null; // No error if advertising
        }
        
        BluetoothAdapter adapter = lifecycleManager.getBluetoothAdapter();
        if (adapter == null) {
            return "Bluetooth adapter not available";
        }
        
        if (!adapter.isEnabled()) {
            return "Bluetooth is disabled";
        }
        
        if (adapter.getBluetoothLeAdvertiser() == null) {
            return "BLE advertising not supported on this device";
        }
        
        return "Unknown advertising error";
    }
    
    /**
     * Gets diagnostic information about the advertising state.
     * 
     * @return A string containing diagnostic information
     */
    public String getDiagnosticInfo() {
        StringBuilder info = new StringBuilder();
        info.append("ADVERTISING STATUS:\n");
        
        BluetoothAdapter adapter = lifecycleManager.getBluetoothAdapter();
        if (adapter == null) {
            info.append("Bluetooth adapter: Not available\n");
            return info.toString();
        }
        
        info.append("Bluetooth adapter: ").append(adapter.isEnabled() ? "Enabled" : "Disabled").append("\n");
        info.append("Device name: ").append(adapter.getName()).append("\n");
        info.append("Device address: ").append(adapter.getAddress()).append("\n");
        
        boolean supportsBle = adapter.getBluetoothLeAdvertiser() != null;
        info.append("BLE Advertising support: ").append(supportsBle ? "Yes" : "No").append("\n");
        
        info.append("Currently advertising: ").append(isAdvertising ? "Yes" : "No").append("\n");
        
        BleHidConfig config = lifecycleManager.getConfig();
        BleHidConfig.AdvertisingConfig advConfig = config.getAdvertisingConfig();
        
        info.append("Advertising config:\n");
        info.append("  - Include name: ").append(advConfig.isIncludeName()).append("\n");
        info.append("  - Include TX power: ").append(advConfig.isIncludeTxPower()).append("\n");
        info.append("  - Mode: ").append(advertisingModeToString(advConfig.getAdvertisingMode())).append("\n");
        info.append("  - TX power level: ").append(txPowerLevelToString(config.getDeviceConfig().getTxPowerLevel())).append("\n");
        info.append("  - Timeout: ").append(advConfig.getAdvertisingTimeout() == 0 ? "None" : 
                                                 advConfig.getAdvertisingTimeout() + "ms").append("\n");
        
        return info.toString();
    }
    
    /**
     * Checks if the device reports support for BLE peripheral mode.
     * 
     * @return true if the device reportedly supports peripheral mode, false otherwise
     */
    public boolean getDeviceReportedPeripheralSupport() {
        BluetoothAdapter adapter = lifecycleManager.getBluetoothAdapter();
        return adapter != null && adapter.getBluetoothLeAdvertiser() != null;
    }
    
    /**
     * Converts an advertising mode value to a readable string.
     */
    private String advertisingModeToString(int mode) {
        switch (mode) {
            case AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY:
                return "Low Latency";
            case AdvertiseSettings.ADVERTISE_MODE_BALANCED:
                return "Balanced";
            case AdvertiseSettings.ADVERTISE_MODE_LOW_POWER:
                return "Low Power";
            default:
                return "Unknown (" + mode + ")";
        }
    }
    
    /**
     * Converts a TX power level value to a readable string.
     */
    private String txPowerLevelToString(int level) {
        switch (level) {
            case AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW:
                return "Ultra Low";
            case AdvertiseSettings.ADVERTISE_TX_POWER_LOW:
                return "Low";
            case AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM:
                return "Medium";
            case AdvertiseSettings.ADVERTISE_TX_POWER_HIGH:
                return "High";
            default:
                return "Unknown (" + level + ")";
        }
    }
}
