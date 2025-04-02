package com.inventonater.hid.core.internal.diagnostics

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.os.Environment
import android.util.Log
import com.inventonater.hid.core.api.ConnectionEvent
import com.inventonater.hid.core.api.ConnectionEventListener
import com.inventonater.hid.core.api.ConnectionEventType
import com.inventonater.hid.core.api.ConnectionMonitor
import com.inventonater.hid.core.api.ConnectionState
import com.inventonater.hid.core.api.DiagnosticsConfig
import com.inventonater.hid.core.api.DiagnosticsManager
import com.inventonater.hid.core.api.GattOperation
import com.inventonater.hid.core.api.LogEntry
import com.inventonater.hid.core.api.LogLevel
import com.inventonater.hid.core.api.LogManager
import com.inventonater.hid.core.api.LogSink
import com.inventonater.hid.core.api.Logger
import com.inventonater.hid.core.api.MetricValue
import com.inventonater.hid.core.api.PerformanceStats
import com.inventonater.hid.core.api.PerformanceTracker
import com.inventonater.hid.core.api.ReportDirection
import com.inventonater.hid.core.api.ReportListener
import com.inventonater.hid.core.api.ReportMonitor
import com.inventonater.hid.core.api.ReportRecord
import com.inventonater.hid.core.api.ReportType
import com.inventonater.hid.core.api.SessionSummary
import com.inventonater.hid.core.api.TimeRange
import android.bluetooth.BluetoothAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import java.io.FileWriter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * Implementation of DiagnosticsManager that provides logging, monitoring,
 * and performance tracking capabilities.
 *
 * @property context Application context
 */
