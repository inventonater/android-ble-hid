package com.inventonater.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/**
 * Implements a BLE HID service supporting media player, mouse, and keyboard functionality.
 * Based on the HID consumer control specification.
 */
public class HidMediaService {
    private static final String TAG = "HidMediaService";
    
    // Media control button constants
    public static final int BUTTON_PLAY_PAUSE = HidConstants.Media.BUTTON_PLAY_PAUSE;
    public static final int BUTTON_NEXT_TRACK = HidConstants.Media.BUTTON_NEXT_TRACK;
    public static final int BUTTON_PREVIOUS_TRACK = HidConstants.Media.BUTTON_PREVIOUS_TRACK;
    public static final int BUTTON_VOLUME_UP = HidConstants.Media.BUTTON_VOLUME_UP;
    public static final int BUTTON_VOLUME_DOWN = HidConstants.Media.BUTTON_VOLUME_DOWN;
    public static final int BUTTON_MUTE = HidConstants.Media.BUTTON_MUTE;
    
    // Mouse button constants
    public static final int BUTTON_LEFT = HidConstants.Mouse.BUTTON_LEFT;
    public static final int BUTTON_RIGHT = HidConstants.Mouse.BUTTON_RIGHT;
    public static final int BUTTON_MIDDLE = HidConstants.Mouse.BUTTON_MIDDLE;
    
    // Keyboard modifiers
    public static final int KEY_MOD_LCTRL = HidConstants.Keyboard.MOD_LCTRL;
    public static final int KEY_MOD_LSHIFT = HidConstants.Keyboard.MOD_LSHIFT;
    public static final int KEY_MOD_LALT = HidConstants.Keyboard.MOD_LALT;
    public static final int KEY_MOD_LMETA = HidConstants.Keyboard.MOD_LMETA;
    public static final int KEY_MOD_RCTRL = HidConstants.Keyboard.MOD_RCTRL;
    public static final int KEY_MOD_RSHIFT = HidConstants.Keyboard.MOD_RSHIFT;
    public static final int KEY_MOD_RALT = HidConstants.Keyboard.MOD_RALT;
    public static final int KEY_MOD_RMETA = HidConstants.Keyboard.MOD_RMETA;
    
    private final BleHidManager bleHidManager;
    private final BleGattServerManager gattServerManager;
    private BluetoothGattService hidService;
    private BluetoothGattCharacteristic reportCharacteristic;
    private BluetoothGattCharacteristic protocolModeCharacteristic;
    
    private BluetoothDevice connectedDevice;
    private boolean isInitialized = false;
    private byte currentProtocolMode = HidConstants.Protocol.MODE_REPORT;
    
    private HidReportHandler reportHandler;
    
    /**
     * Creates a new HID Media Service.
     *
     * @param bleHidManager The BLE HID manager
     */
    public HidMediaService(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        this.gattServerManager = bleHidManager.getGattServerManager();
    }
    
