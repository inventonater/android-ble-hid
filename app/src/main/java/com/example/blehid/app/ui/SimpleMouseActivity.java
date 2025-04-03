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
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blehid.app.R;
import com.example.blehid.core.BleHidManager;
import com.example.blehid.core.BlePairingManager;
import com.example.blehid.core.HidMouseService;

import java.util.Date;

/**
 * Activity for testing the BLE HID mouse functionality.
 * Provides a touchpad-like interface for controlling the mouse pointer.
 */
public class SimpleMouseActivity extends AppCompatActivity {
    private static final String TAG = "SimpleMouseActivity";
    
    private BleHidManager bleHidManager;
    private Button advertisingButton;
    private TextView statusText;
    private TextView connectionText;
    
    // Diagnostic views
    private TextView deviceNameText;
    private TextView deviceAddressText;
    private TextView pairingStateText;
    private TextView logText;
    
    // Mouse controls
    private View touchpadArea;
    private Button leftButton;
    private Button middleButton;
    private Button rightButton;
    private Button scrollUpButton;
    private Button scrollDownButton;
    
    // Touch processing
    private float lastTouchX;
    private float lastTouchY;
    private boolean isTrackingTouch = false;
    
    // Movement sensitivity - adjusted for better control
    private static final float MOVEMENT_FACTOR = 1.5f;
    
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
        setContentView(R.layout.activity_mouse);
        
        // Initialize views
        statusText = findViewById(R.id.statusText);
        connectionText = findViewById(R.id.connectionText);
        advertisingButton = findViewById(R.id.advertisingButton);
        
        // Initialize diagnostic views
        deviceNameText = findViewById(R.id.deviceNameText);
        deviceAddressText = findViewById(R.id.deviceAddressText);
        pairingStateText = findViewById(R.id.pairingStateText);
        logText = findViewById(R.id.logText);
        
        // Initialize mouse controls
        touchpadArea = findViewById(R.id.touchpadArea);
        leftButton = findViewById(R.id.leftButton);
        middleButton = findViewById(R.id.middleButton);
        rightButton = findViewById(R.id.rightButton);
        scrollUpButton = findViewById(R.id.scrollUpButton);
        scrollDownButton = findViewById(R.id.scrollDownButton);
        
        // Initialize BLE HID manager
        bleHidManager = new BleHidManager(this);
        
    // Set up advertising button
    advertisingButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleAdvertising();
            
