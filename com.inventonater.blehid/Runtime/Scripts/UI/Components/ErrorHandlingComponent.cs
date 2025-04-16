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

        // Accessibility state
        private bool hasAccessibilityError = false;
        private string accessibilityErrorMessage = "Accessibility Service is required for local device control";
        private bool accessibilityServiceEnabled = false;
        private bool checkingAccessibilityService = false;

        // External reference
        private MonoBehaviour owner;

        // Error styling
        private readonly Color permissionErrorColor = new Color(0.8f, 0.2f, 0.2f, 1.0f);
        private readonly Color accessibilityErrorColor = new Color(0.8f, 0.4f, 0.0f, 1.0f);

        // Public properties
        public bool HasPermissionError => hasPermissionError;
        public bool HasAccessibilityError => hasAccessibilityError;

        public ErrorHandlingComponent(MonoBehaviour owner)
        {
            this.owner = owner;

            // In editor mode, initially set accessibility error to true
            // so we can show the accessibility UI for testing
            if (!IsEditorMode) return;
            hasAccessibilityError = true;
            Logger.AddLogEntry("Editor mode: Simulating accessibility service not enabled for testing");
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
            checkingAccessibilityService = true;
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

            // Header
            GUILayout.Label("Accessibility Service Required", GUIStyle.none);
            GUILayout.Space(5);

            if (checkingAccessibilityService)
            {
                GUILayout.Label("Checking accessibility service status...", GUIStyle.none);
            }
            else
            {
                // Show error message
                GUILayout.Label(accessibilityErrorMessage, GUIStyle.none);

                if (fullUI)
                {
                    DrawFullAccessibilityUI();
                }
                else
                {
                    DrawSimpleAccessibilityNotice();
                }
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
            GUILayout.Label("Go to the Local tab to enable this feature", GUIStyle.none);
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
                // Use the improved method with fallback mechanism
                if (BleHidLocalControl.Instance != null)
                {
                    BleHidLocalControl.Instance.OpenAccessibilitySettings();
                }

                // Schedule a check after a delay to see if the settings change
                if (owner != null)
                {
                    owner.StartCoroutine(DelayedAccessibilityCheck(3.0f));
                }
            }
            catch (Exception e)
            {
                Logger.AddLogEntry("Error opening accessibility settings: " + e.Message);
            }

            // In editor mode, simulate enabling the accessibility service
            Logger.AddLogEntry("Editor mode: Simulating enabling accessibility service");
            hasAccessibilityError = false;
            accessibilityServiceEnabled = true;
            Logger.AddLogEntry("Editor mode: Accessibility service enabled successfully");
        }


        /// <summary>
        /// Coroutine to check accessibility service status
        /// </summary>
        private IEnumerator CheckAccessibilityServiceCoroutine()
        {
            yield return null; // Wait a frame to let UI update

            bool isEnabled = false;

            if (IsEditorMode)
            {
                isEnabled = BleHidLocalControl.CheckAccessibilityServiceEnabledDirect();
                if (!isEnabled)
                {
                    string errorMsg;
                    isEnabled = BleHidEnvironmentChecker.CheckAccessibilityServiceEnabled(out errorMsg);

                    if (!isEnabled) Logger.AddLogEntry("Accessibility service check failed: " + errorMsg);
                }
            }
            else
            {
                isEnabled = !hasAccessibilityError;
            }

            accessibilityServiceEnabled = isEnabled;
            hasAccessibilityError = !isEnabled;

            Logger.AddLogEntry(isEnabled ? "Accessibility service is enabled" : "Accessibility service is not enabled");

            checkingAccessibilityService = false;

            // Add exponential backoff for repeated checks when service is not enabled
            if (!isEnabled && owner != null)
            {
                // Schedule a delayed check with exponential backoff up to a maximum
                // If the UI is already periodically checking, this will just add an extra check
                owner.StartCoroutine(DelayedAccessibilityCheck(2.0f)); // Add an extra check after 2 seconds
            }
        }

        /// <summary>
        /// Coroutine to check missing permissions
        /// </summary>
        private IEnumerator CheckMissingPermissionsCoroutine()
        {
            yield return null; // Wait a frame to let UI update

            // Get missing permissions
            if (IsEditorMode)
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
                // Editor mode simulation
                missingPermissions.Clear();
                hasPermissionError = false;
            }

            checkingPermissions = false;
        }

        /// <summary>
        /// Check accessibility service status after a delay
        /// </summary>
        /// <param name="delaySeconds">How long to wait before checking</param>
        private IEnumerator DelayedAccessibilityCheck(float delaySeconds)
        {
            // Wait for the specified delay
            yield return new WaitForSeconds(delaySeconds);

            // Check accessibility status
            Logger.AddLogEntry("Performing delayed accessibility service check");
            CheckAccessibilityServiceStatus();
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
