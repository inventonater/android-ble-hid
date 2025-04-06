package com.example.blehid.core;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.Arrays;
import java.util.UUID;

/**
 * Manages the GATT server for BLE HID functionality.
 */
public class BleGattServerManager {
    private static final String TAG = "BleGattServerManager";
    
    // Standard UUIDs for HID service required characteristics
    private static final UUID HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_INFO_UUID = UUID.fromString("00002a4a-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_REPORT_MAP_UUID = UUID.fromString("00002a4b-0000-1000-8000-00805f9b34fb");
    private static final UUID HID_CONTROL_POINT_UUID = UUID.fromString("00002a4c-0000-1000-8000-00805f9b34fb");
    
    // Client Characteristic Configuration Descriptor UUID
    private static final UUID CLIENT_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    
    private final BleHidManager bleHidManager;
    private final Context context;
    private final BluetoothManager bluetoothManager;
    
    private BluetoothGattServer gattServer;
    private BluetoothGattService hidService;
    
    /**
     * Creates a new GATT server manager.
     * 
     * @param bleHidManager The parent BLE HID manager
     */
    public BleGattServerManager(BleHidManager bleHidManager) {
        this.bleHidManager = bleHidManager;
        this.context = bleHidManager.getContext();
        this.bluetoothManager = bleHidManager.getBluetoothManager();
    }
    
    /**
     * Initializes the GATT server.
     * 
     * @return true if initialization was successful, false otherwise
     */
    public boolean initialize() {
        try {
            gattServer = bluetoothManager.openGattServer(context, gattServerCallback);
            
            if (gattServer == null) {
                Log.e(TAG, "Failed to open GATT server");
                return false;
            }
            
            Log.i(TAG, "GATT server initialized successfully");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize GATT server", e);
            return false;
        }
    }
    
    /**
     * Adds a HID service to the GATT server.
     * 
     * @param service The HID service to add
     * @return true if the service was added, false otherwise
     */
    public boolean addHidService(BluetoothGattService service) {
        if (gattServer == null) {
            Log.e(TAG, "GATT server not initialized");
            return false;
        }
        
        try {
            // Add the service to the GATT server
            boolean result = gattServer.addService(service);
            
            if (!result) {
                Log.e(TAG, "Failed to add HID service to GATT server");
                return false;
            }
            
            hidService = service;
            Log.i(TAG, "Added HID service to GATT server");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error adding HID service", e);
            return false;
        }
    }
    
    /**
     * Sends a notification for a characteristic.
     * 
     * @param charUuid The UUID of the characteristic
     * @param value The value to send
     * @return true if the notification was sent, false otherwise
     */
    public boolean sendNotification(UUID charUuid, byte[] value) {
        if (gattServer == null || hidService == null) {
            Log.e(TAG, "GATT server not initialized or HID service not added");
            return false;
        }
        
        BluetoothGattCharacteristic characteristic = hidService.getCharacteristic(charUuid);
        if (characteristic == null) {
            Log.e(TAG, "Characteristic not found: " + charUuid);
            return false;
        }
        
        BluetoothDevice connectedDevice = bleHidManager.getConnectedDevice();
        if (connectedDevice == null) {
            Log.e(TAG, "No connected device for sending notification");
            return false;
        }
        
        // Ensure that notifications are enabled by checking descriptor
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID);
        if (descriptor != null) {
            byte[] descValue = descriptor.getValue();
            if (descValue == null || !Arrays.equals(descValue, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                Log.w(TAG, "Notifications may not be enabled for " + charUuid + ", attempting to set");
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                // Not calling write descriptor here as we don't have access to the GATT client
            }
        }
        
        try {
            characteristic.setValue(value);
            
            // Simple retry once if it fails
            boolean success = gattServer.notifyCharacteristicChanged(connectedDevice, characteristic, false);
            if (!success) {
                Log.w(TAG, "First notification attempt failed, retrying once");
                try {
                    Thread.sleep(10); // Small delay before retry
                } catch (InterruptedException e) {
                    // Ignore
                }
                success = gattServer.notifyCharacteristicChanged(connectedDevice, characteristic, false);
            }
            
            if (success) {
                Log.d(TAG, "HID notification sent successfully");
            } else {
                Log.e(TAG, "Failed to send HID notification after retry");
            }
            
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error sending notification", e);
            return false;
        }
    }
    
    /**
     * Utility method to convert byte array to hex string for logging
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
    
    /**
     * Gets the GATT server instance.
     * 
     * @return The BluetoothGattServer instance
     */
    public BluetoothGattServer getGattServer() {
        return gattServer;
    }
    
    /**
     * Closes the GATT server and cleans up resources.
     */
    public void close() {
        if (gattServer != null) {
            gattServer.close();
            gattServer = null;
            hidService = null;
            Log.i(TAG, "GATT server closed");
        }
    }
    
    /**
     * Callback for GATT server operations.
     */
    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Device connected: " + device.getAddress());
                    bleHidManager.onDeviceConnected(device);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Device disconnected: " + device.getAddress());
                    bleHidManager.onDeviceDisconnected(device);
                }
            } else {
                Log.e(TAG, "Error in connection state change: " + status);
            }
        }

        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Service added: " + service.getUuid());
            } else {
                Log.e(TAG, "Failed to add service: " + status);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "Read request for characteristic: " + characteristic.getUuid() +
                    " from device: " + device.getAddress());
            
            UUID charUuid = characteristic.getUuid();
            boolean success = false;
            
            // Provide the requested data based on which characteristic was requested
            if (charUuid.equals(HID_INFO_UUID)) {
                // Default HID info
                byte[] hidInfo = new byte[] { 
                    0x11, 0x01,   // Version 1.11
                    0x00,         // Country code
                    0x03          // Flags (Remote wake + Normally connectable) - Changed from 0x01 to 0x03
                };
                
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 
                        offset, hidInfo);
                success = true;
            } 
            else if (charUuid.equals(HID_REPORT_MAP_UUID)) {
                // Get the report map from the mouse service only
                byte[] reportMap = null;
                if (bleHidManager.getHidMediaService() != null) {
                    reportMap = bleHidManager.getHidMediaService().getReportMap();
                }
                
                if (reportMap != null) {
                    // If the request wants part of the data (offset > 0)
                    if (offset > 0) {
                        if (offset < reportMap.length) {
                            byte[] offsetData = Arrays.copyOfRange(reportMap, offset, reportMap.length);
                            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 
                                    offset, offsetData);
                            success = true;
                        } else {
                            // Offset beyond data length
                            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, 
                                    0, null);
                        }
                    } else {
                        // Send the whole report map
                        gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 
                                0, reportMap);
                        success = true;
                    }
                }
            } 
            else {
                // Try the mouse service only
                byte[] response = null;
                if (bleHidManager.getHidMediaService() != null) {
                    response = bleHidManager.getHidMediaService().handleCharacteristicRead(charUuid, offset);
                }
                
                if (response != null) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 
                            offset, response);
                    success = true;
                }
            }
            
            // If we didn't handle it above, send error response
            if (!success) {
                Log.e(TAG, "Unhandled read characteristic: " + charUuid);
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 
                        0, null);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, 
                                                BluetoothGattCharacteristic characteristic,
                                                boolean preparedWrite, boolean responseNeeded,
                                                int offset, byte[] value) {
            Log.d(TAG, "Write request for characteristic: " + characteristic.getUuid() +
                    " from device: " + device.getAddress());
            
            UUID charUuid = characteristic.getUuid();
            boolean success = false;
            
            // Handle the write based on which characteristic is being written
            if (charUuid.equals(HID_CONTROL_POINT_UUID)) {
                // The HID Control Point only takes one-byte values
                if (value.length == 1) {
                    byte controlPoint = value[0];
                    Log.d(TAG, "HID Control Point set to: " + controlPoint);
                    
                    // Process the control point command for mouse service only
                    // Mouse doesn't generally need control point handling, but keep the structure
                    // for future expansion if needed
                    success = true;
                }
            } 
            else {
                // Delegate other characteristics to the mouse service handler - fixed comma issue
                success = bleHidManager.getHidMediaService()
                        .handleCharacteristicWrite(charUuid, value);
            }
            
            // Send response if needed
            if (responseNeeded) {
                int status = success ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE;
                gattServer.sendResponse(device, requestId, status, 0, null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset,
                                           BluetoothGattDescriptor descriptor) {
            Log.d(TAG, "Read request for descriptor: " + descriptor.getUuid() +
                    " from device: " + device.getAddress());
            
            // Most commonly requested descriptors are the Client Characteristic Config (for notifications)
            if (descriptor.getUuid().equals(CLIENT_CONFIG_UUID)) {
                byte[] value = new byte[] { 0x00, 0x00 }; // Default: notifications disabled
                
                // Check the current configuration for this descriptor
                // This would need to be stored per-device and per-characteristic
                
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 
                        0, value);
            } else {
                // Get descriptor value from mouse service handler - fixed comma issue
                byte[] response = bleHidManager.getHidMediaService()
                        .handleDescriptorRead(descriptor.getUuid(), descriptor.getCharacteristic().getUuid());
                
                if (response != null) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 
                            offset, response);
                } else {
                    Log.e(TAG, "Unhandled descriptor read: " + descriptor.getUuid());
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 
                            0, null);
                }
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                            BluetoothGattDescriptor descriptor,
                                            boolean preparedWrite, boolean responseNeeded,
                                            int offset, byte[] value) {
            Log.d(TAG, "Write request for descriptor: " + descriptor.getUuid() +
                    " from device: " + device.getAddress());
            
            boolean success = false;
            
            // Most commonly written descriptor is the Client Characteristic Config (for notifications)
            if (descriptor.getUuid().equals(CLIENT_CONFIG_UUID)) {
                if (value.length == 2) {
                    // Store descriptor value in the descriptor itself
                    descriptor.setValue(value);
                    
                    // 0x00 0x00 = notifications disabled
                    // 0x01 0x00 = notifications enabled
                    // 0x02 0x00 = indications enabled
                    if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                        Log.d(TAG, "Notifications ENABLED for " + descriptor.getCharacteristic().getUuid());
                        // Send an initial notification when notifications are enabled
                        try {
                            if (descriptor.getCharacteristic().getValue() != null) {
                                boolean result = sendNotification(
                                    descriptor.getCharacteristic().getUuid(),
                                    descriptor.getCharacteristic().getValue());
                                Log.d(TAG, "Sent initial notification after enabling: " + result);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error sending initial notification", e);
                        }
                    } else if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
                        Log.d(TAG, "Indications ENABLED for " + descriptor.getCharacteristic().getUuid());
                    } else {
                        Log.d(TAG, "Notifications/indications DISABLED for " + 
                                descriptor.getCharacteristic().getUuid());
                    }
                    
                    success = true;
                }
            } else {
                // Delegate to mouse service handler for other descriptors - fixed comma and params
                success = bleHidManager.getHidMediaService()
                        .handleDescriptorWrite(descriptor.getUuid(), 
                                descriptor.getCharacteristic().getUuid(), 
                                value);
            }
            
            // Send response if needed
            if (responseNeeded) {
                int status = success ? BluetoothGatt.GATT_SUCCESS : BluetoothGatt.GATT_FAILURE;
                gattServer.sendResponse(device, requestId, status, 0, null);
            }
        }
    };
}
