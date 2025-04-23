using UnityEngine;
using System;

namespace Inventonater.BleHid
{
    /// <summary>
    /// UI component for displaying BLE HID status and advertising controls
    /// </summary>
    public class StatusUI
    {
        private bool isInitialized = false;
        private ConnectionBridge _connectionBridge;
        public StatusUI(ConnectionBridge connectionBridge) => _connectionBridge = connectionBridge;
        private bool IsEditorMode => Application.isEditor;

        public void SetInitialized(bool initialized) => isInitialized = initialized;

        public void DrawUI()
        {
            UIHelper.BeginSection("Connection Status");
            DrawStatusInfo();
            DrawConnectionInfo();
            DrawAdvertisingButton();
            UIHelper.EndSection();
        }

        private void DrawStatusInfo()
        {
            GUILayout.Label("Status: " + (isInitialized ? "Ready" : "Initializing..."));
        }

        private void DrawConnectionInfo()
        {
            if (_connectionBridge.IsConnected)
            {
                // Connected - show device details
                GUILayout.Label("Connected to: " + _connectionBridge.ConnectedDeviceName);
                GUILayout.Label("Device: " + _connectionBridge.ConnectedDeviceName +
                                " (" + _connectionBridge.ConnectedDeviceAddress + ")");
            }
            else
            {
                // Not connected - show appropriate message
                GUILayout.Label("Not connected");
                if (IsEditorMode) GUILayout.Label("EDITOR MODE: UI visible but BLE functions disabled");
            }
        }

        private void DrawAdvertisingButton()
        {
            if (isInitialized || IsEditorMode)
            {
                string[] labels = { _connectionBridge.IsAdvertising ? "Stop Advertising" : "Start Advertising" };
                Action[] actions = { ToggleAdvertising };
                string[] messages = { "Advertising toggle (not functional in editor)" };

                UIHelper.ActionButtonRow(
                    labels,
                    actions,
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

        private void ToggleAdvertising()
        {
            if (_connectionBridge.IsAdvertising) _connectionBridge.StopAdvertising();
            else _connectionBridge.StartAdvertising();
        }
    }
}
