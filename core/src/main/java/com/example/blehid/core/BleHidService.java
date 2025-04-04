package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.example.blehid.core.handler.ConsumerReportHandler;
import com.example.blehid.core.handler.KeyboardReportHandler;
import com.example.blehid.core.handler.MouseReportHandler;
import com.example.blehid.core.manager.BleGattServiceRegistry;
import com.example.blehid.core.report.ReportRegistry;

import java.util.UUID;
import java.util.function.Function;

/**
 * Combined HID service providing mouse, keyboard, and consumer control functionality.
 * This is the main service for BLE HID peripheral implementation.
 */
public class BleHidService {
    private static final String TAG = "BleHidService";
    
    // Expose constants for backward compatibility
    public static final int BUTTON_LEFT = HidConstants.Mouse.BUTTON_LEFT;
    public static final int BUTTON_RIGHT = HidConstants.Mouse.BUTTON_RIGHT;
    public static final int BUTTON_MIDDLE = HidConstants.Mouse.BUTTON_MIDDLE;
    
    private final BleHidManager bleHidManager;
    private final BleGattServiceRegistry gattServerManager;
    private final BluetoothGattService hidService;
    private final BleNotifier notifier;
    private final String deviceName;
    
    // Report registry and handlers
    private final ReportRegistry reportRegistry = new ReportRegistry();
    private MouseReportHandler mouseReportHandler;
    private KeyboardReportHandler keyboardReportHandler;
    private ConsumerReportHandler consumerReportHandler;
    
    // Configuration
    private final boolean enableMouse;
    private final boolean enableKeyboard;
    private final boolean enableConsumer;
    
    // State
    private boolean isInitialized = false;
    private BluetoothDevice connectedDevice = null;
    private byte currentProtocolMode = HidConstants.PROTOCOL_MODE_REPORT;
    
    /**
     * Creates a new BLE HID Service.
     *
     * @param bleHidManager The BLE HID manager
     * @param gattServerManager The GATT service registry
     * @param hidService The HID service
     * @param notifier The BLE notifier for characteristic notifications
     * @param deviceName The device name
     * @param enableMouse Whether to enable mouse functionality
     * @param enableKeyboard Whether to enable keyboard functionality
     * @param enableConsumer Whether to enable consumer control functionality
     */
    public BleHidService(
            BleHidManager bleHidManager,
            BleGattServiceRegistry gattServerManager,
            BluetoothGattService hidService,
            BleNotifier notifier,
            String deviceName,
            boolean enableMouse,
            boolean enableKeyboard,
            boolean enableConsumer) {
        this.bleHidManager = bleHidManager;
        this.gattServerManager = gattServerManager;
        this.hidService = hidService;
        this.notifier = notifier;
        this.deviceName = deviceName;
        this.enableMouse = enableMouse;
        this.enableKeyboard = enableKeyboard;
        this.enableConsumer = enableConsumer;
    }
    
    /**
     * Initializes the HID service.
     *
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.w(TAG, "HID service already initialized");
            return true;
        }
        
        if (gattServerManager == null) {
            Log.e(TAG, "GATT server manager not available");
            return false;
        }
        
        // Add service to GATT server
        boolean success = gattServerManager.addService(hidService);
        
        if (success) {
            // Create report handlers
            setupReportHandlers();
            
            isInitialized = true;
            Log.i(TAG, "HID service initialized: " + 
                  "mouse=" + enableMouse + ", " + 
                  "keyboard=" + enableKeyboard + ", " + 
                  "consumer=" + enableConsumer);
        } else {
            Log.e(TAG, "Failed to add HID service to GATT server");
        }
        
        return success;
    }
    
    /**
     * Sets up report handlers based on configuration.
     */
    private void setupReportHandlers() {
        if (enableMouse) {
            setupMouseHandler();
        }
        
        if (enableKeyboard) {
            setupKeyboardHandler();
        }
        
        if (enableConsumer) {
            setupConsumerHandler();
        }
    }
    
