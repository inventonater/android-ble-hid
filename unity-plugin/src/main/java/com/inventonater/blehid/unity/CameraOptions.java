package com.inventonater.blehid.unity;

import android.os.Bundle;

/**
 * Options for camera picture capture operations.
 * This class provides a clean way to configure camera operations
 * with sensible defaults.
 */
public class CameraOptions {
    // Default values set in the constructor
    private int tapDelay = 0;
    private int returnDelay = 0;
    private float buttonX = 0.5f;
    private float buttonY = 0.8f;
    private int acceptDialogDelay = 300;
    private float acceptXOffset = 0.2f;
    private float acceptYOffset = 0.05f;
    
    /**
     * Create options with default values
     */
    public CameraOptions() {
        // Default constructor uses the default values
    }
    
    /**
     * Set the delay before tapping the shutter button
     * @param tapDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public CameraOptions setTapDelay(int tapDelay) {
        this.tapDelay = tapDelay;
        return this;
    }
    
    /**
     * Set the delay before returning to the app
     * @param returnDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public CameraOptions setReturnDelay(int returnDelay) {
        this.returnDelay = returnDelay;
        return this;
    }
    
    /**
     * Set the X position of the shutter button
     * @param buttonX X position as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public CameraOptions setButtonX(float buttonX) {
        this.buttonX = buttonX;
        return this;
    }
    
    /**
     * Set the Y position of the shutter button
     * @param buttonY Y position as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public CameraOptions setButtonY(float buttonY) {
        this.buttonY = buttonY;
        return this;
    }
    
    /**
     * Set the delay before tapping the accept dialog button
     * @param acceptDialogDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public CameraOptions setAcceptDialogDelay(int acceptDialogDelay) {
        this.acceptDialogDelay = acceptDialogDelay;
        return this;
    }
    
    /**
     * Set the X offset from center for the accept button
     * @param acceptXOffset X offset as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public CameraOptions setAcceptXOffset(float acceptXOffset) {
        this.acceptXOffset = acceptXOffset;
        return this;
    }
    
    /**
     * Set the Y offset from center for the accept button
     * @param acceptYOffset Y offset as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public CameraOptions setAcceptYOffset(float acceptYOffset) {
        this.acceptYOffset = acceptYOffset;
        return this;
    }
    
    /**
     * Get the tap delay
     * @return Tap delay in milliseconds
     */
    public int getTapDelay() {
        return tapDelay;
    }
    
    /**
     * Get the return delay
     * @return Return delay in milliseconds
     */
    public int getReturnDelay() {
        return returnDelay;
    }
    
    /**
     * Get the button X position
     * @return Button X position as ratio (0.0-1.0)
     */
    public float getButtonX() {
        return buttonX;
    }
    
    /**
     * Get the button Y position
     * @return Button Y position as ratio (0.0-1.0)
     */
    public float getButtonY() {
        return buttonY;
    }
    
    /**
     * Get the accept dialog delay
     * @return Accept dialog delay in milliseconds
     */
    public int getAcceptDialogDelay() {
        return acceptDialogDelay;
    }
    
    /**
     * Get the accept button X offset
     * @return Accept button X offset as ratio (0.0-1.0)
     */
    public float getAcceptXOffset() {
        return acceptXOffset;
    }
    
    /**
     * Get the accept button Y offset
     * @return Accept button Y offset as ratio (0.0-1.0)
     */
    public float getAcceptYOffset() {
        return acceptYOffset;
    }
    
    /**
     * Convert options to a Bundle for legacy API compatibility
     * @return Bundle with all options as parameters
     */
    public Bundle toBundle() {
        Bundle bundle = new Bundle();
        bundle.putInt("tap_delay_ms", tapDelay);
        bundle.putInt("return_delay_ms", returnDelay);
        bundle.putFloat("button_x_position", buttonX);
        bundle.putFloat("button_y_position", buttonY);
        bundle.putInt("accept_dialog_delay_ms", acceptDialogDelay);
        bundle.putFloat("accept_button_x_offset", acceptXOffset);
        bundle.putFloat("accept_button_y_offset", acceptYOffset);
        return bundle;
    }
}
