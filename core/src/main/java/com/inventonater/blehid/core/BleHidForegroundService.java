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

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

/**
 * Foreground service that ensures the LocalAccessibilityService remains running.
 * 
 * This service displays a persistent notification as required by Android
 * to keep the app running in the background. This ensures our accessibility
 * service doesn't get killed when the app is not in focus.
 */
public class BleHidForegroundService extends Service {
    private static final String CHANNEL_ID = "BleHidForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "BleHidForegroundSvc";
    private static final boolean VERBOSE_LOGGING = true;
    
    // Service state
    private boolean isRunning = false;
    private Handler handler;
    
    /**
     * Static flag to track if service is running
     */
    private static boolean isServiceRunning = false;
    
    /**
     * Check if service is running
     */
    public static boolean isRunning() {
        return isServiceRunning;
    }
    
    @Override
    public void onCreate() {
        Log.e(TAG, "SERVICE ONCREATE CALLED - VERY FIRST LOG"); // This line will help see if the service starts at all
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        
        try {
            Log.d(TAG, "Creating notification channel in onCreate");
            createNotificationChannel();
            Log.d(TAG, "Notification channel created successfully");
        } catch (Exception e) {
            Log.e(TAG, "ERROR creating notification channel", e);
        }
        
        // Set static running flag
        isServiceRunning = true;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand received with startId: " + startId); // Early logging
        
        String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "Service action received: " + action);
        
        try {
            if ("START_FOREGROUND".equals(action)) {
                if (!isRunning) {
                    Log.d(TAG, "Starting foreground service from onStartCommand");
                    startForegroundService();
                    isRunning = true;
                    Log.d(TAG, "Foreground service started");
                } else {
                    Log.d(TAG, "Service already running, not starting again");
                }
            } else if ("STOP_FOREGROUND".equals(action)) {
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
        Log.d(TAG, "Foreground service destroyed");
    }
    
    /**
     * Creates the notification channel required for Android 8.0+
     */
    private void createNotificationChannel() {
        // Use IMPORTANCE_HIGH to ensure the notification is visible
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "BleHID Background Service",
            NotificationManager.IMPORTANCE_HIGH
        );
        
        if (VERBOSE_LOGGING) {
            Log.d(TAG, "Creating notification channel: " + CHANNEL_ID);
        }
        
        channel.setDescription("Keeps BleHID accessibility service running in the background");
        channel.enableLights(false);
        channel.enableVibration(false);
        channel.setShowBadge(false);
        
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }
    
    /**
     * Starts the foreground service with a persistent notification
     */
    private void startForegroundService() {
        if (VERBOSE_LOGGING) {
            Log.d(TAG, "Starting foreground service...");
        }
        
        try {
            // Create notification
            Notification notification = buildNotification();
            
            if (VERBOSE_LOGGING) {
                Log.d(TAG, "Notification built successfully, calling startForeground...");
            }
            
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
        if (VERBOSE_LOGGING) {
            Log.d(TAG, "Building notification...");
        }
        
        // Create a pending intent to open the main Unity activity when notification is tapped
        Intent notificationIntent;
        
        // The exact activity class name used by this Unity project
        final String UNITY_ACTIVITY_CLASS = "com.unity3d.player.UnityPlayerGameActivity";
        
        try {
            // Direct approach using the correct class name
            Log.e(TAG, "Creating notification intent for activity: " + UNITY_ACTIVITY_CLASS);
            Class<?> activityClass = Class.forName(UNITY_ACTIVITY_CLASS);
            notificationIntent = new Intent(this, activityClass);
            Log.e(TAG, "Created intent with explicit activity class");
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "Couldn't find activity class, using launcher intent: " + e.getMessage());
            // If we can't load the class directly, fall back to a launcher intent
            notificationIntent = new Intent();
            notificationIntent.setPackage(getPackageName());
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            Log.e(TAG, "Using launcher intent for package: " + getPackageName());
        }
        
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        // Using FLAG_IMMUTABLE as recommended for Android 12+
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        // Build the notification with enhanced visibility
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BleHID Service Active")
            .setContentText("ACTIVE: Maintaining background connection for accessibility services")
            .setSmallIcon(android.R.drawable.ic_secure) // More attention-grabbing icon
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_MAX) // MAX to ensure it's shown
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Make visible on lock screen
            .setOngoing(true)
            .setAutoCancel(false)
            .setTicker("BleHID service is active") // For older devices
            .setWhen(System.currentTimeMillis());
            
        // Add a color to make it more noticeable
        builder.setColor(getColor(android.R.color.holo_blue_bright));
            
        Notification notification = builder.build();
            
        if (VERBOSE_LOGGING) {
            Log.d(TAG, "Notification built: " + notification);
        }
        
        return notification;
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
        
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Action Required")
            .setContentText("BleHID needs accessibility permission to function properly")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(settingsPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build();
            
        notificationManager.notify(NOTIFICATION_ID + 1, notification);
    }
}
