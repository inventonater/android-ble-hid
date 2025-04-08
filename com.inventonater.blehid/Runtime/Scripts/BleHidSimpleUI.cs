using System;
using System.Collections;
using UnityEngine;
using Inventonater.BleHid.UI;
using Inventonater.BleHid.UI.Panels.Local;
using Inventonater.BleHid.UI.Panels.Remote;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Simple UI for controlling BLE HID functionality.
    /// This class serves as an entry point to the modern UI system.
    /// </summary>
    public class BleHidSimpleUI : MonoBehaviour
    {
        private BleHidUIController uiController;

        private void Start()
        {
            // Create UI controller if not already present
            if (FindObjectOfType<BleHidUIController>() == null)
            {
                GameObject uiControllerObj = new GameObject("BleHidUIController");
                uiController = uiControllerObj.AddComponent<BleHidUIController>();
                
                // Keep the controller alive between scenes
                DontDestroyOnLoad(uiControllerObj);
            }
            else
            {
                uiController = FindObjectOfType<BleHidUIController>();
            }

            // Register UI panels
            StartCoroutine(RegisterPanels());
        }

        /// <summary>
        /// Register all UI panels with the controller.
        /// </summary>
        private IEnumerator RegisterPanels()
        {
            // Wait a frame to ensure the UI controller is fully initialized
            yield return null;

            if (uiController != null)
            {
                try
                {
                    // Register Local panels
                    uiController.RegisterPanel("Local", new LocalControlPanel());

                    // Register Remote panels
                    uiController.RegisterPanel("Remote", new RemoteControlPanel());

                    BleHidLogger.Instance.Log("UI panels registered successfully");
                }
                catch (Exception e)
                {
                    BleHidLogger.Instance.LogError($"Error registering UI panels: {e.Message}");
                }
            }
            else
            {
                Debug.LogError("UI Controller not found");
            }
        }
    }
}
