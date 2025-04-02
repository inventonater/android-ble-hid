package com.inventonater.hid.core.api

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.coroutines.flow.Flow
import java.time.Instant

/**
 * Manages diagnostic functionality for the BLE HID system.
 *
 * This is the central coordinator for all diagnostic-related activities,
 * including logging, report monitoring, and performance tracking.
 */
interface DiagnosticsManager {
    /**
     * The log manager for structured logging.
     */
    val logManager: LogManager
    
    /**
     * The report monitor for tracking HID reports.
     */
    val reportMonitor: ReportMonitor
    
    /**
     * The connection monitor for tracking connection events.
     */
    val connectionMonitor: ConnectionMonitor
    
    /**
     * The performance tracker for monitoring system performance.
     */
    val performanceTracker: PerformanceTracker
    
    /**
     * Initialize the diagnostics manager.
     *
     * @param config Configuration options for diagnostics
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(config: DiagnosticsConfig): Boolean
    
    /**
     * Start a diagnostic session.
     *
     * @param sessionName Optional name for the session
     * @return The session ID
     */
    fun startSession(sessionName: String? = null): String
    
    /**
     * End a diagnostic session.
     *
     * @param sessionId The session ID to end
     * @return A summary of the session
     */
    fun endSession(sessionId: String): SessionSummary
    
    /**
     * Export diagnostic data to a file.
     *
     * @param sessionId Optional session ID to export
     * @param format Export format (e.g., "json", "csv", "text")
     * @return The path to the exported file
     */
    fun exportDiagnostics(sessionId: String? = null, format: String = "json"): String
    
    /**
     * Close the diagnostics manager and release all resources.
     */
    fun close()
}

/**
 * Configuration options for diagnostics.
 */
data class DiagnosticsConfig(
    val logLevel: LogLevel = LogLevel.INFO,
    val enableReportMonitoring: Boolean = true,
    val enableConnectionMonitoring: Boolean = true,
    val enablePerformanceTracking: Boolean = false,
    val logRetentionDays: Int = 7,
    val maxLogFileSizeBytes: Long = 10 * 1024 * 1024 // 10 MB
)

/**
 * Summary of a diagnostic session.
 */
data class SessionSummary(
    val sessionId: String,
    val sessionName: String?,
    val startTime: Instant,
    val endTime: Instant,
    val connectionEvents: Int,
    val reportsSent: Int,
    val reportsReceived: Int,
    val errors: Int,
    val warnings: Int
)

/**
 * Manages structured logging.
 */
interface LogManager {
    /**
     * Log a message.
     *
     * @param level The log level
     * @param tag The log tag
     * @param message The log message
     * @param throwable Optional throwable
     * @param metadata Optional metadata
     */
    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        metadata: Map<String, Any>? = null
    )
    
    /**
     * Get a logger for a specific tag.
     *
     * @param tag The log tag
     * @return A logger for the specified tag
     */
    fun getLogger(tag: String): Logger
    
    /**
     * Set the log level.
     *
     * @param level The new log level
     */
    fun setLogLevel(level: LogLevel)
    
    /**
     * Add a log sink.
     *
     * @param sink The log sink to add
     */
    fun addLogSink(sink: LogSink)
    
    /**
     * Remove a log sink.
     *
     * @param sink The log sink to remove
     */
    fun removeLogSink(sink: LogSink)
}

/**
 * Logger for a specific tag.
 */
interface Logger {
    /**
     * Log an error message.
     *
     * @param message The log message
     * @param throwable Optional throwable
     * @param metadata Optional metadata
     */
    fun error(message: String, throwable: Throwable? = null, metadata: Map<String, Any>? = null)
    
    /**
     * Log a warning message.
     *
     * @param message The log message
     * @param throwable Optional throwable
     * @param metadata Optional metadata
     */
    fun warn(message: String, throwable: Throwable? = null, metadata: Map<String, Any>? = null)
    
    /**
     * Log an info message.
     *
     * @param message The log message
     * @param metadata Optional metadata
     */
    fun info(message: String, metadata: Map<String, Any>? = null)
    
    /**
     * Log a debug message.
     *
     * @param message The log message
     * @param metadata Optional metadata
     */
    fun debug(message: String, metadata: Map<String, Any>? = null)
    
    /**
     * Log a verbose message.
     *
     * @param message The log message
     * @param metadata Optional metadata
     */
    fun verbose(message: String, metadata: Map<String, Any>? = null)
}

// Using the LogLevel enum from core.api.LogLevel instead of redefining it here

/**
 * Sink for log messages.
 */
interface LogSink {
    /**
     * Process a log entry.
     *
     * @param entry The log entry to process
     */
    fun processLog(entry: LogEntry)
    
    /**
     * Close the log sink.
     */
    fun close()
}

/**
 * Log entry.
 */
data class LogEntry(
    val timestamp: Instant,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: Throwable?,
    val metadata: Map<String, Any>?
)

/**
 * Monitors HID reports.
 */
interface ReportMonitor {
    /**
     * Track a report.
     *
     * @param reportType The report type
     * @param reportId The report ID
     * @param data The report data
     * @param direction The report direction
     */
    fun trackReport(
        reportType: ReportType,
        reportId: Int,
        data: ByteArray,
        direction: ReportDirection
    )
    
