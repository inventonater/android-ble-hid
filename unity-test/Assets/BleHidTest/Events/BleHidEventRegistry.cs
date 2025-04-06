using System;
using System.Collections.Generic;
using UnityEngine;

namespace BleHid.Events
{
    /// <summary>
    /// Central event registry for managing event listeners and dispatching events.
    /// Provides thread-safe operations for event handling.
    /// </summary>
    public class BleHidEventRegistry : MonoBehaviour
    {
        private static BleHidEventRegistry instance;
        
        /// <summary>
        /// Dictionary of event types to lists of listeners
        /// </summary>
        private Dictionary<BleHidEventType, List<IBleHidEventListener>> listenerMap = 
            new Dictionary<BleHidEventType, List<IBleHidEventListener>>();
        
        /// <summary>
        /// Queue of events to be processed on the next update
        /// </summary>
        private Queue<BleHidEvent> eventQueue = new Queue<BleHidEvent>();
        
        /// <summary>
        /// List of recent events for debugging and diagnostics
        /// </summary>
        private List<BleHidEvent> recentEvents = new List<BleHidEvent>();
        
        /// <summary>
        /// Maximum number of recent events to keep
        /// </summary>
        private const int MaxRecentEvents = 50;
        
        /// <summary>
        /// Lock object for thread safety
        /// </summary>
        private readonly object syncLock = new object();
        
        /// <summary>
        /// Gets the singleton instance of the registry
        /// </summary>
        public static BleHidEventRegistry Instance
        {
            get
            {
                if (instance == null)
                {
                    // Try to find an existing instance in the scene
                    instance = FindObjectOfType<BleHidEventRegistry>();
                    
                    // If no instance exists, create one
                    if (instance == null)
                    {
                        GameObject obj = new GameObject("BleHidEventRegistry");
                        instance = obj.AddComponent<BleHidEventRegistry>();
                        DontDestroyOnLoad(obj);
                    }
                }
                
                return instance;
            }
        }
        
        private void Awake()
        {
            // Ensure there's only one instance
            if (instance != null && instance != this)
            {
                Destroy(gameObject);
                return;
            }
            
            instance = this;
            DontDestroyOnLoad(gameObject);
            
            // Initialize listener lists for all event types
            foreach (BleHidEventType type in Enum.GetValues(typeof(BleHidEventType)))
            {
                listenerMap[type] = new List<IBleHidEventListener>();
            }
        }
        
        private void Update()
        {
            // Process any queued events
            ProcessEventQueue();
        }
        
        /// <summary>
        /// Registers a listener for all event types
        /// </summary>
        /// <param name="listener">The listener to register</param>
        public void RegisterListener(IBleHidEventListener listener)
        {
            if (listener == null)
            {
                Debug.LogWarning("Attempted to register null listener");
                return;
            }
            
            lock (syncLock)
            {
                foreach (var listeners in listenerMap.Values)
                {
                    if (!listeners.Contains(listener))
                    {
                        listeners.Add(listener);
                    }
                }
            }
            
            Debug.Log($"Registered listener for all event types: {listener}");
        }
        
        /// <summary>
        /// Registers a listener for a specific event type
        /// </summary>
        /// <param name="listener">The listener to register</param>
        /// <param name="eventType">The event type to listen for</param>
        public void RegisterListener(IBleHidEventListener listener, BleHidEventType eventType)
        {
            if (listener == null)
            {
                Debug.LogWarning($"Attempted to register null listener for event type: {eventType}");
                return;
            }
            
            lock (syncLock)
            {
                if (!listenerMap.TryGetValue(eventType, out var listeners))
                {
                    listeners = new List<IBleHidEventListener>();
                    listenerMap[eventType] = listeners;
                }
                
                if (!listeners.Contains(listener))
                {
                    listeners.Add(listener);
                }
            }
            
            Debug.Log($"Registered listener for event type {eventType}: {listener}");
        }
        
        /// <summary>
        /// Unregisters a listener from all event types
        /// </summary>
        /// <param name="listener">The listener to unregister</param>
        public void UnregisterListener(IBleHidEventListener listener)
        {
            if (listener == null)
            {
                return;
            }
            
            lock (syncLock)
            {
                foreach (var listeners in listenerMap.Values)
                {
                    listeners.Remove(listener);
                }
            }
            
            Debug.Log($"Unregistered listener from all event types: {listener}");
        }
        
        /// <summary>
        /// Unregisters a listener from a specific event type
        /// </summary>
        /// <param name="listener">The listener to unregister</param>
        /// <param name="eventType">The event type to stop listening for</param>
        public void UnregisterListener(IBleHidEventListener listener, BleHidEventType eventType)
        {
            if (listener == null)
            {
                return;
            }
            
            lock (syncLock)
            {
                if (listenerMap.TryGetValue(eventType, out var listeners))
                {
                    listeners.Remove(listener);
                }
            }
            
            Debug.Log($"Unregistered listener from event type {eventType}: {listener}");
        }
        
