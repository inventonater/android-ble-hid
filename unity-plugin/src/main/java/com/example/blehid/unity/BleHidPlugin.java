package com.example.blehid.unity;

import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.function.Supplier;

import com.example.blehid.core.BleHidManager;
import com.example.blehid.core.HidMediaConstants;

/**
 * Static interface for Unity to access BLE HID functionality.
 * Provides methods for initializing, controlling advertising, and sending media and mouse events.
 */
public class BleHidPlugin {
    private static final String TAG = "BleHidPlugin";
    
    private static BleHidManager bleHidManager;
    private static UnityCallback callback;
    private static boolean isInitialized = false;
    
    /**
     * Initializes the BLE HID plugin with the given context.
     * 
     * @param context The application context
     * @return true if initialization was successful, false otherwise
     */
    public static boolean initialize(Context context) {
        if (isInitialized) {
            Log.w(TAG, "BLE HID Plugin already initialized");
            return true;
        }
        
        if (context == null) {
            Log.e(TAG, "Context is null, cannot initialize");
            return false;
        }
        
        try {
            // Store the application context to prevent leaks
            Context appContext = context.getApplicationContext();
            
            // Check for Android 12+ permissions - the Unity side will handle requesting them
            // This just logs the status for debugging purposes
            if (android.os.Build.VERSION.SDK_INT >= 31) { // Android 12
                Log.i(TAG, "Running on Android 12+, checking permissions status");
                boolean hasAdvertisePermission = hasPermission(appContext, "android.permission.BLUETOOTH_ADVERTISE");
                boolean hasConnectPermission = hasPermission(appContext, "android.permission.BLUETOOTH_CONNECT");
                boolean hasScanPermission = hasPermission(appContext, "android.permission.BLUETOOTH_SCAN");
                
                Log.i(TAG, "BLUETOOTH_ADVERTISE permission: " + (hasAdvertisePermission ? "Granted" : "Not granted"));
                Log.i(TAG, "BLUETOOTH_CONNECT permission: " + (hasConnectPermission ? "Granted" : "Not granted"));
                Log.i(TAG, "BLUETOOTH_SCAN permission: " + (hasScanPermission ? "Granted" : "Not granted"));
            }
            
            // Create and initialize the BLE HID manager
            bleHidManager = new BleHidManager(appContext);
            boolean result = bleHidManager.initialize();
            
            if (result) {
                isInitialized = true;
                Log.i(TAG, "BLE HID Plugin initialized successfully");
            } else {
                Log.e(TAG, "Failed to initialize BLE HID Manager");
                bleHidManager = null;
            }
            
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error initializing BLE HID Plugin", e);
            return false;
        }
    }
    
