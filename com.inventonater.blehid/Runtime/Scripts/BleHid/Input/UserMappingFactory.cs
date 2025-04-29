namespace Inventonater.BleHid
{
    /// <summary>
    /// Factory for creating InputDeviceMapping objects based on user mappings.
    /// </summary>
    public static class UserMappingFactory
    {
        /// <summary>
        /// Creates an InputDeviceMapping based on user mappings.
        /// </summary>
        /// <param name="bridge">The BleBridge to use for default actions</param>
        /// <returns>A new InputDeviceMapping with user mappings applied</returns>
        public static InputDeviceMapping CreateUserMapping(BleBridge bridge)
        {
            var mapping = new InputDeviceMapping("UserMapping");
            
            // Map Primary button actions
            MapButtonAction(mapping, "primary_tap", new (InputEvent.Id.Primary, InputEvent.Temporal.Tap));
            
            MapButtonAction(mapping, "primary_double_tap", new (InputEvent.Id.Primary, InputEvent.Temporal.DoubleTap));
            
            MapButtonAction(mapping, "primary_press", new (InputEvent.Id.Primary, InputEvent.Temporal.Press)); // Screen center
            
            MapButtonAction(mapping, "primary_release", new (InputEvent.Id.Primary, InputEvent.Temporal.Release));
            
            // Map Secondary button actions
            MapButtonAction(mapping, "secondary_tap", new (InputEvent.Id.Secondary, InputEvent.Temporal.Tap));
            
            MapButtonAction(mapping, "secondary_double_tap", new (InputEvent.Id.Secondary, InputEvent.Temporal.DoubleTap));
            
            // Map Tertiary button actions
            MapButtonAction(mapping, "tertiary_tap", new (InputEvent.Id.Tertiary, InputEvent.Temporal.Tap));
            
            // Map direction actions
            MapButtonAction(mapping, "direction_up", new (InputEvent.Direction.Up));
            
            MapButtonAction(mapping, "direction_right", new(InputEvent.Direction.Right));
            
            MapButtonAction(mapping, "direction_down", new (InputEvent.Direction.Down));
            
            MapButtonAction(mapping, "direction_left", new (InputEvent.Direction.Left));
            
            // Add axis mappings
            mapping.Add(new SingleIncrementalAxisMapping(Axis.Z, () => bridge.AccessibilityServiceBridge.VolumeUp(), () => bridge.AccessibilityServiceBridge.VolumeDown()));
            
            return mapping;
        }

        private static void MapButtonAction(InputDeviceMapping mapping, string buttonId, InputEvent inputEvent)
        {
            if (UserMappingManager.TryGetActionForButton(buttonId, out var actionId) && ActionRegistry.TryGetAction(actionId, out var action))
            {
                mapping.Add(inputEvent, () => action.Invoke());
                LoggingManager.Instance.Log($"UserMappingFactory: Mapped {buttonId} to custom action {actionId}");
            }
        }
    }
}
