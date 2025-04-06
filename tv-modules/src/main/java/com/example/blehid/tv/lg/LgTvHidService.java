package com.example.blehid.tv.lg;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.example.blehid.core.BleGattServerManager;
import com.example.blehid.tv.BaseTvHidService;
import com.example.blehid.tv.TvHidService;

import static com.example.blehid.tv.lg.LgTvHidConstants.*;

/**
 * LG Smart TV-specific implementation of the TvHidService.
 * Uses a minimal 3-byte report format optimized for LG Smart TVs.
 */
public class LgTvHidService extends BaseTvHidService {
    private static final String TAG = "LgTvHidService";
    
    // The HID GATT service
    private BluetoothGattService hidService;
    
    // Input report characteristic for sending reports
    private BluetoothGattCharacteristic inputReportCharacteristic;
    
    // The current report state
    private byte buttonsByte = 0;
    private byte xByte = 0;
    private byte yByte = 0;
    
    /**
     * Constructor for LgTvHidService.
     * 
     * @param gattServerManager The GATT server manager
     */
    public LgTvHidService(BleGattServerManager gattServerManager) {
        super(gattServerManager);
    }
    
    @Override
    public boolean initialize() {
        logDebug("Initializing LG TV HID Service");
        
        // Create the HID service
        hidService = createHidService();
        
        // Register the service with the GATT server
        if (!gattServerManager.addHidService(hidService)) {
            logError("Failed to add HID service to GATT server");
            return false;
        }
        
        // Get the input report characteristic
        inputReportCharacteristic = hidService.getCharacteristic(HID_REPORT_UUID);
        if (inputReportCharacteristic == null) {
            logError("Input report characteristic not found");
            return false;
        }
        
        logDebug("LG TV HID Service initialized successfully");
        return true;
    }
    
    @Override
    public BluetoothGattService getGattService() {
        return hidService;
    }
    
