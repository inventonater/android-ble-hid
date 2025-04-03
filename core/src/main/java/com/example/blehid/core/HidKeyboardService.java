package com.example.blehid.core;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

/**
 * Implements a BLE HID Keyboard service.
 * This class handles HID report descriptors, input/output reports, and keyboard functionality.
 */
public class HidKeyboardService {
    private static final String TAG = "HidKeyboardService";
    
    // Standard UUIDs for HID service and characteristics
    private static final UUID HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_INFO_UUID = UUID.fromString("00002a4a-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_REPORT_MAP_UUID = UUID.fromString("00002a4b-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_CONTROL_POINT_UUID = UUID.fromString("00002a4c-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_REPORT_UUID = UUID.fromString("00002a4d-0000-1000-8000-00805f9b34fb");
    
    // Descriptor UUIDs
    private static final UUID REPORT_REFERENCE_UUID = UUID.fromString("00002908-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    // Report IDs
    private static final byte KEYBOARD_REPORT_ID = 0x01;
    
    // Report types
    private static final byte REPORT_TYPE_INPUT = 0x01;
    private static final byte REPORT_TYPE_OUTPUT = 0x02;
    private static final byte REPORT_TYPE_FEATURE = 0x03;
    
    // HID Control Point values
    private static final byte CONTROL_POINT_SUSPEND = 0x00;
    private static final byte CONTROL_POINT_EXIT_SUSPEND = 0x01;
    
    // Report sizes - changed from 8 to 8 (original) or 7 (without report ID)
    private static final int KEYBOARD_REPORT_SIZE = 7; // No report ID in standard format
    
    // Keyboard modifier byte bit masks (byte 0 of report)
    private static final byte KEYBOARD_MODIFIER_LEFT_CTRL = (byte)0x01;
    private static final byte KEYBOARD_MODIFIER_LEFT_SHIFT = (byte)0x02;
    private static final byte KEYBOARD_MODIFIER_LEFT_ALT = (byte)0x04;
    private static final byte KEYBOARD_MODIFIER_LEFT_GUI = (byte)0x08;
    private static final byte KEYBOARD_MODIFIER_RIGHT_CTRL = (byte)0x10;
    private static final byte KEYBOARD_MODIFIER_RIGHT_SHIFT = (byte)0x20;
    private static final byte KEYBOARD_MODIFIER_RIGHT_ALT = (byte)0x40;
    private static final byte KEYBOARD_MODIFIER_RIGHT_GUI = (byte)0x80;
    
    // Maximum number of keys to report at once (6 is standard for boot protocol)
    private static final int MAX_KEYS = 6;
    
    private final BleHidManager bleHidManager;
    private BluetoothGattService hidService;
    private BluetoothGattCharacteristic inputReportCharacteristic;
    
    private boolean suspended = false;
    private byte keyboardState[] = new byte[KEYBOARD_REPORT_SIZE];
    
    /**
     * Creates a new HID Keyboard service.
     * 
     * @param bleHidManager The parent BLE HID manager
     */
    public HidKeyboardService(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        
        // Initialize keyboard report state (all zeros)
        Arrays.fill(keyboardState, (byte)0);
        // No report ID in standard format
    }
    
    /**
     * Initializes the HID service and registers it with the GATT server.
     * 
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        // Create the HID service with all required characteristics
        hidService = createHidService();
        
        // Add the service to the GATT server
        boolean success = bleHidManager.getGattServerManager().addHidService(hidService);
        
        if (success) {
            // Cache the input report characteristic for sending keyboard reports
            inputReportCharacteristic = hidService.getCharacteristic(HID_REPORT_UUID);
            Log.i(TAG, "HID Keyboard service initialized successfully");
        } else {
            Log.e(TAG, "Failed to initialize HID Keyboard service");
        }
        
        return success;
    }
    
    /**
     * Creates the HID GATT service with all required characteristics and descriptors.
     * 
     * @return The configured BluetoothGattService
     */
    private BluetoothGattService createHidService() {
        BluetoothGattService service = new BluetoothGattService(
                HID_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        
        // 1. HID Information characteristic (mandatory)
        BluetoothGattCharacteristic hidInfoChar = new BluetoothGattCharacteristic(
                HID_INFO_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        // HID info value: version 1.11, country code 0, flags bit 0 = not normally connectable
        hidInfoChar.setValue(new byte[] { 0x11, 0x01, 0x00, 0x01 });
        
        // 2. HID Report Map characteristic (mandatory)
        BluetoothGattCharacteristic reportMapChar = new BluetoothGattCharacteristic(
                HID_REPORT_MAP_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        reportMapChar.setValue(getReportMap());
        
        // 3. HID Control Point characteristic (mandatory)
        BluetoothGattCharacteristic controlPointChar = new BluetoothGattCharacteristic(
                HID_CONTROL_POINT_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        
        // 4. HID Report characteristic for keyboard input reports (mandatory)
        BluetoothGattCharacteristic inputReportChar = new BluetoothGattCharacteristic(
                HID_REPORT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ |
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        // Set initial state (all keys up) - without Report ID
        inputReportChar.setValue(new byte[] { 0, 0, 0, 0, 0, 0, 0 });
        
        // Add Client Characteristic Configuration descriptor for enabling notifications
        BluetoothGattDescriptor inputReportCccDesc = new BluetoothGattDescriptor(
                CLIENT_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        inputReportCccDesc.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        inputReportChar.addDescriptor(inputReportCccDesc);
        
        // We're not using Report Reference descriptors since we don't use report IDs in our standard descriptor
        // The standard HID keyboard format doesn't use report IDs
        
        // 5. HID Report characteristic for keyboard output reports (LED states)
        BluetoothGattCharacteristic outputReportChar = new BluetoothGattCharacteristic(
                HID_REPORT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ |
                BluetoothGattCharacteristic.PROPERTY_WRITE |
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ |
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        
        // We're not using Report Reference descriptors since we don't use report IDs in our standard descriptor
        
        // Add all characteristics to the service
        service.addCharacteristic(hidInfoChar);
        service.addCharacteristic(reportMapChar);
        service.addCharacteristic(controlPointChar);
        service.addCharacteristic(inputReportChar);
        service.addCharacteristic(outputReportChar);
        
        return service;
    }
    
    /**
     * Gets the HID report map (report descriptor) for a keyboard.
     * This descriptor defines the format of the HID reports.
     * 
     * @return The report map byte array
     */
    public byte[] getReportMap() {
        // More standard HID report descriptor for a keyboard
        return new byte[] {
            (byte) 0x05, 0x01,  // Usage Page (Generic Desktop)
            (byte) 0x09, 0x06,  // Usage (Keyboard)
            (byte) 0xA1, 0x01,  // Collection (Application)
            
            // Report ID is removed in this more standard format
            
            (byte) 0x05, 0x07,  //   Usage Page (Key Codes)
            (byte) 0x19, (byte) 0xE0, // Usage Min (224)
            (byte) 0x29, (byte) 0xE7, // Usage Max (231)
            (byte) 0x15, 0x00,  // Logical Min (0)
            (byte) 0x25, 0x01,  // Logical Max (1)
            (byte) 0x75, 0x01,  // Report Size (1)
            (byte) 0x95, 0x08,  // Report Count (8)
            (byte) 0x81, 0x02,  // Input (Data, Var, Abs) ; Modifier byte
            
            (byte) 0x95, 0x01,  // Report Count (1)
            (byte) 0x75, 0x08,  // Report Size (8)
            (byte) 0x81, 0x01,  // Input (Const) ; Reserved byte
            
            // LED output section is removed in this more standard format
            
            (byte) 0x95, 0x06,  // Report Count (6)
            (byte) 0x75, 0x08,  // Report Size (8)
            (byte) 0x15, 0x00,  // Logical Min (0)
            (byte) 0x25, 0x65,  // Logical Max (101) - Standard keys only, not 255
            (byte) 0x05, 0x07,  // Usage Page (Key codes)
            (byte) 0x19, 0x00,  // Usage Min (0)
            (byte) 0x29, 0x65,  // Usage Max (101) - Standard keys only, not 255
            (byte) 0x81, 0x00,  // Input (Data, Array)
            
            (byte) 0xC0         // End Collection
        };
    }
    
    /**
     * Sends a single key press.
     * 
     * @param keyCode The HID key code to send
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKey(int keyCode) {
        if (suspended) {
            Log.w(TAG, "Keyboard is suspended, cannot send key");
            return false;
        }
        
        // Reset report state
        Arrays.fill(keyboardState, (byte)0);
        
        // Set the key in the first key slot (index 1 when no report ID)
        keyboardState[1] = (byte)(keyCode & 0xFF);
        
        // Send the report
        return sendReport(keyboardState);
    }
    
    /**
     * Sends a key press with modifier keys.
     * 
     * @param keyCode The HID key code to send
     * @param modifiers Modifier byte (ctrl, shift, alt, gui)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeyWithModifiers(int keyCode, byte modifiers) {
        if (suspended) {
            Log.w(TAG, "Keyboard is suspended, cannot send key");
            return false;
        }
        
        // Reset report state
        Arrays.fill(keyboardState, (byte)0);
        
        // Set modifiers and key (index 0 for modifiers when no report ID)
        keyboardState[0] = modifiers;
        keyboardState[1] = (byte)(keyCode & 0xFF);
        
        // Send the report
        return sendReport(keyboardState);
    }
    
    /**
     * Sends multiple key presses simultaneously.
     * 
     * @param keyCodes Array of HID key codes to send
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeys(int[] keyCodes) {
        if (suspended) {
            Log.w(TAG, "Keyboard is suspended, cannot send keys");
            return false;
        }
        
        if (keyCodes == null || keyCodes.length == 0) {
            return releaseAllKeys();
        }
        
        // Reset report state
        Arrays.fill(keyboardState, (byte)0);
        
        // Add keys (up to 6) starting at index 1 (after modifier byte)
        int numKeys = Math.min(keyCodes.length, MAX_KEYS);
        for (int i = 0; i < numKeys; i++) {
            keyboardState[i + 1] = (byte)(keyCodes[i] & 0xFF);
        }
        
        // Send the report
        return sendReport(keyboardState);
    }
    
    /**
     * Sends key presses with modifier keys.
     * 
     * @param keyCodes Array of HID key codes to send
     * @param modifiers Modifier byte (ctrl, shift, alt, gui)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeysWithModifiers(int[] keyCodes, byte modifiers) {
        if (suspended) {
            Log.w(TAG, "Keyboard is suspended, cannot send keys");
            return false;
        }
        
        // Reset report state
        Arrays.fill(keyboardState, (byte)0);
        
        // Set modifiers at index 0
        keyboardState[0] = modifiers;
        
        if (keyCodes != null && keyCodes.length > 0) {
            // Add keys (up to 6) starting at index 1
            int numKeys = Math.min(keyCodes.length, MAX_KEYS);
            for (int i = 0; i < numKeys; i++) {
                keyboardState[i + 1] = (byte)(keyCodes[i] & 0xFF);
            }
        }
        
        // Send the report
        return sendReport(keyboardState);
    }
    
    /**
     * Releases all pressed keys.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseAllKeys() {
        if (suspended) {
            Log.w(TAG, "Keyboard is suspended, cannot release keys");
            return false;
        }
        
        // Reset report state (all zeros means no keys pressed)
        Arrays.fill(keyboardState, (byte)0);
        
        // Send the report
        return sendReport(keyboardState);
    }
    
    /**
     * Handles HID control point commands.
     * 
     * @param controlPoint The control point value
     */
    public void handleControlPoint(byte controlPoint) {
        switch (controlPoint) {
            case CONTROL_POINT_SUSPEND:
                Log.i(TAG, "HID Control Point: Suspend");
                suspended = true;
                break;
                
            case CONTROL_POINT_EXIT_SUSPEND:
                Log.i(TAG, "HID Control Point: Exit Suspend");
                suspended = false;
                break;
                
            default:
                Log.w(TAG, "Unknown HID Control Point value: " + controlPoint);
                break;
        }
    }
    
    /**
     * Handles read requests for characteristics.
     * 
     * @param charUuid UUID of the characteristic being read
     * @param offset The offset to read from
     * @return The characteristic value, or null if not handled
     */
    public byte[] handleCharacteristicRead(UUID charUuid, int offset) {
        // This is called by BleGattServerManager for characteristics not explicitly handled there
        return null;
    }
    
    /**
     * Handles write requests for characteristics.
     * 
     * @param charUuid UUID of the characteristic being written
     * @param value The value being written
     * @return true if handled successfully, false otherwise
     */
    public boolean handleCharacteristicWrite(UUID charUuid, byte[] value) {
        // Handle output report (LED status from host)
        if (charUuid.equals(HID_REPORT_UUID) && value.length > 0) {
            // Output report contains LED state (num lock, caps lock, etc.)
            // In standard format without Report ID, it's directly in first byte
            byte ledState = value[0];
            Log.d(TAG, "Received LED state: " + ledState);
            
            // Process LED state if needed
            // bit 0: Num Lock
            // bit 1: Caps Lock
            // bit 2: Scroll Lock
            // bit 3: Compose
            // bit 4: Kana
            return true;
        }
        
        return false;
    }
    
    /**
     * Handles read requests for descriptors.
     * 
     * @param descUuid UUID of the descriptor being read
     * @param charUuid UUID of the characteristic containing the descriptor
     * @return The descriptor value, or null if not handled
     */
    public byte[] handleDescriptorRead(UUID descUuid, UUID charUuid) {
        // We're not using Report Reference descriptors anymore
        // All descriptors are handled by BleGattServerManager
        return null;
    }
    
    /**
     * Handles write requests for descriptors.
     * 
     * @param descUuid UUID of the descriptor being written
     * @param charUuid UUID of the characteristic containing the descriptor
     * @param value The value being written
     * @return true if handled successfully, false otherwise
     */
    public boolean handleDescriptorWrite(UUID descUuid, UUID charUuid, byte[] value) {
        // Most descriptor writes are handled by BleGattServerManager
        return false;
    }
    
    /**
     * Sends a keyboard HID report.
     * 
     * @param report The report data to send
     * @return true if the report was sent successfully, false otherwise
     */
    private boolean sendReport(byte[] report) {
        if (inputReportCharacteristic == null) {
            Log.e(TAG, "Input report characteristic not initialized");
            return false;
        }
        
        // Log the report being sent for debugging
        StringBuilder reportHex = new StringBuilder();
        for (byte b : report) {
            reportHex.append(String.format("%02X ", b));
        }
        Log.d(TAG, "Sending keyboard report: " + reportHex.toString().trim());
        
        // Send notification for the input report characteristic
        return bleHidManager.getGattServerManager().sendNotification(
                inputReportCharacteristic.getUuid(), report);
    }
    
    /**
     * Gets the BluetoothGattService for the HID service.
     * 
     * @return The HID service
     */
    public BluetoothGattService getHidService() {
        return hidService;
    }
}
