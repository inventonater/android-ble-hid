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
        
        public void CheckAccessibilityServiceStatus()
        {
            if (owner == null) return;
            
            checkingAccessibilityService = true;
            owner.StartCoroutine(CheckAccessibilityServiceCoroutine());
        }
        
        private IEnumerator CheckAccessibilityServiceCoroutine()
        {
            yield return null; // Wait a frame to let UI update
            
            #if UNITY_ANDROID && !UNITY_EDITOR
            if (BleHidLocalControl.Instance != null)
            {
                try
                {
                    accessibilityServiceEnabled = BleHidLocalControl.Instance.IsAccessibilityServiceEnabled();
                    hasAccessibilityError = !accessibilityServiceEnabled;
                    
                    if (accessibilityServiceEnabled)
                    {
                        Logger.AddLogEntry("Accessibility service is enabled");
                    }
                    else
                    {
                        Logger.AddLogEntry("Accessibility service is not enabled");
                    }
                }
                catch (Exception e)
                {
                    hasAccessibilityError = true;
                    Logger.AddLogEntry("Error checking accessibility service: " + e.Message);
                }
            }
            else
            {
                hasAccessibilityError = true;
                Logger.AddLogEntry("BleHidLocalControl instance not available");
            }
            #endif
            
            checkingAccessibilityService = false;
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
                        #if UNITY_ANDROID && !UNITY_EDITOR
                        if (BleHidLocalControl.Instance != null)
                        {
                            BleHidLocalControl.Instance.OpenAccessibilitySettings();
                            Logger.AddLogEntry("Opening accessibility settings");
                        }
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
