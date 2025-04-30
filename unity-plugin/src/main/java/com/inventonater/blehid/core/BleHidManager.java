package com.inventonater.blehid.core;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.util.Log;

import com.inventonater.blehid.unity.BleHidUnityCallback;


public class BleHidManager {
    private static final String TAG = "BleHidManager";

    private final Context context;
    private final BluetoothControl bluetoothControl;
    private final BluetoothAdapter bluetoothAdapter;
    private final BleAdvertiser advertiser;
    private final BleGattServerManager gattServerManager;
    private final BlePairingManager pairingManager;
    private final BleConnectionManager connectionManager;
    private final HidMediaService hidMediaService;
    private boolean isInitialized = false;
    private BluetoothDevice connectedDevice = null;
    BleHidUnityCallback callback;

    public boolean isConnected() {
        return connectedDevice != null;
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    public BlePairingManager getBlePairingManager() {
        return pairingManager;
    }

    public BleAdvertiser getAdvertiser() {
        return advertiser;
    }

    public BluetoothControl getBluetoothControl() {
        return bluetoothControl;
    }

    public BleHidManager(Context context, BleHidUnityCallback callback) {
        this.context = context.getApplicationContext();
        this.callback = callback;
        this.bluetoothControl = new BluetoothControl(context);
        this.bluetoothAdapter = bluetoothControl.getBluetoothAdapter();
        this.advertiser = new BleAdvertiser(callback, this.context, this.bluetoothAdapter);
        this.gattServerManager = new BleGattServerManager(this);
        this.pairingManager = new BlePairingManager(this);
        this.connectionManager = new BleConnectionManager(this);
        this.hidMediaService = new HidMediaService(this);
    }

    public boolean initialize() {
        if (!bluetoothControl.validateAll()) {
            Log.e(TAG, "Failed to meet initialization prerequisites");
            return false;
        }

        if (!gattServerManager.initialize()) {
            Log.e(TAG, "Failed to initialize GATT server");
            return false;
        }

        if (!hidMediaService.initialize()) {
            Log.e(TAG, "Failed to initialize HID service");
            return false;
        }

        isInitialized = true;
        Log.i(TAG, "BLE HID Manager initialized successfully");
        return isInitialized;
    }

    public boolean startAdvertising() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }

