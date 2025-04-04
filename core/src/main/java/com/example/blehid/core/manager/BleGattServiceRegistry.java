package com.example.blehid.core.manager;

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
import android.os.Build;
import android.util.Log;

import com.example.blehid.core.BleNotifier;
import com.example.blehid.core.HidConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registry for BLE GATT services.
 * This class is responsible for managing the GATT server and registered services.
 */
public class BleGattServiceRegistry {
    private static final String TAG = "BleGattServiceRegistry";
    
    private final BleLifecycleManager lifecycleManager;
    private BluetoothGattServer gattServer;
    private final Map<UUID, BluetoothGattService> services = new HashMap<>();
    private final Map<UUID, ServiceHandler> serviceHandlers = new HashMap<>();
    private BleNotifier notifier;
    
    /**
     * Creates a new BLE GATT service registry.
     *
     * @param lifecycleManager The BLE lifecycle manager
     */
    public BleGattServiceRegistry(BleLifecycleManager lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }
    
    /**
     * Starts the GATT server.
     *
     * @return true if successful, false otherwise
     */
    public boolean start() {
        if (gattServer != null) {
            Log.w(TAG, "GATT server already started");
            return true;
        }
        
        Context context = lifecycleManager.getContext();
        BluetoothManager bluetoothManager = lifecycleManager.getBluetoothManager();
        
        if (bluetoothManager == null) {
            Log.e(TAG, "Bluetooth manager not available");
            return false;
        }
        
        // Create the GATT server
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback);
        
        if (gattServer == null) {
            Log.e(TAG, "Failed to open GATT server");
            return false;
        }
        
        // Create the BLE notifier
        notifier = new BleNotifier(this);
        
        // Add any registered services
        for (BluetoothGattService service : services.values()) {
            boolean success = gattServer.addService(service);
            if (!success) {
                Log.e(TAG, "Failed to add service: " + service.getUuid());
                return false;
            }
        }
        
