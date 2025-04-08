using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Remote
{
    /// <summary>
    /// Panel for remote device control over BLE HID.
    /// </summary>
    public class RemoteControlPanel : BaseBleHidPanel
    {
        // Subtab panels
        private RemoteMediaPanel mediaPanel;
        private RemoteMousePanel mousePanel;
        private RemoteKeyboardPanel keyboardPanel;
        
        public override bool RequiresConnectedDevice => true;
        
        public override void Initialize(BleHidManager manager)
        {
            base.Initialize(manager);
            
            // Define subtabs
            subtabNames.Add("Media");
            subtabNames.Add("Mouse");
            subtabNames.Add("Keyboard");
            
            // Initialize subtab panels
            mediaPanel = new RemoteMediaPanel();
            mousePanel = new RemoteMousePanel();
            keyboardPanel = new RemoteKeyboardPanel();
            
            // Initialize each panel
            mediaPanel.Initialize(manager);
            mousePanel.Initialize(manager);
            keyboardPanel.Initialize(manager);
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
                    mousePanel.OnActivate();
                    break;
                case 2:
                    keyboardPanel.OnActivate();
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
                    mousePanel.OnDeactivate();
                    break;
                case 2:
                    keyboardPanel.OnDeactivate();
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
                    mousePanel.Update();
                    break;
                case 2:
                    keyboardPanel.Update();
                    break;
            }
        }
        
        protected override void DrawPanelContent()
        {
            // This panel uses subtabs, so this method will not be called directly
            GUILayout.Label("Remote Control Panel", titleStyle);
        }
        
        protected override void DrawSubtab(int subtabIndex)
        {
            // Check if BLE is connected
            if (bleHidManager == null || !bleHidManager.IsConnected)
            {
                GUILayout.BeginVertical(GUI.skin.box);
                GUILayout.Label("Not Connected", titleStyle);
                GUILayout.Label("Please connect to a device to use remote controls");
                
                if (GUILayout.Button("Start Advertising", GUILayout.Height(60)))
                {
                    if (!bleHidManager.IsAdvertising)
                    {
                        bleHidManager.StartAdvertising();
                        logger.Log("Started advertising");
                    }
                    else
                    {
                        logger.Log("Already advertising");
                    }
                }
                
                GUILayout.EndVertical();
                
                return;
            }
            
            // Draw the selected subtab
            switch (subtabIndex)
            {
                case 0:
                    mediaPanel.DrawPanel();
                    break;
                case 1:
                    mousePanel.DrawPanel();
                    break;
                case 2:
                    keyboardPanel.DrawPanel();
                    break;
            }
        }
    }
}
