using UnityEngine;
using System;

namespace Inventonater.BleHid
{
    public class MouseControlsComponent : UIComponent
    {
        public const string Name = "Mouse";
        public override string TabName => Name;
        private readonly PointerInputProcessor _inputProcessor;
        private Rect touchpadRect;

        private IInputFilter inputFilter;
        private InputFilterFactory.FilterType _currentFilterType;

        public MouseControlsComponent()
        {
            _currentFilterType = InputFilterFactory.FilterType.OneEuro;
            inputFilter = InputFilterFactory.CreateFilter(_currentFilterType);
            touchpadRect = new Rect(Screen.width / 2 - 150, Screen.height / 2 - 100, 300, 200);
            _inputProcessor = new PointerInputProcessor();
            _inputProcessor.SetInputFilter(inputFilter);
            UpdateTouchpadRect();
        }

        public void HandlePointerInput(Vector2 position, PointerInputState state, string inputSource = "External") =>
            _inputProcessor.HandlePointerInput(position, state, inputSource);

        public void SetCurrentFilterType(InputFilterFactory.FilterType value)
        {
            if (_currentFilterType == value) return;
            _currentFilterType = value;
            inputFilter = InputFilterFactory.CreateFilter(_currentFilterType);
            inputFilter.Reset();
            _inputProcessor.SetInputFilter(inputFilter);
            Logger.AddLogEntry($"Changed input filter to: {inputFilter.Name}");
        }


        private void UpdateTouchpadRect()
        {
            _inputProcessor.SetTouchpadRect(new Rect(
                touchpadRect.x, // X stays the same
                Screen.height - touchpadRect.y - touchpadRect.height, // Convert GUI Y to screen Y
                touchpadRect.width, // Width stays the same
                touchpadRect.height // Height stays the same
            ));
        }

        public override void Update()
        {
            if (IsEditorMode)
            {
                Vector2 mouseScreenPos = new Vector2(Input.mousePosition.x, Input.mousePosition.y);
                if (Input.GetMouseButtonDown(0)) HandlePointerInput(mouseScreenPos, PointerInputState.Begin, "Mouse");
                else if (Input.GetMouseButton(0)) HandlePointerInput(mouseScreenPos, PointerInputState.Move, "Mouse");
                else if (Input.GetMouseButtonUp(0)) HandlePointerInput(mouseScreenPos, PointerInputState.End, "Mouse");
            }

            if (Input.touchCount > 0)
            {
                Touch touch = Input.GetTouch(0);
                PointerInputState state;
                if (touch.phase == TouchPhase.Began) state = PointerInputState.Begin;
                else if (touch.phase == TouchPhase.Moved) state = PointerInputState.Move;
                else if (touch.phase is TouchPhase.Ended or TouchPhase.Canceled) state = PointerInputState.End;
                else return;

                HandlePointerInput(touch.position, state, "Touch");
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
                UpdateTouchpadRect();
            }

            UIHelper.EndSection();

            UIHelper.BeginSection("Mouse Tuning");

            // --- GLOBAL SETTINGS SECTION ---
            GUILayout.Label("Global Speed: Adjusts overall mouse movement speed");
            _inputProcessor.GlobalScale =
                UIHelper.SliderWithLabels("Slow", _inputProcessor.GlobalScale, 0.25f, 10.0f, "Fast", "Global Speed: {0:F2}×", UIHelper.StandardSliderOptions);
            GUILayout.Label("Horizontal Speed: Adjusts left-right sensitivity");
            _inputProcessor.HorizontalSensitivity = UIHelper.SliderWithLabels("Low", _inputProcessor.HorizontalSensitivity, 1.0f, 10.0f, "High", "Horizontal Speed: {0:F1}",
                UIHelper.StandardSliderOptions);
            GUILayout.Label("Vertical Speed: Adjusts up-down sensitivity");
            _inputProcessor.VerticalSensitivity = UIHelper.SliderWithLabels("Low", _inputProcessor.VerticalSensitivity, 1.0f, 10.0f, "High", "Vertical Speed: {0:F1}",
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
                () => BleHidManager.ClickMouseButton(BleHidConstants.BUTTON_LEFT),
                () => BleHidManager.ClickMouseButton(BleHidConstants.BUTTON_MIDDLE),
                () => BleHidManager.ClickMouseButton(BleHidConstants.BUTTON_RIGHT)
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
