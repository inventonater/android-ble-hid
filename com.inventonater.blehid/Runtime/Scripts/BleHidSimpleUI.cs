using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Improved immediate mode UI for controlling BLE HID functionality.
    /// This script reorganizes the controls into a more intuitive hierarchy,
    /// separating device selection, control types, and settings.
    /// </summary>
    public class BleHidSimpleUI : MonoBehaviour
    {
        #region Private Fields
        private BleHidManager bleHidManager;
        
        // Main tab system
        private int currentMainTab = 0;
        private string[] mainTabNames = new string[] { "Remote Device", "Local Device", "Settings" };
        
        // Subtab systems
        private int currentRemoteSubtab = 0;
        private string[] remoteSubtabNames = new string[] { "Media", "Mouse", "Keyboard" };
        
        private int currentLocalSubtab = 0;
        private string[] localSubtabNames = new string[] { "Media", "Direction Keys" };
        
        private int currentSettingsSubtab = 0;
        private string[] settingsSubtabNames = new string[] { "Connection", "Permissions", "About" };
        
        // Text input for keyboard
        private string textToSend = "";
        
        // Logging
        private Vector2 scrollPosition;
        private List<string> logMessages = new List<string>();
        private bool showLogs = false;
        
        // State tracking
        private bool isInitialized = false;
        private Rect touchpadRect;
        private Vector2 lastTouchPosition;
        private bool isMouseDragging = false;

        // Flag to enable UI in editor even without full BLE functionality
        private bool isEditorMode = false;

        // Permission handling
        private bool hasPermissionError = false;
        private string permissionErrorMessage = "";
        
        // UI styling
        private GUIStyle remoteTabStyle;
        private GUIStyle localTabStyle;
        private GUIStyle settingsTabStyle;
        private GUIStyle subtabStyle;
        private GUIStyle headerStyle;
        private GUIStyle buttonStyle;
        private GUIStyle statusEnabledStyle;
        private GUIStyle statusDisabledStyle;
        #endregion

        #region Unity Lifecycle
        private void Start()
        {
            // Check if running in the Unity Editor
            #if UNITY_EDITOR
            isEditorMode = true;
            isInitialized = true; // Auto-initialize in editor
            AddLogEntry("Running in Editor mode - BLE functionality limited");
            #endif

            // Initialize touchpad area (will be in the center of the mouse tab)
            touchpadRect = new Rect(Screen.width / 2 - 150, Screen.height / 2 - 100, 300, 200);

            // Create and initialize BleHidManager (even in editor)
            GameObject managerObj = new GameObject("BleHidManager");
            bleHidManager = managerObj.AddComponent<BleHidManager>();

            // Register for events
            bleHidManager.OnInitializeComplete += OnInitializeComplete;
            bleHidManager.OnAdvertisingStateChanged += OnAdvertisingStateChanged;
            bleHidManager.OnConnectionStateChanged += OnConnectionStateChanged;
            bleHidManager.OnPairingStateChanged += OnPairingStateChanged;
            bleHidManager.OnError += OnError;
            bleHidManager.OnDebugLog += OnDebugLog;

            // Initialize BLE HID
            StartCoroutine(bleHidManager.Initialize());

            // Add log message
            AddLogEntry("Starting BLE HID initialization...");
        }

        private void Update()
        {
            // Only process touchpad input when:
            // 1. We're on the Remote Device > Mouse tab
            // 2. BLE is initialized or in editor mode
            // 3. Connected to a device or in editor mode
            if (currentMainTab != 0 || currentRemoteSubtab != 1 || 
                bleHidManager == null || (!bleHidManager.IsConnected && !isEditorMode))
            {
                return;
            }

            // Process direct touch input (mobile)
            if (Input.touchCount > 0)
            {
                HandleTouchInput(Input.GetTouch(0));
            }
            // Process mouse input (editor/desktop)
            #if UNITY_EDITOR
            else if (isEditorMode)
            {
                HandleMouseInput();
            }
            #endif
        }

        /// <summary>
        /// Handles touch input for the touchpad area
        /// </summary>
        private void HandleTouchInput(Touch touch)
        {
            // Always work in screen coordinates for input
            Vector2 touchScreenPos = touch.position;
            Rect screenTouchpadRect = GetScreenTouchpadRect();

            // Log touchpad boundaries and touch positions for debugging
            if (touch.phase == TouchPhase.Began)
            {
                AddLogEntry($"Touchpad screen rect: ({screenTouchpadRect.x}, {screenTouchpadRect.y}, w:{screenTouchpadRect.width}, h:{screenTouchpadRect.height})");
                AddLogEntry($"Touch began at: ({touchScreenPos.x}, {touchScreenPos.y})");
            }

            switch (touch.phase)
            {
                case TouchPhase.Began:
                    // Start dragging if touch begins inside touchpad
                    if (screenTouchpadRect.Contains(touchScreenPos))
                    {
                        lastTouchPosition = touchScreenPos;
                        isMouseDragging = true;
                        AddLogEntry("Touch drag started inside touchpad");
                    }
                    break;

                case TouchPhase.Moved:
                    // Process movement if we're dragging
                    if (isMouseDragging)
                    {
                        ProcessPointerMovement(touchScreenPos);
                    }
                    break;

                case TouchPhase.Ended:
                case TouchPhase.Canceled:
                    if (isMouseDragging)
                    {
                        isMouseDragging = false;
                        AddLogEntry("Touch drag ended");
                    }
                    break;
            }
        }

        #if UNITY_EDITOR
        /// <summary>
        /// Handles mouse input for the touchpad area (editor only)
        /// </summary>
        private void HandleMouseInput()
        {
            // Convert mouse position to screen coordinates (Unity mouse Y is inverted)
            Vector2 mouseScreenPos = new Vector2(
                Input.mousePosition.x,
                Input.mousePosition.y
            );

            Rect screenTouchpadRect = GetScreenTouchpadRect();

            // Start drag on mouse down inside touchpad
            if (Input.GetMouseButtonDown(0))
            {
                if (screenTouchpadRect.Contains(mouseScreenPos))
                {
                    lastTouchPosition = mouseScreenPos;
                    isMouseDragging = true;
                    AddLogEntry($"Mouse drag started at ({mouseScreenPos.x}, {mouseScreenPos.y})");
                }
            }
            // Continue drag during movement
            else if (Input.GetMouseButton(0) && isMouseDragging)
            {
                ProcessPointerMovement(mouseScreenPos);
            }
            // End drag on mouse up
            else if (Input.GetMouseButtonUp(0) && isMouseDragging)
            {
                isMouseDragging = false;
                AddLogEntry("Mouse drag ended");
            }
        }
        #endif

        float mouseScale = 3;

        /// <summary>
        /// Processes pointer movement for both touch and mouse input
        /// </summary>
        private void ProcessPointerMovement(Vector2 currentPosition)
        {
            // Calculate delta since last position
            Vector2 delta = currentPosition - lastTouchPosition;

            // Scale the movement (adjusted for sensitivity)
            int scaledDeltaX = (int)(delta.x * mouseScale);
            int scaledDeltaY = (int)(delta.y * mouseScale);

            // Only process if movement is significant
            if (Mathf.Abs(scaledDeltaX) > 0 || Mathf.Abs(scaledDeltaY) > 0)
            {
                // Invert Y direction for mouse movement (screen Y goes down, mouse Y goes up)
                scaledDeltaY = -scaledDeltaY;

                // Send movement or log in editor mode
                if (!isEditorMode)
                {
                    bleHidManager.MoveMouse(scaledDeltaX, scaledDeltaY);
                    AddLogEntry($"Sending mouse delta: ({scaledDeltaX}, {scaledDeltaY})");
                }
                else
                {
                    AddLogEntry($"Mouse delta: ({scaledDeltaX}, {scaledDeltaY})");
                }

                // Update last position for next calculation
                lastTouchPosition = currentPosition;
            }
        }

        /// <summary>
        /// Converts GUI touchpad rect to screen coordinates
        /// </summary>
        private Rect GetScreenTouchpadRect()
        {
            // Convert GUI coordinates to screen coordinates
            return new Rect(
                touchpadRect.x,               // X stays the same
                Screen.height - touchpadRect.y - touchpadRect.height, // Convert GUI Y to screen Y
                touchpadRect.width,           // Width stays the same
                touchpadRect.height           // Height stays the same
            );
        }

        private void OnDestroy()
        {
            // Unregister events to prevent memory leaks
            if (bleHidManager != null)
            {
                bleHidManager.OnInitializeComplete -= OnInitializeComplete;
                bleHidManager.OnAdvertisingStateChanged -= OnAdvertisingStateChanged;
                bleHidManager.OnConnectionStateChanged -= OnConnectionStateChanged;
                bleHidManager.OnPairingStateChanged -= OnPairingStateChanged;
                bleHidManager.OnError -= OnError;
                bleHidManager.OnDebugLog -= OnDebugLog;
            }
        }

        private void InitializeStyles()
        {
            // Create styles if not already created
            if (remoteTabStyle == null)
            {
                // Remote tab style - blue theme
                remoteTabStyle = new GUIStyle(GUI.skin.button);
                remoteTabStyle.normal.background = MakeColorTexture(new Color(0.2f, 0.4f, 0.8f, 0.8f));
                remoteTabStyle.active.background = MakeColorTexture(new Color(0.2f, 0.4f, 0.8f, 1.0f));
                remoteTabStyle.normal.textColor = Color.white;
                remoteTabStyle.fontSize = 24;
                
                // Local tab style - green theme
                localTabStyle = new GUIStyle(GUI.skin.button);
                localTabStyle.normal.background = MakeColorTexture(new Color(0.2f, 0.7f, 0.3f, 0.8f));
                localTabStyle.active.background = MakeColorTexture(new Color(0.2f, 0.7f, 0.3f, 1.0f));
                localTabStyle.normal.textColor = Color.white;
                localTabStyle.fontSize = 24;
                
                // Settings tab style - orange theme
                settingsTabStyle = new GUIStyle(GUI.skin.button);
                settingsTabStyle.normal.background = MakeColorTexture(new Color(0.8f, 0.5f, 0.2f, 0.8f));
                settingsTabStyle.active.background = MakeColorTexture(new Color(0.8f, 0.5f, 0.2f, 1.0f));
                settingsTabStyle.normal.textColor = Color.white;
                settingsTabStyle.fontSize = 24;
                
                // Subtab style
                subtabStyle = new GUIStyle(GUI.skin.button);
                subtabStyle.fontSize = 20;
                
                // Header style
                headerStyle = new GUIStyle(GUI.skin.label);
                headerStyle.fontSize = 22;
                headerStyle.fontStyle = FontStyle.Bold;
                headerStyle.alignment = TextAnchor.MiddleCenter;
                
                // Button style
                buttonStyle = new GUIStyle(GUI.skin.button);
                buttonStyle.fontSize = 22;
                
                // Status styles
                statusEnabledStyle = new GUIStyle(GUI.skin.label);
                statusEnabledStyle.normal.textColor = Color.green;
                statusEnabledStyle.fontSize = 20;
                
                statusDisabledStyle = new GUIStyle(GUI.skin.label);
                statusDisabledStyle.normal.textColor = Color.red;
                statusDisabledStyle.fontSize = 20;
            }
        }
        
        private void OnGUI()
        {
            // Initialize styles if needed
            InitializeStyles();
            
            // Set up GUI style for better touch targets
            GUI.skin.button.fontSize = 24;
            GUI.skin.label.fontSize = 20;
            GUI.skin.textField.fontSize = 20;
            GUI.skin.box.fontSize = 20;

            // Adjust layout to fill the screen
            float padding = 10;
            Rect layoutArea = new Rect(padding, padding, Screen.width - padding * 2, Screen.height - padding * 2);
            GUILayout.BeginArea(layoutArea);

            // Permission error warning - show at the top with a red background
            if (hasPermissionError)
            {
                GUIStyle errorStyle = new GUIStyle(GUI.skin.box);
                errorStyle.normal.background = MakeColorTexture(new Color(0.8f, 0.2f, 0.2f, 1.0f));
                errorStyle.normal.textColor = Color.white;
                errorStyle.fontSize = 22;
                errorStyle.fontStyle = FontStyle.Bold;
                errorStyle.padding = new RectOffset(20, 20, 20, 20);

                GUILayout.BeginVertical(errorStyle);
                GUILayout.Label("Permission Error", GUIStyle.none);
                GUILayout.Space(5);
                GUILayout.Label(permissionErrorMessage, GUIStyle.none);

                // Workaround notification for API level 31+
                GUILayout.Space(10);
                GUILayout.Label("Android 12+ requires Bluetooth permissions to be granted manually.", GUIStyle.none);
                GUILayout.Label("Please go to Settings > Apps > BLE HID and grant all Bluetooth permissions.", GUIStyle.none);

                GUILayout.Space(10);
                if (GUILayout.Button("Try Again", GUILayout.Height(60)))
                {
                    // Clear error and try to initialize again
                    hasPermissionError = false;
                    permissionErrorMessage = "";
                    AddLogEntry("Retrying initialization...");
                    StartCoroutine(bleHidManager.Initialize());
                }
                GUILayout.EndVertical();

                GUILayout.Space(20);
            }

            // Status area (minimized version)
            GUILayout.BeginVertical(GUI.skin.box);
            
            // Condensed status line
            string statusText = $"Status: {(isInitialized ? "Ready" : "Initializing...")}";
            if (bleHidManager != null && bleHidManager.IsConnected) {
                statusText += $" | Connected to: {bleHidManager.ConnectedDeviceName}";
            } else {
                statusText += " | Not connected";
            }
            
            if (isEditorMode) {
                statusText += " | EDITOR MODE";
            }
            
            GUILayout.Label(statusText);
            
            GUILayout.EndVertical();

            // If we have a permission error, don't show the rest of the UI
            if (hasPermissionError)
            {
                GUILayout.EndArea();
                return;
            }

            // Main Tab Selection
            GUILayout.BeginHorizontal();
            
            // Remote Tab Button
            if (GUILayout.Toggle(currentMainTab == 0, "Remote Device", remoteTabStyle, GUILayout.Height(60), GUILayout.ExpandWidth(true)))
            {
                if (currentMainTab != 0)
                {
                    currentMainTab = 0;
                    if (!isEditorMode)
                    {
                        bleHidManager.SetInputMode(BleHidConstants.InputMode.REMOTE);
                    }
                    AddLogEntry("Switched to Remote Device tab");
                }
            }
            
            // Local Tab Button
            if (GUILayout.Toggle(currentMainTab == 1, "Local Device", localTabStyle, GUILayout.Height(60), GUILayout.ExpandWidth(true)))
            {
                if (currentMainTab != 1)
                {
                    currentMainTab = 1;
                    if (!isEditorMode)
                    {
                        bleHidManager.SetInputMode(BleHidConstants.InputMode.LOCAL);
                    }
                    AddLogEntry("Switched to Local Device tab");
                }
            }
            
            // Settings Tab Button
            if (GUILayout.Toggle(currentMainTab == 2, "Settings", settingsTabStyle, GUILayout.Height(60), GUILayout.ExpandWidth(true)))
            {
                if (currentMainTab != 2)
                {
                    currentMainTab = 2;
                    AddLogEntry("Switched to Settings tab");
                }
            }
            
            GUILayout.EndHorizontal();
            
            // Subtab bar based on main tab selection
            GUILayout.BeginHorizontal();
            
            switch (currentMainTab)
            {
                case 0: // Remote Device tab
                    currentRemoteSubtab = GUILayout.Toolbar(currentRemoteSubtab, remoteSubtabNames, subtabStyle, GUILayout.Height(40));
                    break;
                case 1: // Local Device tab
                    currentLocalSubtab = GUILayout.Toolbar(currentLocalSubtab, localSubtabNames, subtabStyle, GUILayout.Height(40));
                    break;
                case 2: // Settings tab
                    currentSettingsSubtab = GUILayout.Toolbar(currentSettingsSubtab, settingsSubtabNames, subtabStyle, GUILayout.Height(40));
                    break;
            }
            
            GUILayout.EndHorizontal();

            // Content area
            GUILayout.BeginVertical(GUI.skin.box, GUILayout.Height(Screen.height * 0.55f));

            // Check if BLE HID is initialized
            if (bleHidManager != null && (isInitialized || isEditorMode))
            {
                switch (currentMainTab)
                {
                    case 0: // Remote Device tab
                        // Remote tabs require device connection
                        GUI.enabled = bleHidManager.IsConnected || isEditorMode;
                        
                        switch (currentRemoteSubtab)
                        {
                            case 0: // Media tab
                                GUILayout.Label("Remote Media Controls", headerStyle);
                                DrawMediaControls(false); // false = remote mode
                                break;
                            case 1: // Mouse tab
                                GUILayout.Label("Remote Mouse Controls", headerStyle);
                                DrawMouseControls();
                                break;
                            case 2: // Keyboard tab
                                GUILayout.Label("Remote Keyboard Controls", headerStyle);
                                DrawKeyboardControls();
                                break;
                        }
                        
                        GUI.enabled = true;
                        break;
                        
                    case 1: // Local Device tab
                        // Check if permissions are enabled
                        bool accessibilityEnabled = isEditorMode || bleHidManager.IsAccessibilityServiceEnabled();
                        bool mediaListenerEnabled = isEditorMode || bleHidManager.IsMediaNotificationListenerEnabled();
                        
                        if (!accessibilityEnabled && !mediaListenerEnabled)
                        {
                            // No permissions - show warning
                            DrawPermissionWarning();
                        }
                        else
                        {
                            switch (currentLocalSubtab)
                            {
                                case 0: // Media tab
                                    GUILayout.Label("Local Media Controls", headerStyle);
                                    if (mediaListenerEnabled)
                                    {
                                        DrawMediaControls(true); // true = local mode
                                    }
                                    else
                                    {
                                        GUILayout.Space(20);
                                        GUILayout.Label("Media Notification Listener permission required", statusDisabledStyle);
                                        if (GUILayout.Button("Enable Permission", GUILayout.Height(60)))
                                        {
                                            if (!isEditorMode)
                                            {
                                                bleHidManager.OpenNotificationListenerSettings();
                                            }
                                        }
                                    }
                                    break;
                                case 1: // Direction Keys tab
                                    GUILayout.Label("Local Direction Controls", headerStyle);
                                    if (accessibilityEnabled)
                                    {
                                        DrawDirectionControls();
                                    }
                                    else
                                    {
                                        GUILayout.Space(20);
                                        GUILayout.Label("Accessibility Service permission required", statusDisabledStyle);
                                        if (GUILayout.Button("Enable Permission", GUILayout.Height(60)))
                                        {
                                            if (!isEditorMode)
                                            {
                                                bleHidManager.OpenAccessibilitySettings();
                                            }
                                        }
                                    }
                                    break;
                            }
                        }
                        break;
                        
                    case 2: // Settings tab
                        switch (currentSettingsSubtab)
                        {
                            case 0: // Connection tab
                                DrawConnectionSettings();
                                break;
                            case 1: // Permissions tab
                                DrawPermissionsSettings();
                                break;
                            case 2: // About tab
                                DrawAboutInfo();
                                break;
                        }
                        break;
                }
            }
            else
            {
                GUILayout.Label("Initializing BLE HID...");
            }

            GUILayout.EndVertical();

            // Log toggle
            if (GUILayout.Button(showLogs ? "Hide Logs" : "Show Logs", GUILayout.Height(40)))
            {
                showLogs = !showLogs;
            }
            
            // Log area (only shown if toggled on)
            if (showLogs)
            {
                GUILayout.Label("Log:");
                scrollPosition = GUILayout.BeginScrollView(scrollPosition, GUI.skin.box, GUILayout.Height(Screen.height * 0.2f));
                foreach (string log in logMessages)
                {
                    GUILayout.Label(log);
                }
                GUILayout.EndScrollView();
            }

            GUILayout.EndArea();
        }
        #endregion

        #region Tab UI Methods
        private void DrawMediaControls(bool isLocal)
        {
            // Media control buttons row 1
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Previous", GUILayout.Height(80)))
            {
                if (isEditorMode)
                    AddLogEntry("Previous track pressed");
                else
                    bleHidManager.PreviousTrack();
            }

            if (GUILayout.Button("Play/Pause", GUILayout.Height(80)))
            {
                if (isEditorMode)
                    AddLogEntry("Play/Pause pressed");
                else
                    bleHidManager.PlayPause();
            }

            if (GUILayout.Button("Next", GUILayout.Height(80)))
            {
                if (isEditorMode)
                    AddLogEntry("Next track pressed");
                else
                    bleHidManager.NextTrack();
            }
            GUILayout.EndHorizontal();

            // Media control buttons row 2
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Volume Down", GUILayout.Height(80)))
            {
                if (isEditorMode)
                    AddLogEntry("Volume down pressed");
                else
                    bleHidManager.VolumeDown();
            }

            if (GUILayout.Button("Mute", GUILayout.Height(80)))
            {
                if (isEditorMode)
                    AddLogEntry("Mute pressed");
                else
                    bleHidManager.Mute();
            }

            if (GUILayout.Button("Volume Up", GUILayout.Height(80)))
            {
                if (isEditorMode)
                    AddLogEntry("Volume up pressed");
                else
                    bleHidManager.VolumeUp();
            }
            GUILayout.EndHorizontal();
        }

        private void DrawMouseControls()
        {
            // Show touchpad instruction
            GUILayout.Label("Touch & Drag in the area below to move mouse");

            // Draw touchpad area using a Box with visual style to make it obvious
            GUIStyle touchpadStyle = new GUIStyle(GUI.skin.box);
            touchpadStyle.normal.background = Texture2D.grayTexture;

            // Add current drag status if in editor mode
            string touchpadLabel = "Click and drag (can drag outside)";
            #if UNITY_EDITOR
            if (isEditorMode && isMouseDragging)
            {
                touchpadLabel = "DRAGGING - Move mouse to control";
            }
            #endif

            // Draw the touchpad with clear visual feedback
            GUILayout.Box(touchpadLabel, touchpadStyle, GUILayout.Width(300), GUILayout.Height(200));

            // Update touchpad rect after layout to get the actual position
            if (Event.current.type == EventType.Repaint)
            {
                Rect lastRect = GUILayoutUtility.GetLastRect();
                touchpadRect = new Rect(lastRect.x, lastRect.y, lastRect.width, lastRect.height);
            }

            // Mouse button controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Left Click", GUILayout.Height(80)))
            {
                if (isEditorMode)
                    AddLogEntry("Left click pressed");
                else
                    bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_LEFT);
            }

            if (GUILayout.Button("Middle Click", GUILayout.Height(80)))
            {
                if (isEditorMode)
                    AddLogEntry("Middle click pressed");
                else
                    bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_MIDDLE);
            }

            if (GUILayout.Button("Right Click", GUILayout.Height(80)))
            {
                if (isEditorMode)
                    AddLogEntry("Right click pressed");
                else
                    bleHidManager.ClickMouseButton(BleHidConstants.BUTTON_RIGHT);
            }
            GUILayout.EndHorizontal();

        }

        private void DrawKeyboardControls()
        {
            // Text input field
            GUILayout.Label("Text to type:");
            textToSend = GUILayout.TextField(textToSend, GUILayout.Height(60));

            // Send button
            if (GUILayout.Button("Send Text", GUILayout.Height(60)))
            {
                if (!string.IsNullOrEmpty(textToSend))
                {
                    if (isEditorMode)
                        AddLogEntry("Text sent: " + textToSend);
                    else
                        bleHidManager.TypeText(textToSend);

                    textToSend = "";
                }
                else
                {
                    AddLogEntry("Cannot send empty text");
                }
            }

            // Common keys
            GUILayout.Label("Common Keys:");

            // Row 1: Q-P
            DrawKeyboardRow(new string[] { "Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P" });

            // Row 2: A-L
            DrawKeyboardRow(new string[] { "A", "S", "D", "F", "G", "H", "J", "K", "L" });

            // Row 3: Z-M
            DrawKeyboardRow(new string[] { "Z", "X", "C", "V", "B", "N", "M" });

            // Row 4: Special keys
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Enter", GUILayout.Height(60)))
            {
                if (isEditorMode)
                    AddLogEntry("Enter key pressed");
                else
                    bleHidManager.SendKey(BleHidConstants.KEY_RETURN);
            }

            if (GUILayout.Button("Space", GUILayout.Height(60), GUILayout.Width(Screen.width * 0.3f)))
            {
                if (isEditorMode)
                    AddLogEntry("Space key pressed");
                else
                    bleHidManager.SendKey(BleHidConstants.KEY_SPACE);
            }

            if (GUILayout.Button("Backspace", GUILayout.Height(60)))
            {
                if (isEditorMode)
                    AddLogEntry("Backspace key pressed");
                else
                    bleHidManager.SendKey(BleHidConstants.KEY_BACKSPACE);
            }
            GUILayout.EndHorizontal();
        }

        private void DrawKeyboardRow(string[] keys)
        {
            GUILayout.BeginHorizontal();
            foreach (string key in keys)
            {
                if (GUILayout.Button(key, GUILayout.Height(60), GUILayout.Width(Screen.width / (keys.Length + 2))))
                {
                    SendKey(key);
                }
            }
            GUILayout.EndHorizontal();
        }

        private void SendKey(string key)
        {
            byte keyCode = GetKeyCode(key);
            if (keyCode > 0)
            {
                if (isEditorMode)
                    AddLogEntry("Key pressed: " + key);
                else
                    bleHidManager.SendKey(keyCode);
            }
        }

        private byte GetKeyCode(string key)
        {
            switch (key)
            {
                case "A": return BleHidConstants.KEY_A;
                case "B": return BleHidConstants.KEY_B;
                case "C": return BleHidConstants.KEY_C;
                case "D": return BleHidConstants.KEY_D;
                case "E": return BleHidConstants.KEY_E;
                case "F": return BleHidConstants.KEY_F;
                case "G": return BleHidConstants.KEY_G;
                case "H": return BleHidConstants.KEY_H;
                case "I": return BleHidConstants.KEY_I;
                case "J": return BleHidConstants.KEY_J;
                case "K": return BleHidConstants.KEY_K;
                case "L": return BleHidConstants.KEY_L;
                case "M": return BleHidConstants.KEY_M;
                case "N": return BleHidConstants.KEY_N;
                case "O": return BleHidConstants.KEY_O;
                case "P": return BleHidConstants.KEY_P;
                case "Q": return BleHidConstants.KEY_Q;
                case "R": return BleHidConstants.KEY_R;
                case "S": return BleHidConstants.KEY_S;
                case "T": return BleHidConstants.KEY_T;
                case "U": return BleHidConstants.KEY_U;
                case "V": return BleHidConstants.KEY_V;
                case "W": return BleHidConstants.KEY_W;
                case "X": return BleHidConstants.KEY_X;
                case "Y": return BleHidConstants.KEY_Y;
                case "Z": return BleHidConstants.KEY_Z;
                default: return 0;
            }
        }
        #endregion

        #region Event Handlers
        private void OnInitializeComplete(bool success, string message)
        {
            isInitialized = success;

            if (success)
            {
                // Clear any permission errors
                hasPermissionError = false;
                permissionErrorMessage = "";

                AddLogEntry("BLE HID initialized successfully: " + message);
            }
            else
            {
                AddLogEntry("BLE HID initialization failed: " + message);
            }
        }

        private void OnAdvertisingStateChanged(bool advertising, string message)
        {
            if (advertising)
            {
                AddLogEntry("BLE advertising started: " + message);
            }
            else
            {
                AddLogEntry("BLE advertising stopped: " + message);
            }
        }

        private void OnConnectionStateChanged(bool connected, string deviceName, string deviceAddress)
        {
            if (connected)
            {
                AddLogEntry("Device connected: " + deviceName + " (" + deviceAddress + ")");
            }
            else
            {
                AddLogEntry("Device disconnected");
            }
        }

        private void OnPairingStateChanged(string status, string deviceAddress)
        {
            AddLogEntry("Pairing state changed: " + status + (deviceAddress != null ? " (" + deviceAddress + ")" : ""));
        }

        private void OnError(int errorCode, string errorMessage)
        {
            // Check for permission error
            if (errorCode == BleHidConstants.ERROR_PERMISSIONS_NOT_GRANTED)
            {
                hasPermissionError = true;
                permissionErrorMessage = errorMessage;
                AddLogEntry("Permission error: " + errorMessage);
            }
            else
            {
                AddLogEntry("Error " + errorCode + ": " + errorMessage);
            }
        }

        private void OnDebugLog(string message)
        {
            AddLogEntry("Debug: " + message);
        }
        #endregion

        #region Helper Methods
        private void AddLogEntry(string entry)
        {
            string timestamp = System.DateTime.Now.ToString("HH:mm:ss");
            string logEntry = timestamp + " - " + entry;

            // Add to list
            logMessages.Add(logEntry);

            // Keep the log size reasonable
            if (logMessages.Count > 100)
            {
                logMessages.RemoveAt(0);
            }

            // Auto-scroll to bottom
            scrollPosition = new Vector2(0, float.MaxValue);

            // Also log to Unity console
            Debug.Log(logEntry);
        }

        /// <summary>
        /// Draw the Local Controls tab - allows controlling the local device
        /// </summary>
        private void DrawLocalControls()
        {
            // Mode selection
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Input Mode:", GUIStyle.none);
            
            int currentMode = BleHidConstants.InputMode.REMOTE;
            if (!isEditorMode)
            {
                currentMode = bleHidManager.GetInputMode();
            }
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Toggle(currentMode == BleHidConstants.InputMode.REMOTE, "Remote Device", GUI.skin.button, GUILayout.Height(60)))
            {
                if (currentMode != BleHidConstants.InputMode.REMOTE && !isEditorMode)
                {
                    bleHidManager.SetInputMode(BleHidConstants.InputMode.REMOTE);
                    AddLogEntry("Switched to Remote mode");
                }
            }
            
            if (GUILayout.Toggle(currentMode == BleHidConstants.InputMode.LOCAL, "Local Device", GUI.skin.button, GUILayout.Height(60)))
            {
                if (currentMode != BleHidConstants.InputMode.LOCAL && !isEditorMode)
                {
                    bleHidManager.SetInputMode(BleHidConstants.InputMode.LOCAL);
                    AddLogEntry("Switched to Local mode");
                }
            }
            GUILayout.EndHorizontal();
            GUILayout.EndVertical();
            
            // Permission checks and settings buttons
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Permissions:", GUIStyle.none);
            
            bool accessibilityEnabled = false;
            bool mediaListenerEnabled = false;
            
            if (!isEditorMode)
            {
                accessibilityEnabled = bleHidManager.IsAccessibilityServiceEnabled();
                mediaListenerEnabled = bleHidManager.IsMediaNotificationListenerEnabled();
            }
            
            // Accessibility Service permission
            GUILayout.BeginHorizontal();
            if (accessibilityEnabled)
            {
                GUIStyle enabledStyle = new GUIStyle(GUI.skin.label);
                enabledStyle.normal.textColor = Color.green;
                GUILayout.Label("✓ Accessibility Service Enabled", enabledStyle);
            }
            else
            {
                GUIStyle disabledStyle = new GUIStyle(GUI.skin.label);
                disabledStyle.normal.textColor = Color.red;
                GUILayout.Label("✗ Accessibility Service Disabled", disabledStyle);
            }
            
            if (GUILayout.Button("Settings", GUILayout.Height(40), GUILayout.Width(150)))
            {
                if (isEditorMode)
                {
                    AddLogEntry("Cannot open Accessibility Settings in editor mode");
                }
                else
                {
                    AddLogEntry("Opening Accessibility Settings...");
                    bleHidManager.OpenAccessibilitySettings();
                }
            }
            GUILayout.EndHorizontal();
            
            // Media Notification Listener permission
            GUILayout.BeginHorizontal();
            if (mediaListenerEnabled)
            {
                GUIStyle enabledStyle = new GUIStyle(GUI.skin.label);
                enabledStyle.normal.textColor = Color.green;
                GUILayout.Label("✓ Media Notification Listener Enabled", enabledStyle);
            }
            else
            {
                GUIStyle disabledStyle = new GUIStyle(GUI.skin.label);
                disabledStyle.normal.textColor = Color.red;
                GUILayout.Label("✗ Media Notification Listener Disabled", disabledStyle);
            }
            
            if (GUILayout.Button("Settings", GUILayout.Height(40), GUILayout.Width(150)))
            {
                if (isEditorMode)
                {
                    AddLogEntry("Cannot open Notification Settings in editor mode");
                }
                else
                {
                    AddLogEntry("Opening Notification Settings...");
                    bleHidManager.OpenNotificationListenerSettings();
                }
            }
            GUILayout.EndHorizontal();
            GUILayout.EndVertical();
            
            // Controls
            if (accessibilityEnabled || mediaListenerEnabled || isEditorMode)
            {
                GUILayout.Space(10);
                
                // Media controls
                if (mediaListenerEnabled || isEditorMode)
                {
                    GUILayout.Label("Media Controls:", GUIStyle.none);
                    
                    // Media control buttons row 1
                    GUILayout.BeginHorizontal();
                    if (GUILayout.Button("Previous", GUILayout.Height(60)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Previous track pressed");
                        else
                            bleHidManager.PreviousTrack();
                    }
                    
                    if (GUILayout.Button("Play/Pause", GUILayout.Height(60)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Play/Pause pressed");
                        else
                            bleHidManager.PlayPause();
                    }
                    
                    if (GUILayout.Button("Next", GUILayout.Height(60)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Next track pressed");
                        else
                            bleHidManager.NextTrack();
                    }
                    GUILayout.EndHorizontal();
                    
                    // Media control buttons row 2
                    GUILayout.BeginHorizontal();
                    if (GUILayout.Button("Volume Down", GUILayout.Height(60)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Volume down pressed");
                        else
                            bleHidManager.VolumeDown();
                    }
                    
                    if (GUILayout.Button("Mute", GUILayout.Height(60)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Mute pressed");
                        else
                            bleHidManager.Mute();
                    }
                    
                    if (GUILayout.Button("Volume Up", GUILayout.Height(60)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Volume up pressed");
                        else
                            bleHidManager.VolumeUp();
                    }
                    GUILayout.EndHorizontal();
                }
                
                // Directional controls (if accessibility enabled)
                if (accessibilityEnabled || isEditorMode)
                {
                    GUILayout.Space(10);
                    GUILayout.Label("Direction Controls:", GUIStyle.none);
                    
                    // Up button
                    GUILayout.BeginHorizontal();
                    GUILayout.FlexibleSpace();
                    if (GUILayout.Button("Up", GUILayout.Height(60), GUILayout.Width(150)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Up key pressed");
                        else
                            bleHidManager.SendDirectionalKey(BleHidConstants.KEY_UP);
                    }
                    GUILayout.FlexibleSpace();
                    GUILayout.EndHorizontal();
                    
                    // Left, Down, Right buttons in one row
                    GUILayout.BeginHorizontal();
                    GUILayout.FlexibleSpace();
                    if (GUILayout.Button("Left", GUILayout.Height(60), GUILayout.Width(150)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Left key pressed");
                        else
                            bleHidManager.SendDirectionalKey(BleHidConstants.KEY_LEFT);
                    }
                    
                    if (GUILayout.Button("Down", GUILayout.Height(60), GUILayout.Width(150)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Down key pressed");
                        else
                            bleHidManager.SendDirectionalKey(BleHidConstants.KEY_DOWN);
                    }
                    
                    if (GUILayout.Button("Right", GUILayout.Height(60), GUILayout.Width(150)))
                    {
                        if (isEditorMode)
                            AddLogEntry("Local: Right key pressed");
                        else
                            bleHidManager.SendDirectionalKey(BleHidConstants.KEY_RIGHT);
                    }
                    GUILayout.FlexibleSpace();
                    GUILayout.EndHorizontal();
                }
            }
            else
            {
                // Display message if neither permission is enabled
                GUIStyle warningStyle = new GUIStyle(GUI.skin.label);
                warningStyle.normal.textColor = new Color(1f, 0.7f, 0f); // Orange
                warningStyle.fontSize = 24;
                warningStyle.alignment = TextAnchor.MiddleCenter;
                
                GUILayout.Space(40);
                GUILayout.Label("Please enable at least one permission above to use local device control", warningStyle);
            }
        }
        
        // Shows warning for permissions
        private void DrawPermissionWarning()
        {
            GUIStyle warningStyle = new GUIStyle(GUI.skin.box);
            warningStyle.normal.background = MakeColorTexture(new Color(1f, 0.7f, 0f, 0.3f)); // Orange background
            warningStyle.padding = new RectOffset(20, 20, 20, 20);
            
            GUILayout.BeginVertical(warningStyle);
            
            GUILayout.Label("Permissions Required", headerStyle);
            GUILayout.Space(10);
            
            GUILayout.Label("Local device control requires at least one of these permissions:", GUI.skin.label);
            GUILayout.Space(10);
            
            GUILayout.Label("• Accessibility Service - For directional keys and keyboard control", GUI.skin.label);
            GUILayout.Label("• Media Notification Listener - For media playback control", GUI.skin.label);
            
            GUILayout.Space(20);
            
            if (GUILayout.Button("Enable Accessibility Service", GUILayout.Height(60)))
            {
                if (!isEditorMode)
                {
                    bleHidManager.OpenAccessibilitySettings();
                    AddLogEntry("Opening Accessibility Settings...");
                }
            }
            
            GUILayout.Space(10);
            
            if (GUILayout.Button("Enable Media Notification Listener", GUILayout.Height(60)))
            {
                if (!isEditorMode)
                {
                    bleHidManager.OpenNotificationListenerSettings();
                    AddLogEntry("Opening Notification Settings...");
                }
            }
            
            GUILayout.EndVertical();
        }
        
        // Shows directional key controls
        private void DrawDirectionControls()
        {
            // Up button
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("Up", buttonStyle, GUILayout.Height(80), GUILayout.Width(150)))
            {
                if (isEditorMode)
                    AddLogEntry("Local: Up key pressed");
                else
                    bleHidManager.SendDirectionalKey(BleHidConstants.KEY_UP);
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
            
            // Left, Down, Right buttons in one row
            GUILayout.BeginHorizontal();
            GUILayout.FlexibleSpace();
            if (GUILayout.Button("Left", buttonStyle, GUILayout.Height(80), GUILayout.Width(150)))
            {
                if (isEditorMode)
                    AddLogEntry("Local: Left key pressed");
                else
                    bleHidManager.SendDirectionalKey(BleHidConstants.KEY_LEFT);
            }
            
            if (GUILayout.Button("Down", buttonStyle, GUILayout.Height(80), GUILayout.Width(150)))
            {
                if (isEditorMode)
                    AddLogEntry("Local: Down key pressed");
                else
                    bleHidManager.SendDirectionalKey(BleHidConstants.KEY_DOWN);
            }
            
            if (GUILayout.Button("Right", buttonStyle, GUILayout.Height(80), GUILayout.Width(150)))
            {
                if (isEditorMode)
                    AddLogEntry("Local: Right key pressed");
                else
                    bleHidManager.SendDirectionalKey(BleHidConstants.KEY_RIGHT);
            }
            GUILayout.FlexibleSpace();
            GUILayout.EndHorizontal();
        }
        
        // Draw connection settings tab
        private void DrawConnectionSettings()
        {
            GUILayout.Label("Bluetooth Connection", headerStyle);
            GUILayout.Space(10);
            
            // Display connection status
            GUIStyle statusStyle = bleHidManager.IsConnected ? statusEnabledStyle : statusDisabledStyle;
            GUILayout.BeginHorizontal();
            GUILayout.Label("Connection Status:", GUI.skin.label);
            GUILayout.Label(bleHidManager.IsConnected ? "Connected" : "Not Connected", statusStyle);
            GUILayout.EndHorizontal();
            
            if (bleHidManager.IsConnected)
            {
                GUILayout.BeginHorizontal();
                GUILayout.Label("Connected Device:", GUI.skin.label);
                GUILayout.Label(bleHidManager.ConnectedDeviceName, GUI.skin.label);
                GUILayout.EndHorizontal();
                
                GUILayout.BeginHorizontal();
                GUILayout.Label("Device Address:", GUI.skin.label);
                GUILayout.Label(bleHidManager.ConnectedDeviceAddress, GUI.skin.label);
                GUILayout.EndHorizontal();
            }
            
            GUILayout.Space(20);
            
            // Advertising controls
            GUILayout.BeginHorizontal();
            GUILayout.Label("Advertising Status:", GUI.skin.label);
            GUILayout.Label(bleHidManager.IsAdvertising ? "Advertising" : "Not Advertising", 
                            bleHidManager.IsAdvertising ? statusEnabledStyle : statusDisabledStyle);
            GUILayout.EndHorizontal();
            
            if (GUILayout.Button(bleHidManager.IsAdvertising ? "Stop Advertising" : "Start Advertising", 
                                GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    AddLogEntry("Advertising toggle (not functional in editor)");
                }
                else
                {
                    if (bleHidManager.IsAdvertising)
                        bleHidManager.StopAdvertising();
                    else
                        bleHidManager.StartAdvertising();
                }
            }
            
            if (bleHidManager.IsConnected)
            {
                GUILayout.Space(20);
                if (GUILayout.Button("Disconnect", GUILayout.Height(60)))
                {
                    if (!isEditorMode)
                    {
                        // There's no explicit disconnect method in BleHidManager,
                        // but stopping advertising will usually disconnect the device
                        bleHidManager.StopAdvertising();
                        AddLogEntry("Stopping advertising to disconnect device");
                    }
                }
            }
        }
        
        // Draw permissions settings tab
        private void DrawPermissionsSettings()
        {
            GUILayout.Label("Android Permissions", headerStyle);
            GUILayout.Space(10);
            
            // BLE permissions
            GUILayout.Label("Bluetooth Permissions:", GUI.skin.label, GUILayout.Height(30));
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("• BLUETOOTH_CONNECT", GUI.skin.label);
            GUILayout.Label(BleHidPermissionHandler.HasUserAuthorizedPermission("android.permission.BLUETOOTH_CONNECT") ? 
                           "Granted" : "Not Granted", 
                           BleHidPermissionHandler.HasUserAuthorizedPermission("android.permission.BLUETOOTH_CONNECT") ? 
                           statusEnabledStyle : statusDisabledStyle);
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("• BLUETOOTH_SCAN", GUI.skin.label);
            GUILayout.Label(BleHidPermissionHandler.HasUserAuthorizedPermission("android.permission.BLUETOOTH_SCAN") ? 
                           "Granted" : "Not Granted", 
                           BleHidPermissionHandler.HasUserAuthorizedPermission("android.permission.BLUETOOTH_SCAN") ? 
                           statusEnabledStyle : statusDisabledStyle);
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("• BLUETOOTH_ADVERTISE", GUI.skin.label);
            GUILayout.Label(BleHidPermissionHandler.HasUserAuthorizedPermission("android.permission.BLUETOOTH_ADVERTISE") ? 
                           "Granted" : "Not Granted", 
                           BleHidPermissionHandler.HasUserAuthorizedPermission("android.permission.BLUETOOTH_ADVERTISE") ? 
                           statusEnabledStyle : statusDisabledStyle);
            GUILayout.EndHorizontal();
            
            GUILayout.Space(20);
            
            // Special permissions
            GUILayout.Label("Special Permissions:", GUI.skin.label, GUILayout.Height(30));
            
            // Accessibility Service
            GUILayout.BeginHorizontal();
            GUILayout.Label("• Accessibility Service", GUI.skin.label);
            bool accessibilityEnabled = isEditorMode || bleHidManager.IsAccessibilityServiceEnabled();
            GUILayout.Label(accessibilityEnabled ? "Enabled" : "Disabled", 
                           accessibilityEnabled ? statusEnabledStyle : statusDisabledStyle);
            GUILayout.EndHorizontal();
            
            if (GUILayout.Button("Open Accessibility Settings", GUILayout.Height(50)))
            {
                if (!isEditorMode)
                {
                    bleHidManager.OpenAccessibilitySettings();
                    AddLogEntry("Opening Accessibility Settings...");
                }
            }
            
            GUILayout.Space(10);
            
            // Media Notification Listener
            GUILayout.BeginHorizontal();
            GUILayout.Label("• Media Notification Listener", GUI.skin.label);
            bool mediaListenerEnabled = isEditorMode || bleHidManager.IsMediaNotificationListenerEnabled();
            GUILayout.Label(mediaListenerEnabled ? "Enabled" : "Disabled", 
                           mediaListenerEnabled ? statusEnabledStyle : statusDisabledStyle);
            GUILayout.EndHorizontal();
            
            if (GUILayout.Button("Open Notification Settings", GUILayout.Height(50)))
            {
                if (!isEditorMode)
                {
                    bleHidManager.OpenNotificationListenerSettings();
                    AddLogEntry("Opening Notification Settings...");
                }
            }
        }
        
        // Draw about info tab
        private void DrawAboutInfo()
        {
            GUILayout.Label("About BLE HID", headerStyle);
            GUILayout.Space(20);
            
            GUIStyle titleStyle = new GUIStyle(GUI.skin.label);
            titleStyle.fontSize = 24;
            titleStyle.fontStyle = FontStyle.Bold;
            titleStyle.alignment = TextAnchor.MiddleCenter;
            
            GUIStyle subtitleStyle = new GUIStyle(GUI.skin.label);
            subtitleStyle.fontSize = 18;
            subtitleStyle.alignment = TextAnchor.MiddleCenter;
            
            GUIStyle normalStyle = new GUIStyle(GUI.skin.label);
            normalStyle.fontSize = 16;
            normalStyle.alignment = TextAnchor.MiddleCenter;
            normalStyle.wordWrap = true;
            
            GUILayout.Label("Android BLE HID", titleStyle);
            GUILayout.Label("Version 1.0.0", subtitleStyle);
            GUILayout.Space(20);
            
            GUILayout.Label("A Unity plugin for Android HID peripherals over BLE", normalStyle);
            GUILayout.Label("Supports both remote device control and local device control", normalStyle);
            GUILayout.Space(20);
            
            GUILayout.Label("© 2025 Inventonater", normalStyle);
            GUILayout.Label("Licensed under the MIT License", normalStyle);
            
            GUILayout.Space(30);
            
            if (GUILayout.Button("Get Diagnostic Info", GUILayout.Height(50)))
            {
                string info = bleHidManager.GetDiagnosticInfo();
                GUIUtility.systemCopyBuffer = info;
                AddLogEntry("Diagnostic info copied to clipboard");
            }
        }
        
        // Helper to create colored textures for UI elements
        private Texture2D MakeColorTexture(Color color)
        {
            Texture2D texture = new Texture2D(1, 1);
            texture.SetPixel(0, 0, color);
            texture.Apply();
            return texture;
        }
        #endregion
    }
}
