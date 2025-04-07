package com.inventonater.blehid.core;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Service for controlling the local device using Accessibility Services.
 * Provides methods to simulate keyboard, mouse, and system actions.
 */
public class AccessibilityControlService {
    private static final String TAG = "AccessibilityControl";
    
    private final Context context;
    private AccessibilityService accessibilityService;
    private boolean isInitialized = false;
    
    /**
     * Creates a new AccessibilityControlService instance.
     * 
     * @param context The application context
     */
    public AccessibilityControlService(Context context) {
        this.context = context;
    }
    
    /**
     * Sets the accessibility service instance.
     * This must be called from the actual AccessibilityService implementation.
     * 
     * @param service The accessibility service instance
     */
    public void setAccessibilityService(AccessibilityService service) {
        this.accessibilityService = service;
        if (service != null) {
            isInitialized = true;
            Log.i(TAG, "Accessibility service set successfully");
        } else {
            isInitialized = false;
            Log.w(TAG, "Accessibility service set to null");
        }
    }
    
    /**
     * Initializes the accessibility control service.
     * Note: Full initialization can only happen when the accessibility service is running.
     * 
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        if (isInitialized) {
            Log.d(TAG, "Accessibility control service already initialized");
            return true;
        }
        
        // We can't fully initialize without the service running,
        // but we can check if accessibility is enabled
        boolean isEnabled = isAccessibilityServiceEnabled();
        Log.i(TAG, "Accessibility service enabled: " + isEnabled);
        
        // Mark as partially initialized
        return isEnabled;
    }
    
    /**
     * Checks if the app's accessibility service is enabled in system settings.
     * 
     * @return true if accessibility service is enabled, false otherwise
     */
    public boolean isAccessibilityServiceEnabled() {
        String expectedServiceName = context.getPackageName() + "/com.inventonater.blehid.unity.BleHidAccessibilityService";
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(), 
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        
        return enabledServices != null && enabledServices.contains(expectedServiceName);
    }
    
    /**
     * Opens accessibility settings to allow the user to enable the service.
     */
    public void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    /**
     * Simulates a key press on the device.
     * 
     * @param keyCode The Android key code to send
     * @return true if the key was sent successfully, false otherwise
     */
    public boolean sendKeyEvent(int keyCode) {
        if (!isInitialized || accessibilityService == null) {
            Log.e(TAG, "Accessibility service not initialized");
            return false;
        }
        
        try {
            // Handle special keys we can map to actions or gestures
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    // Use gestures since there's no direct scroll action
                    return scroll("up");
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return scroll("down");
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return scroll("left");
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return scroll("right");
                case KeyEvent.KEYCODE_BACK:
                    return performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                case KeyEvent.KEYCODE_HOME:
                    return performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
                case KeyEvent.KEYCODE_APP_SWITCH:
                    return performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
                default:
                    // For other keys, simulate with click
                    Log.d(TAG, "Simulating key press for keyCode: " + keyCode);
                    return click(); // As a fallback, just click
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending key event", e);
            return false;
        }
    }
    
    /**
     * Simulates a directional key press (Up, Down, Left, Right).
     * 
     * @param direction One of KeyEvent.KEYCODE_DPAD_UP, KEYCODE_DPAD_DOWN, KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT
     * @return true if the key was sent successfully, false otherwise
     */
    public boolean sendDirectionalKey(int direction) {
        if (!isInitialized || accessibilityService == null) {
            Log.e(TAG, "Accessibility service not initialized");
            return false;
        }
        
        // Validate direction key code
        if (direction != KeyEvent.KEYCODE_DPAD_UP && 
            direction != KeyEvent.KEYCODE_DPAD_DOWN && 
            direction != KeyEvent.KEYCODE_DPAD_LEFT && 
            direction != KeyEvent.KEYCODE_DPAD_RIGHT) {
            Log.e(TAG, "Invalid direction key code: " + direction);
            return false;
        }
        
        return sendKeyEvent(direction);
    }
    
    /**
     * Simulates a mouse click at the current pointer location.
     * 
     * @return true if the click was performed successfully, false otherwise
     */
    public boolean click() {
        if (!isInitialized || accessibilityService == null) {
            Log.e(TAG, "Accessibility service not initialized");
            return false;
        }
        
        // Best we can do is simulate a tap at the center of the screen
        // without knowing the current pointer position
        return tapScreen(0.5f, 0.5f);
    }
    
