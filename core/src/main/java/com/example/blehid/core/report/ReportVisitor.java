package com.example.blehid.core.report;

/**
 * Visitor interface for HID reports.
 * This enables operations across different report types without 
 * using instanceof checks or type casting.
 *
 * @param <R> The return type of the visit operations
 */
public interface ReportVisitor<R> {
    
    /**
     * Visits a mouse report.
     *
     * @param report The mouse report
     * @return The result of the operation
     */
    R visit(MouseReport report);
    
    /**
     * Visits a keyboard report.
     *
     * @param report The keyboard report
     * @return The result of the operation
     */
    R visit(KeyboardReport report);
    
    /**
     * Visits a consumer control report.
     *
     * @param report The consumer report
     * @return The result of the operation
     */
    R visit(ConsumerReport report);
}
