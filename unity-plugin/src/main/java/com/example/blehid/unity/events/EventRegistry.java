package com.example.blehid.unity.events;

import android.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;

/**
 * Central event registry for managing event listeners and dispatching events.
 * Provides thread-safe operations for event handling.
 */
public class EventRegistry {
    private static final String TAG = "EventRegistry";
    
    // Singleton instance
    private static EventRegistry instance;
    
    // Map of event types to sets of listeners
    private final Map<EventType, Set<EventListener>> listenerMap;
    
    // Event queue for asynchronous processing
    private final LinkedBlockingQueue<BaseEvent> eventQueue;
    
    // Flag indicating if the event processor is running
    private boolean processorRunning;
    
    // Thread for processing events in the queue
    private Thread processorThread;
    
    // List of recent events for debugging and diagnostics
    private final List<BaseEvent> recentEvents;
    private static final int MAX_RECENT_EVENTS = 50;
    
    /**
     * Private constructor to prevent direct instantiation.
     */
    private EventRegistry() {
        listenerMap = new EnumMap<>(EventType.class);
        eventQueue = new LinkedBlockingQueue<>();
        recentEvents = Collections.synchronizedList(new ArrayList<>(MAX_RECENT_EVENTS));
        processorRunning = false;
        
        // Initialize listener sets for all event types
        for (EventType type : EventType.values()) {
            listenerMap.put(type, new CopyOnWriteArraySet<>());
        }
    }
    
    /**
     * Gets the singleton instance of the event registry.
     * 
     * @return The event registry instance
     */
    public static synchronized EventRegistry getInstance() {
        if (instance == null) {
            instance = new EventRegistry();
        }
        return instance;
    }
    
    /**
     * Registers a listener for all event types.
     * 
     * @param listener The listener to register
     */
    public void registerListener(EventListener listener) {
        if (listener == null) {
            Log.w(TAG, "Attempted to register null listener");
            return;
        }
        
        for (Set<EventListener> listeners : listenerMap.values()) {
            listeners.add(listener);
        }
        
        Log.d(TAG, "Registered listener for all event types: " + listener);
    }
    
    /**
     * Registers a listener for a specific event type.
     * 
     * @param listener The listener to register
     * @param eventType The event type to listen for
     */
    public void registerListener(EventListener listener, EventType eventType) {
        if (listener == null) {
            Log.w(TAG, "Attempted to register null listener for event type: " + eventType);
            return;
        }
        
        if (eventType == null) {
            Log.w(TAG, "Attempted to register listener for null event type");
            return;
        }
        
        Set<EventListener> listeners = listenerMap.get(eventType);
        if (listeners != null) {
            listeners.add(listener);
            Log.d(TAG, "Registered listener for event type " + eventType + ": " + listener);
        }
    }
    
    /**
     * Unregisters a listener from all event types.
     * 
     * @param listener The listener to unregister
     */
    public void unregisterListener(EventListener listener) {
        if (listener == null) {
            return;
        }
        
        for (Set<EventListener> listeners : listenerMap.values()) {
            listeners.remove(listener);
        }
        
        Log.d(TAG, "Unregistered listener from all event types: " + listener);
    }
    
    /**
     * Unregisters a listener from a specific event type.
     * 
     * @param listener The listener to unregister
     * @param eventType The event type to stop listening for
     */
    public void unregisterListener(EventListener listener, EventType eventType) {
        if (listener == null || eventType == null) {
            return;
        }
        
        Set<EventListener> listeners = listenerMap.get(eventType);
        if (listeners != null) {
            listeners.remove(listener);
            Log.d(TAG, "Unregistered listener from event type " + eventType + ": " + listener);
        }
    }
    
