package com.inventonater.hid.app

import android.app.Application
import android.util.Log
import com.inventonater.hid.core.BleHid
import com.inventonater.hid.core.api.LogLevel

/**
 * Main application class for the Inventonater HID library.
 * Handles initialization of core BLE HID functionality at application start.
 */
class InventonaterHidApp : Application() {
    companion object {
        private const val TAG = "InventonaterHidApp"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize BLE HID library
        initializeHidLibrary()
    }
    
    /**
     * Initialize the core BLE HID library with application context
     */
    private fun initializeHidLibrary() {
        Log.d(TAG, "Initializing BLE HID library")
        
        val initialized = BleHid.initialize(applicationContext, LogLevel.DEBUG)
        
        if (initialized) {
            Log.d(TAG, "BLE HID library initialization successful")
        } else {
            Log.e(TAG, "BLE HID library initialization failed")
        }
    }
    
    override fun onTerminate() {
        // Shutdown BLE HID functionality
        if (BleHid.isInitialized()) {
            Log.d(TAG, "Shutting down BLE HID library")
            BleHid.shutdown()
        }
        
        super.onTerminate()
    }
}
