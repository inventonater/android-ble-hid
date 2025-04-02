package com.inventonater.hid.core.internal.diagnostics

import com.inventonater.hid.core.api.Logger

/**
 * Factory for creating Logger instances.
 * 
 * This interface abstracts the creation of loggers, allowing
 * for different logging implementations to be used.
 */
interface LoggerFactory {
    /**
     * Get a logger for a specific tag.
     *
     * @param tag The tag for the logger
     * @return A logger instance for the specified tag
     */
    fun getLogger(tag: String): Logger
}
