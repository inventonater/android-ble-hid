package com.inventonater.blehid.core;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

/**
 * Service for controlling media playback on the local device.
 * Uses Android MediaSession APIs to control media apps like Spotify, YouTube, and Audible.
 */
public class MediaControlService {
    private static final String TAG = "MediaControlService";

    private final Context context;
    private MediaSessionManager mediaSessionManager;
    private ComponentName notificationListenerComponent;
    private MediaController activeMediaController;
    private boolean isInitialized = false;

    /**
     * Creates a new MediaControlService instance
     * 
     * @param context The application context
     */
    public MediaControlService(Context context) {
        this.context = context;
    }

    /**
     * Initializes the media control service.
     * 
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.d(TAG, "Media control service already initialized");
            return true;
        }
        
        try {
            // Get the MediaSessionManager service
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaSessionManager = (MediaSessionManager) context.getSystemService(Context.MEDIA_SESSION_SERVICE);
                
                // We'll need a NotificationListenerService to access active media sessions
                notificationListenerComponent = new ComponentName(context, "com.inventonater.blehid.unity.MediaNotificationListenerService");
                
                // Try to get active sessions, but this will fail if permission is not granted
                getActiveSessions();
                
                isInitialized = true;
                Log.i(TAG, "Media control service initialized successfully");
                return true;
            } else {
                Log.e(TAG, "MediaSession API not available on this Android version");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize media control service", e);
            return false;
        }
    }

    /**
     * Checks if the app has notification listener permission.
     * This is required for MediaSession control.
     * 
     * @return true if permission is granted, false otherwise
     */
    public boolean hasNotificationListenerPermission() {
        String enabledListeners = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners");
        
        return enabledListeners != null && enabledListeners.contains(context.getPackageName());
    }

    /**
     * Gets active media sessions from the MediaSessionManager.
     * This will fail with a SecurityException if notification listener permission is not granted.
     */
    private void getActiveSessions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                List<MediaController> controllers = mediaSessionManager.getActiveSessions(notificationListenerComponent);
                if (controllers != null && !controllers.isEmpty()) {
                    // Use the first active session
                    activeMediaController = controllers.get(0);
                    Log.d(TAG, "Found active media session: " + activeMediaController.getPackageName());
                } else {
                    Log.d(TAG, "No active media sessions found");
                    activeMediaController = null;
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception when getting media sessions - notification listener permission not granted", e);
                activeMediaController = null;
            } catch (Exception e) {
                Log.e(TAG, "Error getting active media sessions", e);
                activeMediaController = null;
            }
        }
    }

    /**
     * Refreshes the active media controller, attempting to find the most recent active session.
     * 
     * @return true if an active media controller was found, false otherwise
     */
    public boolean refreshActiveController() {
        getActiveSessions();
        return activeMediaController != null;
    }

    /**
     * Sends a play/pause command to the active media session.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean playPause() {
        if (!isInitialized) {
            Log.e(TAG, "Media control service not initialized");
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (activeMediaController == null && !refreshActiveController()) {
                // Fallback to audio manager
                return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
            
            MediaController.TransportControls controls = activeMediaController.getTransportControls();
            try {
                PlaybackState state = activeMediaController.getPlaybackState();
                if (state != null && state.getState() == PlaybackState.STATE_PLAYING) {
                    controls.pause();
                } else {
                    controls.play();
                }
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error sending play/pause command", e);
                // Fallback to audio manager
                return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
            }
        }
        
        return false;
    }

    /**
     * Sends a next track command to the active media session.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean nextTrack() {
        if (!isInitialized) {
            Log.e(TAG, "Media control service not initialized");
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (activeMediaController == null && !refreshActiveController()) {
                // Fallback to audio manager
                return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
            }
            
            try {
                activeMediaController.getTransportControls().skipToNext();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error sending next track command", e);
                // Fallback to audio manager
                return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_NEXT);
            }
        }
        
        return false;
    }

    /**
     * Sends a previous track command to the active media session.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean previousTrack() {
        if (!isInitialized) {
            Log.e(TAG, "Media control service not initialized");
            return false;
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (activeMediaController == null && !refreshActiveController()) {
                // Fallback to audio manager
                return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
            
            try {
                activeMediaController.getTransportControls().skipToPrevious();
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error sending previous track command", e);
                // Fallback to audio manager
                return sendMediaKeyEvent(KeyEvent.KEYCODE_MEDIA_PREVIOUS);
            }
        }
        
        return false;
    }

    /**
     * Adjusts the volume up.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeUp() {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_VOLUME_UP);
    }

    /**
     * Adjusts the volume down.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean volumeDown() {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_VOLUME_DOWN);
    }

    /**
     * Mutes/unmutes the volume.
     * 
     * @return true if the command was sent successfully, false otherwise
     */
    public boolean mute() {
        return sendMediaKeyEvent(KeyEvent.KEYCODE_VOLUME_MUTE);
    }

    /**
     * Sends a media key event using AudioManager as a fallback when MediaSession API is unavailable.
     * 
     * @param keyCode The key code to send
     * @return true if the key event was sent successfully, false otherwise
     */
    private boolean sendMediaKeyEvent(int keyCode) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            
            // Create and send key down event
            KeyEvent keyDown = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            audioManager.dispatchMediaKeyEvent(keyDown);
            
            // Create and send key up event
            KeyEvent keyUp = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
            audioManager.dispatchMediaKeyEvent(keyUp);
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error sending media key event: " + keyCode, e);
            return false;
        }
    }

    /**
     * Checks if the app has MEDIA_CONTENT_CONTROL permission.
     * 
     * @return true if permission is granted, false otherwise
     */
    public boolean hasMediaContentControlPermission() {
        return context.checkCallingOrSelfPermission("android.permission.MEDIA_CONTENT_CONTROL") 
                == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Opens notification settings to allow the user to grant notification listener permission.
     */
    public void openNotificationListenerSettings() {
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * Gets the active media app package name, if available.
     * 
     * @return The package name of the active media app, or null if not available
     */
    public String getActiveMediaAppPackage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && activeMediaController != null) {
            return activeMediaController.getPackageName();
        }
        return null;
    }

    /**
     * Cleans up resources.
     */
    public void close() {
        activeMediaController = null;
        isInitialized = false;
        Log.i(TAG, "Media control service closed");
    }
}