        return advertiser.startAdvertising();
    }

    public void stopAdvertising() {
        if (isInitialized) {
            advertiser.stopAdvertising();
        }
    }

    public boolean setBleIdentity(String identityUuid, String deviceName) {
        if (!isInitialized) {
            Log.e(TAG, "Cannot set identity: Not initialized");
            return false;
        }

        return advertiser.setDeviceIdentity(identityUuid, deviceName);
    }

    @SuppressLint("MissingPermission")
    public boolean isDeviceBonded(String address) {
        if (bluetoothAdapter == null || address == null || address.isEmpty()) {
            return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        return device != null && device.getBondState() == BluetoothDevice.BOND_BONDED;
    }

    @SuppressLint("MissingPermission")
    public boolean removeBond(String address) {
        if (bluetoothAdapter == null || address == null || address.isEmpty()) {
            Log.e(TAG, "Cannot remove bond: Invalid parameters");
            return false;
        }

        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                Log.e(TAG, "Cannot remove bond: Device not found");
                return false;
            }

            if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                Log.i(TAG, "Device not bonded: " + address);
                return true; // Already not bonded
            }

            return pairingManager.removeBond(device);
        } catch (Exception e) {
            Log.e(TAG, "Error removing bond: " + e.getMessage(), e);
            return false;
        }
    }

    protected boolean validateConnectionState() {
        if (!isInitialized) {
            Log.e(TAG, "Not initialized");
            return false;
        }

        if (connectedDevice == null) {
            Log.e(TAG, "No device connected");
            return false;
        }

        return true;
    }

    public void close() {
        stopAdvertising();

        if (gattServerManager != null) {
            gattServerManager.close();
        }

        connectedDevice = null;
        isInitialized = false;

        Log.i(TAG, "BLE HID Manager closed");
    }

    // Getters for internal components

    public Context getContext() {
        return context;
    }

    public BluetoothManager getBluetoothManager() {
        return (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
    }

    public BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    public BleGattServerManager getGattServerManager() {
        return gattServerManager;
    }

    public HidMediaService getHidMediaService() {
        return hidMediaService;
    }

    public BleConnectionManager getConnectionManager() {
        return connectionManager;
    }

    public boolean playPause() {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.playPause();
    }

    public boolean nextTrack() {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.nextTrack();
    }

    public boolean previousTrack() {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.previousTrack();
    }

    public boolean volumeUp() {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.volumeUp();
    }

    public boolean volumeDown() {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.volumeDown();
    }

    public boolean mute() {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.mute();
    }

    public boolean moveMouse(int x, int y) {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.movePointer(x, y);
    }

    public boolean pressMouseButton(int button) {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.pressButton(button);
    }

    public boolean releaseMouseButtons() {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.releaseButtons();
    }

    public boolean releaseMouseButton(int button) {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.releaseButton(button);
    }

    public boolean clickMouseButton(int button) {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.click(button);
    }

    public boolean scrollMouseWheel(int amount) {
        if (!validateConnectionState()) {
            return false;
        }

        // Vertical scrolling is often implemented as mouse movement along the Y axis
        return hidMediaService.movePointer(0, amount);
    }

    public boolean sendCombinedReport(int mediaButtons, int mouseButtons, int x, int y) {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.sendCombinedReport(mediaButtons, mouseButtons, x, y);
    }

    public boolean sendKey(byte keyCode, int modifiers) {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.sendKey(keyCode, modifiers);
    }

    public void releaseAllKeys() {
        if (!validateConnectionState()) {
            return;
        }

        hidMediaService.releaseAllKeys();
    }

    public boolean sendKeys(byte[] keyCodes, int modifiers) {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.sendKeys(keyCodes, modifiers);
    }

    public boolean typeKey(byte keyCode, int modifiers) {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.typeKey(keyCode, modifiers);
    }

    public boolean typeText(String text) {
        if (!validateConnectionState()) {
            return false;
        }

        return hidMediaService.typeText(text);
    }

    void onDeviceConnected(BluetoothDevice device) {
        connectedDevice = device;
        Log.i(TAG, "Device connected: " + BluetoothControl.getDeviceInfo(device));

        // Stop advertising once connected
        stopAdvertising();

        // Create client GATT connection to monitor connection parameters
        if (gattServerManager.createClientConnection(device)) {
            Log.d(TAG, "Created client GATT connection for parameter monitoring");
        }

        // Notify connection manager
        connectionManager.onDeviceConnected(device);

        // Kickstart HID functionality to ensure it works on reconnection
        kickstartHidFunctionality(device);
    }

    private void kickstartHidFunctionality(BluetoothDevice device) {
        Log.i(TAG, "Kickstarting HID functionality for device: " + device.getAddress());

        // 1. Force notification setup for HID characteristics
        gattServerManager.setupHidNotifications(device);

        // 2. Send empty reports to initialize HID state
        if (hidMediaService != null) {
            // Send empty/zero reports for each HID function to initialize the state
            hidMediaService.sendInitialReports();
        }
    }

    void onDeviceDisconnected(BluetoothDevice device) {
        Log.i(TAG, "Device disconnected: " + BluetoothControl.getDeviceInfo(device));
        connectionManager.onDeviceDisconnected();
        connectedDevice = null;
    }

    public void clearConnectedDevice() {
        Log.i(TAG, "Forcing disconnection");
        BluetoothDevice device = connectedDevice;
        connectedDevice = null;
        if (device != null) connectionManager.onDeviceDisconnected();
    }

    public boolean isAdvertising() {
        return advertiser != null && advertiser.isAdvertising();
    }
}
