package com.example.blehid.core.gatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.util.Log;

import com.example.blehid.core.HidConstants;

import java.util.UUID;

/**
 * Factory for creating GATT characteristics and descriptors.
 * Centralizes characteristic creation to ensure consistent configuration.
 */
public class CharacteristicFactory {
    private static final String TAG = "CharacteristicFactory";
    
    /**
     * Creates a HID information characteristic.
     *
     * @return The created characteristic
     */
    public static BluetoothGattCharacteristic createHidInfoCharacteristic() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HidConstants.HID_INFORMATION_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        characteristic.setValue(HidConstants.HID_INFORMATION);
        return characteristic;
    }
    
    /**
     * Creates a report map characteristic.
     *
     * @return The created characteristic
     */
    public static BluetoothGattCharacteristic createReportMapCharacteristic() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HidConstants.HID_REPORT_MAP_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        characteristic.setValue(HidConstants.REPORT_MAP);
        return characteristic;
    }
    
    /**
     * Creates a HID control point characteristic.
     *
     * @return The created characteristic
     */
    public static BluetoothGattCharacteristic createHidControlPointCharacteristic() {
        return new BluetoothGattCharacteristic(
                HidConstants.HID_CONTROL_POINT_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
    }
    
    /**
     * Creates a protocol mode characteristic.
     *
     * @param initialMode The initial protocol mode
     * @return The created characteristic
     */
    public static BluetoothGattCharacteristic createProtocolModeCharacteristic(byte initialMode) {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HidConstants.HID_PROTOCOL_MODE_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        characteristic.setValue(new byte[] { initialMode });
        return characteristic;
    }
    
    /**
     * Creates a boot mouse input report characteristic.
     *
     * @return The created characteristic
     */
    public static BluetoothGattCharacteristic createBootMouseInputReportCharacteristic() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                HidConstants.Mouse.BOOT_MOUSE_INPUT_REPORT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED);
        
        // Add CCCD to Boot Mouse Input Report
        BluetoothGattDescriptor bootMouseCccd = createClientConfigDescriptor();
        characteristic.addDescriptor(bootMouseCccd);
        
        // Set initial boot mouse report value (3 bytes: buttons, x, y)
        byte[] initialBootReport = new byte[3];
        characteristic.setValue(initialBootReport);
        
        return characteristic;
    }
    
    /**
     * Creates a report characteristic.
     *
     * @param reportId The report ID
     * @param reportType The report type (input, output, feature)
     * @param reportSize The size of the report in bytes (not including report ID)
     * @param allowWrite Whether to allow writes to the characteristic
     * @return The created characteristic
     */
    public static BluetoothGattCharacteristic createReportCharacteristic(
            byte reportId, byte reportType, int reportSize, boolean allowWrite) {
        
        int properties = BluetoothGattCharacteristic.PROPERTY_READ | 
                         BluetoothGattCharacteristic.PROPERTY_NOTIFY;
        int permissions = BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
        
        if (allowWrite) {
            properties |= BluetoothGattCharacteristic.PROPERTY_WRITE;
            permissions |= BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED;
        }
        
        // Create a unique UUID for each report characteristic for Android compatibility
        // All are based on HID_REPORT_UUID but with a slight variation based on report ID
        UUID uniqueReportUuid = new UUID(
                HidConstants.HID_REPORT_UUID.getMostSignificantBits(),
                HidConstants.HID_REPORT_UUID.getLeastSignificantBits() + reportId);
                
        Log.d(TAG, "Creating report characteristic with unique UUID: " + uniqueReportUuid + 
              " for report ID: " + reportId);
              
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(
                uniqueReportUuid,
                properties,
                permissions);
        
        // Set initial report value WITHOUT including report ID in the payload
        // For Android compatibility - report ID is only in the descriptor
        byte[] initialReport = new byte[reportSize];
        characteristic.setValue(initialReport);
        
        // Add Report Reference descriptor
        BluetoothGattDescriptor reportRefDescriptor = createReportReferenceDescriptor(reportId, reportType);
        characteristic.addDescriptor(reportRefDescriptor);
        
        // Add Client Characteristic Configuration Descriptor (CCCD)
        BluetoothGattDescriptor cccdDescriptor = createClientConfigDescriptor();
        characteristic.addDescriptor(cccdDescriptor);
        
        return characteristic;
    }
    
    /**
     * Creates a mouse report characteristic.
     *
     * @return The created characteristic
     */
    public static BluetoothGattCharacteristic createMouseReportCharacteristic() {
        // Mouse report: 4 bytes (buttons, x, y, wheel) plus report ID
        return createReportCharacteristic(
                HidConstants.REPORT_ID_MOUSE,
                (byte) 0x01, // Input report
                4, // 4 bytes - buttons, x, y, wheel
                true); // Allow writes
    }
    
    /**
     * Creates a keyboard report characteristic.
     *
     * @return The created characteristic
     */
    public static BluetoothGattCharacteristic createKeyboardReportCharacteristic() {
        // Keyboard report: 8 bytes (modifiers, reserved, 6 key codes) plus report ID
        return createReportCharacteristic(
                HidConstants.REPORT_ID_KEYBOARD,
                (byte) 0x01, // Input report
                HidConstants.Keyboard.KEYBOARD_REPORT_SIZE,
                false); // No writes needed
    }
    
    /**
     * Creates a consumer report characteristic.
     *
     * @return The created characteristic
     */
    public static BluetoothGattCharacteristic createConsumerReportCharacteristic() {
        // Consumer report: 1 byte (controls bitmap) plus report ID
        return createReportCharacteristic(
                HidConstants.REPORT_ID_CONSUMER,
                (byte) 0x01, // Input report
                HidConstants.Consumer.CONSUMER_REPORT_SIZE,
                false); // No writes needed
    }
    
    /**
     * Creates a client configuration descriptor.
     *
     * @return The created descriptor
     */
    public static BluetoothGattDescriptor createClientConfigDescriptor() {
        return new BluetoothGattDescriptor(
                HidConstants.CLIENT_CONFIG_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattDescriptor.PERMISSION_WRITE_ENCRYPTED);
    }
    
    /**
     * Creates a report reference descriptor.
     *
     * @param reportId The report ID
     * @param reportType The report type (input, output, feature)
     * @return The created descriptor
     */
    public static BluetoothGattDescriptor createReportReferenceDescriptor(byte reportId, byte reportType) {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                HidConstants.REPORT_REFERENCE_UUID,
                BluetoothGattDescriptor.PERMISSION_READ_ENCRYPTED);
        descriptor.setValue(new byte[] { reportId, reportType });
        return descriptor;
    }
}
