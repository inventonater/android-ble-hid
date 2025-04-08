using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Events;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// Main controller for the BLE HID UI.
    /// Manages the top-level tabs (Local and Remote) and permission handling.
    /// </summary>
    public class BleHidUIController : MonoBehaviour
    {
        private const float PERMISSION_CHECK_INTERVAL = 1.0f;
        
        #region Private Fields
        private BleHidManager bleHidManager;
        private BleHidLocalControl localControl;
        private BleHidLogger logger;
        
        // Tab management
        private string[] mainTabNames = new string[] { "Local", "Remote" };
        private int currentMainTab = 0;
        
        // Panel references
        private Dictionary<string, IBleHidPanel> panels = new Dictionary<string, IBleHidPanel>();
        private IBleHidPanel activeLocalPanel;
        private IBleHidPanel activeRemotePanel;
        
        // Permission tracking
        private bool checkingPermissions = false;
        private float permissionCheckTimer = 0f;
        private bool hasBluetoothPermission = false;
        private bool hasAccessibilityPermission = false;
        
        // UI elements
        private GUIStyle headerStyle;
        private GUIStyle statusStyle;
        private GUIStyle connectionIndicatorStyle;
        private Vector2 logScrollPosition;
        private string deviceNameInput = "BLE HID Controller";
        
        // Flag to enable UI in editor
        private bool isEditorMode = false;
        #endregion
        
        #region Unity Lifecycle
        private void Awake()
        {
            // Check if running in the Unity Editor
            #if UNITY_EDITOR
            isEditorMode = true;
            #endif
            
            // Initialize the logger
            logger = BleHidLogger.Instance;
            
            // Create UI styles
            InitializeStyles();
        }
        
        private void Start()
        {
            // Create BleHidManager (if not already created)
            if (FindObjectOfType<BleHidManager>() == null)
            {
                GameObject managerObj = new GameObject("BleHidManager");
                bleHidManager = managerObj.AddComponent<BleHidManager>();
                DontDestroyOnLoad(managerObj);
            }
            else
            {
                bleHidManager = FindObjectOfType<BleHidManager>();
            }
            
            // Register event handlers
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
            
            // Register panels - will be replaced with actual implementations
            RegisterPanel("Local", new DummyLocalPanel());
            RegisterPanel("Remote", new DummyRemotePanel());
            
            // Start permission checking
            StartCoroutine(CheckPermissions());
            
            logger.Log("Starting BLE HID initialization...");
        }
        
        private void Update()
        {
            // Update permission check timer
            if (!checkingPermissions)
            {
                permissionCheckTimer += Time.deltaTime;
                if (permissionCheckTimer >= PERMISSION_CHECK_INTERVAL)
                {
                    permissionCheckTimer = 0f;
                    StartCoroutine(CheckPermissions());
                }
            }
            
            // Pass Update to active panel
            if (currentMainTab == 0 && activeLocalPanel != null)
            {
                activeLocalPanel.Update();
            }
            else if (currentMainTab == 1 && activeRemotePanel != null)
            {
                activeRemotePanel.Update();
            }
        }
        
        private void OnApplicationFocus(bool hasFocus)
        {
            if (hasFocus)
            {
                // Check permissions when app regains focus
                StartCoroutine(CheckPermissions());
            }
        }
        
        private void OnDestroy()
        {
            // Unregister event handlers to prevent memory leaks
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
            // Set up common GUI styles
            SetupGUIStyles();
            
            // Calculate layout areas
            float padding = 10;
            Rect mainArea = new Rect(padding, padding, 
                                    Screen.width - padding * 2, 
                                    Screen.height - padding * 2);
            
            float headerHeight = 150;
            float tabBarHeight = 60;
            float logAreaHeight = Screen.height * 0.15f;
            float contentAreaHeight = mainArea.height - headerHeight - tabBarHeight - logAreaHeight - padding * 3;
            
            Rect headerArea = new Rect(mainArea.x, mainArea.y, 
                                      mainArea.width, headerHeight);
            
            Rect tabBarArea = new Rect(mainArea.x, headerArea.yMax + padding, 
                                      mainArea.width, tabBarHeight);
            
            Rect contentArea = new Rect(mainArea.x, tabBarArea.yMax + padding, 
                                       mainArea.width, contentAreaHeight);
            
            Rect logArea = new Rect(mainArea.x, contentArea.yMax + padding, 
                                   mainArea.width, logAreaHeight);
            
            // Draw the layout areas
            DrawHeader(headerArea);
            DrawTabs(tabBarArea);
            DrawContent(contentArea);
            DrawLogs(logArea);
        }
        #endregion
        
        #region UI Drawing Methods
        private void DrawHeader(Rect area)
        {
            GUILayout.BeginArea(area, GUI.skin.box);
            
            GUILayout.BeginVertical();
            
            // Title
            GUILayout.Label("BLE HID Controller", headerStyle);
            
            // Status area
            GUILayout.BeginHorizontal();
            
            // Connection status indicator
            if (bleHidManager != null && bleHidManager.IsConnected)
            {
                GUILayout.Box(" ", connectionIndicatorStyle, GUILayout.Width(20), GUILayout.Height(20));
                GUILayout.Label($"Connected to: {bleHidManager.ConnectedDeviceName} ({bleHidManager.ConnectedDeviceAddress})", 
                               statusStyle);
            }
            else
            {
                GUILayout.Box(" ", GUILayout.Width(20), GUILayout.Height(20));
                GUILayout.Label("Not connected", statusStyle);
            }
            
            GUILayout.EndHorizontal();
            
            // Device name and advertising controls
            GUILayout.BeginHorizontal();
            
            GUILayout.Label("Device Name:", GUILayout.Width(100));
            
            GUI.enabled = (bleHidManager != null) && !bleHidManager.IsAdvertising;
            deviceNameInput = GUILayout.TextField(deviceNameInput, GUILayout.Height(40));
            GUI.enabled = true;
            
            if (bleHidManager != null && (bleHidManager.IsInitialized || isEditorMode))
            {
                if (GUILayout.Button(
                    bleHidManager.IsAdvertising ? "Stop Advertising" : "Start Advertising", 
                    GUILayout.Height(40), GUILayout.Width(180)))
                {
                    if (isEditorMode)
                    {
                        logger.Log("Advertising toggle (not functional in editor)");
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
                GUILayout.Button("Start Advertising", GUILayout.Height(40), GUILayout.Width(180));
                GUI.enabled = true;
            }
            
            GUILayout.EndHorizontal();
            
            // Permission warnings
            DrawPermissionWarnings();
            
            GUILayout.EndVertical();
            
            GUILayout.EndArea();
        }
        
        private void DrawTabs(Rect area)
        {
            GUILayout.BeginArea(area);
            
            // Main tab selection
            int newTab = GUILayout.Toolbar(currentMainTab, mainTabNames, GUILayout.Height(area.height));
            
            // Handle tab change
            if (newTab != currentMainTab)
            {
                // Deactivate old panel
                if (currentMainTab == 0 && activeLocalPanel != null)
                {
                    activeLocalPanel.OnDeactivate();
                }
                else if (currentMainTab == 1 && activeRemotePanel != null)
                {
                    activeRemotePanel.OnDeactivate();
                }
                
                // Update current tab
                currentMainTab = newTab;
                
                // Activate new panel
                if (currentMainTab == 0 && activeLocalPanel != null)
                {
                    activeLocalPanel.OnActivate();
                }
                else if (currentMainTab == 1 && activeRemotePanel != null)
                {
                    activeRemotePanel.OnActivate();
                }
            }
            
            GUILayout.EndArea();
        }
        
        private void DrawContent(Rect area)
        {
            GUILayout.BeginArea(area, GUI.skin.box);
            
            // Draw the appropriate panel content
            if (currentMainTab == 0)
            {
                if (activeLocalPanel != null)
                {
                    activeLocalPanel.DrawPanel();
                }
                else
                {
                    GUILayout.Label("Local control not available");
                }
            }
            else // Remote tab
            {
                if (activeRemotePanel != null)
                {
                    if (bleHidManager != null && (bleHidManager.IsConnected || isEditorMode))
                    {
                        activeRemotePanel.DrawPanel();
                    }
                    else
                    {
                        GUILayout.Label("Start advertising and connect a device to use remote controls");
                    }
                }
                else
                {
                    GUILayout.Label("Remote control not available");
                }
            }
            
            GUILayout.EndArea();
        }
        
        private void DrawLogs(Rect area)
        {
            logger.DrawLogView(area);
        }
        
        private void DrawPermissionWarnings()
        {
            // Bluetooth permission warning
            if (!hasBluetoothPermission && !isEditorMode)
            {
                GUILayout.BeginVertical(GUI.skin.box);
                GUILayout.Label("Bluetooth permissions required", headerStyle);
                GUILayout.Label("Please grant Bluetooth permissions to use BLE features");
                
                if (GUILayout.Button("Open Bluetooth Settings", GUILayout.Height(40)))
                {
                    OpenAppPermissionSettings();
                }
                GUILayout.EndVertical();
            }
            
            // Accessibility permission warning
            if (!hasAccessibilityPermission && !isEditorMode && currentMainTab == 0)
            {
                GUILayout.BeginVertical(GUI.skin.box);
                GUILayout.Label("Accessibility Service Required", headerStyle);
                GUILayout.Label("Please enable the Accessibility Service to use local controls");
                
                if (GUILayout.Button("Enable Accessibility Service", GUILayout.Height(40)))
                {
                    if (localControl != null)
                    {
                        localControl.OpenAccessibilitySettings();
                    }
                }
                GUILayout.EndVertical();
            }
        }
        #endregion
        
        #region Initialization and Registration
        /// <summary>
        /// Initialize local control component for Android.
        /// </summary>
        private IEnumerator InitializeLocalControl()
        {
            logger.Log("Initializing local control...");
            
            // Get instance of BleHidLocalControl
            try {
                localControl = BleHidLocalControl.Instance;
            }
            catch (Exception e) {
                logger.LogError($"Error creating local control instance: {e.Message}");
                yield break;
            }
            
            if (localControl == null)
            {
                logger.LogError("Failed to create local control instance");
                yield break;
            }
            
            // Initialize with retries
            yield return StartCoroutine(localControl.Initialize(5));
            
            // Check if initialization was successful
            bool accessibilityEnabled = false;
            try {
                accessibilityEnabled = localControl.IsAccessibilityServiceEnabled();
            }
            catch (Exception e) {
                logger.LogError($"Error checking accessibility service: {e.Message}");
            }
            
            hasAccessibilityPermission = accessibilityEnabled;
            
            if (!accessibilityEnabled)
            {
                logger.LogWarning("Accessibility service not enabled. Some features will be limited.");
            }
            else
            {
                logger.Log("Local control fully initialized");
            }
        }
        
        /// <summary>
        /// Check required permissions.
        /// </summary>
        private IEnumerator CheckPermissions()
        {
            if (checkingPermissions) yield break;
            
            checkingPermissions = true;
            
            // Check Bluetooth permissions
            #if UNITY_ANDROID && !UNITY_EDITOR
            if (bleHidManager != null)
            {
                bool hasPermission = bleHidManager.CheckBluetoothPermissions();
                
                if (hasPermission != hasBluetoothPermission)
                {
                    hasBluetoothPermission = hasPermission;
                    if (hasPermission)
                    {
                        logger.Log("Bluetooth permissions granted");
                    }
                    else
                    {
                        logger.LogWarning("Bluetooth permissions not granted");
                    }
                }
            }
            
            // Check Accessibility Service
            if (localControl != null)
            {
                bool accessibilityEnabled = localControl.IsAccessibilityServiceEnabled();
                
                if (accessibilityEnabled != hasAccessibilityPermission)
                {
                    hasAccessibilityPermission = accessibilityEnabled;
                    if (accessibilityEnabled)
                    {
                        logger.Log("Accessibility service enabled");
                    }
                    else
                    {
                        logger.LogWarning("Accessibility service not enabled");
                    }
                }
            }
            #endif
            
            checkingPermissions = false;
            yield break;
        }
        
        /// <summary>
        /// Register a panel with the UI controller.
        /// </summary>
        /// <param name="tabName">The tab name to register the panel for.</param>
        /// <param name="panel">The panel instance.</param>
        public void RegisterPanel(string tabName, IBleHidPanel panel)
        {
            if (panel == null)
            {
                logger.LogError($"Attempted to register null panel for tab '{tabName}'");
                return;
            }
            
            // Initialize the panel
            panel.Initialize(bleHidManager);
            
            // Store the panel
            panels[tabName] = panel;
            
            // Set as active panel if appropriate
            if (tabName == "Local")
            {
                activeLocalPanel = panel;
                
                // Activate if this is the current tab
                if (currentMainTab == 0)
                {
                    panel.OnActivate();
                }
            }
            else if (tabName == "Remote")
            {
                activeRemotePanel = panel;
                
                // Activate if this is the current tab
                if (currentMainTab == 1)
                {
                    panel.OnActivate();
                }
            }
            
            logger.Log($"Panel '{tabName}' registered");
        }
        
        private void InitializeStyles()
        {
            // Header style
            headerStyle = new GUIStyle();
            headerStyle.fontSize = 24;
            headerStyle.fontStyle = FontStyle.Bold;
            headerStyle.alignment = TextAnchor.MiddleCenter;
            headerStyle.normal.textColor = Color.white;
            
            // Status style
            statusStyle = new GUIStyle();
            statusStyle.fontSize = 16;
            statusStyle.normal.textColor = Color.white;
            
            // Connection indicator style
            connectionIndicatorStyle = new GUIStyle();
            connectionIndicatorStyle.normal.background = MakeColorTexture(Color.green);
        }
        
        /// <summary>
        /// Ensure styles are set up for OnGUI.
        /// </summary>
        private void SetupGUIStyles()
        {
            // Set up fonts for better readability
            GUI.skin.button.fontSize = 18;
            GUI.skin.label.fontSize = 18;
            GUI.skin.textField.fontSize = 18;
            GUI.skin.box.fontSize = 18;
        }
        
        /// <summary>
        /// Open the app's permission settings.
        /// </summary>
        private void OpenAppPermissionSettings()
        {
            #if UNITY_ANDROID && !UNITY_EDITOR
            try
            {
                using (var unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer"))
                using (var currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity"))
                using (var intentObject = new AndroidJavaObject("android.content.Intent"))
                using (var uriObject = new AndroidJavaClass("android.net.Uri"))
                {
                    // Create Intent with proper action and data
                    intentObject.Call<AndroidJavaObject>("setAction", "android.settings.APPLICATION_DETAILS_SETTINGS");
                    
                    // Build the package URI
                    string packageName = currentActivity.Call<string>("getPackageName");
                    using (var uri = uriObject.CallStatic<AndroidJavaObject>("fromParts", "package", packageName, null))
                    {
                        intentObject.Call<AndroidJavaObject>("setData", uri);
                        intentObject.Call<AndroidJavaObject>("addFlags", 0x10000000); // FLAG_ACTIVITY_NEW_TASK
                        currentActivity.Call("startActivity", intentObject);
                    }
                }
            }
            catch (Exception e)
            {
                logger.LogError($"Failed to open app settings: {e.Message}");
            }
            #endif
        }
        
        private Texture2D MakeColorTexture(Color color)
        {
            Texture2D texture = new Texture2D(1, 1);
            texture.SetPixel(0, 0, color);
            texture.Apply();
            return texture;
        }
        #endregion
        
        #region Event Handlers
        private void OnInitializeComplete(bool success, string message)
        {
            if (success)
            {
                logger.Log($"BLE HID initialized successfully: {message}");
            }
            else
            {
                logger.LogError($"BLE HID initialization failed: {message}");
            }
        }
        
        private void OnAdvertisingStateChanged(bool advertising, string message)
        {
            if (advertising)
            {
                logger.Log($"Advertising started: {message}");
            }
            else
            {
                logger.Log($"Advertising stopped: {message}");
            }
        }
        
        private void OnConnectionStateChanged(bool connected, string deviceName, string deviceAddress)
        {
            if (connected)
            {
                logger.Log($"Device connected: {deviceName} ({deviceAddress})");
            }
            else
            {
                logger.Log("Device disconnected");
            }
        }
        
        private void OnPairingStateChanged(string status, string deviceAddress)
        {
            logger.Log($"Pairing state changed: {status} ({deviceAddress})");
        }
        
        private void OnError(int errorCode, string errorMessage)
        {
            logger.LogError($"Error {errorCode}: {errorMessage}");
        }
        
        private void OnDebugLog(string message)
        {
            logger.Log($"Debug: {message}");
        }
        #endregion
        
        #region Temporary Dummy Panels (To be replaced)
        /// <summary>
        /// Temporary dummy local panel implementation.
        /// </summary>
        private class DummyLocalPanel : BaseBleHidPanel
        {
            public override bool RequiresConnectedDevice => false;
            
            protected override void DrawPanelContent()
            {
                GUILayout.Label("Local Panel (Placeholder)");
                GUILayout.Label("This will be replaced with actual implementation");
            }
        }
        
        /// <summary>
        /// Temporary dummy remote panel implementation.
        /// </summary>
        private class DummyRemotePanel : BaseBleHidPanel
        {
            public override bool RequiresConnectedDevice => true;
            
            protected override void DrawPanelContent()
            {
                GUILayout.Label("Remote Panel (Placeholder)");
                GUILayout.Label("This will be replaced with actual implementation");
            }
        }
        #endregion
    }
}
