using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// Handles error UI for permissions and accessibility
    /// </summary>
public class ErrorHandlingComponent : UIComponent
{
    /// <summary>
    /// Implementation of the abstract DrawUI method.
    /// This class has specialized drawing methods called directly by the main UI.
    /// </summary>
    public override void DrawUI()
    {
        // This component uses specialized drawing methods that are called directly
        // from the main UI class based on error conditions:
        // - DrawPermissionErrorUI()
        // - DrawAccessibilityErrorUI(bool)
    }
    
    private bool hasPermissionError = false;
        private string permissionErrorMessage = "";
        private List<BleHidPermissionHandler.AndroidPermission> missingPermissions = new List<BleHidPermissionHandler.AndroidPermission>();
        private bool checkingPermissions = false;
        
        private bool hasAccessibilityError = false;
        private string accessibilityErrorMessage = "Accessibility Service is required for local device control";
        private bool accessibilityServiceEnabled = false;
        private bool checkingAccessibilityService = false;
        
        private MonoBehaviour owner;
        
        public bool HasPermissionError => hasPermissionError;
        public bool HasAccessibilityError => hasAccessibilityError;
        
        public override void Initialize(BleHidManager bleHidManager, LoggingManager logger, bool isEditorMode)
        {
            base.Initialize(bleHidManager, logger, isEditorMode);
            
            // In editor mode, initially set accessibility error to true
            // so we can show the accessibility UI for testing
            if (isEditorMode)
            {
                hasAccessibilityError = true;
                Logger.AddLogEntry("Editor mode: Simulating accessibility service not enabled for testing");
            }
        }
        
        public void SetMonoBehaviourOwner(MonoBehaviour owner)
        {
            this.owner = owner;
        }
        
        public void SetPermissionError(string errorMessage)
        {
            hasPermissionError = true;
            permissionErrorMessage = errorMessage;
            Logger.AddLogEntry("Permission error: " + errorMessage);
            
            // Check permissions
            CheckMissingPermissions();
        }
        
        public void SetAccessibilityError(bool hasError)
        {
            hasAccessibilityError = hasError;
            if (!hasError)
            {
                Logger.AddLogEntry("Accessibility service is enabled");
            }
            else
            {
                Logger.AddLogEntry("Accessibility service is not enabled");
            }
        }
        
        /// <summary>
        /// Check accessibility service status using the environment checker
        /// </summary>
        public void CheckAccessibilityServiceStatus()
        {
            if (owner == null) return;
            
            checkingAccessibilityService = true;
            owner.StartCoroutine(CheckAccessibilityServiceCoroutine());
        }
        
        private IEnumerator CheckAccessibilityServiceCoroutine()
        {
            yield return null; // Wait a frame to let UI update
            
            // First try using the direct method that doesn't require initialization
            bool isEnabled = BleHidLocalControl.CheckAccessibilityServiceEnabledDirect();
            
            // If that fails, fall back to the environment checker
            if (!isEnabled)
            {
                string errorMsg;
                isEnabled = BleHidEnvironmentChecker.CheckAccessibilityServiceEnabled(out errorMsg);
                
                if (!isEnabled)
                {
                    Logger.AddLogEntry("Accessibility service check failed: " + errorMsg);
                }
            }
            
            // Update accessibility status
            accessibilityServiceEnabled = isEnabled;
            hasAccessibilityError = !isEnabled;
            
            if (isEnabled)
            {
                Logger.AddLogEntry("Accessibility service is enabled");
            }
            else
            {
                Logger.AddLogEntry("Accessibility service is not enabled");
            }
            
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
        /// Draw the permission error UI with instructions
        /// </summary>
        public void DrawPermissionErrorUI()
        {
            // Create a style for the error box
            GUIStyle errorStyle = UIHelper.CreateErrorStyle(new Color(0.8f, 0.2f, 0.2f, 1.0f));
            
            GUILayout.BeginVertical(errorStyle);
            
            // Header
            GUILayout.Label("Missing Permissions", GUIStyle.none);
            GUILayout.Space(5);
            
            if (checkingPermissions)
            {
                GUILayout.Label("Checking permissions...", GUIStyle.none);
            }
            else if (missingPermissions.Count > 0)
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
                    GUILayout.Label($"• {permission.Name}: {permission.Description}", GUIStyle.none, GUILayout.Width(Screen.width * 0.6f));
                    
                    if (GUILayout.Button("Request", GUILayout.Height(40)))
                    {
                        Logger.AddLogEntry($"Requesting permission: {permission.Name}");
                        if (owner != null)
                        {
                            owner.StartCoroutine(RequestSinglePermission(permission));
                        }
                    }
                    GUILayout.EndHorizontal();
                }
                
                // Open settings button
                GUILayout.Space(10);
                GUILayout.Label("If permission requests don't work, try granting them manually:", GUIStyle.none);
                
                if (GUILayout.Button("Open App Settings", GUILayout.Height(50)))
                {
                    Logger.AddLogEntry("Opening app settings");
                    BleHidPermissionHandler.OpenAppSettings();
                }
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
            // Create a style for the error box
            GUIStyle errorStyle = UIHelper.CreateErrorStyle(new Color(0.8f, 0.4f, 0.0f, 1.0f)); // Orange for accessibility
            
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
                    // Full UI with detailed instructions
                    GUILayout.Space(10);
                    
                    if (GUILayout.Button("Open Accessibility Settings", GUILayout.Height(60)))
                    {
                        Logger.AddLogEntry("Opening accessibility settings");
                        
                        #if UNITY_ANDROID && !UNITY_EDITOR
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
                        #else
                        // In editor mode, simulate enabling the accessibility service
                        Logger.AddLogEntry("Editor mode: Simulating enabling accessibility service");
                        hasAccessibilityError = false;
                        accessibilityServiceEnabled = true;
                        Logger.AddLogEntry("Editor mode: Accessibility service enabled successfully");
                        #endif
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
                    // Simple notification with link to Local tab
                    GUILayout.Space(5);
                    GUILayout.Label("Go to the Local tab to enable this feature", GUIStyle.none);
                }
            }
            
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Check for missing permissions and update the UI
        /// </summary>
        public void CheckMissingPermissions()
        {
            if (owner == null) return;
            
            checkingPermissions = true;
            owner.StartCoroutine(CheckMissingPermissionsCoroutine());
        }
        
        private IEnumerator CheckMissingPermissionsCoroutine()
        {
            yield return null; // Wait a frame to let UI update
            
            // Get missing permissions
            #if UNITY_ANDROID && !UNITY_EDITOR
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
            #endif
            
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
            #if UNITY_ANDROID && !UNITY_EDITOR
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
                    yield return owner.StartCoroutine(BleHidManager.Initialize());
                }
            }
            #else
            yield return null;
            #endif
        }
    }
}
