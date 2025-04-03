package com.inventonater.hid.app.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.format.DateFormat
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.inventonater.hid.app.R
import com.inventonater.hid.core.BleHid
import com.inventonater.hid.core.api.MouseButton
import java.util.Date

/**
 * Activity for controlling mouse movements and button clicks.
 * Uses a simplified UI for demonstrating BLE HID mouse capabilities.
 */
class SimpleMouseActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SimpleMouseActivity"
        
        // Sensitivity of mouse movement (higher = more sensitive)
        private const val MOVEMENT_SENSITIVITY = 0.5f
    }
    
    // UI components
    private lateinit var touchpadArea: View
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var middleButton: Button
    private lateinit var statusText: TextView
    private lateinit var connectionText: TextView
    private lateinit var advertisingButton: Button
    private lateinit var logText: TextView
    
    // Tracking for mouse movement
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    
    // StringBuilder for log entries
    private val logEntries = StringBuilder()
    
    // Bluetooth state receiver
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
            val deviceInfo = device?.let { dev ->
                "${dev.name ?: "unnamed"} (${dev.address})"
            } ?: "unknown device"
            
            when (action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    when (state) {
                        BluetoothAdapter.STATE_OFF -> addLogEntry("BLUETOOTH: Turned OFF")
                        BluetoothAdapter.STATE_TURNING_OFF -> addLogEntry("BLUETOOTH: Turning OFF")
                        BluetoothAdapter.STATE_ON -> addLogEntry("BLUETOOTH: Turned ON")
                        BluetoothAdapter.STATE_TURNING_ON -> addLogEntry("BLUETOOTH: Turning ON")
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    addLogEntry("DEVICE FOUND: $deviceInfo")
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)
                    addLogEntry("BOND STATE: $deviceInfo changed from $prevBondState to $bondState")
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    addLogEntry("ACL CONNECTED: $deviceInfo")
                    updateStatus()
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    addLogEntry("ACL DISCONNECTED: $deviceInfo")
                    updateStatus()
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mouse)
        
        // Initialize UI components
        touchpadArea = findViewById(R.id.touchpadArea)
        leftButton = findViewById(R.id.leftButton)
        rightButton = findViewById(R.id.rightButton)
        middleButton = findViewById(R.id.middleButton)
        statusText = findViewById(R.id.statusText)
        connectionText = findViewById(R.id.connectionText)
        advertisingButton = findViewById(R.id.advertisingButton)
        logText = findViewById(R.id.logText)
        
        // Set up advertising button
        advertisingButton.setOnClickListener { toggleAdvertising() }
        
        // Setup touch listeners
        touchpadArea.setOnTouchListener(this::onMousepadTouch)
        
        // Setup button click listeners for mouse buttons
        leftButton.setOnClickListener { handleButtonClick(MouseButton.LEFT) }
        rightButton.setOnClickListener { handleButtonClick(MouseButton.RIGHT) }
        middleButton.setOnClickListener { handleButtonClick(MouseButton.MIDDLE) }
        
        // Initialize BLE HID functionality
        initializeBleHid()
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
        
        // Register for Bluetooth state changes
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(bluetoothReceiver, filter)
        
        addLogEntry("BLUETOOTH: Receiver registered for state changes")
    }
    
    override fun onPause() {
        super.onPause()
        // Stop advertising when the activity is not visible
        if (BleHid.isAdvertising()) {
            BleHid.stopAdvertising()
            updateAdvertisingStatus(false)
        }
        
        // Unregister Bluetooth receiver
        try {
            unregisterReceiver(bluetoothReceiver)
            addLogEntry("BLUETOOTH: Receiver unregistered")
        } catch (e: IllegalArgumentException) {
            // Receiver not registered
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (BleHid.isAdvertising()) {
            BleHid.stopAdvertising()
        }
    }
    
    /**
     * Initializes the BLE HID functionality.
     */
    private fun initializeBleHid() {
        val initialized = BleHid.isInitialized()
        if (initialized) {
            // Ensure mouse service is specifically activated
            ensureMouseServiceActive()
            
            statusText.setText(R.string.ready)
            advertisingButton.isEnabled = true
            addLogEntry("BLE HID initialized successfully")
        } else {
            statusText.setText(R.string.initialization_failed)
            advertisingButton.isEnabled = false
            addLogEntry("BLE HID initialization FAILED")
        }
    }
    
    /**
     * Ensures that the mouse service is properly activated.
     */
    private fun ensureMouseServiceActive() {
        try {
            // Deactivate keyboard service if active to prevent profile confusion
            BleHid.manager?.deactivateService("keyboard")
            
            // Activate mouse service
            val mouseActivated = BleHid.manager?.activateService("mouse") ?: false
            if (mouseActivated) {
                addLogEntry("Mouse service activated successfully")
            } else {
                addLogEntry("Failed to activate mouse service")
                Toast.makeText(this, "Failed to activate mouse service", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            addLogEntry("Error activating mouse service: ${e.message}")
        }
    }
    
    /**
     * Adds a timestamped entry to the log.
     */
    private fun addLogEntry(entry: String) {
        val timestamp = DateFormat.format("HH:mm:ss", Date()).toString()
        val logEntry = "$timestamp - $entry\n"
        
        logEntries.insert(0, logEntry) // Add to the beginning
        
        // Trim if too long
        if (logEntries.length > 2000) {
            logEntries.setLength(2000)
        }
        
        logText.text = logEntries.toString()
    }
    
    private fun onMousepadTouch(v: View, event: MotionEvent): Boolean {
        if (!BleHid.isConnected()) {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show()
            return false
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Store initial position
                lastX = event.x
                lastY = event.y
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Calculate movement delta
                val deltaX = event.x - lastX
                val deltaY = event.y - lastY
                
                // Update last position
                lastX = event.x
                lastY = event.y
                
                // Apply sensitivity and convert to int values
                val moveX = (deltaX * MOVEMENT_SENSITIVITY).toInt()
                val moveY = (deltaY * MOVEMENT_SENSITIVITY).toInt()
                
                // Only send if there's actual movement
                if (moveX != 0 || moveY != 0) {
                    val success = BleHid.moveMouse(moveX, moveY)
                    
                    if (!success) {
                        addLogEntry("MOUSE MOVE: Failed to send movement ($moveX, $moveY)")
                    }
                }
                return true
            }
            
            else -> return false
        }
    }
    
    private fun handleButtonClick(button: MouseButton) {
        if (!BleHid.isConnected()) {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Send mouse button click
        val success = BleHid.clickMouseButton(button)
        
        if (success) {
            addLogEntry("MOUSE BUTTON: ${button.name} clicked")
        } else {
            addLogEntry("MOUSE BUTTON: Failed to click ${button.name}")
            Toast.makeText(this, "Failed to send mouse button click", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Toggles BLE advertising on/off.
     */
    private fun toggleAdvertising() {
        // Check if we're already advertising
        val isCurrentlyAdvertising = BleHid.isAdvertising()
        if (isCurrentlyAdvertising) {
            BleHid.stopAdvertising()
            updateAdvertisingStatus(false)
            addLogEntry("ADVERTISING: Stopped")
        } else {
            val result = BleHid.startAdvertising()
            updateAdvertisingStatus(result)
            
            if (result) {
                addLogEntry("ADVERTISING: Started")
            } else {
                addLogEntry("ADVERTISING: Failed to start")
                Toast.makeText(this, R.string.advertising_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * Updates the advertising button text and status.
     * 
     * @param isAdvertising Whether advertising is active
     */
    private fun updateAdvertisingStatus(isAdvertising: Boolean) {
        advertisingButton.setText(if (isAdvertising) R.string.stop_advertising else R.string.start_advertising)
        statusText.setText(if (isAdvertising) R.string.advertising else R.string.ready)
    }
    
    private fun updateStatus() {
        if (BleHid.isConnected()) {
            val device = BleHid.getConnectedDevice()
            val deviceName = device?.name ?: device?.address ?: "Unknown"
            connectionText.text = getString(R.string.connected_to, deviceName)
            statusText.text = "Status: Connected"
            enableControls(true)
        } else {
            connectionText.setText(R.string.not_connected)
            enableControls(false)
        }
    }
    
    private fun enableControls(enabled: Boolean) {
        touchpadArea.isEnabled = enabled
        leftButton.isEnabled = enabled
        rightButton.isEnabled = enabled
        middleButton.isEnabled = enabled
        
        // Change the alpha for visual feedback
        val alpha = if (enabled) 1.0f else 0.5f
        touchpadArea.alpha = alpha
        leftButton.alpha = alpha
        rightButton.alpha = alpha
        middleButton.alpha = alpha
    }
}
