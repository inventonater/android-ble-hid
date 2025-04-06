package com.example.blehid.app.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.blehid.app.R;
import com.example.blehid.tv.TvBleHidManager;
import com.example.blehid.tv.TvHidService;
import com.example.blehid.tv.TvHidServiceFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Activity for controlling Smart TVs via BLE HID.
 * Provides an interface with pointer, d-pad, media, and navigation controls.
 */
public class TvRemoteActivity extends AppCompatActivity {
    private static final String TAG = "TvRemoteActivity";
    
    private TvBleHidManager tvHidManager;
    private Button advertisingButton;
    private TextView statusText;
    private TextView connectionText;
    private TextView logText;
    private Spinner tvBrandSpinner;
    
    // Touchpad state for pointer movement
    private FrameLayout touchpadArea;
    private float lastTouchX;
    private float lastTouchY;
    private boolean isMoving = false;
    
    // StringBuilder for log entries
    private StringBuilder logEntries = new StringBuilder();
    
    // Buttons
    private Button clickButton;
    private Button upButton, downButton, leftButton, rightButton, centerButton;
    private Button backButton, homeButton;
    private Button playPauseButton, previousButton, nextButton;
    private Button volumeUpButton, volumeDownButton, muteButton;
    
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
                case BluetoothDevice.ACTION_BOND_STATE_CHANGED:
                    int bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR);
                    int prevBondState = intent.getIntExtra(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE, BluetoothDevice.ERROR);
                    String bondStateStr = bondStateToString(bondState);
                    String prevBondStateStr = bondStateToString(prevBondState);
                    addLogEntry("BOND STATE: " + deviceInfo + " " + prevBondStateStr + " -> " + bondStateStr);
                    
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
                    
                    // Auto-accept pairing if appropriate for this variant
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        // PAIRING_VARIANT_CONSENT = 3, PAIRING_VARIANT_PASSKEY_CONFIRMATION = 4
                        if (variant == 3 || variant == 4) {
                            addLogEntry("AUTO-ACCEPTING pairing request");
                            try {
                                if (ActivityCompat.checkSelfPermission(TvRemoteActivity.this, 
                                        Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                                    device.setPairingConfirmation(true);
                                }
                            } catch (Exception e) {
                                addLogEntry("ERROR auto-accepting: " + e.getMessage());
                            }
                        }
                    }
                    break;
            }
        }
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tv_remote);
        
        // Initialize views
        statusText = findViewById(R.id.statusText);
        connectionText = findViewById(R.id.connectionText);
        advertisingButton = findViewById(R.id.advertisingButton);
        logText = findViewById(R.id.logText);
        tvBrandSpinner = findViewById(R.id.tvBrandSpinner);
        
        // Initialize touchpad area
        touchpadArea = findViewById(R.id.touchpadArea);
        
        // Initialize buttons
        clickButton = findViewById(R.id.clickButton);
        upButton = findViewById(R.id.upButton);
        downButton = findViewById(R.id.downButton);
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);
        centerButton = findViewById(R.id.centerButton);
        backButton = findViewById(R.id.backButton);
        homeButton = findViewById(R.id.homeButton);
        playPauseButton = findViewById(R.id.playPauseButton);
        previousButton = findViewById(R.id.previousButton);
        nextButton = findViewById(R.id.nextButton);
        volumeUpButton = findViewById(R.id.volumeUpButton);
        volumeDownButton = findViewById(R.id.volumeDownButton);
        muteButton = findViewById(R.id.muteButton);
        
        // Initialize the TV BLE HID manager
        tvHidManager = new TvBleHidManager(this);
        
        // Set up TV brand spinner
        setupTvBrandSpinner();
        
        // Set up advertising button
        advertisingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAdvertising();
            }
        });
        
        // Set up touchpad area
        touchpadArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouchpadEvent(event);
            }
        });
        
        // Set up button click listeners
        setupButtonListeners();
        
        // Initialize BLE HID functionality
        initializeBleHid();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
        
        // Register for Bluetooth state changes
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
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
        if (tvHidManager != null) {
            tvHidManager.stopAdvertising();
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
        if (tvHidManager != null) {
            tvHidManager.close();
        }
    }
    
    /**
     * Sets up the TV brand spinner with available TV types.
     */
    private void setupTvBrandSpinner() {
        // Get the list of supported TV types
        String[] tvTypes = TvHidServiceFactory.getSupportedTvTypes();
        
        // Create a list of friendly names for the UI
        List<String> tvBrandNames = new ArrayList<>();
        for (String tvType : tvTypes) {
            tvBrandNames.add(TvHidServiceFactory.getTvTypeName(tvType));
        }
        
        // Create adapter for the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, tvBrandNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // Apply adapter to the spinner
        tvBrandSpinner.setAdapter(adapter);
        
        // Set initial selection based on current TV type
        String currentTvType = tvHidManager.getCurrentTvType();
        for (int i = 0; i < tvTypes.length; i++) {
            if (tvTypes[i].equals(currentTvType)) {
                tvBrandSpinner.setSelection(i);
                break;
            }
        }
        
        // Set spinner listener
        tvBrandSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedTvType = tvTypes[position];
                
                // Only update if the TV type has changed
                if (!selectedTvType.equals(tvHidManager.getCurrentTvType())) {
                    addLogEntry("Switching to " + tvBrandNames.get(position));
                    boolean wasAdvertising = tvHidManager.isAdvertising();
                    
                    // Set the new TV type
                    tvHidManager.setTvType(selectedTvType);
                    
                    // Restart advertising if it was active
                    if (wasAdvertising) {
                        tvHidManager.startAdvertising();
                    }
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    /**
     * Sets up button click listeners for all remote control buttons.
     */
    private void setupButtonListeners() {
        // Click/OK button
        clickButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Click");
                tvHidManager.clickSelectButton();
            }
        });
        
        // D-pad buttons
        upButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Up");
                tvHidManager.up();
            }
        });
        
        downButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Down");
                tvHidManager.down();
            }
        });
        
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Left");
                tvHidManager.left();
            }
        });
        
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Right");
                tvHidManager.right();
            }
        });
        
        centerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Center/OK");
                tvHidManager.center();
            }
        });
        
        // Navigation buttons
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Back");
                tvHidManager.pressBackButton();
            }
        });
        
        homeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Home");
                tvHidManager.pressHomeButton();
            }
        });
        
        // Media control buttons
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Play/Pause");
                tvHidManager.playPause();
            }
        });
        
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Previous");
                tvHidManager.previousTrack();
            }
        });
        
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Next");
                tvHidManager.nextTrack();
            }
        });
        
        volumeUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Volume Up");
                tvHidManager.volumeUp();
            }
        });
        
        volumeDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Volume Down");
                tvHidManager.volumeDown();
            }
        });
        
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addLogEntry("BUTTON: Mute");
                tvHidManager.mute();
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
        if (!tvHidManager.isConnected()) {
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
                        
                        // Send the pointer movement
                        boolean result = tvHidManager.moveCursor(scaledDeltaX, scaledDeltaY);
                        
                        if (result) {
                            // Update last position if the movement was sent successfully
                            lastTouchX = currentX;
                            lastTouchY = currentY;
                            
                            // Log only for significant movements to avoid spam
                            if (Math.abs(scaledDeltaX) > 5 || Math.abs(scaledDeltaY) > 5) {
                                addLogEntry("POINTER: X:" + scaledDeltaX + " Y:" + scaledDeltaY);
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
        boolean initialized = tvHidManager.initialize();
        if (initialized) {
            statusText.setText(R.string.ready);
            advertisingButton.setEnabled(true);
            addLogEntry("BLE HID initialized successfully for " + 
                    TvHidServiceFactory.getTvTypeName(tvHidManager.getCurrentTvType()));
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
        if (tvHidManager == null) return;
        
        // Check if we're already advertising
        boolean isCurrentlyAdvertising = tvHidManager.isAdvertising();
        if (isCurrentlyAdvertising) {
            tvHidManager.stopAdvertising();
            updateAdvertisingStatus(false);
            addLogEntry("ADVERTISING: Stopped");
        } else {
            addLogEntry("ADVERTISING: Attempting to start...");
            boolean result = tvHidManager.startAdvertising();
            updateAdvertisingStatus(result);
            
            if (result) {
                addLogEntry("ADVERTISING: Start initiated");
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
        if (tvHidManager != null && tvHidManager.isConnected()) {
            BluetoothDevice device = tvHidManager.getConnectedDevice();
            String deviceName = device.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = device.getAddress();
            }
            connectionText.setText(getString(R.string.connected_to, deviceName));
            addLogEntry("CONNECTION: Connected to " + deviceName + " (" + device.getAddress() + ")");
        } else {
            connectionText.setText(R.string.not_connected);
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
}
