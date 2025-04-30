package com.inventonater.blehid.core;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BluetoothControl {
    private static final String TAG = "BluetoothEnvValidator";

    private final BluetoothManager bluetoothManager;
    private final BluetoothAdapter bluetoothAdapter;

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BluetoothControl(Context context) {
        this.bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        this.bluetoothAdapter = bluetoothManager.getAdapter();
    }

    public boolean validateAll() {
        if (bluetoothManager == null || bluetoothAdapter == null) {
            Log.e(TAG, "Bluetooth not available on this device");
            return false;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Log.e(TAG, "Bluetooth not enabled");
            return false;
        }

        boolean supported = bluetoothAdapter.isMultipleAdvertisementSupported();
        if (!supported) {
            Log.e(TAG, "BLE peripheral mode not supported");
            return false;
        }

        return true;
    }

    @SuppressLint("MissingPermission")
    public List<Map<String, String>> getBondedDevicesInfo() {
        List<Map<String, String>> deviceInfoList = new ArrayList<>();

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Cannot get bonded devices info: No Bluetooth adapter");
            return deviceInfoList;
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            Map<String, String> deviceInfo = new HashMap<>();
            deviceInfo.put("name", BluetoothControl.getDeviceName(device));
            deviceInfo.put("address", device.getAddress());
            deviceInfo.put("type", BluetoothControl.getDeviceTypeString(device.getType()));
            deviceInfo.put("bondState", BluetoothControl.getBondStateString(device.getBondState()));
            deviceInfo.put("uuids", BluetoothControl.getDeviceUuidsString(device));

            deviceInfoList.add(deviceInfo);
        }

        return deviceInfoList;
    }

    public static String getDeviceTypeString(int type) {
        switch (type) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                return "CLASSIC";
            case BluetoothDevice.DEVICE_TYPE_LE:
                return "LE";
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                return "DUAL";
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
            default:
                return "UNKNOWN";
        }
    }

    public static String getBondStateString(int bondState) {
        switch (bondState) {
            case BluetoothDevice.BOND_BONDED:
                return "BONDED";
            case BluetoothDevice.BOND_BONDING:
                return "BONDING";
            case BluetoothDevice.BOND_NONE:
            default:
                return "NONE";
        }
    }

    @SuppressLint("MissingPermission")
    public static String getDeviceUuidsString(BluetoothDevice device) {
        if (device == null) return "None";
        if (device.getUuids() == null) return "None";
        if (device.getUuids().length == 0) return "None";

        StringBuilder sb = new StringBuilder();
        for (android.os.ParcelUuid uuid : device.getUuids()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(uuid.toString());
        }

        return sb.toString();
    }

    @SuppressLint("MissingPermission")
    public static String getDeviceInfo(BluetoothDevice device) {
        if (device == null) {
            return "null";
        }

        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            deviceName = "Unknown Device";
        }

        return deviceName + " (" + device.getAddress() + ")";
    }

    @SuppressLint("MissingPermission")
    public static String getDeviceName(BluetoothDevice device) {
        if (device == null) {
            return "Unknown Device";
        }

        String deviceName = device.getName();
        if (deviceName == null || deviceName.isEmpty()) {
            return "Unknown Device";
        }

        return deviceName;
    }
}
