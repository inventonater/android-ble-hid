package com.inventonater.blehid.app.ui;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.inventonater.blehid.app.R;
import com.inventonater.blehid.core.BleHidManager;
import com.inventonater.blehid.core.HidConstants;

/**
 * Manages the keyboard panel UI and functionality.
 * Handles keyboard input and special keys for HID keyboard emulation.
 */
public class KeyboardPanelManager {
    private static final String TAG = "KeyboardPanelManager";

    public interface Callback {
        void logEvent(String message);
    }

    private final Context context;
    private final View keyboardPanel;
    private final BleHidManager bleHidManager;
    private final Callback callback;

    private EditText textInputField;
    private Button sendTextButton;
    private Button ctrlButton;
    private Button shiftButton;
    private Button altButton;
    private Button metaButton;

    /**
     * Creates a new KeyboardPanelManager.
     *
     * @param context The activity context
     * @param keyboardPanel The keyboard panel view
     * @param bleHidManager The BLE HID manager instance
     * @param callback Callback for logging events
     */
    public KeyboardPanelManager(Context context, View keyboardPanel, BleHidManager bleHidManager, Callback callback) {
        this.context = context;
        this.keyboardPanel = keyboardPanel;
        this.bleHidManager = bleHidManager;
        this.callback = callback;

        initializeViews();
        setupListeners();
    }

    /**
     * Initialize the keyboard panel UI elements.
     */
    private void initializeViews() {
        textInputField = keyboardPanel.findViewById(R.id.textInputField);
        sendTextButton = keyboardPanel.findViewById(R.id.sendTextButton);
        
        ctrlButton = keyboardPanel.findViewById(R.id.key_ctrl);
        shiftButton = keyboardPanel.findViewById(R.id.key_shift);
        altButton = keyboardPanel.findViewById(R.id.key_alt);
        metaButton = keyboardPanel.findViewById(R.id.key_meta);

        // Add more keyboard buttons here as needed
    }

    /**
     * Set up click listeners for the keyboard buttons.
     */
    private void setupListeners() {
        // Set up the send text button
        sendTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendText();
            }
        });

        // Set up Ctrl key
        ctrlButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.logEvent("KEYBOARD: Pressed Ctrl+C (Copy)");
                if (bleHidManager.isConnected()) {
                    // Send Ctrl+C (commonly used for copy)
                    boolean result = bleHidManager.sendKey(HidConstants.Keyboard.KEY_C, HidConstants.Keyboard.MOD_LCTRL);
                    if (!result) {
                        Toast.makeText(context, "Failed to send Ctrl+C", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up Shift key
        shiftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.logEvent("KEYBOARD: Pressed Shift+Tab (Backward Tab)");
                if (bleHidManager.isConnected()) {
                    // Send Shift+Tab (commonly used for backward tabbing)
                    boolean result = bleHidManager.sendKey(HidConstants.Keyboard.KEY_TAB, HidConstants.Keyboard.MOD_LSHIFT);
                    if (!result) {
                        Toast.makeText(context, "Failed to send Shift+Tab", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up Alt key
        altButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.logEvent("KEYBOARD: Pressed Alt+Tab (Window Switcher)");
                if (bleHidManager.isConnected()) {
                    // Send Alt+Tab (commonly used for window switching)
                    boolean result = bleHidManager.sendKey(HidConstants.Keyboard.KEY_TAB, HidConstants.Keyboard.MOD_LALT);
                    if (!result) {
                        Toast.makeText(context, "Failed to send Alt+Tab", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up Meta/Win key
        metaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.logEvent("KEYBOARD: Pressed Meta/Win key");
                if (bleHidManager.isConnected()) {
                    // Send just the Win/Meta key
                    byte[] keys = new byte[1];
                    keys[0] = 0;
                    boolean result = bleHidManager.sendKeys(keys, HidConstants.Keyboard.MOD_LMETA);
                    if (!result) {
                        Toast.makeText(context, "Failed to send Meta key", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // You could set up additional key buttons here
        // setupKeyButton(R.id.key_1, HidConstants.Keyboard.KEY_1, "1");
        // etc.
    }

    /**
     * Helper method to set up a keyboard key button.
     * 
     * @param buttonId The button resource ID
     * @param keyCode The HID key code to send
     * @param keyName The name of the key for logging
     */
    private void setupKeyButton(int buttonId, final byte keyCode, final String keyName) {
        Button button = keyboardPanel.findViewById(buttonId);
        if (button == null) {
            Log.w(TAG, "Key button not found for " + keyName);
            return;
        }
        
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.logEvent("KEYBOARD: Pressed " + keyName);
                if (bleHidManager.isConnected()) {
                    boolean result = bleHidManager.typeKey(keyCode, 0);
                    if (!result) {
                        Toast.makeText(context, "Failed to send " + keyName, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Send the text from the input field as keyboard keystrokes.
     */
    private void sendText() {
        if (!bleHidManager.isConnected()) {
            Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
            callback.logEvent("KEYBOARD: Not connected");
            return;
        }
        
        String text = textInputField.getText().toString();
        if (text.isEmpty()) {
            Toast.makeText(context, "Please enter text to send", Toast.LENGTH_SHORT).show();
            return;
        }
        
        callback.logEvent("KEYBOARD: Sending text: " + text);
        boolean result = bleHidManager.typeText(text);
        
        if (result) {
            callback.logEvent("KEYBOARD: Text sent successfully");
            textInputField.setText(""); // Clear the input field
        } else {
            callback.logEvent("KEYBOARD: Failed to send text");
            Toast.makeText(context, "Failed to send text", Toast.LENGTH_SHORT).show();
        }
    }
}
