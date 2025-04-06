using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace BleHid
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
        private string[] tabNames = new string[] { "Media", "Mouse", "Keyboard" };
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
            // Handle touchpad input in the mouse tab
            if (currentTab == 1 && bleHidManager != null && (bleHidManager.IsConnected || isEditorMode))
            {
                // Process touch input for the touchpad (similar to mouse input behavior)
                if (Input.touchCount > 0)
                {
                    Touch touch = Input.GetTouch(0);
                    
                    // Start drag when touching inside touchpad
                    if (touch.phase == TouchPhase.Began && touchpadRect.Contains(touch.position))
                    {
                        lastTouchPosition = touch.position;
                        isMouseDragging = true;
                    }
                    // Continue drag as long as finger is down, regardless of position
                    else if (touch.phase == TouchPhase.Moved && isMouseDragging)
                    {
                        // Calculate delta movement
                        Vector2 delta = touch.position - lastTouchPosition;
                        
                        // Scale the movement (adjust sensitivity as needed)
                        int scaledDeltaX = (int)(delta.x * 0.5f);
                        int scaledDeltaY = (int)(delta.y * 0.5f);
                        
                        // Only send if there's significant movement
                        if (Mathf.Abs(scaledDeltaX) > 0 || Mathf.Abs(scaledDeltaY) > 0)
                        {
                            if (!isEditorMode)
                            {
                                // Send the mouse movement
                                bleHidManager.MoveMouse(scaledDeltaX, scaledDeltaY);
                            }
                            else
                            {
                                AddLogEntry($"Touch delta: ({scaledDeltaX}, {scaledDeltaY})");
                            }
                            
                            // Update last position
                            lastTouchPosition = touch.position;
                        }
                    }
                    // End drag when finger is lifted
                    else if ((touch.phase == TouchPhase.Ended || touch.phase == TouchPhase.Canceled) && isMouseDragging)
                    {
                        isMouseDragging = false;
                    }
                }
                
                // Handle mouse input in editor
                #if UNITY_EDITOR
                if (isEditorMode)
                {
                    // Convert screen mouse position to GUI coordinates (flip Y)
                    Vector2 mousePos = new Vector2(
                        Input.mousePosition.x,
                        Screen.height - Input.mousePosition.y
                    );
                    
                    // Start drag when clicking inside touchpad
                    if (Input.GetMouseButtonDown(0) && touchpadRect.Contains(mousePos))
                    {
                        lastTouchPosition = mousePos;
                        isMouseDragging = true;
                        // Don't log start - reduces noise
                    }
                    // Continue drag as long as mouse button is held, regardless of position
                    else if (Input.GetMouseButton(0) && isMouseDragging)
                    {
                        // Calculate delta movement
                        Vector2 delta = mousePos - lastTouchPosition;
                        
                        // Scale the movement
                        int scaledDeltaX = (int)(delta.x * 0.5f);
                        int scaledDeltaY = (int)(delta.y * 0.5f);
                        
                        // Only send/log if there's significant movement
                        if (Mathf.Abs(scaledDeltaX) > 0 || Mathf.Abs(scaledDeltaY) > 0)
                        {
                            AddLogEntry($"Mouse delta: ({scaledDeltaX}, {scaledDeltaY})");
                            lastTouchPosition = mousePos;
                        }
                    }
                    // End drag when mouse is released
                    else if (Input.GetMouseButtonUp(0) && isMouseDragging)
                    {
                        isMouseDragging = false;
                        // Don't log end - reduces noise
                    }
                }
                #endif
            }
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
                // In editor mode, enable UI without requiring connection
                GUI.enabled = bleHidManager.IsConnected || isEditorMode;
                
                switch (currentTab)
                {
                    case 0: // Media tab
                        DrawMediaControls();
                        break;
                    case 1: // Mouse tab
                        DrawMouseControls();
                        break;
                    case 2: // Keyboard tab
                        DrawKeyboardControls();
                        break;
                }
                
                GUI.enabled = true;
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
