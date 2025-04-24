using System;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// UI component for local device control functionality
    /// </summary>
    public class AccessibilityUI : SectionUI
    {
        public const string Name = "Local";
        public override string TabName => Name;

        private readonly AccessibilityServiceBridge _bridge;
        private readonly string[] _mediaRow1Labels;
        private readonly Action[] _mediaRow1Actions;
        private readonly string[] _mediaRow2Labels;
        private readonly Action[] _mediaRow2Actions;
        private readonly string[] _cameraButtonLabels;
        private readonly Action[] _cameraButtonActions;
        private readonly string[] _navRow1Labels;
        private readonly Action[] _navRow1Actions;
        private readonly string[] _navRow1EditorMessages;
        private readonly string[] _navRow3Labels;
        private readonly Action[] _navRow3Actions;
        private readonly string[] _navRow3EditorMessages;

        public AccessibilityUI(AccessibilityServiceBridge bridge)
        {
            _bridge = bridge;

            _mediaRow1Labels = new[] { "Previous", "Play/Pause", "Next" };
            _mediaRow1Actions = new Action[]
            {
                () => ExecuteLocalControl(l => l.PreviousTrack(), "Local Previous track pressed"),
                () => ExecuteLocalControl(l => l.PlayPause(), "Local Play/Pause pressed"),
                () => ExecuteLocalControl(l => l.NextTrack(), "Local Next track pressed")
            };

            _mediaRow2Labels = new[] { "Vol -", "Mute", "Vol +" };
            _mediaRow2Actions = new Action[]
            {
                () => ExecuteLocalControl(l => l.VolumeDown(), "Local Volume down pressed"),
                () => ExecuteLocalControl(l => l.Mute(), "Local Mute pressed"),
                () => ExecuteLocalControl(l => l.VolumeUp(), "Local Volume up pressed")
            };

            _cameraButtonLabels = new[] { "Launch Camera", "Launch Video" };
            _cameraButtonActions = new Action[]
            {
                () => ExecuteLocalControl(l => l.LaunchCameraApp(), "Launch Camera pressed (not available in editor)"),
                () => ExecuteLocalControl(l => l.LaunchVideoCapture(), "Launch Video pressed (not available in editor)")
            };

            // Navigation row 1 - Back, Home, Recents
            _navRow1Labels = new[] { "Back", "Home", "Recents" };
            _navRow1Actions = new Action[]
            {
                () => NavigateTo(AccessibilityServiceBridge.NavigationDirection.Back),
                () => NavigateTo(AccessibilityServiceBridge.NavigationDirection.Home),
                () => NavigateTo(AccessibilityServiceBridge.NavigationDirection.Recents)
            };
            _navRow1EditorMessages = new[] { "Local Back pressed", "Local Home pressed", "Local Recents pressed" };

            _navRow3Labels = new[] { "Left", "Down", "Right" };
            _navRow3Actions = new Action[]
            {
                () => NavigateTo(AccessibilityServiceBridge.NavigationDirection.Left),
                () => NavigateTo(AccessibilityServiceBridge.NavigationDirection.Down),
                () => NavigateTo(AccessibilityServiceBridge.NavigationDirection.Right)
            };
            _navRow3EditorMessages = new[] { "Local Left pressed", "Local Down pressed", "Local Right pressed" };
        }

        public override void Shown()
        {
        }

        public override void Hidden()
        {
        }

        public override void Update()
        {
        }

        public override void DrawUI()
        {
            UIHelper.BeginSection("Local Device Control");

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
            UIHelper.ActionButtonRow(_mediaRow1Labels, _mediaRow1Actions, _mediaRow1Labels, UIHelper.StandardButtonOptions);
            UIHelper.ActionButtonRow(_mediaRow2Labels, _mediaRow2Actions, _mediaRow2Labels, UIHelper.StandardButtonOptions);
            UIHelper.EndSection();
        }

        private void DrawCameraControlsSection()
        {
            UIHelper.BeginSection("Camera Controls");

            // Camera button position label
            GUILayout.Label("Camera Button Position");
            GUILayout.Space(5);
            UIHelper.ActionButtonRow(_cameraButtonLabels, _cameraButtonActions, _cameraButtonLabels, UIHelper.StandardButtonOptions);
            UIHelper.EndSection();
        }

        private void DrawNavigationSection()
        {
            UIHelper.BeginSection("Navigation");

            GUILayout.Space(5);
            UIHelper.ActionButtonRow(_navRow1Labels, _navRow1Actions, _navRow1EditorMessages, UIHelper.StandardButtonOptions);

            // Navigation row 2 - Up button centered
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();

            const float BUTTON_HEIGHT = 60f;

            void Action() => NavigateTo(AccessibilityServiceBridge.NavigationDirection.Up);
            GUILayoutOption[] options = { GUILayout.Height(BUTTON_HEIGHT), GUILayout.Width(Screen.width / 3) };
            if (UIHelper.Button("Up", Action, "Local Up pressed", options))
            {
                // Button action handled by ActionButton
            }

            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            UIHelper.ActionButtonRow(_navRow3Labels, _navRow3Actions, _navRow3EditorMessages, UIHelper.StandardButtonOptions);

            UIHelper.EndSection();
        }


        private void ExecuteLocalControl(Func<AccessibilityServiceBridge, bool> action, string editorMessage)
        {
            if (IsEditorMode) { LoggingManager.Instance.Log(editorMessage); }
            else
            {
                try { action(_bridge); }
                catch (Exception ex) { LoggingManager.Instance.Log($"Error executing local control: {ex.Message}"); }
            }
        }

        private void NavigateTo(AccessibilityServiceBridge.NavigationDirection direction)
        {
            ExecuteLocalControl(l => l.Navigate(direction), $"Local {direction} pressed");
        }
    }
}
