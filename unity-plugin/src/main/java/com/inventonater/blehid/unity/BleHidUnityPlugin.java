package com.inventonater.blehid.unity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.inventonater.blehid.core.BleConnectionManager;
import com.inventonater.blehid.core.BleHidManager;
import com.inventonater.blehid.core.BlePairingManager;
import com.inventonater.blehid.core.CameraOptions;
import com.inventonater.blehid.core.HidConstants;
import com.inventonater.blehid.core.LocalInputManager;
import com.inventonater.blehid.core.OptionsConstants;
import com.inventonater.blehid.core.VideoOptions;

import java.util.Map;

/**
 * Main Unity plugin class for BLE HID functionality.
 * This class serves as the primary interface between Unity and the BLE HID core.
 */
public class BleHidUnityPlugin {
    private static final String TAG = "BleHidUnityPlugin";
    private static final boolean VERBOSE_LOGGING = true;
    
    // Error codes
    public static final int ERROR_INITIALIZATION_FAILED = 1001;
    public static final int ERROR_NOT_INITIALIZED = 1002;
    public static final int ERROR_NOT_CONNECTED = 1003;
    public static final int ERROR_BLUETOOTH_DISABLED = 1004;
    public static final int ERROR_PERIPHERAL_NOT_SUPPORTED = 1005;
    public static final int ERROR_ADVERTISING_FAILED = 1006;
    public static final int ERROR_INVALID_PARAMETER = 1007;
    public static final int ERROR_ACCESSIBILITY_NOT_ENABLED = 1008;
    
    private static BleHidUnityPlugin instance;
    private Activity unityActivity;
    private BleHidManager bleHidManager;
    private BleHidUnityCallback callback;
    private boolean isInitialized = false;
    private LocalInputManager localInputManager;
    
    /**
     * Get the singleton instance of the plugin.
     */
    public static synchronized BleHidUnityPlugin getInstance() {
        if (instance == null) {
            instance = new BleHidUnityPlugin();
        }
        return instance;
    }
    
    private BleHidUnityPlugin() {
        // Private constructor to prevent direct instantiation
    }
    
