package com.inventonater.blehid.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

// Removed AndroidX dependency

/**
 * Foreground service that ensures the LocalAccessibilityService remains running.
 * 
 * This service displays a simple persistent notification as required by Android
 * to keep the app running in the background. This ensures our accessibility
 * service doesn't get killed when the app is not in focus.
 */
public class BleHidForegroundService extends Service {
    private static final String CHANNEL_ID = "BleHidForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "BleHidForegroundSvc";
    
    // Service state
    private boolean isRunning = false;
    private Handler handler;
    private NotificationManager notificationManager;
    
    /**
     * Static flag to track if service is running
     */
    private static boolean isServiceRunning = false;
    
    /**
     * Static instance for singleton access
     */
    private static BleHidForegroundService instance;
    
    /**
     * Check if service is running
     */
    public static boolean isRunning() {
        return isServiceRunning;
    }
    
    /**
     * Get the single instance of the service
     */
    public static BleHidForegroundService getInstance() {
        return instance;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        
        try {
            notificationManager = getSystemService(NotificationManager.class);
            createNotificationChannel();
        } catch (Exception e) {
            Log.e(TAG, "ERROR creating notification channel", e);
        }
        
        // Set static running flag and instance
        isServiceRunning = true;
        instance = this;
    }
    
    // Service action constants
    public static final String ACTION_START_FOREGROUND = "START_FOREGROUND";
    public static final String ACTION_STOP_FOREGROUND = "STOP_FOREGROUND";
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "Service action received: " + action);
        
        try {
            if (ACTION_START_FOREGROUND.equals(action)) {
                if (!isRunning) {
                    Log.d(TAG, "Starting foreground service from onStartCommand");
                    startForegroundService();
                    isRunning = true;
                    Log.d(TAG, "Foreground service started");
                } else {
                    Log.d(TAG, "Service already running, not starting again");
                }
            } else if (ACTION_STOP_FOREGROUND.equals(action)) {
                Log.d(TAG, "Stopping foreground service");
                stopForeground(Service.STOP_FOREGROUND_REMOVE);
                stopSelf();
                isRunning = false;
                isServiceRunning = false;
                Log.d(TAG, "Foreground service stopped");
            } else {
                Log.w(TAG, "Unknown action received: " + action);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onStartCommand", e);
        }
        
        return START_STICKY;
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not designed as a bound service
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        isRunning = false;
        isServiceRunning = false;
        
        // Clear the static instance
        if (instance == this) {
            instance = null;
        }
        
        Log.d(TAG, "Foreground service destroyed");
    }
    
    /**
     * Creates the notification channel required for Android 8.0+
     */
    private void createNotificationChannel() {
        // Use IMPORTANCE_LOW for a more subtle notification
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "BleHID Service",
            NotificationManager.IMPORTANCE_LOW
        );
        
        channel.setDescription("Keeps BleHID service running in the background");
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        
        notificationManager.createNotificationChannel(channel);
    }
    
    /**
     * Starts the foreground service with a persistent notification
     */
    private void startForegroundService() {
        try {
            // Create notification
            Notification notification = buildNotification();
            
            try {
                // Handle different Android versions for starting foreground service
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    Log.d(TAG, "Using startForeground with FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE for Android 10+");
                    // On Android 10+, specify the foreground service type
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE);
                } else {
                    // For older Android versions
                    startForeground(NOTIFICATION_ID, notification);
                }
                Log.d(TAG, "Foreground service started successfully");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start foreground service", e);
                
                // Fallback method - just try the basic version
                try {
                    Log.d(TAG, "Trying fallback to basic startForeground");
                    startForeground(NOTIFICATION_ID, notification);
                    Log.d(TAG, "Fallback to basic startForeground succeeded");
                } catch (Exception e2) {
                    Log.e(TAG, "All startForeground attempts failed", e2);
                    throw e2; // Re-throw to be caught by outer try-catch
                }
            }
            
            // Ensure accessibility service is running and monitored
            establishAccessibilityServiceConnection();
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service", e);
        }
    }
    
    /**
     * Builds the persistent notification for the foreground service
     */
    private Notification buildNotification() {
        // Create a pending intent to open the main Unity activity when notification is tapped
        Intent notificationIntent;
        
        // The exact activity class name used by this Unity project
        final String UNITY_ACTIVITY_CLASS = "com.unity3d.player.UnityPlayerGameActivity";
        
        try {
            // Direct approach using the correct class name
            Class<?> activityClass = Class.forName(UNITY_ACTIVITY_CLASS);
            notificationIntent = new Intent(this, activityClass);
        } catch (ClassNotFoundException e) {
            // If we can't load the class directly, fall back to a launcher intent
            notificationIntent = new Intent();
            notificationIntent.setPackage(getPackageName());
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        // Using FLAG_IMMUTABLE as recommended for Android 12+
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        // Simple static notification using standard Android APIs instead of AndroidX
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("BleHID Running")
            .setContentText("Bluetooth HID service is active")
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false);
            
        return builder.build();
    }
    
    /**
     * Sets up periodic monitoring of the accessibility service
     */
    private void establishAccessibilityServiceConnection() {
        // Set up monitoring of the accessibility service
        // Check every 15 seconds that the service is still running
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isRunning) {
                    return; // Stop checking if foreground service is stopped
                }
                
                // Check if accessibility service is running
                LocalAccessibilityService service = LocalAccessibilityService.getInstance();
                if (service == null || !service.isReady()) {
                    Log.w(TAG, "Accessibility service not running, prompting user");
                    promptForAccessibilityService();
                } else {
                    Log.d(TAG, "Accessibility service is running properly");
                }
                
                // Schedule next check
                handler.postDelayed(this, 15000);
            }
        }, 15000);
    }
    
    /**
     * Creates a notification prompting the user to enable the accessibility service
     * if it's not running
     */
    private void promptForAccessibilityService() {
        // Create a notification to direct users to enable the accessibility service
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        
        // Accessibility settings intent
        Intent settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        PendingIntent settingsPendingIntent = PendingIntent.getActivity(this, 0,
                settingsIntent, PendingIntent.FLAG_IMMUTABLE);
        
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Action Required")
            .setContentText("BleHID needs accessibility permission to function properly")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(settingsPendingIntent)
            .setPriority(Notification.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .build();
            
        notificationManager.notify(NOTIFICATION_ID + 1, notification);
    }
}
