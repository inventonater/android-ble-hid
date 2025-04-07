package com.inventonater.blehid.core;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Central manager class for BLE HID functionality.
 * Coordinates between the advertiser, GATT server, and pairing components.
 * Also provides local device control via MediaControlService and AccessibilityControlService.
 */
public class BleHidManager {
    private static final String TAG = "BleHidManager";

    // Mode for input routing
    public static final int MODE_REMOTE = 0;  // Send inputs over BLE HID to remote device
    public static final int MODE_LOCAL = 1;   // Control the local device
    
    private final Context context;
    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;
    private final BleAdvertiser advertiser;
    private final BleGattServerManager gattServerManager;
    private final BlePairingManager pairingManager;
    // Using media service for HID functionality
    private final HidMediaService hidMediaService;
    
    // New services for local device control
    private final MediaControlService mediaControlService;
    private final AccessibilityControlService accessibilityControlService;

    private boolean isInitialized = false;
    private BluetoothDevice connectedDevice = null;
    private int currentMode = MODE_REMOTE;  // Default to remote mode

    /**
     * Creates a new BLE HID Manager
     * 
     * @param context Application context
     */
    public BleHidManager(Context context) {
        this.context = context.getApplicationContext(); // Use application context to prevent leaks
        
        // Get Bluetooth adapter
        bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Log.e(TAG, "Bluetooth manager not found");
            bluetoothAdapter = null;
        } else {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        
        // Initialize components
        advertiser = new BleAdvertiser(this);
        gattServerManager = new BleGattServerManager(this);
        pairingManager = new BlePairingManager(this);
        hidMediaService = new HidMediaService(this);
        
        // Initialize local control services
        mediaControlService = new MediaControlService(context);
        accessibilityControlService = new AccessibilityControlService(context);
    }

