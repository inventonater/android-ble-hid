package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import java.util.UUID;

import static com.example.blehid.core.HidMediaConstants.*;

/**
 * Implements a BLE HID media player service.
 * Based on the HID consumer control specification for media players.
 */
public class HidMediaService {
    private static final String TAG = "HidMediaService";
    
    // Media control button constants (duplicated from HidMediaConstants for convenience)
    public static final int BUTTON_PLAY_PAUSE = HidMediaConstants.BUTTON_PLAY_PAUSE;
    public static final int BUTTON_NEXT_TRACK = HidMediaConstants.BUTTON_NEXT_TRACK;
    public static final int BUTTON_PREVIOUS_TRACK = HidMediaConstants.BUTTON_PREVIOUS_TRACK;
    public static final int BUTTON_VOLUME_UP = HidMediaConstants.BUTTON_VOLUME_UP;
    public static final int BUTTON_VOLUME_DOWN = HidMediaConstants.BUTTON_VOLUME_DOWN;
    public static final int BUTTON_MUTE = HidMediaConstants.BUTTON_MUTE;
    
    private final BleHidManager bleHidManager;
    private final BleGattServerManager gattServerManager;
    private BluetoothGattService hidService;
    private BluetoothGattCharacteristic reportCharacteristic;
    private BluetoothGattCharacteristic protocolModeCharacteristic;
    
    private BluetoothDevice connectedDevice;
    private boolean isInitialized = false;
    private byte currentProtocolMode = PROTOCOL_MODE_REPORT;
    
    private HidMediaReportHandler reportHandler;
    
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
                HID_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        
        setupCharacteristics();
        
        // Add service to GATT server
        boolean success = gattServerManager.addHidService(hidService);
        
        if (success) {
            // Create the report handler
            reportHandler = new HidMediaReportHandler(
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
                HID_REPORT_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ | 
                BluetoothGattCharacteristic.PROPERTY_NOTIFY |
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED | 
                BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED);
        
        // Set initial report value for 1-byte format (buttons)
        byte[] initialReport = new byte[1];
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
        return REPORT_MAP;
    }
    
    // --------------- GATT Server Callbacks ---------------
    
    /**
     * Handles characteristic read requests.
     */
    public byte[] handleCharacteristicRead(UUID charUuid, int offset) {
        if (charUuid.equals(HID_REPORT_UUID)) {
            return handleReportRead(offset, reportHandler.getMediaReport());
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
            Log.d(TAG, "Received write to report characteristic: " + HidMediaConstants.bytesToHex(value));
            return true;
        } else if (charUuid.equals(HID_PROTOCOL_MODE_UUID)) {
            // Handle write to protocol mode characteristic
            if (value != null && value.length > 0) {
                byte newMode = value[0];
                if (newMode == PROTOCOL_MODE_REPORT) {
                    Log.d(TAG, "Protocol mode set to Report Protocol");
                    currentProtocolMode = newMode;
                    protocolModeCharacteristic.setValue(new byte[] { currentProtocolMode });
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
