package com.inventonater.blehid.debug;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.inventonater.blehid.HidDebugListener;
import com.inventonater.blehid.HidManager;
import com.inventonater.blehid.HidReportConstants;
import com.inventonater.blehid.descriptor.HidDescriptors;
import com.inventonater.blehid.util.LogUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Advanced debugging utility for BLE HID communication.
 * 
 * This class provides enhanced logging and analysis for BLE HID operations,
 * helping to diagnose issues with the peripheral implementation, connection
 * handling, and report transmission.
 */
public class BleHidDebugger {
    private static final String TAG = "BleHidDebugger";
    
    // Singleton instance
    private static BleHidDebugger instance;
    
    // Reference to the HID manager
    private HidManager hidManager;
    
    // Debug state
    private boolean verboseLogging = false;
    private boolean fileLogging = false;
    private String logFilePath;
    private FileOutputStream logOutputStream;
    
    // Counters for report statistics
    private final AtomicInteger mouseReportsSent = new AtomicInteger(0);
    private final AtomicInteger keyboardReportsSent = new AtomicInteger(0);
    private final AtomicInteger consumerReportsSent = new AtomicInteger(0);
    private final AtomicInteger reportsSendFailed = new AtomicInteger(0);
    
    // Timestamps for performance measurements
    private final ConcurrentHashMap<String, Long> timestamps = new ConcurrentHashMap<>();
    
    // Debug listener for UI updates
    private HidDebugListener debugListener;
    private Handler mainHandler;
    
