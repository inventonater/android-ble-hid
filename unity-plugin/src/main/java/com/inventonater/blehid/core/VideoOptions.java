package com.inventonater.blehid.core;

import android.os.Bundle;

public class VideoOptions {
    private long durationMs = 5000; // 5 seconds by default
    private int tapDelay = 0;
    private int returnDelay = 0;
    private float buttonX = 0.5f;
    private float buttonY = 0.8f;
    private int acceptDialogDelay = 300;
    private float acceptXOffset = 0.2f;
    private float acceptYOffset = 0.05f;

    public VideoOptions() {
        // Default constructor uses the default values
    }

    public VideoOptions(Bundle bundle) {
        fromBundle(bundle);
    }

    public VideoOptions(float durationSeconds) {
        this.durationMs = (long) (durationSeconds * 1000);
    }

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

    public VideoOptions setDuration(float durationSeconds) {
        this.durationMs = (long) (durationSeconds * 1000);
        return this;
    }

    public VideoOptions setTapDelay(int tapDelay) {
        this.tapDelay = tapDelay;
        return this;
    }

    public VideoOptions setReturnDelay(int returnDelay) {
        this.returnDelay = returnDelay;
        return this;
    }

    public VideoOptions setButtonX(float buttonX) {
        this.buttonX = buttonX;
        return this;
    }

    public VideoOptions setButtonY(float buttonY) {
        this.buttonY = buttonY;
        return this;
    }

    public VideoOptions setAcceptDialogDelay(int acceptDialogDelay) {
        this.acceptDialogDelay = acceptDialogDelay;
        return this;
    }

    public VideoOptions setAcceptXOffset(float acceptXOffset) {
        this.acceptXOffset = acceptXOffset;
        return this;
    }

    public VideoOptions setAcceptYOffset(float acceptYOffset) {
        this.acceptYOffset = acceptYOffset;
        return this;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public int getTapDelay() {
        return tapDelay;
    }

    public int getReturnDelay() {
        return returnDelay;
    }

    public float getButtonX() {
        return buttonX;
    }

    public float getButtonY() {
        return buttonY;
    }

    public int getAcceptDialogDelay() {
        return acceptDialogDelay;
    }

    public float getAcceptXOffset() {
        return acceptXOffset;
    }

    public float getAcceptYOffset() {
        return acceptYOffset;
    }

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
