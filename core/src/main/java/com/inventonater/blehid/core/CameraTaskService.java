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
    private static final int CAMERA_TAP_DELAY_MS = 2000; // Delay before tapping camera button
    private static final int RETURN_TO_APP_DELAY_MS = 1000; // Delay before returning to app
    
    public static final String ACTION_TAKE_PHOTO = "com.inventonater.blehid.TAKE_PHOTO";
    public static final String ACTION_RECORD_VIDEO = "com.inventonater.blehid.RECORD_VIDEO";
    public static final String EXTRA_VIDEO_DURATION = "video_duration_ms";
    
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRecordingVideo = false;
    private long videoDuration = 5000; // Default 5 seconds
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        
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
        
        // Schedule returning to our app
        handler.postDelayed(this::returnToApp, CAMERA_TAP_DELAY_MS + RETURN_TO_APP_DELAY_MS);
    }
    
    private void scheduleVideoCapture() {
        // First tap to start recording
        handler.postDelayed(() -> {
            performCameraButtonTap();
            isRecordingVideo = true;
            Log.d(TAG, "Starting video recording");
        }, CAMERA_TAP_DELAY_MS);
        
        // Second tap to stop recording after duration
        handler.postDelayed(() -> {
            performCameraButtonTap();
            isRecordingVideo = false;
            Log.d(TAG, "Stopping video recording");
            
            // Return to our app after stopping recording
            handler.postDelayed(this::returnToApp, RETURN_TO_APP_DELAY_MS);
        }, CAMERA_TAP_DELAY_MS + videoDuration);
    }
    
    private void performCameraButtonTap() {
        // Get display metrics to find center-bottom of screen
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int x = metrics.widthPixels / 2;
        int y = (int)(metrics.heightPixels * 0.85); // Camera button usually near bottom
        
        // Use accessibility service to perform the tap
        boolean success = LocalInputController.performGlobalTap(x, y);
        Log.d(TAG, "Performing tap at " + x + "," + y + " (success: " + success + ")");
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