    /**
     * Gets the singleton instance of the debugger.
     * 
     * @return The debugger instance
     */
    public static synchronized BleHidDebugger getInstance() {
        if (instance == null) {
            instance = new BleHidDebugger();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private BleHidDebugger() {
        mainHandler = new Handler(Looper.getMainLooper());
        log("BleHidDebugger initialized");
    }
    
    /**
     * Initializes the debugger with a HID manager.
     * 
     * @param manager The HID manager to debug
     */
    public void initialize(HidManager manager) {
        this.hidManager = manager;
        log("BleHidDebugger attached to HidManager");
    }
    
    /**
     * Sets a debug listener to receive debug events.
     * 
     * @param listener The debug listener
     */
    public void setDebugListener(HidDebugListener listener) {
        this.debugListener = listener;
        log("Debug listener set");
    }
    
    /**
     * Enables or disables verbose logging.
     * 
     * @param enable Whether to enable verbose logging
     */
    public void enableVerboseLogging(boolean enable) {
        this.verboseLogging = enable;
        log("Verbose logging " + (enable ? "enabled" : "disabled"));
    }
    
    /**
     * Enables or disables file logging.
     * 
     * @param context The context to use for file operations
     * @param enable Whether to enable file logging
     */
    public void enableFileLogging(Context context, boolean enable) {
        if (enable && !fileLogging) {
            try {
                File logDir = new File(context.getExternalFilesDir(null), "hid_logs");
                if (!logDir.exists()) {
                    logDir.mkdirs();
                }
                
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
                String timestamp = sdf.format(new Date());
                File logFile = new File(logDir, "hid_log_" + timestamp + ".txt");
                
                logOutputStream = new FileOutputStream(logFile, true);
                logFilePath = logFile.getAbsolutePath();
                fileLogging = true;
                
                log("File logging enabled: " + logFilePath);
            } catch (IOException e) {
                Log.e(TAG, "Failed to create log file", e);
                fileLogging = false;
            }
        } else if (!enable && fileLogging) {
            try {
                if (logOutputStream != null) {
                    logOutputStream.close();
                    logOutputStream = null;
                }
                fileLogging = false;
                log("File logging disabled");
            } catch (IOException e) {
                Log.e(TAG, "Error closing log file", e);
            }
        }
    }
    
    /**
     * Logs a report sent event for statistics.
     * 
     * @param reportId The report ID
     * @param success Whether the report was sent successfully
     */
    public void logReportSent(byte reportId, boolean success) {
        if (success) {
            switch (reportId) {
                case HidReportConstants.REPORT_ID_MOUSE:
                    mouseReportsSent.incrementAndGet();
                    break;
                case HidReportConstants.REPORT_ID_KEYBOARD:
                    keyboardReportsSent.incrementAndGet();
                    break;
                case HidReportConstants.REPORT_ID_CONSUMER:
                    consumerReportsSent.incrementAndGet();
                    break;
            }
        } else {
            reportsSendFailed.incrementAndGet();
        }
        
        if (verboseLogging) {
            log("Report sent: ID=" + reportId + ", success=" + success + 
                    getReportStatistics());
        }
    }
    
    /**
     * Gets a formatted string with report statistics.
     * 
     * @return The report statistics
     */
    public String getReportStatistics() {
        return "\nMouse reports: " + mouseReportsSent.get() + 
               "\nKeyboard reports: " + keyboardReportsSent.get() + 
               "\nConsumer reports: " + consumerReportsSent.get() + 
               "\nFailed reports: " + reportsSendFailed.get();
    }
    
    /**
     * Records a timestamp for performance measurement.
     * 
     * @param marker The marker name
     */
    public void markTimestamp(String marker) {
        timestamps.put(marker, System.currentTimeMillis());
    }
    
    /**
     * Gets the elapsed time since a timestamp was recorded.
     * 
     * @param marker The marker name
     * @return The elapsed time in milliseconds, or -1 if the marker is not found
     */
    public long getElapsedTime(String marker) {
        Long start = timestamps.get(marker);
        if (start == null) {
            return -1;
        }
        return System.currentTimeMillis() - start;
    }
    
    /**
     * Logs the elapsed time since a timestamp was recorded.
     * 
     * @param marker The marker name
     * @param description A description of the operation
     */
    public void logElapsedTime(String marker, String description) {
        long elapsed = getElapsedTime(marker);
        if (elapsed >= 0) {
            log(description + " took " + elapsed + "ms");
        }
    }
    
    /**
     * Analyzes a connection state change.
     * 
     * @param device The Bluetooth device
     * @param newState The new connection state
     * @param previousState The previous connection state
     */
    public void analyzeConnectionStateChange(BluetoothDevice device, int newState, int previousState) {
        String deviceInfo = formatDeviceInfo(device);
        String newStateStr = connectionStateToString(newState);
        String prevStateStr = connectionStateToString(previousState);
        
        log("Connection state changed: " + deviceInfo + " " + prevStateStr + " -> " + newStateStr);
        
        if (previousState == BluetoothProfile.STATE_CONNECTING && 
                newState == BluetoothProfile.STATE_DISCONNECTED) {
            log("WARNING: Connection attempt failed. Possible causes:");
            log("  - Device pairing issue");
            log("  - Device out of range");
            log("  - HID service not properly registered");
            log("  - Host rejected the connection");
        }
        
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            markTimestamp("connected");
            logElapsedTime("initialized", "Connection establishment");
        }
    }
    
    /**
     * Analyzes a pairing state change.
     * 
     * @param device The Bluetooth device
     * @param bondState The new bond state
     * @param prevBondState The previous bond state
     */
    public void analyzePairingStateChange(BluetoothDevice device, int bondState, int prevBondState) {
        String deviceInfo = formatDeviceInfo(device);
        String bondStateStr = bondStateToString(bondState);
        String prevBondStateStr = bondStateToString(prevBondState);
        
        log("Pairing state changed: " + deviceInfo + " " + prevBondStateStr + " -> " + bondStateStr);
        
        if (prevBondState == BluetoothDevice.BOND_BONDING && 
                bondState == BluetoothDevice.BOND_NONE) {
            log("WARNING: Pairing failed. Possible causes:");
            log("  - Incorrect PIN/passkey");
            log("  - User rejected pairing on host");
            log("  - Timeout during pairing");
        }
    }
    
    /**
     * Analyzes the HID descriptor to verify it's properly formatted.
     */
    public void analyzeHidDescriptor() {
        byte[] reportMap = HidDescriptors.REPORT_MAP;
        
        log("Analyzing HID descriptor (" + reportMap.length + " bytes)");
        
        boolean hasKeyboard = false;
        boolean hasMouse = false;
        boolean hasConsumer = false;
        
        // Simple analysis - look for report IDs
        for (int i = 0; i < reportMap.length - 1; i++) {
            if (reportMap[i] == (byte)0x85) { // REPORT_ID tag
                byte reportId = reportMap[i+1];
                switch (reportId) {
                    case HidReportConstants.REPORT_ID_KEYBOARD:
                        hasKeyboard = true;
                        break;
                    case HidReportConstants.REPORT_ID_MOUSE:
                        hasMouse = true;
                        break;
                    case HidReportConstants.REPORT_ID_CONSUMER:
                        hasConsumer = true;
                        break;
                }
            }
        }
        
        log("Report descriptor analysis:");
        log("  Keyboard report: " + (hasKeyboard ? "Found" : "Not found"));
        log("  Mouse report: " + (hasMouse ? "Found" : "Not found"));
        log("  Consumer control report: " + (hasConsumer ? "Found" : "Not found"));
        
        if (!hasKeyboard || !hasMouse || !hasConsumer) {
            log("WARNING: Some report descriptors are missing, which may cause functionality issues");
        }
    }
    
    /**
     * Analyzes the device environment and available Bluetooth features.
     * 
     * @param context The context to use for system service access
     */
    public void analyzeEnvironment(Context context) {
        log("Environment analysis:");
        log("  Device: " + Build.MANUFACTURER + " " + Build.MODEL);
        log("  Android version: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            log("  ERROR: Bluetooth not supported on this device");
            return;
        }
        
        log("  Bluetooth enabled: " + adapter.isEnabled());
        log("  Bluetooth name: " + adapter.getName());
        log("  Bluetooth address: " + adapter.getAddress());
        
        // Check if BLE advertising is supported
        if (Build.VERSION.SDK_INT >= 21) { // Lollipop or higher
            try {
                log("  BLE Advertising supported: " + (adapter.getBluetoothLeAdvertiser() != null));
            } catch (Exception e) {
                log("  BLE Advertising check failed: " + e.getMessage());
            }
        }
        
        try {
            // Use reflection to get supported profiles
            java.lang.reflect.Method method = adapter.getClass().getMethod("getSupportedProfiles");
            int[] profiles = (int[]) method.invoke(adapter);
            
            StringBuilder supportedProfiles = new StringBuilder();
            boolean hidSupported = false;
            
            for (int profile : profiles) {
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidSupported = true;
                    supportedProfiles.append("HID_DEVICE, ");
                } else {
                    switch (profile) {
                        case BluetoothProfile.HEADSET:
                            supportedProfiles.append("HEADSET, ");
                            break;
                        case BluetoothProfile.A2DP:
                            supportedProfiles.append("A2DP, ");
                            break;
                        case BluetoothProfile.GATT:
                            supportedProfiles.append("GATT, ");
                            break;
                        case BluetoothProfile.GATT_SERVER:
                            supportedProfiles.append("GATT_SERVER, ");
                            break;
                        default:
                            supportedProfiles.append(profile).append(", ");
                            break;
                    }
                }
            }
            
            log("  Supported profiles: " + supportedProfiles.toString());
            log("  HID Device profile supported: " + hidSupported);
            
            if (!hidSupported) {
                log("  ERROR: HID Device profile not supported on this device!");
                log("  This is a critical requirement for HID peripheral functionality.");
            }
            
        } catch (Exception e) {
            log("  Error checking supported profiles: " + e.getMessage());
        }
    }
    
