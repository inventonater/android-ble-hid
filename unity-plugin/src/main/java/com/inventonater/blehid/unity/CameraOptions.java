package com.inventonater.blehid.unity;

import android.os.Parcelable;

/**
 * Adapter class for CameraOptions that bridges the Unity interface with the core implementation.
 * This class exists to maintain backward compatibility with the Unity C# API.
 */
public class CameraOptions {
    private com.inventonater.blehid.core.CameraOptions coreOptions;
    
    /**
     * Create options with default values
     */
    public CameraOptions() {
        coreOptions = new com.inventonater.blehid.core.CameraOptions();
    }
    
    /**
     * Set the delay before tapping the shutter button
     * @param tapDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public CameraOptions setTapDelay(int tapDelay) {
        coreOptions.setTapDelay(tapDelay);
        return this;
    }
    
    /**
     * Set the delay before returning to the app
     * @param returnDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public CameraOptions setReturnDelay(int returnDelay) {
        coreOptions.setReturnDelay(returnDelay);
        return this;
    }
    
    /**
     * Set the X position of the shutter button
     * @param buttonX X position as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public CameraOptions setButtonX(float buttonX) {
        coreOptions.setButtonX(buttonX);
        return this;
    }
    
    /**
     * Set the Y position of the shutter button
     * @param buttonY Y position as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public CameraOptions setButtonY(float buttonY) {
        coreOptions.setButtonY(buttonY);
        return this;
    }
    
    /**
     * Set the delay before tapping the accept dialog button
     * @param acceptDialogDelay Delay in milliseconds
     * @return This options object for chaining
     */
    public CameraOptions setAcceptDialogDelay(int acceptDialogDelay) {
        coreOptions.setAcceptDialogDelay(acceptDialogDelay);
        return this;
    }
    
    /**
     * Set the X offset from center for the accept button
     * @param acceptXOffset X offset as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public CameraOptions setAcceptXOffset(float acceptXOffset) {
        coreOptions.setAcceptXOffset(acceptXOffset);
        return this;
    }
    
    /**
     * Set the Y offset from center for the accept button
     * @param acceptYOffset Y offset as ratio (0.0-1.0)
     * @return This options object for chaining
     */
    public CameraOptions setAcceptYOffset(float acceptYOffset) {
        coreOptions.setAcceptYOffset(acceptYOffset);
        return this;
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
     * @return Core CameraOptions object
     */
    public com.inventonater.blehid.core.CameraOptions toAndroidObject() {
        return coreOptions;
    }
}