    /**
     * Initialize the plugin with the Unity activity context.
     * 
     * @param activity The Unity activity
     * @param callback Callback interface for Unity events
     * @return true if initialization succeeded, false otherwise
     */
    public boolean initialize(Activity activity, BleHidUnityCallback callback) {
        if (activity == null) {
            Log.e(TAG, "Activity cannot be null");
            return false;
        }
        
        this.unityActivity = activity;
        this.callback = callback;
        
        // Create BLE HID manager
        bleHidManager = new BleHidManager(activity);
        
        // Check if BLE peripheral mode is supported
        if (!bleHidManager.isBlePeripheralSupported()) {
            String error = "BLE peripheral mode is not supported on this device";
            Log.e(TAG, error);
            if (callback != null) {
                callback.onError(ERROR_PERIPHERAL_NOT_SUPPORTED, error);
                callback.onInitializeComplete(false, error);
            }
            return false;
        }
        
        // Set up pairing callback
        bleHidManager.getBlePairingManager().setPairingCallback(new BlePairingManager.PairingCallback() {
            @Override
            public void onPairingRequested(BluetoothDevice device, int variant) {
                final String deviceInfo = getDeviceInfo(device);
                final String message = "Pairing requested by " + deviceInfo + ", variant: " + variant;
                Log.d(TAG, message);
                
                if (callback != null) {
                    callback.onDebugLog(message);
                    callback.onPairingStateChanged("REQUESTED", device.getAddress());
                }
                
                // Auto-accept pairing requests
                bleHidManager.getBlePairingManager().setPairingConfirmation(device, true);
            }
            
            @Override
            public void onPairingComplete(BluetoothDevice device, boolean success) {
                final String deviceInfo = getDeviceInfo(device);
                final String result = success ? "SUCCESS" : "FAILED";
                final String message = "Pairing " + result + " with " + deviceInfo;
                Log.d(TAG, message);
                
                if (callback != null) {
                    callback.onDebugLog(message);
                    callback.onPairingStateChanged(result, device.getAddress());
                    updateConnectionStatus();
                }
            }
        });
        
        // Initialize the BLE HID functionality
        boolean initialized = bleHidManager.initialize();
        isInitialized = initialized;
        
        if (initialized) {
            Log.d(TAG, "BLE HID initialized successfully");
            
            // Set up connection parameter listener
            if (bleHidManager.getConnectionManager() != null) {
                bleHidManager.getConnectionManager().setConnectionParameterListener(
                        new BleConnectionManager.ConnectionParameterListener() {
                    @Override
                    public void onConnectionParametersChanged(int interval, int latency, int timeout, int mtu) {
                        if (callback != null) {
                            Log.d(TAG, "Connection parameters changed: interval=" + interval + 
                                   "ms, latency=" + latency + ", timeout=" + timeout + "ms, MTU=" + mtu);
                            callback.onConnectionParametersChanged(interval, latency, timeout, mtu);
                        }
                    }
                    
                    @Override
                    public void onRssiRead(int rssi) {
                        if (callback != null) {
                            Log.d(TAG, "RSSI: " + rssi + " dBm");
                            callback.onRssiRead(rssi);
                        }
                    }
                    
                    @Override
                    public void onRequestComplete(String parameterName, boolean success, String actualValue) {
                        if (callback != null) {
                            Log.d(TAG, "Parameter request complete: " + parameterName + 
                                   " success=" + success + " actual=" + actualValue);
                            callback.onConnectionParameterRequestComplete(parameterName, success, actualValue);
                        }
                    }
                });
            }
            
            if (callback != null) {
                callback.onInitializeComplete(true, "BLE HID initialized successfully");
            }
        } else {
            String error = "BLE HID initialization failed";
            Log.e(TAG, error);
            if (callback != null) {
                callback.onError(ERROR_INITIALIZATION_FAILED, error);
                callback.onInitializeComplete(false, error);
            }
        }
        
        return initialized;
    }
    
    /**
     * Check if the plugin has been initialized successfully.
     */
    public boolean isInitialized() {
        return isInitialized && bleHidManager != null;
    }
    
    /**
     * Start BLE advertising.
     * 
     * @return true if advertising started successfully, false otherwise
     */
    public boolean startAdvertising() {
        if (!checkInitialized()) return false;
        
        boolean result = bleHidManager.startAdvertising();
        if (result) {
            Log.d(TAG, "Advertising started");
            if (callback != null) {
                callback.onAdvertisingStateChanged(true, "Advertising started");
                callback.onDebugLog("Advertising started");
            }
        } else {
            String error = "Failed to start advertising";
            Log.e(TAG, error);
            if (callback != null) {
                callback.onError(ERROR_ADVERTISING_FAILED, error);
                callback.onAdvertisingStateChanged(false, error);
                callback.onDebugLog(error);
            }
        }
        
        return result;
    }
    
    /**
     * Stop BLE advertising.
     */
    public void stopAdvertising() {
        if (!checkInitialized()) return;
        
        bleHidManager.stopAdvertising();
        Log.d(TAG, "Advertising stopped");
        if (callback != null) {
            callback.onAdvertisingStateChanged(false, "Advertising stopped");
            callback.onDebugLog("Advertising stopped");
        }
    }
    
    /**
     * Check if BLE advertising is active.
     */
    public boolean isAdvertising() {
        if (!checkInitialized()) return false;
        return bleHidManager.isAdvertising();
    }
    
    /**
     * Check if a device is connected.
     */
    public boolean isConnected() {
        if (!checkInitialized()) return false;
        return bleHidManager.isConnected();
    }
    
    /**
     * Get information about the connected device.
     * 
     * @return A string array with [deviceName, deviceAddress] or null if not connected
     */
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
        
