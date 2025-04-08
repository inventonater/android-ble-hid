package com.inventonater.blehid.core;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

/**
 * Centralized manager for all permission-related functionality.
 * Handles both Bluetooth and Accessibility Service permissions.
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";
    
    private final Context context;
    private final LocalInputController inputController;
    
    /**
     * Permission types that can be checked.
     */
    public enum PermissionType {
        BLUETOOTH,
        ACCESSIBILITY
    }
    
    /**
     * Creates a new PermissionManager.
     *
     * @param context The application context
     */
    public PermissionManager(Context context) {
        this.context = context;
        this.inputController = new LocalInputController(context);
    }
    
    /**
     * Checks if a specific permission type is granted.
     *
     * @param type The permission type to check
     * @return true if permission is granted, false otherwise
     */
    public boolean hasPermission(PermissionType type) {
        switch (type) {
            case BLUETOOTH:
                return checkBluetoothPermissions();
            case ACCESSIBILITY:
                return checkAccessibilityServiceEnabled();
            default:
                return false;
        }
    }
    
    /**
     * Opens the system settings page to request the specified permission.
     *
     * @param type The permission type to request
     * @return true if settings page was opened, false otherwise
     */
    public boolean requestPermission(PermissionType type) {
        switch (type) {
            case BLUETOOTH:
                return openBluetoothSettings();
            case ACCESSIBILITY:
                return openAccessibilitySettings();
            default:
                return false;
        }
    }
    
    /**
     * Checks if the app has all required Bluetooth permissions.
     *
     * @return true if all Bluetooth permissions are granted, false otherwise
     */
    private boolean checkBluetoothPermissions() {
        // For Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                   context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED &&
                   context.checkSelfPermission(android.Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        }
        // For Android 11 and below
        else {
            BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                return false;
            }
            
            BluetoothAdapter adapter = bluetoothManager.getAdapter();
            return adapter != null && adapter.isEnabled();
        }
    }
    
    /**
     * Checks if the accessibility service is enabled.
     *
     * @return true if the accessibility service is enabled, false otherwise
     */
    private boolean checkAccessibilityServiceEnabled() {
        try {
            String serviceName = context.getPackageName() + "/" + 
                              LocalAccessibilityService.class.getCanonicalName();
            
            String enabledServices = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            
            return enabledServices != null && enabledServices.contains(serviceName);
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility service: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Opens the application's settings page where Bluetooth permissions can be granted.
     *
     * @return true if settings page was opened, false otherwise
     */
    private boolean openBluetoothSettings() {
        try {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            android.net.Uri uri = android.net.Uri.fromParts("package", context.getPackageName(), null);
            intent.setData(uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error opening Bluetooth settings: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Opens the Accessibility Settings page where the accessibility service can be enabled.
     *
     * @return true if settings page was opened, false otherwise
     */
    private boolean openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error opening accessibility settings: " + e.getMessage());
            return false;
        }
    }
}
