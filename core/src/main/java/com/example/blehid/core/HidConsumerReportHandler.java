package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import java.util.UUID;

import static com.example.blehid.core.HidConsumerConstants.*;
import static com.example.blehid.core.HidMouseConstants.*;

/**
 * Handles the creation and management of HID Consumer Control (Media Keys) reports.
 * This class encapsulates the logic for generating and sending media control reports.
 */
public class HidConsumerReportHandler extends AbstractReportHandler {
    private static final String TAG = "HidConsumerReportHandler";
    
    // Consumer report: [control bitmap] (1 byte)
    private final byte[] consumerReport = new byte[CONSUMER_REPORT_SIZE];
    
    private final BluetoothGattCharacteristic consumerCharacteristic;
    
    /**
     * Creates a new HID Consumer Report Handler.
     *
     * @param gattServerManager      The GATT server manager
     * @param consumerCharacteristic The consumer control characteristic
     */
    public HidConsumerReportHandler(
            BleGattServerManager gattServerManager,
            BluetoothGattCharacteristic consumerCharacteristic) {
        super(gattServerManager);
        this.consumerCharacteristic = consumerCharacteristic;
        
        // Initialize empty report (no buttons pressed)
        consumerReport[0] = 0;
    }
    
    /**
     * Sends a consumer control report.
     * 
     * @param device     The connected Bluetooth device
     * @param controlBit The control bit to set (one of CONSUMER_* constants)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendConsumerControl(BluetoothDevice device, byte controlBit) {
        Log.d(TAG, "sendConsumerControl - control: 0x" + String.format("%02X", controlBit));
        
        if (device == null) {
            Log.e(TAG, "No connected device");
            return false;
        }
        
        // Simple check to ensure notifications are enabled
        if (!notificationsEnabled) {
            Log.d(TAG, "Ensuring notifications are enabled");
            enableNotifications();
            notificationsEnabled = true;
        }
        
        // Set the control bit (one button at a time)
        consumerReport[0] = controlBit;
        
        // Send the report
        boolean pressed = sendReport(device, consumerReport);
        
        // Always follow with a release after a short delay
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Send release (all zeros)
        consumerReport[0] = 0;
        boolean released = sendReport(device, consumerReport);
        
        return pressed && released;
    }
    
    /**
     * Sends a play/pause control.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPlayPause(BluetoothDevice device) {
        return sendConsumerControl(device, CONSUMER_PLAY_PAUSE);
    }
    
    /**
     * Sends a volume up control.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeUp(BluetoothDevice device) {
        return sendConsumerControl(device, CONSUMER_VOLUME_UP);
    }
    
    /**
     * Sends a volume down control.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeDown(BluetoothDevice device) {
        return sendConsumerControl(device, CONSUMER_VOLUME_DOWN);
    }
    
    /**
     * Sends a mute control.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMute(BluetoothDevice device) {
        return sendConsumerControl(device, CONSUMER_MUTE);
    }
    
    /**
     * Sends a next track control.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendNextTrack(BluetoothDevice device) {
        return sendConsumerControl(device, CONSUMER_NEXT_TRACK);
    }
    
    /**
     * Sends a previous track control.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPrevTrack(BluetoothDevice device) {
        return sendConsumerControl(device, CONSUMER_PREV_TRACK);
    }
    
    /**
     * Sends a stop control.
     * 
     * @param device The connected Bluetooth device
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendStop(BluetoothDevice device) {
        return sendConsumerControl(device, CONSUMER_STOP);
    }
    
    /**
     * Helper method to send consumer report with retry.
     */
    private boolean sendReport(BluetoothDevice device, byte[] report) {
        if (consumerCharacteristic == null) {
            Log.e(TAG, "Consumer characteristic not available");
            return false;
        }
        
        consumerCharacteristic.setValue(report);
        
        // Send with retry for reliability
        return sendNotificationWithRetry(consumerCharacteristic.getUuid(), report);
    }
    
    /**
     * Determines if this handler should handle the given characteristic.
     * 
     * @param characteristicUuid The characteristic UUID to check
     * @return true if this handler should handle the characteristic
     */
    @Override
    protected boolean shouldHandleCharacteristic(UUID characteristicUuid) {
        return characteristicUuid.equals(HID_CONSUMER_CONTROL_UUID);
    }
    
    /**
     * Enables consumer notifications.
     */
    @Override
    protected void enableNotifications() {
        BluetoothGattDescriptor descriptor = consumerCharacteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor != null) {
            // Enable notifications
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d(TAG, "Set consumer descriptor value to enable notifications");
            
            // Send an empty report to initialize
            consumerReport[0] = 0;
            consumerCharacteristic.setValue(consumerReport);
            
            // Send two initial reports (reliability)
            try {
                gattServerManager.sendNotification(consumerCharacteristic.getUuid(), consumerReport);
                Thread.sleep(20); // Small delay
                gattServerManager.sendNotification(consumerCharacteristic.getUuid(), consumerReport);
            } catch (Exception e) {
                Log.e(TAG, "Error sending initial consumer report", e);
            }
        } else {
            Log.e(TAG, "Missing CCCD for consumer characteristic");
        }
    }
    
    /**
     * Called when notifications are enabled or disabled.
     * 
     * @param enabled Whether notifications are enabled
     */
    public void setNotificationsEnabled(boolean enabled) {
        this.notificationsEnabled = enabled;
    }
    
    /**
     * Gets the current consumer report.
     * 
     * @return The current consumer report
     */
    public byte[] getConsumerReport() {
        return consumerReport;
    }
}
