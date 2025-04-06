package com.example.blehid.unity;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import com.example.blehid.unity.events.AdvertisingStateChangedEvent;
import com.example.blehid.unity.events.DeviceConnectedEvent;
import com.example.blehid.unity.events.DeviceDisconnectedEvent;
import com.example.blehid.unity.events.DeviceDisconnectedEvent.DisconnectReason;
import com.example.blehid.unity.events.EventRegistry;
import com.example.blehid.unity.events.PairingFailedEvent;
import com.example.blehid.unity.events.PairingFailedEvent.PairingFailureReason;
import com.example.blehid.unity.events.PairingRequestedEvent;
import com.example.blehid.unity.stubs.UnityPlayerStub;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridge between the Java event system and Unity.
 * Implements the UnityCallback interface and translates calls into events,
 * then forwards them to Unity via UnitySendMessage.
 */
public class UnityEventBridge implements UnityCallback {
    private static final String TAG = "UnityEventBridge";

    // Singleton instance
    private static UnityEventBridge instance;
    
    // GameObject name in Unity that will receive callbacks
    private String gameObjectName;
    
    // Map of event IDs to type info for Unity
    private final ConcurrentHashMap<UUID, String> pendingEvents;
    
    /**
     * Private constructor to prevent direct instantiation.
     */
    private UnityEventBridge() {
        pendingEvents = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the singleton instance of the bridge.
     * 
     * @return The bridge instance
     */
    public static synchronized UnityEventBridge getInstance() {
        if (instance == null) {
            instance = new UnityEventBridge();
        }
        return instance;
    }
    
    /**
     * Sets the name of the Unity GameObject that will receive callbacks.
     * 
     * @param gameObjectName Name of the GameObject
     */
    public void setUnityGameObject(String gameObjectName) {
        this.gameObjectName = gameObjectName;
        Log.d(TAG, "Set Unity GameObject name: " + gameObjectName);
    }
    
    /**
     * Gets the name of the Unity GameObject that receives callbacks.
     * 
     * @return GameObject name
     */
    public String getUnityGameObject() {
        return gameObjectName;
    }
    
    /**
     * Sends a message to Unity.
     * 
     * @param methodName Name of the method to call
     * @param message Message to send
     */
    private void sendToUnity(String methodName, String message) {
        if (gameObjectName == null || gameObjectName.isEmpty()) {
            Log.e(TAG, "Cannot send to Unity: no GameObject name set");
            return;
        }
        
        try {
            UnityPlayerStub.UnitySendMessage(gameObjectName, methodName, message);
            Log.d(TAG, "Sent to Unity: " + methodName + " - " + message);
        } catch (Exception e) {
            Log.e(TAG, "Error sending to Unity", e);
        }
    }

    @Override
    public void onPairingRequested(String deviceAddress, int variant) {
        PairingRequestedEvent event = new PairingRequestedEvent(deviceAddress, variant);
        
        // Store event in pending events
        pendingEvents.put(event.getEventId(), "PairingRequested");
        
        // Fire the event
        EventRegistry.getInstance().fireEvent(event);
        
        // Send to Unity
        sendToUnity("OnPairingRequested", 
                String.format("%s|%d|%s", 
                        deviceAddress, 
                        variant,
                        event.getEventId().toString()));
    }

    @Override
    public void onDeviceConnected(String deviceAddress) {
        DeviceConnectedEvent event = new DeviceConnectedEvent(deviceAddress);
        
        // Store event in pending events
        pendingEvents.put(event.getEventId(), "DeviceConnected");
        
        // Fire the event
        EventRegistry.getInstance().fireEvent(event);
        
        // Send to Unity
        sendToUnity("OnDeviceConnected", 
                String.format("%s|%s", 
                        deviceAddress,
                        event.getEventId().toString()));
    }

    @Override
    public void onDeviceDisconnected(String deviceAddress) {
        // Default to unknown reason when not specified
        DeviceDisconnectedEvent event = new DeviceDisconnectedEvent(
                deviceAddress, DisconnectReason.UNKNOWN);
        
        // Store event in pending events
        pendingEvents.put(event.getEventId(), "DeviceDisconnected");
        
        // Fire the event
        EventRegistry.getInstance().fireEvent(event);
        
        // Send to Unity
        sendToUnity("OnDeviceDisconnected", 
                String.format("%s|%s|%s", 
                        deviceAddress,
                        DisconnectReason.UNKNOWN.name(),
                        event.getEventId().toString()));
    }

    @Override
    public void onPairingFailed(String deviceAddress) {
        // Default to unknown reason when not specified
        PairingFailedEvent event = new PairingFailedEvent(
                deviceAddress, PairingFailureReason.UNKNOWN);
        
        // Store event in pending events
        pendingEvents.put(event.getEventId(), "PairingFailed");
        
        // Fire the event
        EventRegistry.getInstance().fireEvent(event);
        
        // Send to Unity
        sendToUnity("OnPairingFailed", 
                String.format("%s|%s|%s", 
                        deviceAddress,
                        PairingFailureReason.UNKNOWN.name(),
                        event.getEventId().toString()));
    }

    @Override
    public void onAdvertisingStateChanged(boolean isAdvertising) {
        AdvertisingStateChangedEvent event = new AdvertisingStateChangedEvent(isAdvertising);
        
        // Store event in pending events
        pendingEvents.put(event.getEventId(), "AdvertisingStateChanged");
        
        // Fire the event
        EventRegistry.getInstance().fireEvent(event);
        
        // Send to Unity
        sendToUnity("OnAdvertisingStateChanged", 
                String.format("%b|%s", 
                        isAdvertising,
                        event.getEventId().toString()));
    }
    
    /**
     * Called by Unity to get more details about an event.
     * 
     * @param eventId The UUID of the event as a string
     * @return JSON representation of the event details, or null if not found
     */
    public String getEventDetails(String eventIdStr) {
        try {
            UUID eventId = UUID.fromString(eventIdStr);
            String eventType = pendingEvents.get(eventId);
            
            if (eventType == null) {
                Log.w(TAG, "Event not found in pending events: " + eventId);
                return null;
            }
            
            // Find the event in the registry
            EventRegistry registry = EventRegistry.getInstance();
            return registry.findEventById(eventId).toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting event details", e);
            return null;
        }
    }
    
    /**
     * Clears an event from the pending events map.
     * 
     * @param eventIdStr The UUID of the event as a string
     */
    public void clearEvent(String eventIdStr) {
        try {
            UUID eventId = UUID.fromString(eventIdStr);
            pendingEvents.remove(eventId);
            Log.d(TAG, "Cleared event: " + eventId);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing event", e);
        }
    }
    
    /**
     * Static method to access the setUnityGameObject method from Unity.
     * 
     * @param gameObjectName Name of the GameObject
     */
    public static void setUnityGameObjectStatic(String gameObjectName) {
        getInstance().setUnityGameObject(gameObjectName);
    }
    
    /**
     * Static method to access the getEventDetails method from Unity.
     * 
     * @param eventIdStr The UUID of the event as a string
     * @return JSON representation of the event details, or null if not found
     */
    public static String getEventDetailsStatic(String eventIdStr) {
        return getInstance().getEventDetails(eventIdStr);
    }
    
    /**
     * Static method to access the clearEvent method from Unity.
     * 
     * @param eventIdStr The UUID of the event as a string
     */
    public static void clearEventStatic(String eventIdStr) {
        getInstance().clearEvent(eventIdStr);
    }
}
