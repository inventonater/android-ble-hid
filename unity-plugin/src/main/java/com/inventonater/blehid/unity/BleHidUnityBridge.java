package com.inventonater.blehid.unity;

import android.app.Activity;
import android.util.Log;
import com.unity3d.player.UnityPlayer;
import com.inventonater.blehid.core.BleConnectionManager;

/**
 * Bridge class between Unity and the BLE HID plugin.
 * This class handles communication between Unity C# code and the Java plugin.
 */
public class BleHidUnityBridge {
    private static final String TAG = "BleHidUnityBridge";
    private static BleHidUnityBridge instance;
    private BleHidUnityPlugin plugin;
    private String unityGameObjectName;

    /**
     * Get the singleton instance of the bridge.
     */
    public static synchronized BleHidUnityBridge getInstance() {
        if (instance == null) {
            instance = new BleHidUnityBridge();
        }
        return instance;
    }
    
    /**
     * Get the BleHidManager instance from the plugin.
     * This is used by the BleHidPlugin class for direct access.
     * 
     * @return The BleHidManager instance
     */
    public com.inventonater.blehid.core.BleHidManager getBleHidManager() {
        return plugin.getBleHidManager();
    }

    private BleHidUnityBridge() {
        // Private constructor to enforce singleton pattern
        plugin = BleHidUnityPlugin.getInstance();
    }

    /**
     * Initialize the BLE HID functionality for Unity.
     * This method is called from Unity.
     *
     * @param gameObjectName The name of the Unity GameObject that will receive callbacks
     * @return true if initialization succeeded, false otherwise
     */
    public boolean initialize(final String gameObjectName) {
        Log.d(TAG, "Initializing BLE HID with callback to Unity GameObject: " + gameObjectName);
        this.unityGameObjectName = gameObjectName;
        
        // Start the foreground service immediately on initialization
        Log.d(TAG, "Starting foreground service on initialization");
        boolean serviceStarted = BleHidUnityPlugin.startForegroundService();
        if (serviceStarted) {
            Log.d(TAG, "Foreground service start requested successfully");
        } else {
            Log.e(TAG, "Failed to start foreground service");
        }

        // Create a callback interface for the plugin to communicate back to Unity
        BleHidUnityCallback callback = new BleHidUnityCallback() {
            @Override
            public void onInitializeComplete(boolean success, String message) {
                sendMessageToUnity("HandleInitializeComplete", success + ":" + message);
            }

            @Override
            public void onAdvertisingStateChanged(boolean advertising, String message) {
                sendMessageToUnity("HandleAdvertisingStateChanged", advertising + ":" + message);
            }

            @Override
            public void onConnectionStateChanged(boolean connected, String deviceName, String deviceAddress) {
                String message = connected ? connected + ":" + deviceName + ":" + deviceAddress : connected + ":";
                sendMessageToUnity("HandleConnectionStateChanged", message);
            }

            @Override
            public void onPairingStateChanged(String status, String deviceAddress) {
                sendMessageToUnity("HandlePairingStateChanged", status + ":" + deviceAddress);
            }
            
            @Override
            public void onConnectionParametersChanged(int interval, int latency, int timeout, int mtu) {
                sendMessageToUnity("HandleConnectionParametersChanged", interval + ":" + latency + ":" + timeout + ":" + mtu);
            }
            
            @Override
            public void onRssiRead(int rssi) {
                sendMessageToUnity("HandleRssiRead", String.valueOf(rssi));
            }
            
            @Override
            public void onConnectionParameterRequestComplete(String parameterName, boolean success, String actualValue) {
                sendMessageToUnity("HandleConnectionParameterRequestComplete", parameterName + ":" + success + ":" + actualValue);
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                sendMessageToUnity("HandleError", errorCode + ":" + errorMessage);
            }

            @Override
            public void onDebugLog(String message) {
                sendMessageToUnity("HandleDebugLog", message);
            }
            
            @Override
            public void onPipModeChanged(boolean isInPipMode) {
                sendMessageToUnity("HandlePipModeChanged", String.valueOf(isInPipMode));
            }
        };

        // Get the Unity activity and initialize the plugin
        Activity activity = UnityPlayer.currentActivity;
        return plugin.initialize(activity, callback);
    }

