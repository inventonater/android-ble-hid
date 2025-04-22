package com.inventonater.blehid.core;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Controls input on the local device using Accessibility Services.
 */
public class LocalInputController {
    private static final String TAG = "LocalInputController";
    
    private final Context context;
    private LocalAccessibilityService accessibilityService;
    
    public LocalInputController(Context context) {
        this.context = context;
    }
    
    /**
     * Registers the accessibility service.
     */
    public void registerAccessibilityService(LocalAccessibilityService service) {
        this.accessibilityService = service;
        Log.d(TAG, "Accessibility service registered");
    }
    
    /**
     * Checks if accessibility service is enabled and registered.
     */
    public boolean isAccessibilityServiceAvailable() {
        if (accessibilityService == null) {
            Log.e(TAG, "Accessibility service not registered");
            return false;
        }
        return true;
    }
    
    /**
     * Checks if the accessibility service is enabled in settings.
     */
    public boolean isAccessibilityServiceEnabled() {
        String serviceName = context.getPackageName() + "/" + 
                             LocalAccessibilityService.class.getCanonicalName();
        
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        return enabledServices != null && enabledServices.contains(serviceName);
    }
    
    /**
     * Opens accessibility settings to enable the service.
     */
    public void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    /**
     * Performs a tap at the specified coordinates.
     */
    public boolean tap(int x, int y) {
        if (!isAccessibilityServiceAvailable()) return false;
        
        Path path = new Path();
        path.moveTo(x, y);
        
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};
        
        accessibilityService.dispatchGesture(builder.build(), 
            new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    result[0] = true;
                    latch.countDown();
                }
                
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    result[0] = false;
                    latch.countDown();
                }
            }, null);
        
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Tap gesture interrupted", e);
            return false;
        }
        
        return result[0];
    }
    
    /**
     * Performs a global tap using the accessibility service, even when app is not in foreground.
     * Used by the camera service to tap the camera button while the camera app is open.
     */
    public static boolean performGlobalTap(int x, int y) {
        LocalAccessibilityService service = LocalAccessibilityService.getInstance();
        if (service == null) {
            Log.e(TAG, "Accessibility service not running for global tap");
            return false;
        }
        
        // Verify service is fully connected and ready
        if (!service.isReady()) {
            Log.e(TAG, "Accessibility service is not ready/connected");
            return false;
        }
        
        Log.i(TAG, "Attempting global tap at (" + x + ", " + y + ")");
        
        Path path = new Path();
        path.moveTo(x, y);
        
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 150)); // longer duration for more reliable tap
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};
        
        try {
            service.dispatchGesture(builder.build(), 
                new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        result[0] = true;
                        Log.d(TAG, "Global tap completed successfully");
                        latch.countDown();
                    }
                    
                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        result[0] = false;
                        Log.w(TAG, "Global tap was cancelled");
                        latch.countDown();
                    }
                }, null);
            
            // Wait for the tap to complete with a longer timeout
            if (!latch.await(1500, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "Global tap timed out waiting for completion");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during global tap: " + e.getMessage(), e);
            return false;
        }
        
        return result[0];
    }
    
    /**
     * Performs a swipe from (x1, y1) to (x2, y2).
     */
    public boolean swipe(int x1, int y1, int x2, int y2) {
        if (!isAccessibilityServiceAvailable()) return false;
        
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 300));
        
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};
        
        accessibilityService.dispatchGesture(builder.build(), 
            new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    result[0] = true;
                    latch.countDown();
                }
                
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    result[0] = false;
                    latch.countDown();
                }
            }, null);
        
        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Swipe gesture interrupted", e);
            return false;
        }
        
        return result[0];
    }
    
    /**
     * Sends a key event to the system.
     */
    public boolean sendKey(int keyCode) {
        if (!isAccessibilityServiceAvailable()) return false;
        
        boolean result = accessibilityService.performGlobalAction(keyCode);
        Log.d(TAG, "Key event sent: " + keyCode + ", result: " + result);
        return result;
    }
    
    /**
     * Navigation key constants
     */
    public static final int NAV_UP = 1;       // Custom action code
    public static final int NAV_DOWN = 2;     // Custom action code
    public static final int NAV_LEFT = 3;     // Custom action code
    public static final int NAV_RIGHT = 4;    // Custom action code
    public static final int NAV_BACK = AccessibilityService.GLOBAL_ACTION_BACK;
    public static final int NAV_HOME = AccessibilityService.GLOBAL_ACTION_HOME;
    public static final int NAV_RECENTS = AccessibilityService.GLOBAL_ACTION_RECENTS;
}
