package com.inventonater.blehid.unity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;

import com.inventonater.blehid.core.BleHidManager;
import com.inventonater.blehid.core.BlePairingManager;
import com.inventonater.blehid.core.HidConstants;
import com.inventonater.blehid.core.LocalInputManager;

/**
 * Main Unity plugin class for BLE HID functionality.
 * This class serves as the primary interface between Unity and the BLE HID core.
 */
public class BleHidUnityPlugin {
    private static final String TAG = "BleHidUnityPlugin";
    
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
     * Send a keyboard key press and release.
     * 
     * @param keyCode The HID key code
     * @return true if the key was sent successfully, false otherwise
     */
    public boolean sendKey(byte keyCode) {
        if (!checkConnected()) return false;
        
        boolean result = bleHidManager.sendKey(keyCode, 0); // Add 0 for modifier parameter
        if (result) {
            // Release key after a short delay
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        bleHidManager.releaseAllKeys(); // Changed to the correct method name
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }).start();
        }
        
        return result;
    }
    
    /**
     * Send a keyboard key with modifier keys.
     * 
     * @param keyCode The HID key code
     * @param modifiers Modifier keys (bit flags)
     * @return true if the key was sent successfully, false otherwise
     */
    public boolean sendKeyWithModifiers(byte keyCode, byte modifiers) {
        if (!checkConnected()) return false;
        
        boolean result = bleHidManager.sendKey(keyCode, modifiers);
        if (result) {
            // Release key after a short delay
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(100);
                        bleHidManager.releaseAllKeys(); // Changed to the correct method name
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
     * Take a picture with the camera by launching the camera app
     * and using the accessibility service to tap the shutter button.
     */
    public boolean takePictureWithCamera() {
        if (localInputManager == null) return false;
        return localInputManager.takePictureWithCamera();
    }
    
    /**
     * Take a picture with configurable parameters.
     * 
     * @param tapDelayMs Delay before tapping shutter button
     * @param returnDelayMs Delay before returning to app
     * @param buttonX X position of shutter button (0.0-1.0)
     * @param buttonY Y position of shutter button (0.0-1.0)
     * @return true if camera was launched successfully
     */
    public boolean takePictureWithCamera(int tapDelayMs, int returnDelayMs, float buttonX, float buttonY) {
        if (localInputManager == null) return false;
        return localInputManager.takePictureWithCamera(tapDelayMs, returnDelayMs, buttonX, buttonY);
    }
    
    /**
     * Take a picture with fully configurable parameters including dialog handling.
     * 
     * @param tapDelayMs Delay before tapping shutter button
     * @param returnDelayMs Delay before returning to app
     * @param buttonX X position of shutter button (0.0-1.0)
     * @param buttonY Y position of shutter button (0.0-1.0)
     * @param acceptDialogDelayMs Delay before tapping accept dialog button
     * @param acceptXOffset X offset from center for accept button (0.0-1.0)
     * @param acceptYOffset Y offset from center for accept button (0.0-1.0)
     * @return true if camera was launched successfully
     */
    public boolean takePictureWithCamera(int tapDelayMs, int returnDelayMs, float buttonX, float buttonY,
                                      int acceptDialogDelayMs, float acceptXOffset, float acceptYOffset) {
        if (localInputManager == null) return false;
        return localInputManager.takePictureWithCamera(tapDelayMs, returnDelayMs, buttonX, buttonY,
                                                     acceptDialogDelayMs, acceptXOffset, acceptYOffset);
    }
    
    /**
     * Record a video by launching the camera in video mode,
     * tapping to start recording, waiting for the specified duration,
     * and tapping again to stop recording.
     * 
     * @param durationMs Duration in milliseconds to record
     */
    public boolean recordVideo(long durationMs) {
        if (localInputManager == null) return false;
        return localInputManager.recordVideo(durationMs);
    }
    
    /**
     * Record video with configurable parameters.
     * 
     * @param durationMs Duration of recording in milliseconds
     * @param tapDelayMs Delay before tapping record button
     * @param returnDelayMs Delay before returning to app
     * @param buttonX X position of record button (0.0-1.0)
     * @param buttonY Y position of record button (0.0-1.0)
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo(long durationMs, int tapDelayMs, int returnDelayMs, float buttonX, float buttonY) {
        if (localInputManager == null) return false;
        return localInputManager.recordVideo(durationMs, tapDelayMs, returnDelayMs, buttonX, buttonY);
    }
    
    /**
     * Record video with fully configurable parameters including dialog handling.
     * 
     * @param durationMs Duration of recording in milliseconds
     * @param tapDelayMs Delay before tapping record button
     * @param returnDelayMs Delay before returning to app
     * @param buttonX X position of record button (0.0-1.0)
     * @param buttonY Y position of record button (0.0-1.0)
     * @param acceptDialogDelayMs Delay before tapping accept dialog button
     * @param acceptXOffset X offset from center for accept button (0.0-1.0)
     * @param acceptYOffset Y offset from center for accept button (0.0-1.0)
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo(long durationMs, int tapDelayMs, int returnDelayMs, float buttonX, float buttonY,
                             int acceptDialogDelayMs, float acceptXOffset, float acceptYOffset) {
        if (localInputManager == null) return false;
        return localInputManager.recordVideo(durationMs, tapDelayMs, returnDelayMs, buttonX, buttonY,
                                          acceptDialogDelayMs, acceptXOffset, acceptYOffset);
    }

    // Navigation constants
    public static final int NAV_UP = LocalInputManager.NAV_UP;
    public static final int NAV_DOWN = LocalInputManager.NAV_DOWN;
    public static final int NAV_LEFT = LocalInputManager.NAV_LEFT;
    public static final int NAV_RIGHT = LocalInputManager.NAV_RIGHT;
    public static final int NAV_BACK = LocalInputManager.NAV_BACK;
    public static final int NAV_HOME = LocalInputManager.NAV_HOME;
    public static final int NAV_RECENTS = LocalInputManager.NAV_RECENTS;
    
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
}
