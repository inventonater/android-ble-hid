package com.inventonater.blehid.core;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.view.KeyEvent;
import android.util.Log;

/**
 * Controls media playback on the local device.
 * Uses MediaSession and AudioManager APIs.
 */
public class LocalMediaController {
    private static final String TAG = "LocalMediaController";
    
    private final Context context;
    private final AudioManager audioManager;
    
    public LocalMediaController(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }
    
    /**
     * Sends a media control event.
     * @param keyCode The key code for the media action
     * @return true if successful, false otherwise
     */
    public boolean sendMediaControlEvent(int keyCode) {
        if (audioManager == null) {
            Log.e(TAG, "AudioManager not available");
            return false;
        }
        
        try {
            // Create key event
            KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
            
            // Send media key events
            audioManager.dispatchMediaKeyEvent(downEvent);
            audioManager.dispatchMediaKeyEvent(upEvent);
            
            Log.d(TAG, "Media control event sent: " + keyCode);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending media control event: " + e.getMessage());
            return false;
        }
    }
    
    public boolean playPause() {
        return sendMediaControlEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
    }
    
    public boolean next() {
        return sendMediaControlEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
    }
    
    public boolean previous() {
        return sendMediaControlEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
    }
    
    public boolean volumeUp() {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE,
                AudioManager.FLAG_SHOW_UI
            );
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting volume up: " + e.getMessage());
            return false;
        }
    }
    
    public boolean volumeDown() {
        try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER,
                AudioManager.FLAG_SHOW_UI
            );
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting volume down: " + e.getMessage());
            return false;
        }
    }
    
    public boolean mute() {
        boolean isMuted = audioManager.isStreamMute(AudioManager.STREAM_MUSIC);
        if (isMuted) {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_UNMUTE,
                AudioManager.FLAG_SHOW_UI
            );
        } else {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_SHOW_UI
            );
        }
        return true;
    }
}