    /**
     * Initializes the BLE HID functionality.
     * 
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.w(TAG, "Already initialized");
            return true;
        }
        
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not supported");
            return false;
        }
        
        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth is not enabled");
            return false;
        }
        
        if (!isBlePeripheralSupported()) {
            Log.e(TAG, "BLE Peripheral mode not supported");
            return false;
        }
        
        // Initialize components
        boolean gattInitialized = gattServerManager.initialize();
        if (!gattInitialized) {
            Log.e(TAG, "Failed to initialize GATT server");
            return false;
        }
        
        // Initialize HID service
        boolean hidInitialized = hidMediaService.initialize();
        if (!hidInitialized) {
            Log.e(TAG, "Failed to initialize HID service");
            gattServerManager.close();
            return false;
        }
        
        // Initialize local control services
        mediaControlService.initialize();
        accessibilityControlService.initialize();
        
        isInitialized = true;
        Log.i(TAG, "BLE HID Manager initialized successfully");
        return true;
    }

    /**
     * Starts advertising the BLE HID device.
     * 
     * @return true if advertising started successfully, false otherwise
     */
    public boolean startAdvertising() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        return advertiser.startAdvertising();
    }

    /**
     * Stops advertising the BLE HID device.
     */
    public void stopAdvertising() {
        if (isInitialized) {
            advertiser.stopAdvertising();
        }
    }

    /**
     * Checks if the device supports BLE peripheral mode.
     * 
     * @return true if peripheral mode is supported, false otherwise
     */
    public boolean isBlePeripheralSupported() {
        if (bluetoothAdapter == null) {
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return bluetoothAdapter.isMultipleAdvertisementSupported();
        }
        
        return false;
    }

    /**
     * Cleans up resources.
     */
    public void close() {
        stopAdvertising();
        
        if (gattServerManager != null) {
            gattServerManager.close();
        }
        
        if (mediaControlService != null) {
            mediaControlService.close();
        }
        
        if (accessibilityControlService != null) {
            accessibilityControlService.close();
        }
        
        connectedDevice = null;
        isInitialized = false;
        
        Log.i(TAG, "BLE HID Manager closed");
    }

    /**
     * Sets the current input mode.
     * 
     * @param mode MODE_REMOTE or MODE_LOCAL
     */
    public void setMode(int mode) {
        if (mode != MODE_REMOTE && mode != MODE_LOCAL) {
            Log.e(TAG, "Invalid mode: " + mode);
            return;
        }
        
        currentMode = mode;
        Log.i(TAG, "Mode set to: " + (mode == MODE_REMOTE ? "REMOTE" : "LOCAL"));
    }
    
    /**
     * Gets the current input mode.
     * 
     * @return MODE_REMOTE or MODE_LOCAL
     */
    public int getMode() {
        return currentMode;
    }
    
    /**
     * Checks if the MediaNotificationListener service is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isMediaNotificationListenerEnabled() {
        return mediaControlService.hasNotificationListenerPermission();
    }
    
    /**
     * Checks if the accessibility service is enabled.
     * 
     * @return true if enabled, false otherwise
     */
    public boolean isAccessibilityServiceEnabled() {
        return accessibilityControlService.isAccessibilityServiceEnabled();
    }
    
    /**
     * Opens the notification listener settings screen.
     */
    public void openNotificationListenerSettings() {
        mediaControlService.openNotificationListenerSettings();
    }
    
    /**
     * Opens the accessibility settings screen.
     */
    public void openAccessibilitySettings() {
        accessibilityControlService.openAccessibilitySettings();
    }
    
    /**
     * Simple conversion of HID key codes to Android KeyEvent codes.
     * This is a very basic mapping and won't work for all keys.
     * 
     * @param hidKeyCode The HID key code
     * @return The corresponding Android KeyEvent code
     */
    private int convertHidKeyToAndroidKey(byte hidKeyCode) {
        switch (hidKeyCode) {
            // Basic alphanumeric keys
            case HidConstants.Keyboard.KEY_A: return KeyEvent.KEYCODE_A;
            case HidConstants.Keyboard.KEY_B: return KeyEvent.KEYCODE_B;
            case HidConstants.Keyboard.KEY_C: return KeyEvent.KEYCODE_C;
            case HidConstants.Keyboard.KEY_D: return KeyEvent.KEYCODE_D;
            case HidConstants.Keyboard.KEY_E: return KeyEvent.KEYCODE_E;
            case HidConstants.Keyboard.KEY_F: return KeyEvent.KEYCODE_F;
            case HidConstants.Keyboard.KEY_G: return KeyEvent.KEYCODE_G;
            case HidConstants.Keyboard.KEY_H: return KeyEvent.KEYCODE_H;
            case HidConstants.Keyboard.KEY_I: return KeyEvent.KEYCODE_I;
            case HidConstants.Keyboard.KEY_J: return KeyEvent.KEYCODE_J;
            case HidConstants.Keyboard.KEY_K: return KeyEvent.KEYCODE_K;
            case HidConstants.Keyboard.KEY_L: return KeyEvent.KEYCODE_L;
            case HidConstants.Keyboard.KEY_M: return KeyEvent.KEYCODE_M;
            case HidConstants.Keyboard.KEY_N: return KeyEvent.KEYCODE_N;
            case HidConstants.Keyboard.KEY_O: return KeyEvent.KEYCODE_O;
            case HidConstants.Keyboard.KEY_P: return KeyEvent.KEYCODE_P;
            case HidConstants.Keyboard.KEY_Q: return KeyEvent.KEYCODE_Q;
            case HidConstants.Keyboard.KEY_R: return KeyEvent.KEYCODE_R;
            case HidConstants.Keyboard.KEY_S: return KeyEvent.KEYCODE_S;
            case HidConstants.Keyboard.KEY_T: return KeyEvent.KEYCODE_T;
            case HidConstants.Keyboard.KEY_U: return KeyEvent.KEYCODE_U;
            case HidConstants.Keyboard.KEY_V: return KeyEvent.KEYCODE_V;
            case HidConstants.Keyboard.KEY_W: return KeyEvent.KEYCODE_W;
            case HidConstants.Keyboard.KEY_X: return KeyEvent.KEYCODE_X;
            case HidConstants.Keyboard.KEY_Y: return KeyEvent.KEYCODE_Y;
            case HidConstants.Keyboard.KEY_Z: return KeyEvent.KEYCODE_Z;
            
            // Numbers
            case HidConstants.Keyboard.KEY_1: return KeyEvent.KEYCODE_1;
            case HidConstants.Keyboard.KEY_2: return KeyEvent.KEYCODE_2;
            case HidConstants.Keyboard.KEY_3: return KeyEvent.KEYCODE_3;
            case HidConstants.Keyboard.KEY_4: return KeyEvent.KEYCODE_4;
            case HidConstants.Keyboard.KEY_5: return KeyEvent.KEYCODE_5;
            case HidConstants.Keyboard.KEY_6: return KeyEvent.KEYCODE_6;
            case HidConstants.Keyboard.KEY_7: return KeyEvent.KEYCODE_7;
            case HidConstants.Keyboard.KEY_8: return KeyEvent.KEYCODE_8;
            case HidConstants.Keyboard.KEY_9: return KeyEvent.KEYCODE_9;
            case HidConstants.Keyboard.KEY_0: return KeyEvent.KEYCODE_0;
            
            // Function keys
            case HidConstants.Keyboard.KEY_F1: return KeyEvent.KEYCODE_F1;
            case HidConstants.Keyboard.KEY_F2: return KeyEvent.KEYCODE_F2;
            case HidConstants.Keyboard.KEY_F3: return KeyEvent.KEYCODE_F3;
            case HidConstants.Keyboard.KEY_F4: return KeyEvent.KEYCODE_F4;
            case HidConstants.Keyboard.KEY_F5: return KeyEvent.KEYCODE_F5;
            case HidConstants.Keyboard.KEY_F6: return KeyEvent.KEYCODE_F6;
            case HidConstants.Keyboard.KEY_F7: return KeyEvent.KEYCODE_F7;
            case HidConstants.Keyboard.KEY_F8: return KeyEvent.KEYCODE_F8;
            case HidConstants.Keyboard.KEY_F9: return KeyEvent.KEYCODE_F9;
            case HidConstants.Keyboard.KEY_F10: return KeyEvent.KEYCODE_F10;
            case HidConstants.Keyboard.KEY_F11: return KeyEvent.KEYCODE_F11;
            case HidConstants.Keyboard.KEY_F12: return KeyEvent.KEYCODE_F12;
            
            // Special keys
            case HidConstants.Keyboard.KEY_ENTER: return KeyEvent.KEYCODE_ENTER;
            case HidConstants.Keyboard.KEY_ESCAPE: return KeyEvent.KEYCODE_ESCAPE;
            case HidConstants.Keyboard.KEY_BACKSPACE: return KeyEvent.KEYCODE_DEL;
            case HidConstants.Keyboard.KEY_TAB: return KeyEvent.KEYCODE_TAB;
            case HidConstants.Keyboard.KEY_SPACE: return KeyEvent.KEYCODE_SPACE;
            
            // Direction keys
            case HidConstants.Keyboard.KEY_UP: return KeyEvent.KEYCODE_DPAD_UP;
            case HidConstants.Keyboard.KEY_DOWN: return KeyEvent.KEYCODE_DPAD_DOWN;
            case HidConstants.Keyboard.KEY_LEFT: return KeyEvent.KEYCODE_DPAD_LEFT;
            case HidConstants.Keyboard.KEY_RIGHT: return KeyEvent.KEYCODE_DPAD_RIGHT;
            
            // Default to unknown key
            default: return KeyEvent.KEYCODE_UNKNOWN;
        }
    }
    
    // Getters for internal components

    public Context getContext() {
        return context;
    }

    public BluetoothManager getBluetoothManager() {
        return bluetoothManager;
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BleGattServerManager getGattServerManager() {
        return gattServerManager;
    }

    public HidMediaService getHidMediaService() {
        return hidMediaService;
    }
    
    public MediaControlService getMediaControlService() {
        return mediaControlService;
    }
    
    public AccessibilityControlService getAccessibilityControlService() {
        return accessibilityControlService;
    }
    
    // ==================== Media Control Methods ====================
    
    /**
     * Sends a play/pause control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean playPause() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.playPause();
        } else {
            return mediaControlService.playPause();
        }
    }
    
    /**
     * Sends a next track control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean nextTrack() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.nextTrack();
        } else {
            return mediaControlService.nextTrack();
        }
    }
    
    /**
     * Sends a previous track control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean previousTrack() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.previousTrack();
        } else {
            return mediaControlService.previousTrack();
        }
    }
    
    /**
     * Sends a volume up control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeUp() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.volumeUp();
        } else {
            return mediaControlService.volumeUp();
        }
    }
    
    /**
     * Sends a volume down control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeDown() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.volumeDown();
        } else {
            return mediaControlService.volumeDown();
        }
    }
    
    /**
     * Sends a mute control.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean mute() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.mute();
        } else {
            return mediaControlService.mute();
        }
    }

    // ==================== Mouse Control Methods ====================
    
    /**
     * Moves the mouse pointer by the specified amount.
     *
     * @param x The X movement amount (-127 to 127)
     * @param y The Y movement amount (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean moveMouse(int x, int y) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.movePointer(x, y);
        } else {
            // No exact equivalent for relative mouse movement in Accessibility API
            // The best we can do is simulate a swipe gesture
            float moveScale = 0.01f;  // Scale factor to convert from relative movement to screen fraction
            return accessibilityControlService.swipeScreen(0.5f, 0.5f, 
                    0.5f + (x * moveScale), 0.5f + (y * moveScale), 100);
        }
    }
    
    /**
     * Presses a mouse button.
     *
     * @param button The button to press (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean pressMouseButton(int button) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.pressButton(button);
        } else {
            // Local mode - just simulate a tap for any button press
            return accessibilityControlService.click();
        }
    }
    
    /**
     * Releases all mouse buttons.
     *
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean releaseMouseButtons() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.releaseButtons();
        } else {
            // No action needed for local mode - taps automatically release
            return true;
        }
    }
    
    /**
     * Performs a click with the specified button.
     *
     * @param button The button to click (BUTTON_LEFT, BUTTON_RIGHT, BUTTON_MIDDLE)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean clickMouseButton(int button) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.click(button);
        } else {
            return accessibilityControlService.click();
        }
    }
    
    /**
     * Scrolls the mouse wheel.
     *
     * @param amount The scroll amount (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean scrollMouseWheel(int amount) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            // Vertical scrolling is often implemented as mouse movement along the Y axis
            return hidMediaService.movePointer(0, amount);
        } else {
            // Determine scroll direction based on amount
            String direction = amount < 0 ? "up" : "down";
            return accessibilityControlService.scroll(direction);
        }
    }
    
    /**
     * Send a combined media and mouse report.
     * 
     * @param mediaButtons Media button flags (BUTTON_PLAY_PAUSE, etc.)
     * @param mouseButtons Mouse button flags (BUTTON_LEFT, etc.)
     * @param x X-axis movement (-127 to 127)
     * @param y Y-axis movement (-127 to 127)
     * @return true if successful, false otherwise
     */
    public boolean sendCombinedReport(int mediaButtons, int mouseButtons, int x, int y) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.sendCombinedReport(mediaButtons, mouseButtons, x, y);
        } else {
            // For local mode, handle media and mouse separately
            boolean mediaResult = true;
            boolean mouseResult = true;
            
            if (mediaButtons != 0) {
                // Handle media buttons - crude implementation
                if ((mediaButtons & HidConstants.Media.BUTTON_PLAY_PAUSE) != 0) {
                    mediaResult &= mediaControlService.playPause();
                }
                if ((mediaButtons & HidConstants.Media.BUTTON_NEXT_TRACK) != 0) {
                    mediaResult &= mediaControlService.nextTrack();
                }
                if ((mediaButtons & HidConstants.Media.BUTTON_PREVIOUS_TRACK) != 0) {
                    mediaResult &= mediaControlService.previousTrack();
                }
                if ((mediaButtons & HidConstants.Media.BUTTON_VOLUME_UP) != 0) {
                    mediaResult &= mediaControlService.volumeUp();
                }
                if ((mediaButtons & HidConstants.Media.BUTTON_VOLUME_DOWN) != 0) {
                    mediaResult &= mediaControlService.volumeDown();
                }
                if ((mediaButtons & HidConstants.Media.BUTTON_MUTE) != 0) {
                    mediaResult &= mediaControlService.mute();
                }
            }
            
            // Handle mouse part
            if (mouseButtons != 0 || x != 0 || y != 0) {
                if (x != 0 || y != 0) {
                    mouseResult &= moveMouse(x, y);
                }
                
                if (mouseButtons != 0) {
                    if ((mouseButtons & HidConstants.Mouse.BUTTON_LEFT) != 0) {
                        mouseResult &= clickMouseButton(HidConstants.Mouse.BUTTON_LEFT);
                    } else if ((mouseButtons & HidConstants.Mouse.BUTTON_RIGHT) != 0) {
                        mouseResult &= clickMouseButton(HidConstants.Mouse.BUTTON_RIGHT);
                    } else if ((mouseButtons & HidConstants.Mouse.BUTTON_MIDDLE) != 0) {
                        mouseResult &= clickMouseButton(HidConstants.Mouse.BUTTON_MIDDLE);
                    }
                }
            }
            
            return mediaResult && mouseResult;
        }
    }
    
    // ==================== Keyboard Control Methods ====================
    
    /**
     * Sends a single key press.
     * 
     * @param keyCode The key code to send
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if successful, false otherwise
     */
    public boolean sendKey(byte keyCode, int modifiers) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.sendKey(keyCode, modifiers);
        } else {
            // Convert HID key code to Android KeyEvent code (simplistic mapping)
            int androidKeyCode = convertHidKeyToAndroidKey(keyCode);
            return accessibilityControlService.sendKeyEvent(androidKeyCode);
        }
    }
    
    /**
     * Releases all currently pressed keys.
     * 
     * @return true if successful, false otherwise
     */
    public boolean releaseAllKeys() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.releaseAllKeys();
        } else {
            // No direct equivalent in accessibility service
            return true;
        }
    }
    
    /**
     * Sends multiple keys at once.
     * 
     * @param keyCodes Array of up to 6 key codes to send simultaneously
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if successful, false otherwise
     */
    public boolean sendKeys(byte[] keyCodes, int modifiers) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.sendKeys(keyCodes, modifiers);
        } else {
            // Send each key sequentially since we can't press multiple keys at once with accessibility
            boolean success = true;
            for (byte keyCode : keyCodes) {
                if (keyCode != 0) {
                    int androidKeyCode = convertHidKeyToAndroidKey(keyCode);
                    success &= accessibilityControlService.sendKeyEvent(androidKeyCode);
                }
            }
            return success;
        }
    }
    
    /**
     * Types a single key (press and release).
     * 
     * @param keyCode The key code to type
     * @param modifiers Optional modifier keys (shift, ctrl, alt, etc.)
     * @return true if successful, false otherwise
     */
    public boolean typeKey(byte keyCode, int modifiers) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.typeKey(keyCode, modifiers);
        } else {
            int androidKeyCode = convertHidKeyToAndroidKey(keyCode);
            return accessibilityControlService.sendKeyEvent(androidKeyCode);
        }
    }
    
    /**
     * Types a string of text character by character.
     * 
     * @param text The text to type
     * @return true if successful, false otherwise
     */
    public boolean typeText(String text) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.typeText(text);
        } else {
            // For local mode, we'll type one character at a time
            boolean success = true;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                int keyCode = KeyEvent.KEYCODE_UNKNOWN;
                
                // Convert character to KeyEvent code
                if (c >= 'a' && c <= 'z') {
                    keyCode = KeyEvent.KEYCODE_A + (c - 'a');
                } else if (c >= 'A' && c <= 'Z') {
                    keyCode = KeyEvent.KEYCODE_A + (c - 'A');
                } else if (c >= '0' && c <= '9') {
                    keyCode = KeyEvent.KEYCODE_0 + (c - '0');
                } else if (c == ' ') {
                    keyCode = KeyEvent.KEYCODE_SPACE;
                } else if (c == '\n') {
                    keyCode = KeyEvent.KEYCODE_ENTER;
                }
                
                if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
                    success &= accessibilityControlService.sendKeyEvent(keyCode);
                    
                    // Small delay between key presses
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        // Ignore
                    }
                }
            }
            return success;
        }
    }
    
    /**
     * Sends a directional key press (up, down, left, right).
     * 
     * @param direction The direction key code (KEY_UP, KEY_DOWN, KEY_LEFT, KEY_RIGHT)
     * @return true if successful, false otherwise
     */
    public boolean sendDirectionalKey(byte direction) {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }
        
        if (currentMode == MODE_REMOTE) {
            if (connectedDevice == null) {
                Log.e(TAG, "No device connected for remote mode");
                return false;
            }
            return hidMediaService.typeKey(direction, 0);
        } else {
            int androidKeyCode;
            
            switch (direction) {
                case HidConstants.Keyboard.KEY_UP:
                    androidKeyCode = KeyEvent.KEYCODE_DPAD_UP;
                    break;
                case HidConstants.Keyboard.KEY_DOWN:
                    androidKeyCode = KeyEvent.KEYCODE_DPAD_DOWN;
                    break;
                case HidConstants.Keyboard.KEY_LEFT:
                    androidKeyCode = KeyEvent.KEYCODE_DPAD_LEFT;
                    break;
                case HidConstants.Keyboard.KEY_RIGHT:
                    androidKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
                    break;
                default:
                    Log.e(TAG, "Invalid direction key: " + direction);
                    return false;
            }
            
            return accessibilityControlService.sendDirectionalKey(androidKeyCode);
        }
    }
    
    // ==================== Connection Management ====================

    /**
     * Called when a device connects.
     * 
     * @param device The connected device
     */
    void onDeviceConnected(BluetoothDevice device) {
        connectedDevice = device;
        Log.i(TAG, "Device connected: " + device.getAddress());
        
        // Stop advertising once connected
        stopAdvertising();
    }

    /**
     * Called when a device disconnects.
     * 
     * @param device The disconnected device
     */
    void onDeviceDisconnected(BluetoothDevice device) {
        Log.i(TAG, "Device disconnected: " + device.getAddress());
        connectedDevice = null;
        
        // Restart advertising after disconnect
        startAdvertising();
    }

    /**
     * Checks if a device is connected.
     * 
     * @return true if a device is connected, false otherwise
     */
    public boolean isConnected() {
        return connectedDevice != null;
    }

    /**
     * Gets the connected device.
     * 
     * @return The connected BluetoothDevice, or null if not connected
     */
    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }
    
    /**
     * Returns the BleAdvertiser instance.
     * 
     * @return The BleAdvertiser instance
     */
    public BleAdvertiser getAdvertiser() {
        return advertiser;
    }
    
    /**
     * Checks if the device is currently advertising.
     * 
     * @return true if advertising, false otherwise
     */
    public boolean isAdvertising() {
        return advertiser != null && advertiser.isAdvertising();
    }
    
    /**
     * Returns the BlePairingManager instance.
     * 
     * @return The BlePairingManager instance
     */
    public BlePairingManager getBlePairingManager() {
        return pairingManager;
    }
}