    /**
     * Sets up the mouse report handler.
     */
    private void setupMouseHandler() {
        // Find characteristics for mouse reports
        BluetoothGattCharacteristic mouseReportChar = findCharacteristicByType(
                HidConstants.HID_REPORT_UUID, HidConstants.REPORT_ID_MOUSE);
        
        BluetoothGattCharacteristic bootMouseChar = findCharacteristicByUuid(
                HidConstants.Mouse.BOOT_MOUSE_INPUT_REPORT_UUID);
        
        if (mouseReportChar != null) {
            mouseReportHandler = new MouseReportHandler(
                    gattServerManager, notifier, mouseReportChar, bootMouseChar);
            
            reportRegistry.registerHandler(HidConstants.REPORT_ID_MOUSE, mouseReportHandler);
            Log.d(TAG, "Mouse report handler registered");
        } else {
            Log.e(TAG, "Missing mouse report characteristic");
        }
    }
    
    /**
     * Sets up the keyboard report handler.
     */
    private void setupKeyboardHandler() {
        // Find characteristic for keyboard reports
        BluetoothGattCharacteristic keyboardReportChar = findCharacteristicByType(
                HidConstants.HID_REPORT_UUID, HidConstants.REPORT_ID_KEYBOARD);
        
        if (keyboardReportChar != null) {
            keyboardReportHandler = new KeyboardReportHandler(
                    gattServerManager, notifier, keyboardReportChar);
            
            reportRegistry.registerHandler(HidConstants.REPORT_ID_KEYBOARD, keyboardReportHandler);
            Log.d(TAG, "Keyboard report handler registered");
        } else {
            Log.e(TAG, "Missing keyboard report characteristic");
        }
    }
    
    /**
     * Sets up the consumer report handler.
     */
    private void setupConsumerHandler() {
        // Find characteristic for consumer reports
        BluetoothGattCharacteristic consumerReportChar = findCharacteristicByType(
                HidConstants.HID_REPORT_UUID, HidConstants.REPORT_ID_CONSUMER);
        
        if (consumerReportChar != null) {
            consumerReportHandler = new ConsumerReportHandler(
                    gattServerManager, notifier, consumerReportChar);
            
            reportRegistry.registerHandler(HidConstants.REPORT_ID_CONSUMER, consumerReportHandler);
            Log.d(TAG, "Consumer report handler registered");
        } else {
            Log.e(TAG, "Missing consumer report characteristic");
        }
    }
    
    /**
     * Finds a characteristic by UUID.
     *
     * @param uuid The UUID to find
     * @return The characteristic, or null if not found
     */
    private BluetoothGattCharacteristic findCharacteristicByUuid(UUID uuid) {
        return hidService.getCharacteristic(uuid);
    }
    
    /**
     * Finds a report characteristic by its report type.
     *
     * @param uuid The characteristic UUID
     * @param reportId The report ID to find
     * @return The characteristic, or null if not found
     */
    private BluetoothGattCharacteristic findCharacteristicByType(UUID uuid, byte reportId) {
        for (BluetoothGattCharacteristic characteristic : hidService.getCharacteristics()) {
            if (characteristic.getUuid().equals(uuid)) {
                // Check if this characteristic has the right report ID
                BluetoothGattDescriptor reportRefDescriptor = characteristic.getDescriptor(
                        HidConstants.REPORT_REFERENCE_UUID);
                
                if (reportRefDescriptor != null && reportRefDescriptor.getValue() != null &&
                        reportRefDescriptor.getValue().length > 0) {
                    byte charReportId = reportRefDescriptor.getValue()[0];
                    if (charReportId == reportId) {
                        return characteristic;
                    }
                }
            }
        }
        return null;
    }
    
    /**
     * Executes an operation with a connected device.
     * This helper method centralizes connection validation.
     *
     * @param operation The operation to execute
     * @param defaultValue The default value to return if the device is not connected
     * @param <T> The return type of the operation
     * @return The result of the operation, or the default value if not connected
     */
    private <T> T withConnectedDevice(Function<BluetoothDevice, T> operation, T defaultValue) {
        if (!isInitialized) {
            Log.e(TAG, "HID service not initialized");
            return defaultValue;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return defaultValue;
        }
        
        return operation.apply(connectedDevice);
    }
    