class DiagnosticsManagerImpl(
    private val context: Context
) : DiagnosticsManager {
    
    // Singletons for sub-managers
    override val logManager: LogManager = LogManagerImpl()
    override val reportMonitor: ReportMonitor = ReportMonitorImpl()
    override val connectionMonitor: ConnectionMonitor = ConnectionMonitorImpl()
    override val performanceTracker: PerformanceTracker = PerformanceTrackerImpl()
    
    // Logger for this class
    private val logger = logManager.getLogger("DiagnosticsManager")
    
    // Scope for asynchronous operations
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Current configuration
    private lateinit var config: DiagnosticsConfig
    
    // Current session
    private var currentSessionId: String? = null
    private var currentSessionName: String? = null
    private var sessionStartTime: Instant? = null
    
    // Session counters
    private val sessionConnections = AtomicLong(0)
    private val sessionReportsSent = AtomicLong(0)
    private val sessionReportsReceived = AtomicLong(0)
    private val sessionErrors = AtomicLong(0)
    private val sessionWarnings = AtomicLong(0)
    
    // Initialized flag
    private var initialized = false
    
    override fun initialize(config: DiagnosticsConfig): Boolean {
        if (initialized) {
            logger.warn("Already initialized")
            return true
        }
        
        logger.info("Initializing diagnostics manager")
        
        this.config = config
        
        // Set log level
        logManager.setLogLevel(config.logLevel)
        
        // Add log sinks
        if (config.logLevel != LogLevel.ERROR) {
            // Add Android log sink
            logManager.addLogSink(AndroidLogSink())
            
            // Add file log sink if enabled
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                val logDir = File(context.getExternalFilesDir(null), "logs")
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }
                
                if (logDir.exists()) {
                    val logFile = File(logDir, "blehid.log")
                    logManager.addLogSink(FileLogSink(logFile, config.maxLogFileSizeBytes))
                } else {
                    logger.error("Failed to create log directory")
                }
            }
        }
        
        initialized = true
        logger.info("Diagnostics manager initialized")
        
        return true
    }
    
    override fun startSession(sessionName: String?): String {
        logger.info("Starting diagnostic session: $sessionName")
        
        // Generate session ID
        val sessionId = UUID.randomUUID().toString()
        
        // Store session info
        currentSessionId = sessionId
        currentSessionName = sessionName
        sessionStartTime = Instant.now()
        
        // Reset counters
        sessionConnections.set(0)
        sessionReportsSent.set(0)
        sessionReportsReceived.set(0)
        sessionErrors.set(0)
        sessionWarnings.set(0)
        
        logger.info("Diagnostic session started: $sessionId")
        return sessionId
    }
    
    override fun endSession(sessionId: String): SessionSummary {
        logger.info("Ending diagnostic session: $sessionId")
        
        if (currentSessionId != sessionId) {
            logger.warn("Session ID mismatch: $sessionId vs $currentSessionId")
        }
        
        // Create summary
        val endTime = Instant.now()
        val summary = SessionSummary(
            sessionId = sessionId,
            sessionName = currentSessionName,
            startTime = sessionStartTime ?: endTime,
            endTime = endTime,
            connectionEvents = sessionConnections.get().toInt(),
            reportsSent = sessionReportsSent.get().toInt(),
            reportsReceived = sessionReportsReceived.get().toInt(),
            errors = sessionErrors.get().toInt(),
            warnings = sessionWarnings.get().toInt()
        )
        
        // Clear session info
        currentSessionId = null
        currentSessionName = null
        sessionStartTime = null
        
        logger.info("Diagnostic session ended: $sessionId")
        return summary
    }
    
    override fun exportDiagnostics(sessionId: String?, format: String): String {
        logger.info("Exporting diagnostics: sessionId=$sessionId, format=$format")
        
        // Create export directory
        val exportDir = File(context.getExternalFilesDir(null), "exports")
        if (!exportDir.exists()) {
            exportDir.mkdirs()
        }
        
        // Generate export file
        val timestamp = Instant.now()
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        
        val exportFile = File(exportDir, "blehid_diagnostics_${timestamp}.$format")
        
        try {
            FileWriter(exportFile).use { writer ->
                when (format.lowercase()) {
                    "json" -> exportJsonFormat(writer, sessionId)
                    "csv" -> exportCsvFormat(writer, sessionId)
                    else -> exportTextFormat(writer, sessionId)
                }
            }
            
            logger.info("Diagnostics exported to: ${exportFile.absolutePath}")
            return exportFile.absolutePath
        } catch (e: Exception) {
            logger.error("Failed to export diagnostics", e)
            return "Export failed: ${e.message}"
        }
    }
    
    private fun exportJsonFormat(writer: FileWriter, sessionId: String?) {
        // Simple JSON format export
        writer.write("{\n")
        writer.write("  \"sessionId\": \"${sessionId ?: "all"}\",\n")
        writer.write("  \"timestamp\": \"${Instant.now()}\",\n")
        writer.write("  \"device\": \"${android.os.Build.MODEL}\",\n")
        writer.write("  \"androidVersion\": \"${android.os.Build.VERSION.RELEASE}\",\n")
        writer.write("  \"summary\": {\n")
        writer.write("    \"reportsSent\": ${sessionReportsSent.get()},\n")
        writer.write("    \"reportsReceived\": ${sessionReportsReceived.get()},\n")
        writer.write("    \"connectionEvents\": ${sessionConnections.get()},\n")
        writer.write("    \"errors\": ${sessionErrors.get()},\n")
        writer.write("    \"warnings\": ${sessionWarnings.get()}\n")
        writer.write("  }\n")
        writer.write("}\n")
    }
    
    private fun exportCsvFormat(writer: FileWriter, sessionId: String?) {
        // Simple CSV format export
        writer.write("SessionId,Timestamp,Device,AndroidVersion,ReportsSent,ReportsReceived,ConnectionEvents,Errors,Warnings\n")
        writer.write("${sessionId ?: "all"},${Instant.now()},${android.os.Build.MODEL},${android.os.Build.VERSION.RELEASE},${sessionReportsSent.get()},${sessionReportsReceived.get()},${sessionConnections.get()},${sessionErrors.get()},${sessionWarnings.get()}\n")
    }
    
    private fun exportTextFormat(writer: FileWriter, sessionId: String?) {
        // Simple text format export
        writer.write("BLE HID DIAGNOSTICS EXPORT\n")
        writer.write("===========================\n\n")
        writer.write("Session ID: ${sessionId ?: "all"}\n")
        writer.write("Timestamp: ${Instant.now()}\n")
        writer.write("Device: ${android.os.Build.MODEL}\n")
        writer.write("Android Version: ${android.os.Build.VERSION.RELEASE}\n\n")
        writer.write("SUMMARY\n")
        writer.write("-------\n")
        writer.write("Reports Sent: ${sessionReportsSent.get()}\n")
        writer.write("Reports Received: ${sessionReportsReceived.get()}\n")
        writer.write("Connection Events: ${sessionConnections.get()}\n")
        writer.write("Errors: ${sessionErrors.get()}\n")
        writer.write("Warnings: ${sessionWarnings.get()}\n")
    }
    
    override fun close() {
        if (!initialized) {
            return
        }
        
        logger.info("Closing diagnostics manager")
        
        // Close log sinks
        (logManager as LogManagerImpl).close()
        
        initialized = false
    }
    
    /**
     * Log manager implementation.
     */
    private inner class LogManagerImpl : LogManager {
        private val logLevel = MutableStateFlow(LogLevel.INFO)
        private val logSinks = CopyOnWriteArrayList<LogSink>()
        
        override fun log(
            level: LogLevel,
            tag: String,
            message: String,
            throwable: Throwable?,
            metadata: Map<String, Any>?
        ) {
            // Check log level
            if (level.ordinal > logLevel.value.ordinal) {
                return
            }
            
            // Update session counters
            if (level == LogLevel.ERROR) {
                sessionErrors.incrementAndGet()
            } else if (level == LogLevel.WARNING) {
                sessionWarnings.incrementAndGet()
            }
            
            // Create log entry
            val entry = LogEntry(
                timestamp = Instant.now(),
                level = level,
                tag = tag,
                message = message,
                throwable = throwable,
                metadata = metadata
            )
            
            // Process log sinks
            for (sink in logSinks) {
                try {
                    sink.processLog(entry)
                } catch (e: Exception) {
                    // Use Android logging as fallback
                    Log.e("LogManager", "Error processing log in sink: ${e.message}")
                }
            }
        }
        
        override fun getLogger(tag: String): Logger {
            return LoggerImpl(this, tag)
        }
        
        override fun setLogLevel(level: LogLevel) {
            logLevel.value = level
        }
        
        override fun addLogSink(sink: LogSink) {
            logSinks.add(sink)
        }
        
        override fun removeLogSink(sink: LogSink) {
            logSinks.remove(sink)
        }
        
        fun close() {
            for (sink in logSinks) {
                try {
                    sink.close()
                } catch (e: Exception) {
                    Log.e("LogManager", "Error closing log sink: ${e.message}")
                }
            }
            logSinks.clear()
        }
    }
    
    /**
     * Logger implementation.
     */
    private class LoggerImpl(
        private val logManager: LogManager,
        private val tag: String
    ) : Logger {
        override fun error(message: String, throwable: Throwable?, metadata: Map<String, Any>?) {
            logManager.log(LogLevel.ERROR, tag, message, throwable, metadata)
        }
        
        override fun warn(message: String, throwable: Throwable?, metadata: Map<String, Any>?) {
            logManager.log(LogLevel.WARNING, tag, message, throwable, metadata)
        }
        
        override fun info(message: String, metadata: Map<String, Any>?) {
            logManager.log(LogLevel.INFO, tag, message, null, metadata)
        }
        
        override fun debug(message: String, metadata: Map<String, Any>?) {
            logManager.log(LogLevel.DEBUG, tag, message, null, metadata)
        }
        
        override fun verbose(message: String, metadata: Map<String, Any>?) {
            logManager.log(LogLevel.DEBUG, tag, message, null, metadata)
        }
    }
    
    /**
     * Android log sink implementation.
     */
    private class AndroidLogSink : LogSink {
        override fun processLog(entry: LogEntry) {
            val priority = when (entry.level) {
                LogLevel.ERROR -> Log.ERROR
                LogLevel.WARNING -> Log.WARN
                LogLevel.INFO -> Log.INFO
                LogLevel.DEBUG -> Log.DEBUG
                LogLevel.NONE -> Log.INFO 
                else -> Log.VERBOSE
            }
            
            val message = entry.message
            
            if (entry.throwable != null) {
                Log.println(priority, entry.tag, "$message\n${Log.getStackTraceString(entry.throwable)}")
            } else {
                Log.println(priority, entry.tag, message)
            }
        }
        
        override fun close() {
            // Nothing to close
        }
    }
    
    /**
     * File log sink implementation.
     */
    private class FileLogSink(
        private val logFile: File,
        private val maxSizeBytes: Long
    ) : LogSink {
        private val formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault())
        
        override fun processLog(entry: LogEntry) {
            try {
                // Check file size
                if (logFile.exists() && logFile.length() > maxSizeBytes) {
                    // Rotate log file
                    val backupFile = File(logFile.parentFile, "${logFile.name}.1")
                    if (backupFile.exists()) {
                        backupFile.delete()
                    }
                    logFile.renameTo(backupFile)
                }
                
                // Ensure parent directory exists
                if (!logFile.parentFile.exists()) {
                    logFile.parentFile.mkdirs()
                }
                
                // Format log entry
                val levelStr = entry.level.toString().substring(0, 1)
                val timestamp = formatter.format(entry.timestamp)
                val message = entry.message.replace("\n", "\\n")
                
                // Write to file
                val logLine = "$timestamp $levelStr/${entry.tag}: $message"
                
                FileWriter(logFile, true).use { writer ->
                    writer.write("$logLine\n")
                    
                    if (entry.throwable != null) {
                        writer.write("${Log.getStackTraceString(entry.throwable)}\n")
                    }
                }
            } catch (e: Exception) {
                Log.e("FileLogSink", "Error writing to log file: ${e.message}")
            }
        }
        
        override fun close() {
            // Nothing to close
        }
    }
    
    /**
     * Report monitor implementation.
     */
    private inner class ReportMonitorImpl : ReportMonitor {
        private val logger = logManager.getLogger("ReportMonitor")
        private val reportListeners = CopyOnWriteArrayList<ReportListener>()
        private val reportHistory = CopyOnWriteArrayList<ReportRecord>()
        private val reportFlow = MutableSharedFlow<ReportRecord>(extraBufferCapacity = 10)
        
        override fun trackReport(
            reportType: ReportType,
            reportId: Int,
            data: ByteArray,
            direction: ReportDirection
        ) {
            // Create record
            val record = ReportRecord(
                timestamp = Instant.now(),
                reportType = reportType,
                reportId = reportId,
                data = data.clone(),
                direction = direction,
                success = true,
                characteristic = null
            )
            
            // Update session counters
            if (direction == ReportDirection.SENT) {
                sessionReportsSent.incrementAndGet()
            } else {
                sessionReportsReceived.incrementAndGet()
            }
            
            // Log the report
            val dirStr = if (direction == ReportDirection.SENT) "→" else "←"
            logger.debug(
                "$dirStr Report $reportId (${reportType.name}): " +
                    data.joinToString(" ") { "%02X".format(it) }
            )
            
            // Add to history
            reportHistory.add(record)
            
            // Emit to flow
            try {
                reportFlow.tryEmit(record)
            } catch (e: Exception) {
                logger.error("Error emitting report record", e)
            }
            
            // Notify listeners
            for (listener in reportListeners) {
                try {
                    listener.onReportTracked(record)
                } catch (e: Exception) {
                    logger.error("Error notifying report listener", e)
                }
            }
        }
        
        override fun getReportHistory(
            reportType: ReportType?,
            timeRange: TimeRange?
        ): List<ReportRecord> {
            var filtered = reportHistory.toList()
            
            // Filter by report type
            if (reportType != null) {
                filtered = filtered.filter { it.reportType == reportType }
            }
            
            // Filter by time range
            if (timeRange != null) {
                filtered = filtered.filter {
                    it.timestamp.isAfter(timeRange.start) && it.timestamp.isBefore(timeRange.end)
                }
            }
            
            return filtered
        }
        
        override fun subscribeToReports(listener: ReportListener) {
            reportListeners.add(listener)
        }
        
        override fun unsubscribeFromReports(listener: ReportListener) {
            reportListeners.remove(listener)
        }
        
        override fun getReportFlow(): Flow<ReportRecord> {
            return reportFlow.asSharedFlow()
        }
    }
    
    /**
     * Connection monitor implementation.
     */
    private inner class ConnectionMonitorImpl : ConnectionMonitor {
        private val logger = logManager.getLogger("ConnectionMonitor")
        private val connectionListeners = CopyOnWriteArrayList<ConnectionEventListener>()
        private val connectionHistory = CopyOnWriteArrayList<ConnectionEvent>()
        private val connectionFlow = MutableSharedFlow<ConnectionEvent>(extraBufferCapacity = 10)
        
        override fun trackConnectionState(device: BluetoothDevice, state: ConnectionState) {
            // Convert state to event type
            val eventType = when (state) {
                is ConnectionState.Connected -> ConnectionEventType.CONNECTED
                is ConnectionState.Disconnected -> ConnectionEventType.DISCONNECTED
                is ConnectionState.Failed -> ConnectionEventType.CONNECTION_FAILED
                else -> return // Ignore other states
            }
            
            // Create event
            val event = ConnectionEvent(
                timestamp = Instant.now(),
                device = device,
                eventType = eventType,
                details = if (state is ConnectionState.Failed) state.reason else null
            )
            
            // Update session counters
            sessionConnections.incrementAndGet()
            
            // Log the event
            logger.info("Connection state change: ${eventType.name}, device: ${device.address}")
            
            // Add to history
            connectionHistory.add(event)
            
            // Emit to flow
            try {
                connectionFlow.tryEmit(event)
            } catch (e: Exception) {
                logger.error("Error emitting connection event", e)
            }
            
            // Notify listeners
            for (listener in connectionListeners) {
                try {
                    listener.onConnectionEvent(event)
                } catch (e: Exception) {
                    logger.error("Error notifying connection listener", e)
                }
            }
        }
        
        override fun trackGattOperation(operation: GattOperation) {
            // Convert operation to event type
            val (eventType, device, details) = when (operation) {
                is GattOperation.ServiceAdded -> Triple(
                    ConnectionEventType.SERVICE_ADDED,
                    null,
                    "Service: ${operation.service.uuid}"
                )
                is GattOperation.ServiceRemoved -> Triple(
                    ConnectionEventType.SERVICE_REMOVED,
                    null,
                    "Service: ${operation.serviceUuid}"
                )
                is GattOperation.CharacteristicRead -> Triple(
                    ConnectionEventType.CHARACTERISTIC_READ,
                    operation.device,
                    "Characteristic: ${operation.characteristic.uuid}"
                )
                is GattOperation.CharacteristicWrite -> Triple(
                    ConnectionEventType.CHARACTERISTIC_WRITE,
                    operation.device,
                    "Characteristic: ${operation.characteristic.uuid}"
                )
                is GattOperation.NotificationSent -> Triple(
                    if (operation.success) ConnectionEventType.NOTIFICATION_SENT else ConnectionEventType.NOTIFICATION_FAILED,
                    null,
                    "Characteristic: ${operation.characteristic.uuid}"
                )
            }
            
            // Skip if no device and not a service operation
            if (device == null && eventType != ConnectionEventType.SERVICE_ADDED && eventType != ConnectionEventType.SERVICE_REMOVED) {
                return
            }
            
            // Create event
            val event = ConnectionEvent(
                timestamp = Instant.now(),
                device = device ?: BluetoothAdapter.getDefaultAdapter()?.getRemoteDevice("00:00:00:00:00:00") ?: return,
                eventType = eventType,
                details = details
            )
            
            // Update session counters
            sessionConnections.incrementAndGet()
            
            // Log the event
            logger.debug("GATT operation: ${eventType.name}, $details")
            
            // Add to history
            connectionHistory.add(event)
            
            // Emit to flow
            try {
                connectionFlow.tryEmit(event)
            } catch (e: Exception) {
                logger.error("Error emitting connection event", e)
            }
            
            // Notify listeners
            for (listener in connectionListeners) {
                try {
                    listener.onConnectionEvent(event)
                } catch (e: Exception) {
                    logger.error("Error notifying connection listener", e)
                }
            }
        }
        
        override fun getConnectionHistory(device: BluetoothDevice?): List<ConnectionEvent> {
            if (device == null) {
                return connectionHistory.toList()
            }
            
            return connectionHistory.filter { it.device.address == device.address }
        }
        
        override fun subscribeToConnectionEvents(listener: ConnectionEventListener) {
            connectionListeners.add(listener)
        }
        
        override fun unsubscribeFromConnectionEvents(listener: ConnectionEventListener) {
            connectionListeners.remove(listener)
        }
        
        override fun getConnectionEventFlow(): Flow<ConnectionEvent> {
            return connectionFlow.asSharedFlow()
        }
    }
    
    /**
     * Performance tracker implementation.
     */
    private inner class PerformanceTrackerImpl : PerformanceTracker {
        private val logger = logManager.getLogger("PerformanceTracker")
        private val operations = ConcurrentHashMap<String, OperationData>()
        private val metrics = ConcurrentHashMap<String, MutableList<MetricValue>>()
        
        override fun startOperation(operationType: String, details: String?): String {
            val operationId = UUID.randomUUID().toString()
            
            operations[operationId] = OperationData(
                type = operationType,
                details = details,
                startTime = Instant.now()
            )
            
            logger.debug("Started operation: $operationType, id: $operationId")
            return operationId
        }
        
        override fun endOperation(operationId: String, success: Boolean, result: String?) {
            val operation = operations.remove(operationId) ?: run {
                logger.warn("Unknown operation ID: $operationId")
                return
            }
            
            val endTime = Instant.now()
            val duration = endTime.toEpochMilli() - operation.startTime.toEpochMilli()
            
            logger.debug("Ended operation: ${operation.type}, id: $operationId, duration: ${duration}ms, success: $success")
            
            // Record duration metric
            recordMetric("${operation.type}.duration", duration.toDouble(), "ms")
            
            // Record success/failure metric
            recordMetric("${operation.type}.success", if (success) 1.0 else 0.0, "boolean")
        }
        
        override fun recordMetric(metricName: String, value: Double, unit: String) {
            val metricValue = MetricValue(
                timestamp = Instant.now(),
                value = value,
                unit = unit
            )
            
            val metricsList = metrics.getOrPut(metricName) { mutableListOf<MetricValue>() }
            metricsList.add(metricValue)
            
            logger.debug("Recorded metric: $metricName = $value $unit")
        }
        
        override fun getPerformanceStats(
            operationType: String?,
            timeRange: TimeRange?
        ): PerformanceStats {
            val operationCounts = mutableMapOf<String, Int>()
            val operationSuccessRates = mutableMapOf<String, Double>()
            val operationAverageDurations = mutableMapOf<String, Double>()
            
            // Filter metrics
            val filteredMetrics = mutableMapOf<String, MutableList<MetricValue>>()
            
            for ((name, values) in metrics) {
                val filtered = ArrayList<MetricValue>()
                
                // Filter by operation type
                if (operationType != null && !name.startsWith(operationType)) {
                    continue
                }
                
                // Add values that match the time range criteria
                for (value in values) {
                    if (timeRange == null || 
                        (value.timestamp.isAfter(timeRange.start) && 
                         value.timestamp.isBefore(timeRange.end))) {
                        filtered.add(value)
                    }
                }
                
                if (filtered.isNotEmpty()) {
                    filteredMetrics[name] = filtered
                }
            }
            
            // Calculate stats
            val operationTypes = filteredMetrics.keys
                .filter { it.endsWith(".duration") }
                .map { it.removeSuffix(".duration") }
                .toSet()
            
            for (type in operationTypes) {
                val durations = filteredMetrics["$type.duration"] ?: emptyList()
                val successes = filteredMetrics["$type.success"] ?: emptyList()
                
                if (durations.isNotEmpty()) {
                    operationCounts[type] = durations.size
                    operationAverageDurations[type] = durations.map { it.value }.average()
                }
                
                if (successes.isNotEmpty()) {
                    val successCount = successes.count { it.value > 0.5 }
                    operationSuccessRates[type] = successCount.toDouble() / successes.size
                }
            }
            
            return PerformanceStats(
                operationCounts = operationCounts,
                operationSuccessRates = operationSuccessRates,
                operationAverageDurations = operationAverageDurations,
                metrics = filteredMetrics
            )
        }
        
    }
    
    /**
     * Data class for tracking operations in PerformanceTracker
     */
    private class OperationData(
        val type: String,
        val details: String?,
        val startTime: Instant
    )
}
