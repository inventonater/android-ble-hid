package com.inventonater.hid.app;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.inventonater.hid.app.ui.SimpleKeyboardActivity;
import com.inventonater.hid.app.ui.SimpleMouseActivity;
import com.inventonater.hid.core.BleHid;
import com.inventonater.hid.core.api.ConnectionListener;
import com.inventonater.hid.core.api.LogLevel;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    
    private TextView statusText;
    private Button advertiseButton;
    private Button mouseButton;
    private Button keyboardButton;
    
    private boolean isInitialized = false;
    private boolean isAdvertising = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // UI components
        statusText = findViewById(R.id.statusText);
        advertiseButton = findViewById(R.id.advertiseButton);
        mouseButton = findViewById(R.id.mouseButton);
        keyboardButton = findViewById(R.id.keyboardButton);
        
        // Setup button click listeners
        advertiseButton.setOnClickListener(this::onAdvertiseClicked);
        mouseButton.setOnClickListener(v -> startActivity(new Intent(this, SimpleMouseActivity.class)));
        keyboardButton.setOnClickListener(v -> startActivity(new Intent(this, SimpleKeyboardActivity.class)));
        
        // Check if BLE is supported
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth LE is not supported on this device", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Check and request permissions
        checkAndRequestPermissions();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Update UI based on current state
        updateUI();
        
        // Try to initialize if needed
        if (!isInitialized && checkPermissions()) {
            initializeBleHid();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        
        // Stop advertising when app goes to background
        if (isAdvertising) {
            BleHid.stopAdvertising();
            isAdvertising = false;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Shutdown BLE HID when activity is destroyed
        if (isInitialized) {
            BleHid.shutdown();
            isInitialized = false;
        }
    }
    
    private void onAdvertiseClicked(View view) {
        if (!isInitialized) {
            Toast.makeText(this, "BLE HID not initialized", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (isAdvertising) {
            // Stop advertising
            BleHid.stopAdvertising();
            isAdvertising = false;
        } else {
            // Start advertising
            boolean success = BleHid.startAdvertising();
            isAdvertising = success;
            
            if (!success) {
                Toast.makeText(this, "Failed to start advertising", Toast.LENGTH_SHORT).show();
            }
        }
        
        updateUI();
    }
    
    private void updateUI() {
        if (!isInitialized) {
            statusText.setText("Status: Not initialized");
            advertiseButton.setEnabled(false);
            mouseButton.setEnabled(false);
            keyboardButton.setEnabled(false);
            return;
        }
        
        advertiseButton.setEnabled(true);
        
        if (isAdvertising) {
            advertiseButton.setText("Stop Advertising");
            statusText.setText("Status: Advertising");
        } else {
            advertiseButton.setText("Start Advertising");
            statusText.setText("Status: Ready");
        }
        
        if (BleHid.isConnected()) {
            statusText.setText("Status: Connected");
            mouseButton.setEnabled(true);
            keyboardButton.setEnabled(true);
        } else {
            mouseButton.setEnabled(false);
            keyboardButton.setEnabled(false);
        }
    }
    
    private void initializeBleHid() {
        // Check if Bluetooth is enabled
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            return;
        }
        
        // Initialize BLE HID
        isInitialized = BleHid.initialize(getApplicationContext(), LogLevel.DEBUG);
        
        if (isInitialized) {
            // Register connection listener
            BleHid.addConnectionListener(new ConnectionListener() {
                @Override
                public void onDeviceConnected(BluetoothDevice device) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Connected to: " + device.getName(), 
                            Toast.LENGTH_SHORT).show();
                        updateUI();
                    });
                }
                
                @Override
                public void onDeviceDisconnected(BluetoothDevice device) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, 
                            "Disconnected from: " + device.getName(), 
                            Toast.LENGTH_SHORT).show();
                        updateUI();
                    });
                }
            });
            
            // Check if BLE peripheral mode is supported
            if (!BleHid.isBlePeripheralSupported()) {
                Toast.makeText(this, "BLE peripheral mode not supported on this device", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Failed to initialize BLE HID", Toast.LENGTH_SHORT).show();
        }
        
        updateUI();
    }
    
    // Permission handling
    private boolean checkPermissions() {
        return getRequiredPermissions().isEmpty();
    }
    
    private void checkAndRequestPermissions() {
        List<String> permissionsToRequest = getRequiredPermissions();
        
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toArray(new String[0]),
                REQUEST_PERMISSIONS
            );
        } else if (!isInitialized) {
            initializeBleHid();
        }
    }
    
    private List<String> getRequiredPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        
        // Check and add required permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN);
            }
        } else {
            // Older Android versions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH);
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        
        return permissionsToRequest;
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
                initializeBleHid();
            } else {
                Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                initializeBleHid();
            } else {
                Toast.makeText(this, "Bluetooth must be enabled to use this app", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
