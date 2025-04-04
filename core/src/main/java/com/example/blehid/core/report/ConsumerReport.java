package com.example.blehid.core.report;

import com.example.blehid.core.HidConstants;

/**
 * Consumer control HID report implementation.
 * Represents a consumer control input report for media controls.
 */
public class ConsumerReport implements Report {
    private final byte controlBits;
    
    /**
     * Creates a new consumer control report.
     *
     * @param controlBits Bitmap of control bits (see HidConstants.Consumer)
     */
    public ConsumerReport(byte controlBits) {
        this.controlBits = controlBits;
    }
    
    /**
     * Creates a consumer control report for a specific media control.
     *
     * @param controlType One of the CONSUMER_* constants from HidConstants.Consumer
     * @return A new consumer report
     */
    public static ConsumerReport forControl(byte controlType) {
        return new ConsumerReport(controlType);
    }
    
    /**
     * Creates an empty (no controls active) consumer report.
     *
     * @return An empty consumer report
     */
    public static ConsumerReport empty() {
        return new ConsumerReport((byte) 0);
    }
    
    /**
     * Gets the control bits.
     *
     * @return Control bits bitmap
     */
    public byte getControlBits() {
        return controlBits;
    }
    
    /**
     * Checks if a specific control is active.
     *
     * @param controlType The control to check
     * @return true if the control is active, false otherwise
     */
    public boolean isControlActive(byte controlType) {
        return (controlBits & controlType) != 0;
    }
    
    @Override
    public byte getReportId() {
        return HidConstants.REPORT_ID_CONSUMER;
    }
    
    @Override
    public byte[] format() {
        // Our HID report descriptor defines this as a 16-bit usage (2 bytes) for Consumer Controls
        // According to the spec and our REPORT_MAP, we need 2 bytes for this report
        // First byte is control bits, second byte is 0
        byte[] report = new byte[2]; // Match the report descriptor's 16-bit usage
        report[0] = controlBits;
        report[1] = 0; // Second byte must be 0 for Android compatibility
        return report;
    }
    
    @Override
    public <R> R accept(ReportVisitor<R> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ConsumerReport{controls=0x")
          .append(Integer.toHexString(controlBits & 0xFF))
          .append(" [");
        
        // Add human-readable descriptions of active controls
        if (isControlActive(HidConstants.Consumer.CONSUMER_MUTE)) {
            sb.append("MUTE ");
        }
        if (isControlActive(HidConstants.Consumer.CONSUMER_VOLUME_UP)) {
            sb.append("VOL_UP ");
        }
        if (isControlActive(HidConstants.Consumer.CONSUMER_VOLUME_DOWN)) {
            sb.append("VOL_DOWN ");
        }
        if (isControlActive(HidConstants.Consumer.CONSUMER_PLAY_PAUSE)) {
            sb.append("PLAY/PAUSE ");
        }
        if (isControlActive(HidConstants.Consumer.CONSUMER_NEXT_TRACK)) {
            sb.append("NEXT ");
        }
        if (isControlActive(HidConstants.Consumer.CONSUMER_PREV_TRACK)) {
            sb.append("PREV ");
        }
        if (isControlActive(HidConstants.Consumer.CONSUMER_STOP)) {
            sb.append("STOP ");
        }
        
        sb.append("]}");
        return sb.toString();
    }
}
