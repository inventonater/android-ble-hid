package com.inventonater.blehid.core;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;

/**
 * Background service that performs camera interactions after the camera app has been launched.
 * This service runs independently of the main activity and can execute tasks even when
 * the Unity activity is paused.
 */
public class CameraTaskService extends Service {
    private static final String TAG = "CameraTaskService";
    private static int CAMERA_TAP_DELAY_MS = 3500; // Delay before tapping camera button, configurable
    private static int RETURN_TO_APP_DELAY_MS = 1500; // Delay before returning to app
    private static float CAMERA_BUTTON_X_POSITION = 0.5f; // Horizontal position (0.5 = center)
    private static float CAMERA_BUTTON_Y_POSITION = 0.92f; // Vertical position (0.92 = 92% down the screen)
    
    // Dialog handling parameters
    private static int ACCEPT_DIALOG_DELAY_MS = 300; // Delay before tapping accept dialog
    private static float ACCEPT_BUTTON_X_OFFSET = 0.2f; // Offset to the right (0.2 = 20% of screen width)
    private static float ACCEPT_BUTTON_Y_OFFSET = 0.05f; // Offset down (5% of screen height)
    
    public static final String ACTION_TAKE_PHOTO = "com.inventonater.blehid.TAKE_PHOTO";
    public static final String ACTION_RECORD_VIDEO = "com.inventonater.blehid.RECORD_VIDEO";
    public static final String EXTRA_VIDEO_DURATION = "video_duration_ms";
    public static final String EXTRA_TAP_DELAY = "tap_delay_ms";
    public static final String EXTRA_RETURN_DELAY = "return_delay_ms";
    public static final String EXTRA_BUTTON_X = "button_x_position";
    public static final String EXTRA_BUTTON_Y = "button_y_position";
    
    // Dialog handling extras
    public static final String EXTRA_ACCEPT_DIALOG_DELAY = "accept_dialog_delay_ms";
    public static final String EXTRA_ACCEPT_BUTTON_X_OFFSET = "accept_button_x_offset";
    public static final String EXTRA_ACCEPT_BUTTON_Y_OFFSET = "accept_button_y_offset";
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRecordingVideo = false;
    private long videoDuration = 5000; // Default 5 seconds
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        
        // Get configurable parameters if provided
        if (intent.hasExtra(EXTRA_TAP_DELAY)) {
            CAMERA_TAP_DELAY_MS = intent.getIntExtra(EXTRA_TAP_DELAY, 3500);
        }
        
        if (intent.hasExtra(EXTRA_RETURN_DELAY)) {
            RETURN_TO_APP_DELAY_MS = intent.getIntExtra(EXTRA_RETURN_DELAY, 1500);
        }
        
        if (intent.hasExtra(EXTRA_BUTTON_X)) {
            CAMERA_BUTTON_X_POSITION = intent.getFloatExtra(EXTRA_BUTTON_X, 0.5f);
        }
        
        if (intent.hasExtra(EXTRA_BUTTON_Y)) {
            CAMERA_BUTTON_Y_POSITION = intent.getFloatExtra(EXTRA_BUTTON_Y, 0.92f);
        }
        
        // Get dialog handling parameters if provided
        if (intent.hasExtra(EXTRA_ACCEPT_DIALOG_DELAY)) {
            ACCEPT_DIALOG_DELAY_MS = intent.getIntExtra(EXTRA_ACCEPT_DIALOG_DELAY, 300);
        }
        
        if (intent.hasExtra(EXTRA_ACCEPT_BUTTON_X_OFFSET)) {
            ACCEPT_BUTTON_X_OFFSET = intent.getFloatExtra(EXTRA_ACCEPT_BUTTON_X_OFFSET, 0.2f);
        }
        
        if (intent.hasExtra(EXTRA_ACCEPT_BUTTON_Y_OFFSET)) {
            ACCEPT_BUTTON_Y_OFFSET = intent.getFloatExtra(EXTRA_ACCEPT_BUTTON_Y_OFFSET, 0.05f);
        }
        
        Log.d(TAG, "Using camera parameters: tap_delay=" + CAMERA_TAP_DELAY_MS + 
                  ", return_delay=" + RETURN_TO_APP_DELAY_MS + 
                  ", button_x=" + CAMERA_BUTTON_X_POSITION + 
                  ", button_y=" + CAMERA_BUTTON_Y_POSITION);
        
        Log.d(TAG, "Using dialog parameters: dialog_delay=" + ACCEPT_DIALOG_DELAY_MS + 
                  ", x_offset=" + ACCEPT_BUTTON_X_OFFSET + 
                  ", y_offset=" + ACCEPT_BUTTON_Y_OFFSET);
        