        Log.i(TAG, "GATT server started");
        return true;
    }
    
    /**
     * Stops the GATT server.
     */
    public void stop() {
        if (gattServer != null) {
            gattServer.close();
            gattServer = null;
            Log.i(TAG, "GATT server stopped");
        }
    }
    
    /**
     * Adds a service to the registry.
     *
     * @param service The service to add
     * @return true if successful, false otherwise
     */
    public boolean addService(BluetoothGattService service) {
        if (service == null) {
            Log.e(TAG, "Cannot add null service");
            return false;
        }
        
        UUID serviceUuid = service.getUuid();
        services.put(serviceUuid, service);
        
        // If the GATT server is already running, add the service
        if (gattServer != null) {
            boolean success = gattServer.addService(service);
            if (!success) {
                Log.e(TAG, "Failed to add service: " + serviceUuid);
                services.remove(serviceUuid);
                return false;
            }
        }
        
        Log.d(TAG, "Service added: " + serviceUuid);
        return true;
    }
    
    /**
     * Removes a service from the registry.
     *
     * @param serviceUuid The UUID of the service to remove
     * @return true if successful, false otherwise
     */
    public boolean removeService(UUID serviceUuid) {
        if (serviceUuid == null) {
            Log.e(TAG, "Cannot remove service with null UUID");
            return false;
        }
        
        if (!services.containsKey(serviceUuid)) {
            Log.w(TAG, "Service not found: " + serviceUuid);
            return false;
        }
        
        // If the GATT server is running, remove the service
        if (gattServer != null) {
            BluetoothGattService service = services.get(serviceUuid);
            if (service != null) {
                gattServer.removeService(service);
            }
        }
        
        services.remove(serviceUuid);
        serviceHandlers.remove(serviceUuid);
        
        Log.d(TAG, "Service removed: " + serviceUuid);
        return true;
    }
    
    /**
     * Gets a service by UUID.
     *
     * @param serviceUuid The UUID of the service
     * @return The service, or null if not found
     */
    public BluetoothGattService getService(UUID serviceUuid) {
        return services.get(serviceUuid);
    }
    
    /**
     * Registers a service handler.
     *
     * @param serviceUuid The UUID of the service
     * @param handler The service handler
     */
    public void registerServiceHandler(UUID serviceUuid, ServiceHandler handler) {
        if (serviceUuid == null || handler == null) {
            Log.e(TAG, "Cannot register null service UUID or handler");
            return;
        }
        
        serviceHandlers.put(serviceUuid, handler);
        Log.d(TAG, "Service handler registered for: " + serviceUuid);
    }
    
    /**
     * Gets a service handler by UUID.
     *
     * @param serviceUuid The UUID of the service
     * @return The service handler, or null if not found
     */
    public ServiceHandler getServiceHandler(UUID serviceUuid) {
        return serviceHandlers.get(serviceUuid);
    }
    
    /**
     * Sends a notification for a characteristic.
     *
     * @param characteristicUuid The UUID of the characteristic
     * @param value The value to send
     * @return true if successful, false otherwise
     */
    public boolean sendNotification(UUID characteristicUuid, byte[] value) {
        if (gattServer == null) {
            Log.e(TAG, "GATT server not started");
            return false;
        }
        
        BluetoothDevice device = lifecycleManager.getConnectionManager().getConnectedDevice();
        if (device == null) {
            Log.e(TAG, "No connected device for notification to: " + characteristicUuid);
            return false;
        }
        
        // Log device info and bond state to help debug
        Log.d(TAG, "Sending notification to device: " + device.getAddress() + 
              " (Bonded: " + (device.getBondState() == BluetoothDevice.BOND_BONDED) + ")");
        
        // Find the characteristic
        BluetoothGattCharacteristic characteristic = findCharacteristic(characteristicUuid);
        if (characteristic == null) {
            // Check if this is the Report characteristic for HID service
            if (characteristicUuid.equals(HidConstants.HID_REPORT_UUID)) {
                // Special handling for HID Report - search in HID service first
                BluetoothGattService hidService = getService(HidConstants.HID_SERVICE_UUID);
                if (hidService != null) {
                    List<BluetoothGattCharacteristic> reportChars = hidService.getCharacteristics();
                    // Use the first Report characteristic we can find
                    for (BluetoothGattCharacteristic reportChar : reportChars) {
                        if (reportChar.getUuid().equals(HidConstants.HID_REPORT_UUID)) {
                            Log.d(TAG, "Found HID Report characteristic");
                            characteristic = reportChar;
                            break;
                        }
                    }
                }
            }
            
            if (characteristic == null) {
                Log.e(TAG, "Characteristic not found for notification: " + characteristicUuid);
                return false;
            }
        }
        
        // Update the value
        characteristic.setValue(value);
        
        // For HID characteristics, try both notification and indication as Android HID might 
        // need reliable delivery - indicated by the second "confirm" parameter
        boolean notificationSuccess = false;
        boolean indicationSuccess = false;
        
        // Try notification first (more common for HID)
        try {
            notificationSuccess = gattServer.notifyCharacteristicChanged(device, characteristic, false);
            if (notificationSuccess) {
                Log.d(TAG, "Notification sent successfully for: " + characteristicUuid);
            } else {
                Log.w(TAG, "Notification failed for: " + characteristicUuid + ", trying indication");
                
                // If notification fails, try indication
                indicationSuccess = gattServer.notifyCharacteristicChanged(device, characteristic, true);
                if (indicationSuccess) {
                    Log.d(TAG, "Indication sent successfully for: " + characteristicUuid);
                } else {
                    Log.e(TAG, "Both notification and indication failed for: " + characteristicUuid);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception sending notification/indication: " + e.getMessage());
            return false;
        }
        
        return notificationSuccess || indicationSuccess;
    }
    
    /**
     * Finds a characteristic by UUID across all services.
     *
     * @param characteristicUuid The UUID of the characteristic
     * @return The characteristic, or null if not found
     */
    private BluetoothGattCharacteristic findCharacteristic(UUID characteristicUuid) {
        Log.d(TAG, "Looking for characteristic with UUID: " + characteristicUuid);
        
        // Log all services and their characteristics
        StringBuilder debugInfo = new StringBuilder("Available services and characteristics:");
        for (BluetoothGattService service : services.values()) {
            debugInfo.append("\nService: ").append(service.getUuid());
            for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                debugInfo.append("\n  - Char: ").append(characteristic.getUuid());
            }
        }
        Log.d(TAG, debugInfo.toString());
        
        // First try exact match
        for (BluetoothGattService service : services.values()) {
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
            if (characteristic != null) {
                Log.d(TAG, "Found exact match for characteristic: " + characteristicUuid);
                return characteristic;
            }
        }
        
        // No exact match was found, try to find the main report characteristic and check report ID
        if (characteristicUuid.toString().startsWith(HidConstants.HID_REPORT_UUID.toString().substring(0, 24))) {
            Log.d(TAG, "No exact match found, but characteristic appears to be a derived HID report UUID");
            // This is one of our derived report UUIDs, try to find the correct characteristic
            for (BluetoothGattService service : services.values()) {
                if (service.getUuid().equals(HidConstants.HID_SERVICE_UUID)) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(HidConstants.REPORT_REFERENCE_UUID);
                        if (descriptor != null && descriptor.getValue() != null && descriptor.getValue().length > 0) {
                            // Compare the report ID in this descriptor with the one embedded in the UUID
                            byte reportId = descriptor.getValue()[0];
                            
                            // Extract report ID from the UUID - the last several bits contain our ReportID 
                            long uuidLsb = characteristicUuid.getLeastSignificantBits();
                            long originalLsb = HidConstants.HID_REPORT_UUID.getLeastSignificantBits();
                            byte derivedReportId = (byte)(uuidLsb - originalLsb);
                            
                            if (reportId == derivedReportId) {
                                Log.d(TAG, "Found matching report ID " + reportId + " for UUID: " + characteristicUuid);
                                return characteristic;
                            }
                        }
                    }
                }
            }
        }
        
        Log.e(TAG, "Characteristic not found: " + characteristicUuid);
        return null;
    }
    
    /**
     * Gets the GATT server.
     *
     * @return The GATT server
     */
    public BluetoothGattServer getGattServer() {
        return gattServer;
    }
    
    /**
     * Gets the BLE notifier.
     *
     * @return The BLE notifier
     */
    public BleNotifier getNotifier() {
        return notifier;
    }
    
    /**
     * Cleans up resources.
     */
    public void close() {
        stop();
        services.clear();
        serviceHandlers.clear();
    }
    
    /**
     * GATT server callback.
     */
    private final BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "Device connected to GATT server: " + device.getAddress());
                    lifecycleManager.getConnectionManager().onDeviceConnected(device);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "Device disconnected from GATT server: " + device.getAddress());
                    lifecycleManager.getConnectionManager().onDeviceDisconnected(device);
                }
            } else {
                Log.e(TAG, "GATT connection state change failed: " + status);
            }
        }
        
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, 
                int offset, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicReadRequest: " + characteristic.getUuid());
            
            UUID serviceUuid = characteristic.getService().getUuid();
            ServiceHandler handler = serviceHandlers.get(serviceUuid);
            
            if (handler != null) {
                byte[] response = handler.handleCharacteristicRead(
                        characteristic.getUuid(), offset);
                
                if (response != null) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 
                            offset, response);
                } else {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 
                            offset, null);
                }
            } else {
                Log.w(TAG, "No handler for service: " + serviceUuid);
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 
                        offset, null);
            }
        }
        
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattCharacteristic characteristic, boolean preparedWrite,
                boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, "onCharacteristicWriteRequest: " + characteristic.getUuid());
            
            UUID serviceUuid = characteristic.getService().getUuid();
            ServiceHandler handler = serviceHandlers.get(serviceUuid);
            
            int status = BluetoothGatt.GATT_FAILURE;
            
            if (handler != null) {
                boolean success = handler.handleCharacteristicWrite(
                        characteristic.getUuid(), value);
                
                if (success) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    // Update the characteristic value
                    characteristic.setValue(value);
                }
            } else {
                Log.w(TAG, "No handler for service: " + serviceUuid);
            }
            
            if (responseNeeded) {
                gattServer.sendResponse(device, requestId, status, offset, value);
            }
        }
        
        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                int offset, BluetoothGattDescriptor descriptor) {
            Log.d(TAG, "onDescriptorReadRequest: " + descriptor.getUuid());
            
            UUID serviceUuid = descriptor.getCharacteristic().getService().getUuid();
            ServiceHandler handler = serviceHandlers.get(serviceUuid);
            
            if (handler != null) {
                byte[] response = handler.handleDescriptorRead(
                        descriptor.getUuid(), descriptor.getCharacteristic().getUuid());
                
                if (response != null) {
                    gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 
                            offset, response);
                    return;
                }
            }
            
            // Default handling for common descriptors
            if (descriptor.getUuid().equals(HidConstants.CLIENT_CONFIG_UUID)) {
                // Return current CCCD value
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 
                        offset, descriptor.getValue());
            } else {
                Log.w(TAG, "Unhandled descriptor read: " + descriptor.getUuid());
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 
                        offset, null);
            }
        }
        
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                BluetoothGattDescriptor descriptor, boolean preparedWrite,
                boolean responseNeeded, int offset, byte[] value) {
            Log.d(TAG, "onDescriptorWriteRequest: " + descriptor.getUuid());
            
            UUID serviceUuid = descriptor.getCharacteristic().getService().getUuid();
            ServiceHandler handler = serviceHandlers.get(serviceUuid);
            
            int status = BluetoothGatt.GATT_FAILURE;
            
            if (handler != null) {
                boolean success = handler.handleDescriptorWrite(
                        descriptor.getUuid(), descriptor.getCharacteristic().getUuid(), value);
                
                if (success) {
                    status = BluetoothGatt.GATT_SUCCESS;
                    // Update the descriptor value
                    descriptor.setValue(value);
                }
            } else if (descriptor.getUuid().equals(HidConstants.CLIENT_CONFIG_UUID)) {
                // Default handling for CCCD
                descriptor.setValue(value);
                status = BluetoothGatt.GATT_SUCCESS;
                
                // Check if notifications are being enabled/disabled
                if (value.length == 2) {
                    boolean enabled = (value[0] == 0x01 && value[1] == 0x00);
                    String state = enabled ? "enabled" : "disabled";
                    Log.d(TAG, "Notifications " + state + " for " + 
                          descriptor.getCharacteristic().getUuid());
                    
                    // Update the notifier if available
                    if (notifier != null) {
                        notifier.setNotificationsEnabled(
                                descriptor.getCharacteristic().getUuid(), enabled);
                    }
                }
            } else {
                Log.w(TAG, "Unhandled descriptor write: " + descriptor.getUuid());
            }
            
            if (responseNeeded) {
                gattServer.sendResponse(device, requestId, status, offset, value);
            }
        }
        
        @Override
        public void onMtuChanged(BluetoothDevice device, int mtu) {
            Log.d(TAG, "onMtuChanged: " + mtu);
        }
        
        @Override
        public void onServiceAdded(int status, BluetoothGattService service) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Service added successfully: " + service.getUuid());
            } else {
                Log.e(TAG, "Failed to add service: " + service.getUuid() + ", status: " + status);
            }
        }
        
        @Override
        public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
            Log.d(TAG, "onExecuteWrite: " + execute);
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
        }
    };
    
    /**
     * Interface for handling GATT service operations.
     */
    public interface ServiceHandler {
        /**
         * Handles a characteristic read request.
         *
         * @param characteristicUuid The UUID of the characteristic
         * @param offset The offset to read from
         * @return The value to return, or null if the request should fail
         */
        byte[] handleCharacteristicRead(UUID characteristicUuid, int offset);
        
        /**
         * Handles a characteristic write request.
         *
         * @param characteristicUuid The UUID of the characteristic
         * @param value The value to write
         * @return true if successful, false otherwise
         */
        boolean handleCharacteristicWrite(UUID characteristicUuid, byte[] value);
        
        /**
         * Handles a descriptor read request.
         *
         * @param descriptorUuid The UUID of the descriptor
         * @param characteristicUuid The UUID of the characteristic
         * @return The value to return, or null if the request should fail
         */
        byte[] handleDescriptorRead(UUID descriptorUuid, UUID characteristicUuid);
        
        /**
         * Handles a descriptor write request.
         *
         * @param descriptorUuid The UUID of the descriptor
         * @param characteristicUuid The UUID of the characteristic
         * @param value The value to write
         * @return true if successful, false otherwise
         */
        boolean handleDescriptorWrite(UUID descriptorUuid, UUID characteristicUuid, byte[] value);
    }
}
