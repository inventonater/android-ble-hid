package com.example.blehid.app.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.widget.TextView;

import com.example.blehid.app.R;
import com.example.blehid.core.BleAdvertiser;
import com.example.blehid.core.BleHidManager;

/**
 * Manages the diagnostic information display.
 * Handles device information, connection status, and pairing state.
 */
public class DiagnosticsManager {
    private static final String TAG = "DiagnosticsManager";

    public interface Callback {
        void logEvent(String message);
    }

    private final TextView deviceNameText;
    private final TextView deviceAddressText;
    private final TextView pairingStateText;
    private final TextView connectionText;
    private final TextView statusText;
    private final BleHidManager bleHidManager;
    private final Callback callback;

    /**
     * Creates a new DiagnosticsManager.
     *
     * @param deviceNameText TextView for device name
     * @param deviceAddressText TextView for device address
     * @param pairingStateText TextView for pairing state
     * @param connectionText TextView for connection status
     * @param statusText TextView for overall status
     * @param bleHidManager BLE HID manager instance
     * @param callback Callback for logging events
     */
    public DiagnosticsManager(TextView deviceNameText,
                              TextView deviceAddressText,
                              TextView pairingStateText,
                              TextView connectionText,
                              TextView statusText,
                              BleHidManager bleHidManager,
                              Callback callback) {
        this.deviceNameText = deviceNameText;
        this.deviceAddressText = deviceAddressText;
        this.pairingStateText = pairingStateText;
        this.connectionText = connectionText;
        this.statusText = statusText;
        this.bleHidManager = bleHidManager;
        this.callback = callback;
    }

    /**
     * Updates device information for diagnostics.
     */
    public void updateDeviceInfo() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) {
            String deviceName = adapter.getName();
            String deviceAddress = adapter.getAddress();
            
            deviceNameText.setText("Device Name: " + deviceName);
            deviceAddressText.setText("Device Address: " + deviceAddress);
            
            callback.logEvent("LOCAL DEVICE: " + deviceName + " (" + deviceAddress + ")");
        } else {
            deviceNameText.setText("Device Name: Not available");
            deviceAddressText.setText("Device Address: Not available");
        }
    }
    
    /**
     * Updates the pairing state text.
     * 
     * @param state Text description of pairing state
     */
    public void updatePairingState(String state) {
        pairingStateText.setText("Pairing State: " + state);
    }
    
    /**
     * Updates the connection status text.
     */
    public void updateConnectionStatus() {
        if (bleHidManager != null && bleHidManager.isConnected()) {
            BluetoothDevice device = bleHidManager.getConnectedDevice();
            String deviceName = device.getName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = device.getAddress();
            }
            connectionText.setText("Connected to " + deviceName);
            callback.logEvent("CONNECTION: Connected to " + deviceName + " (" + device.getAddress() + ")");
            
            // Check HID profile status for the connected device
            checkHidProfileStatus(device);
        } else {
            connectionText.setText(R.string.not_connected);
        }
    }
    
    /**
     * Updates the advertising status text.
     * 
     * @param isAdvertising Whether advertising is active
     */
    public void updateAdvertisingStatus(boolean isAdvertising) {
        statusText.setText(isAdvertising ? R.string.advertising : R.string.ready);
    }
    
    /**
     * Updates the diagnostic information display with data from BleAdvertiser.
     */
    public void updateDiagnosticInfo() {
        if (bleHidManager != null && bleHidManager.getAdvertiser() != null) {
            BleAdvertiser advertiser = bleHidManager.getAdvertiser();
            
            // Get diagnostic info from the advertiser
            String diagnosticInfo = advertiser.getDiagnosticInfo();
            
            // Add diagnostic info to log
            callback.logEvent("DIAGNOSTIC INFO:\n" + diagnosticInfo);
            
            // Update device capability info
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            boolean peripheralSupported = advertiser.getDeviceReportedPeripheralSupport();
            
            if (adapter != null) {
                deviceNameText.setText("Device Name: " + adapter.getName() +
                                  " (Peripheral Mode: " + (peripheralSupported ? "✅" : "❌") + ")");
            }
        }
    }
    
    /**
     * Check and log HID profile status for a device.
     */
    private void checkHidProfileStatus(BluetoothDevice device) {
        if (device == null) return;
        
        try {
            // Get profile information via reflection
            Class<?> profileClass = Class.forName("android.bluetooth.BluetoothProfile");
            int hidDeviceProfile = BluetoothProfile.HID_DEVICE;
            
            callback.logEvent("CHECKING HID PROFILE: Device " + device.getAddress());
            
            // Get bond state
            int bondState = device.getBondState();
            callback.logEvent("BOND STATE: " + BluetoothReceiver.bondStateToString(bondState));
            
            // Check UUIDs supported by device
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                android.os.ParcelUuid[] uuids = device.getUuids();
                if (uuids != null) {
                    callback.logEvent("DEVICE UUIDS: " + uuids.length + " UUIDs");
                    for (android.os.ParcelUuid uuid : uuids) {
                        callback.logEvent("  - " + uuid.toString());
                        
                        // Check if this is HID UUID
                        if (uuid.toString().toLowerCase().contains("1812")) {
                            callback.logEvent("    (HID SERVICE SUPPORTED)");
                        }
                    }
                } else {
                    callback.logEvent("DEVICE UUIDS: None or not available");
                }
            }
            
            // Try to get device type
            int deviceType = device.getType();
            String typeStr = "";
            switch (deviceType) {
                case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                    typeStr = "CLASSIC";
                    break;
                case BluetoothDevice.DEVICE_TYPE_LE:
                    typeStr = "LE";
                    break;
                case BluetoothDevice.DEVICE_TYPE_DUAL:
                    typeStr = "DUAL";
                    break;
                case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                    typeStr = "UNKNOWN";
                    break;
            }
            callback.logEvent("DEVICE TYPE: " + typeStr);
            
        } catch (Exception e) {
            callback.logEvent("ERROR checking HID profile: " + e.getMessage());
        }
    }
}
