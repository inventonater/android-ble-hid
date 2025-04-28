using UnityEngine;
using System;
using System.Linq;
using JetBrains.Annotations;

namespace Inventonater.BleHid
{
    public class MouseDeviceUI : SectionUI, IInputSourceDevice
    {
        public string Name { get; } = "Mouse";
        public event Action<BleHidButtonEvent> EmitButtonEvent = delegate { };
        public event Action<Vector3> EmitPositionDelta = delegate { };

        public event Action<BleHidDirection> EmitDirection = delegate { };
        public override string TabName => Name;

        private Rect touchpadRect = new(Screen.width / 2 - 150, Screen.height / 2 - 100, 300, 200);

        private readonly MouseBridge _mouse;
        
        [CanBeNull] private MousePositionAxisMapping _mousePositionAxisMapping;
        private readonly string[] _buttonLabels = { "Left Click", "Middle Click", "Right Click" };
        private readonly string[] _buttonMessages = { "Left click pressed", "Middle click pressed", "Right click pressed" };
        private readonly Action[] _buttonActions;

        public MouseDeviceUI(MouseBridge mouse)
        {
            _mouse = mouse;
            _buttonActions = new Action[] {
                () => _mouse.ClickMouseButton(BleHidConstants.BUTTON_LEFT),
                () => _mouse.ClickMouseButton(BleHidConstants.BUTTON_MIDDLE),
                () => _mouse.ClickMouseButton(BleHidConstants.BUTTON_RIGHT)
            };
            BleHidManager.Instance.InputRouter.WhenMappingChanged += HandleMappingChanged;
        }

        private void HandleMappingChanged(InputDeviceMapping mapping)
        {
            Debug.Log("TODO improve MouseDeviceUI HandleMappingChanged");
            _mousePositionAxisMapping = mapping.AxisMappings.FirstOrDefault(m => m is MousePositionAxisMapping) as MousePositionAxisMapping;
        }

        public override void Shown()
        {
            if (!BleHidManager.Instance.InputRouter.HasDevice) BleHidManager.Instance.InputRouter.SetSourceDevice(this);
        }

        public override void Hidden()
        {
        }

        public void InputDeviceEnabled()
        {
        }

        public void InputDeviceDisabled()
        {
        }

        private Rect GetTouchpadRect()
        {
            var touchpadRectHeight = Screen.height - touchpadRect.y - touchpadRect.height;
            return new Rect(touchpadRect.x, touchpadRectHeight, touchpadRect.width, touchpadRect.height);
        }

        private Vector3 _lastPosition;
        public override void Update()
        {
            if (IsEditorMode)
            {
                if (Input.GetMouseButtonDown(0)) _lastPosition = Input.mousePosition;
                EmitPositionDelta(Input.mousePosition - _lastPosition);
                return;
            }

            if (Input.touchCount > 0)
            {
                var touch = Input.GetTouch(0);
                if (touch.phase == TouchPhase.Began) _lastPosition = touch.position;
                EmitPositionDelta((Vector3)touch.position - _lastPosition);
            }
        }

        public override void DrawUI()
        {
            DrawTouchpad();
            DrawFilterSection();
            DrawMouseButtons();
        }

        private void DrawTouchpad()
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
        }

        private void DrawMouseButtons()
        {
            UIHelper.BeginSection("Mouse Buttons");
            GUILayout.Label("Click buttons to send mouse button actions to the connected device");
            UIHelper.ActionButtonRow(_buttonLabels, _buttonActions, _buttonMessages, UIHelper.LargeButtonOptions);
            if (GUILayout.Button("Switch Mapping")) BleHidManager.Instance.InputRouter.CycleMapping();
            UIHelper.EndSection();
        }

        private void DrawFilterSection()
        {
            if(_mousePositionAxisMapping == null) return;

            UIHelper.BeginSection("Mouse Tuning");
            IInputFilter filter = _mousePositionAxisMapping.Filter;

            // --- GLOBAL SETTINGS SECTION ---
            GUILayout.Label("Global Speed: Adjusts overall mouse movement speed");
            _mousePositionAxisMapping.GlobalScale =
                UIHelper.SliderWithLabels("Slow", _mousePositionAxisMapping.GlobalScale, 0.25f, 10.0f, "Fast", "Global Speed: {0:F2}×", UIHelper.StandardSliderOptions);
            GUILayout.Label("Horizontal Speed: Adjusts left-right sensitivity");
            _mousePositionAxisMapping.HorizontalSensitivity = UIHelper.SliderWithLabels("Low", _mousePositionAxisMapping.HorizontalSensitivity, 1.0f, 10.0f, "High", "Horizontal Speed: {0:F1}",
                UIHelper.StandardSliderOptions);
            GUILayout.Label("Vertical Speed: Adjusts up-down sensitivity");
            _mousePositionAxisMapping.VerticalSensitivity = UIHelper.SliderWithLabels("Low", _mousePositionAxisMapping.VerticalSensitivity, 1.0f, 10.0f, "High", "Vertical Speed: {0:F1}",
                UIHelper.StandardSliderOptions);

            GUILayout.Space(10);

            // --- FILTER SELECTION SECTION ---

            // Filter type selection
            GUILayout.Label("Input Filter: Determines how mouse movement is processed");
            GUILayout.BeginHorizontal();

            foreach (var filterType in InputFilterFactory.GetAvailableFilterTypes())
            {
                bool isSelected = filterType == _mousePositionAxisMapping.CurrentFilterType;
                GUI.enabled = !isSelected;
                if (GUILayout.Button(InputFilterFactory.GetFilterName(filterType), isSelected ? GUI.skin.box : GUI.skin.button, GUILayout.Height(30))) _mousePositionAxisMapping.SetInputFilter(filterType);
                GUI.enabled = true;
            }

            GUILayout.EndHorizontal();

            if (filter != null) GUILayout.Label(filter.Description, GUI.skin.box);

            GUILayout.Space(10);

            // --- FILTER-SPECIFIC PARAMETERS SECTION ---

            // Let the filter draw its own parameter controls
            if (filter != null) { filter.DrawParameterControls(); }

            UIHelper.EndSection();
        }
    }
}
