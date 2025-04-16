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
    public class ErrorHandlingComponent
    {
        private BleHidManager BleHidManager => BleHidManager.Instance;
        private LoggingManager Logger => LoggingManager.Instance;
        private bool IsEditorMode => Application.isEditor;

        // Permission state
        private bool hasPermissionError = false;
        private string permissionErrorMessage = "";
        private List<BleHidPermissionHandler.AndroidPermission> missingPermissions = new List<BleHidPermissionHandler.AndroidPermission>();
        private bool checkingPermissions = false;
        
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

        public ErrorHandlingComponent(MonoBehaviour owner)
        {
            this.owner = owner;
            
            // Always initialize with accessibility error to ensure the UI appears
            hasAccessibilityError = true;
            Logger.AddLogEntry("Initializing accessibility service status check");
            
            CheckAccessibilityServiceStatus();
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
            checkingPermissions = true;
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

            if (checkingPermissions)
            {
                GUILayout.Label("Checking permissions...", GUIStyle.none);
            }
            else if (missingPermissions.Count > 0)
            {
                DrawMissingPermissionsList();
            }
            else
            {
                DrawGenericPermissionError();
            }

            // Retry button
            DrawPermissionRetryButton();

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

            if (fullUI) DrawFullAccessibilityUI();
            else DrawSimpleAccessibilityNotice();

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
                DrawPermissionRequestRow(permission);
            }

            // Open settings button
            DrawOpenSettingsButton();
        }

        private void DrawPermissionRequestRow(BleHidPermissionHandler.AndroidPermission permission)
        {
            GUILayout.BeginHorizontal();

            GUILayout.Label($"• {permission.Name}: {permission.Description}",
                GUIStyle.none, GUILayout.Width(Screen.width * 0.6f));

            if (GUILayout.Button("Request", GUILayout.Height(40)))
            {
                RequestPermission(permission);
            }

            GUILayout.EndHorizontal();
        }

        private void DrawOpenSettingsButton()
        {
            GUILayout.Space(10);
            GUILayout.Label("If permission requests don't work, try granting them manually:", GUIStyle.none);

            if (GUILayout.Button("Open App Settings", GUILayout.Height(50)))
            {
                Logger.AddLogEntry("Opening app settings");
                OpenAppSettings();
            }
        }

        private void DrawGenericPermissionError()
        {
            GUILayout.Label("Permission error occurred but no missing permissions were found.", GUIStyle.none);
            GUILayout.Label("This could be a temporary issue. Please try again.", GUIStyle.none);
        }

        private void DrawPermissionRetryButton()
        {
            GUILayout.Space(10);

            if (GUILayout.Button("Check Permissions Again", GUILayout.Height(60)))
            {
                // Re-check permissions
                CheckMissingPermissions();
                Logger.AddLogEntry("Rechecking permissions...");
            }
        }

        private void DrawFullAccessibilityUI()
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

        private void DrawSimpleAccessibilityNotice()
        {
            GUILayout.Space(5);
            GUILayout.Label("The accessibility service is required for local device control", GUIStyle.none);
            GUILayout.Label("Look for the 'Open Accessibility Settings' button at the top of the screen", GUIStyle.none);
        }

        private void RequestPermission(BleHidPermissionHandler.AndroidPermission permission)
        {
            Logger.AddLogEntry($"Requesting permission: {permission.Name}");
            owner.StartCoroutine(RequestSinglePermission(permission));
        }

        private void OpenAppSettings()
        {
            BleHidPermissionHandler.OpenAppSettings();
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

            if (!IsEditorMode)
            {
                missingPermissions = BleHidPermissionHandler.GetMissingPermissions();

                // Log the results
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
            else
            {
                missingPermissions.Clear();
                hasPermissionError = false;
            }

            checkingPermissions = false;
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