    // --------------- MOUSE API ---------------
    
    /**
     * Sends a mouse movement report.
     */
    public boolean sendMouseReport(int buttons, int x, int y, int wheel) {
        return withConnectedDevice(
                device -> mouseReportHandler != null && 
                          mouseReportHandler.sendMouseReport(device, buttons, x, y, wheel),
                false);
    }
    
    /**
     * Sends a mouse button press report.
     */
    public boolean pressButton(int button) {
        return sendMouseReport(button, 0, 0, 0);
    }
    
    /**
     * Releases all mouse buttons.
     */
    public boolean releaseButtons() {
        return sendMouseReport(0, 0, 0, 0);
    }
    
    /**
     * Sends a mouse movement report with no buttons pressed.
     */
    public boolean movePointer(int x, int y) {
        Log.d(TAG, "HID movePointer ENTRY - x: " + x + ", y: " + y);
        boolean result = withConnectedDevice(
                device -> mouseReportHandler != null && 
                          mouseReportHandler.movePointer(device, x, y),
                false);
        Log.d(TAG, "HID movePointer EXIT - result: " + result);
        return result;
    }
    
    /**
     * Sends a mouse wheel scroll report.
     */
    public boolean scroll(int amount) {
        return withConnectedDevice(
                device -> mouseReportHandler != null && 
                          mouseReportHandler.scroll(device, amount),
                false);
    }
    
    /**
     * Performs a click with the specified button.
     */
    public boolean click(int button) {
        return withConnectedDevice(
                device -> mouseReportHandler != null && 
                          mouseReportHandler.click(device, button),
                false);
    }
    
    // --------------- KEYBOARD API ---------------
    
    /**
     * Sends a key press.
     * 
     * @param keyCode The key code to send (see HidConstants.Keyboard)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKey(byte keyCode) {
        return withConnectedDevice(
                device -> keyboardReportHandler != null && 
                          keyboardReportHandler.sendKey(device, keyCode),
                false);
    }
    
    /**
     * Sends a key press with modifiers.
     * 
     * @param keyCode The key code to send
     * @param modifiers The modifiers (shift, ctrl, etc.)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeyWithModifiers(byte keyCode, byte modifiers) {
        return withConnectedDevice(
                device -> keyboardReportHandler != null && 
                          keyboardReportHandler.sendKeyWithModifiers(device, keyCode, modifiers),
                false);
    }
    
    /**
     * Sends multiple key presses.
     * 
     * @param keyCodes Array of key codes to send (up to 6)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeys(byte[] keyCodes) {
        return withConnectedDevice(
                device -> keyboardReportHandler != null && 
                          keyboardReportHandler.sendKeys(device, keyCodes),
                false);
    }
    
    /**
     * Sends multiple key presses with modifiers.
     * 
     * @param keyCodes Array of key codes to send (up to 6)
     * @param modifiers The modifiers (shift, ctrl, etc.)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeysWithModifiers(byte[] keyCodes, byte modifiers) {
        return withConnectedDevice(
                device -> keyboardReportHandler != null && 
                          keyboardReportHandler.sendKeysWithModifiers(device, keyCodes, modifiers),
                false);
    }
    
    /**
     * Releases all keyboard keys.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseAllKeys() {
        return withConnectedDevice(
                device -> keyboardReportHandler != null && 
                          keyboardReportHandler.releaseAllKeys(device),
                false);
    }
    
    /**
     * Types a text string by sending key presses for each character.
     * 
     * @param text The text to type
     * @return true if all key reports were sent successfully, false otherwise
     */
    public boolean typeText(String text) {
        return withConnectedDevice(
                device -> keyboardReportHandler != null && 
                          keyboardReportHandler.typeText(device, text),
                false);
    }
    
    // --------------- CONSUMER CONTROL API ---------------
    
