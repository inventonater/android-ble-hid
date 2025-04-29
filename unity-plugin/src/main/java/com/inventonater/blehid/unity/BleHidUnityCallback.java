package com.inventonater.blehid.unity;

import android.util.Log;

import com.unity3d.player.UnityPlayer;

public class BleHidUnityCallback {

    private static final String TAG = "BleHidUnityCallback";

    private final String unityGameObjectName;

    public BleHidUnityCallback(String gameObjectName) {
        unityGameObjectName = gameObjectName;
    }

    public void sendMessageToUnity(String methodName, String message) {
        if (unityGameObjectName != null && !unityGameObjectName.isEmpty()) {
            UnityPlayer.UnitySendMessage(unityGameObjectName, methodName, message);
        } else {
            Log.e(TAG, "Cannot send message to Unity: GameObject name is not set");
        }
    }

    public void onInitializeComplete(boolean success, String message) {
        sendMessageToUnity("HandleInitializeComplete", success + ":" + message);
    }

    public void onAdvertisingStateChanged(boolean advertising, String message) {
        sendMessageToUnity("HandleAdvertisingStateChanged", advertising + ":" + message);
    }

    public void onConnectionStateChanged(boolean connected, String deviceName, String deviceAddress) {
        String message = connected ? connected + ":" + deviceName + ":" + deviceAddress : connected + ":";
        sendMessageToUnity("HandleConnectionStateChanged", message);
    }

    public void onPairingStateChanged(String status, String deviceAddress) {
        sendMessageToUnity("HandlePairingStateChanged", status + ":" + deviceAddress);
    }

    public void onConnectionParametersChanged(int interval, int latency, int timeout, int mtu) {
        sendMessageToUnity("HandleConnectionParametersChanged", interval + ":" + latency + ":" + timeout + ":" + mtu);
    }

    public void onRssiRead(int rssi) {
        sendMessageToUnity("HandleRssiRead", String.valueOf(rssi));
    }

    public void onConnectionParameterRequestComplete(String parameterName, boolean success, String actualValue) {
        sendMessageToUnity("HandleConnectionParameterRequestComplete", parameterName + ":" + success + ":" + actualValue);
    }

    public void onPipModeChanged(boolean isInPipMode) {
        sendMessageToUnity("HandlePipModeChanged", isInPipMode + ":");
    }

    public void onError(int errorCode, String errorMessage) {
        sendMessageToUnity("HandleError", errorCode + ":" + errorMessage);
    }

    public void onDebugLog(String message) {
        sendMessageToUnity("HandleDebugLog", message);
    }
}
