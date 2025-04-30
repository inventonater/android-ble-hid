package com.inventonater.blehid.core;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import com.inventonater.blehid.unity.BleHidUnityCallback;

import java.nio.ByteBuffer;
import java.util.UUID;

public class BleAdvertiser {
    private static final String TAG = "BleAdvertiser";

    // Human Interface Device service UUID
    private static final UUID HID_SERVICE_UUID = UUID.fromString("00001812-0000-1000-8000-00805f9b34fb");

    private final BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser bluetoothLeAdvertiser;
    private BleHidUnityCallback callback;

    private boolean isAdvertising = false;
    private String lastErrorMessage = null;
    private Context context;

    private long lastAdvertisingStartTime = 0;
    private int txPowerLevel = AdvertiseSettings.ADVERTISE_TX_POWER_HIGH;

    // Identity management
    private UUID deviceIdentityUuid = null;
    private String customDeviceName = null;
    private static final int MANUFACTURER_ID = 0x0822; // Example ID, consider registering with Bluetooth SIG

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            isAdvertising = true;
            long timeTaken = System.currentTimeMillis() - lastAdvertisingStartTime;
            Log.i(TAG, "✅ BLE Advertising started successfully (took " + timeTaken + "ms)");
            String message = "Advertising started successfully!";
            showToast(message);
            callback.onAdvertisingStateChanged(isAdvertising, message);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            isAdvertising = false;
            String errorMessage = getAdvertiseErrorMessage(errorCode);
            Log.e(TAG, "❌ BLE Advertising failed to start: " + errorMessage);
            showToast("Advertising failed: " + errorMessage);
            lastErrorMessage = errorMessage;
            callback.onAdvertisingStateChanged(isAdvertising, errorMessage);
        }
    };

    public BleAdvertiser(BleHidUnityCallback callback, Context context, BluetoothAdapter bluetoothAdapter) {
        this.bluetoothAdapter = bluetoothAdapter;
        this.context = context;
        this.callback = callback;
        Log.i(TAG, "📱 BleAdvertiser initialized");
    }

    @SuppressLint("MissingPermission")
    public boolean startAdvertising() {
        lastAdvertisingStartTime = System.currentTimeMillis();

        if (isAdvertising) {
            String message = "Already advertising!";
            Log.w(TAG, "⚠️ " + message);
            showToast(message);
            return true; // Already advertising is not a failure
        }

        logDeviceCapabilities();

        try {
            String deviceName = customDeviceName != null ? customDeviceName : "Android HID BLE";
            bluetoothAdapter.setName(deviceName);
            Log.i(TAG, "📱 Device name set to: " + deviceName);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to set device name", e);
        }

        bluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();

        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData advertiseData = buildSimplifiedAdvertiseData(); // Using simplified data
        AdvertiseData scanResponseData = buildSimplifiedScanResponseData(); // Using simplified response

        try {
            Log.i(TAG, "📢 Starting advertising with HID service UUID: " + HID_SERVICE_UUID);
            Log.i(TAG, "📋 Advertise data: " + advertiseDataToString(advertiseData));
            Log.i(TAG, "📋 Scan response: " + advertiseDataToString(scanResponseData));

            bluetoothLeAdvertiser.startAdvertising(settings, advertiseData, scanResponseData, advertiseCallback);

            return true;
        } catch (Exception e) {
            lastErrorMessage = "Failed to start advertising: " + e.getMessage();
            Log.e(TAG, "❌ " + lastErrorMessage, e);
            showToast("Failed to start advertising: " + e.getMessage());
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    private void logDeviceCapabilities() {
        Log.i(TAG, "=== 📱 DEVICE CAPABILITIES ===");
        Log.i(TAG, "Device name: " + bluetoothAdapter.getName());
        Log.i(TAG, "Device address: " + bluetoothAdapter.getAddress());
        Log.i(TAG, "Bluetooth enabled: " + bluetoothAdapter.isEnabled());
        Log.i(TAG, "isMultipleAdvertisementSupported: " + bluetoothAdapter.isMultipleAdvertisementSupported());
        Log.i(TAG, "isOffloadedFilteringSupported: " + bluetoothAdapter.isOffloadedFilteringSupported());
        Log.i(TAG, "isOffloadedScanBatchingSupported: " + bluetoothAdapter.isOffloadedScanBatchingSupported());
        Log.i(TAG, "isLe2MPhySupported: " + bluetoothAdapter.isLe2MPhySupported());
        Log.i(TAG, "isLeCodedPhySupported: " + bluetoothAdapter.isLeCodedPhySupported());
        Log.i(TAG, "isLeExtendedAdvertisingSupported: " + bluetoothAdapter.isLeExtendedAdvertisingSupported());
        Log.i(TAG, "BluetoothLeAdvertiser: " + (bluetoothAdapter.getBluetoothLeAdvertiser() != null ? "Available" : "Not available"));
        Log.i(TAG, "================================");
    }


    private String advertiseDataToString(AdvertiseData data) {
        StringBuilder sb = new StringBuilder("AdvertiseData{");
        sb.append("includeTxPower=").append(data.getIncludeTxPowerLevel());
        sb.append(", includeDeviceName=").append(data.getIncludeDeviceName());
        sb.append(", numServiceUuids=").append(data.getServiceUuids() != null ? data.getServiceUuids().size() : 0);
        sb.append(", numServiceData=").append(data.getServiceData() != null ? data.getServiceData().size() : 0);
        sb.append(", numManufacturerSpecificData=").append(data.getManufacturerSpecificData() != null ? data.getManufacturerSpecificData().size() : 0);
        sb.append('}');
        return sb.toString();
    }

    private void showToast(final String message) {
        try {
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Log.e(TAG, "Error showing toast", e);
        }
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(txPowerLevel)
                .build();

        Log.d(TAG, "🔧 Using advertising settings with power level: " + txPowerLevel);
        return settings;
    }

    private AdvertiseData buildSimplifiedAdvertiseData() {
        // Create the absolute minimal advertising data to avoid size issues
        // We'll use just the 16-bit HID service UUID instead of the full 128-bit one
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder()
                .setIncludeDeviceName(false) // Name is too large for adv packet
                .setIncludeTxPowerLevel(false); // Save bytes

        try {
            // This is a special abbreviated format for standard BLE services
            // Service UUIDs defined in the Bluetooth SIG can use the 16-bit format
            ParcelUuid shortHidUuid = ParcelUuid.fromString("0000" + "1812" + "-0000-1000-8000-00805f9b34fb");
            dataBuilder.addServiceUuid(shortHidUuid);

            // Add manufacturer data with identity UUID if available
            if (deviceIdentityUuid != null) {
                // Convert UUID to bytes (use only first 16 bytes if the byte array is larger)
                byte[] identityBytes = convertUuidToBytes(deviceIdentityUuid);
                dataBuilder.addManufacturerData(MANUFACTURER_ID, identityBytes);
                Log.d(TAG, "📦 Added device identity to manufacturer data: " + deviceIdentityUuid.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error adding advertising data: " + e.getMessage());
        }

        return dataBuilder.build();
    }

    private byte[] convertUuidToBytes(UUID uuid) {
        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
        return buffer.array();
    }

    private AdvertiseData buildSimplifiedScanResponseData() {
        // Minimal scan response - just include device name, no additional data
        AdvertiseData.Builder responseBuilder = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false); // Omit TX power level to save space

        Log.d(TAG, "📦 Created minimal scan response with device name: " + (customDeviceName != null ? customDeviceName : "Default"));
        return responseBuilder.build();
    }

    public boolean setDeviceIdentity(String identityUuid, String deviceName) {
        try {
            if (identityUuid != null && !identityUuid.isEmpty()) {
                this.deviceIdentityUuid = UUID.fromString(identityUuid);
                Log.i(TAG, "📱 Device identity UUID set to: " + identityUuid);
            } else {
                this.deviceIdentityUuid = null;
                Log.w(TAG, "⚠️ No identity UUID provided, using default advertising");
            }

            this.customDeviceName = deviceName;
            Log.i(TAG, "📱 Custom device name set to: " +
                    (deviceName != null ? deviceName : "null (will use default)"));

            // If already advertising, restart to apply new identity
            if (isAdvertising) {
                Log.i(TAG, "🔄 Restarting advertising to apply new identity");
                stopAdvertising();
                return startAdvertising();
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to set device identity", e);
            return false;
        }
    }

    @SuppressLint("MissingPermission")
    public void stopAdvertising() {
        if (!isAdvertising || bluetoothLeAdvertiser == null) {
            return;
        }

        try {
            bluetoothLeAdvertiser.stopAdvertising(advertiseCallback);
            isAdvertising = false;

            String message = "Advertising stopped";
            callback.onAdvertisingStateChanged(isAdvertising, message);
            Log.i(TAG, message);
            showToast(message);
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to stop advertising", e);
        }
    }

    private String getAdvertiseErrorMessage(int errorCode) {
        switch (errorCode) {
            case AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED:
                return "Advertising already started";
            case AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE:
                return "Advertising data too large";
            case AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                return "Advertising feature unsupported";
            case AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR:
                return "Advertising internal error";
            case AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                return "Too many advertisers";
            default:
                return "Unknown error: " + errorCode;
        }
    }

    public boolean isAdvertising() {
        return isAdvertising;
    }

    public boolean setTxPowerLevel(int level) {
        if (level < AdvertiseSettings.ADVERTISE_TX_POWER_ULTRA_LOW ||
                level > AdvertiseSettings.ADVERTISE_TX_POWER_HIGH) {
            Log.e(TAG, "Invalid TX power level: " + level);
            return false;
        }

        this.txPowerLevel = level;
        Log.i(TAG, "TX power level set to: " + level);

        // If already advertising, restart to apply new power level
        if (isAdvertising) {
            Log.i(TAG, "Restarting advertising to apply new TX power level");
            stopAdvertising();
            return startAdvertising();
        }

        return true;
    }
}
