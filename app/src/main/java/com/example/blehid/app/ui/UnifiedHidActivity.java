package com.example.blehid.app.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.blehid.app.R;
import com.example.blehid.core.BleHidManager;
import com.example.blehid.core.IPairingManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Date;

/**
 * Unified activity for BLE HID peripheral emulation.
 * Provides combined mouse, keyboard, and media control functionality.
 */
public class UnifiedHidActivity extends AppCompatActivity implements MouseFragment.HidEventListener {
    private static final String TAG = "UnifiedHidActivity";
    
    // Number of tabs
    private static final int NUM_TABS = 3;
    
    // Tab indices
    private static final int TAB_MOUSE = 0;
    private static final int TAB_KEYBOARD = 1;
    private static final int TAB_MEDIA = 2;
    
    // UI elements
    private TextView statusText;
    private TextView connectionText;
    private Button advertisingButton;
    private Button pairingActionButton;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    
    // Diagnostic views
    private TextView deviceNameText;
    private TextView deviceAddressText;
    private TextView pairingStateText;
    private TextView logText;
    
    // Fragments
    private MouseFragment mouseFragment;
    private KeyboardFragment keyboardFragment;
    private MediaFragment mediaFragment;
    
    // Core components
    private BleHidManager bleHidManager;
    
    // StringBuilder for log entries
    private StringBuilder logEntries = new StringBuilder();
    
    // Bluetooth state receiver
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceInfo = device != null ? 
                    (device.getName() != null ? device.getName() : "unnamed") + 
                    " (" + device.getAddress() + ")" : "unknown device";
            
