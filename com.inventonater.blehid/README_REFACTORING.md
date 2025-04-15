# BLE HID Component Refactoring

This document outlines the refactoring changes made to improve the BLE HID component architecture, enhancing code organization, maintainability, and extensibility.

## Key Improvements

1. **Enhanced UIHelper Class**
   - Added dropdown control helpers
   - Added toggle button controls
   - Added slider drag handling
   - Added formatting utilities
   - Added dialog display helpers

2. **Extracted Processing Logic**
   - Created `PointerInputProcessor` to centralize input handling
   - Created `PerformanceTracker` for metrics collection

3. **Renamed Components**
   - Renamed `BleHidSimpleUI` to `BleHidControlPanel` for clarity

4. **Improved Organization**
   - Centralized component initialization
   - Improved event handling
   - Better separation of UI drawing and logic

## New Components

### PointerInputProcessor

Handles all pointer input logic (touch, mouse, external devices) with proper filtering and processing.

```csharp
// Example usage
var processor = new PointerInputProcessor(bleHidManager, logger, isEditorMode);
processor.SetInputFilter(inputFilter);
processor.SetSensitivity(globalScale, horizontalSensitivity, verticalSensitivity);
processor.HandlePointerInput(position, state, "Touch");
```

### PerformanceTracker

Tracks and calculates performance metrics like message rates and frame rates.

```csharp
// Example usage
var tracker = new PerformanceTracker();
tracker.Update(); // Call every frame
tracker.TrackMessage(); // Call when message is sent
float messagesPerSecond = tracker.MessagesPerSecond;
float fps = tracker.FramesPerSecond;
```

## Using the Updated Components

### BleHidControlPanel

The main controller component (formerly BleHidSimpleUI) that manages all UI components and their interactions.

1. Add the `BleHidControlPanel` component to a GameObject in your scene
2. The component will automatically initialize all required UI components
3. UI is displayed using Unity's immediate mode GUI system (OnGUI)

### Example Scene

An example scene is included that demonstrates how to use the refactored components:
- `Examples/BleHidExampleScene.unity`

## Enhanced UIHelper Methods

### DrawDropdownControl

```csharp
// Create a dropdown control with proper styling and consistent behavior
bool selectionChanged = UIHelper.DrawDropdownControl(
    "Priority:",  // Label
    currentValue, // Current selected value
    options,      // Array of options
    ref isExpanded, // Reference to expanded state boolean
    (index) => {    // Selection changed callback
        // Handle selection change
    }
);
```

### ToggleButton

```csharp
// Create a toggle button that switches between two states
bool newState = UIHelper.ToggleButton(
    "Feature:",   // Label
    currentState, // Current state
    "Enabled",    // Text when enabled
    "Disabled",   // Text when disabled
    UIHelper.StandardButtonOptions
);
```

### HandleSliderDragging

```csharp
// Handle slider being dragged with proper tracking
bool valueApplied = UIHelper.HandleSliderDragging(
    currentValue,    // Current slider value
    previousValue,   // Previous slider value
    ref isDragging,  // Reference to dragging state
    () => {          // Action to call when value changes and drag ends
        // Apply value change
    }
);
```

## Upgrade Guide

If you're using the previous version of the components:

1. Replace uses of `BleHidSimpleUI` with `BleHidControlPanel`
2. If you've custom-extended any components, review the new helper classes
3. Update any UI code to use the new UIHelper methods for consistent styling
4. If you've customized mouse input handling, consider using the `PointerInputProcessor`

## Compatibility

These changes maintain full compatibility with the existing API while providing new functionality and improved organization. No changes to your existing code should be required unless you've directly extended the refactored components.
