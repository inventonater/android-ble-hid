package com.inventonater.blehid.core;

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

public class BleHidManager {
    private static final String TAG = "BleHidManager";

    private final Context context;
    private final BluetoothEnvironmentValidator environmentValidator;
    private final BluetoothAdapter bluetoothAdapter;
    private final BleAdvertiser advertiser;
    private final BleGattServerManager gattServerManager;
    private final BlePairingManager pairingManager;
    private final BleConnectionManager connectionManager;
    // Using media service for HID functionality
    private final HidMediaService hidMediaService;

    // Initialization coordinator
    private final BleInitializationCoordinator initializationCoordinator;

    private boolean isInitialized = false;
    private BluetoothDevice connectedDevice = null;

    public BleHidManager(Context context) {
        this.context = context.getApplicationContext();
        this.environmentValidator = new BluetoothEnvironmentValidator(context);
        this.bluetoothAdapter = environmentValidator.getBluetoothAdapter();

        this.advertiser = new BleAdvertiser(this);
        this.gattServerManager = new BleGattServerManager(this);
        this.pairingManager = new BlePairingManager(this);
        this.connectionManager = new BleConnectionManager(this);
        this.hidMediaService = new HidMediaService(this);

        this.initializationCoordinator = new BleInitializationCoordinator(context, gattServerManager, hidMediaService);
    }

    public boolean initialize() {
        boolean success = initializationCoordinator.initialize();
        if (success) {
            isInitialized = true;
            Log.i(TAG, "BLE HID Manager initialized successfully");
        } else {
            Log.e(TAG, "Initialization failed");
        }

        return success;
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

    public List<BluetoothDevice> getBondedDevices() {
        if (bluetoothAdapter == null) {
            Log.e(TAG, "Cannot get bonded devices: No Bluetooth adapter");
            return new ArrayList<>();
        }

        Set<BluetoothDevice> bondedSet = bluetoothAdapter.getBondedDevices();
        return new ArrayList<>(bondedSet);
    }

    public List<Map<String, String>> getBondedDevicesInfo() {
        List<Map<String, String>> deviceInfoList = new ArrayList<>();

        if (bluetoothAdapter == null) {
            Log.e(TAG, "Cannot get bonded devices info: No Bluetooth adapter");
            return deviceInfoList;
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bondedDevices) {
            Map<String, String> deviceInfo = new HashMap<>();
            deviceInfo.put("name", BluetoothDeviceHelper.getDeviceName(device));
            deviceInfo.put("address", device.getAddress());
            deviceInfo.put("type", BluetoothDeviceHelper.getDeviceTypeString(device.getType()));
            deviceInfo.put("bondState", BluetoothDeviceHelper.getBondStateString(device.getBondState()));
            deviceInfo.put("uuids", BluetoothDeviceHelper.getDeviceUuidsString(device));

            deviceInfoList.add(deviceInfo);
        }

        return deviceInfoList;
    }

    public boolean isDeviceBonded(String address) {
        if (bluetoothAdapter == null || address == null || address.isEmpty()) {
            return false;
        }

        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        return device != null && device.getBondState() == BluetoothDevice.BOND_BONDED;
    }

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

    public boolean isBlePeripheralSupported() {
        return environmentValidator.isPeripheralModeSupported();
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
        Log.i(TAG, "Device connected: " + BluetoothDeviceHelper.getDeviceInfo(device));

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
        Log.i(TAG, "Device disconnected: " + BluetoothDeviceHelper.getDeviceInfo(device));

        // Notify connection manager
        connectionManager.onDeviceDisconnected();

        connectedDevice = null;

        // Restart advertising after disconnect
        startAdvertising();
    }

    public boolean isConnected() {
        return connectedDevice != null;
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    public void clearConnectedDevice() {
        Log.i(TAG, "Forcing disconnection");
        BluetoothDevice device = connectedDevice;
        connectedDevice = null;
        if (device != null) connectionManager.onDeviceDisconnected();
    }

    public BlePairingManager getBlePairingManager() {
        return pairingManager;
    }

    public BleAdvertiser getAdvertiser() {
        return advertiser;
    }

    public boolean isAdvertising() {
        return advertiser != null && advertiser.isAdvertising();
    }
}
