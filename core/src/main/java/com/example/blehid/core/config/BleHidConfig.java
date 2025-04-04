package com.example.blehid.core.config;

/**
 * Configuration for the BLE HID stack.
 * This class provides configuration options for various aspects of the BLE HID implementation.
 */
public class BleHidConfig {
    private DeviceConfig deviceConfig = new DeviceConfig();
    private AdvertisingConfig advertisingConfig = new AdvertisingConfig();
    private ServiceConfig serviceConfig = new ServiceConfig();
    private ConnectionConfig connectionConfig = new ConnectionConfig();
    
    /**
     * Gets the device configuration.
     *
     * @return The device configuration
     */
    public DeviceConfig getDeviceConfig() {
        return deviceConfig;
    }
    
    /**
     * Sets the device configuration.
     *
     * @param deviceConfig The device configuration
     */
    public void setDeviceConfig(DeviceConfig deviceConfig) {
        this.deviceConfig = deviceConfig != null ? deviceConfig : new DeviceConfig();
    }
    
    /**
     * Gets the advertising configuration.
     *
     * @return The advertising configuration
     */
    public AdvertisingConfig getAdvertisingConfig() {
        return advertisingConfig;
    }
    
    /**
     * Sets the advertising configuration.
     *
     * @param advertisingConfig The advertising configuration
     */
    public void setAdvertisingConfig(AdvertisingConfig advertisingConfig) {
        this.advertisingConfig = advertisingConfig != null ? advertisingConfig : new AdvertisingConfig();
    }
    
