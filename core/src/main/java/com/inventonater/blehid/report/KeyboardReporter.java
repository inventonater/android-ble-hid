package com.inventonater.blehid.report;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;

import com.inventonater.blehid.HidManager;
import com.inventonater.blehid.HidReportConstants;

import java.util.Arrays;

/**
 * Handles keyboard HID reports.
 * This class provides methods for sending keyboard reports to the host device,
 * including key presses, releases, and modifier keys.
 */
public class KeyboardReporter {
    private final HidManager manager;
    private byte currentModifiers = 0;
    private final byte[] currentKeys = new byte[6]; // 6 key rollover
    private final byte[] report = new byte[HidReportConstants.KEYBOARD_REPORT_SIZE];
    
    /**
     * Creates a new keyboard reporter.
     *
     * @param manager The HID manager
     */
    public KeyboardReporter(HidManager manager) {
        this.manager = manager;
    }
    
    /**
     * Gets an empty keyboard report.
     *
     * @return An empty keyboard report
     */
    public byte[] getEmptyReport() {
        // Clear the report
        for (int i = 0; i < report.length; i++) {
            report[i] = 0;
        }
        return report.clone();
    }
    
    /**
     * Sends a keyboard report with a single key press.
     *
     * @param key The key code (see HidReportConstants.KEY_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKey(byte key) {
        return sendKey(key, HidReportConstants.KEYBOARD_MODIFIER_NONE);
    }
    
    /**
     * Sends a keyboard report with a single key press and modifiers.
     *
     * @param key The key code (see HidReportConstants.KEY_*)
     * @param modifiers The modifier keys (see HidReportConstants.KEYBOARD_MODIFIER_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKey(byte key, byte modifiers) {
        // Clear the current keys
        Arrays.fill(currentKeys, (byte) 0);
        
        // Set the key and modifiers
        if (key != 0) {
            currentKeys[0] = key;
        }
        currentModifiers = modifiers;
        
        // Prepare and send the report
        return prepareAndSendReport();
    }
    
    /**
     * Sends a keyboard report with multiple key presses.
     *
     * @param keys The key codes (see HidReportConstants.KEY_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeys(byte[] keys) {
        return sendKeys(keys, HidReportConstants.KEYBOARD_MODIFIER_NONE);
    }
    
    /**
     * Sends a keyboard report with multiple key presses and modifiers.
     *
     * @param keys The key codes (see HidReportConstants.KEY_*)
     * @param modifiers The modifier keys (see HidReportConstants.KEYBOARD_MODIFIER_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean sendKeys(byte[] keys, byte modifiers) {
        // Clear the current keys
        Arrays.fill(currentKeys, (byte) 0);
        
        // Set the keys
        if (keys != null) {
            for (int i = 0; i < Math.min(keys.length, currentKeys.length); i++) {
                currentKeys[i] = keys[i];
            }
        }
        
        // Set the modifiers
        currentModifiers = modifiers;
        
        // Prepare and send the report
        return prepareAndSendReport();
    }
    
    /**
     * Presses a single key.
     *
     * @param key The key code (see HidReportConstants.KEY_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean pressKey(byte key) {
        return pressKey(key, currentModifiers);
    }
    
    /**
     * Presses a single key with modifiers.
     *
     * @param key The key code (see HidReportConstants.KEY_*)
     * @param modifiers The modifier keys (see HidReportConstants.KEYBOARD_MODIFIER_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean pressKey(byte key, byte modifiers) {
        // First check if the key is already pressed
        for (int i = 0; i < currentKeys.length; i++) {
            if (currentKeys[i] == key) {
                // Key already pressed, just update modifiers if needed
                if (currentModifiers != modifiers) {
                    currentModifiers = modifiers;
                    return prepareAndSendReport();
                }
                return true; // Key already pressed with same modifiers
            }
        }
        
        // Find the first empty slot
        for (int i = 0; i < currentKeys.length; i++) {
            if (currentKeys[i] == 0) {
                currentKeys[i] = key;
                currentModifiers = modifiers;
                return prepareAndSendReport();
            }
        }
        
        // No empty slots, report is full (6 keys already pressed)
        manager.logError("Cannot press key: Keyboard report full (6 keys already pressed)");
        return false;
    }
    
    /**
     * Releases a specific key.
     *
     * @param key The key code to release (see HidReportConstants.KEY_*)
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseKey(byte key) {
        boolean found = false;
        
        // Find the key and remove it
        for (int i = 0; i < currentKeys.length; i++) {
            if (currentKeys[i] == key) {
                currentKeys[i] = 0;
                found = true;
            }
        }
        
        if (!found) {
            // Key wasn't pressed
            return true;
        }
        
        // Compact the keys array (remove any gaps)
        byte[] tempKeys = new byte[currentKeys.length];
        int index = 0;
        
        for (int i = 0; i < currentKeys.length; i++) {
            if (currentKeys[i] != 0) {
                tempKeys[index++] = currentKeys[i];
            }
        }
        
        System.arraycopy(tempKeys, 0, currentKeys, 0, currentKeys.length);
        
        // Prepare and send the report
        return prepareAndSendReport();
    }
    
    /**
     * Releases all keys and modifiers.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    public boolean releaseAll() {
        // Clear the current keys and modifiers
        Arrays.fill(currentKeys, (byte) 0);
        currentModifiers = 0;
        
        // Prepare and send the report
        return prepareAndSendReport();
    }
    
    /**
     * Types a string of characters.
     * This method simulates typing a string by sending the appropriate key presses
     * and releases for each character.
     *
     * @param text The text to type
     * @return true if all reports were sent successfully, false otherwise
     */
    public boolean typeString(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }
        
