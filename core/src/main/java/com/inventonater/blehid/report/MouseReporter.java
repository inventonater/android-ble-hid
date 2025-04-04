package com.inventonater.blehid.report;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;

import com.inventonater.blehid.HidManager;
import com.inventonater.blehid.HidReportConstants;
import com.inventonater.blehid.util.LogUtils;

/**
 * Handles mouse HID reports.
 * This class provides methods for sending mouse reports to the host device,
 * including movement, scrolling, and button presses.
 */
public class MouseReporter {
    private final HidManager manager;
    private byte currentButtons = 0;
    private final byte[] report = new byte[HidReportConstants.MOUSE_REPORT_SIZE];
    
    /**
     * Creates a new mouse reporter.
     *
     * @param manager The HID manager
     */
    public MouseReporter(HidManager manager) {
        this.manager = manager;
    }
    
    /**
     * Gets an empty mouse report.
     *
     * @return An empty mouse report
     */
    public byte[] getEmptyReport() {
        // Clear the report
        for (int i = 0; i < report.length; i++) {
            report[i] = 0;
        }
        return report.clone();
    }
    
    /**
     * Moves the mouse pointer.
     *
     * @param x Horizontal movement (-127 to 127)
     * @param y Vertical movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean move(int x, int y) {
        return move(x, y, 0);
    }
    
    /**
     * Moves the mouse pointer and scrolls the wheel.
     *
     * @param x Horizontal movement (-127 to 127)
     * @param y Vertical movement (-127 to 127)
     * @param wheel Wheel movement (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean move(int x, int y, int wheel) {
        // Clamp values to valid range
        x = clamp(x, -127, 127);
        y = clamp(y, -127, 127);
        wheel = clamp(wheel, -127, 127);
        
        // Prepare the report
        report[0] = currentButtons;
        report[1] = (byte) x;
        report[2] = (byte) y;
        report[3] = (byte) wheel;
        
        // Log verbose info
        manager.logVerbose("Mouse move report: buttons=" + currentButtons + 
                ", x=" + x + ", y=" + y + ", wheel=" + wheel, report);
        
        // Send the report
        return sendReport();
    }
    
    /**
     * Scrolls the mouse wheel.
     *
     * @param amount Scroll amount (-127 to 127)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean scroll(int amount) {
        return move(0, 0, amount);
    }
    
    /**
     * Performs a button press and release (click).
     *
     * @param button The button to click (MouseReportConstants.MOUSE_BUTTON_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean click(int button) {
        if (!press(button)) {
            return false;
        }
        
        try {
            // Small delay to simulate a click
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        return release(button);
    }
    
    /**
     * Presses a mouse button.
     *
     * @param button The button to press (MouseReportConstants.MOUSE_BUTTON_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean press(int button) {
        // Set the button bit
        currentButtons |= button;
        
        // Prepare the report
        report[0] = currentButtons;
        report[1] = 0;  // No movement
        report[2] = 0;
        report[3] = 0;
        
        // Log verbose info
        manager.logVerbose("Mouse button press report: buttons=" + currentButtons, report);
        
        // Send the report
        return sendReport();
    }
    
    /**
     * Releases a mouse button.
     *
     * @param button The button to release (MouseReportConstants.MOUSE_BUTTON_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean release(int button) {
        // Clear the button bit
        currentButtons &= ~button;
        
        // Prepare the report
        report[0] = currentButtons;
        report[1] = 0;  // No movement
        report[2] = 0;
        report[3] = 0;
        
        // Log verbose info
        manager.logVerbose("Mouse button release report: buttons=" + currentButtons, report);
        
        // Send the report
        return sendReport();
    }
    
    /**
     * Releases all buttons.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseAll() {
        // Clear all buttons
        currentButtons = 0;
        
        // Prepare the report
        report[0] = 0;
        report[1] = 0;
        report[2] = 0;
        report[3] = 0;
        
        // Log verbose info
        manager.logVerbose("Mouse release all report", report);
        
        // Send the report
        return sendReport();
    }
    
    /**
     * Sends the current report to the host device.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    private boolean sendReport() {
        if (!manager.isConnected()) {
            manager.logError("Cannot send mouse report: Not connected");
            return false;
        }
        
        BluetoothDevice device = manager.getConnectedDevice();
        BluetoothHidDevice hidDevice = manager.getHidDevice();
        
        if (hidDevice == null) {
            manager.logError("Cannot send mouse report: HID device not available");
            return false;
        }
        
        try {
            boolean success = hidDevice.sendReport(
                    device, 
                    HidReportConstants.REPORT_ID_MOUSE, 
                    report);
            
            if (!success) {
                manager.logError("Failed to send mouse report");
            }
            
            return success;
        } catch (Exception e) {
            manager.logError("Exception sending mouse report", e);
            return false;
        }
    }
    
    /**
     * Clamps a value to a range.
     *
     * @param value The value to clamp
     * @param min The minimum value
     * @param max The maximum value
     * @return The clamped value
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