    /**
     * Helper method to check if a permission is granted
     */
    private static boolean hasPermission(Context context, String permission) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return context.checkSelfPermission(permission) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true; // On older versions, permissions are granted at install time
    }
    
    /**
     * Checks if BLE peripheral mode is supported on this device.
     * 
     * @return true if BLE peripheral mode is supported, false otherwise
     */
    public static boolean isBlePeripheralSupported() {
        if (bleHidManager == null) {
            Log.e(TAG, "BLE HID Manager not initialized");
            return false;
        }
        
        return bleHidManager.isBlePeripheralSupported();
    }
    
    /**
     * Starts advertising the BLE HID device.
     * 
     * @return true if advertising started successfully, false otherwise
     */
    public static boolean startAdvertising() {
        Boolean result = executeCommand("Start Advertising", false, 
            () -> {
                boolean success = bleHidManager.startAdvertising();
                if (success) {
                    // Use Log.i to maintain the same log level as before
                    Log.i(TAG, "BLE advertising started");
                }
                return success;
            }, false);
        
        return result;
    }
    
    /**
     * Stops advertising the BLE HID device.
     */
    public static void stopAdvertising() {
        // Execute as a command for consistent logging and error handling
        executeCommand("Stop Advertising", false, 
            () -> {
                bleHidManager.stopAdvertising();
                // Use Log.i to maintain the same log level as before
                Log.i(TAG, "BLE advertising stopped");
                return true; // Return success
            }, false);
    }
    
    /**
     * Checks if the device is connected to a host.
     * 
     * @return true if connected, false otherwise
     */
    public static boolean isConnected() {
        return executeCommand("Check Connection", false, 
            () -> bleHidManager.isConnected(), false);
    }
    
    /**
     * Media Control Methods
     */
    
    /**
     * Sends a play/pause control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean playPause() {
        return executeCommand("Play/Pause", true, 
            () -> bleHidManager.playPause(), false);
    }
    
    /**
     * Sends a next track control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean nextTrack() {
        return executeCommand("Next Track", true, 
            () -> bleHidManager.nextTrack(), false);
    }
    
    /**
     * Sends a previous track control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean previousTrack() {
        return executeCommand("Previous Track", true, 
            () -> bleHidManager.previousTrack(), false);
    }
    
    /**
     * Sends a volume up control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean volumeUp() {
        return executeCommand("Volume Up", true, 
            () -> bleHidManager.volumeUp(), false);
    }
    
    /**
     * Sends a volume down control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean volumeDown() {
        return executeCommand("Volume Down", true, 
            () -> bleHidManager.volumeDown(), false);
    }
    
    /**
     * Sends a mute control command.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean mute() {
        return executeCommand("Mute", true, 
            () -> bleHidManager.mute(), false);
    }
    
    /**
     * Mouse Control Methods
     */
    
    /**
     * Moves the mouse pointer by the specified amount.
     *
     * @param x The X movement amount (-127 to 127)
     * @param y The Y movement amount (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean moveMouse(int x, int y) {
        // Clamp values to valid range
        final int clampedX = Math.max(-127, Math.min(127, x));
        final int clampedY = Math.max(-127, Math.min(127, y));
        
        return executeCommand("Move Mouse (x=" + clampedX + ", y=" + clampedY + ")", true, 
            () -> bleHidManager.moveMouse(clampedX, clampedY), false);
    }
    
    /**
     * Presses a mouse button.
     *
     * @param button The button to press (HidMediaConstants.BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean pressMouseButton(int button) {
        return executeCommand("Press Mouse Button " + button, true, 
            () -> bleHidManager.pressMouseButton(button), false);
    }
    
    /**
     * Releases all mouse buttons.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean releaseMouseButtons() {
        return executeCommand("Release Mouse Buttons", true, 
            () -> bleHidManager.releaseMouseButtons(), false);
    }
    
    /**
     * Performs a click with the specified button.
     *
     * @param button The button to click (HidMediaConstants.BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean clickMouseButton(int button) {
        return executeCommand("Click Mouse Button " + button, true, 
            () -> bleHidManager.clickMouseButton(button), false);
    }
    
    /**
     * Scrolls the mouse wheel.
     *
     * @param amount The scroll amount (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    public static boolean scrollMouseWheel(int amount) {
        // Clamp value to valid range
        final int clampedAmount = Math.max(-127, Math.min(127, amount));
        
        return executeCommand("Scroll Mouse Wheel " + clampedAmount, true, 
            () -> bleHidManager.scrollMouseWheel(clampedAmount), false);
    }
    
    /**
     * Combined Media and Mouse Control
     */
    
    /**
     * Sends a combined media and mouse report.
     * 
     * @param mediaButtons Media button flags (HidMediaConstants.BUTTON_PLAY_PAUSE, etc.)
     * @param mouseButtons Mouse button flags (HidMediaConstants.BUTTON_LEFT, etc.)
     * @param x X-axis movement (-127 to 127)
     * @param y Y-axis movement (-127 to 127)
     * @return true if successful, false otherwise
     */
    public static boolean sendCombinedReport(int mediaButtons, int mouseButtons, int x, int y) {
        // Clamp values to valid range
        final int clampedX = Math.max(-127, Math.min(127, x));
        final int clampedY = Math.max(-127, Math.min(127, y));
        
        String commandDesc = String.format("Send Combined Report (media=0x%02X, mouse=0x%02X, x=%d, y=%d)", 
                                          mediaButtons, mouseButtons, clampedX, clampedY);
        
        return executeCommand(commandDesc, true, 
            () -> bleHidManager.sendCombinedReport(mediaButtons, mouseButtons, clampedX, clampedY), false);
    }
    
    /**
     * Sets the Unity callback for BLE HID events.
     * 
     * @param callback The callback to set
     */
    public static void setCallback(UnityCallback callback) {
        BleHidPlugin.callback = callback;
        
        if (bleHidManager != null) {
            // Set up connection callback
            ConnectionCallback connectionCallback = new ConnectionCallback(callback);
            bleHidManager.getBlePairingManager().setPairingCallback(connectionCallback);
        }
    }
    
    /**
     * Gets the address of the connected device.
     * 
     * @return The MAC address of the connected device, or null if not connected
     */
    public static String getConnectedDeviceAddress() {
        return executeCommand("Get Connected Device Address", true, 
            () -> bleHidManager.getConnectedDevice().getAddress(), null);
    }
    
    /**
     * Cleans up resources when the plugin is no longer needed.
     */
    public static void close() {
        // Using executeCommand with special handling since isInitialized will be set to false
        // We don't want to check initialization since we're in the process of closing
        if (bleHidManager != null) {
            try {
                bleHidManager.close();
                Log.i(TAG, "BLE HID resources closed");
            } catch (Exception e) {
                Log.e(TAG, "Error closing BLE HID resources", e);
            } finally {
                bleHidManager = null;
                callback = null;
                isInitialized = false;
                Log.i(TAG, "BLE HID Plugin closed");
            }
        } else {
            // Already closed or never initialized
            callback = null;
            isInitialized = false;
        }
    }
    
    /**
     * Executes a BLE HID command with standard error checking and logging.
     * 
     * @param <T> Return type of the command
     * @param commandName Name of the command for logging
     * @param requiresConnection Whether the command requires an active connection
     * @param command The command to execute
     * @param defaultValue Value to return if prerequisites fail
     * @return The result of the command, or defaultValue if prerequisites fail
     */
    private static <T> T executeCommand(String commandName, boolean requiresConnection, 
                                      Supplier<T> command, T defaultValue) {
        if (!checkInitialized()) return defaultValue;
        
        if (requiresConnection && !bleHidManager.isConnected()) {
            Log.w(TAG, "Not connected to a host, cannot execute: " + commandName);
            return defaultValue;
        }
        
        try {
            T result = command.get();
            if (result instanceof Boolean) {
                Boolean boolResult = (Boolean)result;
                if (boolResult) {
                    Log.d(TAG, commandName + " succeeded");
                } else {
                    Log.e(TAG, "Failed to " + commandName);
                }
            } else {
                Log.d(TAG, commandName + " executed, result: " + result);
            }
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error executing " + commandName, e);
            return defaultValue;
        }
    }
    
    /**
     * Gets the current system state as a set of flags.
     * Used to verify system state across language boundaries.
     * 
     * @return Byte with appropriate state flags set
     */
    public static byte getSystemState() {
        byte state = 0;
        
        if (isInitialized && bleHidManager != null) {
            state |= BleHidProtocol.State.INITIALIZED;
            
            if (bleHidManager.isConnected()) {
                state |= BleHidProtocol.State.CONNECTED;
            }
            
            if (bleHidManager.isAdvertising()) {
                state |= BleHidProtocol.State.ADVERTISING;
            }
            
            if (bleHidManager.isBlePeripheralSupported()) {
                state |= BleHidProtocol.State.PERIPHERAL_SUPPORTED;
            }
        }
        
        return state;
    }
    
    /**
     * Verifies that a particular aspect of the system state is active.
     * 
     * @param stateFlag The state flag to verify
     * @return true if the state flag is active, false otherwise
     */
    public static boolean verifyState(byte stateFlag) {
        byte currentState = getSystemState();
        return (currentState & stateFlag) == stateFlag;
    }
    
    /**
     * Checks if the BLE HID Manager is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    private static boolean checkInitialized() {
        if (!isInitialized || bleHidManager == null) {
            Log.e(TAG, "BLE HID Manager not initialized");
            return false;
        }
        
        return true;
    }
    
    // Command batch fields and methods
    
    /**
     * Represents a single command in a batch operation
     */
    private static class BatchCommand {
        final int commandId;
        final Object[] params;
        
        BatchCommand(int commandId, Object... params) {
            this.commandId = commandId;
            this.params = params;
        }
    }
    
    /**
     * Batch of commands to be executed together
     */
    private static final ArrayList<BatchCommand> commandBatch = new ArrayList<>();
    
    /**
     * Starts a new command batch.
     * Call this before adding commands to the batch.
     */
    public static void startBatch() {
        commandBatch.clear();
        Log.d(TAG, "Command batch started");
    }
    
    /**
     * Adds a media command to the current batch.
     * 
     * @param mediaButtonFlag Media button flag (BleHidProtocol.MediaButton constants)
     */
    public static void addMediaCommand(int mediaButtonFlag) {
        commandBatch.add(new BatchCommand(
            BleHidProtocol.Command.SEND_COMBINED_REPORT,
            mediaButtonFlag, // mediaButtons
            0,               // mouseButtons
            0,               // x
            0                // y
        ));
        Log.d(TAG, "Media command added to batch: 0x" + Integer.toHexString(mediaButtonFlag));
    }
    
    /**
     * Adds a mouse movement command to the current batch.
     * 
     * @param x X-axis movement (-127 to 127)
     * @param y Y-axis movement (-127 to 127)
     */
    public static void addMouseMove(int x, int y) {
        // Clamp values to valid range
        final int clampedX = Math.max(-127, Math.min(127, x));
        final int clampedY = Math.max(-127, Math.min(127, y));
        
        commandBatch.add(new BatchCommand(
            BleHidProtocol.Command.SEND_COMBINED_REPORT,
            0,               // mediaButtons
            0,               // mouseButtons
            clampedX,        // x
            clampedY         // y
        ));
        Log.d(TAG, "Mouse move added to batch: x=" + clampedX + ", y=" + clampedY);
    }
    
    /**
     * Adds a mouse button command to the current batch.
     * 
     * @param mouseButtonFlag Mouse button flag (BleHidProtocol.MouseButton constants)
     * @param pressed True for press, false for release
     */
    public static void addMouseButton(int mouseButtonFlag, boolean pressed) {
        commandBatch.add(new BatchCommand(
            BleHidProtocol.Command.SEND_COMBINED_REPORT,
            0,                          // mediaButtons
            pressed ? mouseButtonFlag : 0, // mouseButtons
            0,                          // x
            0                           // y
        ));
        Log.d(TAG, "Mouse button added to batch: button=0x" + Integer.toHexString(mouseButtonFlag) 
            + ", pressed=" + pressed);
    }
    
    /**
     * Executes all commands in the current batch.
     * 
     * @return True if all commands were executed successfully, false otherwise
     */
    public static boolean executeBatch() {
        if (commandBatch.isEmpty()) {
            Log.w(TAG, "Batch is empty, nothing to execute");
            return true; // Nothing to do, but not an error
        }
        
        return executeCommand("Execute Batch (" + commandBatch.size() + " commands)", true, 
            () -> {
                boolean success = true;
                int commandCount = 0;
                
                for (BatchCommand cmd : commandBatch) {
                    commandCount++;
                    boolean cmdResult = false;
                    
                    try {
                        switch (cmd.commandId) {
                            case BleHidProtocol.Command.SEND_COMBINED_REPORT:
                                int mediaButtons = (int)cmd.params[0];
                                int mouseButtons = (int)cmd.params[1];
                                int x = (int)cmd.params[2];
                                int y = (int)cmd.params[3];
                                cmdResult = bleHidManager.sendCombinedReport(mediaButtons, mouseButtons, x, y);
                                break;
                                
                            // Add more command types as needed
                            
                            default:
                                Log.e(TAG, "Unknown command ID in batch: " + cmd.commandId);
                                cmdResult = false;
                                break;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error executing batch command #" + commandCount, e);
                        cmdResult = false;
                    }
                    
                    // We continue executing even if one command fails, but track overall success
                    success = success && cmdResult;
                }
                
                // Clear the batch after execution
                int batchSize = commandBatch.size();
                commandBatch.clear();
                
                Log.d(TAG, "Batch execution completed: " + batchSize + " commands, success=" + success);
                return success;
            }, false);
    }
    
    /**
     * Inner class for handling connection callbacks and forwarding them to Unity.
     */
    private static class ConnectionCallback implements com.example.blehid.core.BlePairingManager.PairingCallback {
        private final UnityCallback unityCallback;
        
        ConnectionCallback(UnityCallback callback) {
            this.unityCallback = callback;
        }
        
        @Override
        public void onPairingRequested(android.bluetooth.BluetoothDevice device, int variant) {
            if (unityCallback != null) {
                unityCallback.onPairingRequested(device.getAddress(), variant);
            }
            
            // Auto-accept pairing requests
            bleHidManager.getBlePairingManager().setPairingConfirmation(device, true);
        }
        
        @Override
        public void onPairingComplete(android.bluetooth.BluetoothDevice device, boolean success) {
            if (unityCallback != null) {
                if (success) {
                    unityCallback.onDeviceConnected(device.getAddress());
                } else {
                    unityCallback.onPairingFailed(device.getAddress());
                }
            }
        }
    }
}
