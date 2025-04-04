package com.inventonater.blehid;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Build;

import com.inventonater.blehid.debug.BleHidDebugger;

/**
 * Implementation of callback to handle HID device events.
 * This class handles callbacks from the HID service, such as registration status,
 * connection state changes, and requests from the host.
 */
public class HidDeviceCallback implements android.bluetooth.BluetoothProfile.ServiceListener {
    private final BleHidDebugger debugger;
    private final HidManager manager;
    
    /**
     * Creates a new HID device callback.
     *
     * @param manager The HID manager
     */
    public HidDeviceCallback(HidManager manager) {
        this.manager = manager;
        this.debugger = BleHidDebugger.getInstance();
    }
    
    // Callback methods for HID device events
    public void onAppStatusChanged(BluetoothDevice device, boolean registered) {
        manager.log("onAppStatusChanged: device=" + 
                (device != null ? device.getAddress() : "null") + 
                ", registered=" + registered);
        
        manager.setRegistered(registered);
    }
    
    public void onConnectionStateChanged(BluetoothDevice device, int state) {
        manager.log("onConnectionStateChanged: device=" + 
                (device != null ? device.getAddress() : "null") + 
                ", state=" + state);
        
        manager.setConnectionState(device, state);
        
        // Track report transmission success rate after connection
        if (state == BluetoothProfile.STATE_CONNECTED) {
            debugger.markTimestamp("connection_established");
        }
    }
    
    public void onGetReport(BluetoothDevice device, byte type, byte id, int bufferSize) {
        manager.log("onGetReport: device=" + 
                (device != null ? device.getAddress() : "null") + 
                ", type=" + type + ", id=" + id + ", bufferSize=" + bufferSize);
        
        debugger.markTimestamp("get_report_" + id);
        
        // Determine which report to send based on the ID
        byte[] report = null;
        
        switch (id) {
            case HidReportConstants.REPORT_ID_KEYBOARD:
                report = manager.getKeyboardReporter().getEmptyReport();
                break;
                
            case HidReportConstants.REPORT_ID_MOUSE:
                report = manager.getMouseReporter().getEmptyReport();
                break;
                
            case HidReportConstants.REPORT_ID_CONSUMER:
                report = manager.getMediaReporter().getEmptyReport();
                break;
        }
        
        // Reply with the report
        if (report != null) {
            manager.logVerbose("Replying to GET_REPORT with report", report);
            boolean success = manager.getHidDevice().replyReport(device, type, id, report);
            debugger.logReportSent(id, success);
            debugger.logElapsedTime("get_report_" + id, "GET_REPORT handling for ID " + id);
        } else {
            manager.logError("No report available for id: " + id);
            debugger.log("ERROR: Unable to find report for GET_REPORT request with ID " + id);
        }
    }
    
    public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
        manager.log("onSetReport: device=" + 
                (device != null ? device.getAddress() : "null") + 
                ", type=" + type + ", id=" + id + ", data length=" + 
                (data != null ? data.length : 0));
        
        if (data != null) {
            manager.logVerbose("SET_REPORT data", data);
        }
        
        // Process the report data if needed
        // For most HID peripherals, this can be a no-op
        
        // Acknowledge the report
        manager.getHidDevice().reportError(device, (byte) 0);
    }
    
    public void onSetProtocol(BluetoothDevice device, byte protocol) {
        String protocolStr = (protocol == 0) ? "REPORT" : 
                            ((protocol == 1) ? "BOOT" : "UNKNOWN");
        
        manager.log("onSetProtocol: device=" + 
                (device != null ? device.getAddress() : "null") + 
                ", protocol=" + protocol + " (" + protocolStr + ")");
        
        // Nothing to do for most implementations
    }
    
    public void onIntrData(BluetoothDevice device, byte reportId, byte[] data) {
        manager.log("onIntrData: device=" + 
                (device != null ? device.getAddress() : "null") + 
                ", reportId=" + reportId + ", data length=" + 
                (data != null ? data.length : 0));
        
        if (data != null) {
            manager.logVerbose("INTR data", data);
        }
        
        // Process interrupt data from host if needed
        // For most HID peripherals, this can be a no-op
    }
    
    public void onVirtualCableUnplug(BluetoothDevice device) {
        manager.log("onVirtualCableUnplug: device=" + 
                (device != null ? device.getAddress() : "null"));
        
        debugger.log("Virtual Cable Unplug received - this is a host-initiated disconnect");
        
        // Reset connection state
        manager.setConnectionState(device, BluetoothProfile.STATE_DISCONNECTED);
    }
    
    /**
     * Analyzes bond state changes for debug purposes.
     * This should be called from the bond state change broadcast receiver.
     *
     * @param device The device whose bond state changed
     * @param bondState The new bond state
     * @param prevBondState The previous bond state
     */
    public void analyzeBondStateChange(BluetoothDevice device, int bondState, int prevBondState) {
        debugger.analyzePairingStateChange(device, bondState, prevBondState);
    }
    
    // Additional method available in newer API levels
    // ServiceListener methods
    public void onServiceConnected(int profile, BluetoothProfile proxy) {
        // Not used for our callback, handled in HidManager
    }
    
    public void onServiceDisconnected(int profile) {
        // Not used for our callback, handled in HidManager
    }
}
