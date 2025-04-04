package com.inventonater.blehid.util;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Utility class for logging.
 * Provides methods for formatting log messages, hexadecimal data dumps, and exception details.
 */
public class LogUtils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    
    /**
     * Converts a byte array to a hex string.
     *
     * @param bytes Byte array to convert
     * @return Formatted hex string
     */
    public static String byteArrayToHex(byte[] bytes) {
        if (bytes == null) {
            return "null";
        }
        
        if (bytes.length == 0) {
            return "empty";
        }
        
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            builder.append("0x")
                   .append(HEX_ARRAY[v >>> 4])
                   .append(HEX_ARRAY[v & 0x0F]);
            
            if (i < bytes.length - 1) {
                builder.append(" ");
            }
        }
        
        return builder.toString();
    }
    
    /**
     * Gets detailed information about an exception, including its stack trace.
     *
     * @param throwable The exception
     * @return Formatted exception details
     */
    public static String getExceptionDetails(Throwable throwable) {
        if (throwable == null) {
            return "null";
        }
        
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        
        return throwable.getMessage() + "\n" + sw.toString();
    }
    
    /**
     * Formats a message with indentation for hierarchical logging.
     *
     * @param indent Number of indentation levels
     * @param message The message to format
     * @return Indented message
     */
    public static String indentMessage(int indent, String message) {
        StringBuilder builder = new StringBuilder();
        
        for (int i = 0; i < indent; i++) {
            builder.append("  ");
        }
        
        builder.append(message);
        return builder.toString();
    }
    
    /**
     * Formats a boolean value for logging, with a descriptive label.
     *
     * @param label The label
     * @param value The boolean value
     * @return Formatted string (e.g., "Label: true")
     */
    public static String formatBoolean(String label, boolean value) {
        return label + ": " + (value ? "true" : "false");
    }
    
    /**
     * Formats an integer value for logging, with a descriptive label.
     *
     * @param label The label
     * @param value The integer value
     * @return Formatted string (e.g., "Label: 42")
     */
    public static String formatInt(String label, int value) {
        return label + ": " + value;
    }
    
    /**
     * Formats a device address for logging.
     *
     * @param address MAC address, can be null
     * @return Formatted address or "N/A" if null
     */
    public static String formatDeviceAddress(String address) {
        return address != null ? address : "N/A";
    }
}
