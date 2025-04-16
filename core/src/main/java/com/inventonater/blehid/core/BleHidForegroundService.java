package com.inventonater.blehid.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.text.Html;
import android.text.Spanned;
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
public class BleHidForegroundService extends Service implements BleConnectionManager.ConnectionParameterListener {
    private static final String CHANNEL_ID = "BleHidForegroundServiceChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final String TAG = "BleHidForegroundSvc";
    private static final boolean VERBOSE_LOGGING = false; // Reduce logging noise
    
    // RSSI signal strength thresholds
    private static final int RSSI_THRESHOLD_GOOD = -60;    // -60 dBm or higher is good
    private static final int RSSI_THRESHOLD_MODERATE = -75; // -75 to -61 dBm is moderate
    // Anything below -75 dBm is considered poor
    
    // Signal strength display names
    private static final String SIGNAL_GOOD = "Good";
    private static final String SIGNAL_MODERATE = "Moderate";
    private static final String SIGNAL_POOR = "Poor";
    
    // Signal strength colors
    private static final int COLOR_SIGNAL_GOOD = Color.parseColor("#4CAF50");     // Green
    private static final int COLOR_SIGNAL_MODERATE = Color.parseColor("#FFC107"); // Amber
    private static final int COLOR_SIGNAL_POOR = Color.parseColor("#F44336");     // Red
    
    // Service state
    private boolean isRunning = false;
    private Handler handler;
    
    // BLE connection management
    private BleHidManager bleHidManager;
    private int currentRssi = 0;
    private String currentSignalLevel = "";
    private int currentSignalColor = COLOR_SIGNAL_MODERATE;
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
    
    /**
     * Get singleton instance of BleHidManager
     */
    private BleHidManager getBleHidManager() {
        if (bleHidManager == null) {
            bleHidManager = new BleHidManager(getApplicationContext());
        }
        return bleHidManager;
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
        
        // Initialize BLE management and register for RSSI updates
        BleHidManager manager = getBleHidManager();
        if (manager != null) {
            BleConnectionManager connectionManager = manager.getConnectionManager();
            if (connectionManager != null) {
                connectionManager.setConnectionParameterListener(this);
            }
        }
    }
    
    // Service action constants
    public static final String ACTION_START_FOREGROUND = "START_FOREGROUND";
    public static final String ACTION_STOP_FOREGROUND = "STOP_FOREGROUND";
    public static final String ACTION_REFRESH_NOTIFICATION = "REFRESH_NOTIFICATION";
    
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
            } else if (ACTION_REFRESH_NOTIFICATION.equals(action)) {
                Log.d(TAG, "Refreshing notification as requested");
                refreshNotification();
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
     * Public method to refresh the notification from outside
     * Called when connection state changes or RSSI changes significantly
     */
    public void refreshNotification() {
        if (isRunning) {
            updateNotification();
        }
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
        
        if (VERBOSE_LOGGING) {
            Log.d(TAG, "Creating notification channel: " + CHANNEL_ID);
        }
        
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
        
        // Check if we have a connected device
        String deviceName = "Not Connected";
        boolean isConnected = false;
        
        BleHidManager manager = getBleHidManager();
        if (manager != null && manager.isConnected() && manager.getConnectedDevice() != null) {
            BluetoothDevice device = manager.getConnectedDevice();
            String name = device.getName();
            // Fall back to address if name is null
            deviceName = name != null && !name.isEmpty() ? name : device.getAddress();
            isConnected = true;
        }
        
        // Determine title and content based on connection state
        String title = isConnected ? "BleHID Connected" : "BleHID Ready";
        
        // Create content text with signal strength if connected
        String contentText;
        if (isConnected) {
            // Format as "Device: My Pixel Phone • Signal: Good (-58 dBm)"
            contentText = "Device: " + deviceName + " • Signal: " + 
                          currentSignalLevel + " (" + currentRssi + " dBm)";
        } else {
            contentText = "Waiting for connection...";
        }
        
        // Use a more subtle icon
        int iconResource = android.R.drawable.stat_sys_data_bluetooth;
        
        // Build a more subtle notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(iconResource)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Default priority instead of MAX
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(System.currentTimeMillis());
            
        // Set color based on signal strength (only when connected)
        if (isConnected) {
            builder.setColor(currentSignalColor);
            
            // Use BigTextStyle to provide more info
            NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle()
                .bigText(contentText)
                .setBigContentTitle(title);
                
            builder.setStyle(bigTextStyle);
        }
        
        return builder.build();
    }
    
    /**
     * Updates the notification with current connection and RSSI information
     */
    private void updateNotification() {
        if (!isRunning) {
            return;
        }
        
        try {
            Notification notification = buildNotification();
            notificationManager.notify(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Error updating notification", e);
        }
    }
    /**
     * Updates the signal level info based on RSSI
     * Returns true if the signal level changed
     */
    private boolean updateSignalLevel(int rssi) {
        String oldSignalLevel = currentSignalLevel;
        int oldSignalColor = currentSignalColor;
        
        // Update the current RSSI value
        currentRssi = rssi;
        
        // Determine signal level based on RSSI thresholds
        if (rssi >= RSSI_THRESHOLD_GOOD) {
            currentSignalLevel = SIGNAL_GOOD;
            currentSignalColor = COLOR_SIGNAL_GOOD;
        } else if (rssi >= RSSI_THRESHOLD_MODERATE) {
            currentSignalLevel = SIGNAL_MODERATE;
            currentSignalColor = COLOR_SIGNAL_MODERATE;
        } else {
            currentSignalLevel = SIGNAL_POOR;
            currentSignalColor = COLOR_SIGNAL_POOR;
        }
        
        // Return true if level or color changed
        return !currentSignalLevel.equals(oldSignalLevel) || currentSignalColor != oldSignalColor;
    }
    
    // ----- Connection Parameter Listener Implementation -----
    
    @Override
    public void onConnectionParametersChanged(int interval, int latency, int timeout, int mtu) {
        // We don't need to update the notification for these changes
    }
    
    @Override
    public void onRssiRead(int rssi) {
        // Update notification only when signal level changes
        if (updateSignalLevel(rssi)) {
            updateNotification();
        }
    }
    
    @Override
    public void onRequestComplete(String parameterName, boolean success, String actualValue) {
        // We don't need to update the notification for these events
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
