package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

import static com.example.blehid.core.HidMouseConstants.*;

/**
 * Combined HID service providing mouse, keyboard, and consumer control functionality.
 * This is the main service for BLE HID peripheral implementation.
 */
public class BleHidService {
    private static final String TAG = "BleHidService";
    
    // Mouse button constants (duplicated from HidMouseConstants for backward compatibility)
    public static final int BUTTON_LEFT = HidMouseConstants.BUTTON_LEFT;
    public static final int BUTTON_RIGHT = HidMouseConstants.BUTTON_RIGHT;
    public static final int BUTTON_MIDDLE = HidMouseConstants.BUTTON_MIDDLE;
    
    private final BleHidManager bleHidManager;
    private final BleGattServerManager gattServerManager;
    private BluetoothGattService hidService;
    
    // Mouse characteristics
    private BluetoothGattCharacteristic mouseReportCharacteristic;
    private BluetoothGattCharacteristic bootMouseInputReportCharacteristic;
    private BluetoothGattCharacteristic protocolModeCharacteristic;
    
    // Keyboard characteristic
    private BluetoothGattCharacteristic keyboardReportCharacteristic;
    
    // Consumer control characteristic
    private BluetoothGattCharacteristic consumerReportCharacteristic;
    
    private BluetoothDevice connectedDevice;
    private boolean isInitialized = false;
    private byte currentProtocolMode = PROTOCOL_MODE_REPORT;
    
    // Report handlers
    private HidMouseReportHandler mouseReportHandler;
    private HidKeyboardReportHandler keyboardReportHandler;
    private HidConsumerReportHandler consumerReportHandler;
    