        return new String[] { deviceName, device.getAddress() };
    }
    
    /**
     * Send a keyboard key with optional modifier keys.
     * 
     * @param keyCode The HID key code
     * @param modifiers Modifier keys (bit flags), use 0 for no modifiers
     * @return true if the key was sent successfully, false otherwise
     */
    public boolean sendKey(byte keyCode, byte modifiers) {
        if (!checkConnected()) return false;
        
        boolean result = bleHidManager.sendKey(keyCode, modifiers);
        if (result) {
            // Release key after a short delay
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        bleHidManager.releaseAllKeys();
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }).start();
        }
        
        return result;
    }
    
    /**
     * Type a string of text.
     * 
     * @param text The text to type
     * @return true if the text was sent successfully, false otherwise
     */
    public boolean typeText(String text) {
        if (!checkConnected()) return false;
        
        return bleHidManager.typeText(text);
    }
    
    /**
     * Send a mouse movement.
     * 
     * @param deltaX X-axis movement (-127 to 127)
     * @param deltaY Y-axis movement (-127 to 127)
     * @return true if the movement was sent successfully, false otherwise
     */
    public boolean moveMouse(int deltaX, int deltaY) {
        if (!checkConnected()) return false;
        
        // Clamp values to valid range
        deltaX = Math.max(-127, Math.min(127, deltaX));
        deltaY = Math.max(-127, Math.min(127, deltaY));
        
        return bleHidManager.moveMouse(deltaX, deltaY);
    }
    
    /**
     * Send a mouse button click.
     * 
     * @param button The button to click (0=left, 1=right, 2=middle)
     * @return true if the click was sent successfully, false otherwise
     */
    public boolean clickMouseButton(int button) {
        if (!checkConnected()) return false;
        
        return bleHidManager.clickMouseButton(button);
    }
    
    /**
     * Send a media play/pause command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean playPause() {
        if (!checkConnected()) return false;
        return bleHidManager.playPause();
    }
    
    /**
     * Send a media next track command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean nextTrack() {
        if (!checkConnected()) return false;
        return bleHidManager.nextTrack();
    }
    
    /**
     * Send a media previous track command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean previousTrack() {
        if (!checkConnected()) return false;
        return bleHidManager.previousTrack();
    }
    
    /**
     * Send a media volume up command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeUp() {
        if (!checkConnected()) return false;
        return bleHidManager.volumeUp();
    }
    
    /**
     * Send a media volume down command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeDown() {
        if (!checkConnected()) return false;
        return bleHidManager.volumeDown();
    }
    
    /**
     * Send a media mute command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean mute() {
        if (!checkConnected()) return false;
        return bleHidManager.mute();
    }
    
    /**
     * Update the Unity Activity reference.
     * This allows refreshing the activity reference when needed.
     * 
     * @param activity The updated Unity activity
     */
    public void updateUnityActivity(Activity activity) {
        if (activity != null) {
            this.unityActivity = activity;
            Log.d(TAG, "Unity activity reference updated");
        } else {
            Log.e(TAG, "Attempted to update with null activity");
        }
    }
    
    /**
     * Initialize local input control.
     */
    public boolean initializeLocalControl() {
        // Check if we have a valid activity reference
        if (unityActivity == null) {
            Log.e(TAG, "Activity not available");
            return false;
        }
        
        try {
            // Initialize the LocalInputManager with the current activity
            localInputManager = LocalInputManager.initialize(unityActivity);
            Log.d(TAG, "Local input manager initialized");
            
            // Check accessibility service
            boolean serviceEnabled = localInputManager.isAccessibilityServiceEnabled();
            if (!serviceEnabled) {
                Log.w(TAG, "Accessibility service not enabled");
                if (callback != null) {
                    callback.onDebugLog("Accessibility service not enabled. Please enable it in Settings.");
                }
            }
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize local control", e);
            if (callback != null) {
                callback.onError(ERROR_INITIALIZATION_FAILED, "Failed to initialize local control: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Check if accessibility service is enabled.
     */
    public boolean isAccessibilityServiceEnabled() {
        if (localInputManager == null) {
            Log.e(TAG, "Local input manager not initialized");
            return false;
        }
        return localInputManager.isAccessibilityServiceEnabled();
    }

    /**
     * Open accessibility settings.
     */
    public void openAccessibilitySettings() {
        if (localInputManager != null) {
            localInputManager.openAccessibilitySettings();
        }
    }

    // Media control methods

    public boolean localPlayPause() {
        if (localInputManager == null) return false;
        return localInputManager.playPause();
    }

    public boolean localNextTrack() {
        if (localInputManager == null) return false;
        return localInputManager.nextTrack();
    }

    public boolean localPreviousTrack() {
        if (localInputManager == null) return false;
        return localInputManager.previousTrack();
    }

    public boolean localVolumeUp() {
        if (localInputManager == null) return false;
        return localInputManager.volumeUp();
    }

    public boolean localVolumeDown() {
        if (localInputManager == null) return false;
        return localInputManager.volumeDown();
    }

    public boolean localMute() {
        if (localInputManager == null) return false;
        return localInputManager.mute();
    }

    // Input control methods

    public boolean localTap(int x, int y) {
        if (localInputManager == null) return false;
        return localInputManager.tap(x, y);
    }

    public boolean localSwipe(int x1, int y1, int x2, int y2) {
        if (localInputManager == null) return false;
        return localInputManager.swipe(x1, y1, x2, y2);
    }

    public boolean localNavigate(int direction) {
        if (localInputManager == null) return false;
        return localInputManager.navigate(direction);
    }
    
    // Camera control methods
    
    public boolean launchCameraApp() {
        if (localInputManager == null) return false;
        return localInputManager.launchCameraApp();
    }
    
    public boolean launchPhotoCapture() {
        if (localInputManager == null) return false;
        return localInputManager.launchPhotoCapture();
    }
    
    public boolean launchVideoCapture() {
        if (localInputManager == null) return false;
        return localInputManager.launchVideoCapture();
    }
    
    /**
     * Take a picture with the camera using the specified options bundle.
     * 
     * @param optionsBundle Bundle with camera options parameters
     * @return true if camera was launched successfully
     */
    public boolean takePicture(android.os.Bundle optionsBundle) {
        if (localInputManager == null) return false;
        
        com.inventonater.blehid.core.CameraOptions coreOptions = null;
        if (optionsBundle != null) {
            coreOptions = new com.inventonater.blehid.core.CameraOptions(optionsBundle);
        }
        
        return localInputManager.takePictureWithCamera(coreOptions);
    }
    
    /**
     * Take a picture with the camera using default options.
     * This is a convenience method that uses the default options.
     * 
     * @return true if camera was launched successfully
     */
    public boolean takePicture() {
        return takePicture((android.os.Bundle)null);
    }
    
    /**
     * Record a video with the specified options bundle.
     * 
     * @param optionsBundle Bundle with video options parameters
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo(android.os.Bundle optionsBundle) {
        if (localInputManager == null) return false;
        
        com.inventonater.blehid.core.VideoOptions coreOptions = null;
        if (optionsBundle != null) {
            coreOptions = new com.inventonater.blehid.core.VideoOptions(optionsBundle);
        }
        
        return localInputManager.recordVideo(coreOptions);
    }
    
    /**
     * Record a video with default options and specified duration.
     * This is a convenience method for simple recording.
     * 
     * @param durationMs Duration in milliseconds to record
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo(long durationMs) {
        com.inventonater.blehid.core.VideoOptions options = new com.inventonater.blehid.core.VideoOptions();
        options.setDuration(durationMs / 1000f);
        return localInputManager.recordVideo(options);
    }

    // Navigation constants
    public static final int NAV_UP = LocalInputManager.NAV_UP;
    public static final int NAV_DOWN = LocalInputManager.NAV_DOWN;
    public static final int NAV_LEFT = LocalInputManager.NAV_LEFT;
    public static final int NAV_RIGHT = LocalInputManager.NAV_RIGHT;
    public static final int NAV_BACK = LocalInputManager.NAV_BACK;
    public static final int NAV_HOME = LocalInputManager.NAV_HOME;
    public static final int NAV_RECENTS = LocalInputManager.NAV_RECENTS;
    
    // ==================== Connection Parameter Methods ====================
    
    /**
     * Request a change in connection priority.
     * 
     * @param priority The connection priority: 0=HIGH, 1=BALANCED, 2=LOW_POWER
     * @return true if the request was sent, false otherwise
     */
    public boolean requestConnectionPriority(int priority) {
        if (!checkConnected()) return false;
        
        if (priority < 0 || priority > 2) {
            Log.e(TAG, "Invalid connection priority: " + priority);
            if (callback != null) {
                callback.onError(ERROR_INVALID_PARAMETER, "Invalid connection priority: " + priority);
            }
            return false;
        }
        
        return bleHidManager.getConnectionManager().requestConnectionPriority(priority);
    }
    
    /**
     * Request a change in MTU size.
     * 
     * @param mtu The MTU size (23-517)
     * @return true if the request was sent, false otherwise
     */
    public boolean requestMtu(int mtu) {
        if (!checkConnected()) return false;
        
        if (mtu < 23 || mtu > 517) {
            Log.e(TAG, "Invalid MTU size: " + mtu);
            if (callback != null) {
                callback.onError(ERROR_INVALID_PARAMETER, "Invalid MTU size: " + mtu);
            }
            return false;
        }
        
        return bleHidManager.getConnectionManager().requestMtu(mtu);
    }
    
    /**
     * Set the transmit power level for advertising.
     * 
     * @param level The power level: 0=LOW, 1=MEDIUM, 2=HIGH
     * @return true if successful, false otherwise
     */
    public boolean setTransmitPowerLevel(int level) {
        if (!checkInitialized()) return false;
        
        if (level < 0 || level > 2) {
            Log.e(TAG, "Invalid TX power level: " + level);
            if (callback != null) {
                callback.onError(ERROR_INVALID_PARAMETER, "Invalid TX power level: " + level);
            }
            return false;
        }
        
        return bleHidManager.getConnectionManager().setTransmitPowerLevel(level);
    }
    
    /**
     * Read the current RSSI value.
     * 
     * @return true if the read request was sent, false otherwise
     */
    public boolean readRssi() {
        if (!checkConnected()) return false;
        
        return bleHidManager.getConnectionManager().readRssi();
    }
    
    /**
     * Get all connection parameters as a string map.
     * 
     * @return Map of parameter names to values, or null if not connected
     */
    public Map<String, String> getConnectionParameters() {
        if (!checkConnected()) return null;
        
        return bleHidManager.getConnectionManager().getAllConnectionParameters();
    }
    
    /**
     * Get diagnostic information about the BLE HID state.
     * 
     * @return A string with diagnostic information
     */
    public String getDiagnosticInfo() {
        if (!checkInitialized()) return "Not initialized";
        
        StringBuilder info = new StringBuilder();
        info.append("Initialized: ").append(isInitialized()).append("\n");
        info.append("Advertising: ").append(bleHidManager.isAdvertising()).append("\n");
        info.append("Connected: ").append(bleHidManager.isConnected()).append("\n");
        
        if (bleHidManager.isConnected()) {
            BluetoothDevice device = bleHidManager.getConnectedDevice();
            if (device != null) {
                info.append("Device: ").append(getDeviceInfo(device)).append("\n");
                info.append("Bond State: ").append(bondStateToString(device.getBondState())).append("\n");
                
                // Add connection parameters
                if (bleHidManager.getConnectionManager() != null) {
                    Map<String, String> params = bleHidManager.getConnectionManager().getAllConnectionParameters();
                    if (params != null) {
                        info.append("\nCONNECTION PARAMETERS:\n");
                        for (Map.Entry<String, String> entry : params.entrySet()) {
                            info.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                        }
                    }
                }
            }
        }
        
        return info.toString();
    }
    
    /**
     * Release all resources and close the plugin.
     */
    public void close() {
        if (bleHidManager != null) {
            bleHidManager.close();
        }
        
        isInitialized = false;
        Log.d(TAG, "Plugin closed");
    }
    
    /**
     * Update the connection status and notify Unity.
     */
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
    
    /**
     * Get a formatted string with device information.
     */
    private String getDeviceInfo(BluetoothDevice device) {
        if (device == null) return "null";
        
        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Unknown Device";
        }
        
        return deviceName + " (" + device.getAddress() + ")";
    }
    
    /**
     * Convert a Bluetooth bond state to a readable string.
     */
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
    
    /**
     * Check if the plugin is initialized and log an error if not.
     */
    private boolean checkInitialized() {
        if (!isInitialized || bleHidManager == null) {
            Log.e(TAG, "Plugin not initialized");
            if (callback != null) {
                callback.onError(ERROR_NOT_INITIALIZED, "Plugin not initialized");
            }
            return false;
        }
        return true;
    }
    
    /**
     * Check if a device is connected and log an error if not.
     */
    private boolean checkConnected() {
        if (!checkInitialized()) return false;
        
        if (!bleHidManager.isConnected()) {
            Log.e(TAG, "No device connected");
            if (callback != null) {
                callback.onError(ERROR_NOT_CONNECTED, "No device connected");
            }
            return false;
        }
        return true;
    }
    
    /**
     * The Unity player activity class name used in this project
     * This is the specific activity class used by this Unity project
     */
    private static final String UNITY_PLAYER_ACTIVITY_CLASS = "com.unity3d.player.UnityPlayerGameActivity";
    
    /**
     * Get the current context from the Unity player.
     * This method attempts multiple strategies to get a valid context.
     *
     * @return The current context, or null if not available
     */
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
    
    /**
     * Start the foreground service to keep accessibility service alive.
     * This should be called when your app needs to ensure the service
     * continues to run in the background.
     * 
     * @return true if the service start request was sent
     */
    public static boolean startForegroundService() {
        Log.e(TAG, "========== FOREGROUND SERVICE START ATTEMPT ==========");
        Log.e(TAG, "Thread: " + Thread.currentThread().getName());
        Log.e(TAG, "Android version: " + android.os.Build.VERSION.SDK_INT);
        
        try {
            // Get context with detailed logging
            Context context = getCurrentContext();
            if (context == null) {
                Log.e(TAG, "FATAL: Unable to get any context to start service");
                return false;
            }
            
            Log.e(TAG, "Using context: " + context.getClass().getName());
            Log.e(TAG, "Package: " + context.getPackageName());
            
            // Create explicit intent with clear service target
            Intent serviceIntent = new Intent();
            String targetClass = "com.inventonater.blehid.core.BleHidForegroundService";
            
            serviceIntent.setClassName(context.getPackageName(), targetClass);
            serviceIntent.setAction("START_FOREGROUND");
            
            Log.e(TAG, "Service intent created: " + serviceIntent);
            Log.e(TAG, "Component: " + serviceIntent.getComponent());
            
            // Start service with direct approach, no Handler (which causes thread exceptions)
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    Log.e(TAG, "Using startForegroundService for Android 8+");
                    context.startForegroundService(serviceIntent);
                    Log.e(TAG, "startForegroundService call completed");
                } else {
                    Log.e(TAG, "Using startService for pre-Android 8");
                    context.startService(serviceIntent);
                    Log.e(TAG, "startService call completed");
                }
                
                // Instead of Handler-based verification which causes thread exceptions,
                // just log that the service was requested
                Log.e(TAG, "Service start requested successfully - not using Handler for verification");
                Log.e(TAG, "NOTE: This fixes the 'Can't create handler inside thread' exception");
                return true;
            } catch (Exception e) {
                Log.e(TAG, "CRITICAL ERROR starting service", e);
                Log.e(TAG, "Exception type: " + e.getClass().getName());
                Log.e(TAG, "Exception cause: " + (e.getCause() != null ? e.getCause().toString() : "null"));
                Log.e(TAG, "Exception stack trace: ", e);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "FATAL ERROR in startForegroundService", e);
            Log.e(TAG, "Exception stack trace: ", e);
            return false;
        } finally {
            Log.e(TAG, "========== FOREGROUND SERVICE START ATTEMPT COMPLETE ==========");
        }
    }
    
    /**
     * Stop the foreground service when it's no longer needed.
     * 
     * @return true if the service stop request was sent
     */
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
