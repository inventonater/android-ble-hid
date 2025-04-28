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
    public boolean performGlobalAction(int globalAction) {
        return inputController.performGlobalAction(globalAction);
    }
    
    /**
     * Performs the specified action on the currently focused accessibility node.
     * @param action The accessibility action to perform (e.g., AccessibilityNodeInfo.ACTION_CLICK)
     * @return true if the action was performed successfully, false otherwise
     */
    public boolean performFocusedNodeAction(int action) {
        return inputController.performFocusedNodeAction(action);
    }
    
    /**
     * Clicks on the currently focused accessibility node.
     * @return true if the click was performed successfully, false otherwise
     */
    public boolean clickFocusedNode() {
        return inputController.clickFocusedNode();
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
     *
     * @param options Camera options to configure the capture (use null for defaults)
     * @return true if camera was launched successfully
     */
    public boolean takePictureWithCamera(CameraOptions options) {
        // Use default options if null
        if (options == null) {
            options = new CameraOptions();
        }
        
        // First, launch the camera app
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            context.startActivity(cameraIntent);
            
            // Then start our service to handle the capture
            Intent serviceIntent = new Intent(context, CameraTaskService.class);
            serviceIntent.setAction(CameraTaskService.ACTION_TAKE_PHOTO);
            
            // Add options as extras
            int tapDelayMs = options.getTapDelay();
            int returnDelayMs = options.getReturnDelay();
            float buttonX = options.getButtonX();
            float buttonY = options.getButtonY();
            int acceptDialogDelayMs = options.getAcceptDialogDelay();
            float acceptXOffset = options.getAcceptXOffset();
            float acceptYOffset = options.getAcceptYOffset();
            
            // Add optional camera parameters if provided
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
            
            // Add dialog parameters if provided
            if (acceptDialogDelayMs > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_ACCEPT_DIALOG_DELAY, acceptDialogDelayMs);
            }
            
            if (acceptXOffset > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_ACCEPT_BUTTON_X_OFFSET, acceptXOffset);
            }
            
            if (acceptYOffset > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_ACCEPT_BUTTON_Y_OFFSET, acceptYOffset);
            }
            
            context.startService(serviceIntent);
            
            Log.d(TAG, String.format("Taking picture with params: tapDelay=%d, returnDelay=%d, buttonPos=(%.2f,%.2f), " +
                                    "dialogDelay=%d, acceptPos=(%.2f,%.2f)",
                    tapDelayMs, returnDelayMs, buttonX, buttonY, 
                    acceptDialogDelayMs, acceptXOffset, acceptYOffset));
            
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Camera app not found", e);
            return false;
        }
    }
    
    /**
     * Takes a picture with the camera using default options.
     * 
     * @return true if camera was launched successfully
     */
    public boolean takePictureWithCamera() {
        return takePictureWithCamera(null);
    }

    /**
     * Records a video with the camera by launching the video camera
     * and using a background service to tap the record button.
     *
     * @param options Video options to configure the recording (use null for defaults)
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo(VideoOptions options) {
        // Use default options if null
        if (options == null) {
            options = new VideoOptions();
        }
        
        // First, launch the video camera
        Intent videoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        videoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        try {
            context.startActivity(videoIntent);
            
            // Then start our service to handle recording
            Intent serviceIntent = new Intent(context, CameraTaskService.class);
            serviceIntent.setAction(CameraTaskService.ACTION_RECORD_VIDEO);
            
            // Add options as extras
            long durationMs = options.getDurationMs();
            int tapDelayMs = options.getTapDelay();
            int returnDelayMs = options.getReturnDelay();
            float buttonX = options.getButtonX();
            float buttonY = options.getButtonY();
            int acceptDialogDelayMs = options.getAcceptDialogDelay();
            float acceptXOffset = options.getAcceptXOffset();
            float acceptYOffset = options.getAcceptYOffset();
            
            serviceIntent.putExtra(CameraTaskService.EXTRA_VIDEO_DURATION, durationMs);
            
            // Add optional camera parameters if provided
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
            
            // Add dialog parameters if provided
            if (acceptDialogDelayMs > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_ACCEPT_DIALOG_DELAY, acceptDialogDelayMs);
            }
            
            if (acceptXOffset > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_ACCEPT_BUTTON_X_OFFSET, acceptXOffset);
            }
            
            if (acceptYOffset > 0) {
                serviceIntent.putExtra(CameraTaskService.EXTRA_ACCEPT_BUTTON_Y_OFFSET, acceptYOffset);
            }
            
            context.startService(serviceIntent);
            
            Log.d(TAG, String.format("Recording video for %dms with params: tapDelay=%d, returnDelay=%d, " +
                                    "buttonPos=(%.2f,%.2f), dialogDelay=%d, acceptPos=(%.2f,%.2f)",
                    durationMs, tapDelayMs, returnDelayMs, buttonX, buttonY, 
                    acceptDialogDelayMs, acceptXOffset, acceptYOffset));
            
            return true;
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Camera app not found", e);
            return false;
        }
    }
    
    /**
     * Records a video with default options.
     * 
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo() {
        return recordVideo(null);
    }
    
    /**
     * Records a video with specified duration using default settings.
     * 
     * @param durationMs Duration in milliseconds
     * @return true if video recording was launched successfully
     */
    public boolean recordVideo(long durationMs) {
        VideoOptions options = new VideoOptions();
        options.setDuration(durationMs / 1000f);
        return recordVideo(options);
    }
}