    /**
     * Initializes the HID media service.
     *
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.w(TAG, "HID media service already initialized");
            return true;
        }
        
        if (gattServerManager == null) {
            Log.e(TAG, "GATT server manager not available");
            return false;
        }
        
        // Create HID service
        hidService = new BluetoothGattService(
                HidConstants.Uuids.HID_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        
        setupCharacteristics();
        
        // Add service to GATT server
        boolean success = gattServerManager.addHidService(hidService);
        
        if (success) {
            // Create the report handler
            reportHandler = new HidReportHandler(
                    gattServerManager, 
                    reportCharacteristic);
            
            isInitialized = true;
            Log.i(TAG, "HID media service initialized with standard HID descriptor");
        } else {
            Log.e(TAG, "Failed to initialize HID media service");
        }
        
        return success;
    }
    
    /**
     * Sets up all the GATT characteristics for the HID service.
     */
    private void setupCharacteristics() {
        // HID Information characteristic
        BluetoothGattCharacteristic hidInfoCharacteristic = new BluetoothGattCharacteristic(
                HidConstants.Uuids.HID_INFORMATION,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        hidInfoCharacteristic.setValue(HidConstants.Protocol.HID_INFORMATION);
        
        // Report Map characteristic
        BluetoothGattCharacteristic reportMapCharacteristic = new BluetoothGattCharacteristic(
                HidConstants.Uuids.HID_REPORT_MAP,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        reportMapCharacteristic.setValue(HidConstants.Combined.REPORT_MAP);
        
        // HID Control Point characteristic
        BluetoothGattCharacteristic hidControlCharacteristic = new BluetoothGattCharacteristic(
                HidConstants.Uuids.HID_CONTROL_POINT,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        
        // Protocol Mode characteristic
        protocolModeCharacteristic = new BluetoothGattCharacteristic(
                HidConstants.Uuids.HID_PROTOCOL_MODE,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        protocolModeCharacteristic.setValue(new byte[] { HidConstants.Protocol.MODE_REPORT });
        
        // Report characteristic (for media control reports)
        reportCharacteristic = setupReportCharacteristic();
        
        // Add characteristics to service
        hidService.addCharacteristic(hidInfoCharacteristic);
        hidService.addCharacteristic(reportMapCharacteristic);
        hidService.addCharacteristic(hidControlCharacteristic);
        hidService.addCharacteristic(protocolModeCharacteristic);
        hidService.addCharacteristic(reportCharacteristic);
    }
    
    /**
     * Sets up the Report characteristic.
     */
    private BluetoothGattCharacteristic setupReportCharacteristic() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HidConstants.Uuids.HID_REPORT,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        
        // Set initial report value for 12-byte combined format (media, mouse, keyboard)
        byte[] initialReport = new byte[12];
        characteristic.setValue(initialReport);
        
        // Add Report Reference descriptor to help hosts identify the report type
        // We're not using report IDs so value is {0, Input report type}
        BluetoothGattDescriptor reportRefDescriptor = new BluetoothGattDescriptor(
                HidConstants.Uuids.REPORT_REFERENCE,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        reportRefDescriptor.setValue(new byte[] { 0, 0x01 });  // No report ID, Input report
        characteristic.addDescriptor(reportRefDescriptor);
        
        // Add Client Characteristic Configuration Descriptor (CCCD) to enable notifications
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                HidConstants.Uuids.CLIENT_CONFIG,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
        characteristic.addDescriptor(descriptor);
        
        return characteristic;
    }
    
    // ==================== Public API ====================
    
    /**
     * Sends a combined media and mouse report.
     */
    public boolean sendCombinedReport(int mediaButtons, int mouseButtons, int x, int y) {
        if (!isInitialized) {
            Log.e(TAG, "HID service not initialized");
            return false;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.sendCombinedReport(connectedDevice, mediaButtons, mouseButtons, x, y);
    }
    
    /**
     * Sends a media control report.
     */
    public boolean sendMediaReport(int buttons) {
        if (!isInitialized) {
            Log.e(TAG, "HID service not initialized");
            return false;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.sendMediaReport(connectedDevice, buttons);
    }
    
    /**
     * Sends a mouse movement report.
     */
    public boolean movePointer(int x, int y) {
        Log.d(TAG, "HID movePointer ENTRY - x: " + x + ", y: " + y);
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        boolean result = reportHandler.movePointer(connectedDevice, x, y);
        Log.d(TAG, "HID movePointer EXIT - result: " + result);
        return result;
    }
    
    /**
     * Sends a mouse button press report.
     */
    public boolean pressButton(int button) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.sendMouseButtons(connectedDevice, button);
    }
    
    /**
     * Releases all mouse buttons.
     */
    public boolean releaseButtons() {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.sendMouseButtons(connectedDevice, 0);
    }
    
    /**
     * Releases a specific mouse button.
     * 
     * @param button The button to release (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean releaseButton(int button) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.releaseMouseButton(connectedDevice, button);
    }
    
    /**
     * Performs a click with the specified button.
     */
    public boolean click(int button) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.click(connectedDevice, button);
    }
    
    // ==================== Keyboard Methods ====================
    
    /**
     * Sends a key press.
     * 
     * @param keyCode The key code to press
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean sendKey(byte keyCode, int modifiers) {
        if (!isInitialized) {
            Log.e(TAG, "HID service not initialized");
            return false;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.sendKey(connectedDevice, keyCode, modifiers);
    }
    
    /**
     * Releases all currently pressed keys.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean releaseAllKeys() {
        if (!isInitialized) {
            Log.e(TAG, "HID service not initialized");
            return false;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.releaseKeys(connectedDevice);
    }
    
    /**
     * Sends multiple keys at once.
     * 
     * @param keyCodes Array of up to 6 key codes to send simultaneously
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean sendKeys(byte[] keyCodes, int modifiers) {
        if (!isInitialized) {
            Log.e(TAG, "HID service not initialized");
            return false;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.sendKeyboardReport(connectedDevice, modifiers, keyCodes);
    }
    
    /**
     * Performs a key press and release (types a key).
     * 
     * @param keyCode The key code to type
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean typeKey(byte keyCode, int modifiers) {
        if (!isInitialized) {
            Log.e(TAG, "HID service not initialized");
            return false;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.typeKey(connectedDevice, keyCode, modifiers);
    }
    
    /**
     * Types a string of text character by character.
     * 
     * @param text The text to type
     * @return true if all characters were sent successfully, false otherwise
     */
    public boolean typeText(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        
        boolean success = true;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            byte keyCode = 0;
            int modifiers = 0;
            
            // Convert ASCII character to HID key code and modifiers
            if (c >= 'a' && c <= 'z') {
                // Lowercase letters
                keyCode = (byte) (HidConstants.Keyboard.KEY_A + (c - 'a'));
            } else if (c >= 'A' && c <= 'Z') {
                // Uppercase letters (use shift modifier)
                keyCode = (byte) (HidConstants.Keyboard.KEY_A + (c - 'A'));
                modifiers = KEY_MOD_LSHIFT;
            } else if (c >= '1' && c <= '9') {
                // Numbers 1-9
                keyCode = (byte) (HidConstants.Keyboard.KEY_1 + (c - '1'));
            } else if (c == '0') {
                keyCode = HidConstants.Keyboard.KEY_0;
            } else if (c == ' ') {
                keyCode = HidConstants.Keyboard.KEY_SPACE;
            } else if (c == '\n' || c == '\r') {
                keyCode = HidConstants.Keyboard.KEY_ENTER;
            } else if (c == '\t') {
                keyCode = HidConstants.Keyboard.KEY_TAB;
            } else if (c == '.') {
                keyCode = HidConstants.Keyboard.KEY_PERIOD;
            } else if (c == ',') {
                keyCode = HidConstants.Keyboard.KEY_COMMA;
            } else if (c == '-') {
                keyCode = HidConstants.Keyboard.KEY_MINUS;
            } else if (c == '=') {
                keyCode = HidConstants.Keyboard.KEY_EQUALS;
            } else if (c == ';') {
                keyCode = HidConstants.Keyboard.KEY_SEMICOLON;
            } else if (c == '/') {
                keyCode = HidConstants.Keyboard.KEY_SLASH;
            } else if (c == '\\') {
                keyCode = HidConstants.Keyboard.KEY_BACKSLASH;
            } else if (c == '[') {
                keyCode = HidConstants.Keyboard.KEY_BRACKET_LEFT;
            } else if (c == ']') {
                keyCode = HidConstants.Keyboard.KEY_BRACKET_RIGHT;
            } else if (c == '\'') {
                keyCode = HidConstants.Keyboard.KEY_APOSTROPHE;
            } else if (c == '`') {
                keyCode = HidConstants.Keyboard.KEY_GRAVE;
            } else {
                // Skip unsupported characters
                continue;
            }
            
            boolean result = typeKey(keyCode, modifiers);
            if (!result) {
                success = false;
            }
            
            // Add a small delay between characters
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // Ignore
            }
        }
        
        return success;
    }
    
    // ==================== Media Control Methods ====================
    
    /**
     * Sends a play/pause control.
     */
    public boolean playPause() {
        return sendControlAction(BUTTON_PLAY_PAUSE);
    }
    
    /**
     * Sends a next track control.
     */
    public boolean nextTrack() {
        return sendControlAction(BUTTON_NEXT_TRACK);
    }
    
    /**
     * Sends a previous track control.
     */
    public boolean previousTrack() {
        return sendControlAction(BUTTON_PREVIOUS_TRACK);
    }
    
    /**
     * Sends a volume up control.
     */
    public boolean volumeUp() {
        return sendControlAction(BUTTON_VOLUME_UP);
    }
    
    /**
     * Sends a volume down control.
     */
    public boolean volumeDown() {
        return sendControlAction(BUTTON_VOLUME_DOWN);
    }
    
    /**
     * Sends a mute control.
     */
    public boolean mute() {
        return sendControlAction(BUTTON_MUTE);
    }
    
    /**
     * Helper method to send a control action (press and release).
     */
    private boolean sendControlAction(int button) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.sendMediaControlAction(connectedDevice, button);
    }
    
    /**
     * Gets the HID Report Map descriptor.
     */
    public byte[] getReportMap() {
        return HidConstants.Combined.REPORT_MAP;
    }
    
    /**
     * Sends initial empty reports to kickstart HID functionality.
     * This ensures HID capabilities are properly initialized on both new connections and reconnections.
     */
    public void sendInitialReports() {
        Log.i(TAG, "Sending initial HID reports to kickstart functionality");
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device for sending initial reports");
            return;
        }
        
        try {
            // 1. Send empty media report (all buttons released)
            sendMediaReport(0);
            
            // 2. Send empty mouse report (no buttons, no movement)
            reportHandler.sendMouseButtons(connectedDevice, 0);
            movePointer(0, 0);
            
            // 3. Send empty keyboard report (no keys pressed)
            reportHandler.releaseKeys(connectedDevice);
            
            // 4. Send a combined report with all values zeroed
            sendCombinedReport(0, 0, 0, 0);
            
            Log.i(TAG, "Initial HID reports sent successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error sending initial HID reports: " + e.getMessage(), e);
        }
    }
    
