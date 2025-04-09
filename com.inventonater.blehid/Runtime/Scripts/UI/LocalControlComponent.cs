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
        private float _cameraButtonX = 0.5f; // Center horizontally
        private float _cameraButtonY = 0.92f; // 92% down the screen
        private int _cameraTapDelay = 3500; // Default tap delay in ms
        private int _cameraReturnDelay = 1500; // Default return delay in ms
        
        // Dialog handling parameters
        private int _acceptDialogDelay = 300; // Delay before tapping accept dialog in ms
        private float _acceptButtonXOffset = 0.2f; // X offset from center (0.2 = 20% right)
        private float _acceptButtonYOffset = 0.05f; // Y offset from center (0.05 = 5% down)
        
        public void SetMonoBehaviourOwner(MonoBehaviour owner)
        {
            this.owner = owner;
        }
        
        public override void DrawUI()
        {
            // Initialize if not already done
            if (!localControlInitialized && !IsEditorMode && owner != null)
            {
                #if UNITY_ANDROID
                owner.StartCoroutine(InitializeLocalControl());
                #endif
            }
            
            // Check if we have an initialized instance
            bool canUseLocalControls = false;
            bool accessibilityEnabled = false;
            
            #if UNITY_ANDROID
            if (!IsEditorMode)
            {
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
            }
            #endif
            
            if (!canUseLocalControls && !IsEditorMode)
            {
                // Show initializing status
                GUILayout.Label("Initializing local control...");
                return;
            }
            
            if (!accessibilityEnabled && !IsEditorMode)
            {
                // This component doesn't handle drawing the accessibility error UI
                // That's handled by the parent UI manager
                return;
            }
            
            // Add local controls once initialized (or in editor mode)
            GUILayout.Label("Local Device Control");
            
            // Two main sections - media and navigation
            DrawMediaControlsSection();
            DrawCameraControlsSection();
            DrawNavigationSection();
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
                GUILayout.Label("Camera permission required for camera features");
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
                // Camera parameters UI
                GUILayout.BeginVertical(GUI.skin.box);
                GUILayout.Label("Camera Button Position", GUI.skin.box);
                
                // Use sliders for the camera settings
                GUILayout.BeginHorizontal();
                GUILayout.Label("X:", GUILayout.Width(30));
                _cameraButtonX = GUILayout.HorizontalSlider(_cameraButtonX, 0f, 1f);
                GUILayout.Label(_cameraButtonX.ToString("F2"), GUILayout.Width(50));
                GUILayout.EndHorizontal();
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Y:", GUILayout.Width(30));
                _cameraButtonY = GUILayout.HorizontalSlider(_cameraButtonY, 0f, 1f);
                GUILayout.Label(_cameraButtonY.ToString("F2"), GUILayout.Width(50));
                GUILayout.EndHorizontal();
                
                GUILayout.Space(5);
                GUILayout.Label("Delays", GUI.skin.box);
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Tap:", GUILayout.Width(70));
                _cameraTapDelay = (int)GUILayout.HorizontalSlider(_cameraTapDelay, 1000, 5000);
                GUILayout.Label(_cameraTapDelay.ToString() + "ms", GUILayout.Width(80));
                GUILayout.EndHorizontal();
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Return:", GUILayout.Width(70));
                _cameraReturnDelay = (int)GUILayout.HorizontalSlider(_cameraReturnDelay, 1000, 5000);
                GUILayout.Label(_cameraReturnDelay.ToString() + "ms", GUILayout.Width(80));
                GUILayout.EndHorizontal();
                
                // Dialog handling parameters
                GUILayout.Space(5);
                GUILayout.Label("Dialog Settings", GUI.skin.box);
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Dialog Delay:", GUILayout.Width(100));
                _acceptDialogDelay = (int)GUILayout.HorizontalSlider(_acceptDialogDelay, 100, 1000);
                GUILayout.Label(_acceptDialogDelay.ToString() + "ms", GUILayout.Width(80));
                GUILayout.EndHorizontal();
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("X Offset:", GUILayout.Width(100));
                _acceptButtonXOffset = GUILayout.HorizontalSlider(_acceptButtonXOffset, 0f, 0.5f);
                GUILayout.Label(_acceptButtonXOffset.ToString("F2"), GUILayout.Width(50));
                GUILayout.EndHorizontal();
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Y Offset:", GUILayout.Width(100));
                _acceptButtonYOffset = GUILayout.HorizontalSlider(_acceptButtonYOffset, -0.2f, 0.2f);
                GUILayout.Label(_acceptButtonYOffset.ToString("F2"), GUILayout.Width(50));
                GUILayout.EndHorizontal();
                
                // Reset to default button
                if (GUILayout.Button("Reset to Defaults", GUILayout.Height(40)))
                {
                    _cameraButtonX = 0.5f;
                    _cameraButtonY = 0.92f;
                    _cameraTapDelay = 3500;
                    _cameraReturnDelay = 1500;
                    _acceptDialogDelay = 300;
                    _acceptButtonXOffset = 0.2f;
                    _acceptButtonYOffset = 0.05f;
                    Logger.AddLogEntry("Camera parameters reset to defaults");
                }
                GUILayout.EndVertical();
                
                // Display visual indicator of the button position
                GUILayout.Box("", GUILayout.Height(100), GUILayout.ExpandWidth(true));
                Rect lastRect = GUILayoutUtility.GetLastRect();
                GUI.DrawTexture(
                    new Rect(
                        lastRect.x + lastRect.width * _cameraButtonX - 10, 
                        lastRect.y + lastRect.height * _cameraButtonY - 10, 
                        20, 20
                    ), 
                    UIHelper.MakeColorTexture(Color.red)
                );
                
                // Camera action buttons
                GUILayout.BeginHorizontal();
                
                // Photo button - uses system photo intent with parameters
                if (GUILayout.Button("Take Photo", GUILayout.Height(60)))
                {
                    if (IsEditorMode)
                    {
                        Logger.AddLogEntry($"Take Photo pressed with params: X={_cameraButtonX}, Y={_cameraButtonY}, " +
                                         $"TapDelay={_cameraTapDelay}, ReturnDelay={_cameraReturnDelay} " +
                                         $"(not available in editor)");
                    }
                    else
                    {
                        #if UNITY_ANDROID
                        if (owner != null)
                        {
                            owner.StartCoroutine(BleHidLocalControl.Instance.TakePictureWithCamera(
                                _cameraTapDelay, _cameraReturnDelay, _cameraButtonX, _cameraButtonY));
                            Logger.AddLogEntry($"Opening camera for photo capture with custom parameters: " + 
                                             $"position=({_cameraButtonX},{_cameraButtonY}), " +
                                             $"delays=({_cameraTapDelay},{_cameraReturnDelay})");
                        }
                        #endif
                    }
                }
                
                // Video button - uses system video intent with parameters
                if (GUILayout.Button("Record Video", GUILayout.Height(60)))
                {
                    if (IsEditorMode)
                    {
                        Logger.AddLogEntry($"Record Video pressed with params: X={_cameraButtonX}, Y={_cameraButtonY}, " +
                                         $"TapDelay={_cameraTapDelay}, ReturnDelay={_cameraReturnDelay} " +
                                         $"(not available in editor)");
                    }
                    else
                    {
                        #if UNITY_ANDROID
                        if (owner != null)
                        {
                            owner.StartCoroutine(BleHidLocalControl.Instance.RecordVideo(
                                5.0f, _cameraTapDelay, _cameraReturnDelay, _cameraButtonX, _cameraButtonY));
                            Logger.AddLogEntry($"Opening camera for video recording with custom parameters: " + 
                                             $"position=({_cameraButtonX},{_cameraButtonY}), " +
                                             $"delays=({_cameraTapDelay},{_cameraReturnDelay})");
                        }
                        #endif
                    }
                }
                GUILayout.EndHorizontal();
                
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
        /// Initialize the local control component for Android
        /// </summary>
        private IEnumerator InitializeLocalControl()
        {
            if (localControlInitialized)
                yield break;
                
            localControlInitialized = true;
            
            Logger.AddLogEntry("Initializing local control...");
            
            // First, ensure we can get an instance
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
        }
    }
}