    /**
     * Send a message to the Unity GameObject.
     *
     * @param methodName The name of the method to call on the GameObject
     * @param message The message to pass to the method
     */
    private void sendMessageToUnity(String methodName, String message) {
        if (unityGameObjectName != null && !unityGameObjectName.isEmpty()) {
            UnityPlayer.UnitySendMessage(unityGameObjectName, methodName, message);
        } else {
            Log.e(TAG, "Cannot send message to Unity: GameObject name is not set");
        }
    }

    // Forward the rest of the methods to the plugin

    public boolean startAdvertising() {
        return plugin.startAdvertising();
    }

    public void stopAdvertising() {
        plugin.stopAdvertising();
    }

    public boolean isAdvertising() {
        return plugin.isAdvertising();
    }

    public boolean isConnected() {
        return plugin.isConnected();
    }

    /**
     * Disconnect from the currently connected device.
     * 
     * @return true if disconnect was successful or already disconnected, false otherwise
     */
    public boolean disconnect() {
        return plugin.disconnect();
    }

    public String[] getConnectedDeviceInfo() {
        return plugin.getConnectedDeviceInfo();
    }

    public boolean sendKey(int keyCode) {
        return plugin.sendKey((byte) keyCode, (byte) 0);
    }

    public boolean sendKeyWithModifiers(int keyCode, int modifiers) {
        return plugin.sendKey((byte) keyCode, (byte) modifiers);
    }

    public boolean typeText(String text) {
        return plugin.typeText(text);
    }

    public boolean moveMouse(int deltaX, int deltaY) {
        return plugin.moveMouse(deltaX, deltaY);
    }

    public boolean pressMouseButton(int button) {
        return plugin.pressMouseButton(button);
    }

    public boolean releaseMouseButton(int button) {
        return plugin.releaseMouseButton(button);
    }

    public boolean clickMouseButton(int button) {
        return plugin.clickMouseButton(button);
    }

    public boolean playPause() {
        return plugin.playPause();
    }

    public boolean nextTrack() {
        return plugin.nextTrack();
    }

    public boolean previousTrack() {
        return plugin.previousTrack();
    }

    public boolean volumeUp() {
        return plugin.volumeUp();
    }

    public boolean volumeDown() {
        return plugin.volumeDown();
    }

    public boolean mute() {
        return plugin.mute();
    }

    public String getDiagnosticInfo() {
        return plugin.getDiagnosticInfo();
    }

    public void close() {
        // Stop the foreground service when closing the plugin
        Log.d(TAG, "Stopping foreground service on plugin close");
        boolean serviceStopped = BleHidUnityPlugin.stopForegroundService();
        if (serviceStopped) {
            Log.d(TAG, "Foreground service stop requested successfully");
        } else {
            Log.e(TAG, "Failed to stop foreground service");
        }
        
        // Close the plugin
        plugin.close();
    }
    
    /**
     * Explicitly start the foreground service.
     * This can be called from Unity if needed.
     * 
     * @return true if the service start request was sent
     */
    public boolean startForegroundService() {
        return BleHidUnityPlugin.startForegroundService();
    }
    
    /**
     * Explicitly stop the foreground service.
     * This can be called from Unity if needed.
     * 
     * @return true if the service stop request was sent
     */
    public boolean stopForegroundService() {
        return BleHidUnityPlugin.stopForegroundService();
    }
    
    // ===================== Identity Management Methods =====================
    
