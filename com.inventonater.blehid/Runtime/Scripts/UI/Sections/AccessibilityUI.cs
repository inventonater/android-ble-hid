using System;
using UnityEngine;

namespace Inventonater
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
            _mediaRow1Actions = new Action[] { _bridge.PreviousTrack, _bridge.PlayPause, _bridge.NextTrack };

            _mediaRow2Labels = new[] { "Vol -", "Mute", "Vol +" };
            _mediaRow2Actions = new Action[] { _bridge.VolumeDown, _bridge.Mute, _bridge.VolumeUp };

            _cameraButtonLabels = new[] { "Launch Camera", "Launch Video" };
            _cameraButtonActions = new Action[] { _bridge.LaunchCameraApp, _bridge.LaunchVideoCapture };

            // Navigation row 1 - Back, Home, Recents
            _navRow1Labels = new[] { "Back", "Home", "Recents" };
            _navRow1Actions = new Action[] { _bridge.Back, _bridge.Home };
            _navRow1EditorMessages = new[] { "Local Back pressed", "Local Home pressed" };

            _navRow3Labels = new[] { "Left", "Down", "Right" };
            _navRow3Actions = new Action[] { _bridge.DPadLeft, _bridge.DPadDown, _bridge.DPadRight };
            _navRow3EditorMessages = new[] { "Local Left pressed", "Local Down pressed", "Local Right pressed" };
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

            GUILayoutOption[] options = { GUILayout.Height(BUTTON_HEIGHT), GUILayout.Width(Screen.width / 3) };
            if (UIHelper.Button("Up", () => _bridge.DPadUp(), "Local Up pressed", options))
            {
                // Button action handled by ActionButton
            }

            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            UIHelper.ActionButtonRow(_navRow3Labels, _navRow3Actions, _navRow3EditorMessages, UIHelper.StandardButtonOptions);

            UIHelper.EndSection();
        }
    }
}
