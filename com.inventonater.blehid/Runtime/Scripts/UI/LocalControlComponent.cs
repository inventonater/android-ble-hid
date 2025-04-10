using System;
using System.Collections;
using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// UI component for local device control functionality
    /// </summary>
    public class LocalControlComponent : UIComponent
    {
        private bool localControlInitialized = false;
        private bool hasCameraPermission = false;
        private MonoBehaviour owner;
        
        // Camera control parameters with default values
        // private float _cameraButtonX = 0.5f; // Center horizontally
        // private float _cameraButtonY = 0.92f; // 92% down the screen
        // private int _cameraTapDelay = 3500; // Default tap delay in ms
        // private int _cameraReturnDelay = 1500; // Default return delay in ms
        // private int _acceptDialogDelay = 300; // Delay before tapping accept dialog in ms
        // private float _acceptButtonXOffset = 0.2f; // X offset from center (0.2 = 20% right)
        // private float _acceptButtonYOffset = 0.05f; // Y offset from center (0.05 = 5% down)
        
        public void SetMonoBehaviourOwner(MonoBehaviour owner)
        {
            this.owner = owner;
        }
        
        public override void DrawUI()
        {
            // Initialize if not already done
            if (!localControlInitialized && owner != null)
            {
                owner.StartCoroutine(InitializeLocalControl());
            }
            
            // Check if we have an initialized instance
            bool canUseLocalControls = false;
            bool accessibilityEnabled = false;
            
            if (IsEditorMode)
            {
                // In editor mode, we simulate that local controls are available
                canUseLocalControls = true;
                accessibilityEnabled = true;
            }
            else
            {
                #if UNITY_ANDROID
                try
                {
                    var instance = BleHidLocalControl.Instance;
                    if (instance != null)
                    {
                        canUseLocalControls = true;
                        try
                        {
                            accessibilityEnabled = instance.IsAccessibilityServiceEnabled();
                        }
                        catch (Exception)
                        {
                            // IsAccessibilityServiceEnabled might throw if not fully initialized
                            accessibilityEnabled = false;
                        }
                    }
                }
                catch (Exception)
                {
                    canUseLocalControls = false;
                }
                #endif
            }
            
            if (!canUseLocalControls && !IsEditorMode)
            {
                // Show initializing status with better styling
                GUILayout.BeginVertical();
                GUILayout.Label("Local Device Control", GUI.skin.box);
                GUILayout.BeginVertical(GUI.skin.box);
                GUILayout.Label("Initializing local control...", GUI.skin.box);
                GUILayout.Space(10);
                GUILayout.Label("Please wait while the local control features are being initialized...");
                GUILayout.EndVertical();
                GUILayout.EndVertical();
                return;
            }
            
            // Always display the UI, even when accessibility service is not enabled
            // If accessibility is not enabled, the parent UI manager will show an error dialog
            // but we'll still show the controls (they will be disabled in the parent UI)
            
            // Add local controls once initialized (or in editor mode)
            GUILayout.BeginVertical();
            GUILayout.Label("Local Device Control", GUI.skin.box);
            
            // Three main sections with consistent spacing
            DrawMediaControlsSection();
            GUILayout.Space(10); // Consistent spacing between sections
            
            DrawCameraControlsSection();
            GUILayout.Space(10); // Consistent spacing between sections
            
            DrawNavigationSection();
            GUILayout.EndVertical();
        }
        
        private void DrawMediaControlsSection()
        {
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Media Controls", GUI.skin.box);
            
            // Media controls row 1
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Previous", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Previous track pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.PreviousTrack();
                    #endif
                }
            }
            
            if (GUILayout.Button("Play/Pause", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Play/Pause pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.PlayPause();
                    #endif
                }
            }
            
            if (GUILayout.Button("Next", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Next track pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.NextTrack();
                    #endif
                }
            }
            GUILayout.EndHorizontal();
            
            // Media controls row 2
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Vol -", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Volume down pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.VolumeDown();
                    #endif
                }
            }
            
            if (GUILayout.Button("Mute", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Mute pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.Mute();
                    #endif
                }
            }
            
            if (GUILayout.Button("Vol +", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Volume up pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.VolumeUp();
                    #endif
                }
            }
            GUILayout.EndHorizontal();
            GUILayout.EndVertical();
        }
        
        private void DrawCameraControlsSection()
        {
            // Camera controls section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Camera Controls", GUI.skin.box);
            
            // Check camera permission
            #if UNITY_ANDROID && !UNITY_EDITOR
            hasCameraPermission = BleHidPermissionHandler.CheckCameraPermission();
            
            if (!hasCameraPermission)
            {
                GUILayout.Space(5);
                GUILayout.Label("Camera permission required for camera features");
                GUILayout.Space(5);
                if (GUILayout.Button("Request Camera Permission", GUILayout.Height(60)))
                {
                    if (owner != null)
                    {
                        owner.StartCoroutine(BleHidPermissionHandler.RequestCameraPermission());
                        Logger.AddLogEntry("Requesting camera permission");
                    }
                }
            }
            else
            #endif
            {
                // Camera parameters UI - simplified to match other sections
                GUILayout.Label("Camera Button Position");
                GUILayout.Space(5); // Consistent spacing

                // Direct launch buttons
                GUILayout.BeginHorizontal();
                if (GUILayout.Button("Launch Camera", GUILayout.Height(60)))
                {
                    if (IsEditorMode)
                    {
                        Logger.AddLogEntry("Launch Camera pressed (not available in editor)");
                    }
                    else
                    {
                        #if UNITY_ANDROID
                        BleHidLocalControl.Instance.LaunchCameraApp();
                        Logger.AddLogEntry("Launching camera app");
                        #endif
                    }
                }
                
                if (GUILayout.Button("Launch Video", GUILayout.Height(60)))
                {
                    if (IsEditorMode)
                    {
                        Logger.AddLogEntry("Launch Video pressed (not available in editor)");
                    }
                    else
                    {
                        #if UNITY_ANDROID
                        BleHidLocalControl.Instance.LaunchVideoCapture();
                        Logger.AddLogEntry("Launching video capture");
                        #endif
                    }
                }
                GUILayout.EndHorizontal();
            }
            
            GUILayout.EndVertical();
        }
        
        private void DrawNavigationSection()
        {
            // Navigation section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Navigation", GUI.skin.box);
            
            // Add consistent spacing at the top of controls
            GUILayout.Space(5);
            
            // Navigation row 1 (Back, Home, Recents)
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Back", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Back pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Back);
                    #endif
                }
            }
            
            if (GUILayout.Button("Home", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Home pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Home);
                    #endif
                }
            }
            
            if (GUILayout.Button("Recents", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Recents pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Recents);
                    #endif
                }
            }
            GUILayout.EndHorizontal();
            
            // Navigation row 2 (Up button)
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("Up", GUILayout.Height(60), GUILayout.Width(Screen.width / 3)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Up pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Up);
                    #endif
                }
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            // Navigation row 3 (Left, Down, Right)
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Left", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Left pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Left);
                    #endif
                }
            }
            
            if (GUILayout.Button("Down", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Down pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Down);
                    #endif
                }
            }
            
            if (GUILayout.Button("Right", GUILayout.Height(60)))
            {
                if (IsEditorMode)
                {
                    Logger.AddLogEntry("Local Right pressed");
                }
                else
                {
                    #if UNITY_ANDROID
                    BleHidLocalControl.Instance.Navigate(BleHidLocalControl.NavigationDirection.Right);
                    #endif
                }
            }
            GUILayout.EndHorizontal();
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Initialize the local control component for Android or safely handle in Editor mode
        /// </summary>
        private IEnumerator InitializeLocalControl()
        {
            if (localControlInitialized)
                yield break;
                
            localControlInitialized = true;
            
            Logger.AddLogEntry("Initializing local control...");
            
            #if UNITY_ANDROID && !UNITY_EDITOR
            // Android-specific initialization
            BleHidLocalControl localControlInstance = null;
            
            try
            {
                localControlInstance = BleHidLocalControl.Instance;
                if (localControlInstance == null)
                {
                    Logger.AddLogEntry("Failed to create local control instance");
                    yield break;
                }
            }
            catch (System.Exception e)
            {
                Logger.AddLogEntry("Error creating local control instance: " + e.Message);
                yield break;
            }
            
            // Now initialize with retries
            yield return owner.StartCoroutine(localControlInstance.Initialize(5));
            
            // Check if initialization was successful
            if (localControlInstance == null || !localControlInstance.IsAccessibilityServiceEnabled())
            {
                Logger.AddLogEntry("Local control initialized, but accessibility service not enabled");
            }
            else
            {
                Logger.AddLogEntry("Local control fully initialized");
            }
            #else
            // Editor-mode initialization
            Logger.AddLogEntry("Editor mode: Local control simulated initialization");
            yield return new WaitForSeconds(0.5f); // Simulate initialization delay
            Logger.AddLogEntry("Editor mode: Local control initialization complete");
            #endif
        }
    }
}
