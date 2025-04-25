# Accessibility Navigation and Selection

This document provides information about the accessibility navigation and selection features in the BleHid plugin.

## Overview

The BleHid plugin provides functionality to navigate through UI elements using cardinal directions (up, down, left, right) and perform actions on the currently focused element, such as clicking, long-clicking, or other accessibility actions.

## Features

- **Directional Navigation**: Navigate through UI elements using cardinal directions (up, down, left, right)
- **System Navigation**: Perform system navigation actions like back, home, and recents
- **Selection**: Click on the currently focused accessibility node
- **Advanced Actions**: Perform various accessibility actions on the focused node, such as long-click, scroll, expand/collapse, etc.

## Usage

### Initialization

Before using the accessibility features, you need to initialize the `AccessibilityServiceBridge`:

```csharp
// Get the JavaBridge instance from your BleHid manager or create a new one
JavaBridge javaBridge = new JavaBridge();

// Create the accessibility bridge
AccessibilityServiceBridge accessibilityBridge = new AccessibilityServiceBridge(javaBridge);

// Initialize the accessibility service
await accessibilityBridge.Initialize();
```

### Navigation

To navigate through UI elements using cardinal directions:

```csharp
// Navigate down
accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Down);

// Navigate right
accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Right);

// Navigate back
accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Back);
```

Available navigation directions:
- `Up`
- `Down`
- `Left`
- `Right`
- `Back`
- `Home`
- `Recents`

### Selection

To click on the currently focused accessibility node:

```csharp
bool success = accessibilityBridge.ClickFocusedNode();
```

### Advanced Actions

To perform other accessibility actions on the focused node:

```csharp
// Long click
bool success = accessibilityBridge.PerformFocusedNodeAction(AccessibilityAction.LongClick);

// Scroll forward
bool success = accessibilityBridge.PerformFocusedNodeAction(AccessibilityAction.ScrollForward);

// Expand a node
bool success = accessibilityBridge.PerformFocusedNodeAction(AccessibilityAction.Expand);
```

Available accessibility actions are defined in the `AccessibilityAction` enum:
- `Click`
- `LongClick`
- `Focus`
- `ClearFocus`
- `Select`
- `ClearSelection`
- `AccessibilityFocus`
- `ClearAccessibilityFocus`
- `ScrollForward`
- `ScrollBackward`
- `Copy`
- `Paste`
- `Cut`
- `SetSelection`
- `Expand`
- `Collapse`
- `Dismiss`
- `SetText`

## Example

Here's a complete example of navigating through UI elements and clicking on the focused node:

```csharp
using System.Collections;
using UnityEngine;
using Inventonater.BleHid;

public class AccessibilityController : MonoBehaviour
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
    
    public IEnumerator NavigateAndSelect()
    {
        // Navigate to the first item
        _accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Down);
        yield return new WaitForSeconds(0.5f);
        
        // Navigate to the second item
        _accessibilityBridge.Navigate(AccessibilityServiceBridge.NavigationDirection.Down);
        yield return new WaitForSeconds(0.5f);
        
        // Click on the currently focused item
        _accessibilityBridge.ClickFocusedNode();
    }
}
```

## Requirements

- Android device running Android 6.0 (API level 23) or higher
- Accessibility service enabled in the device settings

## Troubleshooting

If the accessibility features are not working:

1. Make sure the accessibility service is enabled in the device settings
2. Check if the initialization was successful
3. Verify that the device supports accessibility services
4. Check the logs for any error messages

## Implementation Details

The accessibility features are implemented using the Android Accessibility Framework. The `AccessibilityServiceBridge` class provides a bridge between Unity and the Android accessibility services.

The navigation and selection functionality is implemented using the following components:

- `AccessibilityServiceBridge.cs`: C# bridge to the Android accessibility service
- `AccessibilityAction.cs`: Enum for accessibility actions
- `LocalAccessibilityService.java`: Android accessibility service implementation
- `LocalInputController.java`: Controller for input operations
- `LocalInputManager.java`: Manager for input operations
