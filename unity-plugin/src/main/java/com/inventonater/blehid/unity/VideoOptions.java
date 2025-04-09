package com.inventonater.blehid.unity;

import android.os.Parcelable;

/**
 * Adapter class for VideoOptions that bridges the Unity interface with the core implementation.
 * This class exists to maintain backward compatibility with the Unity C# API.
 */
public class VideoOptions {
    private com.inventonater.blehid.core.VideoOptions coreOptions;
    
    /**
     * Create options with default values
     */
    public VideoOptions() {
        coreOptions = new com.inventonater.blehid.core.VideoOptions();
    }
    
    /**
     * Create options with specified duration
     * @param durationSeconds Duration in seconds
     */
    public VideoOptions(float durationSeconds) {
        coreOptions = new com.inventonater.blehid.core.VideoOptions(durationSeconds);
    }
    
    /**
     * Set the recording duration
     * @param durationSeconds Duration in seconds
     * @return This options object for chaining
     */
    public VideoOptions setDuration(float durationSeconds) {
        coreOptions.setDuration(durationSeconds);
        return this;
    }
    
    /**
     * Set the delay before tapping the record button
     * @param tapDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public VideoOptions setTapDelay(int tapDelay) {
        coreOptions.setTapDelay(tapDelay);
        return this;
    }
    
    /**
     * Set the delay before returning to the app
     * @param returnDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public VideoOptions setReturnDelay(int returnDelay) {
        coreOptions.setReturnDelay(returnDelay);
        return this;
    }
    
    /**
     * Set the X position of the record button
     * @param buttonX X position as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public VideoOptions setButtonX(float buttonX) {
        coreOptions.setButtonX(buttonX);
        return this;
    }
    
    /**
     * Set the Y position of the record button
     * @param buttonY Y position as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public VideoOptions setButtonY(float buttonY) {
        coreOptions.setButtonY(buttonY);
        return this;
    }
    
    /**
     * Set the delay before tapping the accept dialog button
     * @param acceptDialogDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public VideoOptions setAcceptDialogDelay(int acceptDialogDelay) {
        coreOptions.setAcceptDialogDelay(acceptDialogDelay);
        return this;
    }
    
    /**
     * Set the X offset from center for the accept button
     * @param acceptXOffset X offset as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public VideoOptions setAcceptXOffset(float acceptXOffset) {
        coreOptions.setAcceptXOffset(acceptXOffset);
        return this;
    }
    
    /**
     * Set the Y offset from center for the accept button
     * @param acceptYOffset Y offset as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public VideoOptions setAcceptYOffset(float acceptYOffset) {
        coreOptions.setAcceptYOffset(acceptYOffset);
        return this;
    }
    
    /**
     * Get the duration in milliseconds
     * @return Duration in milliseconds
     */
    public long getDurationMs() {
        return coreOptions.getDurationMs();
    }
    
    /**
     * Get the tap delay
     * @return Tap delay in milliseconds
     */
    public int getTapDelay() {
        return coreOptions.getTapDelay();
    }
    
    /**
     * Get the return delay
     * @return Return delay in milliseconds
     */
    public int getReturnDelay() {
        return coreOptions.getReturnDelay();
    }
    
    /**
     * Get the button X position
     * @return Button X position as ratio (0.0-1.0)
     */
    public float getButtonX() {
        return coreOptions.getButtonX();
    }
    
    /**
     * Get the button Y position
     * @return Button Y position as ratio (0.0-1.0)
     */
    public float getButtonY() {
        return coreOptions.getButtonY();
    }
    
    /**
     * Get the accept dialog delay
     * @return Accept dialog delay in milliseconds
     */
    public int getAcceptDialogDelay() {
        return coreOptions.getAcceptDialogDelay();
    }
    
    /**
     * Get the accept button X offset
     * @return Accept button X offset as ratio (0.0-1.0)
     */
    public float getAcceptXOffset() {
        return coreOptions.getAcceptXOffset();
    }
    
    /**
     * Get the accept button Y offset
     * @return Accept button Y offset as ratio (0.0-1.0)
     */
    public float getAcceptYOffset() {
        return coreOptions.getAcceptYOffset();
    }
    
    /**
     * Get the underlying core options object
     * @return Core VideoOptions object
     */
    public com.inventonater.blehid.core.VideoOptions toAndroidObject() {
        return coreOptions;
    }
}