    // ==================== GATT Server Callbacks ====================
    
    /**
     * Handles characteristic read requests.
     */
    public byte[] handleCharacteristicRead(UUID charUuid, int offset) {
        if (charUuid.equals(HidConstants.Uuids.HID_REPORT)) {
            return handleReportRead(offset, reportHandler.getReport());
        } else if (charUuid.equals(HidConstants.Uuids.HID_PROTOCOL_MODE)) {
            // Return the current protocol mode
            return new byte[] { currentProtocolMode };
        }
        
        // For other characteristics use default handling
        return null;
    }
    
    /**
     * Helper method for handling report reads with offset.
     */
    private byte[] handleReportRead(int offset, byte[] report) {
        if (offset > report.length) {
            return null;
        }
        
        if (offset == 0) {
            return report;
        } else {
            byte[] offsetResponse = new byte[report.length - offset];
            System.arraycopy(report, offset, offsetResponse, 0, offsetResponse.length);
            return offsetResponse;
        }
    }
    
    /**
     * Handles characteristic write requests.
     */
    public boolean handleCharacteristicWrite(UUID charUuid, byte[] value) {
        if (charUuid.equals(HidConstants.Uuids.HID_REPORT)) {
            // Handle write to report characteristic if needed
            Log.d(TAG, "Received write to report characteristic: " + HidConstants.bytesToHex(value));
            return true;
        } else if (charUuid.equals(HidConstants.Uuids.HID_PROTOCOL_MODE)) {
            // Handle write to protocol mode characteristic
            if (value != null && value.length > 0) {
                byte newMode = value[0];
                if (newMode == HidConstants.Protocol.MODE_REPORT) {
                    Log.d(TAG, "Protocol mode set to Report Protocol");
                    currentProtocolMode = newMode;
                    protocolModeCharacteristic.setValue(new byte[] { currentProtocolMode });
                    
                    // Let the report handler know about the protocol mode change
                    if (reportHandler != null) {
                        reportHandler.setProtocolMode(newMode);
                    }
                    
                    return true;
                } else {
                    Log.w(TAG, "Invalid protocol mode value: " + newMode);
                }
            }
            return true;
        } else if (charUuid.equals(HidConstants.Uuids.HID_CONTROL_POINT)) {
            // Handle write to control point characteristic
            if (value != null && value.length > 0) {
                byte controlPoint = value[0];
                Log.d(TAG, "Control point value written: " + controlPoint);
                // Handle control point commands as needed
            }
            return true;
        }
        
        // Unhandled characteristic
        return false;
    }
    
