package com.inventonater.blehid.report;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;

import com.inventonater.blehid.HidManager;
import com.inventonater.blehid.HidReportConstants;

/**
 * Handles consumer control (media) HID reports.
 * This class provides methods for sending consumer control reports to the host device,
 * including media control functions like play/pause, volume control, etc.
 */
public class MediaReporter {
    private final HidManager manager;
    private final byte[] report = new byte[HidReportConstants.CONSUMER_REPORT_SIZE];
    
    /**
     * Creates a new media reporter.
     *
     * @param manager The HID manager
     */
    public MediaReporter(HidManager manager) {
        this.manager = manager;
    }
    
    /**
     * Gets an empty consumer report.
     *
     * @return An empty consumer report
     */
    public byte[] getEmptyReport() {
        // Clear the report
        for (int i = 0; i < report.length; i++) {
            report[i] = 0;
        }
        return report.clone();
    }
    
    /**
     * Sends a play/pause command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPlayPause() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_PLAY_PAUSE);
    }
    
    /**
     * Sends a next track command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendNextTrack() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_SCAN_NEXT);
    }
    
    /**
     * Sends a previous track command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPreviousTrack() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_SCAN_PREVIOUS);
    }
    
    /**
     * Sends a stop command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendStop() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_STOP);
    }
    
    /**
     * Sends a volume up command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeUp() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_VOLUME_UP);
    }
    
    /**
     * Sends a volume down command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendVolumeDown() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_VOLUME_DOWN);
    }
    
    /**
     * Sends a mute command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMute() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_MUTE);
    }
    
    /**
     * Sends a fast forward command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendFastForward() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_FAST_FORWARD);
    }
    
    /**
     * Sends a rewind command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendRewind() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_REWIND);
    }
    
    /**
     * Sends a power command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendPower() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_POWER);
    }
    
    /**
     * Sends a menu command.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendMenu() {
        return sendConsumerControl(HidReportConstants.CONSUMER_USAGE_MENU);
    }
    
    /**
     * Sends a consumer control command and then releases it.
     *
     * @param usage The consumer control usage code
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendConsumerControl(short usage) {
        if (!manager.isConnected()) {
            manager.logError("Cannot send consumer control: Not connected");
            return false;
        }
        
        boolean pressed = pressConsumerControl(usage);
        if (!pressed) {
            return false;
        }
        
        try {
            // Small delay to simulate a button press
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return releaseConsumerControl();
    }
    
    /**
     * Presses a consumer control.
     *
     * @param usage The consumer control usage code
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean pressConsumerControl(short usage) {
        // Set the usage in the report (little-endian)
        report[0] = (byte) (usage & 0xFF);         // Low byte
        report[1] = (byte) ((usage >> 8) & 0xFF);  // High byte
        
        manager.logVerbose("Consumer control press: usage=0x" + 
                String.format("%04X", usage), report);
        
        return sendReport();
    }
    
    /**
     * Releases all consumer controls.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseConsumerControl() {
        // Clear the report
        report[0] = 0;
        report[1] = 0;
        
        manager.logVerbose("Consumer control release", report);
        
        return sendReport();
    }
    
    /**
     * Sends the current report to the host device.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    private boolean sendReport() {
        if (!manager.isConnected()) {
            manager.logError("Cannot send consumer report: Not connected");
            return false;
        }
        
        BluetoothDevice device = manager.getConnectedDevice();
        BluetoothHidDevice hidDevice = manager.getHidDevice();
        
        if (hidDevice == null) {
            manager.logError("Cannot send consumer report: HID device not available");
            return false;
        }
        
        try {
            boolean success = hidDevice.sendReport(
                    device, 
                    HidReportConstants.REPORT_ID_CONSUMER, 
                    report);
            
            if (!success) {
                manager.logError("Failed to send consumer report");
            }
            
            return success;
        } catch (Exception e) {
            manager.logError("Exception sending consumer report", e);
            return false;
        }
    }
}
