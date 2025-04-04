package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.util.Log;

import com.example.blehid.core.manager.BleGattServiceRegistry;
import com.example.blehid.core.report.Report;
import com.example.blehid.core.report.ReportVisitor;

import java.util.UUID;

/**
 * Abstract base class for HID report handlers.
 * Provides common functionality for different types of HID reports.
 * 
 * @param <T> The type of report this handler processes
 */
public abstract class AbstractReportHandler<T extends Report> {
    private static final String TAG = "AbstractReportHandler";
    
    protected final BleGattServiceRegistry gattServerManager;
    protected final BleNotifier notifier;
    protected final BluetoothGattCharacteristic primaryCharacteristic;
    protected byte protocolMode = HidConstants.PROTOCOL_MODE_REPORT;
    
    /**
     * Creates a new HID Report Handler.
     *
     * @param gattServerManager The GATT service registry
     * @param notifier The BLE notifier
     * @param primaryCharacteristic The primary characteristic for this report type
     */
    public AbstractReportHandler(
            BleGattServiceRegistry gattServerManager,
            BleNotifier notifier,
            BluetoothGattCharacteristic primaryCharacteristic) {
        this.gattServerManager = gattServerManager;
        this.notifier = notifier;
        this.primaryCharacteristic = primaryCharacteristic;
    }
    
    /**
     * Creates and initializes a report of the appropriate type.
     * Subclasses should implement this to create the specific report type.
     *
     * @return A new report instance
     */
    protected abstract T createEmptyReport();
    
    /**
     * Gets the report ID for this handler.
     *
     * @return The report ID
     */
    public abstract byte getReportId();
    
    /**
     * Sends a report to the connected device.
     *
     * @param device The connected device
     * @param report The report to send
     * @return true if the report was sent successfully, false otherwise
     */
    protected boolean sendReport(BluetoothDevice device, T report) {
        if (device == null) {
            Log.e(TAG, "Cannot send report: No device connected");
            return false;
        }
        
        byte[] reportData = report.format();
        UUID charUuid = primaryCharacteristic.getUuid();
        
        Log.d(TAG, "Sending report: " + HidConstants.bytesToHex(reportData) + 
              " to characteristic: " + charUuid);
        
        // Try direct notification first for higher reliability
        try {
            if (device.getBondState() == BluetoothDevice.BOND_BONDED) {
                primaryCharacteristic.setValue(reportData);
                
                // Get the GATT server directly
                BluetoothGattServer gattServer = gattServerManager.getGattServer();
                if (gattServer != null) {
                    boolean success = false;
                    
                    // Try both notification and indication if needed
                    try {
                        success = gattServer.notifyCharacteristicChanged(device, primaryCharacteristic, false);
                        Log.d(TAG, "Direct notification result: " + success);
                    } catch (Exception e) {
                        Log.w(TAG, "Regular notification failed, trying indication", e);
                    }
                    
                    if (!success) {
                        try {
                            // Try indication as fallback (some Android HID implementations might need this)
                            success = gattServer.notifyCharacteristicChanged(device, primaryCharacteristic, true);
                            Log.d(TAG, "Direct indication result: " + success);
                        } catch (Exception e) {
                            Log.w(TAG, "Indication failed too", e);
                        }
                    }
                    
                    if (success) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error with direct notification", e);
        }
        
        // Fall back to using the notifier (which may have its own retry logic)
        return notifier.sendNotificationWithRetry(charUuid, reportData);
    }
    
    /**
     * Called when notifications are enabled or disabled for a characteristic.
     *
     * @param enabled Whether notifications are enabled or disabled
     */
    public void setNotificationsEnabled(boolean enabled) {
        if (primaryCharacteristic != null) {
            notifier.setNotificationsEnabled(primaryCharacteristic.getUuid(), enabled);
        }
    }
    
    /**
     * Called when notifications are enabled or disabled for a characteristic.
     *
     * @param characteristicUuid The UUID of the characteristic
     * @param enabled Whether notifications are enabled or disabled
     */
    public void setNotificationsEnabled(UUID characteristicUuid, boolean enabled) {
        notifier.setNotificationsEnabled(characteristicUuid, enabled);
    }
    
    /**
     * Called when the protocol mode is changed.
     * 
     * @param mode The new protocol mode
     */
    public void setProtocolMode(byte mode) {
        this.protocolMode = mode;
        Log.d(TAG, "Protocol mode set to " + 
              (mode == HidConstants.PROTOCOL_MODE_REPORT ? "Report" : "Boot"));
    }
    
    /**
     * Enables notifications for the primary characteristic.
     */
    protected void enableNotifications() {
        if (primaryCharacteristic != null) {
            byte[] initialReport = createEmptyReport().format();
            notifier.enableNotificationsForCharacteristic(primaryCharacteristic, initialReport);
        }
    }
    
    /**
     * Process a report using a visitor.
     *
     * @param report The report to process
     * @param visitor The visitor to apply
     * @param <R> The return type of the visitor operation
     * @return The result of the visitor operation
     */
    protected <R> R processWithVisitor(T report, ReportVisitor<R> visitor) {
        return report.accept(visitor);
    }
    
    /**
     * Determines if this handler should handle the given characteristic.
     * 
     * @param characteristicUuid The UUID of the characteristic
     * @return true if this handler should handle the characteristic, false otherwise
     */
    protected boolean shouldHandleCharacteristic(UUID characteristicUuid) {
        return primaryCharacteristic != null && 
               primaryCharacteristic.getUuid().equals(characteristicUuid);
    }
}
