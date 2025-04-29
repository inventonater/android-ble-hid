package com.inventonater.blehid.unity;

import android.util.Log;

import com.unity3d.player.UnityPlayer;

public class UnityMessenger {

    private static final String TAG = "UnityMessenger";

    private final String unityGameObjectName;

    public UnityMessenger(String gameObjectName) {
        unityGameObjectName = gameObjectName;
    }

    public void sendMessageToUnity(String methodName, String message) {
        if (unityGameObjectName != null && !unityGameObjectName.isEmpty()) {
            UnityPlayer.UnitySendMessage(unityGameObjectName, methodName, message);
        } else {
            Log.e(TAG, "Cannot send message to Unity: GameObject name is not set");
        }
    }
}
