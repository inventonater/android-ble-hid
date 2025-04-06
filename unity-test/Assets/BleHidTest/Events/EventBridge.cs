using System;
using System.Collections.Generic;
using UnityEngine;

namespace BleHid.Events
{
    /// <summary>
    /// Bridge between native plugin events and the Unity event system.
    /// Handles message conversion and dispatching.
    /// </summary>
    public class EventBridge : MonoBehaviour
    {
        private static EventBridge instance;
        
        private Dictionary<string, Func<string, string, string, BleHidEvent>> eventFactories = 
            new Dictionary<string, Func<string, string, string, BleHidEvent>>();

        /// <summary>
        /// Gets the singleton instance
        /// </summary>
        public static EventBridge Instance
        {
            get
            {
                if (instance == null)
                {
                    // Try to find an existing instance
                    instance = FindObjectOfType<EventBridge>();
                    
                    // If no instance exists, create one
                    if (instance == null)
                    {
                        GameObject obj = new GameObject("BleHidEventBridge");
                        instance = obj.AddComponent<EventBridge>();
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
            
            // Register event factories
            InitializeEventFactories();
        }
        
        private void InitializeEventFactories()
        {
            // PairingRequested event factory
            eventFactories["PairingRequested"] = (deviceAddress, data, eventIdStr) => 
            {
                // Parse parameters (format: "[address]|[variant]|[eventId]")
                string[] parts = data.Split('|');
                if (parts.Length < 2)
                {
                    Debug.LogError("Invalid PairingRequested event data format");
                    return null;
                }
                
                if (!int.TryParse(parts[1], out int variant))
                {
                    Debug.LogError("Invalid variant in PairingRequested event");
                    return null;
                }
                
                Guid eventId = new Guid(eventIdStr);
                return new PairingRequestedEvent(deviceAddress, variant, eventId, DateTimeOffset.Now.ToUnixTimeMilliseconds());
            };
            
            // DeviceConnected event factory
            eventFactories["DeviceConnected"] = (deviceAddress, data, eventIdStr) => 
            {
                // Format: "[address]|[eventId]"
                Guid eventId = new Guid(eventIdStr);
                return new DeviceConnectedEvent(deviceAddress, eventId, DateTimeOffset.Now.ToUnixTimeMilliseconds());
            };
            
            // DeviceDisconnected event factory
            eventFactories["DeviceDisconnected"] = (deviceAddress, data, eventIdStr) => 
            {
                // Format: "[address]|[reason]|[eventId]"
                string[] parts = data.Split('|');
                if (parts.Length < 2)
                {
                    Debug.LogError("Invalid DeviceDisconnected event data format");
                    return null;
                }
                
                // Parse disconnect reason
                DisconnectReason reason = DisconnectReason.Unknown;
                if (Enum.TryParse(parts[1], out DisconnectReason parsedReason))
                {
                    reason = parsedReason;
                }
                
                Guid eventId = new Guid(eventIdStr);
                return new DeviceDisconnectedEvent(deviceAddress, reason, eventId, DateTimeOffset.Now.ToUnixTimeMilliseconds());
            };
            
            // PairingFailed event factory
            eventFactories["PairingFailed"] = (deviceAddress, data, eventIdStr) => 
            {
                // Format: "[address]|[reason]|[eventId]"
                string[] parts = data.Split('|');
                if (parts.Length < 2)
                {
                    Debug.LogError("Invalid PairingFailed event data format");
                    return null;
                }
                
                // Parse failure reason
                PairingFailureReason reason = PairingFailureReason.Unknown;
                if (Enum.TryParse(parts[1], out PairingFailureReason parsedReason))
                {
                    reason = parsedReason;
                }
                
                Guid eventId = new Guid(eventIdStr);
                return new PairingFailedEvent(deviceAddress, reason, eventId, DateTimeOffset.Now.ToUnixTimeMilliseconds());
            };
            
            // AdvertisingStateChanged event factory
            eventFactories["AdvertisingStateChanged"] = (deviceAddress, data, eventIdStr) => 
            {
                // Format: "[isAdvertising]|[eventId]"
                string[] parts = data.Split('|');
                if (parts.Length < 1)
                {
                    Debug.LogError("Invalid AdvertisingStateChanged event data format");
                    return null;
                }
                
                if (!bool.TryParse(parts[0], out bool isAdvertising))
                {
                    Debug.LogError("Invalid advertising state in AdvertisingStateChanged event");
                    return null;
                }
                
                Guid eventId = new Guid(eventIdStr);
                return new AdvertisingStateChangedEvent(isAdvertising, eventId, DateTimeOffset.Now.ToUnixTimeMilliseconds());
            };
        }
        
        /// <summary>
        /// Called from native code when a pairing request is received.
        /// </summary>
        /// <param name="data">Event data in format: "[address]|[variant]|[eventId]"</param>
        public void OnPairingRequested(string data)
        {
            ProcessEvent("PairingRequested", data);
        }
        
        /// <summary>
        /// Called from native code when a device is connected.
        /// </summary>
        /// <param name="data">Event data in format: "[address]|[eventId]"</param>
        public void OnDeviceConnected(string data)
        {
            ProcessEvent("DeviceConnected", data);
        }
        
        /// <summary>
        /// Called from native code when a device is disconnected.
        /// </summary>
        /// <param name="data">Event data in format: "[address]|[reason]|[eventId]"</param>
        public void OnDeviceDisconnected(string data)
        {
            ProcessEvent("DeviceDisconnected", data);
        }
        
        /// <summary>
        /// Called from native code when pairing fails.
        /// </summary>
        /// <param name="data">Event data in format: "[address]|[reason]|[eventId]"</param>
        public void OnPairingFailed(string data)
        {
            ProcessEvent("PairingFailed", data);
        }
        
        /// <summary>
        /// Called from native code when advertising state changes.
        /// </summary>
        /// <param name="data">Event data in format: "[isAdvertising]|[eventId]"</param>
        public void OnAdvertisingStateChanged(string data)
        {
            ProcessEvent("AdvertisingStateChanged", data);
        }
        
        /// <summary>
        /// Common event processing logic
        /// </summary>
        private void ProcessEvent(string eventType, string data)
        {
            if (string.IsNullOrEmpty(data))
            {
                Debug.LogError($"Received empty data for event type: {eventType}");
                return;
            }
            
            try
            {
                string[] parts = data.Split('|');
                string deviceAddress = null;
                string eventData = data;
                string eventIdStr = null;
                
                // Extract eventId (always the last part)
                if (parts.Length >= 2)
                {
                    eventIdStr = parts[parts.Length - 1];
                    deviceAddress = parts[0];
                }
                
                if (eventIdStr == null)
                {
                    Debug.LogError($"Invalid event data format for {eventType}: {data}");
                    return;
                }
                
                // Create event using factory
                if (eventFactories.TryGetValue(eventType, out var factory))
                {
                    BleHidEvent evt = factory(deviceAddress, data, eventIdStr);
                    if (evt != null)
                    {
                        // Send to event registry
                        BleHidEventRegistry.Instance.FireEvent(evt);
                    }
                }
                else
                {
                    Debug.LogError($"No factory registered for event type: {eventType}");
                }
            }
            catch (Exception ex)
            {
                Debug.LogError($"Error processing {eventType} event: {ex.Message}\nData: {data}\n{ex.StackTrace}");
            }
        }
    }
}
