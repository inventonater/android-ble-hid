using System.Collections;
using UnityEngine;

namespace Inventonater.BleHid.Examples
{
    /// <summary>
    /// Example class demonstrating how to use the accessibility navigation and selection features.
    /// </summary>
    public class AccessibilityExample : MonoBehaviour
    {
        private AccessibilityServiceBridge _accessibilityBridge;
        
        void Start()
        {
            // Get the JavaBridge instance from your BleHid manager or create a new one
            JavaBridge javaBridge = new JavaBridge();
            
            // Create the accessibility bridge
            _accessibilityBridge = new AccessibilityServiceBridge(javaBridge);
            
            // Initialize the accessibility service
            StartCoroutine(InitializeAccessibility());
        }
        
        private IEnumerator InitializeAccessibility()
        {
            // Wait for the accessibility service to initialize
            var initTask = _accessibilityBridge.Initialize();
            yield return new WaitUntil(() => initTask.IsCompleted);
            
            if (initTask.Result)
            {
                Debug.Log("Accessibility service initialized successfully");
            }
            else
            {
                Debug.LogError("Failed to initialize accessibility service");
                
                // Open accessibility settings to enable the service
                _accessibilityBridge.OpenAccessibilitySettings();
            }
        }
        
        /// <summary>
        /// Example of navigating through UI elements using cardinal directions
        /// </summary>
        public void NavigateExample()
        {
            // Navigate down
            _accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Down);
            
            // Navigate right
            _accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Right);
            
            // Navigate back
            _accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Back);
        }
        
        /// <summary>
        /// Example of clicking on the currently focused accessibility node
        /// </summary>
        public void ClickFocusedNodeExample()
        {
            bool success = _accessibilityBridge.ClickFocusedNode();
            Debug.Log($"Click on focused node: {(success ? "succeeded" : "failed")}");
        }
        
        /// <summary>
        /// Example of performing a long click on the currently focused accessibility node
        /// </summary>
        public void LongClickFocusedNodeExample()
        {
            bool success = _accessibilityBridge.PerformFocusedNodeAction(AccessibilityAction.LongClick);
            Debug.Log($"Long click on focused node: {(success ? "succeeded" : "failed")}");
        }
        
        /// <summary>
        /// Example of a complete navigation and selection workflow
        /// </summary>
        public IEnumerator NavigateAndSelectExample()
        {
            // Navigate to the first item
            _accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Down);
            yield return new WaitForSeconds(0.5f);
            
            // Navigate to the second item
            _accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Down);
            yield return new WaitForSeconds(0.5f);
            
            // Navigate to the third item
            _accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Down);
            yield return new WaitForSeconds(0.5f);
            
            // Click on the currently focused item
            _accessibilityBridge.ClickFocusedNode();
            yield return new WaitForSeconds(0.5f);
            
            // Navigate back
            _accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Back);
        }
        
        /// <summary>
        /// Example of performing various accessibility actions
        /// </summary>
        public void PerformActionsExample()
        {
            // Scroll forward
            _accessibilityBridge.PerformFocusedNodeAction(AccessibilityAction.ScrollForward);
            
            // Expand a node
            _accessibilityBridge.PerformFocusedNodeAction(AccessibilityAction.Expand);
            
            // Select a node
            _accessibilityBridge.PerformFocusedNodeAction(AccessibilityAction.Select);
        }
    }
}
