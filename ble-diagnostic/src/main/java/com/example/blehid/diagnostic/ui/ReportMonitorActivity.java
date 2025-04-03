package com.example.blehid.diagnostic.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blehid.diagnostic.R;
import com.example.blehid.diagnostic.model.HidReport;
import com.example.blehid.diagnostic.service.BleMonitorService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class ReportMonitorActivity extends AppCompatActivity {
    private static final String TAG = "ReportMonitorActivity";
    
    // Standard UUIDs for HID service and characteristics
    private static final UUID HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_REPORT_MAP_UUID = UUID.fromString("00002a4b-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_REPORT_UUID = UUID.fromString("00002a4d-0000-1000-8000-00805f9b34fb");
    private static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    private static final int REQUEST_DEVICE_SCAN = 1;
    private static final int MAX_REPORTS = 100;
    
    private TextView connectionStatusTextView;
    private TextView reportTimestampTextView;
    private TextView reportTypeTextView;
    private TextView rawDataTextView;
    private TextView decodedDataTextView;
    private TextView emptyStateTextView;
    private Button connectButton;
    private Button startMonitoringButton;
    private Button clearButton;
    private Button saveLogsButton;
    private RecyclerView reportsRecyclerView;
    
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private BluetoothDevice targetDevice;
    private boolean isMonitoring = false;
    
    private List<HidReport> reports = new ArrayList<>();
    private ReportAdapter reportAdapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_monitor);
        
        // Initialize Bluetooth
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
        }
        
        // Initialize UI components
        connectionStatusTextView = findViewById(R.id.connectionStatusTextView);
        reportTimestampTextView = findViewById(R.id.reportTimestampTextView);
        reportTypeTextView = findViewById(R.id.reportTypeTextView);
        rawDataTextView = findViewById(R.id.rawDataTextView);
        decodedDataTextView = findViewById(R.id.decodedDataTextView);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        connectButton = findViewById(R.id.connectButton);
        startMonitoringButton = findViewById(R.id.startMonitoringButton);
        clearButton = findViewById(R.id.clearButton);
        saveLogsButton = findViewById(R.id.saveLogsButton);
        reportsRecyclerView = findViewById(R.id.reportsRecyclerView);
        
        // Initialize RecyclerView
        reportAdapter = new ReportAdapter(reports);
        reportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reportsRecyclerView.setAdapter(reportAdapter);
        
        setupButtons();
        updateEmptyState();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectFromDevice();
    }
    
    private void setupButtons() {
        connectButton.setOnClickListener(v -> {
            if (targetDevice == null) {
                showDeviceScanDialog();
            } else {
                disconnectFromDevice();
                connectButton.setText("Connect");
            }
        });
        
        startMonitoringButton.setOnClickListener(v -> {
            if (isMonitoring) {
                stopMonitoring();
            } else {
                startMonitoring();
            }
        });
        
        clearButton.setOnClickListener(v -> {
            reports.clear();
            reportAdapter.notifyDataSetChanged();
            updateEmptyState();
            resetLatestReportView();
        });
        
        saveLogsButton.setOnClickListener(v -> {
            if (reports.isEmpty()) {
                Toast.makeText(this, "No logs to save", Toast.LENGTH_SHORT).show();
                return;
            }
            
            saveLogsToFile();
        });
    }
    
    private void showDeviceScanDialog() {
        // Here we would start a device scan activity and get the result
        // For simplicity, let's show a simple dialog with some options
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Device Type");
        builder.setMessage("This would normally show a device scan. For now, select the type of device:");
        
        builder.setPositiveButton("HID Peripheral (Pixel)", (dialog, which) -> {
            // Connect to the HID peripheral device running our app
            // In a real implementation, you would get the MAC address from scan results
            if (bluetoothAdapter != null) {
                // Use the first paired device for demonstration
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                        == PackageManager.PERMISSION_GRANTED) {
                    for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                        // This is just for demonstration. You would normally use the scanned device
                        targetDevice = device;
                        connectToDevice(device);
                        break;
                    }
                }
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void connectToDevice(BluetoothDevice device) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        connectionStatusTextView.setText("Connecting to " + device.getName() + "...");
        connectButton.setText("Disconnect");
        
        // Connect to the device
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
    }
    
    private void disconnectFromDevice() {
        if (bluetoothGatt != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
        
        targetDevice = null;
        isMonitoring = false;
        startMonitoringButton.setEnabled(false);
        startMonitoringButton.setText(R.string.monitor_start);
        connectionStatusTextView.setText("Not connected");
        connectionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
    }
    
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread(() -> {
                    connectionStatusTextView.setText("Connected to device");
                    connectionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                    startMonitoringButton.setEnabled(true);
                });
                
                if (ActivityCompat.checkSelfPermission(ReportMonitorActivity.this, 
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread(() -> {
                    connectionStatusTextView.setText("Disconnected");
                    connectionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    startMonitoringButton.setEnabled(false);
                    connectButton.setText("Connect");
                });
            }
        }
        
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (ActivityCompat.checkSelfPermission(ReportMonitorActivity.this, 
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                
                BluetoothGattService hidService = gatt.getService(HID_SERVICE_UUID);
                if (hidService != null) {
                    runOnUiThread(() -> {
                        connectionStatusTextView.setText("Connected, HID service found");
                    });
                } else {
                    runOnUiThread(() -> {
                        connectionStatusTextView.setText("Connected, but no HID service found");
                        connectionStatusTextView.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    });
                }
            }
        }
        
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            if (characteristic.getUuid().equals(HID_REPORT_UUID)) {
                if (ActivityCompat.checkSelfPermission(ReportMonitorActivity.this, 
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                
                byte[] value = characteristic.getValue();
                String deviceAddress = gatt.getDevice().getAddress();
                
                // Create a new HID report and add it to the list
                HidReport report = new HidReport(value, HidReport.TYPE_INPUT, deviceAddress);
                addReport(report);
            }
        }
    };
    
    private void startMonitoring() {
        if (bluetoothGatt == null) {
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        BluetoothGattService hidService = bluetoothGatt.getService(HID_SERVICE_UUID);
        if (hidService == null) {
            Toast.makeText(this, "HID service not found on the device", Toast.LENGTH_SHORT).show();
            return;
        }
        
        List<BluetoothGattCharacteristic> characteristics = hidService.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristics) {
            if (characteristic.getUuid().equals(HID_REPORT_UUID)) {
                boolean success = bluetoothGatt.setCharacteristicNotification(characteristic, true);
                
                if (success) {
                    // Enable notifications on the client configuration descriptor
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID);
                    if (descriptor != null) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        bluetoothGatt.writeDescriptor(descriptor);
                        
                        isMonitoring = true;
                        startMonitoringButton.setText(R.string.monitor_stop);
                        Toast.makeText(this, "Started monitoring HID reports", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    
    private void stopMonitoring() {
        if (bluetoothGatt == null) {
            return;
        }
        
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) 
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        
        BluetoothGattService hidService = bluetoothGatt.getService(HID_SERVICE_UUID);
        if (hidService == null) {
            return;
        }
        
        List<BluetoothGattCharacteristic> characteristics = hidService.getCharacteristics();
        for (BluetoothGattCharacteristic characteristic : characteristics) {
            if (characteristic.getUuid().equals(HID_REPORT_UUID)) {
                bluetoothGatt.setCharacteristicNotification(characteristic, false);
                
                // Disable notifications on the client configuration descriptor
                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID);
                if (descriptor != null) {
                    descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(descriptor);
                    
                    isMonitoring = false;
                    startMonitoringButton.setText(R.string.monitor_start);
                    Toast.makeText(this, "Stopped monitoring HID reports", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    private void addReport(HidReport report) {
        runOnUiThread(() -> {
            // Add to the beginning of the list
            reports.add(0, report);
            
            // Keep the list capped at MAX_REPORTS
            if (reports.size() > MAX_REPORTS) {
                reports.remove(reports.size() - 1);
            }
            
            // Update the adapter
            reportAdapter.notifyDataSetChanged();
            
            // Update the latest report view
            updateLatestReportView(report);
            
            // Update empty state
            updateEmptyState();
        });
    }
    
    private void updateLatestReportView(HidReport report) {
        reportTimestampTextView.setText("Timestamp: " + report.getFormattedTimestamp());
        reportTypeTextView.setText("Type: " + report.getReportTypeString());
        rawDataTextView.setText("Raw Data: " + report.getReportDataHex());
        decodedDataTextView.setText("Decoded: " + report.interpretKeyboardReport());
    }
    
    private void resetLatestReportView() {
        reportTimestampTextView.setText("Timestamp: -");
        reportTypeTextView.setText("Type: -");
        rawDataTextView.setText("Raw Data: -");
        decodedDataTextView.setText("Decoded: -");
    }
    
    private void updateEmptyState() {
        if (reports.isEmpty()) {
            emptyStateTextView.setVisibility(View.VISIBLE);
            reportsRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateTextView.setVisibility(View.GONE);
            reportsRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void saveLogsToFile() {
        if (ActivityCompat.checkSelfPermission(this, 
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            return;
        }
        
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "hid_report_log_" + timeStamp + ".txt";
        
        try {
            File outputDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), "HidDiagnostic");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            File outputFile = new File(outputDir, fileName);
            FileOutputStream fos = new FileOutputStream(outputFile);
            
            // Write header
            String header = "HID Report Log - " + timeStamp + "\n"
                    + "Device: " + (targetDevice != null ? 
                    (targetDevice.getName() + " (" + targetDevice.getAddress() + ")") : "Unknown") + "\n"
                    + "========================================\n\n";
            fos.write(header.getBytes());
            
            // Write reports in chronological order (oldest first)
            for (int i = reports.size() - 1; i >= 0; i--) {
                HidReport report = reports.get(i);
                String reportText = report.getFormattedTimestamp() + " - "
                        + report.getReportTypeString() + " Report\n"
                        + "Raw Data: " + report.getReportDataHex() + "\n"
                        + "Decoded: " + report.interpretKeyboardReport() + "\n\n";
                fos.write(reportText.getBytes());
            }
            
            fos.close();
            
            Toast.makeText(this, getString(R.string.logs_saved, outputFile.getAbsolutePath()), 
                    Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Log.e(TAG, "Error saving logs", e);
            Toast.makeText(this, "Error saving logs: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ViewHolder> {
        private final List<HidReport> reports;
        
        public ReportAdapter(List<HidReport> reports) {
            this.reports = reports;
        }
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            HidReport report = reports.get(position);
            holder.text1.setText(report.getFormattedTimestamp() + " - " + report.getReportTypeString());
            holder.text2.setText("Raw: " + report.getReportDataHex() + " | " 
                    + report.interpretKeyboardReport());
        }
        
        @Override
        public int getItemCount() {
            return reports.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text1;
            TextView text2;
            
            ViewHolder(View view) {
                super(view);
                text1 = view.findViewById(android.R.id.text1);
                text2 = view.findViewById(android.R.id.text2);
                
                view.setOnClickListener(v -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        updateLatestReportView(reports.get(position));
                    }
                });
            }
        }
    }
}
