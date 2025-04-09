package com.inventonater.blehid.core;

import android.os.Bundle;

/**
 * Options for video recording operations.
 * This class provides a clean way to configure video operations
 * with sensible defaults.
 */
public class VideoOptions {
    // Default values
    private long durationMs = 5000; // 5 seconds by default
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
public VideoOptions() {
    // Default constructor uses the default values
}

/**
 * Create options from a Bundle
 * @param bundle Bundle containing parameters
 */
public VideoOptions(Bundle bundle) {
    fromBundle(bundle);
}

/**
 * Create options with specified duration
 * @param durationSeconds Duration in seconds
 */
public VideoOptions(float durationSeconds) {
    this.durationMs = (long)(durationSeconds * 1000);
}

/**
 * Apply settings from a bundle
 * @param bundle Bundle containing parameters
 * @return This options object for chaining
 */
public VideoOptions fromBundle(Bundle bundle) {
    if (bundle == null) return this;
    
    if (bundle.containsKey(OptionsConstants.PARAM_VIDEO_DURATION)) {
        this.durationMs = bundle.getLong(OptionsConstants.PARAM_VIDEO_DURATION);
    }
    
    if (bundle.containsKey(OptionsConstants.PARAM_TAP_DELAY)) {
        this.tapDelay = bundle.getInt(OptionsConstants.PARAM_TAP_DELAY);
    }
    
    if (bundle.containsKey(OptionsConstants.PARAM_RETURN_DELAY)) {
        this.returnDelay = bundle.getInt(OptionsConstants.PARAM_RETURN_DELAY);
    }
    
    if (bundle.containsKey(OptionsConstants.PARAM_BUTTON_X)) {
        this.buttonX = bundle.getFloat(OptionsConstants.PARAM_BUTTON_X);
    }
    
    if (bundle.containsKey(OptionsConstants.PARAM_BUTTON_Y)) {
        this.buttonY = bundle.getFloat(OptionsConstants.PARAM_BUTTON_Y);
    }
    
    if (bundle.containsKey(OptionsConstants.PARAM_ACCEPT_DIALOG_DELAY)) {
        this.acceptDialogDelay = bundle.getInt(OptionsConstants.PARAM_ACCEPT_DIALOG_DELAY);
    }
    
    if (bundle.containsKey(OptionsConstants.PARAM_ACCEPT_X_OFFSET)) {
        this.acceptXOffset = bundle.getFloat(OptionsConstants.PARAM_ACCEPT_X_OFFSET);
    }
    
    if (bundle.containsKey(OptionsConstants.PARAM_ACCEPT_Y_OFFSET)) {
        this.acceptYOffset = bundle.getFloat(OptionsConstants.PARAM_ACCEPT_Y_OFFSET);
    }
    
    return this;
}
    
    /**
     * Set the recording duration
     * @param durationSeconds Duration in seconds
     * @return This options object for chaining
     */
    public VideoOptions setDuration(float durationSeconds) {
        this.durationMs = (long)(durationSeconds * 1000);
        return this;
    }
    
    /**
     * Set the delay before tapping the record button
     * @param tapDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public VideoOptions setTapDelay(int tapDelay) {
        this.tapDelay = tapDelay;
        return this;
    }
    
    /**
     * Set the delay before returning to the app
     * @param returnDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public VideoOptions setReturnDelay(int returnDelay) {
        this.returnDelay = returnDelay;
        return this;
    }
    
    /**
     * Set the X position of the record button
     * @param buttonX X position as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public VideoOptions setButtonX(float buttonX) {
        this.buttonX = buttonX;
        return this;
    }
    
    /**
     * Set the Y position of the record button
     * @param buttonY Y position as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public VideoOptions setButtonY(float buttonY) {
        this.buttonY = buttonY;
        return this;
    }
    
    /**
     * Set the delay before tapping the accept dialog button
     * @param acceptDialogDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public VideoOptions setAcceptDialogDelay(int acceptDialogDelay) {
        this.acceptDialogDelay = acceptDialogDelay;
        return this;
    }
    
    /**
     * Set the X offset from center for the accept button
     * @param acceptXOffset X offset as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public VideoOptions setAcceptXOffset(float acceptXOffset) {
        this.acceptXOffset = acceptXOffset;
        return this;
    }
    
    /**
     * Set the Y offset from center for the accept button
     * @param acceptYOffset Y offset as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public VideoOptions setAcceptYOffset(float acceptYOffset) {
        this.acceptYOffset = acceptYOffset;
        return this;
    }
    
    /**
     * Get the duration in milliseconds
     * @return Duration in milliseconds
     */
    public long getDurationMs() {
        return durationMs;
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
        bundle.putLong("video_duration_ms", durationMs);
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