    /**
     * Creates a new BLE HID Service.
     *
     * @param bleHidManager The BLE HID manager
     */
    public BleHidService(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        this.gattServerManager = bleHidManager.getGattServerManager();
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
        
        // Create HID service
        hidService = new BluetoothGattService(
                HID_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        
        setupCharacteristics();
        
        // Add service to GATT server
        boolean success = gattServerManager.addHidService(hidService);
        
        if (success) {
            // Create the report handlers
            mouseReportHandler = new HidMouseReportHandler(
                    gattServerManager, 
                    mouseReportCharacteristic, 
                    bootMouseInputReportCharacteristic);
            
            keyboardReportHandler = new HidKeyboardReportHandler(
                    gattServerManager,
                    keyboardReportCharacteristic);
                    
            consumerReportHandler = new HidConsumerReportHandler(
                    gattServerManager,
                    consumerReportCharacteristic);
            
            isInitialized = true;
            Log.i(TAG, "HID combined service initialized with mouse, keyboard, and consumer controls");
        } else {
            Log.e(TAG, "Failed to initialize HID service");
        }
        
        return success;
    }
    
    /**
     * Sets up all the GATT characteristics for the HID service.
     */
    private void setupCharacteristics() {
        // HID Information characteristic
        BluetoothGattCharacteristic hidInfoCharacteristic = new BluetoothGattCharacteristic(
                HID_INFORMATION_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        hidInfoCharacteristic.setValue(HID_INFORMATION);
        
        // Report Map characteristic
        BluetoothGattCharacteristic reportMapCharacteristic = new BluetoothGattCharacteristic(
                HID_REPORT_MAP_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        reportMapCharacteristic.setValue(REPORT_MAP);
        
        // HID Control Point characteristic
        BluetoothGattCharacteristic hidControlCharacteristic = new BluetoothGattCharacteristic(
                HID_CONTROL_POINT_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        
        // Protocol Mode characteristic
        protocolModeCharacteristic = new BluetoothGattCharacteristic(
                HID_PROTOCOL_MODE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        protocolModeCharacteristic.setValue(new byte[] { PROTOCOL_MODE_REPORT });
        
        // Boot Mouse Input Report characteristic
        bootMouseInputReportCharacteristic = setupBootMouseInputReportCharacteristic();
        
        // Mouse Report characteristic (for mouse input reports)
        mouseReportCharacteristic = setupMouseReportCharacteristic();
        
        // Keyboard Report characteristic
        keyboardReportCharacteristic = setupKeyboardReportCharacteristic();
        
        // Consumer Control Report characteristic
        consumerReportCharacteristic = setupConsumerReportCharacteristic();
        
        // Add characteristics to service
        hidService.addCharacteristic(hidInfoCharacteristic);
        hidService.addCharacteristic(reportMapCharacteristic);
        hidService.addCharacteristic(hidControlCharacteristic);
        hidService.addCharacteristic(protocolModeCharacteristic);
        hidService.addCharacteristic(bootMouseInputReportCharacteristic);
        hidService.addCharacteristic(mouseReportCharacteristic);
        hidService.addCharacteristic(keyboardReportCharacteristic);
        hidService.addCharacteristic(consumerReportCharacteristic);
    }
    
    /**
     * Sets up the Boot Mouse Input Report characteristic.
     */
    private BluetoothGattCharacteristic setupBootMouseInputReportCharacteristic() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HID_BOOT_MOUSE_INPUT_REPORT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        
        // Add CCCD to Boot Mouse Input Report
        BluetoothGattDescriptor bootMouseCccd = new BluetoothGattDescriptor(
                CLIENT_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
        characteristic.addDescriptor(bootMouseCccd);
        
        // Set initial boot mouse report value (3 bytes: buttons, x, y)
        byte[] initialBootReport = new byte[3];
        characteristic.setValue(initialBootReport);
        
        return characteristic;
    }
    
    /**
     * Sets up the Mouse Report characteristic.
     */
    private BluetoothGattCharacteristic setupMouseReportCharacteristic() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HID_REPORT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        
        // Set initial report value for 5-byte format [reportId, buttons, x, y, wheel]
        byte[] initialReport = new byte[5];
        initialReport[0] = REPORT_ID_MOUSE;  // Set report ID
        characteristic.setValue(initialReport);
        
        // Add Report Reference descriptor to help hosts identify the report type
        // Using Report ID 1 for Mouse
        BluetoothGattDescriptor reportRefDescriptor = new BluetoothGattDescriptor(
                REPORT_REFERENCE_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        reportRefDescriptor.setValue(new byte[] { REPORT_ID_MOUSE, 0x01 });  // Report ID 1, Input report
        characteristic.addDescriptor(reportRefDescriptor);
        
        // Add Client Characteristic Configuration Descriptor (CCCD) to enable notifications
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CLIENT_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
        characteristic.addDescriptor(descriptor);
        
        return characteristic;
    }
    
    /**
     * Sets up the Keyboard Report characteristic.
     */
    private BluetoothGattCharacteristic setupKeyboardReportCharacteristic() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HID_REPORT_UUID,  // Using standard UUID instead of custom UUID
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        
        // Set initial keyboard report value (with report ID at the beginning)
        byte[] initialReport = new byte[HidKeyboardConstants.KEYBOARD_REPORT_SIZE + 1];
        initialReport[0] = REPORT_ID_KEYBOARD;  // Set report ID
        characteristic.setValue(initialReport);
        
        // Add Report Reference descriptor to help hosts identify the report type
        // Using Report ID 2 for Keyboard
        BluetoothGattDescriptor reportRefDescriptor = new BluetoothGattDescriptor(
                REPORT_REFERENCE_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        reportRefDescriptor.setValue(new byte[] { REPORT_ID_KEYBOARD, 0x01 });  // Report ID 2, Input report
        characteristic.addDescriptor(reportRefDescriptor);
        
        // Add Client Characteristic Configuration Descriptor (CCCD) to enable notifications
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CLIENT_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
        characteristic.addDescriptor(descriptor);
        
        return characteristic;
    }
    
    /**
     * Sets up the Consumer Control Report characteristic.
     */
    private BluetoothGattCharacteristic setupConsumerReportCharacteristic() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HID_REPORT_UUID,  // Using standard UUID instead of custom UUID
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        
        // Set initial consumer report value (with report ID at the beginning)
        byte[] initialReport = new byte[HidConsumerConstants.CONSUMER_REPORT_SIZE + 1];
        initialReport[0] = REPORT_ID_CONSUMER;  // Set report ID
        characteristic.setValue(initialReport);
        
        // Add Report Reference descriptor to help hosts identify the report type
        // Using Report ID 3 for Consumer Control
        BluetoothGattDescriptor reportRefDescriptor = new BluetoothGattDescriptor(
                REPORT_REFERENCE_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        reportRefDescriptor.setValue(new byte[] { REPORT_ID_CONSUMER, 0x01 });  // Report ID 3, Input report
        characteristic.addDescriptor(reportRefDescriptor);
        
        // Add Client Characteristic Configuration Descriptor (CCCD) to enable notifications
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CLIENT_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
        characteristic.addDescriptor(descriptor);
        
        return characteristic;
    }
    
    // --------------- MOUSE API ---------------
    
    /**
     * Sends a mouse movement report.
     */
    public boolean sendMouseReport(int buttons, int x, int y, int wheel) {
        if (!isInitialized) {
            Log.e(TAG, "HID service not initialized");
            return false;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return mouseReportHandler.sendMouseReport(connectedDevice, buttons, x, y, wheel);
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
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        boolean result = mouseReportHandler.movePointer(connectedDevice, x, y);
        Log.d(TAG, "HID movePointer EXIT - result: " + result);
        return result;
    }
    
    /**
     * Sends a mouse wheel scroll report.
     */
    public boolean scroll(int amount) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return mouseReportHandler.scroll(connectedDevice, amount);
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
        
        return mouseReportHandler.click(connectedDevice, button);
    }
    
    // --------------- KEYBOARD API ---------------
    
    /**
     * Sends a key press.
     * 
     * @param keyCode The key code to send (see HidKeyboardConstants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKey(byte keyCode) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return keyboardReportHandler.sendKey(connectedDevice, keyCode);
    }
    
    /**
     * Sends a key press with modifiers.
     * 
     * @param keyCode The key code to send
     * @param modifiers The modifiers (shift, ctrl, etc.)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeyWithModifiers(byte keyCode, byte modifiers) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return keyboardReportHandler.sendKeyWithModifiers(connectedDevice, keyCode, modifiers);
    }
    
    /**
     * Sends multiple key presses.
     * 
     * @param keyCodes Array of key codes to send (up to 6)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeys(byte[] keyCodes) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return keyboardReportHandler.sendKeys(connectedDevice, keyCodes);
    }
    
    /**
     * Sends multiple key presses with modifiers.
     * 
     * @param keyCodes Array of key codes to send (up to 6)
     * @param modifiers The modifiers (shift, ctrl, etc.)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeysWithModifiers(byte[] keyCodes, byte modifiers) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return keyboardReportHandler.sendKeysWithModifiers(connectedDevice, keyCodes, modifiers);
    }
    
    /**
     * Releases all keyboard keys.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseAllKeys() {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return keyboardReportHandler.releaseAllKeys(connectedDevice);
    }
    
    // --------------- CONSUMER CONTROL API ---------------
    
    /**
     * Sends a media play/pause control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPlayPause() {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return consumerReportHandler.sendPlayPause(connectedDevice);
    }
    
    /**
     * Sends a media next track control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendNextTrack() {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return consumerReportHandler.sendNextTrack(connectedDevice);
    }
    
    /**
     * Sends a media previous track control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPrevTrack() {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return consumerReportHandler.sendPrevTrack(connectedDevice);
    }
    
    /**
     * Sends a media volume up control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeUp() {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return consumerReportHandler.sendVolumeUp(connectedDevice);
    }
    
    /**
     * Sends a media volume down control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeDown() {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return consumerReportHandler.sendVolumeDown(connectedDevice);
    }
    
    /**
     * Sends a media mute control.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMute() {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return consumerReportHandler.sendMute(connectedDevice);
    }
    
    /**
     * Sends a generic consumer control.
     * 
     * @param controlBit The control bit to set (see HidConsumerConstants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendConsumerControl(byte controlBit) {
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return consumerReportHandler.sendConsumerControl(connectedDevice, controlBit);
    }
    
    // --------------- GATT Server Callbacks ---------------
    
    /**
     * Gets the HID Report Map descriptor.
     */
    public byte[] getReportMap() {
        return REPORT_MAP;
    }
    
    /**
     * Handles characteristic read requests.
     */
    public byte[] handleCharacteristicRead(UUID charUuid, int offset) {
        if (charUuid.equals(HID_REPORT_UUID)) {
            // For Report Protocol mode, we need to determine which report is being accessed
            // This would typically be determined by the Report Reference descriptor,
            // but we can check the Report ID in the report data directly
            
            // First try to get the report reference descriptor value from the characteristic if available
            BluetoothGattCharacteristic characteristic = hidService.getCharacteristic(charUuid);
            if (characteristic != null) {
                BluetoothGattDescriptor reportRefDescriptor = characteristic.getDescriptor(REPORT_REFERENCE_UUID);
                if (reportRefDescriptor != null && reportRefDescriptor.getValue() != null && reportRefDescriptor.getValue().length > 0) {
                    byte reportId = reportRefDescriptor.getValue()[0];
                    
                    if (reportId == REPORT_ID_MOUSE) {
                        return handleReportRead(offset, mouseReportHandler.getMouseReport());
                    } else if (reportId == REPORT_ID_KEYBOARD) {
                        return handleReportRead(offset, keyboardReportHandler.getKeyboardReport());
                    } else if (reportId == REPORT_ID_CONSUMER) {
                        return handleReportRead(offset, consumerReportHandler.getConsumerReport());
                    }
                }
            }
            
            // Fallback: Return the mouse report by default
            return handleReportRead(offset, mouseReportHandler.getMouseReport());
        } else if (charUuid.equals(HID_BOOT_MOUSE_INPUT_REPORT_UUID)) {
            // Boot mouse report (doesn't use Report ID)
            return handleReportRead(offset, mouseReportHandler.getBootMouseReport());
        } else if (charUuid.equals(HID_PROTOCOL_MODE_UUID)) {
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
        if (charUuid.equals(HID_REPORT_UUID)) {
            // For Report UUID, determine which report based on the report ID
            if (value != null && value.length > 0) {
                byte reportId = value[0];
                
                if (reportId == REPORT_ID_MOUSE) {
                    Log.d(TAG, "Received write to mouse report characteristic: " + HidMouseConstants.bytesToHex(value));
                } else if (reportId == REPORT_ID_KEYBOARD) {
                    Log.d(TAG, "Received write to keyboard report characteristic: " + HidMouseConstants.bytesToHex(value));
                } else if (reportId == REPORT_ID_CONSUMER) {
                    Log.d(TAG, "Received write to consumer report characteristic: " + HidMouseConstants.bytesToHex(value));
                } else {
                    Log.d(TAG, "Received write to unknown report ID " + reportId + ": " + HidMouseConstants.bytesToHex(value));
                }
            } else {
                Log.d(TAG, "Received empty write to report characteristic");
            }
            return true;
        } else if (charUuid.equals(HID_PROTOCOL_MODE_UUID)) {
            // Handle write to protocol mode characteristic
            if (value != null && value.length > 0) {
                byte newMode = value[0];
                if (newMode == PROTOCOL_MODE_BOOT || newMode == PROTOCOL_MODE_REPORT) {
                    Log.d(TAG, "Protocol mode changed to: " + 
                          (newMode == PROTOCOL_MODE_REPORT ? "Report Protocol" : "Boot Protocol"));
                    currentProtocolMode = newMode;
                    protocolModeCharacteristic.setValue(new byte[] { currentProtocolMode });
                    
                    // Update the report handler
                    if (mouseReportHandler != null) {
                        mouseReportHandler.setProtocolMode(newMode);
                    }
                    
                    return true;
                } else {
                    Log.w(TAG, "Invalid protocol mode value: " + newMode);
                }
            }
            return true;
        } else if (charUuid.equals(HID_CONTROL_POINT_UUID)) {
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
        if (descriptorUuid.equals(REPORT_REFERENCE_UUID)) {
            if (characteristicUuid.equals(HID_REPORT_UUID)) {
                // Since we're now using the same UUID for all report characteristics,
                // we need to figure out which one is being accessed
                
                // Try to get the actual characteristic instance and check its properties
                BluetoothGattCharacteristic characteristic = hidService.getCharacteristic(characteristicUuid);
                if (characteristic != null) {
                    // Get the Report Reference descriptor to see which report it is
                    BluetoothGattDescriptor reportRefDescriptor = characteristic.getDescriptor(REPORT_REFERENCE_UUID);
                    if (reportRefDescriptor != null && reportRefDescriptor.getValue() != null && reportRefDescriptor.getValue().length > 0) {
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
                return new byte[] { REPORT_ID_MOUSE, 0x01 };  // Mouse report ID, Input report
            }
        }
        
        // Unhandled descriptor
        return null;
    }
    
    /**
     * Handles descriptor write requests.
     */
    public boolean handleDescriptorWrite(UUID descriptorUuid, UUID characteristicUuid, byte[] value) {
        if (descriptorUuid.equals(CLIENT_CONFIG_UUID)) {
            if (value.length == 2) {
                boolean enabled = (value[0] == 0x01 && value[1] == 0x00);
                String state = enabled ? "enabled" : "disabled";
                
                if (characteristicUuid.equals(HID_REPORT_UUID)) {
                    // For our combined implementation, need to determine which report type
                    // Try to identify which report characteristic this is
                    BluetoothGattCharacteristic characteristic = hidService.getCharacteristic(characteristicUuid);
                    if (characteristic != null) {
                        // Check if we can determine which report from descriptor
                        BluetoothGattDescriptor reportRefDescriptor = characteristic.getDescriptor(REPORT_REFERENCE_UUID);
                        if (reportRefDescriptor != null && reportRefDescriptor.getValue() != null && reportRefDescriptor.getValue().length > 0) {
                            byte reportId = reportRefDescriptor.getValue()[0];
                            
                            if (reportId == REPORT_ID_MOUSE) {
                                Log.d(TAG, "Notifications " + state + " for mouse report characteristic");
                                if (mouseReportHandler != null) {
                                    mouseReportHandler.setNotificationsEnabled(characteristicUuid, enabled);
                                }
                            } else if (reportId == REPORT_ID_KEYBOARD) {
                                Log.d(TAG, "Notifications " + state + " for keyboard report characteristic");
                                if (keyboardReportHandler != null) {
                                    keyboardReportHandler.setNotificationsEnabled(enabled);
                                }
                            } else if (reportId == REPORT_ID_CONSUMER) {
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
                            
                            if (reportId == REPORT_ID_MOUSE) {
                                Log.d(TAG, "Notifications " + state + " for mouse report characteristic");
                                if (mouseReportHandler != null) {
                                    mouseReportHandler.setNotificationsEnabled(characteristicUuid, enabled);
                                }
                            } else if (reportId == REPORT_ID_KEYBOARD) {
                                Log.d(TAG, "Notifications " + state + " for keyboard report characteristic");
                                if (keyboardReportHandler != null) {
                                    keyboardReportHandler.setNotificationsEnabled(enabled);
                                }
                            } else if (reportId == REPORT_ID_CONSUMER) {
                                Log.d(TAG, "Notifications " + state + " for consumer report characteristic");
                                if (consumerReportHandler != null) {
                                    consumerReportHandler.setNotificationsEnabled(enabled);
                                }
                            }
                            return true;
                        }
                    }
                } else if (characteristicUuid.equals(HID_BOOT_MOUSE_INPUT_REPORT_UUID)) {
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
     * Cleans up resources.
     */
    public void close() {
        isInitialized = false;
        Log.i(TAG, "HID combined service closed");
    }
}
