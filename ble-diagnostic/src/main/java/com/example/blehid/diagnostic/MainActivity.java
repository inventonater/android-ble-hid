package com.example.blehid.diagnostic;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.example.blehid.diagnostic.ui.ConnectionDebugActivity;
import com.example.blehid.diagnostic.ui.DeviceScanActivity;
import com.example.blehid.diagnostic.ui.GattProfileActivity;
import com.example.blehid.diagnostic.ui.ReportMonitorActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    
    // Permissions needed for BLE operations
    private static final String[] PERMISSIONS_S_ABOVE = new String[] {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
    };
    private static final String[] PERMISSIONS_R_BELOW = new String[] {
            Manifest.permission.ACCESS_FINE_LOCATION
    };
    
    private BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        // Set up navigation buttons
        setupButtons();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if Bluetooth is enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    == PackageManager.PERMISSION_GRANTED) {
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                requestPermissions();
            }
        }
        
        // Check for permissions
        if (!hasRequiredPermissions()) {
            requestPermissions();
        }
    }
    
    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) 
                   == PackageManager.PERMISSION_GRANTED
                   && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                   == PackageManager.PERMISSION_GRANTED;
        } else {
            // Android 11 and below
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                   == PackageManager.PERMISSION_GRANTED;
        }
    }
    
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(this, PERMISSIONS_S_ABOVE, REQUEST_PERMISSIONS);
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS_R_BELOW, REQUEST_PERMISSIONS);
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                          @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (!allGranted) {
                Toast.makeText(this, R.string.error_bluetooth_permissions, Toast.LENGTH_LONG).show();
            }
        }
    }
    
    private void setupButtons() {
        // Device Scan
        Button scanButton = findViewById(R.id.scanButton);
        scanButton.setOnClickListener(v -> {
            if (hasRequiredPermissions()) {
                Intent intent = new Intent(MainActivity.this, DeviceScanActivity.class);
                startActivity(intent);
            } else {
                requestPermissions();
            }
        });
        
        // Connection Debug
        Button connectionButton = findViewById(R.id.connectionButton);
        connectionButton.setOnClickListener(v -> {
            if (hasRequiredPermissions()) {
                Intent intent = new Intent(MainActivity.this, ConnectionDebugActivity.class);
                startActivity(intent);
            } else {
                requestPermissions();
            }
        });
        
        // Report Monitor
        Button reportButton = findViewById(R.id.reportButton);
        reportButton.setOnClickListener(v -> {
            if (hasRequiredPermissions()) {
                Intent intent = new Intent(MainActivity.this, ReportMonitorActivity.class);
                startActivity(intent);
            } else {
                requestPermissions();
            }
        });
        
        // GATT Profile
        Button gattButton = findViewById(R.id.gattButton);
        gattButton.setOnClickListener(v -> {
            if (hasRequiredPermissions()) {
                Intent intent = new Intent(MainActivity.this, GattProfileActivity.class);
                startActivity(intent);
            } else {
                requestPermissions();
            }
        });
    }
}
