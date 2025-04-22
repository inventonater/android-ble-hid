# BleHid Configuration System

This folder contains the components that make up the BleHid configuration system, which allows for flexible mapping of input events to BLE HID actions.

## Overview

The configuration system enables:

1. Mapping button events to mouse/keyboard/media actions
2. Mapping directional inputs to keyboard keys
3. Mapping axis inputs to mouse movement or incremental actions
4. Serializing and deserializing configurations to/from JSON
5. Saving and loading custom configurations
6. Serializing filter configurations for consistent input processing

## Key Components

### MappingConfiguration

The core data structure that holds all mapping information. It contains:

- Button mappings (e.g., "Primary.Press" → "Mouse.LeftButton.Press")
- Direction mappings (e.g., "Up" → "Keyboard.Key:UpArrow")
- Axis mappings (e.g., "XY" → Mouse movement with filter settings)

### MappingConfigurationManager

Handles loading, saving, and creating default configurations. It also manages serialization and deserialization of filter objects.

### ActionRegistry

Registers available actions that can be mapped to input events. It provides a factory pattern for creating action delegates based on string identifiers.

### ActionResolver

Resolves action strings to actual delegates that can be executed when input events occur.

### AxisMappingFactory

Creates axis mapping objects based on configuration entries. It handles both mouse position filters and incremental axis mappings.

### FilterSerializer

Provides utilities for serializing and deserializing filter objects, allowing filter configurations to be saved and loaded.

## Usage

### Creating a Configuration

```csharp
var configManager = new MappingConfigurationManager();
var config = configManager.CreateDefaultConfiguration();

// Customize the configuration
config.ButtonMappings.Add(new ButtonMappingEntry 
{ 
    InputEvent = "Custom.Press", 
    Action = "Mouse.RightButton.Press" 
});

// Save the configuration
configManager.SaveConfiguration(config, "path/to/config.json");
```

### Loading a Configuration

```csharp
var configManager = new MappingConfigurationManager();
var config = configManager.LoadConfiguration("path/to/config.json");
```

### Applying a Configuration

```csharp
// Assuming you have an InputDeviceMapping instance
inputDeviceMapping.ApplyConfiguration(config, bleBridge, actionRegistry);
```

## Filter Serialization

The system supports serializing and deserializing filter objects, which allows for saving and loading complex filter configurations. This is done using the `FilterSerializer` class:

```csharp
// Serialize a filter to JSON
var filter = new OneEuroFilter(1.0f, 0.007f);
string json = FilterSerializer.Serialize(filter);

// Deserialize a filter from JSON
var deserializedFilter = FilterSerializer.Deserialize(json);
```

## Extending the System

### Adding New Actions

To add new actions, register them in the `ActionRegistry`:

```csharp
actionRegistry.RegisterAction("Custom.Action", parameters => {
    return () => {
        // Your action implementation here
    };
});
```

### Adding New Filter Types

1. Create a new filter class that implements `IInputFilter`
2. Add the filter type to the `InputFilterFactory.FilterType` enum
3. Update the `InputFilterFactory.CreateFilter` method to create your filter
4. Update the `MousePositionFilter.DetermineFilterType` method to recognize your filter
