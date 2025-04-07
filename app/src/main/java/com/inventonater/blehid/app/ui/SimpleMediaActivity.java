package com.inventonater.blehid.app.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.inventonater.blehid.app.R;
import com.inventonater.blehid.core.BleHidManager;
import com.inventonater.blehid.core.BlePairingManager;

/**
 * Main activity for testing the BLE HID functionality.
 * Provides a UI for controlling media playback, mouse, and keyboard functions.
 */
public class SimpleMediaActivity extends AppCompatActivity implements 
        BluetoothReceiver.Callback, 
        KeyboardPanelManager.Callback,
        MousePanelManager.Callback,
        MediaPanelManager.Callback,
        DiagnosticsManager.Callback {
    
    private static final String TAG = "SimpleMediaActivity";
    
    // UI Components
    private Button advertisingButton;
    
    // Managers
    private BleHidManager bleHidManager;
    private LogManager logManager;
    private TabManager tabManager;
    private KeyboardPanelManager keyboardManager;
    private MousePanelManager mouseManager;
    private MediaPanelManager mediaManager;
    private DiagnosticsManager diagnosticsManager;
    private BluetoothReceiver bluetoothReceiver;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media);
        
        // Initialize BLE HID manager
        bleHidManager = new BleHidManager(this);
        
        // Initialize UI components
        initializeViews();
        
        // Create specialized managers
        createManagers();
        
        // Set up advertising button
        setupAdvertisingButton();
        
        // Set up pairing callback
        setupPairingCallback();
        
        // Set up timer to refresh diagnostic info every 2 seconds
        setupPeriodicRefresh();
        
        // Initialize BLE HID functionality
        initializeBleHid();
        
        // Show device info
        diagnosticsManager.updateDeviceInfo();
    }
    
    /**
     * Initialize all UI components.
     */
    private void initializeViews() {
        // Status components
        TextView statusText = findViewById(R.id.statusText);
        TextView connectionText = findViewById(R.id.connectionText);
        advertisingButton = findViewById(R.id.advertisingButton);
        
        // Diagnostic components
        TextView deviceNameText = findViewById(R.id.deviceNameText);
        TextView deviceAddressText = findViewById(R.id.deviceAddressText);
        TextView pairingStateText = findViewById(R.id.pairingStateText);
        TextView logText = findViewById(R.id.logText);
        
        // Tab components
        Button mediaTabButton = findViewById(R.id.mediaTabButton);
        Button mouseTabButton = findViewById(R.id.mouseTabButton);
        Button keyboardTabButton = findViewById(R.id.keyboardTabButton);
        View mediaPanel = findViewById(R.id.mediaPanel);
        View mousePanel = findViewById(R.id.mousePanel);
        View keyboardPanel = findViewById(R.id.keyboardPanel);
        
        // Create the tab manager with these components
        tabManager = new TabManager(
            this,
            mediaTabButton,
            mouseTabButton,
            keyboardTabButton,
            mediaPanel,
            mousePanel,
            keyboardPanel
        );
        
        // Create log manager
        logManager = new LogManager(logText);
        
        // Create diagnostics manager
        diagnosticsManager = new DiagnosticsManager(
            deviceNameText,
            deviceAddressText,
            pairingStateText,
            connectionText,
            statusText,
            bleHidManager,
            this // The activity implements DiagnosticsManager.Callback
        );
    }
    
    /**
     * Create specialized managers for different panels.
     */
    private void createManagers() {
        // Create media panel manager
        View mediaPanel = findViewById(R.id.mediaPanel);
        mediaManager = new MediaPanelManager(this, mediaPanel, bleHidManager, this);
        
        // Create mouse panel manager
        View mousePanel = findViewById(R.id.mousePanel);
        mouseManager = new MousePanelManager(this, mousePanel, bleHidManager, this);
        
        // Create keyboard panel manager
        View keyboardPanel = findViewById(R.id.keyboardPanel);
        keyboardManager = new KeyboardPanelManager(this, keyboardPanel, bleHidManager, this);
        
        // Create Bluetooth receiver
        bluetoothReceiver = new BluetoothReceiver(this);
    }
    
    /**
     * Set up advertising button.
     */
    private void setupAdvertisingButton() {
        advertisingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAdvertising();
                
                // Update diagnostic info after advertising toggle
                diagnosticsManager.updateDiagnosticInfo();
            }
        });
    }
    
    /**
     * Set up periodic refresh of diagnostic information.
     */
    private void setupPeriodicRefresh() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                diagnosticsManager.updateDiagnosticInfo();
                new Handler().postDelayed(this, 2000);
            }
        }, 2000);
    }
    
    /**
     * Set up pairing callback for BLE HID manager.
     */
    private void setupPairingCallback() {
        bleHidManager.getBlePairingManager().setPairingCallback(new BlePairingManager.PairingCallback() {
            @Override
            public void onPairingRequested(BluetoothDevice device, int variant) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = "Pairing requested by " + device.getAddress() + 
                                         ", variant: " + variant;
                        
                        Toast.makeText(SimpleMediaActivity.this, message, Toast.LENGTH_SHORT).show();
                        logEvent("PAIRING REQUESTED: " + message);
                        diagnosticsManager.updatePairingState("REQUESTED");
                        
                        // Auto-accept pairing requests for testing
                        bleHidManager.getBlePairingManager().setPairingConfirmation(device, true);
                    }
                });
            }
            
            @Override
            public void onPairingComplete(BluetoothDevice device, boolean success) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String result = success ? "SUCCESS" : "FAILED";
                        String message = "Pairing " + result + " with " + device.getAddress();
                        
                        Toast.makeText(SimpleMediaActivity.this, message, Toast.LENGTH_SHORT).show();
                        logEvent("PAIRING COMPLETE: " + message);
                        diagnosticsManager.updatePairingState(result);
                        diagnosticsManager.updateConnectionStatus();
                        diagnosticsManager.updateDeviceInfo();
                    }
                });
            }
        });
    }
    
    /**
     * Initializes the BLE HID functionality.
     */
    private void initializeBleHid() {
        boolean initialized = bleHidManager.initialize();
        if (initialized) {
            advertisingButton.setEnabled(true);
            logEvent("BLE HID initialized successfully");
        } else {
            advertisingButton.setEnabled(false);
            logEvent("BLE HID initialization FAILED");
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        diagnosticsManager.updateConnectionStatus();
        diagnosticsManager.updateDeviceInfo();
        
        // Register for Bluetooth state changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        registerReceiver(bluetoothReceiver, filter);
        
        logEvent("BLUETOOTH: Receiver registered for state changes");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop advertising when the activity is not visible
        if (bleHidManager != null) {
            bleHidManager.stopAdvertising();
            diagnosticsManager.updateAdvertisingStatus(false);
        }
        
        // Unregister Bluetooth receiver
        try {
            unregisterReceiver(bluetoothReceiver);
            logEvent("BLUETOOTH: Receiver unregistered");
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleHidManager != null) {
            bleHidManager.close();
        }
    }
    
    /**
     * Toggles BLE advertising on/off.
     */
    private void toggleAdvertising() {
        if (bleHidManager == null) return;
        
        // Check if we're already advertising
        boolean isCurrentlyAdvertising = bleHidManager.isAdvertising();
        if (isCurrentlyAdvertising) {
            bleHidManager.stopAdvertising();
            updateAdvertisingStatus(false);
            logEvent("ADVERTISING: Stopped");
        } else {
            logEvent("ADVERTISING: Attempting to start...");
            boolean result = bleHidManager.startAdvertising();
            updateAdvertisingStatus(result);
            
            if (result) {
                logEvent("ADVERTISING: Start initiated");
                // Note: actual success is determined in the callback in BleAdvertiser
            } else {
                String errorMsg = bleHidManager.getAdvertiser().getLastErrorMessage();
                logEvent("ADVERTISING: Failed to start: " + errorMsg);
                Toast.makeText(this, 
                    errorMsg != null ? errorMsg : getString(R.string.advertising_failed), 
                    Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Updates the advertising button text and status.
     * 
     * @param isAdvertising Whether advertising is active
     */
    private void updateAdvertisingStatus(boolean isAdvertising) {
        advertisingButton.setText(isAdvertising ? R.string.stop_advertising : R.string.start_advertising);
        diagnosticsManager.updateAdvertisingStatus(isAdvertising);
    }
    
    // BluetoothReceiver.Callback implementation
    
    @Override
    public void onBluetoothStateChanged(int state) {
        // Nothing to do here, logging is handled in the receiver
    }
    
    @Override
    public void onScanModeChanged(int mode) {
        // Nothing to do here, logging is handled in the receiver
    }
    
    @Override
    public void onDeviceFound(BluetoothDevice device) {
        // Nothing to do here, logging is handled in the receiver
    }
    
    @Override
    public void onBondStateChanged(BluetoothDevice device, int prevState, int newState) {
        if (newState == BluetoothDevice.BOND_BONDED) {
            diagnosticsManager.updatePairingState("BONDED");
        } else if (newState == BluetoothDevice.BOND_NONE) {
            diagnosticsManager.updatePairingState("NONE");
        }
    }
    
    @Override
    public void onDeviceConnected(BluetoothDevice device) {
        diagnosticsManager.updateConnectionStatus();
    }
    
    @Override
    public void onDeviceDisconnectRequested(BluetoothDevice device) {
        // Nothing special to do here
    }
    
    @Override
    public void onDeviceDisconnected(BluetoothDevice device) {
        diagnosticsManager.updateConnectionStatus();
    }
    
    @Override
    public void onPairingRequest(BluetoothDevice device, int variant) {
        // Nothing special to do here, handled by the pairing callback
    }
    
    @Override
    public void logEvent(String message) {
        // This method is used by all callbacks to log events
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logManager.addLogEntry(message);
            }
        });
    }
}
