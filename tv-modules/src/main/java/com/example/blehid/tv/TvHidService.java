package com.example.blehid.tv;

import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothDevice;

import java.util.UUID;

/**
 * Interface defining common HID functionality for TV remote controls.
 * All TV-specific implementations should implement this interface.
 */
public interface TvHidService {
    // Standard HID service UUID
    UUID HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    /**
     * Direction constants for D-pad navigation
     */
    int DIRECTION_UP = 1;
    int DIRECTION_DOWN = 2;
    int DIRECTION_LEFT = 3;
    int DIRECTION_RIGHT = 4;
    int DIRECTION_CENTER = 5; // Center/OK/Select button
    
    /**
     * Media control constants
     */
    int MEDIA_PLAY_PAUSE = 1;
    int MEDIA_NEXT = 2;
    int MEDIA_PREVIOUS = 3;
    int MEDIA_VOLUME_UP = 4;
    int MEDIA_VOLUME_DOWN = 5;
    int MEDIA_MUTE = 6;
    
    /**
     * Initializes the service.
     * 
     * @return true if initialization was successful, false otherwise
     */
    boolean initialize();
    
    /**
     * Cleans up resources used by the service.
     */
    void close();
    
    /**
     * Gets the Bluetooth GATT service for this HID service.
     * 
     * @return The Bluetooth GATT service
     */
    BluetoothGattService getGattService();
    
    /**
     * Sets the connected device.
     * 
     * @param device The connected device
     */
    void setConnectedDevice(BluetoothDevice device);
    
    /**
     * Gets the connected device.
     * 
     * @return The connected device, or null if not connected
     */
    BluetoothDevice getConnectedDevice();
    
    /**
     * Sends a directional button press (D-pad).
     * 
     * @param direction One of DIRECTION_* constants
     * @return true if the command was sent successfully, false otherwise
     */
    boolean pressDirectionalButton(int direction);
    
    /**
     * Releases all directional buttons.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    boolean releaseDirectionalButtons();
    
    /**
     * Performs a directional button click (press and release).
     * 
     * @param direction One of DIRECTION_* constants
     * @return true if the command was sent successfully, false otherwise
     */
    boolean clickDirectionalButton(int direction);
    
    /**
     * Moves the cursor/pointer by the specified amount.
     * 
     * @param x The X movement (-127 to 127)
     * @param y The Y movement (-127 to 127)
     * @return true if the command was sent successfully, false otherwise
     */
    boolean moveCursor(int x, int y);
    
    /**
     * Presses the select/OK button (often used as mouse click).
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    boolean pressSelectButton();
    
    /**
     * Releases the select/OK button.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    boolean releaseSelectButton();
    
    /**
     * Performs a click with the select/OK button (press and release).
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    boolean clickSelectButton();
    
    /**
     * Sends a media control command.
     * 
     * @param mediaControl One of MEDIA_* constants
     * @return true if the command was sent successfully, false otherwise
     */
    boolean sendMediaControl(int mediaControl);
    
    /**
     * Sends a back button press.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    boolean pressBackButton();
    
    /**
     * Sends a home button press.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    boolean pressHomeButton();
    
    /**
     * Checks if this TV implementation supports pointer/cursor movement.
     * 
     * @return true if pointer movement is supported, false otherwise
     */
    boolean supportsPointer();
    
    /**
     * Checks if this TV implementation supports media controls.
     * 
     * @return true if media controls are supported, false otherwise
     */
    boolean supportsMediaControls();
    
    /**
     * Gets a human-readable name for this TV implementation.
     * 
     * @return The name of the TV implementation (e.g., "LG Smart TV")
     */
    String getImplementationName();
}