    /**
     * Fire an event to all registered listeners for its type.
     * Events are processed on the calling thread.
     * 
     * @param event The event to fire
     */
    public void fireEvent(BaseEvent event) {
        if (event == null) {
            Log.w(TAG, "Attempted to fire null event");
            return;
        }
        
        // Add to recent events list
        addToRecentEvents(event);
        
        // Get listeners for this event type
        Set<EventListener> listeners = listenerMap.get(event.getEventType());
        if (listeners == null || listeners.isEmpty()) {
            Log.d(TAG, "No listeners registered for event type: " + event.getEventType());
            return;
        }
        
        // Notify all listeners
        for (EventListener listener : listeners) {
            try {
                listener.onEventReceived(event);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying listener: " + listener, e);
            }
        }
        
        Log.d(TAG, "Fired event: " + event);
    }
    
    /**
     * Queues an event for asynchronous processing.
     * Events are processed on a background thread.
     * 
     * @param event The event to queue
     */
    public void queueEvent(BaseEvent event) {
        if (event == null) {
            Log.w(TAG, "Attempted to queue null event");
            return;
        }
        
        // Add to recent events list
        addToRecentEvents(event);
        
        // Add to queue
        eventQueue.offer(event);
        
        // Ensure processor is running
        ensureProcessorRunning();
        
        Log.d(TAG, "Queued event: " + event);
    }
    
    /**
     * Adds an event to the recent events list.
     * 
     * @param event The event to add
     */
    private synchronized void addToRecentEvents(BaseEvent event) {
        recentEvents.add(event);
        if (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.remove(0);
        }
    }
    
    /**
     * Gets a list of recent events.
     * 
     * @return List of recent events
     */
    public synchronized List<BaseEvent> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }
    
    /**
     * Gets a list of recent events of a specific type.
     * 
     * @param eventType The event type to filter by
     * @return List of recent events of the specified type
     */
    public synchronized List<BaseEvent> getRecentEvents(EventType eventType) {
        List<BaseEvent> result = new ArrayList<>();
        for (BaseEvent event : recentEvents) {
            if (event.getEventType() == eventType) {
                result.add(event);
            }
        }
        return result;
    }
    
    /**
     * Gets a list of recent events that match a predicate.
     * 
     * @param predicate The predicate to filter events by
     * @return List of matching recent events
     */
    public synchronized List<BaseEvent> getRecentEvents(Predicate<BaseEvent> predicate) {
        List<BaseEvent> result = new ArrayList<>();
        for (BaseEvent event : recentEvents) {
            if (predicate.test(event)) {
                result.add(event);
            }
        }
        return result;
    }
    
    /**
     * Ensures the event processor thread is running.
     */
    private synchronized void ensureProcessorRunning() {
        if (!processorRunning) {
            processorRunning = true;
            processorThread = new Thread(this::processEventQueue);
            processorThread.setName("EventProcessorThread");
            processorThread.setDaemon(true);
            processorThread.start();
            Log.d(TAG, "Started event processor thread");
        }
    }
    
    /**
     * Stops the event processor thread.
     */
    public synchronized void shutdown() {
        processorRunning = false;
        if (processorThread != null) {
            processorThread.interrupt();
            processorThread = null;
        }
        eventQueue.clear();
        Log.d(TAG, "Shutdown event registry");
    }
    
    /**
     * Process events from the queue.
     */
    private void processEventQueue() {
        Log.d(TAG, "Event processor started");
        while (processorRunning) {
            try {
                BaseEvent event = eventQueue.take();
                fireEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                Log.d(TAG, "Event processor interrupted");
                break;
            } catch (Exception e) {
                Log.e(TAG, "Error processing event", e);
            }
        }
        Log.d(TAG, "Event processor stopped");
    }
    
    /**
     * Finds an event by its ID in the recent events list.
     * 
     * @param eventId The ID of the event to find
     * @return The event, or null if not found
     */
    public synchronized BaseEvent findEventById(UUID eventId) {
        for (BaseEvent event : recentEvents) {
            if (event.getEventId().equals(eventId)) {
                return event;
            }
        }
        return null;
    }
    
    /**
     * Clears all registered listeners.
     */
    public void clearAllListeners() {
        for (Set<EventListener> listeners : listenerMap.values()) {
            listeners.clear();
        }
        Log.d(TAG, "Cleared all event listeners");
    }
}
