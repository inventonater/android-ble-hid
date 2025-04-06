package com.example.blehid.unity;

import android.app.Activity;
import android.util.Log;

/**
 * Bridge class to handle communication between Unity and the BleHidUnityPlugin.
 * This class implements BleHidUnityCallback and forwards all events to Unity
 * through UnitySendMessage.
 */
public class BleHidUnityBridge implements BleHidUnityCallback {
    private static final String TAG = "BleHidUnityBridge";
    
    // Unity GameObject name that will receive callbacks
    private String unityGameObjectName;
    
    // Singleton instance
    private static BleHidUnityBridge instance;
    
    /**
     * Get the singleton instance of the bridge.
     */
    public static synchronized BleHidUnityBridge getInstance() {
        if (instance == null) {
            instance = new BleHidUnityBridge();
        }
        return instance;
    }
    
    private BleHidUnityBridge() {
        // Private constructor to prevent direct instantiation
    }
    
    /**
     * Initialize the bridge with a Unity GameObject name.
     * 
     * @param gameObjectName Name of the Unity GameObject that will receive callbacks
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize(String gameObjectName) {
        if (gameObjectName == null || gameObjectName.isEmpty()) {
            Log.e(TAG, "GameObject name cannot be null or empty");
            return false;
        }
        
        this.unityGameObjectName = gameObjectName;
        Log.d(TAG, "Bridge initialized with GameObject: " + gameObjectName);
        
        // Initialize the plugin with the Unity activity
        Activity unityActivity = null;
        try {
            Class<?> unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
            unityActivity = (Activity) unityPlayerClass.getDeclaredField("currentActivity").get(null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get Unity activity: " + e.getMessage());
            // Fallback to using application context instead of activity if needed
        }
        
        return BleHidUnityPlugin.getInstance().initialize(unityActivity, this);
    }
    
    /**
     * Start BLE advertising.
     */
    public boolean startAdvertising() {
        return BleHidUnityPlugin.getInstance().startAdvertising();
    }
    
    /**
     * Stop BLE advertising.
     */
    public void stopAdvertising() {
        BleHidUnityPlugin.getInstance().stopAdvertising();
    }
    
    /**
     * Check if BLE advertising is active.
     */
    public boolean isAdvertising() {
        return BleHidUnityPlugin.getInstance().isAdvertising();
    }
    
    /**
     * Send a keyboard key press and release.
     */
    public boolean sendKey(int keyCode) {
        return BleHidUnityPlugin.getInstance().sendKey((byte)keyCode);
    }
    
    /**
     * Send a keyboard key with modifier keys.
     */
    public boolean sendKeyWithModifiers(int keyCode, int modifiers) {
        return BleHidUnityPlugin.getInstance().sendKeyWithModifiers((byte)keyCode, (byte)modifiers);
    }
    
    /**
     * Type a string of text.
     */
    public boolean typeText(String text) {
        return BleHidUnityPlugin.getInstance().typeText(text);
    }
    
    /**
     * Send a mouse movement.
     */
    public boolean moveMouse(int deltaX, int deltaY) {
        return BleHidUnityPlugin.getInstance().moveMouse(deltaX, deltaY);
    }
    
    /**
     * Click a mouse button.
     */
    public boolean clickMouseButton(int button) {
        return BleHidUnityPlugin.getInstance().clickMouseButton(button);
    }
    
    /**
     * Send a media play/pause command.
     */
    public boolean playPause() {
        return BleHidUnityPlugin.getInstance().playPause();
    }
    
    /**
     * Send a media next track command.
     */
    public boolean nextTrack() {
        return BleHidUnityPlugin.getInstance().nextTrack();
    }
    
    /**
     * Send a media previous track command.
     */
    public boolean previousTrack() {
        return BleHidUnityPlugin.getInstance().previousTrack();
    }
    
    /**
     * Send a media volume up command.
     */
    public boolean volumeUp() {
        return BleHidUnityPlugin.getInstance().volumeUp();
    }
    
    /**
     * Send a media volume down command.
     */
    public boolean volumeDown() {
        return BleHidUnityPlugin.getInstance().volumeDown();
    }
    
    /**
     * Send a media mute command.
     */
    public boolean mute() {
        return BleHidUnityPlugin.getInstance().mute();
    }
    
    /**
     * Get diagnostic information.
     */
    public String getDiagnosticInfo() {
        return BleHidUnityPlugin.getInstance().getDiagnosticInfo();
    }
    
    /**
     * Check if a device is connected.
     */
    public boolean isConnected() {
        return BleHidUnityPlugin.getInstance().isConnected();
    }
    
    /**
     * Get information about the connected device.
     */
    public String[] getConnectedDeviceInfo() {
        return BleHidUnityPlugin.getInstance().getConnectedDeviceInfo();
    }
    
    /**
     * Release all resources and close the plugin.
     */
    public void close() {
        BleHidUnityPlugin.getInstance().close();
    }
    
    //
    // BleHidUnityCallback implementations
    //
    
    @Override
    public void onInitializeComplete(boolean success, String message) {
        sendUnityMessage("HandleInitializeComplete", success + ":" + message);
    }
    
    @Override
    public void onAdvertisingStateChanged(boolean advertising, String message) {
        sendUnityMessage("HandleAdvertisingStateChanged", advertising + ":" + message);
    }
    
    @Override
    public void onConnectionStateChanged(boolean connected, String deviceName, String deviceAddress) {
        String payload = connected + ":";
        if (connected && deviceName != null && deviceAddress != null) {
            payload += deviceName + ":" + deviceAddress;
        }
        sendUnityMessage("HandleConnectionStateChanged", payload);
    }
    
    @Override
    public void onPairingStateChanged(String status, String deviceAddress) {
        String payload = status;
        if (deviceAddress != null) {
            payload += ":" + deviceAddress;
        }
        sendUnityMessage("HandlePairingStateChanged", payload);
    }
    
    @Override
    public void onError(int errorCode, String errorMessage) {
        sendUnityMessage("HandleError", errorCode + ":" + errorMessage);
    }
    
    @Override
    public void onDebugLog(String message) {
        sendUnityMessage("HandleDebugLog", message);
    }
    
    /**
     * Send a message to the Unity GameObject.
     */
    private void sendUnityMessage(String methodName, String message) {
        if (unityGameObjectName == null || unityGameObjectName.isEmpty()) {
            Log.e(TAG, "Cannot send message to Unity: GameObject name not set");
            return;
        }
        
        try {
            // Use reflection to call UnitySendMessage to avoid direct dependency
            Class<?> unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
            java.lang.reflect.Method sendMessageMethod = 
                unityPlayerClass.getDeclaredMethod("UnitySendMessage", String.class, String.class, String.class);
            sendMessageMethod.invoke(null, unityGameObjectName, methodName, message);
        } catch (Exception e) {
            Log.e(TAG, "Error sending message to Unity: " + e.getMessage());
        }
    }
}
