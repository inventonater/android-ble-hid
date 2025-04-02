package com.inventonater.hid.core.api

/**
 * Enum representing different log levels for the BLE HID library.
 * Used to control the verbosity of logging.
 */
enum class LogLevel {
    /**
     * Debug level - most verbose, includes detailed information for debugging
     */
    DEBUG,
    
    /**
     * Info level - informational messages about normal operation
     */
    INFO,
    
    /**
     * Warning level - potential issues that don't prevent normal operation
     */
    WARNING,
    
    /**
     * Error level - errors that prevent normal operation
     */
    ERROR,
    
    /**
     * None - disable all logging
     */
    NONE
}
