package com.example.blehid.app.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blehid.app.R;
import com.example.blehid.core.BleHidManager;
import com.example.blehid.core.BlePairingManager;

import java.util.Date;

/**
 * Activity for testing the BLE HID keyboard functionality.
 * Provides a simple keyboard UI for sending key presses and managing BLE advertising.
 */
public class SimpleKeyboardActivity extends AppCompatActivity {
    private static final String TAG = "SimpleKeyboardActivity";
    
    // HID keyboard scan codes (USB HID spec)
    private static final int KEY_A = 0x04;
    private static final int KEY_B = 0x05;
    private static final int KEY_C = 0x06;
    private static final int KEY_D = 0x07;
    private static final int KEY_E = 0x08;
    private static final int KEY_F = 0x09;
    private static final int KEY_G = 0x0A;
    private static final int KEY_H = 0x0B;
    private static final int KEY_I = 0x0C;
    private static final int KEY_J = 0x0D;
    private static final int KEY_K = 0x0E;
    private static final int KEY_L = 0x0F;
    private static final int KEY_M = 0x10;
    private static final int KEY_N = 0x11;
    private static final int KEY_O = 0x12;
    private static final int KEY_P = 0x13;
    private static final int KEY_Q = 0x14;
    private static final int KEY_R = 0x15;
    private static final int KEY_S = 0x16;
    private static final int KEY_T = 0x17;
    private static final int KEY_U = 0x18;
    private static final int KEY_V = 0x19;
    private static final int KEY_W = 0x1A;
    private static final int KEY_X = 0x1B;
    private static final int KEY_Y = 0x1C;
    private static final int KEY_Z = 0x1D;
    private static final int KEY_1 = 0x1E;
    private static final int KEY_2 = 0x1F;
    private static final int KEY_3 = 0x20;
    private static final int KEY_4 = 0x21;
    private static final int KEY_5 = 0x22;
    private static final int KEY_6 = 0x23;
    private static final int KEY_7 = 0x24;
    private static final int KEY_8 = 0x25;
    private static final int KEY_9 = 0x26;
    private static final int KEY_0 = 0x27;
    private static final int KEY_RETURN = 0x28;  // Enter
    private static final int KEY_ESCAPE = 0x29;
    private static final int KEY_BACKSPACE = 0x2A;
    private static final int KEY_TAB = 0x2B;
    private static final int KEY_SPACE = 0x2C;
    
    private BleHidManager bleHidManager;
    private Button advertisingButton;
    private TextView statusText;
    private TextView connectionText;
    
    // Diagnostic views
    private TextView deviceNameText;
    private TextView deviceAddressText;
    private TextView pairingStateText;
    private TextView logText;
    
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
        setContentView(R.layout.activity_keyboard);
        
        // Initialize views
        statusText = findViewById(R.id.statusText);
        connectionText = findViewById(R.id.connectionText);
        advertisingButton = findViewById(R.id.advertisingButton);
        
        // Initialize diagnostic views
        deviceNameText = findViewById(R.id.deviceNameText);
        deviceAddressText = findViewById(R.id.deviceAddressText);
        pairingStateText = findViewById(R.id.pairingStateText);
        logText = findViewById(R.id.logText);
        
        // Initialize BLE HID manager
        bleHidManager = new BleHidManager(this);
        