    /**
     * Handles descriptor read requests.
     */
    public byte[] handleDescriptorRead(UUID descriptorUuid, UUID characteristicUuid) {
        if (descriptorUuid.equals(HidConstants.Uuids.REPORT_REFERENCE) && 
                characteristicUuid.equals(HidConstants.Uuids.HID_REPORT)) {
            // Report Reference descriptor for report characteristic (no report ID)
            return new byte[] { 0x00, 0x01 };  // No report ID, Input report
        }
        
        // Unhandled descriptor
        return null;
    }
    
    /**
     * Handles descriptor write requests.
     */
    public boolean handleDescriptorWrite(UUID descriptorUuid, UUID characteristicUuid, byte[] value) {
        if (descriptorUuid.equals(HidConstants.Uuids.CLIENT_CONFIG)) {
            if (value.length == 2) {
                boolean enabled = (value[0] == 0x01 && value[1] == 0x00);
                String state = enabled ? "enabled" : "disabled";
                
                if (characteristicUuid.equals(HidConstants.Uuids.HID_REPORT)) {
                    Log.d(TAG, "Notifications " + state + " for media report characteristic");
                    if (reportHandler != null) {
                        reportHandler.setNotificationsEnabled(characteristicUuid, enabled);
                    }
                    return true;
                }
            }
        }
        
        // Unhandled descriptor
        return false;
    }
    
    /**
     * Cleans up resources.
     */
    public void close() {
        isInitialized = false;
        Log.i(TAG, "HID media service closed");
    }
}