    /**
     * Gets the service configuration.
     *
     * @return The service configuration
     */
    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }
    
    /**
     * Sets the service configuration.
     *
     * @param serviceConfig The service configuration
     */
    public void setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig != null ? serviceConfig : new ServiceConfig();
    }
    
    /**
     * Gets the connection configuration.
     *
     * @return The connection configuration
     */
    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }
    
    /**
     * Sets the connection configuration.
     *
     * @param connectionConfig The connection configuration
     */
    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig != null ? connectionConfig : new ConnectionConfig();
    }
    
    /**
     * Device-specific configuration.
     */
    public static class DeviceConfig {
        private String deviceName = "BLE HID Device";
        private boolean requireBonding = true;
        private int txPowerLevel = 0; // Default power level
        
        /**
         * Gets the device name.
         *
         * @return The device name
         */
        public String getDeviceName() {
            return deviceName;
        }
        
        /**
         * Sets the device name.
         *
         * @param deviceName The device name
         */
        public void setDeviceName(String deviceName) {
            this.deviceName = deviceName != null && !deviceName.isEmpty() ? deviceName : "BLE HID Device";
        }
        
        /**
         * Checks if bonding is required.
         *
         * @return true if bonding is required, false otherwise
         */
        public boolean isRequireBonding() {
            return requireBonding;
        }
        
        /**
         * Sets whether bonding is required.
         *
         * @param requireBonding Whether bonding is required
         */
        public void setRequireBonding(boolean requireBonding) {
            this.requireBonding = requireBonding;
        }
        
        /**
         * Gets the TX power level.
         *
         * @return The TX power level
         */
        public int getTxPowerLevel() {
            return txPowerLevel;
        }
        
        /**
         * Sets the TX power level.
         *
         * @param txPowerLevel The TX power level
         */
        public void setTxPowerLevel(int txPowerLevel) {
            this.txPowerLevel = txPowerLevel;
        }
    }
    
    /**
     * Advertising-specific configuration.
     */
    public static class AdvertisingConfig {
        private boolean includeTxPower = true;
        private boolean includeName = true;
        private int advertisingMode = 0; // Default mode (ADVERTISE_MODE_LOW_POWER)
        private int advertisingTimeout = 0; // 0 = No timeout
        
        /**
         * Checks if TX power should be included in advertising.
         *
         * @return true if TX power should be included, false otherwise
         */
        public boolean isIncludeTxPower() {
            return includeTxPower;
        }
        
        /**
         * Sets whether TX power should be included in advertising.
         *
         * @param includeTxPower Whether TX power should be included
         */
        public void setIncludeTxPower(boolean includeTxPower) {
            this.includeTxPower = includeTxPower;
        }
        
        /**
         * Checks if device name should be included in advertising.
         *
         * @return true if device name should be included, false otherwise
         */
        public boolean isIncludeName() {
            return includeName;
        }
        
        /**
         * Sets whether device name should be included in advertising.
         *
         * @param includeName Whether device name should be included
         */
        public void setIncludeName(boolean includeName) {
            this.includeName = includeName;
        }
        
        /**
         * Gets the advertising mode.
         *
         * @return The advertising mode
         */
        public int getAdvertisingMode() {
            return advertisingMode;
        }
        
        /**
         * Sets the advertising mode.
         *
         * @param advertisingMode The advertising mode
         */
        public void setAdvertisingMode(int advertisingMode) {
            this.advertisingMode = advertisingMode;
        }
        
        /**
         * Gets the advertising timeout.
         *
         * @return The advertising timeout
         */
        public int getAdvertisingTimeout() {
            return advertisingTimeout;
        }
        
        /**
         * Sets the advertising timeout.
         *
         * @param advertisingTimeout The advertising timeout
         */
        public void setAdvertisingTimeout(int advertisingTimeout) {
            this.advertisingTimeout = advertisingTimeout;
        }
    }
    
    /**
     * Service-specific configuration.
     */
    public static class ServiceConfig {
        private boolean enableMouse = true;
        private boolean enableKeyboard = true;
        private boolean enableConsumer = true;
        private byte initialProtocolMode = 0x01; // REPORT_PROTOCOL
        
        /**
         * Checks if mouse functionality is enabled.
         *
         * @return true if mouse functionality is enabled, false otherwise
         */
        public boolean isEnableMouse() {
            return enableMouse;
        }
        
        /**
         * Sets whether mouse functionality is enabled.
         *
         * @param enableMouse Whether mouse functionality is enabled
         */
        public void setEnableMouse(boolean enableMouse) {
            this.enableMouse = enableMouse;
        }
        
        /**
         * Checks if keyboard functionality is enabled.
         *
         * @return true if keyboard functionality is enabled, false otherwise
         */
        public boolean isEnableKeyboard() {
            return enableKeyboard;
        }
        
        /**
         * Sets whether keyboard functionality is enabled.
         *
         * @param enableKeyboard Whether keyboard functionality is enabled
         */
        public void setEnableKeyboard(boolean enableKeyboard) {
            this.enableKeyboard = enableKeyboard;
        }
        
        /**
         * Checks if consumer control functionality is enabled.
         *
         * @return true if consumer control functionality is enabled, false otherwise
         */
        public boolean isEnableConsumer() {
            return enableConsumer;
        }
        
        /**
         * Sets whether consumer control functionality is enabled.
         *
         * @param enableConsumer Whether consumer control functionality is enabled
         */
        public void setEnableConsumer(boolean enableConsumer) {
            this.enableConsumer = enableConsumer;
        }
        
        /**
         * Gets the initial protocol mode.
         *
         * @return The initial protocol mode
         */
        public byte getInitialProtocolMode() {
            return initialProtocolMode;
        }
        
        /**
         * Sets the initial protocol mode.
         *
         * @param initialProtocolMode The initial protocol mode
         */
        public void setInitialProtocolMode(byte initialProtocolMode) {
            this.initialProtocolMode = initialProtocolMode;
        }
    }
    
    /**
     * Connection-specific configuration.
     */
    public static class ConnectionConfig {
        private int maxPairingRetries = 3;
        private boolean autoReconnect = true;
        private boolean autoStartAdvertising = true;
        
        /**
         * Gets the maximum number of pairing retries.
         *
         * @return The maximum number of pairing retries
         */
        public int getMaxPairingRetries() {
            return maxPairingRetries;
        }
        
        /**
         * Sets the maximum number of pairing retries.
         *
         * @param maxPairingRetries The maximum number of pairing retries
         */
        public void setMaxPairingRetries(int maxPairingRetries) {
            this.maxPairingRetries = maxPairingRetries > 0 ? maxPairingRetries : 1;
        }
        
        /**
         * Checks if auto-reconnect is enabled.
         *
         * @return true if auto-reconnect is enabled, false otherwise
         */
        public boolean isAutoReconnect() {
            return autoReconnect;
        }
        
        /**
         * Sets whether auto-reconnect is enabled.
         *
         * @param autoReconnect Whether auto-reconnect is enabled
         */
        public void setAutoReconnect(boolean autoReconnect) {
            this.autoReconnect = autoReconnect;
        }
        
        /**
         * Checks if advertising should be automatically started after disconnection.
         *
         * @return true if auto-start advertising is enabled, false otherwise
         */
        public boolean isAutoStartAdvertising() {
            return autoStartAdvertising;
        }
        
        /**
         * Sets whether advertising should be automatically started after disconnection.
         *
         * @param autoStartAdvertising Whether auto-start advertising is enabled
         */
        public void setAutoStartAdvertising(boolean autoStartAdvertising) {
            this.autoStartAdvertising = autoStartAdvertising;
        }
    }
}
