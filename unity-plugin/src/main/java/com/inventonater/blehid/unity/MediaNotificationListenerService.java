package com.inventonater.blehid.unity;

import android.content.ComponentName;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.util.Log;

import java.util.List;

/**
 * NotificationListenerService to access and control media sessions.
 * This service must be enabled by the user in system settings.
 * It provides access to the current media sessions on the device.
 */
public class MediaNotificationListenerService extends NotificationListenerService {
    private static final String TAG = "MediaNotificationListener";
    
    // Singleton instance that can be accessed by BleHidUnityPlugin
    private static MediaNotificationListenerService instance;
    
    // Media controllers from active sessions
    private List<MediaController> activeControllers;
    
    /**
     * Gets the singleton instance of the service.
     * Will be null if the service is not running.
     */
    public static MediaNotificationListenerService getInstance() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Log.i(TAG, "MediaNotificationListenerService created");
    }
    
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.i(TAG, "MediaNotificationListenerService connected");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            refreshMediaSessions();
        }
        
        // Notify the Unity plugin that the service is connected
        BleHidUnityPlugin plugin = BleHidUnityPlugin.getInstance();
        if (plugin != null) {
            plugin.onMediaListenerServiceConnected();
        }
    }
    
    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.i(TAG, "MediaNotificationListenerService disconnected");
        
        // Notify the Unity plugin that the service is disconnected
        BleHidUnityPlugin plugin = BleHidUnityPlugin.getInstance();
        if (plugin != null) {
            plugin.onMediaListenerServiceDisconnected();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clear singleton instance
        if (instance == this) {
            instance = null;
        }
        
        Log.i(TAG, "MediaNotificationListenerService destroyed");
    }
    
    /**
     * Refreshes the list of active media sessions.
     */
    public void refreshMediaSessions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                MediaSessionManager manager = 
                        (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
                
                if (manager != null) {
                    activeControllers = manager.getActiveSessions(
                            new ComponentName(getPackageName(), getClass().getName()));
                    
                    if (activeControllers != null && !activeControllers.isEmpty()) {
                        Log.i(TAG, "Found " + activeControllers.size() + " active media sessions");
                        for (MediaController controller : activeControllers) {
                            Log.d(TAG, "Active media session: " + controller.getPackageName());
                        }
                    } else {
                        Log.i(TAG, "No active media sessions found");
                    }
                }
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception getting media sessions", e);
            } catch (Exception e) {
                Log.e(TAG, "Error getting active media sessions", e);
            }
        }
    }
    
    /**
     * Gets the active media controllers.
     * 
     * @return List of active media controllers, may be null
     */
    public List<MediaController> getActiveControllers() {
        return activeControllers;
    }
    
    /**
     * Gets the most recent active media controller.
     * 
     * @return The most recent active MediaController, or null if none
     */
    public MediaController getMostRecentController() {
        if (activeControllers != null && !activeControllers.isEmpty()) {
            return activeControllers.get(0);
        }
        return null;
    }
}
