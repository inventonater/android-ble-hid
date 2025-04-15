package com.inventonater.blehid.core;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

/**
 * Accessibility service for input control.
 * This service runs at the system level and can perform gestures even when our app is not in focus.
 */
public class LocalAccessibilityService extends AccessibilityService {
    private static final String TAG = "LocalAccessibilityService";
    
    // Singleton instance
    private static LocalAccessibilityService instance;
    
    // Handler for periodic connection checking
    private Handler handler = new Handler(Looper.getMainLooper());
    
    // Service status tracking
    private boolean isConnected = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Accessibility service created");
    }
    
    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        isConnected = true;
        Log.d(TAG, "Accessibility service connected and ready for use");
        
        // Start the foreground service to ensure we stay alive
        startForegroundServiceIfNeeded();
        
        // Register with controller if available
        LocalInputManager manager = LocalInputManager.getInstance();
        if (manager != null) {
            manager.registerAccessibilityService(this);
            Log.d(TAG, "Registered with LocalInputManager");
        } else {
            Log.w(TAG, "LocalInputManager not available for registration");
        }
        
        // Start periodic self-checks to verify service is running
        startPeriodicChecks();
    }
    
    /**
     * Starts the foreground service to keep this accessibility service alive
     */
    private void startForegroundServiceIfNeeded() {
        Intent serviceIntent = new Intent(this, BleHidForegroundService.class);
        serviceIntent.setAction("START_FOREGROUND");
        
        // Android 12+ (API 31+) can directly call startForegroundService
        startForegroundService(serviceIntent);
        
        Log.d(TAG, "Requested foreground service to start");
    }
    
    /**
     * Start periodic checks to verify service is still running correctly
     */
    private void startPeriodicChecks() {
        // Check every 10 seconds that we're still connected
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (instance == null && isConnected) {
                    Log.e(TAG, "Instance reference lost but service still connected");
                    instance = LocalAccessibilityService.this;
                }
                
                Log.d(TAG, "Accessibility service status check: connected=" + isConnected);
                
                // Schedule next check
                handler.postDelayed(this, 10000);
            }
        }, 10000);
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "Accessibility service unbind requested");
        isConnected = false;
        if (instance == this) {
            instance = null;
        }
        return super.onUnbind(intent);
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Accessibility service destroyed");
        isConnected = false;
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
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
        if (instance != null && !instance.isConnected) {
            Log.w(TAG, "Returning instance that is not connected");
        }
        return instance;
    }
    
    /**
     * Check if the service is connected and ready for use
     */
    public boolean isReady() {
        return isConnected;
    }
}