            // Update diagnostic info after advertising toggle
            updateDiagnosticInfo();
        }
    });
    
    // Set up timer to refresh diagnostic info every 2 seconds
    new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
            updateDiagnosticInfo();
            new Handler().postDelayed(this, 2000);
        }
    }, 2000);
        
        // Set up pairing callback
        bleHidManager.getBlePairingManager().setPairingCallback(new BlePairingManager.PairingCallback() {
            @Override
            public void onPairingRequested(BluetoothDevice device, int variant) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = "Pairing requested by " + device.getAddress() + 
                                         ", variant: " + variant;
                        
                        Toast.makeText(SimpleMouseActivity.this, message, Toast.LENGTH_SHORT).show();
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
                        
                        Toast.makeText(SimpleMouseActivity.this, message, Toast.LENGTH_SHORT).show();
                        addLogEntry("PAIRING COMPLETE: " + message);
                        updatePairingState(result);
                        updateConnectionStatus();
                        updateDeviceInfo();
                    }
                });
            }
        });
        
        // Set up mouse controls
        setupMouseControls();
        
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
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleHidManager != null) {
            bleHidManager.close();
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
     * Sets up the mouse control elements.
     */
    private void setupMouseControls() {
        // Set up touchpad area
        touchpadArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouchpad(event);
            }
        });
        
        // Set up mouse buttons
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickButton(HidMouseService.BUTTON_LEFT);
            }
        });
        
        middleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickButton(HidMouseService.BUTTON_MIDDLE);
            }
        });
        
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickButton(HidMouseService.BUTTON_RIGHT);
            }
        });
        
        // Set up scroll buttons
        scrollUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scroll(10); // Positive = scroll up
            }
        });
        
        scrollDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scroll(-10); // Negative = scroll down
            }
        });
    }
    
    /**
     * Handles touch events on the touchpad area.
     */
    private boolean handleTouchpad(MotionEvent event) {
        if (!bleHidManager.isConnected()) {
            addLogEntry("TOUCH IGNORED: No connected device");
            return false;
        }
        
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                isTrackingTouch = true;
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (isTrackingTouch) {
                    // Calculate movement delta
                    float deltaX = x - lastTouchX;
                    float deltaY = y - lastTouchY;
                    
                    // Only send movement if it's significant
                    if (Math.abs(deltaX) > 1 || Math.abs(deltaY) > 1) {
                        // Convert to relative movement values (-127 to 127)
                        // Use more natural movement values without forced minimums
                        int moveX = (int)(deltaX * MOVEMENT_FACTOR);
                        int moveY = (int)(deltaY * MOVEMENT_FACTOR);
                        
                        // Ensure small movements aren't lost but don't force large jumps
                        if (moveX != 0 && Math.abs(moveX) < 2) {
                            moveX = moveX > 0 ? 2 : -2;
                        }
                        if (moveY != 0 && Math.abs(moveY) < 2) {
                            moveY = moveY > 0 ? 2 : -2;
                        }
                        
                        // Clamp values
                        moveX = Math.max(-127, Math.min(127, moveX));
                        moveY = Math.max(-127, Math.min(127, moveY));
                        
                        // Log the original delta values for debugging
                        Log.d(TAG, "TOUCH DELTA - Original: (" + deltaX + ", " + deltaY + ")");
                        
                        // Log before sending to HID service
                        Log.d(TAG, "SENDING TO HID - moveX: " + moveX + ", moveY: " + moveY);
                        
                        // Send mouse movement with debug info
                        boolean result = bleHidManager.moveMouse(moveX, moveY);
                        if (result) {
                            addLogEntry("MOUSE MOVE: deltaXY(" + deltaX + ", " + deltaY + 
                                       ") → moveXY(" + moveX + ", " + moveY + ")");
                            
                            // Add small delay to avoid overwhelming the connection
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        } else {
                            addLogEntry("MOUSE MOVE FAILED: (" + moveX + ", " + moveY + ")");
                        }
                        
                        // Update last position
                        lastTouchX = x;
                        lastTouchY = y;
                    }
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTrackingTouch = false;
                break;
        }
        
        return false;
    }
    
    /**
     * Clicks a mouse button.
     */
    private void clickButton(int button) {
        if (!bleHidManager.isConnected()) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            addLogEntry("BUTTON CLICK IGNORED: No connected device");
            return;
        }
        
        // Send a press event first with a short delay before releasing
        addLogEntry("BUTTON PRESS: Button " + button);
        boolean pressResult = bleHidManager.pressMouseButton(button);
        
        // Add a longer delay for button press (50ms)
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        boolean releaseResult = bleHidManager.releaseMouseButtons();
        boolean result = pressResult && releaseResult;
        
        String buttonName;
        switch (button) {
            case HidMouseService.BUTTON_LEFT:
                buttonName = "LEFT";
                break;
            case HidMouseService.BUTTON_RIGHT:
                buttonName = "RIGHT";
                break;
            case HidMouseService.BUTTON_MIDDLE:
                buttonName = "MIDDLE";
                break;
            default:
                buttonName = "UNKNOWN";
        }
        
        addLogEntry("MOUSE BUTTON: " + buttonName + (result ? " clicked" : " FAILED"));
    }
    
    /**
     * Scrolls the mouse wheel.
     */
    private void scroll(int amount) {
        if (!bleHidManager.isConnected()) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            addLogEntry("SCROLL IGNORED: No connected device");
            return;
        }
        
        // Increase scroll amount for better recognition
        int scaledAmount = amount * 3;
        
        boolean result = bleHidManager.scrollMouseWheel(scaledAmount);
        addLogEntry("SCROLL: " + scaledAmount + (result ? "" : " FAILED"));
        
        // Add a small delay after scrolling
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
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
     * Updates the diagnostic information display.
     */
    private void updateDiagnosticInfo() {
        if (bleHidManager != null && bleHidManager.getAdvertiser() != null) {
            // Get diagnostic info from the advertiser
            String diagnosticInfo = bleHidManager.getAdvertiser().getDiagnosticInfo();
            
            // Add diagnostic info to the top of the log
            addLogEntry("DIAGNOSTIC INFO:\n" + diagnosticInfo);
            
            // Update device capability info
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            boolean peripheralSupported = bleHidManager.getAdvertiser().getDeviceReportedPeripheralSupport();
            
            if (adapter != null) {
                deviceNameText.setText("Device Name: " + adapter.getName() +
                                  " (Peripheral Mode: " + (peripheralSupported ? "✅" : "❌") + ")");
            }
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
                // Note: actual success is determined in the callback in BleAdvertiser
            } else {
                String errorMsg = bleHidManager.getAdvertiser().getLastErrorMessage();
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