    /**
     * Sets the BLE peripheral identity (UUID and device name).
     * This helps maintain a consistent identity across app restarts
     * for persistent device recognition and pairing.
     * 
     * @param identityUuid The UUID string to use as the device's unique identifier
     * @param deviceName Optional custom device name (can be null for default)
     * @return true if identity was set successfully
     */
    public boolean setBleIdentity(String identityUuid, String deviceName) {
        return plugin.setBleIdentity(identityUuid, deviceName);
    }
    
    /**
     * Gets detailed information about all devices currently bonded to this peripheral.
     * 
     * @return List of maps containing device information
     */
    public java.util.List<java.util.Map<String, String>> getBondedDevicesInfo() {
        return plugin.getBondedDevicesInfo();
    }
    
    /**
     * Checks if a specific device is bonded to this peripheral.
     * 
     * @param address The MAC address of the device to check
     * @return true if the device is bonded
     */
    public boolean isDeviceBonded(String address) {
        return plugin.isDeviceBonded(address);
    }
    
    /**
     * Removes a bond with a specific device.
     * 
     * @param address The MAC address of the device to forget
     * @return true if the device was forgotten or already not bonded
     */
    public boolean removeBond(String address) {
        return plugin.removeBond(address);
    }
    
    // Local Control methods
    
    public boolean initializeLocalControl() {
        // Always refresh the activity reference first
        Activity activity = UnityPlayer.currentActivity;
        if (activity == null) {
            Log.e(TAG, "Unity activity is null - not ready for initialization");
            return false;
        }
        
        // Update the plugin's activity reference and initialize
        plugin.updateUnityActivity(activity);
        return plugin.initializeLocalControl();
    }

    public boolean isAccessibilityServiceEnabled() {
        return plugin.isAccessibilityServiceEnabled();
    }

    public void openAccessibilitySettings() {
        plugin.openAccessibilitySettings();
    }

    // Media control methods
    public boolean localPlayPause() {
        return plugin.localPlayPause();
    }

    public boolean localNextTrack() {
        return plugin.localNextTrack();
    }

    public boolean localPreviousTrack() {
        return plugin.localPreviousTrack();
    }

    public boolean localVolumeUp() {
        return plugin.localVolumeUp();
    }

    public boolean localVolumeDown() {
        return plugin.localVolumeDown();
    }

    public boolean localMute() {
        return plugin.localMute();
    }

    // Input control methods
    public boolean localTap(int x, int y) {
        return plugin.localTap(x, y);
    }

    public boolean localSwipe(int x1, int y1, int x2, int y2) {
        return plugin.localSwipe(x1, y1, x2, y2);
    }

    public boolean localNavigate(int direction) {
        return plugin.localNavigate(direction);
    }
    
    // Camera control methods
    
    public boolean launchCameraApp() {
        return plugin.launchCameraApp();
    }
    
    public boolean launchPhotoCapture() {
        return plugin.launchPhotoCapture();
    }
    
    public boolean launchVideoCapture() {
        return plugin.launchVideoCapture();
    }
    
    /**
     * Take a picture with the camera using default options.
     * 
     * @return true if camera was launched successfully
     */
    public boolean takePicture() {
        return plugin.takePicture();
    }
    
    /**
     * Take a picture with the camera using the specified options bundle.
     * 
     * @param optionsBundle Bundle with camera options parameters
     * @return true if camera was launched successfully
     */
    public boolean takePicture(android.os.Bundle optionsBundle) {
        return plugin.takePicture(optionsBundle);
    }
    
    /**
     * Take a picture with custom parameters.
     * This method is maintained for backward compatibility and passes the bundle directly.
     * 
     * @param params Bundle containing parameters for the camera capture
     * @return true if camera was launched successfully
     */
    public boolean takePictureWithOptions(android.os.Bundle params) {
        return plugin.takePicture(params);
    }
    
