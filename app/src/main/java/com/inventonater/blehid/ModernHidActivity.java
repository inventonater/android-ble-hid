package com.inventonater.blehid;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Switch;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.DateFormat;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.lang.reflect.Method;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.inventonater.blehid.app.R;
import com.inventonater.blehid.debug.BleHidDebugger;

import java.util.Date;

/**
 * Modern HID Activity using the BluetoothHidDevice API.
 * This activity provides a simple UI for testing the HID functionality.
 */
public class ModernHidActivity extends AppCompatActivity implements HidDebugListener {
    private static final String TAG = "ModernHidActivity";
    
    // Permission request code
    private static final int REQUEST_WRITE_STORAGE = 101;
    
    // UI Elements
    private TextView statusText;
    private TextView connectionText;
    private Button registerButton;
    private Button mouseMoveButton;
    private Button mouseClickButton;
    private Button keyboardButton;
    private Button mediaButton;
    private TextView logTextView;
    private NestedScrollView logScrollView;
    private Button debugButton;
    private Button clearLogButton;
    private Button reportStatsButton;
    
    // Modern HID implementation and debugging
    private HidManager hidManager;
    private BleHidDebugger hidDebugger;
    
    // Log buffer
    private StringBuilder logBuffer = new StringBuilder();
    
    // Bluetooth state receiver
    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            
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
                    
                    // Let the debugger analyze the bond state change
                    if (hidManager != null && hidManager.getDebugger() != null) {
                        hidManager.getDebugger().analyzePairingStateChange(device, bondState, prevBondState);
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
    
    // Bluetooth permissions request codes
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 102;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modern_hid);
        
        // Initialize UI elements
        initializeUI();
        
