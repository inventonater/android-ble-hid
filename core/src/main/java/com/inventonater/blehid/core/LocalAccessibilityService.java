package com.inventonater.blehid.core;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Accessibility service for input control.
 */
public class LocalAccessibilityService extends AccessibilityService {
    private static final String TAG = "LocalAccessibilityService";
    
    // Static reference to the service instance for the controller
    private static LocalAccessibilityService instance;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Accessibility service created");
    }
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        Log.d(TAG, "Accessibility service connected");
        
        // Register with controller if available
        LocalInputManager manager = LocalInputManager.getInstance();
        if (manager != null) {
            manager.registerAccessibilityService(this);
        }
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        Log.d(TAG, "Accessibility service unbound");
        return super.onUnbind(intent);
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // We don't need to process accessibility events
    }
    
    @Override
    public void onInterrupt() {
        // Interruption handling not needed
    }
    
    /**
     * Gets the current instance if available.
     */
    public static LocalAccessibilityService getInstance() {
        return instance;
    }
}
