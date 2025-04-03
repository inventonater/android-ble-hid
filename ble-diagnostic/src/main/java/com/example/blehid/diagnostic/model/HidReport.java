package com.example.blehid.diagnostic.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Model class to represent HID reports received from a connected device.
 * This class handles both the raw report data and the interpretation of that data.
 */
public class HidReport {
    // Report types
    public static final int TYPE_INPUT = 1;
    public static final int TYPE_OUTPUT = 2;
    public static final int TYPE_FEATURE = 3;
    
    private final byte[] reportData;
    private final int reportType;
    private final long timestamp;
    private final String sourceDeviceAddress;
    
    /**
     * Create a new HID report instance
     * 
     * @param reportData Raw report data bytes
     * @param reportType Report type (input, output, feature)
     * @param sourceDeviceAddress MAC address of the source device
     */
    public HidReport(byte[] reportData, int reportType, String sourceDeviceAddress) {
        this.reportData = reportData.clone();
        this.reportType = reportType;
        this.timestamp = System.currentTimeMillis();
        this.sourceDeviceAddress = sourceDeviceAddress;
    }
    
    /**
     * Get the raw report data bytes
     * 
     * @return Report data as byte array
     */
    public byte[] getReportData() {
        return reportData.clone();
    }
    
    /**
     * Get the report type (input, output, feature)
     * 
     * @return Report type constant
     */
    public int getReportType() {
        return reportType;
    }
    
    /**
     * Get the timestamp when this report was received
     * 
     * @return Timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Get the MAC address of the device that sent this report
     * 
     * @return Device MAC address
     */
    public String getSourceDeviceAddress() {
        return sourceDeviceAddress;
    }
    
    /**
     * Get a human-readable string for the report type
     * 
     * @return Report type as string
     */
    public String getReportTypeString() {
        switch (reportType) {
            case TYPE_INPUT:
                return "INPUT";
            case TYPE_OUTPUT:
                return "OUTPUT";
            case TYPE_FEATURE:
                return "FEATURE";
            default:
                return "UNKNOWN";
        }
    }
    
    /**
     * Get a formatted timestamp string
     * 
     * @return Formatted timestamp
     */
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
        return sdf.format(new Date(timestamp));
    }
    
    /**
     * Get the report data as a hex string
     * 
     * @return Hex representation of the report data
     */
    public String getReportDataHex() {
        StringBuilder sb = new StringBuilder();
        for (byte b : reportData) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
    
    /**
     * Interpret keyboard report data
     * This assumes standard HID keyboard report format
     * 
     * @return Human-readable interpretation of the report
     */
    public String interpretKeyboardReport() {
        if (reportData.length < 2) {
            return "Invalid keyboard report (too short)";
        }
        
        StringBuilder result = new StringBuilder();
        
        // First byte contains modifier keys
        byte modifiers = reportData[0];
        if (modifiers != 0) {
            result.append("Modifiers: ");
            if ((modifiers & 0x01) != 0) result.append("LEFT_CTRL ");
            if ((modifiers & 0x02) != 0) result.append("LEFT_SHIFT ");
            if ((modifiers & 0x04) != 0) result.append("LEFT_ALT ");
            if ((modifiers & 0x08) != 0) result.append("LEFT_GUI ");
            if ((modifiers & 0x10) != 0) result.append("RIGHT_CTRL ");
            if ((modifiers & 0x20) != 0) result.append("RIGHT_SHIFT ");
            if ((modifiers & 0x40) != 0) result.append("RIGHT_ALT ");
            if ((modifiers & 0x80) != 0) result.append("RIGHT_GUI ");
        } else {
            result.append("No modifiers pressed. ");
        }
        
        // Next bytes are the keys pressed (up to 6 in a standard report)
        boolean keysPressed = false;
        result.append("Keys: ");
        
        for (int i = 1; i < reportData.length && i < 8; i++) {
            if (reportData[i] != 0) {
                keysPressed = true;
                result.append(getKeyNameFromCode(reportData[i] & 0xFF)).append(" ");
            }
        }
        
        if (!keysPressed) {
            result.append("No keys pressed");
        }
        
        return result.toString();
    }
    
    /**
     * Convert a HID keyboard key code to its name
     * 
     * @param keyCode HID key code
     * @return Key name
     */
    private String getKeyNameFromCode(int keyCode) {
        // Standard HID keyboard codes (incomplete list - add more as needed)
        switch (keyCode) {
            case 0x00: return "NONE";
            case 0x04: return "A";
            case 0x05: return "B";
            case 0x06: return "C";
            case 0x07: return "D";
            case 0x08: return "E";
            case 0x09: return "F";
            case 0x0A: return "G";
            case 0x0B: return "H";
            case 0x0C: return "I";
            case 0x0D: return "J";
            case 0x0E: return "K";
            case 0x0F: return "L";
            case 0x10: return "M";
            case 0x11: return "N";
            case 0x12: return "O";
            case 0x13: return "P";
            case 0x14: return "Q";
            case 0x15: return "R";
            case 0x16: return "S";
            case 0x17: return "T";
            case 0x18: return "U";
            case 0x19: return "V";
            case 0x1A: return "W";
            case 0x1B: return "X";
            case 0x1C: return "Y";
            case 0x1D: return "Z";
            case 0x1E: return "1";
            case 0x1F: return "2";
            case 0x20: return "3";
            case 0x21: return "4";
            case 0x22: return "5";
            case 0x23: return "6";
            case 0x24: return "7";
            case 0x25: return "8";
            case 0x26: return "9";
            case 0x27: return "0";
            case 0x28: return "ENTER";
            case 0x29: return "ESCAPE";
            case 0x2A: return "BACKSPACE";
            case 0x2B: return "TAB";
            case 0x2C: return "SPACE";
            case 0x2D: return "MINUS";
            case 0x2E: return "EQUALS";
            case 0x2F: return "OPEN_BRACKET";
            case 0x30: return "CLOSE_BRACKET";
            case 0x31: return "BACKSLASH";
            case 0x33: return "SEMICOLON";
            case 0x34: return "QUOTE";
            case 0x35: return "GRAVE";
            case 0x36: return "COMMA";
            case 0x37: return "PERIOD";
            case 0x38: return "SLASH";
            case 0x39: return "CAPS_LOCK";
            case 0x4F: return "RIGHT_ARROW";
            case 0x50: return "LEFT_ARROW";
            case 0x51: return "DOWN_ARROW";
            case 0x52: return "UP_ARROW";
            default: return String.format("KEY(0x%02X)", keyCode);
        }
    }
}
