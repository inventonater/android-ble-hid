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
    private static final String CHANNEL_NAME = "BleHID Background Service";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "BleHidForegroundSvc";
    private static final boolean VERBOSE_LOGGING = true;
    private static final String ACTION_DIRECT_NOTIFICATION = "SEND_DIRECT_NOTIFICATION";
    
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
        Log.e(TAG, "========== FOREGROUND SERVICE ONCREATE TRIGGERED ==========");
        Log.e(TAG, "Thread: " + Thread.currentThread().getName());
        Log.e(TAG, "Android version: " + android.os.Build.VERSION.SDK_INT);
        Log.e(TAG, "Package: " + getPackageName());
        
        // Standard initialization
        super.onCreate();
        
        // Detailed logging for all steps
        try {
            Log.e(TAG, "Creating handler on main looper");
            handler = new Handler(Looper.getMainLooper());
            Log.e(TAG, "Handler created successfully");
            
            Log.e(TAG, "Creating notification channel");
            createNotificationChannel();
            Log.e(TAG, "Notification channel creation completed");
            
            // Set static running flag
            isServiceRunning = true;
            Log.e(TAG, "Service running flag set to TRUE");
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR in onCreate", e);
            // Try to recover if possible
            try {
                if (handler == null) {
                    handler = new Handler(Looper.getMainLooper());
                }
            } catch (Exception e2) {
                Log.e(TAG, "Failed to recover from onCreate error", e2);
            }
        } finally {
            Log.e(TAG, "========== FOREGROUND SERVICE ONCREATE COMPLETED ==========");
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "========== FOREGROUND SERVICE ONSTARTCOMMAND TRIGGERED ==========");
        Log.e(TAG, "startId: " + startId);
        Log.e(TAG, "Thread: " + Thread.currentThread().getName());
        
        String action = intent != null ? intent.getAction() : null;
        Log.e(TAG, "Intent action: " + action);
        
        if (intent != null) {
            Log.e(TAG, "Intent extras: " + (intent.getExtras() != null ? intent.getExtras().toString() : "null"));
            Log.e(TAG, "Intent component: " + intent.getComponent());
        }
        
        try {
            if ("START_FOREGROUND".equals(action)) {
                // IMPORTANT: We always start the foreground service regardless of isRunning flag
                // This ensures the notification is always displayed
                Log.e(TAG, "Starting foreground service (always starting regardless of isRunning=" + isRunning + ")");
                startForegroundService();
                isRunning = true;
                Log.e(TAG, "Foreground service started/refreshed successfully");
            } else if ("STOP_FOREGROUND".equals(action)) {
                Log.e(TAG, "Stopping foreground service");
                stopForeground(Service.STOP_FOREGROUND_REMOVE);
                stopSelf();
                isRunning = false;
                isServiceRunning = false;
                Log.e(TAG, "Foreground service stopped");
            } else {
                Log.e(TAG, "Unknown action received: " + action);
                
                // If no specific action, still try to start foreground service
                // This helps handle cases where the service is restarted by the system
                if (!isRunning) {
                    Log.e(TAG, "No specific action but starting service anyway");
                    startForegroundService();
                    isRunning = true;
                    Log.e(TAG, "Foreground service started with default behavior");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR in onStartCommand", e);
            Log.e(TAG, "Exception type: " + e.getClass().getName());
            Log.e(TAG, "Exception cause: " + (e.getCause() != null ? e.getCause().toString() : "null"));
        } finally {
            Log.e(TAG, "========== FOREGROUND SERVICE ONSTARTCOMMAND COMPLETED ==========");
        }
        
        // Ensure service is recreated if killed
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
     * With enhanced visibility and verification
     */
    private NotificationChannel createNotificationChannel() {
        Log.e(TAG, "Creating notification channel with IMPORTANCE_HIGH: " + CHANNEL_ID);
        
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        
        // First check if channel already exists
        NotificationChannel existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (existingChannel != null) {
            Log.e(TAG, "Channel already exists - importance: " + existingChannel.getImportance());
            
            // Check if importance needs upgrading
            if (existingChannel.getImportance() < NotificationManager.IMPORTANCE_HIGH) {
                Log.e(TAG, "Channel exists but with low importance - deleting and recreating");
                notificationManager.deleteNotificationChannel(CHANNEL_ID);
                existingChannel = null;
            } else {
                return existingChannel;
            }
        }
        
        // Create channel with maximum visibility
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        );
        
        Log.e(TAG, "Configuring channel with maximum visibility settings");
        
        // Configure for maximum visibility
        channel.setDescription("CRITICAL: Keeps BleHID accessibility service running in background");
        channel.enableLights(true);
        channel.setLightColor(android.graphics.Color.RED);
        channel.enableVibration(true);
        channel.setVibrationPattern(new long[]{0, 250, 250, 250});
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setShowBadge(true);
        channel.setBypassDnd(true); // Try to bypass Do Not Disturb
        
        // Create the channel
        notificationManager.createNotificationChannel(channel);
        
        // Verify the channel was created
        NotificationChannel verifyChannel = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (verifyChannel != null) {
            Log.e(TAG, "Channel created successfully with importance: " + verifyChannel.getImportance());
            return verifyChannel;
        } else {
            Log.e(TAG, "CRITICAL ERROR: Failed to create notification channel");
            return null;
        }
    }
    
    /**
     * Starts the foreground service with a persistent notification
     * Enhanced with redundant notification and verification
     */
    private void startForegroundService() {
        Log.e(TAG, "Starting foreground service with enhanced debug and visibility...");
        
        try {
            // Create notification channel first and verify it exists
            NotificationChannel channel = createNotificationChannel();
            if (channel == null) {
                Log.e(TAG, "WARNING: Failed to confirm notification channel exists");
            } else {
                Log.e(TAG, "Confirmed notification channel exists with importance: " + channel.getImportance());
            }
            
            // Create notification with high visibility
            Notification notification = buildNotification();
            Log.e(TAG, "Enhanced notification built successfully");
            
            // Create notification manager for direct notification
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            
            try {
                // Handle different Android versions for starting foreground service
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    Log.e(TAG, "Using startForeground with FOREGROUND_SERVICE_TYPE for Android 10+");
                    
                    // For Android 10+, specify multiple foreground service types
                    startForeground(NOTIFICATION_ID, notification, 
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE | 
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
                    
                    Log.e(TAG, "Foreground service with multiple types started successfully");
                } else {
                    // For older Android versions
                    Log.e(TAG, "Using basic startForeground for pre-Android 10");
                    startForeground(NOTIFICATION_ID, notification);
                    Log.e(TAG, "Basic foreground service started successfully");
                }
                
                // Send duplicate direct notification for redundancy
                Log.e(TAG, "Sending duplicate direct notification as backup");
                notificationManager.notify(NOTIFICATION_ID + 100, notification);
                Log.e(TAG, "Direct notification sent as backup with ID: " + (NOTIFICATION_ID + 100));
                
                // Schedule a verification check
                scheduleServiceVerification();
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to start foreground service", e);
                
                // More detailed error logging
                Log.e(TAG, "Error type: " + e.getClass().getName());
                Log.e(TAG, "Error message: " + e.getMessage());
                Log.e(TAG, "Stack trace: ", e);
                
                // Fallback method - try the basic version
                try {
                    Log.e(TAG, "Trying fallback to basic startForeground without type");
                    startForeground(NOTIFICATION_ID, notification);
                    Log.e(TAG, "Fallback to basic startForeground succeeded");
                    
                    // Also send direct notification
                    notificationManager.notify(NOTIFICATION_ID + 100, notification);
                    
                } catch (Exception e2) {
                    Log.e(TAG, "All startForeground attempts failed", e2);
                    
                    // Last resort - just send a direct notification
                    Log.e(TAG, "Last resort: Sending only direct notification");
                    notificationManager.notify(NOTIFICATION_ID, notification);
                    notificationManager.notify(NOTIFICATION_ID + 100, notification);
                    
                    throw e2; // Re-throw to be caught by outer try-catch
                }
            }
            
            // Ensure accessibility service is running and monitored
            establishAccessibilityServiceConnection();
            
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR starting foreground service", e);
            Log.e(TAG, "Exception type: " + e.getClass().getName());
            Log.e(TAG, "Stack trace: ", e);
        }
    }
    
    /**
     * Schedule a delayed verification of service status
     */
    private void scheduleServiceVerification() {
        Log.e(TAG, "Scheduling service verification checks");
        
        // Check after 1 second
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                verifyServiceRunning("1-second");
            }
        }, 1000);
        
        // Check after 5 seconds
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                verifyServiceRunning("5-second");
            }
        }, 5000);
    }
    
    /**
     * Verify the service is still running and notifications are visible
     */
    private void verifyServiceRunning(String checkName) {
        Log.e(TAG, "Running " + checkName + " verification check");
        
        // Check if service is running according to our flag
        Log.e(TAG, "Service running flag: " + isRunning);
        Log.e(TAG, "Static service running flag: " + isServiceRunning);
        
        // Check notification channel
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
        if (channel != null) {
            Log.e(TAG, "Notification channel still exists with importance: " + channel.getImportance());
        } else {
            Log.e(TAG, "WARNING: Notification channel is missing during verification!");
            
            // Try to recreate it
            createNotificationChannel();
            
            // And resend notification
            notificationManager.notify(NOTIFICATION_ID + 200, buildNotification());
        }
        
        // Send a test notification during verification
        Log.e(TAG, "Sending verification test notification");
        notificationManager.notify(NOTIFICATION_ID + 300, buildVerificationNotification(checkName));
    }
    
    /**
     * Build a special verification notification
     */
    private Notification buildVerificationNotification(String checkName) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BleHID Verification Check")
            .setContentText("Service verification: " + checkName)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build();
    }
    
    /**
     * Builds a highly visible persistent notification for the foreground service
     * with aggressive alerting features
     */
    private Notification buildNotification() {
        Log.e(TAG, "Building enhanced attention-grabbing notification");
        
        try {
            // Create launcher intent that definitely works
            Intent notificationIntent = new Intent(Intent.ACTION_MAIN);
            notificationIntent.setPackage(getPackageName());
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                      Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                                      Intent.FLAG_ACTIVITY_SINGLE_TOP);
            
            Log.e(TAG, "Created enhanced launcher intent for package: " + getPackageName());
            
            // Create pending intent with immutable flag
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                    notificationIntent, PendingIntent.FLAG_IMMUTABLE);
            
            // Create a more eye-catching title to help find in notification shade
            String attentionTitle = "⚠️ BleHID CRITICAL SERVICE ⚠️";
            
            // Build the notification with ALL possible attention-grabbing features
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(attentionTitle)
                .setContentText("ACTIVE: Background accessibility service must remain running")
                .setStyle(new NotificationCompat.BigTextStyle()
                    .bigText("This notification ensures background services remain running. " +
                             "Do not dismiss or disable this notification or app functionality will be lost."))
                .setSmallIcon(android.R.drawable.ic_secure)
                // Maximum priority settings
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setCategory(NotificationCompat.CATEGORY_ALARM) // ALARM has higher priority than SERVICE
                // Make it sticky
                .setOngoing(true)
                .setAutoCancel(false)
                // Add action buttons to make it larger and more noticeable
                .addAction(android.R.drawable.ic_menu_info_details, "Service Info", pendingIntent)
                .setContentIntent(pendingIntent)
                // Immediate display
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                // Colorize it bright red for maximum visibility
                .setColor(0xFFFF0000)
                .setColorized(true)
                // Use alert once to force initial display
                .setOnlyAlertOnce(true)
                // Add timestamp
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                // Add a timeout of 0 to prevent it from being automatically dismissed
                .setTimeoutAfter(0);
                
            // Add channel effects when notification is sent
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID);
            }
            
            Notification notification = builder.build();
            Log.e(TAG, "Enhanced attention-grabbing notification built successfully");
            return notification;
        } catch (Exception e) {
            Log.e(TAG, "CRITICAL ERROR building notification: " + e.getMessage(), e);
            Log.e(TAG, "Stack trace: ", e);
            
            // Absolute minimal fallback notification that should never fail
            try {
                Log.e(TAG, "Attempting to build minimal emergency notification");
                return new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("BleHID Emergency Mode")
                    .setContentText("CRITICAL: Service must keep running")
                    .setSmallIcon(android.R.drawable.ic_secure)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setOngoing(true)
                    .build();
            } catch (Exception e2) {
                Log.e(TAG, "Even emergency fallback notification failed: " + e2.getMessage(), e2);
                Log.e(TAG, "Last resort trace: ", e2);
                throw new RuntimeException("Cannot create any notification", e2);
            }
        }
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