            switch (action) {
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:
                            addLogEntry("BLUETOOTH: Turned OFF");
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            addLogEntry("BLUETOOTH: Turning OFF");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            addLogEntry("BLUETOOTH: Turned ON");
                            break;
                        case BluetoothAdapter.STATE_TURNING_ON:
                            addLogEntry("BLUETOOTH: Turning ON");
                            break;
                    }
                    break;
                case BluetoothAdapter.ACTION_SCAN_MODE_CHANGED:
                    int scanMode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                    switch (scanMode) {
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                            addLogEntry("BLUETOOTH: Discoverable");
                            break;
                        case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                            addLogEntry("BLUETOOTH: Connectable but not discoverable");
                            break;
                        case BluetoothAdapter.SCAN_MODE_NONE:
                            addLogEntry("BLUETOOTH: Not connectable or discoverable");
                            break;
                    }
                    break;
                case BluetoothDevice.ACTION_FOUND:
                    addLogEntry("DEVICE FOUND: " + deviceInfo);
                    break;
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    String bondStateStr = bondStateToString(bondState);
                    String prevBondStateStr = bondStateToString(prevBondState);
                    addLogEntry("BOND STATE: " + deviceInfo + " " + prevBondStateStr + " -> " + bondStateStr);
                    
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        updatePairingState("BONDED");
                    } else if (bondState == BluetoothDevice.BOND_NONE) {
                        updatePairingState("NONE");
                    }
                    break;
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    addLogEntry("ACL CONNECTED: " + deviceInfo);
                    updateConnectionStatus();
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED:
                    addLogEntry("ACL DISCONNECT REQUESTED: " + deviceInfo);
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    addLogEntry("ACL DISCONNECTED: " + deviceInfo);
                    updateConnectionStatus();
                    break;
                case BluetoothDevice.ACTION_PAIRING_REQUEST:
                    int variant = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
                    addLogEntry("PAIRING REQUEST: " + deviceInfo + ", variant: " + variant);
                    break;
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unified_hid);
        
        // Initialize UI elements
        statusText = findViewById(R.id.statusText);
        connectionText = findViewById(R.id.connectionText);
        advertisingButton = findViewById(R.id.advertisingButton);
        pairingActionButton = findViewById(R.id.pairingActionButton);
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        
        // Initialize diagnostic views
        deviceNameText = findViewById(R.id.deviceNameText);
        deviceAddressText = findViewById(R.id.deviceAddressText);
        pairingStateText = findViewById(R.id.pairingStateText);
        logText = findViewById(R.id.logText);
        
        // Initialize BLE HID manager
        bleHidManager = new BleHidManager(this);
        
        // Set up fragments and tabs
        setupFragments();
        
        // Set up advertising button
        advertisingButton.setOnClickListener(v -> {
            toggleAdvertising();
            updateDiagnosticInfo();
        });
        
        // Set up pairing action button
        setupPairingActionButton();
        
        // Set up timer to refresh diagnostic info every 2 seconds
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                updateDiagnosticInfo();
                new Handler().postDelayed(this, 2000);
            }
        }, 2000);
        
        // Initialize BLE HID functionality
        initializeBleHid();
        
        // Show device info
        updateDeviceInfo();
    }
    
    private void setupFragments() {
        // Create the fragments
        mouseFragment = new MouseFragment();
        keyboardFragment = new KeyboardFragment();
        mediaFragment = new MediaFragment();
        
        // Pass BleHidManager and event listener to fragments
        mouseFragment.setBleHidManager(bleHidManager);
        mouseFragment.setEventListener(this);
        
        keyboardFragment.setBleHidManager(bleHidManager);
        keyboardFragment.setEventListener(this);
        
        mediaFragment.setBleHidManager(bleHidManager);
        mediaFragment.setEventListener(this);
        
        // Set up the ViewPager with tabs
        TabAdapter tabAdapter = new TabAdapter(this);
        viewPager.setAdapter(tabAdapter);
        
        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager,
            (tab, position) -> {
                switch (position) {
                    case TAB_MOUSE:
                        tab.setText("Mouse");
                        break;
                    case TAB_KEYBOARD:
                        tab.setText("Keyboard");
                        break;
                    case TAB_MEDIA:
                        tab.setText("Media");
                        break;
                }
            }
        ).attach();
    }
    
    /**
     * Sets up the pairing action button.
     */
    private void setupPairingActionButton() {
        if (pairingActionButton != null) {
            pairingActionButton.setOnClickListener(v -> handlePairingAction());
            updatePairingActionButton();
        }
    }
    
    /**
     * Updates the pairing action button text and action
     * based on the current pairing state.
     */
    private void updatePairingActionButton() {
        if (pairingActionButton == null || bleHidManager == null || 
            bleHidManager.getPairingManager() == null) {
            return;
        }
        
        IPairingManager.PairingState state = bleHidManager.getPairingManager().getPairingState();
        
        switch (state) {
            case IDLE:
                if (bleHidManager.isConnected()) {
                    // Show remove bond button for connected device
                    pairingActionButton.setText("Remove Bond");
                    pairingActionButton.setEnabled(true);
                } else {
                    // No action when idle and not connected
                    pairingActionButton.setText("No Pairing Action");
                    pairingActionButton.setEnabled(false);
                }
                break;
                
            case PAIRING_REQUESTED:
            case PAIRING_STARTED:
            case WAITING_FOR_BOND:
                // Show cancel button during active pairing
                pairingActionButton.setText("Cancel Pairing");
                pairingActionButton.setEnabled(true);
                break;
                
            case BONDED:
                // Show remove bond button
                pairingActionButton.setText("Remove Bond");
                pairingActionButton.setEnabled(true);
                break;
                
            case PAIRING_FAILED:
                // Show retry button
                pairingActionButton.setText("Retry Pairing");
                pairingActionButton.setEnabled(true);
                break;
                
            case UNPAIRING:
                // Disable button during unpairing
                pairingActionButton.setText("Unpairing...");
                pairingActionButton.setEnabled(false);
                break;
                
            default:
                pairingActionButton.setText("Unknown State");
                pairingActionButton.setEnabled(false);
                break;
        }
    }
    
    /**
     * Handles pairing action button clicks based on current state.
     */
    private void handlePairingAction() {
        if (bleHidManager == null || bleHidManager.getPairingManager() == null) {
            return;
        }
        
        IPairingManager.PairingState state = bleHidManager.getPairingManager().getPairingState();
        
        switch (state) {
            case PAIRING_REQUESTED:
            case PAIRING_STARTED:
            case WAITING_FOR_BOND:
                // Cancel the pairing
                boolean cancelled = bleHidManager.getPairingManager().cancelPairing();
                if (cancelled) {
                    addLogEntry("PAIRING: Cancelled by user");
                    Toast.makeText(this, "Pairing cancelled", Toast.LENGTH_SHORT).show();
                } else {
                    addLogEntry("PAIRING: Failed to cancel");
                }
                break;
                
            case BONDED:
            case IDLE:
                // If connected, remove the bond
                if (bleHidManager.getConnectionManager() != null && bleHidManager.isConnected()) {
                    BluetoothDevice device = bleHidManager.getConnectedDevice();
                    if (device != null) {
                        boolean removed = bleHidManager.getPairingManager().removeBond(device);
                        if (removed) {
                            addLogEntry("PAIRING: Bond removal initiated for " + device.getAddress());
                            Toast.makeText(this, "Removing bond...", Toast.LENGTH_SHORT).show();
                        } else {
                            addLogEntry("PAIRING: Failed to remove bond");
                        }
                    }
                }
                break;
                
            case PAIRING_FAILED:
                // Retry pairing if we have a connected device
                if (bleHidManager.getConnectionManager() != null && bleHidManager.isConnected()) {
                    BluetoothDevice device = bleHidManager.getConnectedDevice();
                    if (device != null) {
                        boolean initiated = bleHidManager.getPairingManager().createBond(device);
                        if (initiated) {
                            addLogEntry("PAIRING: Retry initiated for " + device.getAddress());
                            Toast.makeText(this, "Retrying pairing...", Toast.LENGTH_SHORT).show();
                        } else {
                            addLogEntry("PAIRING: Failed to retry");
                        }
                    }
                }
                break;
        }
        
        // Update button state after action
        updatePairingActionButton();
    }
    
    /**
     * Initializes the BLE HID functionality.
     */
    private void initializeBleHid() {
        boolean initialized = bleHidManager.initialize();
        if (initialized) {
            statusText.setText(R.string.ready);
            advertisingButton.setEnabled(true);
            addLogEntry("BLE HID initialized successfully");
            
            // Set up pairing callback
            bleHidManager.getPairingManager().setPairingCallback(new IPairingManager.PairingCallback() {
                @Override
                public void onPairingRequested(BluetoothDevice device, int variant) {
                    runOnUiThread(() -> {
                        String message = "Pairing requested by " + device.getAddress() + 
                                      ", variant: " + variant;
                        
                        Toast.makeText(UnifiedHidActivity.this, message, Toast.LENGTH_SHORT).show();
                        addLogEntry("PAIRING REQUESTED: " + message);
                        updatePairingState("REQUESTED");
                        
                        // Auto-accept pairing requests
                        bleHidManager.getPairingManager().setPairingConfirmation(device, true);
                        
                        // Update pairing action button 
                        updatePairingActionButton();
                    });
                }
                
                @Override
                public void onPairingComplete(BluetoothDevice device, boolean success, String status) {
                    runOnUiThread(() -> {
                        String result = success ? "SUCCESS" : "FAILED";
                        String message = "Pairing " + result + " with " + device.getAddress();
                        if (status != null && !status.isEmpty()) {
                            message += " - " + status;
                        }
                        
                        Toast.makeText(UnifiedHidActivity.this, message, Toast.LENGTH_SHORT).show();
                        addLogEntry("PAIRING COMPLETE: " + message);
                        updatePairingState(result);
                        updateConnectionStatus();
                        updateDeviceInfo();
                        
                        // Update pairing action button
                        updatePairingActionButton();
                    });
                }
                
                @Override
                public void onPairingProgress(BluetoothDevice device, IPairingManager.PairingState state, String message) {
                    runOnUiThread(() -> {
                        addLogEntry("PAIRING PROGRESS: " + state + (message != null ? " - " + message : ""));
                        updatePairingState(state.toString());
                        updatePairingActionButton();
                    });
                }
            });
        } else {
            statusText.setText(R.string.initialization_failed);
            advertisingButton.setEnabled(false);
            addLogEntry("BLE HID initialization FAILED");
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
            addLogEntry("ADVERTISING: Stopped");
        } else {
            addLogEntry("ADVERTISING: Attempting to start...");
            boolean result = bleHidManager.startAdvertising();
            updateAdvertisingStatus(result);
            
            if (result) {
                addLogEntry("ADVERTISING: Start initiated");
            } else {
                String errorMsg = bleHidManager.getAdvertisingManager().getLastErrorMessage();
                addLogEntry("ADVERTISING: Failed to start: " + errorMsg);
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
        statusText.setText(isAdvertising ? R.string.advertising : R.string.ready);
    }
    
    /**
     * Updates device information for diagnostics.
     */
    private void updateDeviceInfo() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            String deviceName = adapter.getName();
            String deviceAddress = adapter.getAddress();
            
            deviceNameText.setText("Device Name: " + deviceName);
            deviceAddressText.setText("Device Address: " + deviceAddress);
            
            addLogEntry("LOCAL DEVICE: " + deviceName + " (" + deviceAddress + ")");
        } else {
            deviceNameText.setText("Device Name: Not available");
            deviceAddressText.setText("Device Address: Not available");
        }
    }
    
    /**
     * Updates the pairing state text.
     */
    private void updatePairingState(String state) {
        // Get more detailed pairing information if available
        if (bleHidManager != null && bleHidManager.getPairingManager() != null) {
            String detailedInfo = bleHidManager.getPairingManager().getPairingStatusInfo();
            pairingStateText.setText("Pairing State: " + state + "\n" + detailedInfo);
        } else {
            pairingStateText.setText("Pairing State: " + state);
        }
    }
    
    /**
     * Updates the connection status text.
     */
    private void updateConnectionStatus() {
        if (bleHidManager != null && bleHidManager.getConnectionManager() != null && bleHidManager.isConnected()) {
            BluetoothDevice device = bleHidManager.getConnectedDevice();
            if (device != null) {
                String deviceName = device.getName();
                if (deviceName == null || deviceName.isEmpty()) {
                    deviceName = device.getAddress();
                }
                connectionText.setText(getString(R.string.connected_to, deviceName));
                addLogEntry("CONNECTION: Connected to " + deviceName + " (" + device.getAddress() + ")");
            } else {
                connectionText.setText(R.string.not_connected);
            }
        } else {
            connectionText.setText(R.string.not_connected);
        }
    }
    
    /**
     * Updates the diagnostic information display.
     */
    private void updateDiagnosticInfo() {
        if (bleHidManager != null) {
            StringBuilder diagnosticInfo = new StringBuilder();
            
            // Get advertiser diagnostic info
            if (bleHidManager.getAdvertisingManager() != null) {
                diagnosticInfo.append(bleHidManager.getAdvertisingManager().getDiagnosticInfo());
                diagnosticInfo.append("\n");
                
                // Update device capability info
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                boolean peripheralSupported = bleHidManager.getAdvertisingManager().getDeviceReportedPeripheralSupport();
                
                if (adapter != null) {
                    deviceNameText.setText("Device Name: " + adapter.getName() +
                                      " (Peripheral Mode: " + (peripheralSupported ? "✅" : "❌") + ")");
                }
            }
            
            // Get enhanced pairing manager info
            if (bleHidManager.getPairingManager() != null) {
                diagnosticInfo.append("\nPAIRING STATUS:\n");
                diagnosticInfo.append("Current State: ")
                             .append(bleHidManager.getPairingManager().getPairingState())
                             .append("\n");
                
                // Get bonded devices info
                String bondedDevicesInfo = bleHidManager.getPairingManager().getPairingStatusInfo();
                diagnosticInfo.append(bondedDevicesInfo);
            }
            
            // Add diagnostic info to the log
            addLogEntry("DIAGNOSTIC INFO:\n" + diagnosticInfo.toString());
            
            // Refresh UI state for controls
            updatePairingActionButton();
            updateConnectionStatus();
        }
    }
    
    /**
     * Adds a timestamped entry to the log.
     */
    private void addLogEntry(String entry) {
        String timestamp = DateFormat.format("HH:mm:ss", new Date()).toString();
        String logEntry = timestamp + " - " + entry + "\n";
        
        logEntries.insert(0, logEntry); // Add to the beginning
        
        // Trim if too long
        if (logEntries.length() > 2000) {
            logEntries.setLength(2000);
        }
        
        logText.setText(logEntries.toString());
    }
    
    @Override
    public void onHidEvent(String event) {
        // This is called from the fragments
        addLogEntry(event);
    }
    
    /**
     * Convert a Bluetooth bond state to a readable string.
     */
    private String bondStateToString(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_NONE:
                return "BOND_NONE";
            case BluetoothDevice.BOND_BONDING:
                return "BOND_BONDING";
            case BluetoothDevice.BOND_BONDED:
                return "BOND_BONDED";
            default:
                return "UNKNOWN(" + bondState + ")";
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
        updateDeviceInfo();
        
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
        
        addLogEntry("BLUETOOTH: Receiver registered for state changes");
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // Stop advertising when the activity is not visible
        if (bleHidManager != null) {
            bleHidManager.stopAdvertising();
            updateAdvertisingStatus(false);
        }
        
        // Unregister Bluetooth receiver
        try {
            unregisterReceiver(bluetoothReceiver);
            addLogEntry("BLUETOOTH: Receiver unregistered");
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
     * Tab adapter for the ViewPager2
     */
    private class TabAdapter extends FragmentStateAdapter {
        public TabAdapter(FragmentActivity fa) {
            super(fa);
        }
        
        @Override
        public int getItemCount() {
            return NUM_TABS;
        }
        
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case TAB_MOUSE:
                    return mouseFragment;
                case TAB_KEYBOARD:
                    return keyboardFragment;
                case TAB_MEDIA:
                    return mediaFragment;
                default:
                    throw new IllegalArgumentException("Invalid position: " + position);
            }
        }
    }
}
