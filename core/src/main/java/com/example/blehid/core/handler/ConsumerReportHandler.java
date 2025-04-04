package com.example.blehid.core.handler;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import com.example.blehid.core.AbstractReportHandler;
import com.example.blehid.core.BleNotifier;
import com.example.blehid.core.HidConstants;
import com.example.blehid.core.manager.BleGattServiceRegistry;
import com.example.blehid.core.report.ConsumerReport;

/**
 * Handler for consumer control HID reports (media keys, etc.).
 */
public class ConsumerReportHandler extends AbstractReportHandler<ConsumerReport> {
    private static final String TAG = "ConsumerReportHandler";
    
    /**
     * Creates a new consumer report handler.
     *
     * @param gattServerManager The GATT server manager
     * @param notifier The BLE notifier
     * @param primaryCharacteristic The primary characteristic for report mode
     */
    public ConsumerReportHandler(
            BleGattServiceRegistry gattServerManager,
            BleNotifier notifier,
            BluetoothGattCharacteristic primaryCharacteristic) {
        super(gattServerManager, notifier, primaryCharacteristic);
    }
    
    @Override
    protected ConsumerReport createEmptyReport() {
        return ConsumerReport.empty();
    }
    
    @Override
    public byte getReportId() {
        return HidConstants.REPORT_ID_CONSUMER;
    }
    
    /**
     * Sends a media control.
     *
     * @param device Connected device
     * @param controlBit The control bit to send
     * @return true if successful, false otherwise
     */
    public boolean sendConsumerControl(BluetoothDevice device, byte controlBit) {
        // Send control press
        ConsumerReport report = ConsumerReport.forControl(controlBit);
        boolean pressResult = sendReport(device, report);
        
        try {
            // Small delay to make the control press noticeable
            Thread.sleep(20);
        } catch (InterruptedException e) {
            Log.w(TAG, "Control press delay interrupted", e);
        }
        
        // Send control release
        boolean releaseResult = sendReport(device, createEmptyReport());
        
        return pressResult && releaseResult;
    }
    
    /**
     * Sends a media play/pause control.
     *
     * @param device Connected device
     * @return true if successful, false otherwise
     */
    public boolean sendPlayPause(BluetoothDevice device) {
        return sendConsumerControl(device, HidConstants.Consumer.CONSUMER_PLAY_PAUSE);
    }
    
    /**
     * Sends a media next track control.
     *
     * @param device Connected device
     * @return true if successful, false otherwise
     */
    public boolean sendNextTrack(BluetoothDevice device) {
        return sendConsumerControl(device, HidConstants.Consumer.CONSUMER_NEXT_TRACK);
    }
    
    /**
     * Sends a media previous track control.
     *
     * @param device Connected device
     * @return true if successful, false otherwise
     */
    public boolean sendPrevTrack(BluetoothDevice device) {
        return sendConsumerControl(device, HidConstants.Consumer.CONSUMER_PREV_TRACK);
    }
    
    /**
     * Sends a media volume up control.
     *
     * @param device Connected device
     * @return true if successful, false otherwise
     */
    public boolean sendVolumeUp(BluetoothDevice device) {
        return sendConsumerControl(device, HidConstants.Consumer.CONSUMER_VOLUME_UP);
    }
    
    /**
     * Sends a media volume down control.
     *
     * @param device Connected device
     * @return true if successful, false otherwise
     */
    public boolean sendVolumeDown(BluetoothDevice device) {
        return sendConsumerControl(device, HidConstants.Consumer.CONSUMER_VOLUME_DOWN);
    }
    
    /**
     * Sends a media mute control.
     *
     * @param device Connected device
     * @return true if successful, false otherwise
     */
    public boolean sendMute(BluetoothDevice device) {
        return sendConsumerControl(device, HidConstants.Consumer.CONSUMER_MUTE);
    }
    
    /**
     * Sends a media stop control.
     *
     * @param device Connected device
     * @return true if successful, false otherwise
     */
    public boolean sendStop(BluetoothDevice device) {
        return sendConsumerControl(device, HidConstants.Consumer.CONSUMER_STOP);
    }
    
    /**
     * Gets the current consumer report.
     *
     * @return Formatted consumer report
     */
    public byte[] getConsumerReport() {
        return createEmptyReport().format();
    }
}
