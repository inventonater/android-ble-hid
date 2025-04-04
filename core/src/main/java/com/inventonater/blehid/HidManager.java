package com.inventonater.blehid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.inventonater.blehid.debug.BleHidDebugger;
import com.inventonater.blehid.descriptor.HidDescriptors;
import com.inventonater.blehid.report.KeyboardReporter;
import com.inventonater.blehid.report.MediaReporter;
import com.inventonater.blehid.report.MouseReporter;
import com.inventonater.blehid.util.LogUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Main manager class for Bluetooth HID Device functionality.
 * Uses the Android BluetoothHidDevice API introduced in Android 9 (API 28).
 * 
 * This class handles:
 * - Connection to the HID Service
 * - Registration as a HID Device
 * - Connecting with host devices
 * - Sending HID reports
 */
public class HidManager {
    private static final String TAG = "HidManager";
    
    // Constants for SDP configuration
    private static final String SDP_NAME = "Android BLE HID";
    private static final String SDP_DESCRIPTION = "HID Input Device";
    private static final String SDP_PROVIDER = "Inventonater";
    private static final byte SDP_SUBCLASS = 0x40; // BluetoothHidDevice.SUBCLASS1_COMBO (0x40)
    
    // Connection timeout
    private static final long SERVICE_CONNECTION_TIMEOUT = 5000; // 5 seconds
    
    // Context and Bluetooth components
    private final Context context;
    private final Handler mainHandler;
    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothHidDevice hidDevice;
    private final HidDeviceCallback callback;
    
    // State variables
    private boolean registered = false;
    private BluetoothDevice connectedDevice = null;
    private int connectionState = BluetoothProfile.STATE_DISCONNECTED;
    
    // HID reporters for different input types
    private MouseReporter mouseReporter;
    private KeyboardReporter keyboardReporter;
    private MediaReporter mediaReporter;
    
    // Debug components
    private HidDebugListener debugListener;
    private BleHidDebugger debugger;
    
    /**
     * Creates a new HID Manager.
     *
     * @param context Application context
     */
    public HidManager(Context context) {
        this.context = context.getApplicationContext();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.callback = new HidDeviceCallback(this);
        
        // Initialize the debugger
        this.debugger = BleHidDebugger.getInstance();
        this.debugger.initialize(this);
        
        log("HidManager created");
    }
    
    /**
     * Sets a debug listener to receive debug events.
     *
     * @param listener The debug listener
     */
    public void setDebugListener(HidDebugListener listener) {
        this.debugListener = listener;
        this.debugger.setDebugListener(listener);
        log("Debug listener set");
    }
    
