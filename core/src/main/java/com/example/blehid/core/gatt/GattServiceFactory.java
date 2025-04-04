package com.example.blehid.core.gatt;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.example.blehid.core.HidConstants;

/**
 * Factory for creating complete GATT services.
 * Centralizes service creation to ensure consistent configuration.
 */
public class GattServiceFactory {
    private static final String TAG = "GattServiceFactory";
    
    /**
     * Creates a complete HID service with all characteristics.
     *
     * @param includeMouse Whether to include mouse functionality
     * @param includeKeyboard Whether to include keyboard functionality
     * @param includeConsumer Whether to include consumer control functionality
     * @return The created HID service
     */
    public static BluetoothGattService createHidService(
            boolean includeMouse, boolean includeKeyboard, boolean includeConsumer) {
        
        BluetoothGattService hidService = new BluetoothGattService(
                HidConstants.HID_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);
        
        // Add standard characteristics
        hidService.addCharacteristic(CharacteristicFactory.createHidInfoCharacteristic());
        hidService.addCharacteristic(CharacteristicFactory.createReportMapCharacteristic());
        hidService.addCharacteristic(CharacteristicFactory.createHidControlPointCharacteristic());
        hidService.addCharacteristic(CharacteristicFactory.createProtocolModeCharacteristic(
                HidConstants.PROTOCOL_MODE_REPORT));
        
        // Add device-specific characteristics
        if (includeMouse) {
            hidService.addCharacteristic(CharacteristicFactory.createBootMouseInputReportCharacteristic());
            hidService.addCharacteristic(CharacteristicFactory.createMouseReportCharacteristic());
            Log.d(TAG, "Added mouse characteristics to HID service");
        }
        
        if (includeKeyboard) {
            hidService.addCharacteristic(CharacteristicFactory.createKeyboardReportCharacteristic());
            Log.d(TAG, "Added keyboard characteristic to HID service");
        }
        
        if (includeConsumer) {
            hidService.addCharacteristic(CharacteristicFactory.createConsumerReportCharacteristic());
            Log.d(TAG, "Added consumer control characteristic to HID service");
        }
        
        return hidService;
    }
    
    /**
     * Creates a standard combined HID service with mouse, keyboard, and consumer controls.
     *
     * @return The created HID service
     */
    public static BluetoothGattService createStandardHidService() {
        return createHidService(true, true, true);
    }
    
    /**
     * Creates a mouse-only HID service.
     *
     * @return The created HID service
     */
    public static BluetoothGattService createMouseHidService() {
        return createHidService(true, false, false);
    }
    
    /**
     * Creates a keyboard-only HID service.
     *
     * @return The created HID service
     */
    public static BluetoothGattService createKeyboardHidService() {
        return createHidService(false, true, false);
    }
    
    /**
     * Creates a combined keyboard and consumer control HID service.
     *
     * @return The created HID service
     */
    public static BluetoothGattService createKeyboardWithMediaHidService() {
        return createHidService(false, true, true);
    }
}
