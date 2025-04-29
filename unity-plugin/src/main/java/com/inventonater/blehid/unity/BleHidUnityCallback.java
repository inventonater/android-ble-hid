package com.inventonater.blehid.unity;

public class BleHidUnityCallback {

    private final UnityMessenger unityMessenger;

    public BleHidUnityCallback(UnityMessenger unityMessenger) {
        this.unityMessenger = unityMessenger;
    }

    private void sendMessageToUnity(String methodName, String message) {
        this.unityMessenger.sendMessageToUnity(methodName, message);
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
