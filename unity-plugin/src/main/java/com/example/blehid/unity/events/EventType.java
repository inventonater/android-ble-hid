package com.example.blehid.unity.events;

/**
 * Enum defining all possible event types in the BLE HID system.
 * Used for event identification and routing.
 */
public enum EventType {
    PAIRING_REQUESTED,
    DEVICE_CONNECTED,
    DEVICE_DISCONNECTED,
    PAIRING_FAILED,
    ADVERTISING_STATE_CHANGED,
    
    // System events
    INITIALIZATION_COMPLETED,
    INITIALIZATION_FAILED,
    
    // Connection events
    CONNECTION_LOST,
    RECONNECTION_STARTED,
    RECONNECTION_SUCCEEDED,
    RECONNECTION_FAILED,
    
    // Command execution events
    COMMAND_SUCCEEDED,
    COMMAND_FAILED
}
