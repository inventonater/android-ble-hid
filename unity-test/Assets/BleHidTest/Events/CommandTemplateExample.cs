using System;
using UnityEngine;
using BleHid.Events;

namespace BleHid
{
    /// <summary>
    /// Example class demonstrating the new command template pattern
    /// integrated with the event system.
    /// </summary>
    public class CommandTemplateExample : MonoBehaviour, IBleHidEventListener
    {
        private BleHidManager bleManager;
        
        private void Start()
        {
            // Get the BleHidManager instance
            bleManager = BleHidManager.Instance;
            
            // Register with the event system
            BleHidEventRegistry.Instance.RegisterListener(this);
        }
        
        private void OnDestroy()
        {
            // Unregister when destroyed
            BleHidEventRegistry.Instance.UnregisterListener(this);
        }
        
        /// <summary>
        /// Example of the new command template pattern using generics.
        /// This significantly reduces code duplication across methods.
        /// </summary>
        /// <typeparam name="T">Return type of the command</typeparam>
        /// <param name="commandName">Name of the command for logging</param>
        /// <param name="command">The command function to execute</param>
        /// <param name="defaultValue">Default value to return if preconditions fail</param>
        /// <returns>The result of the command or defaultValue if failed</returns>
        private T ExecuteCommand<T>(string commandName, Func<T> command, T defaultValue)
        {
            // Check initialization
            if (!bleManager.IsInitialized())
            {
                Debug.LogError($"[CommandTemplate] Cannot execute {commandName}: Not initialized");
                return defaultValue;
            }
            
            // Check peripheral support
            if (!bleManager.IsBlePeripheralSupported())
            {
                Debug.LogWarning($"[CommandTemplate] Cannot execute {commandName}: BLE peripheral not supported");
                return defaultValue;
            }
            
            // Check connection state
            if (!bleManager.IsConnected())
            {
                Debug.LogWarning($"[CommandTemplate] Cannot execute {commandName}: Not connected to host");
                return defaultValue;
            }
            
            // Execute command with proper error handling
            try
            {
                T result = command();
                if (result is bool boolResult)
                {
                    if (boolResult)
                    {
                        Debug.Log($"[CommandTemplate] {commandName} succeeded");
                    }
                    else
                    {
                        Debug.LogError($"[CommandTemplate] {commandName} failed");
                    }
                }
                else
                {
                    Debug.Log($"[CommandTemplate] {commandName} executed, result: {result}");
                }
                return result;
            }
            catch (Exception e)
            {
                Debug.LogError($"[CommandTemplate] Error executing {commandName}: {e.Message}");
                return defaultValue;
            }
        }
        
        /// <summary>
        /// Example usage of the command template for media control
        /// </summary>
        public bool PlayPauseExample()
        {
            return ExecuteCommand<bool>("Play/Pause", () => bleManager.PlayPause(), false);
        }
        
        /// <summary>
        /// Example usage of the command template for mouse movement
        /// </summary>
        public bool MoveMouseExample(int x, int y)
        {
            // Clamp values to valid range
            int clampedX = Mathf.Clamp(x, -127, 127);
            int clampedY = Mathf.Clamp(y, -127, 127);
            
            return ExecuteCommand<bool>("Move Mouse", 
                () => bleManager.MoveMouse(clampedX, clampedY), 
                false);
        }
        
        /// <summary>
        /// Example batch operation with improved error handling
        /// </summary>
        public bool ExecuteBatchExample(Vector2 movement, bool pressLeftButton)
        {
            return ExecuteCommand<bool>("Batch Operation", () => 
            {
                // Start a new batch
                bleManager.StartBatch();
                
                // Add mouse movement
                bleManager.AddMouseMove(movement);
                
                // Add button press if needed
                if (pressLeftButton)
                {
                    bleManager.AddMouseButton(BleHidManager.HidConstants.BUTTON_LEFT, true);
                }
                
                // Execute the batch
                return bleManager.ExecuteBatch();
            }, false);
        }
        
        /// <summary>
        /// Handle events from the event system
        /// </summary>
        public void OnEventReceived(BleHidEvent @event)
        {
            Debug.Log($"Event received: {@event}");
            
            // Handle different event types
            switch (@event.EventType)
            {
                case BleHidEventType.DeviceConnected:
                    Debug.Log("Device connected event received");
                    // Do something when device connects
                    break;
                    
                case BleHidEventType.DeviceDisconnected:
                    if (@event is DeviceDisconnectedEvent disconnectEvent)
                    {
                        Debug.Log($"Device disconnected due to: {disconnectEvent.Reason}");
                        // Handle different disconnect reasons
                        switch (disconnectEvent.Reason)
                        {
                            case DisconnectReason.ConnectionLost:
                                Debug.Log("Connection lost, attempting to reconnect...");
                                break;
                        }
                    }
                    break;
                
                case BleHidEventType.AdvertisingStateChanged:
                    if (@event is AdvertisingStateChangedEvent advertisingEvent)
                    {
                        Debug.Log($"Advertising state changed to: {(advertisingEvent.IsAdvertising ? "Advertising" : "Not advertising")}");
                    }
                    break;
            }
        }
    }
}
