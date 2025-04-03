package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

import static com.example.blehid.core.HidMouseConstants.*;

/**
 * Implements a BLE HID mouse service.
 * Based on the standard USB HID specification for mice.
 */
public class HidMouseService {
    private static final String TAG = "HidMouseService";
    
    // Mouse button constants (duplicated from HidMouseConstants for backward compatibility)
    public static final int BUTTON_LEFT = HidMouseConstants.BUTTON_LEFT;
    public static final int BUTTON_RIGHT = HidMouseConstants.BUTTON_RIGHT;
    public static final int BUTTON_MIDDLE = HidMouseConstants.BUTTON_MIDDLE;
    
    private final BleHidManager bleHidManager;
    private final BleGattServerManager gattServerManager;
    private BluetoothGattService hidService;
    private BluetoothGattCharacteristic reportCharacteristic;
    private BluetoothGattCharacteristic bootMouseInputReportCharacteristic;
    private BluetoothGattCharacteristic protocolModeCharacteristic;
    
    private BluetoothDevice connectedDevice;
    private boolean isInitialized = false;
    private byte currentProtocolMode = PROTOCOL_MODE_REPORT;
    
    private HidMouseReportHandler reportHandler;
    
    /**
     * Creates a new HID Mouse Service.
     *
     * @param bleHidManager The BLE HID manager
     */
    public HidMouseService(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        this.gattServerManager = bleHidManager.getGattServerManager();
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
        
        setupCharacteristics();
        
        // Add service to GATT server
        boolean success = gattServerManager.addHidService(hidService);
        
        if (success) {
            // Create the report handler
            reportHandler = new HidMouseReportHandler(
                    gattServerManager, 
                    reportCharacteristic, 
                    bootMouseInputReportCharacteristic);
            
            isInitialized = true;
            Log.i(TAG, "HID mouse service initialized with standard HID descriptor");
        } else {
            Log.e(TAG, "Failed to initialize HID mouse service");
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
        
        // Report characteristic (for mouse input reports)
        reportCharacteristic = setupReportCharacteristic();
        
        // Add characteristics to service
        hidService.addCharacteristic(hidInfoCharacteristic);
        hidService.addCharacteristic(reportMapCharacteristic);
        hidService.addCharacteristic(hidControlCharacteristic);
        hidService.addCharacteristic(protocolModeCharacteristic);
        hidService.addCharacteristic(bootMouseInputReportCharacteristic);
        hidService.addCharacteristic(reportCharacteristic);
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
     * Sets up the Report characteristic.
     */
    private BluetoothGattCharacteristic setupReportCharacteristic() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HID_REPORT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        
        // Set initial report value for 3-byte format [buttons, x, y]
        byte[] initialReport = new byte[3];
        characteristic.setValue(initialReport);
        
        // Add Report Reference descriptor to help hosts identify the report type
        // We're not using report IDs so value is {0, Input report type}
        BluetoothGattDescriptor reportRefDescriptor = new BluetoothGattDescriptor(
                REPORT_REFERENCE_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        reportRefDescriptor.setValue(new byte[] { 0, 0x01 });  // No report ID, Input report
        characteristic.addDescriptor(reportRefDescriptor);
        
        // Add Client Characteristic Configuration Descriptor (CCCD) to enable notifications
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                CLIENT_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
        characteristic.addDescriptor(descriptor);
        
        return characteristic;
    }
    
    // --------------- Public API ---------------
    
    /**
     * Sends a mouse movement report.
     */
    public boolean sendMouseReport(int buttons, int x, int y) {
        if (!isInitialized) {
            Log.e(TAG, "HID service not initialized");
            return false;
        }
        
        connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        return reportHandler.sendMouseReport(connectedDevice, buttons, x, y);
    }
    
    /**
     * Sends a mouse button press report.
     */
    public boolean pressButton(int button) {
        return sendMouseReport(button, 0, 0);
    }
    
    /**
     * Releases all mouse buttons.
     */
    public boolean releaseButtons() {
        return sendMouseReport(0, 0, 0);
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
        
        boolean result = reportHandler.movePointer(connectedDevice, x, y);
        Log.d(TAG, "HID movePointer EXIT - result: " + result);
        return result;
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
    
    /**
     * Gets the HID Report Map descriptor.
     */
    public byte[] getReportMap() {
        return REPORT_MAP;
    }
    
    // --------------- GATT Server Callbacks ---------------
    
    /**
     * Handles characteristic read requests.
     */
    public byte[] handleCharacteristicRead(UUID charUuid, int offset) {
        if (charUuid.equals(HID_REPORT_UUID)) {
            return handleReportRead(offset, reportHandler.getMouseReport());
        } else if (charUuid.equals(HID_BOOT_MOUSE_INPUT_REPORT_UUID)) {
            return handleReportRead(offset, reportHandler.getBootMouseReport());
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
            // Handle write to report characteristic if needed
            Log.d(TAG, "Received write to report characteristic: " + HidMouseConstants.bytesToHex(value));
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
                    if (reportHandler != null) {
                        reportHandler.setProtocolMode(newMode);
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
        if (descriptorUuid.equals(REPORT_REFERENCE_UUID) && characteristicUuid.equals(HID_REPORT_UUID)) {
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
        if (descriptorUuid.equals(CLIENT_CONFIG_UUID)) {
            if (value.length == 2) {
                boolean enabled = (value[0] == 0x01 && value[1] == 0x00);
                String state = enabled ? "enabled" : "disabled";
                
                if (characteristicUuid.equals(HID_REPORT_UUID)) {
                    Log.d(TAG, "Notifications " + state + " for mouse report characteristic");
                    if (reportHandler != null) {
                        reportHandler.setNotificationsEnabled(characteristicUuid, enabled);
                    }
                    return true;
                } else if (characteristicUuid.equals(HID_BOOT_MOUSE_INPUT_REPORT_UUID)) {
                    Log.d(TAG, "Notifications " + state + " for boot mouse report characteristic");
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
        Log.i(TAG, "HID mouse service closed");
    }
}
