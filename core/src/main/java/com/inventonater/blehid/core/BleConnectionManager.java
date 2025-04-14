package com.inventonater.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages BLE connection parameters and optimization.
 * Provides functionality to monitor and adjust connection parameters like
 * connection interval, latency, MTU size, and transmit power.
 */
public class BleConnectionManager {
    private static final String TAG = "BleConnectionManager";
    
    // Default connection parameters
    private static final int DEFAULT_CONNECTION_INTERVAL = 30; // 30 ms
    private static final int DEFAULT_SLAVE_LATENCY = 0;
    private static final int DEFAULT_SUPERVISION_TIMEOUT = 2000; // 2000 ms
    private static final int DEFAULT_MTU_SIZE = 23; // Default BLE MTU size
    
    // Connection priorities
    public static final int CONNECTION_PRIORITY_HIGH = BluetoothGatt.CONNECTION_PRIORITY_HIGH;        // 0
    public static final int CONNECTION_PRIORITY_BALANCED = BluetoothGatt.CONNECTION_PRIORITY_BALANCED; // 1
    public static final int CONNECTION_PRIORITY_LOW_POWER = BluetoothGatt.CONNECTION_PRIORITY_LOW_POWER; // 2
    
    // Transmit power levels
    public static final int TX_POWER_LEVEL_HIGH = 2;
    public static final int TX_POWER_LEVEL_MEDIUM = 1;
    public static final int TX_POWER_LEVEL_LOW = 0;
    
    private final BleHidManager bleHidManager;
    private final Handler handler;
    
    // Connection parameters
    private int connectionInterval = DEFAULT_CONNECTION_INTERVAL;
    private int slaveLatency = DEFAULT_SLAVE_LATENCY;
    private int supervisionTimeout = DEFAULT_SUPERVISION_TIMEOUT;
    private int mtuSize = DEFAULT_MTU_SIZE;
    private int rssi = 0;
    private int txPowerLevel = TX_POWER_LEVEL_MEDIUM;
    
    // Parameter tracking
    private int requestedConnectionPriority = CONNECTION_PRIORITY_BALANCED;
    private int requestedMtu = DEFAULT_MTU_SIZE;
    private int requestedTxPowerLevel = TX_POWER_LEVEL_MEDIUM;
    
    // Connection parameter update listener
    public interface ConnectionParameterListener {
        void onConnectionParametersChanged(int interval, int latency, int timeout, int mtu);
        void onRssiRead(int rssi);
        void onRequestComplete(String parameterName, boolean success, String actualValue);
    }
    
    private ConnectionParameterListener listener;
    
