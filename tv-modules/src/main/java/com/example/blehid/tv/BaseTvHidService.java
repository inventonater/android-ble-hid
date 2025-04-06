package com.example.blehid.tv;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.blehid.core.BleGattServerManager;

/**
 * Base abstract implementation of TvHidService that provides common functionality.
 * TV-specific implementations can extend this class and override only what's needed.
 */
public abstract class BaseTvHidService implements TvHidService {
    private static final String TAG = "BaseTvHidService";
    
    protected final BleGattServerManager gattServerManager;
    protected BluetoothDevice connectedDevice;
    protected final Handler handler;
    
    // Default delay between button press and release in milliseconds
    protected static final int DEFAULT_CLICK_DELAY = 100;
    
    /**
     * Constructor for BaseTvHidService.
     * 
     * @param gattServerManager The GATT server manager
     */
    public BaseTvHidService(BleGattServerManager gattServerManager) {
        this.gattServerManager = gattServerManager;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    public void setConnectedDevice(BluetoothDevice device) {
        this.connectedDevice = device;
        Log.d(TAG, "Connected device set to: " + (device != null ? device.getAddress() : "null"));
    }
    
    @Override
    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }
    
    @Override
    public boolean clickDirectionalButton(int direction) {
        if (!pressDirectionalButton(direction)) {
            return false;
        }
        
        // Schedule a release after a short delay
        handler.postDelayed(() -> releaseDirectionalButtons(), DEFAULT_CLICK_DELAY);
        return true;
    }
    
    @Override
    public boolean clickSelectButton() {
        if (!pressSelectButton()) {
            return false;
        }
        
        // Schedule a release after a short delay
        handler.postDelayed(() -> releaseSelectButton(), DEFAULT_CLICK_DELAY);
        return true;
    }
    
    /**
     * Helper method to check if we're ready to send a report.
     * 
     * @return true if we're ready to send, false otherwise
     */
    protected boolean isReadyToSend() {
        if (connectedDevice == null) {
            Log.d(TAG, "No connected device");
            return false;
        }
        
        if (gattServerManager == null) {
            Log.e(TAG, "GATT server manager is null");
            return false;
        }
        
        return true;
    }
    
    /**
     * Helper method to log errors.
     * 
     * @param message The error message
     */
    protected void logError(String message) {
        Log.e(TAG, message);
    }
    
    /**
     * Helper method to log debug messages.
     * 
     * @param message The debug message
     */
    protected void logDebug(String message) {
        Log.d(TAG, message);
    }
    
    @Override
    public boolean supportsPointer() {
        // Default implementation, override as needed
        return true;
    }
    
    @Override
    public boolean supportsMediaControls() {
        // Default implementation, override as needed
        return true;
    }
    
    @Override
    public void close() {
        // Default implementation, override as needed
        connectedDevice = null;
    }
}
