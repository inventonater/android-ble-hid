package com.example.blehid.unity.events;

/**
 * Interface for objects that can listen for BLE HID events.
 */
public interface EventListener {
    /**
     * Called when an event occurs.
     * 
     * @param event The event that occurred
     */
    void onEventReceived(BaseEvent event);
}
