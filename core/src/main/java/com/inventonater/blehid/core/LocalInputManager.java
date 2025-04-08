package com.inventonater.blehid.core;

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Manager class for local input control.
 * Handles both media control and input (mouse/keyboard) control.
 */
public class LocalInputManager {
    private static final String TAG = "LocalInputManager";
    
    private static LocalInputManager instance;
    
    private final Context context;
    private final LocalMediaController mediaController;
    private final LocalInputController inputController;
    
    /**
     * Gets the singleton instance of the manager.
     */
    public static synchronized LocalInputManager getInstance() {
        return instance;
    }
    
    /**
     * Initializes the singleton instance.
     */
    public static synchronized LocalInputManager initialize(Context context) {
        if (instance == null) {
            instance = new LocalInputManager(context);
        }
        return instance;
    }
    
    private LocalInputManager(Context context) {
        this.context = context.getApplicationContext();
        this.mediaController = new LocalMediaController(this.context);
        this.inputController = new LocalInputController(this.context);
        
        // Register with accessibility service if already running
        LocalAccessibilityService service = LocalAccessibilityService.getInstance();
        if (service != null) {
            inputController.registerAccessibilityService(service);
        }
    }
    
    /**
     * Registers the accessibility service.
     */
    public void registerAccessibilityService(LocalAccessibilityService service) {
        inputController.registerAccessibilityService(service);
    }
    
    /**
     * Checks if accessibility service is enabled.
     */
    public boolean isAccessibilityServiceEnabled() {
        return inputController.isAccessibilityServiceEnabled();
    }
    
    /**
     * Opens accessibility settings to enable the service.
     */
    public void openAccessibilitySettings() {
        inputController.openAccessibilitySettings();
    }
    
    /**
     * Media playback control: Play/Pause
     */
    public boolean playPause() {
        return mediaController.playPause();
    }
    
    /**
     * Media playback control: Next track
     */
    public boolean nextTrack() {
        return mediaController.next();
    }
    
    /**
     * Media playback control: Previous track
     */
    public boolean previousTrack() {
        return mediaController.previous();
    }
    
    /**
     * Media playback control: Volume up
     */
    public boolean volumeUp() {
        return mediaController.volumeUp();
    }
    
    /**
     * Media playback control: Volume down
     */
    public boolean volumeDown() {
        return mediaController.volumeDown();
    }
    
    /**
     * Media playback control: Mute
     */
    public boolean mute() {
        return mediaController.mute();
    }
    
    /**
     * Input control: Tap at coordinates
     */
    public boolean tap(int x, int y) {
        return inputController.tap(x, y);
    }
    
    /**
     * Input control: Swipe from point to point
     */
    public boolean swipe(int x1, int y1, int x2, int y2) {
        return inputController.swipe(x1, y1, x2, y2);
    }
    
    /**
     * Input control: Directional navigation
     */
    public boolean navigate(int direction) {
        return inputController.sendKey(direction);
    }
    
    /**
     * Launch camera app directly.
     * This opens the default camera app without taking a picture.
     */
    public boolean launchCameraApp() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setPackage("com.android.camera");  // This might not work on all devices
        