        String action = intent.getAction();
        if (ACTION_TAKE_PHOTO.equals(action)) {
            Log.d(TAG, "Starting photo capture task");
            schedulePhotoCapture();
        } else if (ACTION_RECORD_VIDEO.equals(action)) {
            videoDuration = intent.getLongExtra(EXTRA_VIDEO_DURATION, 5000);
            Log.d(TAG, "Starting video recording task for " + videoDuration + "ms");
            scheduleVideoCapture();
        }
        
        return START_NOT_STICKY;
    }
    
    private void schedulePhotoCapture() {
        // Schedule the tap after a delay to allow camera to open
        handler.postDelayed(this::performCameraButtonTap, CAMERA_TAP_DELAY_MS);
        
        // Schedule tap for the accept dialog
        handler.postDelayed(this::tapAcceptDialog, CAMERA_TAP_DELAY_MS + ACCEPT_DIALOG_DELAY_MS);
        
        // Schedule returning to our app
        handler.postDelayed(this::returnToApp, CAMERA_TAP_DELAY_MS + ACCEPT_DIALOG_DELAY_MS + RETURN_TO_APP_DELAY_MS);
    }
    
    private void scheduleVideoCapture() {
        // First tap to start recording
        handler.postDelayed(() -> {
            performCameraButtonTap();
            // Schedule tap for the accept dialog right after camera button tap
            handler.postDelayed(this::tapAcceptDialog, ACCEPT_DIALOG_DELAY_MS);
            isRecordingVideo = true;
            Log.d(TAG, "Starting video recording");
        }, CAMERA_TAP_DELAY_MS);
        
        // Second tap to stop recording after duration
        handler.postDelayed(() -> {
            performCameraButtonTap();
            // Schedule another accept dialog tap after stopping
            handler.postDelayed(this::tapAcceptDialog, ACCEPT_DIALOG_DELAY_MS);
            isRecordingVideo = false;
            Log.d(TAG, "Stopping video recording");
            
            // Return to our app after stopping recording
            handler.postDelayed(this::returnToApp, ACCEPT_DIALOG_DELAY_MS + RETURN_TO_APP_DELAY_MS);
        }, CAMERA_TAP_DELAY_MS + videoDuration);
    }
    
    /**
     * Taps the accept/ok button in the confirmation dialog.
     * This method taps to the right and slightly down from center of the screen.
     */
    private void tapAcceptDialog() {
        // Get display metrics for screen dimensions
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        
        // Calculate position for the accept button - right side of screen, slightly down from center
        int centerX = metrics.widthPixels / 2;
        int centerY = metrics.heightPixels / 2;
        
        // Apply offsets
        int x = centerX + (int)(metrics.widthPixels * ACCEPT_BUTTON_X_OFFSET);
        int y = centerY + (int)(metrics.heightPixels * ACCEPT_BUTTON_Y_OFFSET);
        
        Log.d(TAG, "Tapping accept dialog at " + x + "," + y);
        
        // Perform the tap at the calculated position
        boolean success = LocalInputController.performGlobalTap(x, y);
        
        if (success) {
            Log.d(TAG, "Accept dialog tap succeeded");
        } else {
            Log.e(TAG, "Accept dialog tap failed or timed out");
        }
    }
    
    private void performCameraButtonTap() {
        // Get display metrics for screen dimensions
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        
        // Calculate actual x,y coordinates based on screen size and configured position
        int x = (int)(metrics.widthPixels * CAMERA_BUTTON_X_POSITION);
        int y = (int)(metrics.heightPixels * CAMERA_BUTTON_Y_POSITION);
        
        Log.d(TAG, "Screen size: " + metrics.widthPixels + "x" + metrics.heightPixels);
        Log.d(TAG, "Performing camera button tap at " + x + "," + y + 
              " (position " + CAMERA_BUTTON_X_POSITION + "," + CAMERA_BUTTON_Y_POSITION + ")");
        
        // Perform the tap at the configured position
        boolean success = LocalInputController.performGlobalTap(x, y);
        
        if (success) {
            Log.d(TAG, "Camera button tap succeeded");
        } else {
            Log.e(TAG, "Camera button tap failed or timed out");
        }
    }
    
    private void returnToApp() {
        // Launch our main activity
        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launchIntent);
            Log.d(TAG, "Returning to app");
        } else {
            Log.e(TAG, "Could not find launch intent for our package");
        }
        
        // Stop the service once done
        stopSelf();
        Log.d(TAG, "Camera task service stopped");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not used for binding
    }
}
