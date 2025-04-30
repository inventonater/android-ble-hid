package com.inventonater.blehid.unity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.inventonater.blehid.core.BleConnectionManager;
import com.inventonater.blehid.core.BleGattServerManager;
import com.inventonater.blehid.core.BleHidManager;
import com.inventonater.blehid.core.BlePairingManager;
import com.inventonater.blehid.core.HidConstants;
import com.inventonater.blehid.core.LocalInputManager;
import com.unity3d.player.UnityPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BleHidUnityBridge {
    private static final String TAG = "BleHidUnityBridge";
    private static BleHidUnityBridge instance;

    public static final int ERROR_INITIALIZATION_FAILED = 1001;
    public static final int ERROR_NOT_INITIALIZED = 1002;
    public static final int ERROR_NOT_CONNECTED = 1003;
    public static final int ERROR_BLUETOOTH_DISABLED = 1004;
    public static final int ERROR_PERIPHERAL_NOT_SUPPORTED = 1005;
    public static final int ERROR_ADVERTISING_FAILED = 1006;
    public static final int ERROR_INVALID_PARAMETER = 1007;

    private Activity unityActivity;
    private BleHidManager bleHidManager;
    private BleHidUnityCallback callback;
    private boolean isInitialized = false;
    private LocalInputManager localInputManager;

    // called from Unity
    public static synchronized BleHidUnityBridge getInstance() {
        if (instance == null) instance = new BleHidUnityBridge();
        return instance;
    }

    private BleHidUnityBridge() {
        // Constructor is now empty as initialization happens in initialize()
    }

    // called from Unity
    public boolean initialize(final String gameObjectName) {
        try {
            Log.d(TAG, "Initializing BLE HID with callback to Unity GameObject: " + gameObjectName);
            this.unityActivity = UnityPlayer.currentActivity;
            this.callback = new BleHidUnityCallback(gameObjectName);

            boolean serviceStarted = startForegroundService();

            if (serviceStarted) Log.d(TAG, "Foreground service start requested successfully");
            else Log.e(TAG, "Failed to start foreground service");

            bleHidManager = new BleHidManager(unityActivity, callback);

            setupBlePairingManager();
            setupConnectionParameterListener();

            isInitialized = bleHidManager.initialize();
        } catch (Exception e) {
            //
        } finally {
            if (!isInitialized) {
                String error = "BLE HID initialization failed";
                Log.e(TAG, error);
                callback.onError(ERROR_INITIALIZATION_FAILED, error);
            }
        }

        String msg = "BLE HID initialized: " + isInitialized;
        Log.d(TAG, msg);
        callback.onInitializeComplete(isInitialized, msg);
        return isInitialized;
    }

    private void setupBlePairingManager() {
        BlePairingManager blePairingManager = bleHidManager.getBlePairingManager();
        BlePairingManager.PairingCallback pairingCallback = new BlePairingManager.PairingCallback() {
            @Override
            public void onPairingRequested(BluetoothDevice device, int variant) {
                final String deviceInfo = getDeviceInfo(device);
                final String message = "Pairing requested by " + deviceInfo + ", variant: " + variant;
                Log.d(TAG, message);

                callback.onDebugLog(message);
                callback.onPairingStateChanged("REQUESTED", device.getAddress());

                // todo not working?
                blePairingManager.setPairingConfirmation(device, true);
            }

            @Override
            public void onPairingComplete(BluetoothDevice device, boolean success) {
                final String deviceInfo = getDeviceInfo(device);
                final String result = success ? "SUCCESS" : "FAILED";
                final String message = "Pairing " + result + " with " + deviceInfo;
                Log.d(TAG, message);

                callback.onDebugLog(message);
                callback.onPairingStateChanged(result, device.getAddress());
                updateConnectionStatus();
            }
        };
        blePairingManager.setPairingCallback(pairingCallback);
    }

    private void setupConnectionParameterListener() {
        BleConnectionManager connectionManager = bleHidManager.getConnectionManager();
        BleConnectionManager.ConnectionParameterListener listener = new BleConnectionManager.ConnectionParameterListener() {
            @Override
            public void onConnectionParametersChanged(int interval, int latency, int timeout, int mtu) {
                Log.d(TAG, "Connection parameters changed: interval=" + interval + "ms, latency=" + latency + ", timeout=" + timeout + "ms, MTU=" + mtu);
                callback.onConnectionParametersChanged(interval, latency, timeout, mtu);
            }

            @Override
            public void onRssiRead(int rssi) {
                // Log.d(TAG, "RSSI: " + rssi + " dBm");
                callback.onRssiRead(rssi);
            }

            @Override
            public void onRequestComplete(String parameterName, boolean success, String actualValue) {
                Log.d(TAG, "Parameter request complete: " + parameterName + " success=" + success + " actual=" + actualValue);
                callback.onConnectionParameterRequestComplete(parameterName, success, actualValue);
            }
        };
        connectionManager.setConnectionParameterListener(listener);
    }

    public void notifyPipModeChanged(boolean isInPipMode) {
        if (callback != null) callback.onPipModeChanged(isInPipMode);
    }

    public boolean pressMouseButton(int button) {
        if (!checkConnected()) return false;

        // Convert button index to button flag
        int buttonFlag = 0;
        switch (button) {
            case 0:
                buttonFlag = HidConstants.Mouse.BUTTON_LEFT;
                break;
            case 1:
                buttonFlag = HidConstants.Mouse.BUTTON_RIGHT;
                break;
            case 2:
                buttonFlag = HidConstants.Mouse.BUTTON_MIDDLE;
                break;
            default:
                Log.e(TAG, "Invalid button index: " + button);
                return false;
        }

        return bleHidManager.pressMouseButton(buttonFlag);
    }

    public boolean releaseMouseButton(int button) {
        if (!checkConnected()) return false;

        // Convert button index to button flag
        int buttonFlag = 0;
        switch (button) {
            case 0:
                buttonFlag = HidConstants.Mouse.BUTTON_LEFT;
                break;
            case 1:
                buttonFlag = HidConstants.Mouse.BUTTON_RIGHT;
                break;
            case 2:
                buttonFlag = HidConstants.Mouse.BUTTON_MIDDLE;
                break;
            default:
                Log.e(TAG, "Invalid button index: " + button);
                return false;
        }

        return bleHidManager.releaseMouseButton(buttonFlag);
    }

    public boolean startAdvertising() {
        if (!checkInitialized()) return false;

        boolean result = bleHidManager.startAdvertising();
        if (result) {
            Log.d(TAG, "Requested advertising");
            callback.onDebugLog("Requested advertising");
        } else {
            String error = "Failed to request advertising";
            Log.e(TAG, error);
            callback.onError(ERROR_ADVERTISING_FAILED, error);
            callback.onAdvertisingStateChanged(false, error);
            callback.onDebugLog(error);
        }

        return result;
    }

    public void stopAdvertising() {
        if (!checkInitialized()) return;

        bleHidManager.stopAdvertising();
        Log.d(TAG, "Advertising stopped");
        callback.onAdvertisingStateChanged(false, "Advertising stopped");
        callback.onDebugLog("Advertising stopped");
    }

    public boolean isAdvertising() {
        if (!checkInitialized()) return false;
        return bleHidManager.isAdvertising();
    }

    public boolean isConnected() {
        if (!checkInitialized()) return false;
        return bleHidManager.isConnected();
    }

    @SuppressLint("MissingPermission")
    public boolean disconnect() {
        BluetoothDevice device = bleHidManager.getConnectedDevice();
        Log.i(TAG, "Starting disconnect process for device: " + device.getName() + " (" + device.getAddress() + ")");

        BleGattServerManager gattManager = bleHidManager.getGattServerManager();
        BluetoothGatt gatt = gattManager.getGattForConnectedDevice();
        if (gatt != null) {
            gatt.disconnect();
            gatt.close();
        } else Log.i(TAG, "No GATT found during disconnect");

        bleHidManager.clearConnectedDevice();
        callback.onConnectionStateChanged(false, null, null);

        Log.i(TAG, "Disconnect process completed successfully");
        callback.onDebugLog("Disconnect process completed successfully");

        return true;
    }

    @SuppressLint("MissingPermission")
    public String[] getConnectedDeviceInfo() {
        if (!checkInitialized() || !bleHidManager.isConnected()) {
            return null;
        }

        BluetoothDevice device = bleHidManager.getConnectedDevice();
        if (device == null) return null;

        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Unknown Device";
        }

        return new String[]{deviceName, device.getAddress()};
    }

    public boolean sendKey(int keyCode) {
        return sendKey((byte) keyCode, (byte) 0);
    }

    public boolean sendKeyWithModifiers(int keyCode, int modifiers) {
        return sendKey((byte) keyCode, (byte) modifiers);
    }

    public boolean sendKey(byte keyCode, byte modifiers) {
        if (!checkConnected()) return false;

        boolean result = bleHidManager.sendKey(keyCode, modifiers);
        if (result) {
            // Release key after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    bleHidManager.releaseAllKeys();
                } catch (InterruptedException e) {
                    // Ignore
                }
            }).start();
        }

        return result;
    }

    public boolean typeText(String text) {
        if (!checkConnected()) return false;

        return bleHidManager.typeText(text);
    }

    public boolean moveMouse(int deltaX, int deltaY) {
        if (!checkConnected()) return false;

        // Clamp values to valid range
        deltaX = Math.max(-127, Math.min(127, deltaX));
        deltaY = Math.max(-127, Math.min(127, deltaY));

        return bleHidManager.moveMouse(deltaX, deltaY);
    }

    public boolean clickMouseButton(int button) {
        if (!checkConnected()) return false;

        // Convert button index to button flag
        int buttonFlag = 0;
        switch (button) {
            case 0:
                buttonFlag = HidConstants.Mouse.BUTTON_LEFT;
                break;
            case 1:
                buttonFlag = HidConstants.Mouse.BUTTON_RIGHT;
                break;
            case 2:
                buttonFlag = HidConstants.Mouse.BUTTON_MIDDLE;
                break;
            default:
                Log.e(TAG, "Invalid button index: " + button);
                return false;
        }

        return bleHidManager.clickMouseButton(buttonFlag);
    }

    public boolean playPause() {
        if (!checkConnected()) return false;
        return bleHidManager.playPause();
    }

    public boolean nextTrack() {
        if (!checkConnected()) return false;
        return bleHidManager.nextTrack();
    }

    public boolean previousTrack() {
        if (!checkConnected()) return false;
        return bleHidManager.previousTrack();
    }

    public boolean volumeUp() {
        if (!checkConnected()) return false;
        return bleHidManager.volumeUp();
    }

    public boolean volumeDown() {
        if (!checkConnected()) return false;
        return bleHidManager.volumeDown();
    }

    public boolean mute() {
        if (!checkConnected()) return false;
        return bleHidManager.mute();
    }

    public boolean initializeLocalControl() {
        Activity currentActivity = UnityPlayer.currentActivity;
        if (currentActivity != null) {
            this.unityActivity = currentActivity;
            Log.d(TAG, "Unity activity reference updated");
        } else {
            Log.e(TAG, "Attempted to update with null activity");
        }

        try {
            localInputManager = LocalInputManager.initialize(unityActivity);
            Log.d(TAG, "Local input manager initialized");

            boolean serviceEnabled = localInputManager.isAccessibilityServiceEnabled();
            if (!serviceEnabled) {
                Log.w(TAG, "Accessibility service not enabled");
                callback.onDebugLog("Accessibility service not enabled. Please enable it in Settings.");
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize local control", e);
            callback.onError(ERROR_INITIALIZATION_FAILED, "Failed to initialize local control: " + e.getMessage());
            return false;
        }
    }

    public boolean isAccessibilityServiceEnabled() {
        return localInputManager.isAccessibilityServiceEnabled();
    }

    public void openAccessibilitySettings() {
        localInputManager.openAccessibilitySettings();
    }

    public boolean localPlayPause() {
        return localInputManager.playPause();
    }

    public boolean localNextTrack() {
        return localInputManager.nextTrack();
    }

    public boolean localPreviousTrack() {
        return localInputManager.previousTrack();
    }

    public boolean localVolumeUp() {
        return localInputManager.volumeUp();
    }

    public boolean localVolumeDown() {
        return localInputManager.volumeDown();
    }

    public boolean localMute() {
        return localInputManager.mute();
    }

    public boolean localTap(int x, int y) {
        return localInputManager.tap(x, y);
    }

    public boolean localSwipeBegin(float startX, float startY) {
        return localInputManager.swipeBegin(startX, startY);
    }

    public boolean localSwipeExtend(float deltaX, float deltaY) {
        return localInputManager.swipeExtend(deltaX, deltaY);
    }

    public boolean localSwipeEnd() {
        return localInputManager.swipeEnd();
    }

    public boolean performGlobalAction(int globalAction) {
        return localInputManager.performGlobalAction(globalAction);
    }

    public boolean localPerformFocusedNodeAction(int action) {
        return localInputManager.performFocusedNodeAction(action);
    }

    public boolean localClickFocusedNode() {
        return localInputManager.clickFocusedNode();
    }

    public boolean launchCameraApp() {
        return localInputManager.launchCameraApp();
    }

    public boolean launchPhotoCapture() {
        return localInputManager.launchPhotoCapture();
    }

    public boolean launchVideoCapture() {
        return localInputManager.launchVideoCapture();
    }

    public boolean takePicture(android.os.Bundle optionsBundle) {
        com.inventonater.blehid.core.CameraOptions coreOptions = null;
        if (optionsBundle != null) {
            coreOptions = new com.inventonater.blehid.core.CameraOptions(optionsBundle);
        }

        return localInputManager.takePictureWithCamera(coreOptions);
    }

    public boolean recordVideo(android.os.Bundle optionsBundle) {
        com.inventonater.blehid.core.VideoOptions coreOptions = null;
        if (optionsBundle != null) {
            coreOptions = new com.inventonater.blehid.core.VideoOptions(optionsBundle);
        }

        return localInputManager.recordVideo(coreOptions);
    }

    public boolean requestConnectionPriority(int priority) {
        if (!checkConnected()) return false;

        if (priority < 0 || priority > 2) {
            Log.e(TAG, "Invalid connection priority: " + priority);
            callback.onError(ERROR_INVALID_PARAMETER, "Invalid connection priority: " + priority);
            return false;
        }

        return bleHidManager.getConnectionManager().requestConnectionPriority(priority);
    }

    public boolean requestMtu(int mtu) {
        if (!checkConnected()) return false;

        if (mtu < 23 || mtu > 517) {
            Log.e(TAG, "Invalid MTU size: " + mtu);
            callback.onError(ERROR_INVALID_PARAMETER, "Invalid MTU size: " + mtu);
            return false;
        }

        return bleHidManager.getConnectionManager().requestMtu(mtu);
    }

    public boolean setTransmitPowerLevel(int level) {
        if (!checkInitialized()) return false;

        if (level < 0 || level > 2) {
            Log.e(TAG, "Invalid TX power level: " + level);
            callback.onError(ERROR_INVALID_PARAMETER, "Invalid TX power level: " + level);
            return false;
        }

        return bleHidManager.getConnectionManager().setTransmitPowerLevel(level);
    }

    public boolean readRssi() {
        if (!checkConnected()) return false;

        return bleHidManager.getConnectionManager().readRssi();
    }

    public Map<String, String> getConnectionParameters() {
        if (!checkConnected()) return null;

        return bleHidManager.getConnectionManager().getAllConnectionParameters();
    }

    public void close() {
        // Stop the foreground service when closing the plugin
        Log.d(TAG, "Stopping foreground service on plugin close");
        boolean serviceStopped = stopForegroundService();
        if (serviceStopped) {
            Log.d(TAG, "Foreground service stop requested successfully");
        } else {
            Log.e(TAG, "Failed to stop foreground service");
        }

        // Close the BleHidManager
        if (bleHidManager != null) {
            bleHidManager.close();
        }

        isInitialized = false;
        Log.d(TAG, "Plugin closed");
    }

    public boolean setBleIdentity(String identityUuid, String deviceName) {
        if (!checkInitialized()) return false;
        Log.i(TAG, "Setting BLE identity: UUID=" + identityUuid + ", Name=" + deviceName);
        return bleHidManager.setBleIdentity(identityUuid, deviceName);
    }

    public List<Map<String, String>> getBondedDevicesInfo() {
        if (!checkInitialized()) return new ArrayList<>();
        return bleHidManager.getBluetoothControl().getBondedDevicesInfo();
    }

    public boolean isDeviceBonded(String address) {
        if (!checkInitialized()) return false;

        if (address == null || address.isEmpty()) {
            Log.e(TAG, "Invalid device address");
            return false;
        }

        return bleHidManager.isDeviceBonded(address);
    }

    public boolean removeBond(String address) {
        if (!checkInitialized()) return false;

        if (address == null || address.isEmpty()) {
            Log.e(TAG, "Invalid device address");
            return false;
        }

        return bleHidManager.removeBond(address);
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

    @SuppressLint("MissingPermission")
    private void updateConnectionStatus() {
        if (callback == null || !isInitialized) return;

        if (bleHidManager.isConnected()) {
            BluetoothDevice device = bleHidManager.getConnectedDevice();
            if (device != null) {
                String deviceName = device.getName();
                if (deviceName == null || deviceName.isEmpty()) {
                    deviceName = "Unknown Device";
                }
                callback.onConnectionStateChanged(true, deviceName, device.getAddress());
            }
        } else {
            callback.onConnectionStateChanged(false, null, null);
        }
    }

    @SuppressLint("MissingPermission")
    private String getDeviceInfo(BluetoothDevice device) {
        if (device == null) return "null";

        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Unknown Device";
        }

        return deviceName + " (" + device.getAddress() + ")";
    }

    private String bondStateToString(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                return "BOND_NONE";
            case BluetoothDevice.BOND_BONDING:
                return "BOND_BONDING";
            case BluetoothDevice.BOND_BONDED:
                return "BOND_BONDED";
            default:
                return "UNKNOWN(" + bondState + ")";
        }
    }

    private boolean checkInitialized() {
        if (!isInitialized) {
            Log.e(TAG, "Plugin not initialized");
            callback.onError(ERROR_NOT_INITIALIZED, "Plugin not initialized");
            return false;
        }
        return true;
    }

    private boolean checkConnected() {
        if (!checkInitialized()) return false;

        if (!bleHidManager.isConnected()) {
            Log.e(TAG, "No device connected");
            callback.onError(ERROR_NOT_CONNECTED, "No device connected");
            return false;
        }
        return true;
    }

    public BleHidManager getBleHidManager() {
        return bleHidManager;
    }

    private static final String UNITY_PLAYER_ACTIVITY_CLASS = "com.unity3d.player.UnityPlayerGameActivity";

    private static Context getCurrentContext() {
        Log.e(TAG, "Attempting to get current context for service");

        // Try approach #1: Direct access through Unity's UnityPlayer class
        try {
            Class<?> unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayer");
            java.lang.reflect.Field currentActivityField = unityPlayerClass.getDeclaredField("currentActivity");
            currentActivityField.setAccessible(true);
            Context context = (Context) currentActivityField.get(null);
            if (context != null) {
                Log.e(TAG, "Got context through UnityPlayer.currentActivity: " + context.getClass().getName());
                return context;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get context through UnityPlayer: " + e.getMessage());
        }

        // Try approach #2: Try to load the specific activity class
        try {
            Class<?> activityClass = Class.forName(UNITY_PLAYER_ACTIVITY_CLASS);
            Log.e(TAG, "Found activity class: " + activityClass.getName());

            // The class exists, but we can't get an instance directly
            // Let's try to get the application context
            try {
                Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
                java.lang.reflect.Method currentActivityThreadMethod = activityThreadClass.getDeclaredMethod("currentActivityThread");
                currentActivityThreadMethod.setAccessible(true);
                Object activityThread = currentActivityThreadMethod.invoke(null);

                java.lang.reflect.Method getApplicationMethod = activityThreadClass.getDeclaredMethod("getApplication");
                Context context = (Context) getApplicationMethod.invoke(activityThread);
                if (context != null) {
                    Log.e(TAG, "Got context from ActivityThread: " + context.getPackageName());
                    return context;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting context from ActivityThread: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to load activity class: " + e.getMessage());
        }

        // Final fallback
        try {
            // This is deprecated but still works in many cases
            return com.unity3d.player.UnityPlayer.currentActivity;
        } catch (Exception e) {
            Log.e(TAG, "All context retrieval attempts failed");
            return null;
        }
    }

    public static boolean startForegroundService() {
        Log.e(TAG, "startForegroundService called from Unity - CRITICAL SERVICE STARTUP LOG");

        try {
            // Get the current context - directly attempt to use the known activity class
            Context context = getCurrentContext();
            if (context == null) {
                Log.e(TAG, "Unable to start foreground service: context is null");
                return false;
            }

            Log.e(TAG, "Application context package: " + context.getPackageName());
            Log.e(TAG, "Current context class: " + context.getClass().getName());

            // Create intent with explicit component name to avoid package confusion
            Intent serviceIntent = new Intent();

            // Set component with explicit package
            serviceIntent.setClassName(
                    context.getPackageName(),
                    "com.inventonater.blehid.core.BleHidForegroundService"
            );
            serviceIntent.setAction("START_FOREGROUND");

            // For debugging
            Log.e(TAG, "Starting service with: " + serviceIntent);
            Log.e(TAG, "Component: " + serviceIntent.getComponent());

            // Try to start the service with the most direct method using explicit intent
            try {
                context.startForegroundService(serviceIntent);
                Log.e(TAG, "startForegroundService called successfully");
            } catch (Exception e) {
                Log.e(TAG, "startForegroundService failed: " + e.getMessage(), e);
                // Last resort - try direct startService
                Log.e(TAG, "Trying regular startService as fallback");
                context.startService(serviceIntent);
                Log.e(TAG, "Fallback startService called successfully");
            }

            // Verify service running
            boolean isServiceRunning = false;
            try {
                Class<?> serviceClass = Class.forName("com.inventonater.blehid.core.BleHidForegroundService");
                java.lang.reflect.Method isRunningMethod = serviceClass.getMethod("isRunning");
                isServiceRunning = (boolean) isRunningMethod.invoke(null);
                Log.e(TAG, "Service running check: " + isServiceRunning);
            } catch (Exception e) {
                Log.e(TAG, "Failed to check if service is running", e);
            }

            Log.e(TAG, "Foreground service start request process completed");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Critical error starting foreground service", e);
            return false;
        }
    }

    public static boolean stopForegroundService() {
        Log.d(TAG, "stopForegroundService called from Unity");

        try {
            Context context = getCurrentContext();
            if (context == null) {
                Log.e(TAG, "Unable to stop foreground service: context is null");
                return false;
            }

            Intent serviceIntent = new Intent();
            serviceIntent.setClassName(
                    context.getPackageName(),
                    "com.inventonater.blehid.core.BleHidForegroundService"
            );
            serviceIntent.setAction("STOP_FOREGROUND");

            Log.d(TAG, "Calling startService with stop action: " + serviceIntent);

            context.startService(serviceIntent);
            Log.d(TAG, "Foreground service stop requested successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error stopping foreground service", e);
            return false;
        }
    }
}
