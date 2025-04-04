package com.example.blehid.core.report;

import android.util.Log;
import com.example.blehid.core.AbstractReportHandler;
import com.example.blehid.core.HidConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for HID report handlers.
 * Provides a central place to register and access report handlers by report ID.
 */
public class ReportRegistry {
    private static final String TAG = "ReportRegistry";
    
    private final Map<Byte, AbstractReportHandler<?>> handlers = new HashMap<>();
    
    /**
     * Registers a report handler.
     *
     * @param reportId Report ID
     * @param handler Handler implementation
     */
    public void registerHandler(byte reportId, AbstractReportHandler<?> handler) {
        if (handler == null) {
            Log.w(TAG, "Attempted to register null handler for report ID " + reportId);
            return;
        }
        
        handlers.put(reportId, handler);
        Log.d(TAG, "Registered handler for report ID " + reportId + ": " + handler.getClass().getSimpleName());
    }
    
    /**
     * Gets a handler by report ID.
     *
     * @param reportId Report ID
     * @return The handler, or null if not found
     */
    public AbstractReportHandler<?> getHandler(byte reportId) {
        AbstractReportHandler<?> handler = handlers.get(reportId);
        if (handler == null) {
            Log.w(TAG, "No handler registered for report ID " + reportId);
        }
        return handler;
    }
    
    /**
     * Gets the mouse report handler.
     *
     * @return The mouse report handler, or null if not registered
     */
    public AbstractReportHandler<?> getMouseReportHandler() {
        return getHandler(HidConstants.REPORT_ID_MOUSE);
    }
    
    /**
     * Gets the keyboard report handler.
     *
     * @return The keyboard report handler, or null if not registered
     */
    public AbstractReportHandler<?> getKeyboardReportHandler() {
        return getHandler(HidConstants.REPORT_ID_KEYBOARD);
    }
    
    /**
     * Gets the consumer report handler.
     *
     * @return The consumer report handler, or null if not registered
     */
    public AbstractReportHandler<?> getConsumerReportHandler() {
        return getHandler(HidConstants.REPORT_ID_CONSUMER);
    }
    
    /**
     * Checks if a handler is registered for the given report ID.
     *
     * @param reportId Report ID
     * @return true if a handler is registered, false otherwise
     */
    public boolean hasHandler(byte reportId) {
        return handlers.containsKey(reportId);
    }
    
    /**
     * Gets the number of registered handlers.
     *
     * @return Number of handlers
     */
    public int getHandlerCount() {
        return handlers.size();
    }
}
