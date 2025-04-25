using UnityEngine;
using System;

namespace Inventonater.BleHid
{
    /// <summary>
    /// UI component for displaying BLE HID status and advertising controls
    /// </summary>
    public class StatusUI
    {
        private bool _isInitialized = false;
        private readonly ConnectionBridge _connectionBridge;
        public StatusUI(ConnectionBridge connectionBridge) => _connectionBridge = connectionBridge;
        private bool IsEditorMode => Application.isEditor;

        public void SetInitialized(bool initialized) => _isInitialized = initialized;

        public void DrawUI()
        {
            UIHelper.BeginSection("Connection Status");
            DrawStatusInfo();
            DrawConnectionInfo();
            DrawAdvertisingButton();
            DrawConnectedDevice();
            UIHelper.EndSection();
        }

        private void DrawStatusInfo()
        {
            GUILayout.Label("Status: " + (_isInitialized ? "Ready" : "Initializing..."));
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
            if (_isInitialized || IsEditorMode)
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

        private void DrawConnectedDevice()
        {
            if (!_connectionBridge.IsConnected) return;
            if (string.IsNullOrEmpty(_connectionBridge.ConnectedDeviceAddress)) return;

            var style = new GUIStyle(GUI.skin.box);
            style.normal.background = UIHelper.MakeColorTexture(new Color(0.0f, 0.5f, 0.0f, 0.2f));

            GUILayout.BeginVertical(style);
            if (GUILayout.Button("Disconnect", GUILayout.Height(50))) _connectionBridge.Disconnect();

            GUILayout.BeginHorizontal();
            GUILayout.Label("Connected Device:", UIHelper.BoldStyle, GUILayout.Width(150));
            GUILayout.Label(_connectionBridge.ConnectedDeviceName ?? "Unknown Device", new GUIStyle(GUI.skin.label) { fontStyle = FontStyle.Bold, normal = { textColor = Color.green } });
            GUILayout.EndHorizontal();

            GUILayout.BeginHorizontal();
            GUILayout.Label("Address:", GUILayout.Width(150));
            GUILayout.Label(_connectionBridge.ConnectedDeviceAddress);
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();

            GUILayout.Space(10);
        }
    }
}