    /**
     * Creates the HID GATT service for LG Smart TVs.
     * 
     * @return The HID GATT service
     */
    private BluetoothGattService createHidService() {
        logDebug("Creating LG TV HID service");
        
        BluetoothGattService service = new BluetoothGattService(
                HID_SERVICE_UUID, 
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        
        // HID Information characteristic (read-only)
        BluetoothGattCharacteristic hidInfo = new BluetoothGattCharacteristic(
                HID_INFORMATION_UUID, 
                BluetoothGattCharacteristic.PROPERTY_READ, 
                BluetoothGattCharacteristic.PERMISSION_READ);
        hidInfo.setValue(HID_INFORMATION);
        service.addCharacteristic(hidInfo);
        
        // Control Point characteristic (write-only, no response)
        BluetoothGattCharacteristic controlPoint = new BluetoothGattCharacteristic(
                HID_CONTROL_POINT_UUID, 
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, 
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(controlPoint);
        
        // Protocol Mode characteristic (read/write)
        BluetoothGattCharacteristic protocolMode = new BluetoothGattCharacteristic(
                HID_PROTOCOL_MODE_UUID, 
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, 
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        protocolMode.setValue(new byte[]{PROTOCOL_MODE_REPORT}); // Report protocol mode
        service.addCharacteristic(protocolMode);
        
        // Report Map characteristic (read-only)
        BluetoothGattCharacteristic reportMap = new BluetoothGattCharacteristic(
                HID_REPORT_MAP_UUID, 
                BluetoothGattCharacteristic.PROPERTY_READ, 
                BluetoothGattCharacteristic.PERMISSION_READ);
        reportMap.setValue(REPORT_MAP);
        service.addCharacteristic(reportMap);
        
        // Input Report characteristic (read/notify)
        BluetoothGattCharacteristic inputReport = new BluetoothGattCharacteristic(
                HID_REPORT_UUID, 
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY, 
                BluetoothGattCharacteristic.PERMISSION_READ);
        
        // Client Characteristic Configuration Descriptor (CCCD) for notifications
        BluetoothGattDescriptor cccd = new BluetoothGattDescriptor(
                CLIENT_CONFIG_UUID, 
                BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        cccd.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        inputReport.addDescriptor(cccd);
        
        service.addCharacteristic(inputReport);
        
        logDebug("HID service created successfully");
        return service;
    }
    
    @Override
    public boolean pressDirectionalButton(int direction) {
        if (!isReadyToSend()) {
            return false;
        }
        
        // Map direction to button bit
        switch (direction) {
            case DIRECTION_UP:
                buttonsByte |= BUTTON_UP;
                break;
            case DIRECTION_DOWN:
                buttonsByte |= BUTTON_DOWN;
                break;
            case DIRECTION_LEFT:
                buttonsByte |= BUTTON_LEFT;
                break;
            case DIRECTION_RIGHT:
                buttonsByte |= BUTTON_RIGHT;
                break;
            case DIRECTION_CENTER:
                buttonsByte |= BUTTON_SELECT;
                break;
            default:
                logError("Unknown direction: " + direction);
                return false;
        }
        
        return sendInputReport(buttonsByte, xByte, yByte);
    }
    
    @Override
    public boolean releaseDirectionalButtons() {
        if (!isReadyToSend()) {
            return false;
        }
        
        // Clear all directional buttons (first 5 bits)
        buttonsByte &= ~(BUTTON_UP | BUTTON_DOWN | BUTTON_LEFT | BUTTON_RIGHT | BUTTON_SELECT);
        return sendInputReport(buttonsByte, xByte, yByte);
    }
    
    @Override
    public boolean pressSelectButton() {
        if (!isReadyToSend()) {
            return false;
        }
        
        buttonsByte |= BUTTON_SELECT;
        return sendInputReport(buttonsByte, xByte, yByte);
    }
    
    @Override
    public boolean releaseSelectButton() {
        if (!isReadyToSend()) {
            return false;
        }
        
        buttonsByte &= ~BUTTON_SELECT;
        return sendInputReport(buttonsByte, xByte, yByte);
    }
    
    @Override
    public boolean pressBackButton() {
        if (!isReadyToSend()) {
            return false;
        }
        
        buttonsByte |= BUTTON_BACK;
        return sendInputReport(buttonsByte, xByte, yByte);
    }
    
    @Override
    public boolean pressHomeButton() {
        if (!isReadyToSend()) {
            return false;
        }
        
        buttonsByte |= BUTTON_HOME;
        return sendInputReport(buttonsByte, xByte, yByte);
    }
    
    @Override
    public boolean moveCursor(int x, int y) {
        if (!isReadyToSend()) {
            return false;
        }
        
        // Clamp values to byte range
        x = Math.max(-127, Math.min(127, x));
        y = Math.max(-127, Math.min(127, y));
        
        // Update x and y values
        xByte = (byte) x;
        yByte = (byte) y;
        
        return sendInputReport(buttonsByte, xByte, yByte);
    }
    
    @Override
    public boolean sendMediaControl(int mediaControl) {
        if (!isReadyToSend()) {
            return false;
        }
        
        // Map media control to button bit
        byte mediaBit = 0;
        switch (mediaControl) {
            case MEDIA_PLAY_PAUSE:
                mediaBit = BUTTON_PLAY_PAUSE;
                break;
            case MEDIA_NEXT:
                mediaBit = BUTTON_NEXT;
                break;
            case MEDIA_PREVIOUS:
                mediaBit = BUTTON_PREVIOUS;
                break;
            case MEDIA_VOLUME_UP:
                mediaBit = BUTTON_VOLUME_UP;
                break;
            case MEDIA_VOLUME_DOWN:
                mediaBit = BUTTON_VOLUME_DOWN;
                break;
            case MEDIA_MUTE:
                mediaBit = BUTTON_MUTE;
                break;
            default:
                logError("Unknown media control: " + mediaControl);
                return false;
        }
        
        // Send button press
        byte oldButtons = buttonsByte;
        buttonsByte |= mediaBit;
        boolean result = sendInputReport(buttonsByte, (byte) 0, (byte) 0);
        
        // Schedule release after a delay
        handler.postDelayed(() -> {
            buttonsByte = oldButtons; // Restore previous button state
            sendInputReport(buttonsByte, (byte) 0, (byte) 0);
        }, DEFAULT_CLICK_DELAY);
        
        return result;
    }
    
    /**
     * Sends an input report with the specified button, x, and y values.
     * 
     * @param buttons Button bitmap
     * @param x X movement (-127 to 127)
     * @param y Y movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendInputReport(byte buttons, byte x, byte y) {
        if (!isReadyToSend()) {
            return false;
        }
        
        // Update current state
        this.buttonsByte = buttons;
        this.xByte = x;
        this.yByte = y;
        
        // Create the 3-byte report
        byte[] report = new byte[]{buttons, x, y};
        
        logDebug("Sending report: " + bytesToHex(report));
        
        // Set the value and send notification
        inputReportCharacteristic.setValue(report);
        boolean success = gattServerManager.sendNotification(
                connectedDevice, 
                inputReportCharacteristic, 
                false);
        
        if (!success) {
            logError("Failed to send input report");
        }
        
        return success;
    }
    
    @Override
    public boolean supportsPointer() {
        return true; // LG Smart TVs support pointer movement
    }
    
    @Override
    public boolean supportsMediaControls() {
        return true; // LG Smart TVs support media controls
    }
    
    @Override
    public String getImplementationName() {
        return "LG Smart TV";
    }
    
    @Override
    public void close() {
        super.close();
        
        // Reset all state
        buttonsByte = 0;
        xByte = 0;
        yByte = 0;
        
        // Clear the service
        hidService = null;
        inputReportCharacteristic = null;
        
        logDebug("LG TV HID Service closed");
    }
}