    /**
     * Formats device information for logging.
     * 
     * @param device The Bluetooth device
     * @return Formatted device information
     */
    private String formatDeviceInfo(BluetoothDevice device) {
        if (device == null) {
            return "unknown device";
        }
        
        String name = device.getName();
        String address = device.getAddress();
        
        if (name == null || name.isEmpty()) {
            return "unnamed device (" + address + ")";
        } else {
            return name + " (" + address + ")";
        }
    }
    
    /**
     * Converts a connection state to a string representation.
     * 
     * @param state The connection state
     * @return String representation of the state
     */
    private String connectionStateToString(int state) {
        switch (state) {
            case BluetoothProfile.STATE_CONNECTED:
                return "CONNECTED";
            case BluetoothProfile.STATE_CONNECTING:
                return "CONNECTING";
            case BluetoothProfile.STATE_DISCONNECTED:
                return "DISCONNECTED";
            case BluetoothProfile.STATE_DISCONNECTING:
                return "DISCONNECTING";
            default:
                return "UNKNOWN(" + state + ")";
        }
    }
    
    /**
     * Converts a bond state to a string representation.
     * 
     * @param bondState The bond state
     * @return String representation of the state
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
     * Logs a debug message and sends it to the debug listener if available.
     * 
     * @param message The message to log
     */
    public void log(String message) {
        String fullMessage = "HidDebugger: " + message;
        Log.d(TAG, fullMessage);
        
        // Write to log file if enabled
        if (fileLogging && logOutputStream != null) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
                String timestamp = sdf.format(new Date());
                String logLine = timestamp + " - " + fullMessage + "\n";
                logOutputStream.write(logLine.getBytes());
                logOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error writing to log file", e);
            }
        }
        
        // Forward to debug listener if available
        if (debugListener != null) {
            final String msg = fullMessage;
            mainHandler.post(() -> debugListener.onDebugMessage(msg));
        }
    }
}