    /**
     * Sends a media play/pause control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPlayPause() {
        return withConnectedDevice(
                device -> consumerReportHandler != null && 
                          consumerReportHandler.sendPlayPause(device),
                false);
    }
    
    /**
     * Sends a media next track control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendNextTrack() {
        return withConnectedDevice(
                device -> consumerReportHandler != null && 
                          consumerReportHandler.sendNextTrack(device),
                false);
    }
    
    /**
     * Sends a media previous track control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPrevTrack() {
        return withConnectedDevice(
                device -> consumerReportHandler != null && 
                          consumerReportHandler.sendPrevTrack(device),
                false);
    }
    
    /**
     * Sends a media volume up control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeUp() {
        return withConnectedDevice(
                device -> consumerReportHandler != null && 
                          consumerReportHandler.sendVolumeUp(device),
                false);
    }
    
    /**
     * Sends a media volume down control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeDown() {
        return withConnectedDevice(
                device -> consumerReportHandler != null && 
                          consumerReportHandler.sendVolumeDown(device),
                false);
    }
    
    /**
     * Sends a media mute control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMute() {
        return withConnectedDevice(
                device -> consumerReportHandler != null && 
                          consumerReportHandler.sendMute(device),
                false);
    }
    
    /**
     * Sends a generic consumer control.
     * 
     * @param controlBit The control bit to set (see HidConstants.Consumer)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendConsumerControl(byte controlBit) {
        return withConnectedDevice(
                device -> consumerReportHandler != null && 
                          consumerReportHandler.sendConsumerControl(device, controlBit),
                false);
    }
    
    // --------------- GATT Server Callbacks ---------------
    
    /**
     * Gets the HID Report Map descriptor.
     */
    public byte[] getReportMap() {
        return HidConstants.REPORT_MAP;
    }
    
