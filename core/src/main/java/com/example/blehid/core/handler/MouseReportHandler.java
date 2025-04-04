package com.example.blehid.core.handler;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.util.Log;

import com.example.blehid.core.AbstractReportHandler;
import com.example.blehid.core.BleNotifier;
import com.example.blehid.core.HidConstants;
import com.example.blehid.core.manager.BleGattServiceRegistry;
import com.example.blehid.core.report.MouseReport;

import java.util.UUID;

/**
 * Handler for mouse HID reports.
 */
public class MouseReportHandler extends AbstractReportHandler<MouseReport> {
    private static final String TAG = "MouseReportHandler";
    
    private final BluetoothGattCharacteristic bootMouseCharacteristic;
    
    /**
     * Creates a new mouse report handler.
     *
     * @param gattServerManager The GATT server manager
     * @param notifier The BLE notifier
     * @param primaryCharacteristic The primary characteristic for report mode
     * @param bootMouseCharacteristic The boot mode characteristic (may be null)
     */
    public MouseReportHandler(
            BleGattServiceRegistry gattServerManager,
            BleNotifier notifier,
            BluetoothGattCharacteristic primaryCharacteristic,
            BluetoothGattCharacteristic bootMouseCharacteristic) {
        super(gattServerManager, notifier, primaryCharacteristic);
        this.bootMouseCharacteristic = bootMouseCharacteristic;
    }
    
    @Override
    protected MouseReport createEmptyReport() {
        return new MouseReport(0, 0, 0, 0);
    }
    
    @Override
    public byte getReportId() {
        return HidConstants.REPORT_ID_MOUSE;
    }
    
    /**
     * Creates a report with the given parameters.
     *
     * @param buttons Button state
     * @param x X movement
     * @param y Y movement
     * @param wheel Wheel movement
     * @return A new mouse report
     */
    public MouseReport createReport(int buttons, int x, int y, int wheel) {
        return new MouseReport(buttons, x, y, wheel);
    }
    
    /**
     * Sends a complete mouse report.
     *
     * @param device Connected device
     * @param buttons Button state
     * @param x X movement
     * @param y Y movement
     * @param wheel Wheel movement
     * @return true if successful, false otherwise
     */
    public boolean sendMouseReport(BluetoothDevice device, int buttons, int x, int y, int wheel) {
        Log.d(TAG, "Attempting to send mouse report - buttons: " + buttons + 
              ", x: " + x + ", y: " + y + ", wheel: " + wheel);
              
        MouseReport report = createReport(buttons, x, y, wheel);
        
        if (protocolMode == HidConstants.PROTOCOL_MODE_BOOT && bootMouseCharacteristic != null) {
            // In boot mode, use the boot characteristic
            byte[] bootReport = report.formatBootMode();
            Log.d(TAG, "Using boot mode report: " + HidConstants.bytesToHex(bootReport) + 
                  " for UUID: " + bootMouseCharacteristic.getUuid());
            return notifier.sendNotificationWithRetry(bootMouseCharacteristic.getUuid(), bootReport);
        } else {
            // Direct notification to bypass any UUID handling issues
            byte[] reportData = report.format();
            Log.d(TAG, "Using report mode: " + HidConstants.bytesToHex(reportData) + 
                  " for UUID: " + primaryCharacteristic.getUuid());
                  
            // Try direct notification without going through the report registry
            try {
                if (device != null && device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    primaryCharacteristic.setValue(reportData);
                    
                    // Get the GATT server directly
                    BluetoothGattServer gattServer = gattServerManager.getGattServer();
                    if (gattServer != null) {
                        boolean success = gattServer.notifyCharacteristicChanged(device, primaryCharacteristic, false);
                        Log.d(TAG, "Direct notification result: " + success);
                        return success;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error sending direct notification", e);
            }
            
            // Fall back to normal method if direct notification fails
            return sendReport(device, report);
        }
    }
    
    /**
     * Moves the pointer (no buttons pressed).
     *
     * @param device Connected device
     * @param x X movement
     * @param y Y movement
     * @return true if successful, false otherwise
     */
    public boolean movePointer(BluetoothDevice device, int x, int y) {
        return sendMouseReport(device, 0, x, y, 0);
    }
    
    /**
     * Scrolls the wheel (no buttons pressed, no movement).
     *
     * @param device Connected device
     * @param amount Scroll amount
     * @return true if successful, false otherwise
     */
    public boolean scroll(BluetoothDevice device, int amount) {
        return sendMouseReport(device, 0, 0, 0, amount);
    }
    
    /**
     * Performs a button click (press and release).
     *
     * @param device Connected device
     * @param button Button to click
     * @return true if successful, false otherwise
     */
    public boolean click(BluetoothDevice device, int button) {
        // Press button
        boolean pressResult = sendMouseReport(device, button, 0, 0, 0);
        
        try {
            // Small delay to make the click noticeable
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Log.w(TAG, "Click delay interrupted", e);
        }
        
        // Release button
        boolean releaseResult = sendMouseReport(device, 0, 0, 0, 0);
        
        return pressResult && releaseResult;
    }
    
    /**
     * Gets the current mouse report.
     *
     * @return Formatted mouse report
     */
    public byte[] getMouseReport() {
        return createEmptyReport().format();
    }
    
    /**
     * Gets the boot mode mouse report.
     *
     * @return Formatted boot mode report
     */
    public byte[] getBootMouseReport() {
        return createEmptyReport().formatBootMode();
    }
    
    @Override
    protected boolean shouldHandleCharacteristic(UUID characteristicUuid) {
        return super.shouldHandleCharacteristic(characteristicUuid) ||
              (bootMouseCharacteristic != null && 
               bootMouseCharacteristic.getUuid().equals(characteristicUuid));
    }
    
    @Override
    public void setProtocolMode(byte mode) {
        super.setProtocolMode(mode);
        // Additional boot mode setup if needed
    }
}
