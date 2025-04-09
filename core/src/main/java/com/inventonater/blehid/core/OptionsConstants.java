package com.inventonater.blehid.core;

/**
 * Constants for options parameter names.
 * These are used to ensure consistency between the core Java layer and Unity C# layer.
 */
public class OptionsConstants {
    // Camera options
    public static final String PARAM_TAP_DELAY = "tap_delay_ms";
    public static final String PARAM_RETURN_DELAY = "return_delay_ms";
    public static final String PARAM_BUTTON_X = "button_x_position";
    public static final String PARAM_BUTTON_Y = "button_y_position";
    public static final String PARAM_ACCEPT_DIALOG_DELAY = "accept_dialog_delay_ms";
    public static final String PARAM_ACCEPT_X_OFFSET = "accept_button_x_offset";
    public static final String PARAM_ACCEPT_Y_OFFSET = "accept_button_y_offset";
    
    // Video options
    public static final String PARAM_VIDEO_DURATION = "video_duration_ms";
    
    private OptionsConstants() {
        // Private constructor to prevent instantiation
    }
}