        /// <summary>
        /// Fires an event to all registered listeners for its type.
        /// Events are processed immediately on the calling thread.
        /// </summary>
        /// <param name="event">The event to fire</param>
        public void FireEvent(BleHidEvent @event)
        {
            if (@event == null)
            {
                Debug.LogWarning("Attempted to fire null event");
                return;
            }
            
            // Add to recent events list
            AddToRecentEvents(@event);
            
            // Get listeners for this event type
            List<IBleHidEventListener> listeners;
            lock (syncLock)
            {
                if (!listenerMap.TryGetValue(@event.EventType, out listeners) || listeners.Count == 0)
                {
                    Debug.Log($"No listeners registered for event type: {@event.EventType}");
                    return;
                }
                
                // Make a copy to avoid modification during iteration
                listeners = new List<IBleHidEventListener>(listeners);
            }
            
            // Notify all listeners
            foreach (var listener in listeners)
            {
                try
                {
                    listener.OnEventReceived(@event);
                }
                catch (Exception e)
                {
                    Debug.LogError($"Error notifying listener: {listener}\n{e}");
                }
            }
            
            Debug.Log($"Fired event: {@event}");
        }
        
        /// <summary>
        /// Queues an event for asynchronous processing.
        /// Events are processed on the next Update.
        /// </summary>
        /// <param name="event">The event to queue</param>
        public void QueueEvent(BleHidEvent @event)
        {
            if (@event == null)
            {
                Debug.LogWarning("Attempted to queue null event");
                return;
            }
            
            // Add to recent events list
            AddToRecentEvents(@event);
            
            // Add to queue
            lock (syncLock)
            {
                eventQueue.Enqueue(@event);
            }
            
            Debug.Log($"Queued event: {@event}");
        }
        
        /// <summary>
        /// Adds an event to the recent events list
        /// </summary>
        /// <param name="event">The event to add</param>
        private void AddToRecentEvents(BleHidEvent @event)
        {
            lock (syncLock)
            {
                recentEvents.Add(@event);
                if (recentEvents.Count > MaxRecentEvents)
                {
                    recentEvents.RemoveAt(0);
                }
            }
        }
        
        /// <summary>
        /// Gets a list of recent events
        /// </summary>
        /// <returns>List of recent events</returns>
        public List<BleHidEvent> GetRecentEvents()
        {
            lock (syncLock)
            {
                return new List<BleHidEvent>(recentEvents);
            }
        }
        
        /// <summary>
        /// Gets a list of recent events of a specific type
        /// </summary>
        /// <param name="eventType">The event type to filter by</param>
        /// <returns>List of recent events of the specified type</returns>
        public List<BleHidEvent> GetRecentEvents(BleHidEventType eventType)
        {
            List<BleHidEvent> result = new List<BleHidEvent>();
            
            lock (syncLock)
            {
                foreach (var @event in recentEvents)
                {
                    if (@event.EventType == eventType)
                    {
                        result.Add(@event);
                    }
                }
            }
            
            return result;
        }
        
        /// <summary>
        /// Gets a list of recent events that match a predicate
        /// </summary>
        /// <param name="predicate">The predicate to filter events by</param>
        /// <returns>List of matching recent events</returns>
        public List<BleHidEvent> GetRecentEvents(Predicate<BleHidEvent> predicate)
        {
            List<BleHidEvent> result = new List<BleHidEvent>();
            
            lock (syncLock)
            {
                foreach (var @event in recentEvents)
                {
                    if (predicate(@event))
                    {
                        result.Add(@event);
                    }
                }
            }
            
            return result;
        }
        
        /// <summary>
        /// Process events from the queue
        /// </summary>
        private void ProcessEventQueue()
        {
            // Get all events from the queue
            BleHidEvent[] events;
            lock (syncLock)
            {
                if (eventQueue.Count == 0)
                {
                    return;
                }
                
                events = new BleHidEvent[eventQueue.Count];
                for (int i = 0; i < events.Length; i++)
                {
                    events[i] = eventQueue.Dequeue();
                }
            }
            
            // Process each event
            foreach (var @event in events)
            {
                FireEvent(@event);
            }
        }
        
        /// <summary>
        /// Finds an event by its ID in the recent events list
        /// </summary>
        /// <param name="eventId">The ID of the event to find</param>
        /// <returns>The event, or null if not found</returns>
        public BleHidEvent FindEventById(Guid eventId)
        {
            lock (syncLock)
            {
                foreach (var @event in recentEvents)
                {
                    if (@event.EventId == eventId)
                    {
                        return @event;
                    }
                }
            }
            
            return null;
        }
        
        /// <summary>
        /// Clears all registered listeners
        /// </summary>
        public void ClearAllListeners()
        {
            lock (syncLock)
            {
                foreach (var listeners in listenerMap.Values)
                {
                    listeners.Clear();
                }
            }
            
            Debug.Log("Cleared all event listeners");
        }
    }
}
