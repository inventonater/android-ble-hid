package com.inventonater.blehid.unity;

import android.util.Log;

import com.unity3d.player.UnityPlayer;
import com.inventonater.blehid.core.BleConnectionManager;

public class BleHidUnityBridge {
    private static final String TAG = "BleHidUnityBridge";
    private static BleHidUnityBridge instance;
    private BleHidUnityPlugin plugin;
    private UnityMessenger unityMessenger;

    // called from Unity
    public static synchronized BleHidUnityBridge getInstance() {
        if (instance == null) instance = new BleHidUnityBridge();
        return instance;
    }

    private BleHidUnityBridge() {
        plugin = new BleHidUnityPlugin();
    }

    // called from Unity
    public boolean initialize(final String gameObjectName) {
        Log.d(TAG, "Initializing BLE HID with callback to Unity GameObject: " + gameObjectName);

        // Start the foreground service immediately on initialization
        Log.d(TAG, "Starting foreground service on initialization");
        boolean serviceStarted = BleHidUnityPlugin.startForegroundService();
        if (serviceStarted) {
            Log.d(TAG, "Foreground service start requested successfully");
        } else {
            Log.e(TAG, "Failed to start foreground service");
        }

        unityMessenger = new UnityMessenger(gameObjectName);
        BleHidUnityCallback callback = new BleHidUnityCallback(unityMessenger);

        return plugin.initialize(UnityPlayer.currentActivity, callback);
    }

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

    public boolean startForegroundService() {
        return BleHidUnityPlugin.startForegroundService();
    }

    public boolean stopForegroundService() {
        return BleHidUnityPlugin.stopForegroundService();
    }

    public boolean setBleIdentity(String identityUuid, String deviceName) {
        return plugin.setBleIdentity(identityUuid, deviceName);
    }

    public java.util.List<java.util.Map<String, String>> getBondedDevicesInfo() {
        return plugin.getBondedDevicesInfo();
    }

    public boolean isDeviceBonded(String address) {
        return plugin.isDeviceBonded(address);
    }

    public boolean removeBond(String address) {
        return plugin.removeBond(address);
    }

    public boolean initializeLocalControl() {
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

    public boolean localTap(int x, int y) {
        return plugin.localTap(x, y);
    }

    public boolean localSwipeBegin(float startX, float startY) {
        return plugin.localSwipeBegin(startX, startY);
    }

    public boolean localSwipeExtend(float deltaX, float deltaY) {
        return plugin.localSwipeExtend(deltaX, deltaY);
    }

    public boolean localSwipeEnd() {
        return plugin.localSwipeEnd();
    }

    public boolean performGlobalAction(int globalAction) {
        return plugin.performGlobalAction(globalAction);
    }

    public boolean localPerformFocusedNodeAction(int action) {
        return plugin.localPerformFocusedNodeAction(action);
    }

    public boolean localClickFocusedNode() {
        return plugin.localClickFocusedNode();
    }

    public boolean launchCameraApp() {
        return plugin.launchCameraApp();
    }

    public boolean launchPhotoCapture() {
        return plugin.launchPhotoCapture();
    }

    public boolean launchVideoCapture() {
        return plugin.launchVideoCapture();
    }

    public boolean takePicture() {
        return plugin.takePicture();
    }

    public boolean takePicture(android.os.Bundle optionsBundle) {
        return plugin.takePicture(optionsBundle);
    }

    public boolean takePictureWithOptions(android.os.Bundle params) {
        return plugin.takePicture(params);
    }

    public boolean recordVideo() {
        return plugin.recordVideo((android.os.Bundle) null);
    }

    public boolean recordVideo(long durationMs) {
        return plugin.recordVideo(durationMs);
    }

    public boolean recordVideo(android.os.Bundle optionsBundle) {
        return plugin.recordVideo(optionsBundle);
    }

    public boolean recordVideoWithOptions(android.os.Bundle params) {
        return plugin.recordVideo(params);
    }

    public boolean requestConnectionPriority(int priority) {
        return plugin.requestConnectionPriority(priority);
    }

    public boolean requestMtu(int mtu) {
        return plugin.requestMtu(mtu);
    }

    public boolean setTransmitPowerLevel(int level) {
        return plugin.setTransmitPowerLevel(level);
    }

    public boolean readRssi() {
        return plugin.readRssi();
    }

    public java.util.Map<String, String> getConnectionParameters() {
        return plugin.getConnectionParameters();
    }

    // Connection priority constants for Unity
    public int getConnectionPriorityHigh() {
        return BleConnectionManager.CONNECTION_PRIORITY_HIGH;
    }

    public int getConnectionPriorityBalanced() {
        return BleConnectionManager.CONNECTION_PRIORITY_BALANCED;
    }

    public int getConnectionPriorityLowPower() {
        return BleConnectionManager.CONNECTION_PRIORITY_LOW_POWER;
    }

    // TX power level constants for Unity
    public int getTxPowerLevelHigh() {
        return BleConnectionManager.TX_POWER_LEVEL_HIGH;
    }

    public int getTxPowerLevelMedium() {
        return BleConnectionManager.TX_POWER_LEVEL_MEDIUM;
    }

    public int getTxPowerLevelLow() {
        return BleConnectionManager.TX_POWER_LEVEL_LOW;
    }

    public void notifyPipModeChanged(boolean isInPipMode) {
        plugin.notifyPipModeChanged(isInPipMode);
    }
}
