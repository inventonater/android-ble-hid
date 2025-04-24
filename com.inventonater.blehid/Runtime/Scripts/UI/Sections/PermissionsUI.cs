using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class PermissionsUI
    {
        private const float PermissionCheckInterval = 0.5f; // Check every 3 seconds
        private List<PermissionsBridge.AndroidPermission> _missingPermissions = new();
        private bool _hasAccessibilityService;

        private static readonly Color permissionErrorColor = new Color(0.8f, 0.2f, 0.2f, 1.0f);
        private static readonly Color accessibilityErrorColor = new Color(0.8f, 0.4f, 0.0f, 1.0f);
        private static readonly Color notificationErrorColor = new Color(0.3f, 0.3f, 0.8f, 1.0f);
        private float _lastCheckTime;
        private readonly AccessibilityServiceBridge _accessibilityServiceBridge;
        private readonly PermissionsBridge _permissionsBridge;

        public PermissionsUI(PermissionsBridge permissionsBridge, AccessibilityServiceBridge accessibilityServiceBridge)
        {
            _permissionsBridge = permissionsBridge;
            _accessibilityServiceBridge = accessibilityServiceBridge;
        }

        private static GUIStyle AccessibilityError => UIHelper.CreateErrorStyle(accessibilityErrorColor);
        private static GUIStyle PermissionErrorStyle => UIHelper.CreateErrorStyle(permissionErrorColor);


        public void Update()
        {
            if (_hasAccessibilityService && _missingPermissions.Count == 0) return;

            if (Time.time > _lastCheckTime + PermissionCheckInterval) return;
            _lastCheckTime = Time.time;

            _hasAccessibilityService = _accessibilityServiceBridge.IsAccessibilityServiceEnabled();
            _missingPermissions = _permissionsBridge.MissingPermissions.ToList();
        }

        public void DrawIssues()
        {
            if (_missingPermissions.Count > 0) DrawMissingPermissions();
            if (!_hasAccessibilityService) DrawMissingAccessibilityService();
        }

        private void DrawMissingPermissions()
        {
            GUILayout.BeginVertical(PermissionErrorStyle);

            GUILayout.Label("Missing Permissions", GUIStyle.none);
            GUILayout.Space(5);
            foreach (var permission in _missingPermissions)
            {
                GUILayout.BeginHorizontal();
                GUILayout.Label($"• {permission.Name}: {permission.Description}", GUIStyle.none, GUILayout.Width(Screen.width * 0.6f));
                // if (GUILayout.Button("Request Permissions", GUILayout.Height(40))) _bleHidPermissionHandler.RequestPermission(permission);
                GUILayout.EndHorizontal();
            }

            GUILayout.Space(10);
            GUILayout.Label("If permission requests don't work, try granting them manually:", GUIStyle.none);
            if (GUILayout.Button("Open App Settings", GUILayout.Height(50))) _permissionsBridge.OpenAppSettings();
        }

        private void DrawMissingAccessibilityService()
        {
            GUILayout.BeginVertical(AccessibilityError);
            GUILayout.Label("The accessibility service is required for local device control", GUIStyle.none);
            GUILayout.Space(5);
            if (GUILayout.Button("Open Accessibility Settings", GUILayout.Height(60))) _accessibilityServiceBridge.OpenAccessibilitySettings();
            GUILayout.Space(5);
            GUILayout.EndVertical();
        }
    }
}
