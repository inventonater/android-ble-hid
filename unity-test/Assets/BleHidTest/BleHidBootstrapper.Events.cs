using System;
using System.Collections.Generic;
using System.Text;
using UnityEngine;
using UnityEngine.UI;

namespace BleHid
{
    public partial class BleHidBootstrapper
    {
        #region Logging
        private Queue<string> logEntries = new Queue<string>();
        private int maxLogEntries = 100;
        
        private void AddLogEntry(string entry)
        {
            string timestamp = System.DateTime.Now.ToString("HH:mm:ss");
            string logEntry = timestamp + " - " + entry;
            
            // Add to queue
            logEntries.Enqueue(logEntry);
            
            // Remove old entries if we have too many
            while (logEntries.Count > maxLogEntries)
            {
                logEntries.Dequeue();
            }
            
            // Update log text
            UpdateLogText();
        }

        private void UpdateLogText()
        {
            if (logText == null) return;
            
            // Build the log text
            StringBuilder sb = new StringBuilder();
            foreach (string entry in logEntries)
            {
                sb.AppendLine(entry);
            }
            
            // Update the UI
            logText.text = sb.ToString();
            
            // Scroll to bottom
            if (logScrollRect != null)
            {
                Canvas.ForceUpdateCanvases();
                logScrollRect.verticalNormalizedPosition = 0f;
            }
        }
        #endregion

        #region BLE HID Event Handlers
        private void OnInitializeComplete(bool success, string message)
        {
            if (success)
            {
                statusText.text = "Status: Ready";
                advertisingButton.interactable = true;
                AddLogEntry("BLE HID initialized successfully: " + message);
            }
            else
            {
                statusText.text = "Status: Initialization failed";
                advertisingButton.interactable = false;
                AddLogEntry("BLE HID initialization failed: " + message);
            }
        }

        private void OnAdvertisingStateChanged(bool advertising, string message)
        {
            if (advertising)
            {
                statusText.text = "Status: Advertising";
                AddLogEntry("BLE advertising started: " + message);
            }
            else
            {
                statusText.text = "Status: Ready";
                AddLogEntry("BLE advertising stopped: " + message);
            }
        }

        private void OnConnectionStateChanged(bool connected, string deviceName, string deviceAddress)
        {
            if (connected)
            {
                connectionText.text = "Connected to: " + deviceName;
                deviceInfoText.text = "Device: " + deviceName + " (" + deviceAddress + ")";
                AddLogEntry("Device connected: " + deviceName + " (" + deviceAddress + ")");
            }
            else
            {
                connectionText.text = "Not connected";
                deviceInfoText.text = "Device info: N/A";
                AddLogEntry("Device disconnected");
            }
        }

        private void OnPairingStateChanged(string status, string deviceAddress)
        {
            AddLogEntry("Pairing state changed: " + status + (deviceAddress != null ? " (" + deviceAddress + ")" : ""));
        }

        private void OnError(int errorCode, string errorMessage)
        {
            AddLogEntry("Error " + errorCode + ": " + errorMessage);
        }

        private void OnDebugLog(string message)
        {
            AddLogEntry("Debug: " + message);
        }
        #endregion
    }
}
