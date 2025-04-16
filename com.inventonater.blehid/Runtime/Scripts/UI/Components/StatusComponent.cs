using UnityEngine;
using System;

namespace Inventonater.BleHid
{
    /// <summary>
    /// UI component for displaying BLE HID status and advertising controls
    /// </summary>
    public class StatusComponent
    {
        private bool isInitialized = false;
        private BleHidManager BleHidManager => BleHidManager.Instance;
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
            if (BleHidManager.IsConnected)
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
                if (IsEditorMode) GUILayout.Label("EDITOR MODE: UI visible but BLE functions disabled");
            }
        }

        private void DrawAdvertisingButton()
        {
            if (isInitialized || IsEditorMode)
            {
                string[] labels = { BleHidManager.IsAdvertising ? "Stop Advertising" : "Start Advertising" };
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
            if (BleHidManager.IsAdvertising) BleHidManager.BleAdvertiser.StopAdvertising();
            else BleHidManager.BleAdvertiser.StartAdvertising();
        }
    }
}
