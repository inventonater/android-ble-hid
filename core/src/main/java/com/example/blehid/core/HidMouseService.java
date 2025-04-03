package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

/**
 * Implements a BLE HID mouse service.
 * Based on the standard USB HID specification for mice.
 */
public class HidMouseService {
    private static final String TAG = "HidMouseService";
    
    // HID Service UUIDs
    private static final UUID HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_INFORMATION_UUID = UUID.fromString("00002A4A-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_CONTROL_POINT_UUID = UUID.fromString("00002A4C-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_REPORT_MAP_UUID = UUID.fromString("00002A4B-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_REPORT_UUID = UUID.fromString("00002A4D-0000-1000-8000-00805f9b34fb");
    
    // Client Characteristic Configuration Descriptor (CCCD) UUID
    private static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    // Report Reference Descriptor UUID
    private static final UUID REPORT_REFERENCE_UUID = UUID.fromString("00002908-0000-1000-8000-00805f9b34fb");
    
    // Report IDs
    private static final byte REPORT_ID_MOUSE = 0x01;
    
    // Standard Mouse Report Descriptor
    private static final byte[] REPORT_MAP = {
        (byte) 0x05, (byte) 0x01,        // USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x02,        // USAGE (Mouse)
        (byte) 0xA1, (byte) 0x01,        // COLLECTION (Application)
        (byte) 0x85, (byte) 0x01,        //   REPORT_ID (1)
        (byte) 0x09, (byte) 0x01,        //   USAGE (Pointer)
        (byte) 0xA1, (byte) 0x00,        //   COLLECTION (Physical)
        
        // Buttons (3 buttons)
        (byte) 0x05, (byte) 0x09,        //     USAGE_PAGE (Button)
        (byte) 0x19, (byte) 0x01,        //     USAGE_MINIMUM (Button 1)
        (byte) 0x29, (byte) 0x03,        //     USAGE_MAXIMUM (Button 3)
        (byte) 0x15, (byte) 0x00,        //     LOGICAL_MINIMUM (0)
        (byte) 0x25, (byte) 0x01,        //     LOGICAL_MAXIMUM (1)
        (byte) 0x95, (byte) 0x03,        //     REPORT_COUNT (3)
        (byte) 0x75, (byte) 0x01,        //     REPORT_SIZE (1)
        (byte) 0x81, (byte) 0x02,        //     INPUT (Data,Var,Abs)
        
        // Padding (5 bits)
        (byte) 0x95, (byte) 0x01,        //     REPORT_COUNT (1)
        (byte) 0x75, (byte) 0x05,        //     REPORT_SIZE (5) 
        (byte) 0x81, (byte) 0x03,        //     INPUT (Cnst,Var,Abs)
        
        // X and Y axis
        (byte) 0x05, (byte) 0x01,        //     USAGE_PAGE (Generic Desktop)
        (byte) 0x09, (byte) 0x30,        //     USAGE (X)
        (byte) 0x09, (byte) 0x31,        //     USAGE (Y)
        (byte) 0x15, (byte) 0x81,        //     LOGICAL_MINIMUM (-127)
        (byte) 0x25, (byte) 0x7F,        //     LOGICAL_MAXIMUM (127)
        (byte) 0x75, (byte) 0x08,        //     REPORT_SIZE (8)
        (byte) 0x95, (byte) 0x02,        //     REPORT_COUNT (2)
        (byte) 0x81, (byte) 0x06,        //     INPUT (Data,Var,Rel)
        
        // Vertical wheel
        (byte) 0x09, (byte) 0x38,        //     USAGE (Wheel)
        (byte) 0x15, (byte) 0x81,        //     LOGICAL_MINIMUM (-127)
        (byte) 0x25, (byte) 0x7F,        //     LOGICAL_MAXIMUM (127)
        (byte) 0x75, (byte) 0x08,        //     REPORT_SIZE (8)
        (byte) 0x95, (byte) 0x01,        //     REPORT_COUNT (1)
        (byte) 0x81, (byte) 0x06,        //     INPUT (Data,Var,Rel)
        
        (byte) 0xC0,                     //   END_COLLECTION
        (byte) 0xC0                      // END_COLLECTION
    };
    
    // Standard HID Information
    private static final byte[] HID_INFORMATION = {
        (byte) 0x11, (byte) 0x01,  // bcdHID (HID spec version 1.11)
        (byte) 0x00,               // bCountryCode (0 = not specified)
        (byte) 0x00                // Flags (0x00 = No special flags)
    };
    
    // Mouse report: [reportId, buttons, x, y, wheel]
    private static final byte[] mouseReport = new byte[5];
    
    private final BleHidManager bleHidManager;
    private final BleGattServerManager gattServerManager;
    private BluetoothGattService hidService;
    private BluetoothGattCharacteristic reportCharacteristic;
    
    private BluetoothDevice connectedDevice;
    private boolean isInitialized = false;
    
    // Button constants
    public static final int BUTTON_LEFT = 0x01;
    public static final int BUTTON_RIGHT = 0x02;
    public static final int BUTTON_MIDDLE = 0x04;
    
    /**
     * Creates a new HID Mouse Service.
     *
     * @param bleHidManager The BLE HID manager
     */
    public HidMouseService(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        this.gattServerManager = bleHidManager.getGattServerManager();
        
        // Initialize report buffer with report ID
        mouseReport[0] = REPORT_ID_MOUSE;
    }
    
    /**
     * Initializes the HID mouse service.
     *
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.w(TAG, "HID mouse service already initialized");
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
        
        // HID Information characteristic
        BluetoothGattCharacteristic hidInfoCharacteristic = new BluetoothGattCharacteristic(
                HID_INFORMATION_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        hidInfoCharacteristic.setValue(HID_INFORMATION);
        Log.d(TAG, "Set HID Information characteristic: " + bytesToHex(HID_INFORMATION));
        
        // Report Map characteristic
        BluetoothGattCharacteristic reportMapCharacteristic = new BluetoothGattCharacteristic(
                HID_REPORT_MAP_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ);
        reportMapCharacteristic.setValue(REPORT_MAP);
        Log.d(TAG, "Set Report Map characteristic (Standard HID)");
        
        // HID Control Point characteristic
        BluetoothGattCharacteristic hidControlCharacteristic = new BluetoothGattCharacteristic(
                HID_CONTROL_POINT_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        
        // Report characteristic (for mouse input reports)
        reportCharacteristic = new BluetoothGattCharacteristic(
                HID_REPORT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | 
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        
        // Set initial report value
        byte[] initialReport = new byte[5];  // [reportId, buttons, x, y, wheel]
        initialReport[0] = REPORT_ID_MOUSE;  // Set report ID
        reportCharacteristic.setValue(initialReport);
        
        // Add Report Reference descriptor to help hosts identify the report type
        BluetoothGattDescriptor reportRefDescriptor = new BluetoothGattDescriptor(
                REPORT_REFERENCE_UUID,
                BluetoothGattDescriptor.PERMISSION_READ);
        reportRefDescriptor.setValue(new byte[] { REPORT_ID_MOUSE, 0x01 });  // Report ID, Input report
        reportCharacteristic.addDescriptor(reportRefDescriptor);
        
        // Add Client Characteristic Configuration Descriptor (CCCD) to enable notifications
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CLIENT_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        reportCharacteristic.addDescriptor(descriptor);
        
        // Add characteristics to service
        hidService.addCharacteristic(hidInfoCharacteristic);
        hidService.addCharacteristic(reportMapCharacteristic);
        hidService.addCharacteristic(hidControlCharacteristic);
        hidService.addCharacteristic(reportCharacteristic);
        
        // No compatibility adjustments needed for standard HID
        
        // Add service to GATT server
        boolean success = gattServerManager.addHidService(hidService);
        
        if (success) {
            isInitialized = true;
            Log.i(TAG, "HID mouse service initialized with standard HID descriptor");
        } else {
            Log.e(TAG, "Failed to initialize HID mouse service");
        }
        
        return success;
    }
    
    /**
     * Sends a mouse movement report.
     * 
     * @param buttons Button state (bit 0: left, bit 1: right, bit 2: middle)
     * @param x X movement (-127 to 127)
     * @param y Y movement (-127 to 127)
     * @param wheel Wheel movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMouseReport(int buttons, int x, int y, int wheel) {
        Log.d(TAG, "sendMouseReport ENTRY - buttons: " + buttons + ", x: " + x + ", y: " + y + ", wheel: " + wheel);
        
        if (!isInitialized || reportCharacteristic == null) {
            Log.e(TAG, "HID service not initialized");
            return false;
        }
        
        // Simple check to ensure notifications are enabled
        if (!notificationsEnabled) {
            Log.d(TAG, "Ensuring notifications are enabled");
            enableNotifications();
            notificationsEnabled = true;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        // Clamp values to byte range
        x = Math.max(-127, Math.min(127, x));
        y = Math.max(-127, Math.min(127, y));
        wheel = Math.max(-127, Math.min(127, wheel));
        
        // Update report data
        mouseReport[1] = (byte) (buttons & 0x07);  // Buttons (3 bits)
        mouseReport[2] = (byte) x;                 // X movement
        mouseReport[3] = (byte) y;                 // Y movement
        mouseReport[4] = (byte) wheel;             // Wheel movement
        
        // Log more detailed report information
        Log.d(TAG, String.format("MOUSE REPORT DATA - X: %d (%02X), Y: %d (%02X), buttons=%d, wheel=%d",
                x, (byte)x & 0xFF, y, (byte)y & 0xFF, buttons, wheel));
        Log.d(TAG, String.format("MOUSE REPORT BYTES - [%02X, %02X, %02X, %02X, %02X]",
                mouseReport[0], mouseReport[1], mouseReport[2], mouseReport[3], mouseReport[4]));
        
        // Set report value
        reportCharacteristic.setValue(mouseReport);
        
        // Send notification with retry for more reliability
        boolean success = false;
        for (int retry = 0; retry < 2 && !success; retry++) {
            success = gattServerManager.sendNotification(
                    reportCharacteristic.getUuid(), mouseReport);
            
            if (!success && retry == 0) {
                Log.w(TAG, "First notification attempt failed, retrying after delay");
                try {
                    Thread.sleep(10); // Small delay before retry
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
        
        if (success) {
            Log.d(TAG, String.format("Mouse report sent: buttons=0x%02X, x=%d, y=%d, wheel=%d",
                    buttons, x, y, wheel));
        } else {
            Log.e(TAG, "Failed to send mouse report after retries");
        }
        
        return success;
    }
    
    /**
     * Sends a mouse button press report.
     * 
     * @param button The button(s) to press (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean pressButton(int button) {
        return sendMouseReport(button, 0, 0, 0);
    }
    
    /**
     * Releases all mouse buttons.
     * 
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseButtons() {
        return sendMouseReport(0, 0, 0, 0);
    }
    
    /**
     * Sends a mouse movement report with no buttons pressed.
     * 
     * @param x X movement (-127 to 127)
     * @param y Y movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean movePointer(int x, int y) {
        Log.d(TAG, "HID movePointer ENTRY - x: " + x + ", y: " + y);
        // Removed X-axis inversion to fix horizontal movement issues
        boolean result = sendMouseReport(0, x, y, 0);
        Log.d(TAG, "HID movePointer EXIT - result: " + result);
        return result;
    }
    
    /**
     * Sends a mouse wheel scroll report.
     * 
     * @param amount Scroll amount (-127 to 127, positive for up, negative for down)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean scroll(int amount) {
        return sendMouseReport(0, 0, 0, amount);
    }
    
    /**
     * Performs a click with the specified button.
     * 
     * @param button The button to click (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if both press and release were successful, false otherwise
     */
    public boolean click(int button) {
        boolean pressResult = pressButton(button);
        
        try {
            Thread.sleep(10); // Short delay between press and release
        } catch (InterruptedException e) {
            // Ignore
        }
        
        boolean releaseResult = releaseButtons();
        return pressResult && releaseResult;
    }
    
    /**
     * Gets the HID Report Map descriptor.
     * 
     * @return The report map byte array
     */
    public byte[] getReportMap() {
        return REPORT_MAP;
    }
    
    /**
     * Utility method to convert byte array to hex string for logging
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
    
    /**
     * Handles characteristic read requests.
     * 
     * @param charUuid The UUID of the characteristic being read
     * @param offset The offset into the characteristic value to start reading from
     * @return The characteristic value, or null if not handled
     */
    public byte[] handleCharacteristicRead(UUID charUuid, int offset) {
        if (charUuid.equals(HID_REPORT_UUID)) {
            // Return the last report
            if (offset > mouseReport.length) {
                return null;
            }
            
            if (offset == 0) {
                return mouseReport;
            } else {
                byte[] offsetResponse = new byte[mouseReport.length - offset];
                System.arraycopy(mouseReport, offset, offsetResponse, 0, offsetResponse.length);
                return offsetResponse;
            }
        }
        
        // For other characteristics use default handling
        return null;
    }
    
    /**
     * Checks if notifications are enabled for a descriptor.
     * 
     * @param descriptor The descriptor to check
     * @return True if notifications are enabled, false otherwise
     */
    // Simple flag to track if notifications have been enabled during this session
    private boolean notificationsEnabled = false;
    
    /**
     * Enables notifications for the report characteristic.
     * This is called internally if notifications aren't already enabled.
     */
    private void enableNotifications() {
        BluetoothGattDescriptor descriptor = reportCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor != null) {
            // Enable notifications (0x01, 0x00)
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d(TAG, "Set descriptor value to enable notifications");
            
            // Send a zero report to initialize the connection
            mouseReport[1] = 0; // No buttons
            mouseReport[2] = 0; // No X movement
            mouseReport[3] = 0; // No Y movement
            mouseReport[4] = 0; // No wheel
            reportCharacteristic.setValue(mouseReport);
            
            // Send two initial reports - one is sometimes not enough
            try {
                gattServerManager.sendNotification(reportCharacteristic.getUuid(), mouseReport);
                Thread.sleep(20); // Small delay
                gattServerManager.sendNotification(reportCharacteristic.getUuid(), mouseReport);
            } catch (Exception e) {
                Log.e(TAG, "Error sending initial report", e);
            }
        } else {
            Log.e(TAG, "Missing Client Configuration Descriptor (CCCD)");
        }
    }
    
    /**
     * Handles characteristic write requests.
     * 
     * @param charUuid The UUID of the characteristic being written
     * @param value The value being written
     * @return true if the write was handled, false otherwise
     */
    public boolean handleCharacteristicWrite(UUID charUuid, byte[] value) {
        if (charUuid.equals(HID_REPORT_UUID)) {
            // Handle write to report characteristic if needed
            Log.d(TAG, "Received write to report characteristic: " + bytesToHex(value));
            return true;
        }
        
        // Unhandled characteristic
        return false;
    }
    
    /**
     * Handles descriptor read requests.
     *
     * @param descriptorUuid The UUID of the descriptor being read
     * @param characteristicUuid The UUID of the characteristic the descriptor belongs to
     * @return The descriptor value, or null if not handled
     */
    public byte[] handleDescriptorRead(UUID descriptorUuid, UUID characteristicUuid) {
        if (descriptorUuid.equals(REPORT_REFERENCE_UUID) && characteristicUuid.equals(HID_REPORT_UUID)) {
            // Report Reference descriptor for report characteristic
            return new byte[] { REPORT_ID_MOUSE, 0x01 };  // Report ID, Input report
        }
        
        // Unhandled descriptor
        return null;
    }
    
    /**
     * Handles descriptor write requests.
     *
     * @param descriptorUuid The UUID of the descriptor being written
     * @param characteristicUuid The UUID of the characteristic the descriptor belongs to
     * @param value The value being written
     * @return true if the write was handled, false otherwise
     */
    public boolean handleDescriptorWrite(UUID descriptorUuid, UUID characteristicUuid, byte[] value) {
        if (descriptorUuid.equals(CLIENT_CONFIG_UUID) && characteristicUuid.equals(HID_REPORT_UUID)) {
            // Client Characteristic Configuration Descriptor for notifications
            if (value.length == 2) {
                if (value[0] == 0x01 && value[1] == 0x00) {
                    // Notifications enabled
                    Log.d(TAG, "Notifications enabled for mouse report characteristic");
                    notificationsEnabled = true;
                } else {
                    // Notifications disabled
                    Log.d(TAG, "Notifications disabled for mouse report characteristic");
                    notificationsEnabled = false;
                }
                return true;
            }
        }
        
        // Unhandled descriptor
        return false;
    }
    
    /**
     * Cleans up resources.
     */
    public void close() {
        if (gattServerManager != null && hidService != null) {
            // Just close the GATT server manager, which will handle removing all services
            // gattServerManager handles service removal internally
        }
        
        isInitialized = false;
        Log.i(TAG, "HID mouse service closed");
    }
}