    /**
     * Simulates a tap on the screen at the specified relative position.
     * 
     * @param relativeX The X position as a fraction of screen width (0-1)
     * @param relativeY The Y position as a fraction of screen height (0-1)
     * @return true if the tap was performed successfully, false otherwise
     */
    public boolean tapScreen(float relativeX, float relativeY) {
        if (!isInitialized || accessibilityService == null) {
            Log.e(TAG, "Accessibility service not initialized");
            return false;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Gesture API not available on this Android version");
            return false;
        }
        
        try {
            // Get screen dimensions
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            
            // Calculate absolute coordinates
            float x = screenWidth * relativeX;
            float y = screenHeight * relativeY;
            
            // Create gesture
            Path clickPath = new Path();
            clickPath.moveTo(x, y);
            
            GestureDescription.StrokeDescription stroke = 
                    new GestureDescription.StrokeDescription(clickPath, 0, 100);
            
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);
            
            // Dispatch gesture
            return accessibilityService.dispatchGesture(builder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error performing tap gesture", e);
            return false;
        }
    }
    
    /**
     * Simulates a swipe gesture on the screen.
     * 
     * @param startX Starting X position as a fraction of screen width (0-1)
     * @param startY Starting Y position as a fraction of screen height (0-1)
     * @param endX Ending X position as a fraction of screen width (0-1)
     * @param endY Ending Y position as a fraction of screen height (0-1)
     * @param duration Duration of the swipe in milliseconds
     * @return true if the swipe was performed successfully, false otherwise
     */
    public boolean swipeScreen(float startX, float startY, float endX, float endY, long duration) {
        if (!isInitialized || accessibilityService == null) {
            Log.e(TAG, "Accessibility service not initialized");
            return false;
        }
        
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.e(TAG, "Gesture API not available on this Android version");
            return false;
        }
        
        try {
            // Get screen dimensions
            int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
            int screenHeight = context.getResources().getDisplayMetrics().heightPixels;
            
            // Calculate absolute coordinates
            float x1 = screenWidth * startX;
            float y1 = screenHeight * startY;
            float x2 = screenWidth * endX;
            float y2 = screenHeight * endY;
            
            // Create swipe path
            Path swipePath = new Path();
            swipePath.moveTo(x1, y1);
            swipePath.lineTo(x2, y2);
            
            GestureDescription.StrokeDescription stroke = 
                    new GestureDescription.StrokeDescription(swipePath, 0, duration);
            
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.addStroke(stroke);
            
            // Dispatch gesture
            return accessibilityService.dispatchGesture(builder.build(), null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error performing swipe gesture", e);
            return false;
        }
    }
    
    /**
     * Simulates scrolling in the specified direction.
     * 
     * @param direction "up", "down", "left", or "right"
     * @return true if the scroll was performed successfully, false otherwise
     */
    public boolean scroll(String direction) {
        if (!isInitialized || accessibilityService == null) {
            Log.e(TAG, "Accessibility service not initialized");
            return false;
        }
        
        float startX = 0.5f;
        float startY = 0.5f;
        float endX = 0.5f;
        float endY = 0.5f;
        
        switch (direction.toLowerCase()) {
            case "up":
                startY = 0.6f;
                endY = 0.4f;
                break;
            case "down":
                startY = 0.4f;
                endY = 0.6f;
                break;
            case "left":
                startX = 0.6f;
                endX = 0.4f;
                break;
            case "right":
                startX = 0.4f;
                endX = 0.6f;
                break;
            default:
                Log.e(TAG, "Invalid scroll direction: " + direction);
                return false;
        }
        
        return swipeScreen(startX, startY, endX, endY, 300);
    }
    
    /**
     * Performs a system global action.
     * 
     * @param action One of AccessibilityService.GLOBAL_ACTION_* constants
     * @return true if the action was performed successfully, false otherwise
     */
    public boolean performGlobalAction(int action) {
        if (!isInitialized || accessibilityService == null) {
            Log.e(TAG, "Accessibility service not initialized");
            return false;
        }
        
        try {
            return accessibilityService.performGlobalAction(action);
        } catch (Exception e) {
            Log.e(TAG, "Error performing global action: " + action, e);
            return false;
        }
    }
    
    /**
     * Performs the Home button action.
     * 
     * @return true if the action was performed successfully, false otherwise
     */
    public boolean goHome() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }
    
    /**
     * Performs the Back button action.
     * 
     * @return true if the action was performed successfully, false otherwise
     */
    public boolean goBack() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
    }
    
    /**
     * Performs the Recent Apps button action.
     * 
     * @return true if the action was performed successfully, false otherwise
     */
    public boolean showRecentApps() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS);
    }
    
    /**
     * Performs the notification panel open action.
     * 
     * @return true if the action was performed successfully, false otherwise
     */
    public boolean openNotifications() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS);
    }
    
    /**
     * Performs the quick settings panel open action.
     * 
     * @return true if the action was performed successfully, false otherwise
     */
    public boolean openQuickSettings() {
        return performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS);
    }
    
    /**
     * Cleans up resources.
     */
    public void close() {
        accessibilityService = null;
        isInitialized = false;
        Log.i(TAG, "Accessibility control service closed");
    }
}
