package com.example.blehid.core.report;

import com.example.blehid.core.HidConstants;

/**
 * Mouse HID report implementation.
 * Represents a mouse input report with buttons, movement, and scroll wheel.
 */
public class MouseReport implements Report {
    private final int buttons;
    private final int x;
    private final int y;
    private final int wheel;
    
    /**
     * Creates a new mouse report.
     *
     * @param buttons Button state bitmap (bit 0: left, bit 1: right, bit 2: middle)
     * @param x Horizontal movement (-127 to 127)
     * @param y Vertical movement (-127 to 127)
     * @param wheel Wheel movement (-127 to 127)
     */
    public MouseReport(int buttons, int x, int y, int wheel) {
        this.buttons = buttons & 0x07; // Only bits 0-2 are used
        
        // Clamp to valid range (-127 to 127)
        this.x = Math.max(-127, Math.min(127, x));
        this.y = Math.max(-127, Math.min(127, y));
        this.wheel = Math.max(-127, Math.min(127, wheel));
    }
    
    /**
     * Gets the button state.
     *
     * @return Button state bitmap
     */
    public int getButtons() {
        return buttons;
    }
    
    /**
     * Gets the horizontal movement.
     *
     * @return X movement
     */
    public int getX() {
        return x;
    }
    
    /**
     * Gets the vertical movement.
     *
     * @return Y movement
     */
    public int getY() {
        return y;
    }
    
    /**
     * Gets the wheel movement.
     *
     * @return Wheel movement
     */
    public int getWheel() {
        return wheel;
    }
    
    @Override
    public byte getReportId() {
        return HidConstants.REPORT_ID_MOUSE;
    }
    
    @Override
    public byte[] format() {
        // 4 bytes: buttons, x, y, wheel (no reportId in payload)
        // For Android compatibility, the reportId is in the descriptor only
        byte[] report = new byte[4];
        report[0] = (byte)(buttons & 0xFF);
        report[1] = (byte)(x & 0xFF);
        report[2] = (byte)(y & 0xFF);
        report[3] = (byte)(wheel & 0xFF);
        return report;
    }
    
    /**
     * Formats a boot mode report (without report ID).
     *
     * @return The formatted boot mode report
     */
    public byte[] formatBootMode() {
        // 3 bytes: buttons, x, y (no report ID, no wheel)
        byte[] report = new byte[3];
        report[0] = (byte)(buttons & 0xFF);
        report[1] = (byte)(x & 0xFF);
        report[2] = (byte)(y & 0xFF);
        return report;
    }
    
    @Override
    public <R> R accept(ReportVisitor<R> visitor) {
        return visitor.visit(this);
    }
}