    /**
     * Creates a new BLE Connection Manager
     * 
     * @param bleHidManager The parent BLE HID manager
     */
    public BleConnectionManager(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        this.handler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Sets the connection parameter listener.
     * 
     * @param listener The listener to set
     */
    public void setConnectionParameterListener(ConnectionParameterListener listener) {
        this.listener = listener;
    }
    
    /**
     * Called when a device connects.
     * 
     * @param device The connected device
     */
    void onDeviceConnected(BluetoothDevice device) {
        // Reset to default values on new connection
        connectionInterval = DEFAULT_CONNECTION_INTERVAL;
        slaveLatency = DEFAULT_SLAVE_LATENCY;
        supervisionTimeout = DEFAULT_SUPERVISION_TIMEOUT;
        mtuSize = DEFAULT_MTU_SIZE;
        
        // Start RSSI monitoring
        startRssiMonitoring();
        
        // Notify listener
        notifyParameterChanged();
    }
    
    /**
     * Called when a device disconnects.
     */
    void onDeviceDisconnected() {
        // Stop RSSI monitoring
        stopRssiMonitoring();
    }
    
    /**
     * Called when connection parameters are updated.
     */
    void onConnectionUpdated(int interval, int latency, int timeout) {
        this.connectionInterval = interval;
        this.slaveLatency = latency;
        this.supervisionTimeout = timeout;
        
        notifyParameterChanged();
    }
    
    /**
     * Called when MTU size is changed.
     */
    void onMtuChanged(int mtu) {
        this.mtuSize = mtu;
        
        if (listener != null) {
            listener.onRequestComplete("mtu", true, String.valueOf(mtu));
        }
        
        notifyParameterChanged();
    }
    
    /**
     * Called when RSSI is read.
     */
    void onRssiRead(int rssi) {
        this.rssi = rssi;
        
        if (listener != null) {
            listener.onRssiRead(rssi);
        }
    }
    
    /**
     * Requests a change in connection priority.
     * 
     * @param priority The priority to request (CONNECTION_PRIORITY_*)
     * @return true if request was sent, false otherwise
     */
    public boolean requestConnectionPriority(int priority) {
        if (!bleHidManager.isConnected()) {
            Log.e(TAG, "Cannot request connection priority: Not connected");
            return false;
        }
        
        BluetoothGatt gatt = bleHidManager.getGattServerManager().getGattForConnectedDevice();
        if (gatt == null) {
            Log.e(TAG, "Cannot request connection priority: No GATT connection");
            return false;
        }
        
        // Store requested value
        requestedConnectionPriority = priority;
        
        boolean result = gatt.requestConnectionPriority(priority);
        
        if (result) {
            Log.d(TAG, "Connection priority request sent: " + priority);
            
            // Actual values will be reported in the connection parameter update callback
            return true;
        } else {
            Log.e(TAG, "Failed to request connection priority");
            
            if (listener != null) {
                listener.onRequestComplete("connectionPriority", false, "Request failed");
            }
            
            return false;
        }
    }
    
    /**
     * Requests a change in MTU size.
     * 
     * @param mtu The MTU size to request (23-517)
     * @return true if request was sent, false otherwise
     */
    public boolean requestMtu(int mtu) {
        if (!bleHidManager.isConnected()) {
            Log.e(TAG, "Cannot request MTU: Not connected");
            return false;
        }
        
        BluetoothGatt gatt = bleHidManager.getGattServerManager().getGattForConnectedDevice();
        if (gatt == null) {
            Log.e(TAG, "Cannot request MTU: No GATT connection");
            return false;
        }
        
        // Validate MTU size
        if (mtu < 23 || mtu > 517) {
            Log.e(TAG, "Invalid MTU size: " + mtu + ". Must be between 23 and 517.");
            return false;
        }
        
        // Store requested value
        requestedMtu = mtu;
        
        boolean result = gatt.requestMtu(mtu);
        
        if (result) {
            Log.d(TAG, "MTU request sent: " + mtu);
            
            // Actual value will be reported in the MTU changed callback
            return true;
        } else {
            Log.e(TAG, "Failed to request MTU");
            
            if (listener != null) {
                listener.onRequestComplete("mtu", false, "Request failed");
            }
            
            return false;
        }
    }
    
    /**
     * Sets the transmit power level for advertising.
     * 
     * @param level The power level (TX_POWER_LEVEL_*)
     * @return true if set successfully, false otherwise
     */
    public boolean setTransmitPowerLevel(int level) {
        // Validate level
        if (level < TX_POWER_LEVEL_LOW || level > TX_POWER_LEVEL_HIGH) {
            Log.e(TAG, "Invalid TX power level: " + level);
            return false;
        }
        
        // Store requested value
        requestedTxPowerLevel = level;
        
        // Set TX power level in advertiser
        boolean result = bleHidManager.getAdvertiser().setTxPowerLevel(level);
        
        if (result) {
            txPowerLevel = level;
            
            if (listener != null) {
                listener.onRequestComplete("txPowerLevel", true, String.valueOf(level));
            }
        } else {
            if (listener != null) {
                listener.onRequestComplete("txPowerLevel", false, "Set failed");
            }
        }
        
        return result;
    }
    
    /**
     * Reads the current RSSI value.
     * 
     * @return true if read request was sent, false otherwise
     */
    public boolean readRssi() {
        if (!bleHidManager.isConnected()) {
            Log.e(TAG, "Cannot read RSSI: Not connected");
            return false;
        }
        
        BluetoothGatt gatt = bleHidManager.getGattServerManager().getGattForConnectedDevice();
        if (gatt == null) {
            Log.e(TAG, "Cannot read RSSI: No GATT connection");
            return false;
        }
        
        boolean result = gatt.readRemoteRssi();
        
        if (!result) {
            Log.e(TAG, "Failed to read RSSI");
        }
        
        return result;
    }
    
    // RSSI monitoring
    private boolean rssiMonitoringEnabled = false;
    private static final long RSSI_MONITORING_INTERVAL = 2000; // 2 seconds
    
    private final Runnable rssiMonitoringRunnable = new Runnable() {
        @Override
        public void run() {
            if (rssiMonitoringEnabled && bleHidManager.isConnected()) {
                readRssi();
                handler.postDelayed(this, RSSI_MONITORING_INTERVAL);
            }
        }
    };
    
    /**
     * Starts RSSI monitoring.
     */
    private void startRssiMonitoring() {
        rssiMonitoringEnabled = true;
        handler.postDelayed(rssiMonitoringRunnable, RSSI_MONITORING_INTERVAL);
    }
    
    /**
     * Stops RSSI monitoring.
     */
    private void stopRssiMonitoring() {
        rssiMonitoringEnabled = false;
        handler.removeCallbacks(rssiMonitoringRunnable);
    }
    
    /**
     * Gets all connection parameters as a map.
     * 
     * @return Map of parameter names to values
     */
    public Map<String, String> getAllConnectionParameters() {
        Map<String, String> params = new HashMap<>();
        
        params.put("connectionInterval", String.valueOf(connectionInterval));
        params.put("slaveLatency", String.valueOf(slaveLatency));
        params.put("supervisionTimeout", String.valueOf(supervisionTimeout));
        params.put("mtuSize", String.valueOf(mtuSize));
        params.put("rssi", String.valueOf(rssi));
        params.put("txPowerLevel", String.valueOf(txPowerLevel));
        
        params.put("requestedConnectionPriority", String.valueOf(requestedConnectionPriority));
        params.put("requestedMtu", String.valueOf(requestedMtu));
        params.put("requestedTxPowerLevel", String.valueOf(requestedTxPowerLevel));
        
        return params;
    }
    
    /**
     * Notifies the listener about parameter changes.
     */
    private void notifyParameterChanged() {
        if (listener != null) {
            listener.onConnectionParametersChanged(
                    connectionInterval,
                    slaveLatency,
                    supervisionTimeout,
                    mtuSize);
        }
    }
    
    /**
     * Gets the connection interval.
     * 
     * @return The connection interval in milliseconds
     */
    public int getConnectionInterval() {
        return connectionInterval;
    }
    
    /**
     * Gets the slave latency.
     * 
     * @return The slave latency
     */
    public int getSlaveLatency() {
        return slaveLatency;
    }
    
    /**
     * Gets the supervision timeout.
     * 
     * @return The supervision timeout in milliseconds
     */
    public int getSupervisionTimeout() {
        return supervisionTimeout;
    }
    
    /**
     * Gets the MTU size.
     * 
     * @return The MTU size in bytes
     */
    public int getMtuSize() {
        return mtuSize;
    }
    
    /**
     * Gets the RSSI value.
     * 
     * @return The RSSI value in dBm
     */
    public int getRssi() {
        return rssi;
    }
    
    /**
     * Gets the transmit power level.
     * 
     * @return The TX power level
     */
    public int getTxPowerLevel() {
        return txPowerLevel;
    }
}
