using System;
using System.Collections;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// UI component for local device control functionality
    /// </summary>
    public class LocalControlComponent : UIComponent
    {
        private bool localControlInitialized = false;
        private bool hasCameraPermission = false;
        private MonoBehaviour owner;

        // Button height constant
        private const float BUTTON_HEIGHT = 60f;

        public void SetMonoBehaviourOwner(MonoBehaviour owner)
        {
            this.owner = owner;
        }

        public virtual void DrawUI()
        {
            // Initialize if not already done
            if (!localControlInitialized && owner != null)
            {
                owner.StartCoroutine(InitializeLocalControl());
            }

            // Check if we have an initialized instance
            bool canUseLocalControls = CheckCanUseLocalControls();

            if (!canUseLocalControls && !IsEditorMode)
            {
                ShowInitializingUI();
                return;
            }

            // Always display the UI, even when accessibility service is not enabled
            // If accessibility is not enabled, the parent UI manager will show an error dialog

            UIHelper.BeginSection("Local Device Control");

            // Three main sections with consistent spacing
            DrawMediaControlsSection();
            GUILayout.Space(10);

            DrawCameraControlsSection();
            GUILayout.Space(10);

            DrawNavigationSection();

            UIHelper.EndSection();
        }


        private void DrawMediaControlsSection()
        {
            UIHelper.BeginSection("Media Controls");

            // Media controls row 1 - Previous, Play/Pause, Next
            string[] mediaRow1Labels = { "Previous", "Play/Pause", "Next" };
            Action[] mediaRow1Actions =
            {
                () => ExecuteLocalControl(l => l.PreviousTrack(), "Local Previous track pressed"),
                () => ExecuteLocalControl(l => l.PlayPause(), "Local Play/Pause pressed"),
                () => ExecuteLocalControl(l => l.NextTrack(), "Local Next track pressed")
            };

            UIHelper.ActionButtonRow(
                mediaRow1Labels,
                mediaRow1Actions,
                Logger,
                mediaRow1Labels,
                UIHelper.StandardButtonOptions);

            // Media controls row 2 - Vol-, Mute, Vol+
            string[] mediaRow2Labels = { "Vol -", "Mute", "Vol +" };
            Action[] mediaRow2Actions =
            {
                () => ExecuteLocalControl(l => l.VolumeDown(), "Local Volume down pressed"),
                () => ExecuteLocalControl(l => l.Mute(), "Local Mute pressed"),
                () => ExecuteLocalControl(l => l.VolumeUp(), "Local Volume up pressed")
            };

            UIHelper.ActionButtonRow(
                mediaRow2Labels,
                mediaRow2Actions,
                Logger,
                mediaRow2Labels,
                UIHelper.StandardButtonOptions);

            UIHelper.EndSection();
        }

        private void DrawCameraControlsSection()
        {
            UIHelper.BeginSection("Camera Controls");

            bool permissionGranted = CheckCameraPermission();

            if (!permissionGranted)
            {
                DrawCameraPermissionRequest();
            }
            else
            {
                // Camera button position label
                GUILayout.Label("Camera Button Position");
                GUILayout.Space(5);

                // Camera launch buttons
                string[] cameraButtonLabels = { "Launch Camera", "Launch Video" };
                Action[] cameraButtonActions =
                {
                    () => ExecuteLocalControl(l => l.LaunchCameraApp(), "Launch Camera pressed (not available in editor)"),
                    () => ExecuteLocalControl(l => l.LaunchVideoCapture(), "Launch Video pressed (not available in editor)")
                };

                UIHelper.ActionButtonRow(
                    cameraButtonLabels,
                    cameraButtonActions,
                    Logger,
                    cameraButtonLabels,
                    UIHelper.StandardButtonOptions);
            }

            UIHelper.EndSection();
        }

        private void DrawNavigationSection()
        {
            UIHelper.BeginSection("Navigation");

            GUILayout.Space(5);

            // Navigation row 1 - Back, Home, Recents
            string[] navRow1Labels = { "Back", "Home", "Recents" };
            Action[] navRow1Actions =
            {
                () => NavigateTo(BleHidLocalControl.NavigationDirection.Back),
                () => NavigateTo(BleHidLocalControl.NavigationDirection.Home),
                () => NavigateTo(BleHidLocalControl.NavigationDirection.Recents)
            };

            UIHelper.ActionButtonRow(
                navRow1Labels,
                navRow1Actions,
                Logger,
                new string[] { "Local Back pressed", "Local Home pressed", "Local Recents pressed" },
                UIHelper.StandardButtonOptions);

            // Navigation row 2 - Up button centered
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();

            if (UIHelper.ActionButton("Up",
                    () => NavigateTo(BleHidLocalControl.NavigationDirection.Up),
                    "Local Up pressed", Logger,
                    new GUILayoutOption[] { GUILayout.Height(BUTTON_HEIGHT), GUILayout.Width(Screen.width / 3) }))
            {
                // Button action handled by ActionButton
            }

            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();

            // Navigation row 3 - Left, Down, Right
            string[] navRow3Labels = { "Left", "Down", "Right" };
            Action[] navRow3Actions =
            {
                () => NavigateTo(BleHidLocalControl.NavigationDirection.Left),
                () => NavigateTo(BleHidLocalControl.NavigationDirection.Down),
                () => NavigateTo(BleHidLocalControl.NavigationDirection.Right)
            };

            UIHelper.ActionButtonRow(
                navRow3Labels,
                navRow3Actions,
                Logger,
                new string[] { "Local Left pressed", "Local Down pressed", "Local Right pressed" },
                UIHelper.StandardButtonOptions);

            UIHelper.EndSection();
        }


        private void ExecuteLocalControl(Func<BleHidLocalControl, bool> action, string editorMessage)
        {
            if (IsEditorMode)
            {
                Logger.AddLogEntry(editorMessage);
            }
            else
            {
#if UNITY_ANDROID
                try
                {
                    action(BleHidLocalControl.Instance);
                }
                catch (Exception ex)
                {
                    Logger.AddLogEntry($"Error executing local control: {ex.Message}");
                }
#endif
            }
        }

        private void NavigateTo(BleHidLocalControl.NavigationDirection direction)
        {
            ExecuteLocalControl(
                l => l.Navigate(direction),
                $"Local {direction} pressed"
            );
        }

        private bool CheckCanUseLocalControls()
        {
            if (IsEditorMode)
            {
                return true;
            }

#if UNITY_ANDROID
            try
            {
                var instance = BleHidLocalControl.Instance;
                if (instance != null)
                {
                    try
                    {
                        return true;
                    }
                    catch (Exception)
                    {
                        return false;
                    }
                }
            }
            catch (Exception)
            {
                // Fall through to return false
            }
#endif

            return false;
        }

        private bool CheckCameraPermission()
        {
#if UNITY_ANDROID && !UNITY_EDITOR
            hasCameraPermission = BleHidPermissionHandler.CheckCameraPermission();
            return hasCameraPermission;
#else
            return true;
#endif
        }

        private void DrawCameraPermissionRequest()
        {
            GUILayout.Space(5);
            GUILayout.Label("Camera permission required for camera features");
            GUILayout.Space(5);

            if (UIHelper.ActionButton("Request Camera Permission",
                    () =>
                    {
#if UNITY_ANDROID
                        if (owner != null)
                        {
                            owner.StartCoroutine(BleHidPermissionHandler.RequestCameraPermission());
                        }
#endif
                    },
                    "Requesting camera permission",Logger,
                    UIHelper.StandardButtonOptions))
            {
                // Button action handled by ActionButton
            }
        }

        private void ShowInitializingUI()
        {
            UIHelper.BeginSection("Local Device Control");

            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Initializing local control...", GUI.skin.box);
            GUILayout.Space(10);
            GUILayout.Label("Please wait while the local control features are being initialized...");
            GUILayout.EndVertical();

            UIHelper.EndSection();
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