        // Set up advertising button
        advertisingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAdvertising();
            }
        });
        
        // Set up pairing callback
        bleHidManager.getBlePairingManager().setPairingCallback(new BlePairingManager.PairingCallback() {
            @Override
            public void onPairingRequested(BluetoothDevice device, int variant) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = "Pairing requested by " + device.getAddress() + 
                                         ", variant: " + variant;
                        
                        Toast.makeText(SimpleKeyboardActivity.this, message, Toast.LENGTH_SHORT).show();
                        addLogEntry("PAIRING REQUESTED: " + message);
                        updatePairingState("REQUESTED");
                        
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
                        
                        Toast.makeText(SimpleKeyboardActivity.this, message, Toast.LENGTH_SHORT).show();
                        addLogEntry("PAIRING COMPLETE: " + message);
                        updatePairingState(result);
                        updateConnectionStatus();
                        updateDeviceInfo();
                    }
                });
            }
        });
        
        // Set up key buttons
        setupKeyButtons();
        
        // Initialize BLE HID functionality
        initializeBleHid();
        
        // Show device info
        updateDeviceInfo();
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
    protected void onDestroy() {
        super.onDestroy();
        if (bleHidManager != null) {
            bleHidManager.close();
        }
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
        } else {
            statusText.setText(R.string.initialization_failed);
            advertisingButton.setEnabled(false);
            addLogEntry("BLE HID initialization FAILED");
        }
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
        pairingStateText.setText("Pairing State: " + state);
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
    
    /**
     * Sets up the click listeners for key buttons.
     */
    private void setupKeyButtons() {
        // Letter keys
        setupKeyButton(R.id.buttonA, KEY_A);
        setupKeyButton(R.id.buttonB, KEY_B);
        setupKeyButton(R.id.buttonC, KEY_C);
        setupKeyButton(R.id.buttonD, KEY_D);
        setupKeyButton(R.id.buttonE, KEY_E);
        setupKeyButton(R.id.buttonF, KEY_F);
        setupKeyButton(R.id.buttonG, KEY_G);
        setupKeyButton(R.id.buttonH, KEY_H);
        setupKeyButton(R.id.buttonI, KEY_I);
        setupKeyButton(R.id.buttonJ, KEY_J);
        setupKeyButton(R.id.buttonK, KEY_K);
        setupKeyButton(R.id.buttonL, KEY_L);
        setupKeyButton(R.id.buttonM, KEY_M);
        setupKeyButton(R.id.buttonN, KEY_N);
        setupKeyButton(R.id.buttonO, KEY_O);
        setupKeyButton(R.id.buttonP, KEY_P);
        setupKeyButton(R.id.buttonQ, KEY_Q);
        setupKeyButton(R.id.buttonR, KEY_R);
        setupKeyButton(R.id.buttonS, KEY_S);
        setupKeyButton(R.id.buttonT, KEY_T);
        setupKeyButton(R.id.buttonU, KEY_U);
        setupKeyButton(R.id.buttonV, KEY_V);
        setupKeyButton(R.id.buttonW, KEY_W);
        setupKeyButton(R.id.buttonX, KEY_X);
        setupKeyButton(R.id.buttonY, KEY_Y);
        setupKeyButton(R.id.buttonZ, KEY_Z);
        
        // Special keys
        setupKeyButton(R.id.buttonSpace, KEY_SPACE);
        setupKeyButton(R.id.buttonEnter, KEY_RETURN);
        setupKeyButton(R.id.buttonBackspace, KEY_BACKSPACE);
    }
    
    /**
     * Sets up a click listener for a key button.
     * 
     * @param buttonId The button resource ID
     * @param keyCode The HID key code to send
     */
    private void setupKeyButton(int buttonId, final int keyCode) {
        Button button = findViewById(buttonId);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    sendKey(keyCode);
                }
            });
        }
    }
    
    /**
     * Sends a key press and release.
     * 
     * @param keyCode The HID key code to send
     */
    private void sendKey(int keyCode) {
        if (bleHidManager.isConnected()) {
            // Send key press
            boolean result = bleHidManager.sendKey(keyCode);
            addLogEntry("KEY PRESS: 0x" + Integer.toHexString(keyCode) + 
                       (result ? " sent" : " FAILED"));
            
            // Release key after a short delay
            findViewById(android.R.id.content).postDelayed(new Runnable() {
                @Override
                public void run() {
                    boolean releaseResult = bleHidManager.releaseKeys();
                    addLogEntry("KEY RELEASE: " + (releaseResult ? "success" : "FAILED"));
                }
            }, 100);
        } else {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            addLogEntry("KEY PRESS IGNORED: No connected device");
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
            boolean result = bleHidManager.startAdvertising();
            updateAdvertisingStatus(result);
            
            if (result) {
                addLogEntry("ADVERTISING: Started");
            } else {
                addLogEntry("ADVERTISING: Failed to start");
                Toast.makeText(this, R.string.advertising_failed, Toast.LENGTH_SHORT).show();
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
     * Updates the connection status text.
     */
    private void updateConnectionStatus() {
        if (bleHidManager != null && bleHidManager.isConnected()) {
            BluetoothDevice device = bleHidManager.getConnectedDevice();
            String deviceName = device.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = device.getAddress();
            }
            connectionText.setText(getString(R.string.connected_to, deviceName));
            addLogEntry("CONNECTION: Connected to " + deviceName + " (" + device.getAddress() + ")");
            
            // Check HID profile status for the connected device
            checkHidProfileStatus(device);
        } else {
            connectionText.setText(R.string.not_connected);
        }
    }
    
    /**
     * Check and log HID profile status for a device.
     */
    private void checkHidProfileStatus(BluetoothDevice device) {
        if (device == null) return;
        
        try {
            // Reflect BluetoothHidDevice.getConnectionState
            Class<?> profileClass = Class.forName("android.bluetooth.BluetoothProfile");
            int hidDeviceProfile = BluetoothProfile.HID_DEVICE;
            
            addLogEntry("CHECKING HID PROFILE: Device " + device.getAddress());
            
            // Get bond state
            int bondState = device.getBondState();
            addLogEntry("BOND STATE: " + bondStateToString(bondState));
            
            // Check UUIDs supported by device
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.os.ParcelUuid[] uuids = device.getUuids();
                if (uuids != null) {
                    addLogEntry("DEVICE UUIDS: " + uuids.length + " UUIDs");
                    for (android.os.ParcelUuid uuid : uuids) {
                        addLogEntry("  - " + uuid.toString());
                        
                        // Check if this is HID UUID
                        if (uuid.toString().toLowerCase().contains("1812")) {
                            addLogEntry("    (HID SERVICE SUPPORTED)");
                        }
                    }
                } else {
                    addLogEntry("DEVICE UUIDS: None or not available");
                }
            }
            
            // Try to get device type
            int deviceType = device.getType();
            String typeStr = "";
            switch (deviceType) {
                case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                    typeStr = "CLASSIC";
                    break;
                case BluetoothDevice.DEVICE_TYPE_LE:
                    typeStr = "LE";
                    break;
                case BluetoothDevice.DEVICE_TYPE_DUAL:
                    typeStr = "DUAL";
                    break;
                case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                    typeStr = "UNKNOWN";
                    break;
            }
            addLogEntry("DEVICE TYPE: " + typeStr);
            
        } catch (Exception e) {
            addLogEntry("ERROR checking HID profile: " + e.getMessage());
        }
    }
}
