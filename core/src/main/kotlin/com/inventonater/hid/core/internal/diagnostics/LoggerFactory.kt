package com.inventonater.hid.core.internal.diagnostics

import com.inventonater.hid.core.api.Logger

/**
 * Factory for creating logger instances.
 * 
 * This interface provides a way to create loggers for different components
 * of the application.
 */
interface LoggerFactory {
    /**
     * Get a logger for a specific tag.
     *
     * @param tag The log tag
     * @return A logger for the specified tag
     */
    fun getLogger(tag: String): Logger
}

/**
 * Implementation of LoggerFactory using the LogManager.
 */
class LogManagerLoggerFactory(
    private val logManager: com.inventonater.hid.core.api.LogManager
) : LoggerFactory {
    override fun getLogger(tag: String): Logger {
        return logManager.getLogger(tag)
    }
}