        // If specific package approach doesn't work, try alternate approach
        if (!isIntentResolvable(intent)) {
            // Alternative approach: use the camera capture intent but don't handle the result
            intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        }
        
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            Log.d(TAG, "Launched camera app");
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No camera app found", e);
            return false;
        }
    }
    
    /**
     * Check if an intent can be resolved by the system
     */
    private boolean isIntentResolvable(Intent intent) {
        return intent.resolveActivity(context.getPackageManager()) != null;
    }
    
    /**
     * Launch photo capture intent.
     * This opens the camera in photo mode and returns the result to the calling app.
     */
    public boolean launchPhotoCapture() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            Log.d(TAG, "Launched photo capture intent");
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No camera app found for photo capture", e);
            return false;
        }
    }
    
    /**
     * Launch video capture intent.
     * This opens the camera in video mode and returns the result to the calling app.
     */
    public boolean launchVideoCapture() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            Log.d(TAG, "Launched video capture intent");
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "No camera app found for video capture", e);
            return false;
        }
    }
    
    /**
     * Takes a picture with the camera by launching the camera app
     * and using a background service to tap the shutter button.
     */
    public boolean takePictureWithCamera() {
        return takePictureWithCamera(0, 0, 0, 0);
    }
    
    /**
     * Takes a picture with the camera using configurable parameters.
     * 
     * @param tapDelayMs Delay in ms before tapping the shutter button (0 = use default)
     * @param returnDelayMs Delay in ms before returning to app (0 = use default)
     * @param buttonX X position of shutter button as a ratio (0.0-1.0, 0 = use default)
     * @param buttonY Y position of shutter button as a ratio (0.0-1.0, 0 = use default)
     * @return true if camera was launched successfully
     */
    public boolean takePictureWithCamera(int tapDelayMs, int returnDelayMs, float buttonX, float buttonY) {
        // First, launch the camera app
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            context.startActivity(cameraIntent);
            
            // Then start our service to handle the capture
            Intent serviceIntent = new Intent(context, CameraTaskService.class);
            serviceIntent.setAction(CameraTaskService.ACTION_TAKE_PHOTO);
            
            // Add optional parameters if provided
            if (tapDelayMs > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_TAP_DELAY, tapDelayMs);
            }
            
            if (returnDelayMs > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_RETURN_DELAY, returnDelayMs);
            }
            
            if (buttonX > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_BUTTON_X, buttonX);
            }
            
            if (buttonY > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_BUTTON_Y, buttonY);
            }
            
            context.startService(serviceIntent);
            Log.d(TAG, "Taking picture with parameters: tapDelay=" + tapDelayMs + 
                  ", returnDelay=" + returnDelayMs + ", buttonPos=(" + buttonX + "," + buttonY + ")");
            
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Camera app not found", e);
            return false;
        }
    }

    /**
     * Records a video using the camera by launching the video camera
     * and using a background service to tap the record button.
     */
    public boolean recordVideo(long durationMs) {
        return recordVideo(durationMs, 0, 0, 0, 0);
    }
    
    /**
     * Records a video with configurable parameters.
     * 
     * @param durationMs Duration of the recording in milliseconds
     * @param tapDelayMs Delay in ms before tapping the record button (0 = use default)
     * @param returnDelayMs Delay in ms before returning to app (0 = use default)
     * @param buttonX X position of record button as a ratio (0.0-1.0, 0 = use default)
     * @param buttonY Y position of record button as a ratio (0.0-1.0, 0 = use default)
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo(long durationMs, int tapDelayMs, int returnDelayMs, float buttonX, float buttonY) {
        // First, launch the video camera
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            context.startActivity(videoIntent);
            
            // Then start our service to handle recording
            Intent serviceIntent = new Intent(context, CameraTaskService.class);
            serviceIntent.setAction(CameraTaskService.ACTION_RECORD_VIDEO);
            serviceIntent.putExtra(CameraTaskService.EXTRA_VIDEO_DURATION, durationMs);
            
            // Add optional parameters if provided
            if (tapDelayMs > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_TAP_DELAY, tapDelayMs);
            }
            
            if (returnDelayMs > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_RETURN_DELAY, returnDelayMs);
            }
            
            if (buttonX > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_BUTTON_X, buttonX);
            }
            
            if (buttonY > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_BUTTON_Y, buttonY);
            }
            
            context.startService(serviceIntent);
            Log.d(TAG, "Recording video for " + durationMs + "ms with parameters: tapDelay=" + 
                  tapDelayMs + ", returnDelay=" + returnDelayMs + ", buttonPos=(" + buttonX + "," + buttonY + ")");
            
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Camera app not found", e);
            return false;
        }
    }
    
    /**
     * Adds navigation key constants for easier access
     */
    public static final int NAV_UP = LocalInputController.NAV_UP;
    public static final int NAV_DOWN = LocalInputController.NAV_DOWN;
    public static final int NAV_LEFT = LocalInputController.NAV_LEFT;
    public static final int NAV_RIGHT = LocalInputController.NAV_RIGHT;
    public static final int NAV_BACK = LocalInputController.NAV_BACK;
    public static final int NAV_HOME = LocalInputController.NAV_HOME;
    public static final int NAV_RECENTS = LocalInputController.NAV_RECENTS;
}