    /**
     * Get the report history.
     *
     * @param reportType Optional filter by report type
     * @param timeRange Optional filter by time range
     * @return List of report records
     */
    fun getReportHistory(
        reportType: ReportType? = null,
        timeRange: TimeRange? = null
    ): List<ReportRecord>
    
    /**
     * Subscribe to reports.
     *
     * @param listener The listener to subscribe
     */
    fun subscribeToReports(listener: ReportListener)
    
    /**
     * Unsubscribe from reports.
     *
     * @param listener The listener to unsubscribe
     */
    fun unsubscribeFromReports(listener: ReportListener)
    
    /**
     * Get a flow of report records.
     *
     * @return Flow of report records
     */
    fun getReportFlow(): Flow<ReportRecord>
}

/**
 * Report types.
 */
enum class ReportType {
    MOUSE,
    KEYBOARD,
    CONSUMER_CONTROL,
    GAMEPAD,
    UNKNOWN
}

/**
 * Report directions.
 */
enum class ReportDirection {
    SENT,
    RECEIVED
}

/**
 * Record of an HID report.
 */
data class ReportRecord(
    val timestamp: Instant,
    val reportType: ReportType,
    val reportId: Int,
    val data: ByteArray,
    val direction: ReportDirection,
    val success: Boolean,
    val characteristic: BluetoothGattCharacteristic?
)

/**
 * Listener for report events.
 */
interface ReportListener {
    /**
     * Called when a report is tracked.
     *
     * @param record The report record
     */
    fun onReportTracked(record: ReportRecord)
}

/**
 * Time range for filtering.
 */
data class TimeRange(val start: Instant, val end: Instant)

/**
 * Monitors connection events.
 */
interface ConnectionMonitor {
    /**
     * Track a connection state change.
     *
     * @param device The device
     * @param state The new connection state
     */
    fun trackConnectionState(device: BluetoothDevice, state: ConnectionState)
    
    /**
     * Track a GATT operation.
     *
     * @param operation The GATT operation
     */
    fun trackGattOperation(operation: GattOperation)
    
    /**
     * Get the connection history.
     *
     * @param device Optional filter by device
     * @return List of connection events
     */
    fun getConnectionHistory(device: BluetoothDevice? = null): List<ConnectionEvent>
    
    /**
     * Subscribe to connection events.
     *
     * @param listener The listener to subscribe
     */
    fun subscribeToConnectionEvents(listener: ConnectionEventListener)
    
    /**
     * Unsubscribe from connection events.
     *
     * @param listener The listener to unsubscribe
     */
    fun unsubscribeFromConnectionEvents(listener: ConnectionEventListener)
    
    /**
     * Get a flow of connection events.
     *
     * @return Flow of connection events
     */
    fun getConnectionEventFlow(): Flow<ConnectionEvent>
}

/**
 * Connection event.
 */
data class ConnectionEvent(
    val timestamp: Instant,
    val device: BluetoothDevice,
    val eventType: ConnectionEventType,
    val details: String?
)

/**
 * Connection event types.
 */
enum class ConnectionEventType {
    CONNECTED,
    DISCONNECTED,
    CONNECTION_FAILED,
    SERVICE_ADDED,
    SERVICE_REMOVED,
    CHARACTERISTIC_READ,
    CHARACTERISTIC_WRITE,
    DESCRIPTOR_READ,
    DESCRIPTOR_WRITE,
    NOTIFICATION_SENT,
    NOTIFICATION_FAILED
}

/**
 * Listener for connection events.
 */
interface ConnectionEventListener {
    /**
     * Called when a connection event occurs.
     *
     * @param event The connection event
     */
    fun onConnectionEvent(event: ConnectionEvent)
}

/**
 * Tracks system performance.
 */
interface PerformanceTracker {
    /**
     * Start tracking an operation.
     *
     * @param operationType The operation type
     * @param details Optional details about the operation
     * @return The operation ID
     */
    fun startOperation(operationType: String, details: String?): String
    
    /**
     * End tracking an operation.
     *
     * @param operationId The operation ID
     * @param success Whether the operation was successful
     * @param result Optional result of the operation
     */
    fun endOperation(operationId: String, success: Boolean, result: String?)
    
    /**
     * Record a metric.
     *
     * @param metricName The metric name
     * @param value The metric value
     * @param unit The metric unit
     */
    fun recordMetric(metricName: String, value: Double, unit: String)
    
    /**
     * Get performance statistics.
     *
     * @param operationType Optional filter by operation type
     * @param timeRange Optional filter by time range
     * @return Performance statistics
     */
    fun getPerformanceStats(
        operationType: String? = null,
        timeRange: TimeRange? = null
    ): PerformanceStats
}

/**
 * Performance statistics.
 */
data class PerformanceStats(
    val operationCounts: Map<String, Int>,
    val operationSuccessRates: Map<String, Double>,
    val operationAverageDurations: Map<String, Double>,
    val metrics: Map<String, List<MetricValue>>
)

/**
 * Metric value.
 */
data class MetricValue(
    val timestamp: Instant,
    val value: Double,
    val unit: String
)
