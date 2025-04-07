package com.example.blehid.app.ui;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.blehid.app.R;
import com.example.blehid.core.BleHidManager;

/**
 * Manages the media panel UI and functionality.
 * Handles media control buttons for playback and volume control.
 */
public class MediaPanelManager {
    private static final String TAG = "MediaPanelManager";

    public interface Callback {
        void logEvent(String message);
    }

    // Functional interface for media control actions with boolean return
    private interface MediaControlAction {
        boolean execute();
    }

    private final Context context;
    private final View mediaPanel;
    private final BleHidManager bleHidManager;
    private final Callback callback;

    private Button playPauseButton;
    private Button previousButton;
    private Button nextButton;
    private Button volumeUpButton;
    private Button volumeDownButton;
    private Button muteButton;

    /**
     * Creates a new MediaPanelManager.
     *
     * @param context The activity context
     * @param mediaPanel The media panel view
     * @param bleHidManager The BLE HID manager instance
     * @param callback Callback for logging events
     */
    public MediaPanelManager(Context context, View mediaPanel, BleHidManager bleHidManager, Callback callback) {
        this.context = context;
        this.mediaPanel = mediaPanel;
        this.bleHidManager = bleHidManager;
        this.callback = callback;

        initializeViews();
        setupListeners();
    }

    /**
     * Initialize the media panel UI elements.
     */
    private void initializeViews() {
        playPauseButton = mediaPanel.findViewById(R.id.playPauseButton);
        previousButton = mediaPanel.findViewById(R.id.previousButton);
        nextButton = mediaPanel.findViewById(R.id.nextButton);
        volumeUpButton = mediaPanel.findViewById(R.id.volumeUpButton);
        volumeDownButton = mediaPanel.findViewById(R.id.volumeDownButton);
        muteButton = mediaPanel.findViewById(R.id.muteButton);
    }

    /**
     * Set up click listeners for the media control buttons.
     */
    private void setupListeners() {
        // Play/Pause button
        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("PLAY/PAUSE", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.playPause();
                    }
                });
            }
        });
        
        // Previous track button
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("PREVIOUS", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.previousTrack();
                    }
                });
            }
        });
        
        // Next track button
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("NEXT", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.nextTrack();
                    }
                });
            }
        });
        
        // Volume up button
        volumeUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("VOLUME UP", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.volumeUp();
                    }
                });
            }
        });
        
        // Volume down button
        volumeDownButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("VOLUME DOWN", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.volumeDown();
                    }
                });
            }
        });
        
        // Mute button
        muteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMediaControl("MUTE", new MediaControlAction() {
                    @Override
                    public boolean execute() {
                        return bleHidManager.mute();
                    }
                });
            }
        });
    }

    /**
     * Helper method to send a media control command.
     * 
     * @param controlName Name of the control for logging
     * @param controlAction MediaControlAction that performs the control action and returns a boolean result
     */
    private void sendMediaControl(String controlName, MediaControlAction controlAction) {
        if (!bleHidManager.isConnected()) {
            Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
            callback.logEvent("MEDIA CONTROL IGNORED: No connected device");
            return;
        }
        
        callback.logEvent("MEDIA CONTROL: " + controlName);
        
        // Execute the control action
        boolean result = controlAction.execute();
        
        if (result) {
            callback.logEvent("MEDIA CONTROL: " + controlName + " sent");
        } else {
            callback.logEvent("MEDIA CONTROL: " + controlName + " FAILED");
            Toast.makeText(context, "Failed to send " + controlName, Toast.LENGTH_SHORT).show();
        }
    }
}
