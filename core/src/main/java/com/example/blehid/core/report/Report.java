package com.example.blehid.core.report;

/**
 * Interface for HID reports.
 * Different report types (mouse, keyboard, consumer) implement this interface.
 */
public interface Report {
    
    /**
     * Gets the report ID.
     *
     * @return The report ID
     */
    byte getReportId();
    
    /**
     * Formats the report data as a byte array to be sent over BLE.
     *
     * @return The formatted report as a byte array
     */
    byte[] format();
    
    /**
     * Accepts a visitor for visitor pattern operations.
     *
     * @param visitor The visitor implementation
     * @param <R> The return type of the visitor operation
     * @return The result of the visitor operation
     */
    <R> R accept(ReportVisitor<R> visitor);
}
