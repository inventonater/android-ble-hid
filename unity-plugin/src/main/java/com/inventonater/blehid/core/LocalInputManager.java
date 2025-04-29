package com.inventonater.blehid.core;

import android.content.Context;
import android.content.Intent;
import android.content.ActivityNotFoundException;
import android.provider.MediaStore;
import android.util.Log;

public class LocalInputManager {
    private static final String TAG = "LocalInputManager";
    
    private static LocalInputManager instance;
    
    private final Context context;
    private final LocalMediaController mediaController;
    private final LocalInputController inputController;
    
    public static synchronized LocalInputManager getInstance() {
        return instance;
    }
    
    public static synchronized LocalInputManager initialize(Context context) {
        if (instance == null)  instance = new LocalInputManager(context);
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

    public void registerAccessibilityService(LocalAccessibilityService service) {
        inputController.registerAccessibilityService(service);
    }

    public boolean isAccessibilityServiceEnabled() {
        return inputController.isAccessibilityServiceEnabled();
    }

    public void openAccessibilitySettings() {
        inputController.openAccessibilitySettings();
    }

    public boolean playPause() {
        return mediaController.playPause();
    }

    public boolean nextTrack() {
        return mediaController.next();
    }

    public boolean previousTrack() {
        return mediaController.previous();
    }

    public boolean volumeUp() {
        return mediaController.volumeUp();
    }

    public boolean volumeDown() {
        return mediaController.volumeDown();
    }

    public boolean mute() {
        return mediaController.mute();
    }

    public boolean tap(int x, int y) {
        return inputController.tap(x, y);
    }

    public boolean swipeBegin(float startX, float startY) {
        return inputController.swipeBegin(startX, startY);
    }
    public boolean swipeExtend(float deltaX, float deltaY) {
        return inputController.swipeExtend(deltaX, deltaY);
    }
    public boolean swipeEnd() {
        return inputController.swipeEnd();
    }

    public boolean performGlobalAction(int globalAction) {
        return inputController.performGlobalAction(globalAction);
    }

    public boolean performFocusedNodeAction(int action) {
        return inputController.performFocusedNodeAction(action);
    }

    public boolean clickFocusedNode() {
        return inputController.clickFocusedNode();
    }

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

    private boolean isIntentResolvable(Intent intent) {
        return intent.resolveActivity(context.getPackageManager()) != null;
    }

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
}
