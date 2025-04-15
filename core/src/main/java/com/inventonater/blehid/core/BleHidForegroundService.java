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
    
    // Service state
    private boolean isRunning = false;
    private Handler handler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        
        if ("START_FOREGROUND".equals(action)) {
            if (!isRunning) {
                startForegroundService();
                isRunning = true;
                Log.d(TAG, "Foreground service started");
            }
        } else if ("STOP_FOREGROUND".equals(action)) {
            stopForeground(Service.STOP_FOREGROUND_REMOVE);
            stopSelf();
            isRunning = false;
            Log.d(TAG, "Foreground service stopped");
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
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "BleHID Background Service",
            NotificationManager.IMPORTANCE_LOW
        );
        
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
        // Create notification
        Notification notification = buildNotification();
        
        // Using Android 12's improved foreground service type specification
        // FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE is appropriate for BLE operations
        if (ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)) {
            Log.d(TAG, "Foreground service started with proper type");
        }
        
        // Ensure accessibility service is running and monitored
        establishAccessibilityServiceConnection();
    }
    
    /**
     * Builds the persistent notification for the foreground service
     */
    private Notification buildNotification() {
        // Create a pending intent to open the main Unity activity when notification is tapped
        Intent notificationIntent;
        try {
            // Try to get the Unity player activity class
            Class<?> unityPlayerClass = Class.forName("com.unity3d.player.UnityPlayerActivity");
            notificationIntent = new Intent(this, unityPlayerClass);
        } catch (ClassNotFoundException e) {
            // Fallback if Unity class not found
            notificationIntent = new Intent();
            notificationIntent.setPackage(getPackageName());
            notificationIntent.setAction(Intent.ACTION_MAIN);
            notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        }
        
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        
        // Using FLAG_IMMUTABLE as recommended for Android 12+
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, 
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        
        // Build the notification with Android 12 design guidelines
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("BleHID Active")
            .setContentText("Maintaining background connection for accessibility services")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Using system icon as placeholder
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .build();
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
