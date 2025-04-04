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
import android.widget.FrameLayout;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blehid.app.R;
import com.example.blehid.core.BleHidManager;
import com.example.blehid.core.BlePairingManager;
import com.example.blehid.core.HidMediaService;

import java.util.Date;

/**
 * Activity for testing the BLE HID media player functionality.
 * Provides an interface for controlling media playback of connected devices.
 */
public class SimpleMediaActivity extends AppCompatActivity {
    private static final String TAG = "SimpleMediaActivity";
    
    private BleHidManager bleHidManager;
    private Button advertisingButton;
    private TextView statusText;
    private TextView connectionText;
    
    // Diagnostic views
    private TextView deviceNameText;
    private TextView deviceAddressText;
    private TextView pairingStateText;
    private TextView logText;
    
    // Media controls
    private Button playPauseButton;
    private Button previousButton;
    private Button nextButton;
    private Button volumeUpButton;
    private Button volumeDownButton;
    private Button muteButton;
    
    // Mouse controls
    private FrameLayout touchpadArea;
    private Button leftButton;
    private Button middleButton;
    private Button rightButton;
    
    // Touchpad state tracking
    private float lastTouchX;
    private float lastTouchY;
    private boolean isMoving = false;
    
    // StringBuilder for log entries
    private StringBuilder logEntries = new StringBuilder();
    
    // Functional interface for media control actions with boolean return
    private interface MediaControlAction {
        boolean execute();
    }
    
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
        setContentView(R.layout.activity_media);
        
        // Initialize views
        statusText = findViewById(R.id.statusText);
        connectionText = findViewById(R.id.connectionText);
        advertisingButton = findViewById(R.id.advertisingButton);
        
        // Initialize diagnostic views
        deviceNameText = findViewById(R.id.deviceNameText);
        deviceAddressText = findViewById(R.id.deviceAddressText);
        pairingStateText = findViewById(R.id.pairingStateText);
        logText = findViewById(R.id.logText);
        
        // Initialize media controls
        playPauseButton = findViewById(R.id.playPauseButton);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        volumeUpButton = findViewById(R.id.volumeUpButton);
        volumeDownButton = findViewById(R.id.volumeDownButton);
        muteButton = findViewById(R.id.muteButton);
        
        // Initialize mouse controls
        touchpadArea = findViewById(R.id.touchpadArea);
        leftButton = findViewById(R.id.leftButton);
        middleButton = findViewById(R.id.middleButton);
        rightButton = findViewById(R.id.rightButton);
        
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
                        
                        Toast.makeText(SimpleMediaActivity.this, message, Toast.LENGTH_SHORT).show();
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
                        
                        Toast.makeText(SimpleMediaActivity.this, message, Toast.LENGTH_SHORT).show();
                        addLogEntry("PAIRING COMPLETE: " + message);
                        updatePairingState(result);
                        updateConnectionStatus();
                        updateDeviceInfo();
                    }
                });
            }
        });
        
        // Set up media controls
        setupMediaControls();
        
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
     * Sets up the mouse control buttons and touchpad.
     */
    private void setupMouseControls() {
        // Set up touchpad area for mouse movement
        touchpadArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouchpadEvent(event);
            }
        });
        
        // Left mouse button
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("MOUSE: Left click");
                if (bleHidManager.isConnected()) {
                    boolean result = bleHidManager.clickMouseButton(HidMediaService.BUTTON_LEFT);
                    if (!result) {
                        Toast.makeText(SimpleMediaActivity.this, 
                                "Failed to send left click", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SimpleMediaActivity.this, 
                            R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Middle mouse button
        middleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("MOUSE: Middle click");
                if (bleHidManager.isConnected()) {
                    boolean result = bleHidManager.clickMouseButton(HidMediaService.BUTTON_MIDDLE);
                    if (!result) {
                        Toast.makeText(SimpleMediaActivity.this, 
                                "Failed to send middle click", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SimpleMediaActivity.this, 
                            R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        // Right mouse button
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("MOUSE: Right click");
                if (bleHidManager.isConnected()) {
                    boolean result = bleHidManager.clickMouseButton(HidMediaService.BUTTON_RIGHT);
                    if (!result) {
                        Toast.makeText(SimpleMediaActivity.this, 
                                "Failed to send right click", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(SimpleMediaActivity.this, 
                            R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    /**
     * Handles touch events on the touchpad area.
     * 
     * @param event The MotionEvent
     * @return true if the event was handled, false otherwise
     */
    private boolean handleTouchpadEvent(MotionEvent event) {
        if (!bleHidManager.isConnected()) {
            return false;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isMoving = true;
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (isMoving) {
                    float currentX = event.getX();
                    float currentY = event.getY();
                    
                    // Calculate movement delta
                    float deltaX = currentX - lastTouchX;
                    float deltaY = currentY - lastTouchY;
                    
                    // Scale movement (adjust sensitivity as needed)
                    int scaledDeltaX = (int)(deltaX * 0.8);
                    int scaledDeltaY = (int)(deltaY * 0.8);
                    
                    // Only send if there's significant movement
                    if (Math.abs(scaledDeltaX) > 0 || Math.abs(scaledDeltaY) > 0) {
                        // Clamp to valid range (-127 to 127)
                        scaledDeltaX = Math.max(-127, Math.min(127, scaledDeltaX));
                        scaledDeltaY = Math.max(-127, Math.min(127, scaledDeltaY));
                        
                        // Send the mouse movement
                        boolean result = bleHidManager.moveMouse(scaledDeltaX, scaledDeltaY);
                        
                        if (result) {
                            // Update last position if the movement was sent successfully
                            lastTouchX = currentX;
                            lastTouchY = currentY;
                            
                            // Log only for significant movements to avoid spam
                            if (Math.abs(scaledDeltaX) > 5 || Math.abs(scaledDeltaY) > 5) {
                                addLogEntry("MOUSE: Move X:" + scaledDeltaX + " Y:" + scaledDeltaY);
                            }
                        }
                    }
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isMoving = false;
                return true;
        }
        
        return false;
    }
    
    /**
     * Sets up the media control buttons.
     */
    private void setupMediaControls() {
        // Play/Pause button
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("PLAY/PAUSE", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.playPause();
                    }
                });
            }
        });
        
        // Previous track button
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("PREVIOUS", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.previousTrack();
                    }
                });
            }
        });
        
        // Next track button
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("NEXT", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.nextTrack();
                    }
                });
            }
        });
        
        // Volume up button
        volumeUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("VOLUME UP", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.volumeUp();
                    }
                });
            }
        });
        
        // Volume down button
        volumeDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("VOLUME DOWN", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.volumeDown();
                    }
                });
            }
        });
        
        // Mute button
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("MUTE", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.mute();
                    }
                });
            }
        });
    }
    
    /**
     * Helper method to send a media control command.
     * 
     * @param controlName Name of the control for logging
     * @param controlAction MediaControlAction that performs the control action and returns a boolean result
     */
    private void sendMediaControl(String controlName, MediaControlAction controlAction) {
        if (!bleHidManager.isConnected()) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            addLogEntry("MEDIA CONTROL IGNORED: No connected device");
            return;
        }
        
        addLogEntry("MEDIA CONTROL: " + controlName);
        
        // Execute the control action
        boolean result = controlAction.execute();
        
        if (result) {
            addLogEntry("MEDIA CONTROL: " + controlName + " sent");
        } else {
            addLogEntry("MEDIA CONTROL: " + controlName + " FAILED");
            Toast.makeText(this, "Failed to send " + controlName, Toast.LENGTH_SHORT).show();
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
