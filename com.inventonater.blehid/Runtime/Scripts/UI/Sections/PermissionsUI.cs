using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Handles error UI for permissions and accessibility
    /// </summary>
    public class PermissionsUI
    {
        private BleHidManager BleHidManager => BleHidManager.Instance;
        private LoggingManager Logger => LoggingManager.Instance;
        private bool IsEditorMode => Application.isEditor;
        private float nextPermissionCheckTime = 0f;

        // Permission state
        private bool hasPermissionError = false;
        private string permissionErrorMessage = "";
        private List<BleHidPermissionHandler.AndroidPermission> missingPermissions = new();

        // Notification permission state
        private bool hasNotificationPermissionError = false;
        private string notificationErrorMessage = "Notification permission is needed for app notifications";
        private bool checkingNotificationPermission = false;

        // Accessibility state
        private bool hasAccessibilityError = false;
        private string accessibilityErrorMessage = "Accessibility Service is required for local device control";

        // External reference
        private MonoBehaviour owner;

        // Error styling
        private readonly Color permissionErrorColor = new Color(0.8f, 0.2f, 0.2f, 1.0f);
        private readonly Color accessibilityErrorColor = new Color(0.8f, 0.4f, 0.0f, 1.0f);
        private readonly Color notificationErrorColor = new Color(0.3f, 0.3f, 0.8f, 1.0f);

        // Public properties
        public bool HasPermissionError => hasPermissionError;
        public bool HasAccessibilityError => hasAccessibilityError;
        public bool HasNotificationPermissionError => hasNotificationPermissionError;
        private void OpenAppSettings() => BleHidPermissionHandler.OpenAppSettings();

        public PermissionsUI(MonoBehaviour owner)
        {
            this.owner = owner;
            
            // Always initialize with accessibility error to ensure the UI appears
            hasAccessibilityError = true;
            Logger.AddLogEntry("Initializing accessibility service status check");
            
            CheckAccessibilityServiceStatus();
        }

        public void InitialCheck()
        {
            // Check all permissions - accessibility status is already being checked in the constructor
            CheckMissingPermissions();
            CheckNotificationPermissionStatus();

            // Perform an extra check to ensure the accessibility service status is detected
            // This helps on devices where the first check might not be reliable
            CheckAccessibilityServiceStatus();
            Logger.AddLogEntry("Performing startup accessibility service check");
        }


        public void Update()
        {
            PerformPeriodicPermissionChecks();
        }

        private const float PERMISSION_CHECK_INTERVAL = 3.0f; // Check every 3 seconds

        private void PerformPeriodicPermissionChecks()
        {
            // Check if we need to check permissions
            if (!HasPermissionError &&
                !HasAccessibilityError &&
                !HasNotificationPermissionError) return;

            if (Time.time < nextPermissionCheckTime) return;

            // Schedule next check
            nextPermissionCheckTime = Time.time + PERMISSION_CHECK_INTERVAL;

            // Check permissions
            if (HasPermissionError)
            {
                CheckMissingPermissions();
                LoggingManager.Instance.AddLogEntry("Periodic permission check");
            }

            // Check accessibility service
            if (HasAccessibilityError)
            {
                CheckAccessibilityServiceStatus();
                LoggingManager.Instance.AddLogEntry("Periodic accessibility check");
            }

            // Check notification permission
            if (HasNotificationPermissionError)
            {
                CheckNotificationPermissionStatus();
                LoggingManager.Instance.AddLogEntry("Periodic notification permission check");
            }
        }

        public void DrawErrorWarnings()
        {
            // Permission error warning - show at the top with a red background
            if (HasPermissionError)
            {
                DrawPermissionErrorUI();
                GUILayout.Space(20);
            }

            // Accessibility error - always show full UI at the top with other permissions
            if (HasAccessibilityError)
            {
                DrawAccessibilityErrorUI(true); // Show full UI with button
                GUILayout.Space(20);
            }

            // Notification permission error - show at the top but don't block functionality
            if (HasNotificationPermissionError)
            {
                DrawNotificationPermissionErrorUI();
                GUILayout.Space(20);
            }
        }

        public bool HasCriticalErrors()
        {
            // Only treat regular permissions as blocking errors
            // Accessibility errors are shown at the top but don't block the UI completely
            return HasPermissionError;
        }


        /// <summary>
        /// Set a permission error
        /// </summary>
        public void SetPermissionError(string errorMessage)
        {
            hasPermissionError = true;
            permissionErrorMessage = errorMessage;
            Logger.AddLogEntry("Permission error: " + errorMessage);

            CheckMissingPermissions();
        }
        
        /// <summary>
        /// Set notification permission error state
        /// </summary>
        public void SetNotificationPermissionError(bool hasError, string errorMessage = null)
        {
            hasNotificationPermissionError = hasError;
            if (errorMessage != null)
            {
                notificationErrorMessage = errorMessage;
            }
            
            Logger.AddLogEntry(hasError
                ? "Notification permission not granted: " + notificationErrorMessage
                : "Notification permission granted");
        }

        /// <summary>
        /// Set accessibility error state
        /// </summary>
        public void SetAccessibilityError(bool hasError)
        {
            hasAccessibilityError = hasError;
            Logger.AddLogEntry(hasError
                ? "Accessibility service is not enabled"
                : "Accessibility service is enabled");
        }

        /// <summary>
        /// Check accessibility service status using the environment checker
        /// </summary>
        public void CheckAccessibilityServiceStatus()
        {
            owner.StartCoroutine(CheckAccessibilityServiceCoroutine());
        }

        /// <summary>
        /// Check for missing permissions and update the UI
        /// </summary>
        public void CheckMissingPermissions()
        {
            owner.StartCoroutine(CheckMissingPermissionsCoroutine());
        }
        
        /// <summary>
        /// Check notification permission status
        /// </summary>
        public void CheckNotificationPermissionStatus()
        {
            checkingNotificationPermission = true;
            owner.StartCoroutine(CheckNotificationPermissionCoroutine());
        }


        /// <summary>
        /// Draw the notification permission error UI with instructions
        /// </summary>
        public void DrawNotificationPermissionErrorUI()
        {
            GUIStyle errorStyle = UIHelper.CreateErrorStyle(notificationErrorColor);

            GUILayout.BeginVertical(errorStyle);

            // Header
            GUILayout.Label("Notification Permission", GUIStyle.none);
            GUILayout.Space(5);

            if (checkingNotificationPermission)
            {
                GUILayout.Label("Checking notification permission status...", GUIStyle.none);
            }
            else
            {
                // Show error message
                GUILayout.Label(notificationErrorMessage, GUIStyle.none);
                GUILayout.Space(10);
                
                if (GUILayout.Button("Request Notification Permission", GUILayout.Height(50)))
                {
                    RequestNotificationPermission();
                }
                
                GUILayout.Space(5);
                
                if (GUILayout.Button("Open App Settings", GUILayout.Height(40)))
                {
                    OpenAppSettings();
                }
            }

            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Draw the permission error UI with instructions
        /// </summary>
        public void DrawPermissionErrorUI()
        {
            GUIStyle errorStyle = UIHelper.CreateErrorStyle(permissionErrorColor);

            GUILayout.BeginVertical(errorStyle);

            // Header
            DrawPermissionErrorHeader();

            if (missingPermissions.Count > 0)
            {
                DrawMissingPermissionsList();
            }
            else
            {
                GUILayout.Label("Permission error occurred but no missing permissions were found.", GUIStyle.none);
                GUILayout.Label("This could be a temporary issue. Please try again.", GUIStyle.none);
            }

            // Retry button
            GUILayout.Space(10);

            if (GUILayout.Button("Check Permissions Again", GUILayout.Height(60)))
            {
                // Re-check permissions
                CheckMissingPermissions();
                Logger.AddLogEntry("Rechecking permissions...");
            }

            GUILayout.EndVertical();
        }

        /// <summary>
        /// Draw the accessibility error UI with instructions
        /// </summary>
        public void DrawAccessibilityErrorUI(bool fullUI)
        {
            GUIStyle errorStyle = UIHelper.CreateErrorStyle(accessibilityErrorColor);

            GUILayout.BeginVertical(errorStyle);

            GUILayout.Label("Accessibility Service Required", GUIStyle.none);
            GUILayout.Space(5);

            GUILayout.Label(accessibilityErrorMessage, GUIStyle.none);

            if (fullUI)
            {
                GUILayout.Space(10);

                if (GUILayout.Button("Open Accessibility Settings", GUILayout.Height(60)))
                {
                    OpenAccessibilitySettings();
                }

                // Detailed instructions
                GUILayout.Space(10);
                GUILayout.Label("To enable the accessibility service:", GUIStyle.none);
                GUILayout.Label("1. Tap 'Open Accessibility Settings'", GUIStyle.none);
                GUILayout.Label("2. Find 'BLE HID' in the list", GUIStyle.none);
                GUILayout.Label("3. Toggle it ON", GUIStyle.none);
                GUILayout.Label("4. Accept the permissions", GUIStyle.none);
            }
            else
            {
                GUILayout.Space(5);
                GUILayout.Label("The accessibility service is required for local device control", GUIStyle.none);
                GUILayout.Label("Look for the 'Open Accessibility Settings' button at the top of the screen", GUIStyle.none);
            }

            GUILayout.EndVertical();
        }

        private void DrawPermissionErrorHeader()
        {
            GUILayout.Label("Missing Permissions", GUIStyle.none);
            GUILayout.Space(5);
        }

        private void DrawMissingPermissionsList()
        {
            // Show general error message
            GUILayout.Label(permissionErrorMessage, GUIStyle.none);
            GUILayout.Space(10);

            // List each missing permission with a request button
            GUILayout.Label("The following permissions are required:", GUIStyle.none);
            GUILayout.Space(5);

            foreach (var permission in missingPermissions)
            {
                GUILayout.BeginHorizontal();

                GUILayout.Label($"• {permission.Name}: {permission.Description}",
                    GUIStyle.none, GUILayout.Width(Screen.width * 0.6f));

                if (GUILayout.Button("Request", GUILayout.Height(40)))
                {
                    Logger.AddLogEntry($"Requesting permission: {permission.Name}");
                    owner.StartCoroutine(RequestSinglePermission(permission));
                }

                GUILayout.EndHorizontal();
            }

            // Open settings button
            GUILayout.Space(10);
            GUILayout.Label("If permission requests don't work, try granting them manually:", GUIStyle.none);

            if (GUILayout.Button("Open App Settings", GUILayout.Height(50)))
            {
                Logger.AddLogEntry("Opening app settings");
                OpenAppSettings();
            }
        }

        private void OpenAccessibilitySettings()
        {
            Logger.AddLogEntry("Opening accessibility settings");

            try
            {
                BleHidLocalControl.Instance.OpenAccessibilitySettings();
            }
            catch (Exception e)
            {
                Logger.AddLogEntry("Error opening accessibility settings: " + e.Message);
            }
        }


        /// <summary>
        /// Coroutine to check accessibility service status
        /// </summary>
        private IEnumerator CheckAccessibilityServiceCoroutine()
        {
            yield return new WaitForEndOfFrame();
            var accessibilityServiceEnabled = BleHidLocalControl.CheckAccessibilityServiceEnabledDirect();
            Logger.AddLogEntry("Accessibility service status: " + (accessibilityServiceEnabled ? "ENABLED" : "NOT ENABLED"));
            hasAccessibilityError = !accessibilityServiceEnabled;
        }

        /// <summary>
        /// Coroutine to check missing permissions
        /// </summary>
        private IEnumerator CheckMissingPermissionsCoroutine()
        {
            yield return null; // Wait a frame to let UI update

            if (IsEditorMode)
            {
                missingPermissions.Clear();
                hasPermissionError = false;
                yield break;
            }

            missingPermissions = BleHidPermissionHandler.GetMissingPermissions();

            if (missingPermissions.Count > 0)
            {
                string missingList = string.Join(", ", missingPermissions.Select(p => p.Name).ToArray());
                Logger.AddLogEntry($"Missing permissions: {missingList}");
                hasPermissionError = true;
            }
            else
            {
                Logger.AddLogEntry("All required permissions are granted");
                hasPermissionError = false;
            }
        }
        
        /// <summary>
        /// Coroutine to check notification permission status
        /// </summary>
        private IEnumerator CheckNotificationPermissionCoroutine()
        {
            yield return null; // Wait a frame to let UI update

            if (!IsEditorMode)
            {
                bool isGranted = BleHidPermissionHandler.CheckNotificationPermission();
                
                hasNotificationPermissionError = !isGranted;
                Logger.AddLogEntry(isGranted 
                    ? "Notification permission is granted" 
                    : "Notification permission is not granted");
            }
            else
            {
                // In editor mode or below Android 13, we don't need this permission
                hasNotificationPermissionError = false;
            }

            checkingNotificationPermission = false;
        }

        /// <summary>
        /// Request notification permission
        /// </summary>
        private void RequestNotificationPermission()
        {
            Logger.AddLogEntry("Requesting notification permission");
            owner.StartCoroutine(RequestNotificationPermissionCoroutine());
        }
        
        /// <summary>
        /// Coroutine to request notification permission
        /// </summary>
        private IEnumerator RequestNotificationPermissionCoroutine()
        {
            yield return owner.StartCoroutine(BleHidPermissionHandler.RequestNotificationPermission());
            
            // Re-check permission after request
            yield return new WaitForSeconds(0.5f);
            CheckNotificationPermissionStatus();
        }

        /// <summary>
        /// Request a single permission and update the UI
        /// </summary>
        private IEnumerator RequestSinglePermission(BleHidPermissionHandler.AndroidPermission permission)
        {
            yield return owner.StartCoroutine(BleHidPermissionHandler.RequestAndroidPermission(permission.PermissionString));

            // Re-check permissions after the request
            yield return new WaitForSeconds(0.5f);
            CheckMissingPermissions();

            // If all permissions have been granted, try initializing again
            if (missingPermissions.Count == 0)
            {
                hasPermissionError = false;
                permissionErrorMessage = "";
                Logger.AddLogEntry("All permissions granted. Initializing...");
                if (BleHidManager != null)
                {
                    yield return owner.StartCoroutine(BleHidManager.BleInitializer.Initialize());
                }
            }
        }

    }
}
