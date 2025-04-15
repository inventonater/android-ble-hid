using UnityEngine;
using System;

namespace Inventonater.BleHid
{
    /// <summary>
    /// UI component for displaying BLE HID status and advertising controls
    /// </summary>
    public class StatusComponent : UIComponent
    {
        private bool isInitialized = false;


        public void SetInitialized(bool initialized)
        {
            isInitialized = initialized;
        }

        public virtual void DrawUI()
        {
            UIHelper.BeginSection("Connection Status");

            // Status display
            DrawStatusInfo();

            // Connection info
            DrawConnectionInfo();

            // Advertising button
            DrawAdvertisingButton();

            UIHelper.EndSection();
        }


        /// <summary>
        /// Display the current status (initialized or initializing)
        /// </summary>
        private void DrawStatusInfo()
        {
            GUILayout.Label("Status: " + (isInitialized ? "Ready" : "Initializing..."));
        }

        /// <summary>
        /// Display connection information if connected, or "Not Connected" message
        /// </summary>
        private void DrawConnectionInfo()
        {
            if (IsConnected())
            {
                // Connected - show device details
                GUILayout.Label("Connected to: " + BleHidManager.ConnectedDeviceName);
                GUILayout.Label("Device: " + BleHidManager.ConnectedDeviceName +
                                " (" + BleHidManager.ConnectedDeviceAddress + ")");
            }
            else
            {
                // Not connected - show appropriate message
                GUILayout.Label("Not connected");

                if (isEditorMode)
                {
                    GUILayout.Label("EDITOR MODE: UI visible but BLE functions disabled");
                }
            }
        }

        /// <summary>
        /// Draw the advertising control button
        /// </summary>
        private void DrawAdvertisingButton()
        {
            if (CanControlAdvertising())
            {
                string[] labels = { BleHidManager.IsAdvertising ? "Stop Advertising" : "Start Advertising" };
                Action[] actions = { ToggleAdvertising };
                string[] messages = { "Advertising toggle (not functional in editor)" };

                UIHelper.ActionButtonRow(
                    labels,
                    actions,
                    Logger,
                    messages,
                    UIHelper.StandardButtonOptions);
            }
            else
            {
                GUI.enabled = false;
                GUILayout.Button("Start Advertising", UIHelper.StandardButtonOptions);
                GUI.enabled = true;
            }
        }


        /// <summary>
        /// Check if BLE is connected
        /// </summary>
        private bool IsConnected()
        {
            return BleHidManager != null && BleHidManager.IsConnected;
        }

        /// <summary>
        /// Check if advertising can be controlled
        /// </summary>
        private bool CanControlAdvertising()
        {
            return BleHidManager != null && (isInitialized || isEditorMode);
        }

        /// <summary>
        /// Toggle the advertising state
        /// </summary>
        private void ToggleAdvertising()
        {
            if (BleHidManager.IsAdvertising)
                BleHidManager.StopAdvertising();
            else
                BleHidManager.StartAdvertising();
        }
    }
}