    /**
     * Initializes the HID manager.
     * This connects to the Bluetooth HID service and prepares for registration.
     *
     * @return true if initialization started successfully, false otherwise
     */
    public boolean initialize() {
        log("Initializing HidManager");
        debugger.markTimestamp("initialized");
        
        // Get the BluetoothManager and adapter
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            logError("Failed to get BluetoothManager");
            return false;
        }
        
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            logError("Bluetooth adapter not available");
            return false;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            logError("Bluetooth is not enabled");
            return false;
        }
        
        // Analyze environment first
        debugger.analyzeEnvironment(context);
        
        // Connect to the HID service
        CountDownLatch connectionLatch = new CountDownLatch(1);
        final boolean[] result = {false};
        
        log("Connecting to Bluetooth HID service");
        bluetoothAdapter.getProfileProxy(context, new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    log("Connected to HID_DEVICE service");
                    hidDevice = (BluetoothHidDevice) proxy;
                    result[0] = true;
                    connectionLatch.countDown();
                    
                    // Create reporters now that we have the HID device
                    createReporters();
                }
            }
            
            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    logError("Disconnected from HID_DEVICE service");
                    hidDevice = null;
                    registered = false;
                    
                    // If not already counted down, do it now
                    if (connectionLatch.getCount() > 0) {
                        result[0] = false;
                        connectionLatch.countDown();
                    }
                }
            }
        }, BluetoothProfile.HID_DEVICE);
        
        // Wait for service connection with timeout
        try {
            connectionLatch.await(SERVICE_CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            logError("Service connection wait interrupted", e);
            return false;
        }
        
        return result[0];
    }
    
    /**
     * Releases resources and cleans up.
     */
    public void close() {
        log("Closing HidManager");
        
        if (hidDevice != null && bluetoothAdapter != null) {
            // Unregister app
            if (registered) {
                try {
                    hidDevice.unregisterApp();
                } catch (Exception e) {
                    logError("Error unregistering app", e);
                }
                registered = false;
            }
            
            // Close proxy
            try {
                bluetoothAdapter.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice);
            } catch (Exception e) {
                logError("Error closing profile proxy", e);
            }
            hidDevice = null;
        }
    }
    
    /**
     * Registers this app as a HID device with the system.
     *
     * @return true if registration is successful, false otherwise
     */
    public boolean register() {
        if (hidDevice == null) {
            logError("HID device service not connected");
            debugger.log("ERROR: HID device service is null. This is critical for HID registration.");
            return false;
        }
        
            if (registered) {
                log("Already registered, unregistering first");
                hidDevice.unregisterApp();
                registered = false;
            }
            
            log("Registering HID device app");
            
            // Check Bluetooth state
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter == null) {
                logError("Bluetooth adapter not available");
                debugger.log("ERROR: Bluetooth adapter is null, cannot register HID device");
                return false;
            }
            
            if (!adapter.isEnabled()) {
                logError("Bluetooth is not enabled");
                debugger.log("ERROR: Bluetooth is disabled, cannot register HID device");
                return false;
            }
            
            // Verify HID profile support
            boolean hidSupported = false;
            try {
                java.lang.reflect.Method method = adapter.getClass().getMethod("getSupportedProfiles");
                int[] profiles = (int[]) method.invoke(adapter);
                for (int profile : profiles) {
                    if (profile == BluetoothProfile.HID_DEVICE) {
                        hidSupported = true;
                        break;
                    }
                }
                
                if (!hidSupported) {
                    logError("HID Device profile not supported on this device");
                    debugger.log("ERROR: HID Device profile is not supported on this device. This is a critical requirement.");
                    return false;
                }
            } catch (Exception e) {
                debugger.log("WARNING: Could not verify HID profile support: " + e.getMessage());
                // Continue anyway since the check might fail on some devices
            }
            
            // Analyze HID descriptor
            debugger.analyzeHidDescriptor();
        
        // Create SDP record
        BluetoothHidDeviceAppSdpSettings sdp = new BluetoothHidDeviceAppSdpSettings(
                SDP_NAME,
                SDP_DESCRIPTION,
                SDP_PROVIDER,
                SDP_SUBCLASS,
                HidDescriptors.REPORT_MAP
        );
        
        debugger.log("SDP Settings created: " + SDP_NAME + ", " + SDP_DESCRIPTION + 
                ", subclass=" + String.format("0x%02X", SDP_SUBCLASS) + 
                ", descriptor size=" + HidDescriptors.REPORT_MAP.length);
        
        // Register app
        try {
            // Create a simple anonymous callback for registration since we can't directly use our callback
            android.bluetooth.BluetoothHidDevice.Callback callbackProxy = new android.bluetooth.BluetoothHidDevice.Callback() {
                public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
                    debugger.log("onAppStatusChanged: device=" + 
                            (pluggedDevice != null ? pluggedDevice.getAddress() : "null") + 
                            ", registered=" + registered);
                    callback.onAppStatusChanged(pluggedDevice, registered);
                }
                
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    debugger.log("onConnectionStateChanged: device=" + 
                            (device != null ? device.getAddress() : "null") + 
                            ", state=" + state);
                    callback.onConnectionStateChanged(device, state);
                }
                
                public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
                    debugger.log("onGetReport: device=" + 
                            (device != null ? device.getAddress() : "null") + 
                            ", type=" + type + ", id=" + id + ", bufferSize=" + bufferSize);
                    callback.onGetReport(device, type, id, bufferSize);
                }
                
                public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
                    debugger.log("onSetReport: device=" + 
                            (device != null ? device.getAddress() : "null") + 
                            ", type=" + type + ", id=" + id + ", data length=" + 
                            (data != null ? data.length : 0));
                    callback.onSetReport(device, type, id, data);
                }
                
                public void onSetProtocol(BluetoothDevice device, byte protocol) {
                    debugger.log("onSetProtocol: device=" + 
                            (device != null ? device.getAddress() : "null") + 
                            ", protocol=" + protocol);
                    callback.onSetProtocol(device, protocol);
                }
                
                public void onIntrData(BluetoothDevice device, byte reportId, byte[] data) {
                    debugger.log("onIntrData: device=" + 
                            (device != null ? device.getAddress() : "null") + 
                            ", reportId=" + reportId + ", data length=" + 
                            (data != null ? data.length : 0));
                    callback.onIntrData(device, reportId, data);
                }
                
                public void onVirtualCableUnplug(BluetoothDevice device) {
                    debugger.log("onVirtualCableUnplug: device=" + 
                            (device != null ? device.getAddress() : "null"));
                    callback.onVirtualCableUnplug(device);
                }
            };
            
            debugger.log("Calling registerApp on BluetoothHidDevice...");
            boolean registrationSuccess = hidDevice.registerApp(
                    sdp,
                    null,  // No in-band encryption
                    null,  // No out-band encryption
                    Executors.newSingleThreadExecutor(),
                    callbackProxy
            );
            
            if (registrationSuccess) {
                log("HID device registration initiated");
                debugger.log("registerApp returned true - registration process initiated");
            } else {
                logError("HID device registration failed");
                debugger.log("ERROR: registerApp returned false - registration process failed immediately");
                debugger.log("This could be due to:");
                debugger.log("1. Insufficient permissions (check Bluetooth permissions)");
                debugger.log("2. HID profile not properly supported by device");
                debugger.log("3. Another app already registered as HID device");
                debugger.log("4. System policy restrictions");
            }
            
            return registrationSuccess;
        } catch (Exception e) {
            logError("Error registering HID device app", e);
            debugger.log("EXCEPTION during registerApp: " + e.getClass().getName() + ": " + e.getMessage());
            if (e.getCause() != null) {
                debugger.log("Cause: " + e.getCause().getMessage());
            }
            return false;
        }
    }
    
    /**
     * Creates reporters for different input types.
     */
    private void createReporters() {
        mouseReporter = new MouseReporter(this);
        keyboardReporter = new KeyboardReporter(this);
        mediaReporter = new MediaReporter(this);
        
        log("Reporters created");
    }
    
    /**
     * Checks if a device is connected.
     *
     * @return true if a device is connected, false otherwise
     */
    public boolean isConnected() {
        return connectedDevice != null && 
               connectionState == BluetoothProfile.STATE_CONNECTED;
    }
    
    /**
     * Checks if this app is registered as a HID device.
     *
     * @return true if registered, false otherwise
     */
    public boolean isRegistered() {
        return registered;
    }
    
    /**
     * Gets the currently connected device.
     *
     * @return The connected device, or null if not connected
     */
    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }
    
    /**
     * Gets the mouse reporter.
     *
     * @return The mouse reporter
     */
    public MouseReporter getMouseReporter() {
        return mouseReporter;
    }
    
    /**
     * Gets the keyboard reporter.
     *
     * @return The keyboard reporter
     */
    public KeyboardReporter getKeyboardReporter() {
        return keyboardReporter;
    }
    
    /**
     * Gets the media reporter.
     *
     * @return The media reporter
     */
    public MediaReporter getMediaReporter() {
        return mediaReporter;
    }
    
    /**
     * Gets the Bluetooth HID device instance.
     *
     * @return The BluetoothHidDevice instance
     */
    public BluetoothHidDevice getHidDevice() {
        return hidDevice;
    }
    
    /**
     * Sets the registration state.
     *
     * @param registered Whether the app is registered
     */
    void setRegistered(boolean registered) {
        this.registered = registered;
        log("Registration state changed: " + registered);
    }
    
    /**
     * Sets the connected device and state.
     *
     * @param device The connected device
     * @param state The connection state
     */
    void setConnectionState(BluetoothDevice device, int state) {
        int previousState = this.connectionState;
        this.connectedDevice = state == BluetoothProfile.STATE_CONNECTED ? device : null;
        this.connectionState = state;
        
        String stateStr = "UNKNOWN";
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                stateStr = "CONNECTED";
                break;
            case BluetoothProfile.STATE_CONNECTING:
                stateStr = "CONNECTING";
                break;
            case BluetoothProfile.STATE_DISCONNECTED:
                stateStr = "DISCONNECTED";
                break;
            case BluetoothProfile.STATE_DISCONNECTING:
                stateStr = "DISCONNECTING";
                break;
        }
        
        String deviceInfo = device != null 
                ? (device.getName() != null ? device.getName() : "unnamed") 
                    + " (" + device.getAddress() + ")" 
                : "null";
                
        log("Connection state changed: " + stateStr + " - Device: " + deviceInfo);
        
        // Let the debugger analyze the connection state change
        debugger.analyzeConnectionStateChange(device, state, previousState);
    }
    
    /**
     * Log a debug message and send to the debug listener if available.
     *
     * @param message The message to log
     */
    public void log(String message) {
        String fullMessage = "HidManager: " + message;
        Log.d(TAG, fullMessage);
        
        if (debugListener != null) {
            mainHandler.post(() -> debugListener.onDebugMessage(fullMessage));
        }
    }
    
    /**
     * Gets the debugger instance.
     *
     * @return The debugger instance
     */
    public BleHidDebugger getDebugger() {
        return debugger;
    }
    
    /**
     * Log an error message and send to the debug listener if available.
     *
     * @param message The error message
     */
    public void logError(String message) {
        String fullMessage = "ERROR: " + message;
        Log.e(TAG, fullMessage);
        
        if (debugListener != null) {
            mainHandler.post(() -> debugListener.onDebugMessage(fullMessage));
        }
    }
    
    /**
     * Log an error message with exception and send to the debug listener if available.
     *
     * @param message The error message
     * @param e The exception
     */
    public void logError(String message, Throwable e) {
        String fullMessage = "ERROR: " + message + " - " + LogUtils.getExceptionDetails(e);
        Log.e(TAG, fullMessage, e);
        
        if (debugListener != null) {
            mainHandler.post(() -> debugListener.onDebugMessage(fullMessage));
        }
    }
    
    /**
     * Log a verbose message with a hex dump of data.
     *
     * @param message The message
     * @param data The data to dump
     */
    public void logVerbose(String message, byte[] data) {
        String hexDump = LogUtils.byteArrayToHex(data);
        String fullMessage = message + " - " + hexDump;
        Log.v(TAG, fullMessage);
        
        if (debugListener != null) {
            mainHandler.post(() -> debugListener.onDebugMessage(fullMessage));
        }
    }
}
