using UnityEngine;
using System;

namespace Inventonater.BleHid
{
    public class MouseControlsComponent : UIComponent
    {
        public const string Name = "Mouse";
        public override string TabName => Name;

        private Rect touchpadRect;

        private IInputFilter inputFilter;
        private InputFilterFactory.FilterType _currentFilterType;

        public MouseControlsComponent()
        {
            _currentFilterType = InputFilterFactory.FilterType.OneEuro;
            inputFilter = InputFilterFactory.CreateFilter(_currentFilterType);
            touchpadRect = new Rect(Screen.width / 2 - 150, Screen.height / 2 - 100, 300, 200);
            BleHidManager.Instance.InputBridge.Mouse.SetInputFilter(inputFilter);
        }

        public void SetCurrentFilterType(InputFilterFactory.FilterType value)
        {
            if (_currentFilterType == value) return;
            _currentFilterType = value;
            inputFilter = InputFilterFactory.CreateFilter(_currentFilterType);
            inputFilter.Reset();
            BleHidManager.Instance.InputBridge.Mouse.SetInputFilter(inputFilter);
            Logger.AddLogEntry($"Changed input filter to: {inputFilter.Name}");
        }

        private Rect GetTouchpadRect()
        {
            var touchpadRectHeight = Screen.height - touchpadRect.y - touchpadRect.height;
            return new Rect(touchpadRect.x, touchpadRectHeight, touchpadRect.width, touchpadRect.height);
        }

        public override void Update()
        {
            if (IsEditorMode)
            {
                BleHidManager.Instance.InputBridge.Mouse.UpdatePosition(Input.mousePosition, Time.time);
                return;
            }

            if (Input.touchCount > 0)
            {
                var touch = Input.GetTouch(0);
                if (touch.phase == TouchPhase.Began) BleHidManager.Instance.InputBridge.Mouse.Reset();
                BleHidManager.Instance.InputBridge.Mouse.UpdatePosition(touch.position, Time.time);
            }
        }

        public override void DrawUI()
        {
            UIHelper.BeginSection("Mouse Touchpad");
            GUILayout.Label("Touchpad Area: Touch and drag to control mouse pointer");
            GUILayout.Label("Drag in touchpad area to send mouse movement to connected device");

            GUIStyle touchpadStyle = new GUIStyle(GUI.skin.box);
            touchpadStyle.normal.background = Texture2D.grayTexture;
            GUILayout.Box("Click and drag (can drag outside)", touchpadStyle, GUILayout.Height(200));

            if (Event.current.type == EventType.Repaint)
            {
                Rect lastRect = GUILayoutUtility.GetLastRect();
                touchpadRect = new Rect(lastRect.x, lastRect.y, lastRect.width, lastRect.height);
            }

            UIHelper.EndSection();

            UIHelper.BeginSection("Mouse Tuning");

            var positionFilter = BleHidManager.Instance.InputBridge.Mouse.PositionFilter;
            // --- GLOBAL SETTINGS SECTION ---
            GUILayout.Label("Global Speed: Adjusts overall mouse movement speed");
            positionFilter.GlobalScale =
                UIHelper.SliderWithLabels("Slow", positionFilter.GlobalScale, 0.25f, 10.0f, "Fast", "Global Speed: {0:F2}×", UIHelper.StandardSliderOptions);
            GUILayout.Label("Horizontal Speed: Adjusts left-right sensitivity");
            positionFilter.HorizontalSensitivity = UIHelper.SliderWithLabels("Low", positionFilter.HorizontalSensitivity, 1.0f, 10.0f, "High", "Horizontal Speed: {0:F1}",
                UIHelper.StandardSliderOptions);
            GUILayout.Label("Vertical Speed: Adjusts up-down sensitivity");
            positionFilter.VerticalSensitivity = UIHelper.SliderWithLabels("Low", positionFilter.VerticalSensitivity, 1.0f, 10.0f, "High", "Vertical Speed: {0:F1}",
                UIHelper.StandardSliderOptions);

            GUILayout.Space(10);

            // --- FILTER SELECTION SECTION ---

            // Filter type selection
            GUILayout.Label("Input Filter: Determines how mouse movement is processed");
            GUILayout.BeginHorizontal();
            foreach (var filterType in InputFilterFactory.GetAvailableFilterTypes())
            {
                bool isSelected = filterType == _currentFilterType;
                GUI.enabled = !isSelected;
                if (GUILayout.Button(InputFilterFactory.GetFilterName(filterType), isSelected ? GUI.skin.box : GUI.skin.button, GUILayout.Height(30)))
                    SetCurrentFilterType(filterType);
                GUI.enabled = true;
            }

            GUILayout.EndHorizontal();

            if (inputFilter != null) GUILayout.Label(inputFilter.Description, GUI.skin.box);

            GUILayout.Space(10);

            // --- FILTER-SPECIFIC PARAMETERS SECTION ---

            // Let the filter draw its own parameter controls
            if (inputFilter != null) { inputFilter.DrawParameterControls(); }

            UIHelper.EndSection();

            // Mouse button controls
            UIHelper.BeginSection("Mouse Buttons");

            GUILayout.Label("Click buttons to send mouse button actions to the connected device");

            string[] buttonLabels = { "Left Click", "Middle Click", "Right Click" };
            Action[] buttonActions =
            {
                () => BleHidManager.InputBridge.Mouse.ClickMouseButton(BleHidConstants.BUTTON_LEFT),
                () => BleHidManager.InputBridge.Mouse.ClickMouseButton(BleHidConstants.BUTTON_MIDDLE),
                () => BleHidManager.InputBridge.Mouse.ClickMouseButton(BleHidConstants.BUTTON_RIGHT)
            };
            string[] buttonMessages =
            {
                "Left click pressed",
                "Middle click pressed",
                "Right click pressed"
            };

            UIHelper.ActionButtonRow(buttonLabels, buttonActions, buttonMessages, UIHelper.LargeButtonOptions);
            UIHelper.EndSection();
        }
    }
}
