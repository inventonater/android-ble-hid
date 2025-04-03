package com.example.blehid.diagnostic.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.blehid.diagnostic.R;

/**
 * Background service for monitoring BLE HID reports.
 * This service can run in the background to collect HID reports even when the app is not in the foreground.
 */
public class BleMonitorService extends Service {
    private static final String TAG = "BleMonitorService";
    private static final String CHANNEL_ID = "BleHidMonitorChannel";
    private static final int NOTIFICATION_ID = 1;
    
    private final IBinder binder = new LocalBinder();
    private boolean isRunning = false;
    
    public class LocalBinder extends Binder {
        public BleMonitorService getService() {
            return BleMonitorService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate()");
        createNotificationChannel();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand()");
        
        if (!isRunning) {
            // Start as a foreground service with a notification
            Notification notification = createNotification();
            startForeground(NOTIFICATION_ID, notification);
            isRunning = true;
        }
        
        // Return START_STICKY to ensure service restarts if it's killed
        return START_STICKY;
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy()");
        isRunning = false;
        super.onDestroy();
    }
    
    /**
     * Creates a notification channel for Android O and above
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "BLE HID Monitor Service",
                    NotificationManager.IMPORTANCE_LOW);
            
            channel.setDescription("Background service for monitoring BLE HID reports");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * Creates a notification for the foreground service
     */
    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("BLE HID Monitor")
                .setContentText("Monitoring HID reports in the background")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setPriority(NotificationCompat.PRIORITY_LOW);
        
        return builder.build();
    }
    
    /**
     * Checks if the service is currently running
     */
    public boolean isRunning() {
        return isRunning;
    }
    
    /**
     * Starts monitoring for HID reports
     */
    public void startMonitoring() {
        // In a real implementation, this would start BLE scanning and connecting
        Log.d(TAG, "Starting BLE HID monitoring");
    }
    
    /**
     * Stops monitoring for HID reports
     */
    public void stopMonitoring() {
        // In a real implementation, this would stop BLE operations
        Log.d(TAG, "Stopping BLE HID monitoring");
    }
}
