package com.example.blehid.app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.blehid.app.ui.SimpleMouseActivity;
import com.example.blehid.core.BleHidManager;

/**
 * Main entry point for the BLE HID application.
 * Handles initialization, permission requests, and navigation to the keyboard interface.
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    
    // Permission request codes
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    
    // Bluetooth permissions for Android 12+
    private static final String[] PERMISSIONS_S_PLUS = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_CONNECT
    };
    
    // Permissions for Android 6.0-11
    private static final String[] PERMISSIONS_M_TO_R = {
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    
    private BleHidManager bleHidManager;
    private TextView statusText;
    private Button startMouseButton;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        statusText = findViewById(R.id.statusText);
        startMouseButton = findViewById(R.id.startMouseButton);
        
        // Initialize the BLE HID manager
        bleHidManager = new BleHidManager(this);
        
        // Check if BLE peripheral mode is supported
        if (!bleHidManager.isBlePeripheralSupported()) {
            statusText.setText(R.string.ble_peripheral_not_supported);
            startMouseButton.setEnabled(false);
            return;
        }
        
        // Set up mouse button click listener
        startMouseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchMouseActivity();
            }
        });
        
        // Check and request required permissions
        checkAndRequestPermissions();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
    
    /**
     * Checks and requests the necessary permissions for BLE operations.
     */
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            if (!hasPermissions(PERMISSIONS_S_PLUS)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_S_PLUS, REQUEST_PERMISSIONS);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-11 permissions
            if (!hasPermissions(PERMISSIONS_M_TO_R)) {
                ActivityCompat.requestPermissions(this, PERMISSIONS_M_TO_R, REQUEST_PERMISSIONS);
                return;
            }
        }
        
        // Check if Bluetooth is enabled
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    // For older versions, try without the permission check
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                } else {
                    // Cannot request Bluetooth enable without permission
                    statusText.setText(R.string.bluetooth_permission_required);
                }
                return;
            }
        }
        
        // All permissions granted and Bluetooth enabled
        updateStatus();
    }
    
    /**
     * Checks if the given permissions are granted.
     * 
     * @param permissions The permissions to check
     * @return true if all permissions are granted, false otherwise
     */
    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                // All permissions granted, check Bluetooth status
                checkAndRequestPermissions();
            } else {
                // Some permissions denied
                statusText.setText(R.string.permissions_required);
                startMouseButton.setEnabled(false);
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Bluetooth enabled successfully
                updateStatus();
            } else {
                // User declined to enable Bluetooth
                statusText.setText(R.string.bluetooth_disabled);
                startMouseButton.setEnabled(false);
            }
        }
    }
    
    /**
     * Updates the status text and button state based on the current BLE state.
     */
    private void updateStatus() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                if (bleHidManager.isBlePeripheralSupported()) {
                    statusText.setText(R.string.ready_to_start);
                    startMouseButton.setEnabled(true);
                } else {
                    statusText.setText(R.string.ble_peripheral_not_supported);
                    startMouseButton.setEnabled(false);
                }
            } else {
                statusText.setText(R.string.bluetooth_disabled);
                startMouseButton.setEnabled(false);
            }
        } else {
            statusText.setText(R.string.bluetooth_not_supported);
            startMouseButton.setEnabled(false);
        }
    }
    
    /**
     * Launches the SimpleMouseActivity.
     */
    private void launchMouseActivity() {
        Intent intent = new Intent(this, SimpleMouseActivity.class);
        startActivity(intent);
    }
}
