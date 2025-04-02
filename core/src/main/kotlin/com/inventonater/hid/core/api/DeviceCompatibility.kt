package com.inventonater.hid.core.api

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService

/**
 * Provides device-specific compatibility adaptations.
 *
 * This interface is responsible for adapting HID services and reports
 * for better compatibility with specific host platforms like macOS,
 * Windows, etc.
 */
interface DeviceCompatibilityManager {
    /**
     * Get the compatibility strategy for a specific device.
     *
     * @param device The connected device
     * @return The appropriate compatibility strategy for the device
     */
    fun getStrategyForDevice(device: BluetoothDevice): CompatibilityStrategy
    
    /**
     * Set a manual override for the device type.
     *
     * @param deviceType The device type to use
     * @return The compatibility strategy for the specified device type
     */
    fun setDeviceTypeOverride(deviceType: DeviceType): CompatibilityStrategy
    
    /**
     * Clear any manual device type override.
     */
    fun clearDeviceTypeOverride()
    
    /**
     * Register a new compatibility strategy.
     *
     * @param deviceType The device type for which this strategy applies
     * @param strategy The compatibility strategy implementation
     */
    fun registerStrategy(deviceType: DeviceType, strategy: CompatibilityStrategy)
    
    /**
     * Get the current device type.
     *
     * @return The detected or overridden device type
     */
    fun getCurrentDeviceType(): DeviceType
}

/**
 * Strategy for device-specific compatibility.
 */
interface CompatibilityStrategy {
    /**
     * Adapt a report map for the specific device.
     *
     * @param reportMap The original report map
     * @return The adapted report map
     */
    fun adaptReportMap(reportMap: ByteArray): ByteArray
    
    /**
     * Get the HID information value for the specific device.
     *
     * @return The HID information byte array
     */
    fun getHidInformation(): ByteArray
    
    /**
     * Get a device-friendly name.
     *
     * @return A name suitable for the specific device
     */
    fun getDeviceName(): String
    
    /**
     * Configure a service for the specific device.
     *
     * @param service The service to configure
     */
    fun configureService(service: BluetoothGattService)
    
    /**
     * Handle a characteristic read request for the specific device.
     *
     * @param characteristic The characteristic being read
     * @return The value to return, or null to use the default value
     */
    fun handleCharacteristicRead(characteristic: BluetoothGattCharacteristic): ByteArray?
    
    /**
     * Adapt a report for the specific device.
     *
     * @param reportId The report ID
     * @param report The original report data
     * @return The adapted report data
     */
    fun adaptReport(reportId: Int, report: ByteArray): ByteArray
}

/**
 * Detector for identifying device types.
 */
interface DeviceDetector {
    /**
     * Detect the type of a connected device.
     *
     * @param device The connected device
     * @return The detected device type
     */
    fun detectDeviceType(device: BluetoothDevice): DeviceType
}

/**
 * Types of host devices.
 */
enum class DeviceType {
    APPLE,
    WINDOWS,
    ANDROID,
    LINUX,
    CHROMEOS,
    UNKNOWN
}
