package com.example.blehid.app.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.blehid.app.R;
import com.example.blehid.core.HidConstants;
import com.example.blehid.core.BleHidManager;

/**
 * Fragment that handles media control input
 */
public class MediaFragment extends Fragment {
    private static final String TAG = "MediaFragment";
    
    // Media control buttons
    private Button mediaPlayPause;
    private Button mediaPrev;
    private Button mediaNext;
    private Button mediaVolDown;
    private Button mediaMute;
    private Button mediaVolUp;
    private Button mediaStop;
    private Button mediaRecord;
    private Button mediaFastForward;
    
    private BleHidManager bleHidManager;
    private MouseFragment.HidEventListener eventListener;
    
    public void setBleHidManager(BleHidManager manager) {
        this.bleHidManager = manager;
    }
    
    public void setEventListener(MouseFragment.HidEventListener listener) {
        this.eventListener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize media control buttons
        mediaPlayPause = view.findViewById(R.id.mediaPlayPause);
        mediaPrev = view.findViewById(R.id.mediaPrev);
        mediaNext = view.findViewById(R.id.mediaNext);
        mediaVolDown = view.findViewById(R.id.mediaVolDown);
        mediaMute = view.findViewById(R.id.mediaMute);
        mediaVolUp = view.findViewById(R.id.mediaVolUp);
        mediaStop = view.findViewById(R.id.mediaStop);
        mediaRecord = view.findViewById(R.id.mediaRecord);
        mediaFastForward = view.findViewById(R.id.mediaFastForward);
        
        setupControls();
    }
    
    private void setupControls() {
        // Set up all media control buttons
        mediaPlayPause.setOnClickListener(v -> sendMediaCommand(HidConstants.Consumer.PLAY_PAUSE));
        mediaPrev.setOnClickListener(v -> sendMediaCommand(HidConstants.Consumer.PREV_TRACK));
        mediaNext.setOnClickListener(v -> sendMediaCommand(HidConstants.Consumer.NEXT_TRACK));
        mediaVolDown.setOnClickListener(v -> sendMediaCommand(HidConstants.Consumer.VOLUME_DOWN));
        mediaMute.setOnClickListener(v -> sendMediaCommand(HidConstants.Consumer.MUTE));
        mediaVolUp.setOnClickListener(v -> sendMediaCommand(HidConstants.Consumer.VOLUME_UP));
        mediaStop.setOnClickListener(v -> sendMediaCommand(HidConstants.Consumer.STOP));
        mediaRecord.setOnClickListener(v -> sendMediaCommand(HidConstants.Consumer.RECORD));
        mediaFastForward.setOnClickListener(v -> sendMediaCommand(HidConstants.Consumer.FAST_FORWARD));
    }
    
    private void sendMediaCommand(byte mediaKey) {
        if (bleHidManager == null || !bleHidManager.isConnected()) {
            logEvent("MEDIA KEY IGNORED: No connected device");
            return;
        }
        
        String keyName = getMediaKeyName(mediaKey);
        
        // Press the media key
        boolean pressResult = bleHidManager.sendConsumerControl(mediaKey);
        
        // Add a delay for the key press to register
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        // Release the key by sending a zero control code
        boolean releaseResult = bleHidManager.sendConsumerControl((byte)0);
        boolean result = pressResult && releaseResult;
        
        logEvent("MEDIA KEY: " + keyName + (result ? " pressed" : " FAILED"));
    }
    
    private String getMediaKeyName(byte mediaKey) {
        switch (mediaKey) {
            case HidConstants.Consumer.PLAY_PAUSE:
                return "PLAY/PAUSE";
            case HidConstants.Consumer.NEXT_TRACK:
                return "NEXT";
            case HidConstants.Consumer.PREV_TRACK:
                return "PREV";
            case HidConstants.Consumer.VOLUME_UP:
                return "VOLUME UP";
            case HidConstants.Consumer.VOLUME_DOWN:
                return "VOLUME DOWN";
            case HidConstants.Consumer.MUTE:
                return "MUTE";
            case HidConstants.Consumer.STOP:
                return "STOP";
            case HidConstants.Consumer.RECORD:
                return "RECORD";
            case HidConstants.Consumer.FAST_FORWARD:
                return "FAST FORWARD";
            case 0:
                return "NONE";
            default:
                return "MEDIA_0x" + Integer.toHexString(mediaKey & 0xFF);
        }
    }
    
    private void logEvent(String event) {
        if (eventListener != null) {
            eventListener.onHidEvent(event);
        }
    }
}