        // Request Bluetooth permissions first
        if (checkAndRequestBluetoothPermissions()) {
            setupHidManager();
        } else {
            statusText.setText("Waiting for permissions...");
            disableAllButtons(true);
        }
    }
    
    private void setupHidManager() {
        // Initialize the HID manager and debugger
        hidManager = new HidManager(this);
        hidManager.setDebugListener(this);
        hidDebugger = hidManager.getDebugger();
        
        // Set up button click listeners
        setupButtons();
        
        // Initialize the HID manager
        if (!initializeHidManager()) {
            statusText.setText("Initialization failed");
            disableAllButtons(true);
            return;
        }
        
        // Register for Bluetooth state changes
        registerBluetoothReceiver();
    }
    
    /**
     * Checks if required Bluetooth permissions are granted,
     * and requests them if necessary.
     *
     * @return true if all permissions are already granted, false otherwise
     */
    private boolean checkAndRequestBluetoothPermissions() {
        // For Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            boolean hasConnectPermission = ContextCompat.checkSelfPermission(this, 
                    android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
            boolean hasAdvertisePermission = ContextCompat.checkSelfPermission(this, 
                    android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED;
                    
            if (!hasConnectPermission || !hasAdvertisePermission) {
                addLogEntry("Requesting Bluetooth permissions for Android 12+");
                ActivityCompat.requestPermissions(this, 
                        new String[]{
                            android.Manifest.permission.BLUETOOTH_CONNECT,
                            android.Manifest.permission.BLUETOOTH_ADVERTISE
                        }, 
                        REQUEST_BLUETOOTH_PERMISSIONS);
                return false;
            }
            
            addLogEntry("Bluetooth permissions already granted");
            return true;
        } 
        // For Android 6.0 - 11 (API 23-30), location permission is needed for BLE
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean hasLocationPermission = ContextCompat.checkSelfPermission(this, 
                    android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
                    
            if (!hasLocationPermission) {
                addLogEntry("Requesting location permission for BLE on Android 6-11");
                ActivityCompat.requestPermissions(this, 
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 
                        REQUEST_BLUETOOTH_PERMISSIONS);
                return false;
            }
            
            addLogEntry("Location permission already granted");
            return true;
        }
        
        // For Android 5.1 and below, permissions are granted at install time
        addLogEntry("Running on Android 5.1 or lower - permissions granted at install time");
        return true;
    }
    
    private void initializeUI() {
        statusText = findViewById(R.id.status_text);
        connectionText = findViewById(R.id.connection_text);
        registerButton = findViewById(R.id.register_button);
        mouseMoveButton = findViewById(R.id.mouse_move_button);
        mouseClickButton = findViewById(R.id.mouse_click_button);
        keyboardButton = findViewById(R.id.keyboard_button);
        mediaButton = findViewById(R.id.media_button);
        logTextView = findViewById(R.id.log_text);
        logScrollView = findViewById(R.id.log_scroll_view);
        debugButton = findViewById(R.id.debug_button);
        clearLogButton = findViewById(R.id.clear_log_button);
        reportStatsButton = findViewById(R.id.report_stats_button);
        
        // Set up scrolling for the log
        logTextView.setMovementMethod(new ScrollingMovementMethod());
        
        // Set up additional debug buttons
        if (debugButton != null) {
            debugButton.setOnClickListener(v -> showDebugOptionsDialog());
        }
        
        if (clearLogButton != null) {
            clearLogButton.setOnClickListener(v -> {
                logBuffer.setLength(0);
                logTextView.setText("");
                addLogEntry("Log cleared");
            });
        }
        
        if (reportStatsButton != null) {
            reportStatsButton.setOnClickListener(v -> {
                if (hidDebugger != null) {
                    addLogEntry("Report Statistics:" + hidDebugger.getReportStatistics());
                }
            });
        }
    }
    
    private void setupButtons() {
        // Register/Unregister button
        registerButton.setOnClickListener(v -> {
            if (hidManager.isRegistered()) {
                hidManager.close();
                registerButton.setText("Register HID Device");
                statusText.setText("Not registered");
                disableActionButtons(true);
                addLogEntry("HID: Unregistered");
            } else {
                boolean registered = hidManager.register();
                registerButton.setText(registered ? "Unregister HID Device" : "Register HID Device");
                statusText.setText(registered ? "Registered - waiting for connection" : "Registration failed");
                disableActionButtons(!registered);
                addLogEntry("HID: Registration " + (registered ? "successful" : "failed"));
            }
        });
        
        // Mouse move button
        mouseMoveButton.setOnClickListener(v -> {
            if (hidManager.isConnected() && hidManager.getMouseReporter() != null) {
                addLogEntry("MOUSE: Sending movement pattern");
                
                // Create a pattern of mouse movements for a small circle
                new Thread(() -> {
                    try {
                        for (int i = 0; i < 36; i++) {
                            double angle = i * 10 * Math.PI / 180;
                            int x = (int)(Math.cos(angle) * 10);
                            int y = (int)(Math.sin(angle) * 10);
                            hidManager.getMouseReporter().move(x, y);
                            Thread.sleep(50);
                        }
                        addLogEntry("MOUSE: Movement pattern complete");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                addLogEntry("MOUSE: Not connected or mouse reporter not available");
            }
        });
        
        // Mouse click button
        mouseClickButton.setOnClickListener(v -> {
            if (hidManager.isConnected() && hidManager.getMouseReporter() != null) {
                addLogEntry("MOUSE: Sending left click");
                hidManager.getMouseReporter().click(HidReportConstants.MOUSE_BUTTON_LEFT);
            } else {
                addLogEntry("MOUSE: Not connected or mouse reporter not available");
            }
        });
        
        // Keyboard button
        keyboardButton.setOnClickListener(v -> {
            if (hidManager.isConnected() && hidManager.getKeyboardReporter() != null) {
                addLogEntry("KEYBOARD: Typing text");
                hidManager.getKeyboardReporter().typeString("Hello from Android HID!");
            } else {
                addLogEntry("KEYBOARD: Not connected or keyboard reporter not available");
            }
        });
        
        // Media button
        mediaButton.setOnClickListener(v -> {
            if (hidManager.isConnected() && hidManager.getMediaReporter() != null) {
                addLogEntry("MEDIA: Sending Play/Pause");
                hidManager.getMediaReporter().sendPlayPause();
            } else {
                addLogEntry("MEDIA: Not connected or media reporter not available");
            }
        });
        
        // Initially disable action buttons
        disableActionButtons(true);
    }
    
    private boolean initializeHidManager() {
        addLogEntry("HID: Initializing HID manager");
        
        // Check if device supports HID Device role
        PackageManager pm = getPackageManager();
        if (!pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            addLogEntry("ERROR: Device does not support Bluetooth LE");
            statusText.setText("BLE not supported");
            return false;
        }
        
        // Check if profile is supported
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;
        if (adapter == null) {
            addLogEntry("ERROR: Bluetooth adapter not available");
            statusText.setText("Bluetooth not available");
            return false;
        }
        
        // Additional debug info
        addLogEntry("SYSTEM INFO: Android SDK level = " + Build.VERSION.SDK_INT);
        addLogEntry("SYSTEM INFO: Device = " + Build.MANUFACTURER + " " + Build.MODEL);
        
        try {
            // Get supported profiles
            Method method = adapter.getClass().getMethod("getSupportedProfiles");
            int[] profiles = (int[]) method.invoke(adapter);
            boolean hidSupported = false;
            
            for (int profile : profiles) {
                addLogEntry("PROFILE: " + profile + (profile == BluetoothProfile.HID_DEVICE ? " (HID_DEVICE)" : ""));
                if (profile == BluetoothProfile.HID_DEVICE) {
                    hidSupported = true;
                }
            }
            
            if (!hidSupported) {
                addLogEntry("ERROR: HID Device profile is not supported on this device");
                statusText.setText("HID not supported");
                return false;
            }
            
        } catch (Exception e) {
            addLogEntry("ERROR: Could not check supported profiles: " + e.getMessage());
        }
        
        boolean success = hidManager.initialize();
        if (success) {
            addLogEntry("HID: Initialization successful");
            statusText.setText("Ready to register");
        } else {
            addLogEntry("HID: Initialization failed");
            statusText.setText("Initialization failed");
        }
        return success;
    }
    
    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        
        registerReceiver(bluetoothReceiver, filter);
        addLogEntry("BLUETOOTH: Receiver registered");
    }
    
    private void updateConnectionStatus() {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            if (hidManager.isConnected()) {
                BluetoothDevice device = hidManager.getConnectedDevice();
                if (device != null) {
                    String deviceName = device.getName();
                    if (deviceName == null || deviceName.isEmpty()) {
                        deviceName = device.getAddress();
                    }
                    connectionText.setText("Connected to: " + deviceName);
                    disableActionButtons(false);
                } else {
                    connectionText.setText("Connected (Unknown device)");
                }
            } else {
                connectionText.setText("Not connected");
                disableActionButtons(true);
            }
        });
    }
    
    private void disableAllButtons(boolean disable) {
        registerButton.setEnabled(!disable);
        disableActionButtons(disable);
    }
    
    private void disableActionButtons(boolean disable) {
        mouseMoveButton.setEnabled(!disable);
        mouseClickButton.setEnabled(!disable);
        keyboardButton.setEnabled(!disable);
        mediaButton.setEnabled(!disable);
    }
    
    /**
     * Shows a dialog with debug options.
     */
    private void showDebugOptionsDialog() {
        if (hidDebugger == null) {
            Toast.makeText(this, "Debug manager not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Debug Options");
        
        // Inflate a custom view for the dialog
        View view = getLayoutInflater().inflate(R.layout.dialog_debug_options, null);
        builder.setView(view);
        
        // Set up switches
        Switch verboseLoggingSwitch = view.findViewById(R.id.switch_verbose_logging);
        Switch fileLoggingSwitch = view.findViewById(R.id.switch_file_logging);
        
        // Analysis buttons
        Button analyzeEnvButton = view.findViewById(R.id.btn_analyze_environment);
        Button analyzeDescriptorButton = view.findViewById(R.id.btn_analyze_descriptor);
        Button showStatsButton = view.findViewById(R.id.btn_show_statistics);
        
        // Set up click listeners
        analyzeEnvButton.setOnClickListener(v -> {
            hidDebugger.analyzeEnvironment(this);
            Toast.makeText(this, "Environment analysis added to log", Toast.LENGTH_SHORT).show();
        });
        
        analyzeDescriptorButton.setOnClickListener(v -> {
            hidDebugger.analyzeHidDescriptor();
            Toast.makeText(this, "HID descriptor analysis added to log", Toast.LENGTH_SHORT).show();
        });
        
        showStatsButton.setOnClickListener(v -> {
            addLogEntry("Report Statistics:" + hidDebugger.getReportStatistics());
            Toast.makeText(this, "Statistics added to log", Toast.LENGTH_SHORT).show();
        });
        
        // Handle file logging switch
        fileLoggingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Check for permission
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_WRITE_STORAGE);
                    fileLoggingSwitch.setChecked(false);
                    return;
                }
                
                hidDebugger.enableFileLogging(this, true);
                
                // Show the log file path
                File logPath = new File(getExternalFilesDir(null), "hid_logs");
                Toast.makeText(this, "Logs saved to: " + logPath.getPath(), Toast.LENGTH_LONG).show();
            } else {
                hidDebugger.enableFileLogging(this, false);
            }
        });
        
        // Handle verbose logging switch
        verboseLoggingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hidDebugger.enableVerboseLogging(isChecked);
        });
        
        builder.setPositiveButton("Close", null);
        builder.show();
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_WRITE_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, enable file logging
                hidDebugger.enableFileLogging(this, true);
            } else {
                Toast.makeText(this, "Storage permission denied, file logging disabled", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                addLogEntry("All Bluetooth permissions granted");
                setupHidManager();
            } else {
                addLogEntry("ERROR: Some Bluetooth permissions were denied");
                statusText.setText("Bluetooth permissions required");
                Toast.makeText(this, 
                        "Bluetooth permissions are required for HID functionality", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void addLogEntry(String message) {
        String timestamp = DateFormat.format("HH:mm:ss", new Date()).toString();
        String entry = timestamp + " - " + message + "\n";
        
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(() -> {
            logBuffer.insert(0, entry); // Add newest entries at the top
            
            // Trim log if it gets too long
            if (logBuffer.length() > 10000) {
                logBuffer.setLength(10000);
            }
            
            logTextView.setText(logBuffer.toString());
        });
    }
    
    @Override
    public void onDebugMessage(String message) {
        addLogEntry(message);
    }
    
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
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Do not unregister or close when pausing
        // to allow for background operation
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Unregister Bluetooth receiver
        try {
            unregisterReceiver(bluetoothReceiver);
            addLogEntry("BLUETOOTH: Receiver unregistered");
        } catch (IllegalArgumentException e) {
            // Receiver not registered
        }
        
        // Close HID manager
        if (hidManager != null) {
            hidManager.close();
            addLogEntry("HID: Manager closed");
        }
    }
}
