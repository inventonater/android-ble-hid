using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Simple immediate mode UI for controlling BLE HID functionality.
    /// This script combines all features (media, mouse, keyboard) into a single script
    /// using Unity's OnGUI system for reliable touch input handling.
    /// </summary>
    public class BleHidSimpleUI : MonoBehaviour
    {
        #region Private Fields
        private BleHidManager bleHidManager;
        private int currentTab = 0;
        private string[] tabNames = new string[] { "Media", "Mouse", "Keyboard", "Local" };
        private string textToSend = "";
        private Vector2 scrollPosition;
        private List<string> logMessages = new List<string>();
        private bool isInitialized = false;
        private Rect touchpadRect;
        private Vector2 lastTouchPosition;
        private bool isMouseDragging = false;

        // Flag to enable UI in editor even without full BLE functionality
        private bool isEditorMode = false;

        // Permission handling
        private bool hasPermissionError = false;
        private string permissionErrorMessage = "";
        private List<BleHidPermissionHandler.AndroidPermission> missingPermissions = new List<BleHidPermissionHandler.AndroidPermission>();
        private bool checkingPermissions = false;
        private bool hasCameraPermission = false;
        
        // Accessibility service handling
        private bool hasAccessibilityError = false;
        private string accessibilityErrorMessage = "Accessibility Service is required for local device control";
        private bool accessibilityServiceEnabled = false;
        private bool checkingAccessibilityService = false;
        
        // Track if we've attempted to initialize local control
        private bool localControlInitialized = false;
        
        // Camera control parameters with default values
        private float _cameraButtonX = 0.5f; // Center horizontally
        private float _cameraButtonY = 0.92f; // 92% down the screen
        private int _cameraTapDelay = 3500; // Default tap delay in ms
        private int _cameraReturnDelay = 1500; // Default return delay in ms
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
            
            // Initialize BleHidLocalControl for Android
            #if UNITY_ANDROID && !UNITY_EDITOR
            StartCoroutine(InitializeLocalControl());
            #endif

            // Add log message
            AddLogEntry("Starting BLE HID initialization...");
            
            // Check permissions on startup (Android only)
            #if UNITY_ANDROID && !UNITY_EDITOR
            CheckMissingPermissions();
            #endif
        }
        
        /// <summary>
        /// Initialize the local control component for Android
        /// </summary>
        private IEnumerator InitializeLocalControl()
        {
            if (localControlInitialized)
                yield break;
                
            localControlInitialized = true;
            
            AddLogEntry("Initializing local control...");
            
            // First, ensure we can get an instance
            BleHidLocalControl localControlInstance = null;
            
            try
            {
                localControlInstance = BleHidLocalControl.Instance;
                if (localControlInstance == null)
                {
                    AddLogEntry("Failed to create local control instance");
                    yield break;
                }
            }
            catch (System.Exception e)
            {
                AddLogEntry("Error creating local control instance: " + e.Message);
                yield break;
            }
            
            // Now initialize with retries
            yield return StartCoroutine(localControlInstance.Initialize(5));
            
            // Check if initialization was successful
            if (localControlInstance == null || !localControlInstance.IsAccessibilityServiceEnabled())
            {
                AddLogEntry("Local control initialized, but accessibility service not enabled");
            }
            else
            {
                AddLogEntry("Local control fully initialized");
            }
        }

        private void Update()
        {
            // Only process touchpad input when:
            // 1. We're on the mouse tab
            // 2. BLE is initialized or in editor mode
            // 3. Connected to a device or in editor mode
            if (currentTab != 1 || bleHidManager == null || (!bleHidManager.IsConnected && !isEditorMode))
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

        private void OnGUI()
        {
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
                DrawPermissionErrorUI();
                GUILayout.Space(20);
            }
            
            // Accessibility error - show if we're not in the Local tab
            if (hasAccessibilityError && currentTab != 3)
            {
                DrawAccessibilityErrorUI(false); // Simple notification when not on Local tab
                GUILayout.Space(20);
            }

            // Status area
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Status: " + (isInitialized ? "Ready" : "Initializing..."));

            if (bleHidManager != null && bleHidManager.IsConnected)
            {
                GUILayout.Label("Connected to: " + bleHidManager.ConnectedDeviceName);
                GUILayout.Label("Device: " + bleHidManager.ConnectedDeviceName + " (" + bleHidManager.ConnectedDeviceAddress + ")");
            }
            else
            {
                GUILayout.Label("Not connected");

                if (isEditorMode)
                {
                    GUILayout.Label("EDITOR MODE: UI visible but BLE functions disabled");
                }
            }

            if (bleHidManager != null && (isInitialized || isEditorMode))
            {
                if (GUILayout.Button(bleHidManager.IsAdvertising ? "Stop Advertising" : "Start Advertising", GUILayout.Height(60)))
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
            }
            else
            {
                GUI.enabled = false;
                GUILayout.Button("Start Advertising", GUILayout.Height(60));
                GUI.enabled = true;
            }
            GUILayout.EndVertical();

            // If we have a permission error, don't show the rest of the UI
            if (hasPermissionError)
            {
                GUILayout.EndArea();
                return;
            }

            // Tab selection
            currentTab = GUILayout.Toolbar(currentTab, tabNames, GUILayout.Height(60));

            // Tab content
            GUILayout.BeginVertical(GUI.skin.box, GUILayout.Height(Screen.height * 0.45f));

            // Check if BLE HID is initialized and a device is connected (or in editor mode)
            if (bleHidManager != null && (isInitialized || isEditorMode))
            {
                switch (currentTab)
                {
                    case 0: // Media tab
                        // Remote BLE controls need a connected device
                        GUI.enabled = bleHidManager.IsConnected || isEditorMode;
                        DrawMediaControls();
                        GUI.enabled = true;
                        break;
                    case 1: // Mouse tab
                        // Remote BLE controls need a connected device
                        GUI.enabled = bleHidManager.IsConnected || isEditorMode;
                        DrawMouseControls();
                        GUI.enabled = true;
                        break;
                    case 2: // Keyboard tab
                        // Remote BLE controls need a connected device
                        GUI.enabled = bleHidManager.IsConnected || isEditorMode;
                        DrawKeyboardControls();
                        GUI.enabled = true;
                        break;
                    case 3: // Local Control tab
                        // Local controls always enabled since they don't rely on a BLE connection
                        GUI.enabled = true;
                        DrawLocalControlTab();
                        break;
                }
            }
            else
            {
                GUILayout.Label("Initializing BLE HID...");
            }

            GUILayout.EndVertical();

            // Log area
            GUILayout.Label("Log:");
            scrollPosition = GUILayout.BeginScrollView(scrollPosition, GUI.skin.box, GUILayout.Height(Screen.height * 0.2f));
            foreach (string log in logMessages)
            {
                GUILayout.Label(log);
            }
            GUILayout.EndScrollView();

            GUILayout.EndArea();
        }
        #endregion

        #region Tab UI Methods
        private void DrawMediaControls()
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
            
            // Navigation keys
            GUILayout.Label("Navigation Keys:");
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Up", GUILayout.Height(60)))
            {
                if (isEditorMode)
                    AddLogEntry("Up key pressed");
                else
                    bleHidManager.SendKey(BleHidConstants.KEY_UP);
            }
            if (GUILayout.Button("Down", GUILayout.Height(60)))
            {
                if (isEditorMode)
                    AddLogEntry("Down key pressed");
                else
                    bleHidManager.SendKey(BleHidConstants.KEY_DOWN);
            }
            if (GUILayout.Button("Left", GUILayout.Height(60)))
            {
                if (isEditorMode)
                    AddLogEntry("Left key pressed");
                else
                    bleHidManager.SendKey(BleHidConstants.KEY_LEFT);
            }
            if (GUILayout.Button("Right", GUILayout.Height(60)))
            {
                if (isEditorMode)
                    AddLogEntry("Right key pressed");
                else
                    bleHidManager.SendKey(BleHidConstants.KEY_RIGHT);
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
        
        private void DrawLocalControlTab()
        {
            // Initialize if not already done
            if (!localControlInitialized && !isEditorMode)
            {
                #if UNITY_ANDROID
                StartCoroutine(InitializeLocalControl());
                #endif
            }
            
            // Check if we have an initialized instance
            bool canUseLocalControls = false;
            bool accessibilityEnabled = false;
            
            #if UNITY_ANDROID
            if (!isEditorMode)
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
            
            if (!canUseLocalControls && !isEditorMode)
            {
                // Show initializing status
                GUILayout.Label("Initializing local control...");
                return;
            }
            
            if (!accessibilityEnabled && !isEditorMode)
            {
                // Show detailed accessibility error UI
                hasAccessibilityError = true;
                DrawAccessibilityErrorUI(true); // Full UI with instructions
                return;
            }
            else
            {
                // Accessibility is enabled, clear the error
                hasAccessibilityError = false;
            }
            
            // Add local controls once initialized (or in editor mode)
            GUILayout.Label("Local Device Control");
            
            // Two main sections - media and navigation
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Media Controls", GUI.skin.box);
            
            // Media controls row 1
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Previous", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    AddLogEntry("Local Previous track pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Play/Pause pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Next track pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Volume down pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Mute pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Volume up pressed");
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
                        StartCoroutine(BleHidPermissionHandler.RequestCameraPermission());
                        AddLogEntry("Requesting camera permission");
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
                    
                    // Reset to default button
                    if (GUILayout.Button("Reset to Defaults", GUILayout.Height(40)))
                    {
                        _cameraButtonX = 0.5f;
                        _cameraButtonY = 0.92f;
                        _cameraTapDelay = 3500;
                        _cameraReturnDelay = 1500;
                        AddLogEntry("Camera parameters reset to defaults");
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
                        MakeColorTexture(Color.red)
                    );
                    
                    // Camera action buttons
                    GUILayout.BeginHorizontal();
                    
                    // Photo button - uses system photo intent with parameters
                    if (GUILayout.Button("Take Photo", GUILayout.Height(60)))
                    {
                        if (isEditorMode)
                        {
                            AddLogEntry($"Take Photo pressed with params: X={_cameraButtonX}, Y={_cameraButtonY}, " +
                                       $"TapDelay={_cameraTapDelay}, ReturnDelay={_cameraReturnDelay} " +
                                       $"(not available in editor)");
                        }
                        else
                        {
                            #if UNITY_ANDROID
                            StartCoroutine(BleHidLocalControl.Instance.TakePictureWithCamera(
                                _cameraTapDelay, _cameraReturnDelay, _cameraButtonX, _cameraButtonY));
                            AddLogEntry($"Opening camera for photo capture with custom parameters: " + 
                                       $"position=({_cameraButtonX},{_cameraButtonY}), " +
                                       $"delays=({_cameraTapDelay},{_cameraReturnDelay})");
                            #endif
                        }
                    }
                    
                    // Video button - uses system video intent with parameters
                    if (GUILayout.Button("Record Video", GUILayout.Height(60)))
                    {
                        if (isEditorMode)
                        {
                            AddLogEntry($"Record Video pressed with params: X={_cameraButtonX}, Y={_cameraButtonY}, " +
                                       $"TapDelay={_cameraTapDelay}, ReturnDelay={_cameraReturnDelay} " +
                                       $"(not available in editor)");
                        }
                        else
                        {
                            #if UNITY_ANDROID
                            StartCoroutine(BleHidLocalControl.Instance.RecordVideo(
                                5.0f, _cameraTapDelay, _cameraReturnDelay, _cameraButtonX, _cameraButtonY));
                            AddLogEntry($"Opening camera for video recording with custom parameters: " + 
                                       $"position=({_cameraButtonX},{_cameraButtonY}), " +
                                       $"delays=({_cameraTapDelay},{_cameraReturnDelay})");
                            #endif
                        }
                    }
                    GUILayout.EndHorizontal();
                
                // Direct launch buttons
                GUILayout.BeginHorizontal();
                if (GUILayout.Button("Launch Camera", GUILayout.Height(60)))
                {
                    if (isEditorMode)
                    {
                        AddLogEntry("Launch Camera pressed (not available in editor)");
                    }
                    else
                    {
                        #if UNITY_ANDROID
                        BleHidLocalControl.Instance.LaunchCameraApp();
                        AddLogEntry("Launching camera app");
                        #endif
                    }
                }
                
                if (GUILayout.Button("Launch Video", GUILayout.Height(60)))
                {
                    if (isEditorMode)
                    {
                        AddLogEntry("Launch Video pressed (not available in editor)");
                    }
                    else
                    {
                        #if UNITY_ANDROID
                        BleHidLocalControl.Instance.LaunchVideoCapture();
                        AddLogEntry("Launching video capture");
                        #endif
                    }
                }
                GUILayout.EndHorizontal();
            }
            
            GUILayout.EndVertical();
            
            // Navigation section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Navigation", GUI.skin.box);
            
            // Navigation row 1 (Back, Home, Recents)
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Back", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    AddLogEntry("Local Back pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Home pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Recents pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Up pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Left pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Down pressed");
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
                if (isEditorMode)
                {
                    AddLogEntry("Local Right pressed");
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
        /// <summary>
        /// Draw the permission error UI with specific missing permissions and resolution buttons
        /// </summary>
        private void DrawPermissionErrorUI()
        {
            GUIStyle errorStyle = new GUIStyle(GUI.skin.box);
            errorStyle.normal.background = MakeColorTexture(new Color(0.8f, 0.2f, 0.2f, 1.0f));
            errorStyle.normal.textColor = Color.white;
            errorStyle.fontSize = 22;
            errorStyle.fontStyle = FontStyle.Bold;
            errorStyle.padding = new RectOffset(20, 20, 20, 20);
            
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
                        AddLogEntry($"Requesting permission: {permission.Name}");
                        StartCoroutine(RequestSinglePermission(permission));
                    }
                    GUILayout.EndHorizontal();
                }
                
                // Open settings button
                GUILayout.Space(10);
                GUILayout.Label("If permission requests don't work, try granting them manually:", GUIStyle.none);
                
                if (GUILayout.Button("Open App Settings", GUILayout.Height(50)))
                {
                    AddLogEntry("Opening app settings");
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
                // Re-check permissions and try to initialize again
                CheckMissingPermissions();
                AddLogEntry("Rechecking permissions...");
            }
            
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Request a single permission and update the UI accordingly
        /// </summary>
        private IEnumerator RequestSinglePermission(BleHidPermissionHandler.AndroidPermission permission)
        {
            yield return StartCoroutine(BleHidPermissionHandler.RequestAndroidPermission(permission.PermissionString));
            
            // Re-check permissions after the request
            yield return new WaitForSeconds(0.5f);
            CheckMissingPermissions();
            
            // If all permissions have been granted, try initializing again
            if (missingPermissions.Count == 0)
            {
                hasPermissionError = false;
                permissionErrorMessage = "";
                AddLogEntry("All permissions granted. Initializing...");
                yield return StartCoroutine(bleHidManager.Initialize());
            }
        }
        
        /// <summary>
        /// Check for missing permissions and update the UI
        /// </summary>
        private void CheckMissingPermissions()
        {
            checkingPermissions = true;
            
            // Run this in coroutine to avoid freezing the UI
            StartCoroutine(CheckMissingPermissionsCoroutine());
        }
        
        private IEnumerator CheckMissingPermissionsCoroutine()
        {
            // Allow UI to update
            yield return null;
            
            // Get missing permissions
            missingPermissions = BleHidPermissionHandler.GetMissingPermissions();
            
            // Log the results
            if (missingPermissions.Count > 0)
            {
                string missingList = string.Join(", ", missingPermissions.Select(p => p.Name).ToArray());
                AddLogEntry($"Missing permissions: {missingList}");
            }
            else
            {
                AddLogEntry("All required permissions are granted");
            }
            
            checkingPermissions = false;
        }

        private void OnInitializeComplete(bool success, string message)
        {
            isInitialized = success;

            if (success)
            {
                // Clear any permission errors
                hasPermissionError = false;
                permissionErrorMessage = "";
                missingPermissions.Clear();

                AddLogEntry("BLE HID initialized successfully: " + message);
            }
            else
            {
                AddLogEntry("BLE HID initialization failed: " + message);
                
                // Check if this is a permission error
                if (message.Contains("permission"))
                {
                    CheckMissingPermissions();
                }
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

        /// <summary>
        /// Draw the accessibility service error UI with instructions
        /// </summary>
        private void DrawAccessibilityErrorUI(bool fullUI)
        {
            // Create a style for the error box
            GUIStyle errorStyle = new GUIStyle(GUI.skin.box);
            errorStyle.normal.background = MakeColorTexture(new Color(0.8f, 0.4f, 0.0f, 1.0f)); // Orange for accessibility
            errorStyle.normal.textColor = Color.white;
            errorStyle.fontSize = 22;
            errorStyle.fontStyle = FontStyle.Bold;
            errorStyle.padding = new RectOffset(20, 20, 20, 20);
            
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
                    // Full UI with detailed instructions - showing only the Open Settings button as requested
                    GUILayout.Space(10);
                    
                    if (GUILayout.Button("Open Accessibility Settings", GUILayout.Height(60)))
                    {
                        #if UNITY_ANDROID
                        if (BleHidLocalControl.Instance != null)
                        {
                            BleHidLocalControl.Instance.OpenAccessibilitySettings();
                            AddLogEntry("Opening accessibility settings");
                        }
                        #endif
                    }
                    
                    // Detailed instructions
                    GUILayout.Space(10);
                    GUILayout.Label("To enable the Accessibility Service:", GUIStyle.none);
                    GUILayout.Label("1. Tap 'Open Accessibility Settings'", GUIStyle.none);
                    GUILayout.Label("2. Find 'Inventonater BLE HID' in the list", GUIStyle.none);
                    GUILayout.Label("3. Toggle it ON", GUIStyle.none);
                    GUILayout.Label("4. Accept the permissions", GUIStyle.none);
                }
                else
                {
                    // Simple notification with link to Local tab
                    GUILayout.Space(5);
                    GUILayout.Label("Go to the Local tab to enable this feature", GUIStyle.none);
                    
                    if (GUILayout.Button("Go to Local Controls Tab", GUILayout.Height(40)))
                    {
                        currentTab = 3; // Switch to Local tab
                    }
                }
            }
            
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Check if the accessibility service is enabled
        /// </summary>
        private void CheckAccessibilityServiceStatus()
        {
            checkingAccessibilityService = true;
            StartCoroutine(CheckAccessibilityServiceCoroutine());
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
                        AddLogEntry("Accessibility service is enabled");
                    }
                    else
                    {
                        AddLogEntry("Accessibility service is not enabled");
                    }
                }
                catch (Exception e)
                {
                    AddLogEntry("Error checking accessibility service: " + e.Message);
                    hasAccessibilityError = true;
                }
            }
            else
            {
                hasAccessibilityError = true;
                AddLogEntry("BleHidLocalControl instance not available");
            }
            #endif
            
            checkingAccessibilityService = false;
        }

        private void OnError(int errorCode, string errorMessage)
        {
            // Check for permission error
            if (errorCode == BleHidConstants.ERROR_PERMISSIONS_NOT_GRANTED)
            {
                hasPermissionError = true;
                permissionErrorMessage = errorMessage;
                AddLogEntry("Permission error: " + errorMessage);
                
                // Check which specific permissions are missing
                CheckMissingPermissions();
            }
            else if (errorCode == BleHidConstants.ERROR_ACCESSIBILITY_NOT_ENABLED)
            {
                hasAccessibilityError = true;
                accessibilityErrorMessage = errorMessage;
                AddLogEntry("Accessibility error: " + errorMessage);
                
                // Check accessibility service status
                CheckAccessibilityServiceStatus();
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
