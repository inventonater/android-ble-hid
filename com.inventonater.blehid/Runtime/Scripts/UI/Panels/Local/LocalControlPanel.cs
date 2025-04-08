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
            GUILayout.BeginVertical(GUI.skin.box);
            
            GUILayout.Label("Local Control Setup", titleStyle);
            GUILayout.Space(10);
            
            GUILayout.Label("Local control requires Accessibility Service permissions to function.");
            GUILayout.Label("These permissions allow the app to control media playback and system navigation.");
            
            GUILayout.Space(20);
            
            if (GUILayout.Button("Initialize Local Control", GUILayout.Height(60)))
            {
                StartInitialization();
            }
            
            GUILayout.Space(10);
            
            if (isInitializing)
            {
                GUILayout.Label("Initializing local control...");
            }
            
            GUILayout.EndVertical();
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
                logger.LogWarning("Accessibility service not enabled");
            }
            else
            {
                logger.Log("Local control fully initialized");
                isInitialized = true;
            }
            
            isInitializing = false;
        }
    }
}
