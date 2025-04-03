package com.inventonater.hid.app

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.inventonater.hid.app.ui.SimpleKeyboardActivity
import com.inventonater.hid.app.ui.SimpleMouseActivity
import com.inventonater.hid.core.BleHid
import com.inventonater.hid.core.api.ConnectionListener
import com.inventonater.hid.core.api.LogLevel

class MainActivity : AppCompatActivity() {
    
    companion object {
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_PERMISSIONS = 2
    }
    
    private lateinit var statusText: TextView
    private lateinit var advertiseButton: Button
    private lateinit var keyboardButton: Button
    private lateinit var mouseButton: Button
    
    private var isInitialized = false
    private var isAdvertising = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // UI components
        statusText = findViewById(R.id.statusText)
        advertiseButton = findViewById(R.id.advertiseButton)
        keyboardButton = findViewById(R.id.keyboardButton)
        mouseButton = findViewById(R.id.mouseButton)
        
        // Setup button click listeners
        advertiseButton.setOnClickListener { onAdvertiseClicked() }
        keyboardButton.setOnClickListener { startActivity(Intent(this, SimpleKeyboardActivity::class.java)) }
        mouseButton.setOnClickListener { startActivity(Intent(this, SimpleMouseActivity::class.java)) }
        
        // Check if BLE is supported
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE is not supported on this device", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Check and request permissions
        checkAndRequestPermissions()
    }
    
    override fun onResume() {
        super.onResume()
        
        // Update UI based on current state
        updateUI()
        
        // Try to initialize if needed
        if (!isInitialized && checkPermissions()) {
            initializeBleHid()
        }
    }
    
    override fun onPause() {
        super.onPause()
        
        // Stop advertising when app goes to background
        if (isAdvertising) {
            BleHid.stopAdvertising()
            isAdvertising = false
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Shutdown BLE HID when activity is destroyed
        if (isInitialized) {
            BleHid.shutdown()
            isInitialized = false
        }
    }
    
    private fun onAdvertiseClicked() {
        if (!isInitialized) {
            Toast.makeText(this, "BLE HID not initialized", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (isAdvertising) {
            // Stop advertising
            BleHid.stopAdvertising()
            isAdvertising = false
        } else {
            // Start advertising
            val success = BleHid.startAdvertising()
            isAdvertising = success
            
            if (!success) {
                Toast.makeText(this, "Failed to start advertising", Toast.LENGTH_SHORT).show()
            }
        }
        
        updateUI()
    }
    
    private fun updateUI() {
        if (!isInitialized) {
            statusText.text = "Status: Core functionality is temporarily disabled"
            advertiseButton.isEnabled = false
            keyboardButton.isEnabled = false
            mouseButton.isEnabled = false
            
            // Check specific reasons and display more detailed message
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val bluetoothAdapter = bluetoothManager.adapter
            
            when {
                bluetoothAdapter == null -> 
                    statusText.text = "Status: Bluetooth not available on this device"
                !bluetoothAdapter.isEnabled -> 
                    statusText.text = "Status: Please enable Bluetooth"
                !checkPermissions() -> 
                    statusText.text = "Status: Missing required Bluetooth permissions"
                !BleHid.isBlePeripheralSupported() -> 
                    statusText.text = "Status: BLE peripheral mode not supported on this device"
                else -> 
                    statusText.text = "Status: Initialization failed"
            }
            return
        }
        
        // Enable input type selection buttons once initialized
        keyboardButton.isEnabled = true
        mouseButton.isEnabled = true
        
        // Update advertising button state
        advertiseButton.isEnabled = false  // Disable in main activity to enforce proper flow
        advertiseButton.visibility = View.GONE  // Hide the advertising button
        
        // Show appropriate status
        if (BleHid.isConnected()) {
            statusText.text = "Status: Connected"
        } else {
            statusText.text = "Status: Ready - Select Keyboard or Mouse"
        }
    }
    
    private fun initializeBleHid() {
        // Check if Bluetooth is enabled
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val bluetoothAdapter = bluetoothManager.adapter
        
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            return
        }
        
        // Initialize BLE HID
        isInitialized = BleHid.initialize(applicationContext, LogLevel.DEBUG)
        
        if (isInitialized) {
            // Register connection listener
            BleHid.addConnectionListener(object : ConnectionListener {
                override fun onDeviceConnected(device: BluetoothDevice) {
                    runOnUiThread { 
                        Toast.makeText(
                            this@MainActivity, 
                            "Connected to: ${device.name}", 
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI()
                    }
                }
                
                override fun onDeviceDisconnected(device: BluetoothDevice) {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity, 
                            "Disconnected from: ${device.name}", 
                            Toast.LENGTH_SHORT
                        ).show()
                        updateUI()
                    }
                }
            })
            
            // Check if BLE peripheral mode is supported
            if (!BleHid.isBlePeripheralSupported()) {
                Toast.makeText(this, "BLE peripheral mode not supported on this device", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Failed to initialize BLE HID", Toast.LENGTH_SHORT).show()
        }
        
        updateUI()
    }
    
    // Permission handling
    private fun checkPermissions(): Boolean {
        return getRequiredPermissions().isEmpty()
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = getRequiredPermissions()
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_PERMISSIONS
            )
        } else if (!isInitialized) {
            initializeBleHid()
        }
    }
    
    private fun getRequiredPermissions(): List<String> {
        val permissionsToRequest = mutableListOf<String>()
        
        // For Android 12+ (API level 31), we need to request the new Bluetooth permissions
        // We're only targeting modern Android, so always request these
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE)
        permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        
        return permissionsToRequest.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
        }
    }
    
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == REQUEST_PERMISSIONS) {
            var allGranted = true
            
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            
            if (allGranted) {
                initializeBleHid()
            } else {
                Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                initializeBleHid()
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to use this app", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