        boolean success = true;
        
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            byte[] keyInfo = charToKeyCode(c);
            
            if (keyInfo != null) {
                // Press the key with any necessary modifiers
                success &= sendKey(keyInfo[0], keyInfo[1]);
                
                try {
                    // Small delay between key presses
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Release all keys
                success &= releaseAll();
                
                try {
                    // Small delay between key releases
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        return success;
    }
    
    /**
     * Prepares and sends the keyboard report.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    private boolean prepareAndSendReport() {
        // Prepare the report
        report[0] = currentModifiers;
        report[1] = 0; // Reserved byte
        
        // Copy the current keys
        System.arraycopy(currentKeys, 0, report, 2, Math.min(currentKeys.length, 6));
        
        // Log the report
        StringBuilder keysStr = new StringBuilder();
        for (byte key : currentKeys) {
            if (key != 0) {
                keysStr.append("0x").append(String.format("%02X", key)).append(" ");
            }
        }
        manager.logVerbose("Keyboard report: modifiers=0x" + 
                String.format("%02X", currentModifiers) + 
                ", keys=[" + keysStr.toString().trim() + "]", report);
        
        // Send the report
        return sendReport();
    }
    
    /**
     * Sends the current report to the host device.
     *
     * @return true if the report was sent successfully, false otherwise
     */
    private boolean sendReport() {
        if (!manager.isConnected()) {
            manager.logError("Cannot send keyboard report: Not connected");
            return false;
        }
        
        BluetoothDevice device = manager.getConnectedDevice();
        BluetoothHidDevice hidDevice = manager.getHidDevice();
        
        if (hidDevice == null) {
            manager.logError("Cannot send keyboard report: HID device not available");
            return false;
        }
        
        try {
            boolean success = hidDevice.sendReport(
                    device, 
                    HidReportConstants.REPORT_ID_KEYBOARD, 
                    report);
            
            if (!success) {
                manager.logError("Failed to send keyboard report");
            }
            
            return success;
        } catch (Exception e) {
            manager.logError("Exception sending keyboard report", e);
            return false;
        }
    }
    
    /**
     * Converts a character to a key code and modifiers.
     * This method converts a character to the corresponding key code and modifier keys
     * required to type that character on a standard US keyboard.
     *
     * @param c The character to convert
     * @return An array containing [keyCode, modifiers], or null if the character is not supported
     */
    private byte[] charToKeyCode(char c) {
        // This is a simple mapping for a US keyboard layout
        // A more complete implementation would handle more characters and different keyboard layouts
        byte keyCode = 0;
        byte modifiers = 0;
        
        if (c >= 'a' && c <= 'z') {
            // Lowercase letters
            keyCode = (byte) (HidReportConstants.KEY_A + (c - 'a'));
        } else if (c >= 'A' && c <= 'Z') {
            // Uppercase letters (require shift)
            keyCode = (byte) (HidReportConstants.KEY_A + (c - 'A'));
            modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
        } else if (c >= '1' && c <= '9') {
            // Digits 1-9
            keyCode = (byte) (HidReportConstants.KEY_1 + (c - '1'));
        } else if (c == '0') {
            // Digit 0
            keyCode = HidReportConstants.KEY_0;
        } else {
            // Special characters
            switch (c) {
                case ' ':
                    keyCode = HidReportConstants.KEY_SPACE;
                    break;
                case '\n':
                    keyCode = HidReportConstants.KEY_ENTER;
                    break;
                case '\t':
                    keyCode = HidReportConstants.KEY_TAB;
                    break;
                case '-':
                    keyCode = HidReportConstants.KEY_MINUS;
                    break;
                case '=':
                    keyCode = HidReportConstants.KEY_EQUAL;
                    break;
                case '[':
                    keyCode = HidReportConstants.KEY_LEFT_BRACKET;
                    break;
                case ']':
                    keyCode = HidReportConstants.KEY_RIGHT_BRACKET;
                    break;
                case '\\':
                    keyCode = HidReportConstants.KEY_BACKSLASH;
                    break;
                case ';':
                    keyCode = HidReportConstants.KEY_SEMICOLON;
                    break;
                case '\'':
                    keyCode = HidReportConstants.KEY_APOSTROPHE;
                    break;
                case '`':
                    keyCode = HidReportConstants.KEY_GRAVE;
                    break;
                case ',':
                    keyCode = HidReportConstants.KEY_COMMA;
                    break;
                case '.':
                    keyCode = HidReportConstants.KEY_PERIOD;
                    break;
                case '/':
                    keyCode = HidReportConstants.KEY_SLASH;
                    break;
                    
                // Shifted special characters
                case '!':
                    keyCode = HidReportConstants.KEY_1;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '@':
                    keyCode = HidReportConstants.KEY_2;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '#':
                    keyCode = HidReportConstants.KEY_3;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '$':
                    keyCode = HidReportConstants.KEY_4;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '%':
                    keyCode = HidReportConstants.KEY_5;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '^':
                    keyCode = HidReportConstants.KEY_6;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '&':
                    keyCode = HidReportConstants.KEY_7;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '*':
                    keyCode = HidReportConstants.KEY_8;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '(':
                    keyCode = HidReportConstants.KEY_9;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case ')':
                    keyCode = HidReportConstants.KEY_0;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '_':
                    keyCode = HidReportConstants.KEY_MINUS;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '+':
                    keyCode = HidReportConstants.KEY_EQUAL;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '{':
                    keyCode = HidReportConstants.KEY_LEFT_BRACKET;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '}':
                    keyCode = HidReportConstants.KEY_RIGHT_BRACKET;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '|':
                    keyCode = HidReportConstants.KEY_BACKSLASH;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case ':':
                    keyCode = HidReportConstants.KEY_SEMICOLON;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '"':
                    keyCode = HidReportConstants.KEY_APOSTROPHE;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '~':
                    keyCode = HidReportConstants.KEY_GRAVE;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '<':
                    keyCode = HidReportConstants.KEY_COMMA;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '>':
                    keyCode = HidReportConstants.KEY_PERIOD;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                case '?':
                    keyCode = HidReportConstants.KEY_SLASH;
                    modifiers = HidReportConstants.KEYBOARD_MODIFIER_LEFT_SHIFT;
                    break;
                default:
                    // Character not supported
                    manager.logError("Unsupported character: " + c);
                    return null;
            }
        }
        
        return new byte[] { keyCode, modifiers };
    }
}
