package com.example.blehid.unity.events;

/**
 * Event fired when the advertising state changes.
 */
public class AdvertisingStateChangedEvent extends BaseEvent {
    private final boolean isAdvertising;
    
    /**
     * Creates a new advertising state changed event.
     * 
     * @param isAdvertising Whether the device is currently advertising
     */
    public AdvertisingStateChangedEvent(boolean isAdvertising) {
        super(EventType.ADVERTISING_STATE_CHANGED);
        this.isAdvertising = isAdvertising;
    }
    
    /**
     * Gets whether the device is currently advertising.
     * 
     * @return True if advertising, false otherwise
     */
    public boolean isAdvertising() {
        return isAdvertising;
    }
    
    @Override
    public String toString() {
        return String.format("%s[isAdvertising=%b]", super.toString(), isAdvertising);
    }
}