    /**
     * Handles characteristic read requests.
     */
    public byte[] handleCharacteristicRead(UUID charUuid, int offset) {
        if (charUuid.equals(HidConstants.HID_REPORT_UUID)) {
            // For Report UUID, try to determine which report based on the characteristic
            BluetoothGattCharacteristic characteristic = findCharacteristicByUuid(charUuid);
            if (characteristic != null) {
                // Get the report ID from the report reference descriptor
                BluetoothGattDescriptor reportRefDescriptor = characteristic.getDescriptor(
                        HidConstants.REPORT_REFERENCE_UUID);
                
                if (reportRefDescriptor != null && reportRefDescriptor.getValue() != null && 
                        reportRefDescriptor.getValue().length > 0) {
                    byte reportId = reportRefDescriptor.getValue()[0];
                    AbstractReportHandler<?> handler = reportRegistry.getHandler(reportId);
                    
                    if (handler != null) {
                        // Get the report data from the handler
                        byte[] report = null;
                        
                        if (reportId == HidConstants.REPORT_ID_MOUSE && mouseReportHandler != null) {
                            report = mouseReportHandler.getMouseReport();
                        } else if (reportId == HidConstants.REPORT_ID_KEYBOARD && keyboardReportHandler != null) {
                            report = keyboardReportHandler.getKeyboardReport();
                        } else if (reportId == HidConstants.REPORT_ID_CONSUMER && consumerReportHandler != null) {
                            report = consumerReportHandler.getConsumerReport();
                        }
                        
                        if (report != null) {
                            return handleReportRead(offset, report);
                        }
                    }
                }
            }
            
            // If we couldn't determine which report, default to mouse
            if (mouseReportHandler != null) {
                return handleReportRead(offset, mouseReportHandler.getMouseReport());
            }
        } else if (charUuid.equals(HidConstants.Mouse.BOOT_MOUSE_INPUT_REPORT_UUID)) {
            // Boot mouse report (doesn't use Report ID)
            if (mouseReportHandler != null) {
                return handleReportRead(offset, mouseReportHandler.getBootMouseReport());
            }
        } else if (charUuid.equals(HidConstants.HID_PROTOCOL_MODE_UUID)) {
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
        if (charUuid.equals(HidConstants.HID_REPORT_UUID)) {
            // For Report UUID, determine which report based on the report ID
            if (value != null && value.length > 0) {
                byte reportId = value[0];
                
                if (reportId == HidConstants.REPORT_ID_MOUSE) {
                    Log.d(TAG, "Received write to mouse report characteristic: " + 
                          HidConstants.bytesToHex(value));
                } else if (reportId == HidConstants.REPORT_ID_KEYBOARD) {
                    Log.d(TAG, "Received write to keyboard report characteristic: " + 
                          HidConstants.bytesToHex(value));
                } else if (reportId == HidConstants.REPORT_ID_CONSUMER) {
                    Log.d(TAG, "Received write to consumer report characteristic: " + 
                          HidConstants.bytesToHex(value));
                } else {
                    Log.d(TAG, "Received write to unknown report ID " + reportId + ": " + 
                          HidConstants.bytesToHex(value));
                }
            } else {
                Log.d(TAG, "Received empty write to report characteristic");
            }
            return true;
        } else if (charUuid.equals(HidConstants.HID_PROTOCOL_MODE_UUID)) {
            // Handle write to protocol mode characteristic
            if (value != null && value.length > 0) {
                byte newMode = value[0];
                if (newMode == HidConstants.PROTOCOL_MODE_BOOT || 
                        newMode == HidConstants.PROTOCOL_MODE_REPORT) {
                    Log.d(TAG, "Protocol mode changed to: " + 
                          (newMode == HidConstants.PROTOCOL_MODE_REPORT ? 
                           "Report Protocol" : "Boot Protocol"));
                    currentProtocolMode = newMode;
                    
                    // Update the protocol mode in the handlers
                    if (mouseReportHandler != null) {
                        mouseReportHandler.setProtocolMode(newMode);
                    }
                    if (keyboardReportHandler != null) {
                        keyboardReportHandler.setProtocolMode(newMode);
                    }
                    if (consumerReportHandler != null) {
                        consumerReportHandler.setProtocolMode(newMode);
                    }
                    
                    return true;
                } else {
                    Log.w(TAG, "Invalid protocol mode value: " + newMode);
                }
            }
            return true;
        } else if (charUuid.equals(HidConstants.HID_CONTROL_POINT_UUID)) {
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
        if (descriptorUuid.equals(HidConstants.REPORT_REFERENCE_UUID)) {
            if (characteristicUuid.equals(HidConstants.HID_REPORT_UUID)) {
                // Since we're using the same UUID for all report characteristics,
                // we need to figure out which one is being accessed
                
                // Try to get the actual characteristic instance and check its properties
                BluetoothGattCharacteristic characteristic = findCharacteristicByUuid(characteristicUuid);
                if (characteristic != null) {
                    // Get the Report Reference descriptor to see which report it is
                    BluetoothGattDescriptor reportRefDescriptor = characteristic.getDescriptor(
                            HidConstants.REPORT_REFERENCE_UUID);
                    if (reportRefDescriptor != null && reportRefDescriptor.getValue() != null && 
                            reportRefDescriptor.getValue().length > 0) {
                        // Return the descriptor value that was set when creating the characteristic
                        return reportRefDescriptor.getValue();
                    }
                    
                    // If we can't determine from the descriptor, check the value
                    byte[] value = characteristic.getValue();
                    if (value != null && value.length > 0) {
                        // First byte should be the report ID
                        byte reportId = value[0];
                        return new byte[] { reportId, 0x01 };  // Report ID, Input report
                    }
                }
                
                // Default to mouse report if we can't determine
                return new byte[] { HidConstants.REPORT_ID_MOUSE, 0x01 };  // Mouse report ID, Input report
            }
        }
        
        // Unhandled descriptor
        return null;
    }
    
    /**
     * Handles descriptor write requests.
     */
    public boolean handleDescriptorWrite(UUID descriptorUuid, UUID characteristicUuid, byte[] value) {
        if (descriptorUuid.equals(HidConstants.CLIENT_CONFIG_UUID)) {
            if (value.length == 2) {
                boolean enabled = (value[0] == 0x01 && value[1] == 0x00);
                String state = enabled ? "enabled" : "disabled";
                
                if (characteristicUuid.equals(HidConstants.HID_REPORT_UUID)) {
                    // For combined implementation, need to determine which report type
                    BluetoothGattCharacteristic characteristic = findCharacteristicByUuid(characteristicUuid);
                    if (characteristic != null) {
                        // Try to determine which report from descriptor
                        BluetoothGattDescriptor reportRefDescriptor = characteristic.getDescriptor(
                                HidConstants.REPORT_REFERENCE_UUID);
                        if (reportRefDescriptor != null && reportRefDescriptor.getValue() != null && 
                                reportRefDescriptor.getValue().length > 0) {
                            byte reportId = reportRefDescriptor.getValue()[0];
                            
                            if (reportId == HidConstants.REPORT_ID_MOUSE) {
                                Log.d(TAG, "Notifications " + state + " for mouse report characteristic");
                                if (mouseReportHandler != null) {
                                    mouseReportHandler.setNotificationsEnabled(characteristicUuid, enabled);
                                }
                            } else if (reportId == HidConstants.REPORT_ID_KEYBOARD) {
                                Log.d(TAG, "Notifications " + state + " for keyboard report characteristic");
                                if (keyboardReportHandler != null) {
                                    keyboardReportHandler.setNotificationsEnabled(enabled);
                                }
                            } else if (reportId == HidConstants.REPORT_ID_CONSUMER) {
                                Log.d(TAG, "Notifications " + state + " for consumer report characteristic");
                                if (consumerReportHandler != null) {
                                    consumerReportHandler.setNotificationsEnabled(enabled);
                                }
                            }
                            return true;
                        }
                        
                        // If we can't determine from the descriptor, check the value
                        byte[] charValue = characteristic.getValue();
                        if (charValue != null && charValue.length > 0) {
                            // First byte should be the report ID
                            byte reportId = charValue[0];
                            
                            if (reportId == HidConstants.REPORT_ID_MOUSE) {
                                Log.d(TAG, "Notifications " + state + " for mouse report characteristic");
                                if (mouseReportHandler != null) {
                                    mouseReportHandler.setNotificationsEnabled(characteristicUuid, enabled);
                                }
                            } else if (reportId == HidConstants.REPORT_ID_KEYBOARD) {
                                Log.d(TAG, "Notifications " + state + " for keyboard report characteristic");
                                if (keyboardReportHandler != null) {
                                    keyboardReportHandler.setNotificationsEnabled(enabled);
                                }
                            } else if (reportId == HidConstants.REPORT_ID_CONSUMER) {
                                Log.d(TAG, "Notifications " + state + " for consumer report characteristic");
                                if (consumerReportHandler != null) {
                                    consumerReportHandler.setNotificationsEnabled(enabled);
                                }
                            }
                            return true;
                        }
                    }
                } else if (characteristicUuid.equals(HidConstants.Mouse.BOOT_MOUSE_INPUT_REPORT_UUID)) {
                    Log.d(TAG, "Notifications " + state + " for boot mouse report characteristic");
                    if (mouseReportHandler != null) {
                        mouseReportHandler.setNotificationsEnabled(characteristicUuid, enabled);
                    }
                    return true;
                }
            }
        }
        
        // Unhandled descriptor
        return false;
    }
    
    /**
     * Gets the device name.
     *
     * @return The device name
     */
    public String getDeviceName() {
        return deviceName;
    }
    
    /**
     * Gets the report registry.
     *
     * @return The report registry
     */
    public ReportRegistry getReportRegistry() {
        return reportRegistry;
    }
    
    /**
     * Gets the mouse report handler.
     *
     * @return The mouse report handler, or null if not enabled
     */
    public MouseReportHandler getMouseReportHandler() {
        return mouseReportHandler;
    }
    
    /**
     * Gets the keyboard report handler.
     *
     * @return The keyboard report handler, or null if not enabled
     */
    public KeyboardReportHandler getKeyboardReportHandler() {
        return keyboardReportHandler;
    }
    
    /**
     * Gets the consumer report handler.
     *
     * @return The consumer report handler, or null if not enabled
     */
    public ConsumerReportHandler getConsumerReportHandler() {
        return consumerReportHandler;
    }
    
    /**
     * Cleans up resources.
     */
    public void close() {
        isInitialized = false;
        Log.i(TAG, "HID combined service closed");
    }
}