    /**
     * Record a video with default duration of 5 seconds.
     * 
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo() {
        return plugin.recordVideo((android.os.Bundle)null);
    }
    
    /**
     * Record a video with the specified duration in milliseconds.
     * 
     * @param durationMs Duration in milliseconds to record
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo(long durationMs) {
        return plugin.recordVideo(durationMs);
    }
    
    /**
     * Record a video with the specified options bundle.
     * 
     * @param optionsBundle Bundle with video options parameters
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo(android.os.Bundle optionsBundle) {
        return plugin.recordVideo(optionsBundle);
    }
    
    /**
     * Record a video with custom parameters.
     * This method is maintained for backward compatibility and passes the bundle directly.
     * 
     * @param params Bundle containing parameters for the video recording
     * @return true if video recording was launched successfully
     */
    public boolean recordVideoWithOptions(android.os.Bundle params) {
        return plugin.recordVideo(params);
    }
    

    // ==================== Connection Parameter Methods ====================
    
    /**
     * Request a change in connection priority.
     * 
     * @param priority The connection priority: 0=HIGH, 1=BALANCED, 2=LOW_POWER
     * @return true if the request was sent, false otherwise
     */
    public boolean requestConnectionPriority(int priority) {
        return plugin.requestConnectionPriority(priority);
    }
    
    /**
     * Request a change in MTU size.
     * 
     * @param mtu The MTU size (23-517)
     * @return true if the request was sent, false otherwise
     */
    public boolean requestMtu(int mtu) {
        return plugin.requestMtu(mtu);
    }
    
    /**
     * Set the transmit power level for advertising.
     * 
     * @param level The power level: 0=LOW, 1=MEDIUM, 2=HIGH
     * @return true if successful, false otherwise
     */
    public boolean setTransmitPowerLevel(int level) {
        return plugin.setTransmitPowerLevel(level);
    }
    
    /**
     * Read the current RSSI value.
     * 
     * @return true if the read request was sent, false otherwise
     */
    public boolean readRssi() {
        return plugin.readRssi();
    }
    
    /**
     * Get all connection parameters as a string map.
     * 
     * @return Map of parameter names to values, or null if not connected
     */
    public java.util.Map<String, String> getConnectionParameters() {
        return plugin.getConnectionParameters();
    }
    
    // Navigation constants for Unity
    public int getNavUp() { return BleHidUnityPlugin.NAV_UP; }
    public int getNavDown() { return BleHidUnityPlugin.NAV_DOWN; }
    public int getNavLeft() { return BleHidUnityPlugin.NAV_LEFT; }
    public int getNavRight() { return BleHidUnityPlugin.NAV_RIGHT; }
    public int getNavBack() { return BleHidUnityPlugin.NAV_BACK; }
    public int getNavHome() { return BleHidUnityPlugin.NAV_HOME; }
    public int getNavRecents() { return BleHidUnityPlugin.NAV_RECENTS; }
    
    // Connection priority constants for Unity
    public int getConnectionPriorityHigh() { return BleConnectionManager.CONNECTION_PRIORITY_HIGH; }
    public int getConnectionPriorityBalanced() { return BleConnectionManager.CONNECTION_PRIORITY_BALANCED; }
    public int getConnectionPriorityLowPower() { return BleConnectionManager.CONNECTION_PRIORITY_LOW_POWER; }
    
    // TX power level constants for Unity
    public int getTxPowerLevelHigh() { return BleConnectionManager.TX_POWER_LEVEL_HIGH; }
    public int getTxPowerLevelMedium() { return BleConnectionManager.TX_POWER_LEVEL_MEDIUM; }
    public int getTxPowerLevelLow() { return BleConnectionManager.TX_POWER_LEVEL_LOW; }
    
    /**
     * Notify Unity about PiP mode changes.
     * This method is called from BleHidUnityPlayerActivity when PiP mode changes.
     * 
     * @param isInPipMode true if entering PiP mode, false if exiting
     */
    public void notifyPipModeChanged(boolean isInPipMode) {
        Log.d(TAG, "Notifying Unity about PiP mode change: " + isInPipMode);
        sendMessageToUnity("HandlePipModeChanged", String.valueOf(isInPipMode));
    }
}
