package com.inventonater.blehid.unity;

import android.app.Activity;
import android.util.Log;
import com.unity3d.player.UnityPlayer;

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
            public void onError(int errorCode, String errorMessage) {
                sendMessageToUnity("HandleError", errorCode + ":" + errorMessage);
            }

            @Override
            public void onDebugLog(String message) {
                sendMessageToUnity("HandleDebugLog", message);
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

    public String[] getConnectedDeviceInfo() {
        return plugin.getConnectedDeviceInfo();
    }

    public boolean sendKey(int keyCode) {
        return plugin.sendKey((byte) keyCode);
    }

    public boolean sendKeyWithModifiers(int keyCode, int modifiers) {
        return plugin.sendKeyWithModifiers((byte) keyCode, (byte) modifiers);
    }

    public boolean typeText(String text) {
        return plugin.typeText(text);
    }

    public boolean moveMouse(int deltaX, int deltaY) {
        return plugin.moveMouse(deltaX, deltaY);
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
        plugin.close();
    }
}
