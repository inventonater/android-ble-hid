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
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.widget.NestedScrollView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.blehid.app.R;
import com.example.blehid.core.BleHidManager;
import com.example.blehid.core.BlePairingManager;
import com.example.blehid.core.EnhancedBlePairingManager;
import com.example.blehid.core.PairingManagerAdapter;
import com.example.blehid.core.HidMouseService;
import com.example.blehid.core.HidKeyboardService;
import com.example.blehid.core.HidKeyboardConstants;
import com.example.blehid.core.HidConsumerConstants;

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
    
    // Keyboard controls
    private Button keyCtrl;
    private Button keyShift;
    private Button keyAlt;
    private Button keyMeta;
    private Button keyA;
    private Button keyB;
    private Button keyC;
    private Button keySpace;
    private Button keyEsc;
    private Button keyEnter;
    private Button keyDelete;
    private Button keyTab;
    
    // Media controls
    private Button mediaPlayPause;
    private Button mediaPrev;
    private Button mediaNext;
    private Button mediaVolDown;
    private Button mediaMute;
    private Button mediaVolUp;
    
    // Touch processing
    private float lastTouchX;
    private float lastTouchY;
    private boolean isTrackingTouch = false;
    
    // Movement sensitivity - adjusted for better control
    private static final float MOVEMENT_FACTOR = 1.5f;
    
    // StringBuilder for log entries
    private StringBuilder logEntries = new StringBuilder();
    
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
    
    // Action button for pairing operations
    private Button pairingActionButton;
    
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
        
        // Touch handling will be set up in setupMouseControls()
        
        // Initialize keyboard controls
        keyCtrl = findViewById(R.id.keyCtrl);
        keyShift = findViewById(R.id.keyShift);
        keyAlt = findViewById(R.id.keyAlt);
        keyMeta = findViewById(R.id.keyMeta);
        keyA = findViewById(R.id.keyA);
        keyB = findViewById(R.id.keyB);
        keyC = findViewById(R.id.keyC);
        keySpace = findViewById(R.id.keySpace);
        keyEsc = findViewById(R.id.keyEsc);
        keyEnter = findViewById(R.id.keyEnter);
        keyDelete = findViewById(R.id.keyDelete);
        keyTab = findViewById(R.id.keyTab);
        
        // Initialize media controls
        mediaPlayPause = findViewById(R.id.mediaPlayPause);
        mediaPrev = findViewById(R.id.mediaPrev);
        mediaNext = findViewById(R.id.mediaNext);
        mediaVolDown = findViewById(R.id.mediaVolDown);
        mediaMute = findViewById(R.id.mediaMute);
        mediaVolUp = findViewById(R.id.mediaVolUp);
        
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
            
        // Initialize new pairing action button
        pairingActionButton = findViewById(R.id.pairingActionButton);
        
        // Set up pairing callback using standard interface
        bleHidManager.getPairingManager().setPairingCallback(new BlePairingManager.PairingCallback() {
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
                        bleHidManager.getPairingManager().setPairingConfirmation(device, true);
                        
                        // Update pairing action button 
                        updatePairingActionButton();
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
                        
                        // Update pairing action button
                        updatePairingActionButton();
                    }
                });
            }
        });
        
        // Set up pairing action button
        setupPairingActionButton();
        
        // Set up all controls
        setupMouseControls();
        setupKeyboardControls();
        setupMediaControls();
        
        // Initialize BLE HID functionality
        initializeBleHid();
        
        // Show device info
        updateDeviceInfo();
    }
    
    /**
     * Sets up the keyboard control elements.
     */
    private void setupKeyboardControls() {
        // Modifier Keys
        keyCtrl.setOnClickListener(v -> sendKeyboardKey(HidKeyboardConstants.MODIFIER_LEFT_CTRL, (byte)0));
        keyShift.setOnClickListener(v -> sendKeyboardKey(HidKeyboardConstants.MODIFIER_LEFT_SHIFT, (byte)0));
        keyAlt.setOnClickListener(v -> sendKeyboardKey(HidKeyboardConstants.MODIFIER_LEFT_ALT, (byte)0));
        keyMeta.setOnClickListener(v -> sendKeyboardKey(HidKeyboardConstants.MODIFIER_LEFT_GUI, (byte)0));
        
        // Letter Keys
        keyA.setOnClickListener(v -> sendKeyboardKey((byte)0, HidKeyboardConstants.KEY_A));
        keyB.setOnClickListener(v -> sendKeyboardKey((byte)0, HidKeyboardConstants.KEY_B));
        keyC.setOnClickListener(v -> sendKeyboardKey((byte)0, HidKeyboardConstants.KEY_C));
        
        // Function Keys
        keySpace.setOnClickListener(v -> sendKeyboardKey((byte)0, HidKeyboardConstants.KEY_SPACE));
        keyEsc.setOnClickListener(v -> sendKeyboardKey((byte)0, HidKeyboardConstants.KEY_ESCAPE));
        keyEnter.setOnClickListener(v -> sendKeyboardKey((byte)0, HidKeyboardConstants.KEY_RETURN));
        keyDelete.setOnClickListener(v -> sendKeyboardKey((byte)0, HidKeyboardConstants.KEY_DELETE));
        keyTab.setOnClickListener(v -> sendKeyboardKey((byte)0, HidKeyboardConstants.KEY_TAB));
    }
    
    /**
     * Sets up the media control elements.
     */
    private void setupMediaControls() {
        // Media Keys
        mediaPlayPause.setOnClickListener(v -> sendMediaKey(HidConsumerConstants.CONSUMER_PLAY_PAUSE));
        mediaPrev.setOnClickListener(v -> sendMediaKey(HidConsumerConstants.CONSUMER_PREV_TRACK));
        mediaNext.setOnClickListener(v -> sendMediaKey(HidConsumerConstants.CONSUMER_NEXT_TRACK));
        mediaVolDown.setOnClickListener(v -> sendMediaKey(HidConsumerConstants.CONSUMER_VOLUME_DOWN));
        mediaMute.setOnClickListener(v -> sendMediaKey(HidConsumerConstants.CONSUMER_MUTE));
        mediaVolUp.setOnClickListener(v -> sendMediaKey(HidConsumerConstants.CONSUMER_VOLUME_UP));
    }
    
    /**
     * Sends a keyboard key press and release.
     * 
     * @param modifiers The modifier keys (shift, ctrl, etc.)
     * @param key The key code
     */
    private void sendKeyboardKey(byte modifiers, byte key) {
        if (!bleHidManager.isConnected()) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            addLogEntry("KEYBOARD KEY IGNORED: No connected device");
            return;
        }
        
        String keyName = getKeyName(modifiers, key);
        
        // Press the key
        boolean pressResult = bleHidManager.sendKeyWithModifiers(key, modifiers);
        
        // Add a delay for the key press to register
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Release the key (send empty report)
        boolean releaseResult = bleHidManager.releaseAllKeys();
        boolean result = pressResult && releaseResult;
        
        addLogEntry("KEYBOARD KEY: " + keyName + (result ? " pressed" : " FAILED"));
    }
    
    /**
     * Sends a media key press and release.
     * 
     * @param mediaKey The media key code
     */
    private void sendMediaKey(byte mediaKey) {
        if (!bleHidManager.isConnected()) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            addLogEntry("MEDIA KEY IGNORED: No connected device");
            return;
        }
        
        String keyName = getMediaKeyName(mediaKey);
        
        // Press the media key
        boolean pressResult = bleHidManager.sendConsumerControl(mediaKey);
        
        // Add a delay for the key press to register
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Release the key by sending a zero control code
        boolean releaseResult = bleHidManager.sendConsumerControl((byte)0);
        boolean result = pressResult && releaseResult;
        
        addLogEntry("MEDIA KEY: " + keyName + (result ? " pressed" : " FAILED"));
    }
    
    /**
     * Gets a human-readable name for a keyboard key.
     */
    private String getKeyName(byte modifiers, byte key) {
        StringBuilder name = new StringBuilder();
        
        // Add modifiers
        if ((modifiers & HidKeyboardConstants.MODIFIER_LEFT_CTRL) != 0) {
            name.append("CTRL+");
        }
        if ((modifiers & HidKeyboardConstants.MODIFIER_LEFT_SHIFT) != 0) {
            name.append("SHIFT+");
        }
        if ((modifiers & HidKeyboardConstants.MODIFIER_LEFT_ALT) != 0) {
            name.append("ALT+");
        }
        if ((modifiers & HidKeyboardConstants.MODIFIER_LEFT_GUI) != 0) {
            name.append("META+");
        }
        
        // Add the key name
        switch (key) {
            case HidKeyboardConstants.KEY_A:
                name.append("A");
                break;
            case HidKeyboardConstants.KEY_B:
                name.append("B");
                break;
            case HidKeyboardConstants.KEY_C:
                name.append("C");
                break;
            case HidKeyboardConstants.KEY_SPACE:
                name.append("SPACE");
                break;
            case HidKeyboardConstants.KEY_ESCAPE:
                name.append("ESC");
                break;
            case HidKeyboardConstants.KEY_RETURN:
                name.append("ENTER");
                break;
            case HidKeyboardConstants.KEY_DELETE:
                name.append("DELETE");
                break;
            case HidKeyboardConstants.KEY_TAB:
                name.append("TAB");
                break;
            case 0:
                // Just a modifier
                if (name.length() > 0) {
                    name.setLength(name.length() - 1); // Remove the trailing '+'
                } else {
                    name.append("NONE");
                }
                break;
            default:
                name.append("KEY_0x" + Integer.toHexString(key & 0xFF));
        }
        
        return name.toString();
    }
    
    /**
     * Gets a human-readable name for a media key.
     */
    private String getMediaKeyName(byte mediaKey) {
        switch (mediaKey) {
            case HidConsumerConstants.CONSUMER_PLAY_PAUSE:
                return "PLAY/PAUSE";
            case HidConsumerConstants.CONSUMER_NEXT_TRACK:
                return "NEXT";
            case HidConsumerConstants.CONSUMER_PREV_TRACK:
                return "PREV";
            case HidConsumerConstants.CONSUMER_VOLUME_UP:
                return "VOLUME UP";
            case HidConsumerConstants.CONSUMER_VOLUME_DOWN:
                return "VOLUME DOWN";
            case HidConsumerConstants.CONSUMER_MUTE:
                return "MUTE";
            case 0:
                return "NONE";
            default:
                return "MEDIA_0x" + Integer.toHexString(mediaKey & 0xFF);
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
    // Set up touchpad area with enhanced scroll prevention
    touchpadArea.setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // Always prevent ANY parent view from intercepting touch events
            // This ensures the app doesn't scroll while using the mouse movement area
            disableParentScrolling(v);
            
            // For ACTION_DOWN, ensure we capture this touch sequence
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                // Re-apply the disallow intercept to ensure it sticks
                disableParentScrolling(v);
            }
            
            // Process the touch event for mouse movement
            boolean handled = handleTouchpad(event);
            
            // Return true to indicate we've handled this touch event
            // This is important to maintain control of the entire touch sequence
            return true;
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
     * Updates the pairing state text with details from the enhanced manager.
     */
    private void updatePairingState(String state) {
        // Get more detailed pairing information if available
        if (bleHidManager != null && bleHidManager.getPairingManager() != null) {
            // Cast to PairingManagerAdapter to access enhanced methods
            PairingManagerAdapter adapter = (PairingManagerAdapter)bleHidManager.getPairingManager();
            String detailedInfo = adapter.getPairingStatusInfo();
            pairingStateText.setText("Pairing State: " + state + "\n" + detailedInfo);
        } else {
            pairingStateText.setText("Pairing State: " + state);
        }
    }
    
    /**
     * Sets up the pairing action button.
     */
    private void setupPairingActionButton() {
        if (pairingActionButton != null) {
            pairingActionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handlePairingAction();
                }
            });
            
            // Initial state
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
        
        // Cast to PairingManagerAdapter to access enhanced methods
        PairingManagerAdapter adapter = (PairingManagerAdapter)bleHidManager.getPairingManager();
        EnhancedBlePairingManager.PairingState state = adapter.getPairingState();
        
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
        
        // Cast to PairingManagerAdapter to access enhanced methods
        PairingManagerAdapter adapter = (PairingManagerAdapter)bleHidManager.getPairingManager();
        EnhancedBlePairingManager.PairingState state = adapter.getPairingState();
        
        switch (state) {
            case PAIRING_REQUESTED:
            case PAIRING_STARTED:
            case WAITING_FOR_BOND:
                // Cancel the pairing
                boolean cancelled = adapter.cancelPairing();
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
                if (bleHidManager.isConnected()) {
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
                if (bleHidManager.isConnected()) {
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
     * Updates the diagnostic information display.
     */
    private void updateDiagnosticInfo() {
        if (bleHidManager != null) {
            StringBuilder diagnosticInfo = new StringBuilder();
            
            // Get advertiser diagnostic info
            if (bleHidManager.getAdvertiser() != null) {
                diagnosticInfo.append(bleHidManager.getAdvertiser().getDiagnosticInfo());
                diagnosticInfo.append("\n");
                
                // Update device capability info
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                boolean peripheralSupported = bleHidManager.getAdvertiser().getDeviceReportedPeripheralSupport();
                
                if (adapter != null) {
                    deviceNameText.setText("Device Name: " + adapter.getName() +
                                      " (Peripheral Mode: " + (peripheralSupported ? "✅" : "❌") + ")");
                }
            }
            
            // Get enhanced pairing manager info
            if (bleHidManager.getPairingManager() != null) {
                diagnosticInfo.append("\nPAIRING STATUS:\n");
                // Cast to PairingManagerAdapter to access enhanced methods
                PairingManagerAdapter adapter = (PairingManagerAdapter)bleHidManager.getPairingManager();
                diagnosticInfo.append("Current State: ")
                             .append(adapter.getPairingState())
                             .append("\n");
                
                // Get bonded devices info
                String bondedDevicesInfo = adapter.getPairingStatusInfo();
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
     * Helper method to disable scrolling in all parent scroll views.
     * This prevents the app from scrolling when using the touchpad area.
     * 
     * @param view The view whose parents should have scrolling disabled
     */
    private void disableParentScrolling(View view) {
        if (view == null) return;
        
        // Request the immediate parent to not intercept touch events
        ViewParent parent = view.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
            
            // Recursively apply to all parent scroll views
            if (parent instanceof View) {
                ViewParent grandParent = parent.getParent();
                while (grandParent != null) {
                    if (grandParent instanceof ScrollView || 
                        grandParent instanceof NestedScrollView) {
                        grandParent.requestDisallowInterceptTouchEvent(true);
                    }
                    grandParent = grandParent.getParent();
                }
            }
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
