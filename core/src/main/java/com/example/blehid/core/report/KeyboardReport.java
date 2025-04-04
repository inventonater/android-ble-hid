package com.example.blehid.core.report;

import com.example.blehid.core.HidConstants;
import java.util.Arrays;

/**
 * Keyboard HID report implementation.
 * Represents a keyboard input report with modifiers and key codes.
 */
public class KeyboardReport implements Report {
    private final byte modifiers;
    private final byte[] keyCodes;
    
    /**
     * Creates a new keyboard report.
     *
     * @param modifiers Modifier keys bitmap (ctrl, shift, alt, etc.)
     * @param keyCodes Array of key codes (up to 6)
     */
    public KeyboardReport(byte modifiers, byte[] keyCodes) {
        this.modifiers = modifiers;
        
        // Ensure we have exactly MAX_KEYS (6) slots
        this.keyCodes = new byte[HidConstants.Keyboard.MAX_KEYS];
        
        // Copy provided key codes
        if (keyCodes != null) {
            int count = Math.min(keyCodes.length, HidConstants.Keyboard.MAX_KEYS);
            System.arraycopy(keyCodes, 0, this.keyCodes, 0, count);
        }
    }
    
    /**
     * Creates a new keyboard report with a single key.
     *
     * @param modifiers Modifier keys bitmap
     * @param keyCode Single key code
     */
    public KeyboardReport(byte modifiers, byte keyCode) {
        this.modifiers = modifiers;
        this.keyCodes = new byte[HidConstants.Keyboard.MAX_KEYS];
        
        if (keyCode != 0) {
            this.keyCodes[0] = keyCode;
        }
    }
    
    /**
     * Creates an empty (all keys released) keyboard report.
     */
    public KeyboardReport() {
        this.modifiers = 0;
        this.keyCodes = new byte[HidConstants.Keyboard.MAX_KEYS];
        // Arrays.fill(this.keyCodes, (byte)0); - not needed as new array is zeroed by default
    }
    
    /**
     * Gets the modifier keys bitmap.
     *
     * @return Modifier keys
     */
    public byte getModifiers() {
        return modifiers;
    }
    
    /**
     * Gets the key codes array (copy).
     *
     * @return Array of key codes
     */
    public byte[] getKeyCodes() {
        return Arrays.copyOf(keyCodes, keyCodes.length);
    }
    
    @Override
    public byte getReportId() {
        return HidConstants.REPORT_ID_KEYBOARD;
    }
    
    @Override
    public byte[] format() {
        // 8 bytes: modifiers(1) + reserved(1) + keys(6) - no reportId for Android compatibility
        // ReportID is only in the descriptor, not in the payload
        byte[] report = new byte[HidConstants.Keyboard.KEYBOARD_REPORT_SIZE];
        report[0] = modifiers;
        // report[1] is reserved byte, leave as 0
        System.arraycopy(keyCodes, 0, report, 2, keyCodes.length);
        return report;
    }
    
    @Override
    public <R> R accept(ReportVisitor<R> visitor) {
        return visitor.visit(this);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("KeyboardReport{modifiers=0x")
          .append(Integer.toHexString(modifiers & 0xFF))
          .append(", keys=[");
        
        for (int i = 0; i < keyCodes.length; i++) {
            if (keyCodes[i] != 0) {
                sb.append("0x").append(Integer.toHexString(keyCodes[i] & 0xFF));
                if (i < keyCodes.length - 1 && keyCodes[i + 1] != 0) {
                    sb.append(", ");
                }
            }
        }
        
        sb.append("]}");
        return sb.toString();
    }
}
