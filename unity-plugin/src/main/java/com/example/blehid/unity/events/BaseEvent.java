package com.example.blehid.unity.events;

import java.util.UUID;

/**
 * Base class for all BLE HID events.
 * Provides common event properties and identity.
 */
public abstract class BaseEvent {
    // Unique identifier for the event instance
    private final UUID eventId;
    
    // Type of the event for routing purposes
    private final EventType eventType;
    
    // Timestamp when the event was created
    private final long timestamp;
    
    /**
     * Creates a new event of the specified type.
     * 
     * @param eventType Type of this event
     */
    protected BaseEvent(EventType eventType) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Gets the unique identifier for this event instance.
     * 
     * @return UUID of this event
     */
    public UUID getEventId() {
        return eventId;
    }
    
    /**
     * Gets the type of this event.
     * 
     * @return Event type
     */
    public EventType getEventType() {
        return eventType;
    }
    
    /**
     * Gets the timestamp when this event was created.
     * 
     * @return Event creation timestamp in milliseconds
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * Creates a string representation of this event.
     */
    @Override
    public String toString() {
        return String.format("%s[id=%s, type=%s, time=%d]", 
            getClass().getSimpleName(), eventId, eventType, timestamp);
    }
}
