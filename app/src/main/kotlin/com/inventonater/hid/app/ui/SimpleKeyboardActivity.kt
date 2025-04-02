package com.inventonater.hid.app.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.inventonater.hid.app.R
import com.inventonater.hid.core.BleHid
import java.util.Date

/**
 * Activity for testing the BLE HID keyboard functionality.
 * Provides a simple keyboard UI for sending key presses and managing BLE advertising.
 */
class SimpleKeyboardActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SimpleKeyboardActivity"
        
        // HID keyboard scan codes (USB HID spec)
        private const val KEY_A = 0x04
        private const val KEY_B = 0x05
        private const val KEY_C = 0x06
        private const val KEY_D = 0x07
        private const val KEY_E = 0x08
        private const val KEY_F = 0x09
        private const val KEY_G = 0x0A
        private const val KEY_H = 0x0B
        private const val KEY_I = 0x0C
        private const val KEY_J = 0x0D
        private const val KEY_K = 0x0E
        private const val KEY_L = 0x0F
        private const val KEY_M = 0x10
        private const val KEY_N = 0x11
        private const val KEY_O = 0x12
        private const val KEY_P = 0x13
        private const val KEY_Q = 0x14
        private const val KEY_R = 0x15
        private const val KEY_S = 0x16
        private const val KEY_T = 0x17
        private const val KEY_U = 0x18
        private const val KEY_V = 0x19
        private const val KEY_W = 0x1A
        private const val KEY_X = 0x1B
        private const val KEY_Y = 0x1C
        private const val KEY_Z = 0x1D
        private const val KEY_1 = 0x1E
        private const val KEY_2 = 0x1F
        private const val KEY_3 = 0x20
        private const val KEY_4 = 0x21
        private const val KEY_5 = 0x22
        private const val KEY_6 = 0x23
        private const val KEY_7 = 0x24
        private const val KEY_8 = 0x25
        private const val KEY_9 = 0x26
        private const val KEY_0 = 0x27
        private const val KEY_RETURN = 0x28  // Enter
        private const val KEY_ESCAPE = 0x29
        private const val KEY_BACKSPACE = 0x2A
        private const val KEY_TAB = 0x2B
        private const val KEY_SPACE = 0x2C
    }
    
    private lateinit var advertisingButton: Button
    private lateinit var statusText: TextView
    private lateinit var connectionText: TextView
    
    // Diagnostic views
    private lateinit var deviceNameText: TextView
    private lateinit var deviceAddressText: TextView
    private lateinit var pairingStateText: TextView
    private lateinit var logText: TextView
    
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
                BluetoothAdapter.ACTION_SCAN_MODE_CHANGED -> {
                    val scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR)
                    when (scanMode) {
                        BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE -> 
                            addLogEntry("BLUETOOTH: Discoverable")
                        BluetoothAdapter.SCAN_MODE_CONNECTABLE -> 
                            addLogEntry("BLUETOOTH: Connectable but not discoverable")
                        BluetoothAdapter.SCAN_MODE_NONE -> 
                            addLogEntry("BLUETOOTH: Not connectable or discoverable")
                    }
                }
                BluetoothDevice.ACTION_FOUND -> {
                    addLogEntry("DEVICE FOUND: $deviceInfo")
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                    val prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR)
                    val bondStateStr = bondStateToString(bondState)
                    val prevBondStateStr = bondStateToString(prevBondState)
                    addLogEntry("BOND STATE: $deviceInfo $prevBondStateStr -> $bondStateStr")
                    
                    when (bondState) {
                        BluetoothDevice.BOND_BONDED -> updatePairingState("BONDED")
                        BluetoothDevice.BOND_NONE -> updatePairingState("NONE")
                    }
                }
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    addLogEntry("ACL CONNECTED: $deviceInfo")
                    updateConnectionStatus()
                }
                BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                    addLogEntry("ACL DISCONNECT REQUESTED: $deviceInfo")
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    addLogEntry("ACL DISCONNECTED: $deviceInfo")
                    updateConnectionStatus()
                }
                BluetoothDevice.ACTION_PAIRING_REQUEST -> {
                    val variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR)
                    addLogEntry("PAIRING REQUEST: $deviceInfo, variant: $variant")
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keyboard)
        
        // Initialize views
        statusText = findViewById(R.id.statusText)
        connectionText = findViewById(R.id.connectionText)
        advertisingButton = findViewById(R.id.advertisingButton)
        
        // Initialize diagnostic views
        deviceNameText = findViewById(R.id.deviceNameText)
        deviceAddressText = findViewById(R.id.deviceAddressText)
        pairingStateText = findViewById(R.id.pairingStateText)
        logText = findViewById(R.id.logText)
        
        // Set up advertising button
        advertisingButton.setOnClickListener { toggleAdvertising() }
        
        // Set up key buttons
        setupKeyButtons()
        
        // Initialize BLE HID functionality
        initializeBleHid()
        
        // Show device info
        updateDeviceInfo()
    }
    
    override fun onResume() {
        super.onResume()
        updateConnectionStatus()
        updateDeviceInfo()
        
        // Register for Bluetooth state changes
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
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
        if (BleHid.isInitialized()) {
            BleHid.shutdown()
        }
    }
    
    /**
     * Convert a Bluetooth bond state to a readable string.
     */
    private fun bondStateToString(bondState: Int): String {
        return when (bondState) {
            BluetoothDevice.BOND_NONE -> "BOND_NONE"
            BluetoothDevice.BOND_BONDING -> "BOND_BONDING"
            BluetoothDevice.BOND_BONDED -> "BOND_BONDED"
            else -> "UNKNOWN($bondState)"
        }
    }
    
    /**
     * Initializes the BLE HID functionality.
     */
    private fun initializeBleHid() {
        val initialized = BleHid.isInitialized()
        if (initialized) {
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
     * Updates device information for diagnostics.
     */
    private fun updateDeviceInfo() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter != null) {
            val deviceName = adapter.name
            val deviceAddress = adapter.address
            
            deviceNameText.text = "Device Name: $deviceName"
            deviceAddressText.text = "Device Address: $deviceAddress"
            
            addLogEntry("LOCAL DEVICE: $deviceName ($deviceAddress)")
        } else {
            deviceNameText.text = "Device Name: Not available"
            deviceAddressText.text = "Device Address: Not available"
        }
    }
    
    /**
     * Updates the pairing state text.
     */
    private fun updatePairingState(state: String) {
        pairingStateText.text = "Pairing State: $state"
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
    
    /**
     * Sets up the click listeners for key buttons.
     */
    private fun setupKeyButtons() {
        // Letter keys
        setupKeyButton(R.id.buttonA, KEY_A)
        setupKeyButton(R.id.buttonB, KEY_B)
        setupKeyButton(R.id.buttonC, KEY_C)
        setupKeyButton(R.id.buttonD, KEY_D)
        setupKeyButton(R.id.buttonE, KEY_E)
        setupKeyButton(R.id.buttonF, KEY_F)
        setupKeyButton(R.id.buttonG, KEY_G)
        setupKeyButton(R.id.buttonH, KEY_H)
        setupKeyButton(R.id.buttonI, KEY_I)
        setupKeyButton(R.id.buttonJ, KEY_J)
        setupKeyButton(R.id.buttonK, KEY_K)
        setupKeyButton(R.id.buttonL, KEY_L)
        setupKeyButton(R.id.buttonM, KEY_M)
        setupKeyButton(R.id.buttonN, KEY_N)
        setupKeyButton(R.id.buttonO, KEY_O)
        setupKeyButton(R.id.buttonP, KEY_P)
        setupKeyButton(R.id.buttonQ, KEY_Q)
        setupKeyButton(R.id.buttonR, KEY_R)
        setupKeyButton(R.id.buttonS, KEY_S)
        setupKeyButton(R.id.buttonT, KEY_T)
        setupKeyButton(R.id.buttonU, KEY_U)
        setupKeyButton(R.id.buttonV, KEY_V)
        setupKeyButton(R.id.buttonW, KEY_W)
        setupKeyButton(R.id.buttonX, KEY_X)
        setupKeyButton(R.id.buttonY, KEY_Y)
        setupKeyButton(R.id.buttonZ, KEY_Z)
        
        // Special keys
        setupKeyButton(R.id.buttonSpace, KEY_SPACE)
        setupKeyButton(R.id.buttonEnter, KEY_RETURN)
        setupKeyButton(R.id.buttonBackspace, KEY_BACKSPACE)
    }
    
    /**
     * Sets up a click listener for a key button.
     * 
     * @param buttonId The button resource ID
     * @param keyCode The HID key code to send
     */
    private fun setupKeyButton(buttonId: Int, keyCode: Int) {
        findViewById<Button>(buttonId)?.setOnClickListener { sendKey(keyCode) }
    }
    
    /**
     * Sends a key press and release.
     * 
     * @param keyCode The HID key code to send
     */
    private fun sendKey(keyCode: Int) {
        if (BleHid.isConnected()) {
            // Send key press
            val result = BleHid.sendKey(keyCode)
            addLogEntry("KEY PRESS: 0x${Integer.toHexString(keyCode)} ${if (result) "sent" else "FAILED"}")
            
            // Release key after a short delay
            findViewById<View>(android.R.id.content).postDelayed({
                val releaseResult = BleHid.releaseKeys()
                addLogEntry("KEY RELEASE: ${if (releaseResult) "success" else "FAILED"}")
            }, 100)
        } else {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show()
            addLogEntry("KEY PRESS IGNORED: No connected device")
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
    
    /**
     * Updates the connection status text.
     */
    private fun updateConnectionStatus() {
        if (BleHid.isConnected()) {
            val device = BleHid.getConnectedDevice()
            val deviceName = device?.name?.takeIf { !it.isNullOrEmpty() } ?: device?.address ?: "Unknown"
            connectionText.text = getString(R.string.connected_to, deviceName)
            addLogEntry("CONNECTION: Connected to ${device?.name ?: "Unknown"} (${device?.address})")
            
            // Check HID profile status for the connected device
            if (device != null) {
                checkHidProfileStatus(device)
            }
        } else {
            connectionText.setText(R.string.not_connected)
        }
    }
    
    /**
     * Check and log HID profile status for a device.
     */
    private fun checkHidProfileStatus(device: BluetoothDevice) {
        try {
            // Reflect BluetoothHidDevice.getConnectionState
            val profileClass = Class.forName("android.bluetooth.BluetoothProfile")
            val hidDeviceProfile = BluetoothProfile.HID_DEVICE
            
            addLogEntry("CHECKING HID PROFILE: Device ${device.address}")
            
            // Get bond state
            val bondState = device.bondState
            addLogEntry("BOND STATE: ${bondStateToString(bondState)}")
            
            // Check UUIDs supported by device
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val uuids = device.uuids
                if (uuids != null) {
                    addLogEntry("DEVICE UUIDS: ${uuids.size} UUIDs")
                    for (uuid in uuids) {
                        addLogEntry("  - ${uuid.toString()}")
                        
                        // Check if this is HID UUID
                        if (uuid.toString().lowercase().contains("1812")) {
                            addLogEntry("    (HID SERVICE SUPPORTED)")
                        }
                    }
                } else {
                    addLogEntry("DEVICE UUIDS: None or not available")
                }
            }
            
            // Try to get device type
            val deviceType = device.type
            val typeStr = when (deviceType) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "CLASSIC"
                BluetoothDevice.DEVICE_TYPE_LE -> "LE"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "DUAL"
                BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "UNKNOWN"
                else -> "UNDEFINED"
            }
            addLogEntry("DEVICE TYPE: $typeStr")
            
        } catch (e: Exception) {
            addLogEntry("ERROR checking HID profile: ${e.message}")
        }
    }
}
