package com.inventonater.blehid.unity;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.inventonater.blehid.core.AccessibilityControlService;

/**
 * Android AccessibilityService implementation for BleHid.
 * This service must be enabled by the user in system settings.
 * It provides the system-level access needed for the AccessibilityControlService.
 */
public class BleHidAccessibilityService extends AccessibilityService {
    private static final String TAG = "BleHidAccessService";
    
    // Singleton instance that can be accessed by BleHidUnityPlugin
    private static BleHidAccessibilityService instance;
    
    // Handler for main thread operations
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    /**
     * Gets the singleton instance of the service.
     * Will be null if the service is not running.
     */
    public static BleHidAccessibilityService getInstance() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "BleHidAccessibilityService created");
    }
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        
        // Store instance for singleton access
        instance = this;
        
        // Configure service capabilities
        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) {
            info = new AccessibilityServiceInfo();
        }
        
        // Set flags to capture various events and provide various capabilities
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE |
                     AccessibilityServiceInfo.FLAG_REQUEST_ENHANCED_WEB_ACCESSIBILITY |
                     AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        
        // Request all event types
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        
        // Request filter for specific package if needed
        //info.packageNames = new String[]{"com.example.package"};
        
        // Set feedback type
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        
        // Note: canRetrieveWindowContent is set in the XML configuration file
        // We don't need to set it programmatically
        
        // Apply updated configuration
        setServiceInfo(info);
        
        Log.i(TAG, "BleHidAccessibilityService connected and configured");
        
        // Notify the Unity plugin that the service is connected
        mainHandler.post(() -> {
            BleHidUnityPlugin plugin = BleHidUnityPlugin.getInstance();
            if (plugin != null) {
                plugin.onAccessibilityServiceConnected(this);
            }
        });
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We don't need to process events for now, just handle actions
    }
    
    @Override
    public void onInterrupt() {
        Log.i(TAG, "BleHidAccessibilityService interrupted");
    }
    
    @Override
    public boolean onKeyEvent(KeyEvent event) {
        // Pass through key events
        return super.onKeyEvent(event);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Clear singleton instance
        if (instance == this) {
            instance = null;
        }
        
        // Notify the Unity plugin that the service is disconnected
        mainHandler.post(() -> {
            BleHidUnityPlugin plugin = BleHidUnityPlugin.getInstance();
            if (plugin != null) {
                plugin.onAccessibilityServiceDisconnected();
            }
        });
        
        Log.i(TAG, "BleHidAccessibilityService destroyed");
    }
}
