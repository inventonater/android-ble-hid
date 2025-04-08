using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Local
{
    /// <summary>
    /// Panel for local device control.
    /// </summary>
    public class LocalControlPanel : BaseBleHidPanel
    {
        private BleHidLocalControl localControl;
        private bool isInitialized = false;
        private bool isInitializing = false;
        private bool permissionCheckActive = false;
        
        // Subtab panels
        private LocalMediaPanel mediaPanel;
        private LocalSystemPanel systemPanel;
        private LocalNavigationPanel navigationPanel;
        private LocalTouchPanel touchPanel;
        
        public override bool RequiresConnectedDevice => false;
        
        public override void Initialize(BleHidManager manager)
        {
            base.Initialize(manager);
            
            // Define subtabs
            subtabNames.Add("Media");
            subtabNames.Add("System");
            subtabNames.Add("Navigation");
            subtabNames.Add("Touch");
            
            // Initialize subtab panels
            mediaPanel = new LocalMediaPanel();
            systemPanel = new LocalSystemPanel();
            navigationPanel = new LocalNavigationPanel();
            touchPanel = new LocalTouchPanel();
            
            // Initialize each panel
            mediaPanel.Initialize(manager);
            systemPanel.Initialize(manager);
            navigationPanel.Initialize(manager);
            touchPanel.Initialize(manager);
            
            // Start local control initialization
            StartInitialization();
        }
        
        public override void OnActivate()
        {
            base.OnActivate();
            
            // Activate the current subtab
            switch (currentSubtab)
            {
                case 0:
                    mediaPanel.OnActivate();
                    break;
                case 1:
                    systemPanel.OnActivate();
                    break;
                case 2:
                    navigationPanel.OnActivate();
                    break;
                case 3:
                    touchPanel.OnActivate();
                    break;
            }
        }
        
        public override void OnDeactivate()
        {
            base.OnDeactivate();
            
            // Deactivate the current subtab
            switch (currentSubtab)
            {
                case 0:
                    mediaPanel.OnDeactivate();
                    break;
                case 1:
                    systemPanel.OnDeactivate();
                    break;
                case 2:
                    navigationPanel.OnDeactivate();
                    break;
                case 3:
                    touchPanel.OnDeactivate();
                    break;
            }
        }
        
        public override void Update()
        {
            base.Update();
            
            // Pass update to the current subtab
            switch (currentSubtab)
            {
                case 0:
                    mediaPanel.Update();
                    break;
                case 1:
                    systemPanel.Update();
                    break;
                case 2:
                    navigationPanel.Update();
                    break;
                case 3:
                    touchPanel.Update();
                    break;
            }
        }
        
        protected override void DrawPanelContent()
        {
            // This panel uses subtabs, so this method will not be called directly
            GUILayout.Label("Local Control Panel", titleStyle);
        }
        
        protected override void DrawSubtab(int subtabIndex)
        {
            // Check if local control is initialized or if we're in editor mode
            if (!isInitialized && !isInitializing)
            {
                DrawInitializationUI();
                return;
            }
            
            // Draw the selected subtab
            switch (subtabIndex)
            {
                case 0:
                    mediaPanel.DrawPanel();
                    break;
                case 1:
                    systemPanel.DrawPanel();
                    break;
                case 2:
                    navigationPanel.DrawPanel();
                    break;
                case 3:
                    touchPanel.DrawPanel();
                    break;
            }
        }
        
        private void DrawInitializationUI()
        {
            GUILayout.BeginVertical(GUI.skin.box, GUILayout.ExpandHeight(true));
            
            GUILayout.Label("Local Control Setup", titleStyle);
            GUILayout.Space(10);
            
            if (localControl != null)
            {
                bool accessibilityEnabled = false;
                try {
                    accessibilityEnabled = localControl.IsAccessibilityServiceEnabled();
                } catch (System.Exception) { /* Handle silently */ }
                
                // Show current permission status
                GUIStyle statusStyle = new GUIStyle(GUI.skin.label);
                statusStyle.fontSize = 16;
                statusStyle.fontStyle = FontStyle.Bold;
                
                if (accessibilityEnabled) {
                    statusStyle.normal.textColor = Color.green;
                    GUILayout.Label("✓ Accessibility Service: ENABLED", statusStyle);
                } else {
                    statusStyle.normal.textColor = Color.red;
                    GUILayout.Label("✗ Accessibility Service: DISABLED", statusStyle);
                }
                
                GUILayout.Space(15);
                
                // Informative text explaining the permissions
                GUILayout.Label("The Accessibility Service allows this app to:", GUI.skin.label);
                
                GUIStyle bulletStyle = new GUIStyle(GUI.skin.label);
                bulletStyle.padding.left = 20;
                GUILayout.Label("• Control media playback on your device", bulletStyle);
                GUILayout.Label("• Perform navigation gestures (swipes, taps)", bulletStyle);
                GUILayout.Label("• Access system controls (volume, brightness)", bulletStyle);
                
                GUILayout.Space(20);
                
                // Different buttons based on status
                if (!accessibilityEnabled) {
                    // Make the enable button prominent
                    GUIStyle enableStyle = new GUIStyle(GUI.skin.button);
                    enableStyle.fontSize = 16;
                    enableStyle.fontStyle = FontStyle.Bold;
                    
                    if (GUILayout.Button("ENABLE ACCESSIBILITY SERVICE", enableStyle, GUILayout.Height(70)))
                    {
                        if (localControl != null) {
                            localControl.OpenAccessibilitySettings();
                            logger.Log("Opening Accessibility Settings...");
                            // Start permission check coroutine
                            bleHidManager.StartCoroutine(PollForAccessibilityPermission());
                        }
                    }
                    
                    GUILayout.Space(10);
                    GUILayout.Label("After enabling the service in Settings, return to this app.", GUI.skin.label);
                }
                
                // Always show initialization button (as fallback)
                if (GUILayout.Button(isInitializing ? "Initializing..." : "Reinitialize Local Control", 
                                     GUILayout.Height(50)))
                {
                    if (!isInitializing) {
                        StartInitialization();
                    }
                }
            }
            else
            {
                // If localControl isn't available yet, show simpler UI
                GUILayout.Label("Local control requires Accessibility Service permissions to function.");
                GUILayout.Label("These permissions allow the app to control media playback and system navigation.");
                
                GUILayout.Space(20);
                
                if (GUILayout.Button("Initialize Local Control", GUILayout.Height(60)))
                {
                    StartInitialization();
                }
            }
            
            if (isInitializing)
            {
                GUILayout.Space(10);
                GUILayout.Label("Initializing local control...");
            }
            
            GUILayout.EndVertical();
        }
        
        /// <summary>
        /// Periodically checks if the user has enabled accessibility permissions after
        /// being directed to the system settings.
        /// </summary>
        private IEnumerator PollForAccessibilityPermission()
        {
            if (permissionCheckActive) yield break;
            permissionCheckActive = true;
            
            logger.Log("Waiting for accessibility permission...");
            int checkCount = 0;
            const int MAX_CHECKS = 20; // Limit the number of checks
            
            while (checkCount < MAX_CHECKS)
            {
                // Give time for the user to enable permissions and return
                yield return new WaitForSeconds(1.0f);
                checkCount++;
                
                if (localControl == null) break;
                
                bool accessibilityEnabled = false;
                try {
                    accessibilityEnabled = localControl.IsAccessibilityServiceEnabled();
                } catch (System.Exception) { /* Continue silently */ }
                
                if (accessibilityEnabled)
                {
                    logger.Log("Accessibility service enabled successfully!");
                    isInitialized = true;
                    break;
                }
            }
            
            permissionCheckActive = false;
            
            // Force UI refresh
            Repaint();
        }
        
        /// <summary>
        /// Forces a UI repaint to reflect updated permission status.
        /// </summary>
        private void Repaint()
        {
            // Force layout recalculation and redraw
            #if UNITY_EDITOR
            UnityEditor.EditorUtility.SetDirty(bleHidManager.gameObject);
            #endif
        }
        
        private void StartInitialization()
        {
            if (isInitializing) return;
            
            isInitializing = true;
            
            #if UNITY_ANDROID && !UNITY_EDITOR
            // Start a coroutine to initialize BleHidLocalControl
            bleHidManager.StartCoroutine(InitializeLocalControl());
            #else
            // In editor, just pretend it's initialized
            isInitialized = true;
            isInitializing = false;
            logger.Log("Local control initialized (editor mode)");
            #endif
        }
        
        private IEnumerator InitializeLocalControl()
        {
            logger.Log("Initializing local control...");
            
            // Get instance of BleHidLocalControl
            try {
                localControl = BleHidLocalControl.Instance;
            }
            catch (System.Exception e) {
                logger.LogError($"Error creating local control instance: {e.Message}");
                isInitializing = false;
                yield break;
            }
            
            if (localControl == null)
            {
                logger.LogError("Failed to create local control instance");
                isInitializing = false;
                yield break;
            }
            
            // Initialize with retries
            yield return bleHidManager.StartCoroutine(localControl.Initialize(5));
            
            // Check if accessibility service is enabled
            bool accessibilityEnabled = false;
            try {
                accessibilityEnabled = localControl.IsAccessibilityServiceEnabled();
            }
            catch (System.Exception e) {
                logger.LogError($"Error checking accessibility service: {e.Message}");
            }
            
            if (!accessibilityEnabled)
            {
                logger.LogWarning("Accessibility service not enabled - user action required");
                // Don't set isInitialized since we still need the user to enable accessibility
            }
            else
            {
                logger.Log("Local control fully initialized");
                isInitialized = true;
            }
            
            isInitializing = false;
            
            // Force UI refresh
            Repaint();
        }
    }
}
