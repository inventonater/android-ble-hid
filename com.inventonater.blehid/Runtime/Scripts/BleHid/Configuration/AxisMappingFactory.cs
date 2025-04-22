using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class AxisMappingFactory
    {
        private readonly BleBridge _bleBridge;
        private readonly ActionResolver _actionResolver;
        
        public AxisMappingFactory(BleBridge bleBridge, ActionResolver actionResolver)
        {
            _bleBridge = bleBridge;
            _actionResolver = actionResolver;
        }
        
        public IAxisMapping CreateAxisMapping(AxisMappingEntry entry)
        {
            switch (entry.Type.ToLowerInvariant())
            {
                case "mouse":
                    return CreateMouseAxisMapping(entry);
                case "incremental":
                    return CreateIncrementalAxisMapping(entry);
                default:
                    Debug.LogWarning($"Unknown axis mapping type: {entry.Type}");
                    return null;
            }
        }
        
        private MousePositionFilter CreateMouseAxisMapping(AxisMappingEntry entry)
        {
            bool flipY = true;
            float horizontalSensitivity = 3.0f;
            float verticalSensitivity = 3.0f;
            string filterType = "OneEuro";
            
            if (entry.Settings != null)
            {
                if (entry.Settings.TryGetValue("flipY", out var flipYObj) && flipYObj is bool flipYValue)
                {
                    flipY = flipYValue;
                }
                
                if (entry.Settings.TryGetValue("horizontalSensitivity", out var hSensObj))
                {
                    if (hSensObj is float hSensFloat)
                    {
                        horizontalSensitivity = hSensFloat;
                    }
                    else if (hSensObj is double hSensDouble)
                    {
                        horizontalSensitivity = (float)hSensDouble;
                    }
                }
                
                if (entry.Settings.TryGetValue("verticalSensitivity", out var vSensObj))
                {
                    if (vSensObj is float vSensFloat)
                    {
                        verticalSensitivity = vSensFloat;
                    }
                    else if (vSensObj is double vSensDouble)
                    {
                        verticalSensitivity = (float)vSensDouble;
                    }
                }
                
                if (entry.Settings.TryGetValue("filter", out var filterObj) && filterObj is string filterStr)
                {
                    filterType = filterStr;
                }
            }
            
            var mouseFilter = new MousePositionFilter(_bleBridge.Mouse, flipY);
            mouseFilter.HorizontalSensitivity = horizontalSensitivity;
            mouseFilter.VerticalSensitivity = verticalSensitivity;
            
            // Check if we have a serialized filter
            if (!string.IsNullOrEmpty(entry.SerializedFilter))
            {
                // Deserialize the filter
                var filter = FilterSerializer.Deserialize(entry.SerializedFilter);
                if (filter != null)
                {
                    mouseFilter.SetInputFilter(filter);
                    Debug.Log($"Applied deserialized filter: {filter.Name}");
                }
                else
                {
                    // Fallback to filter type and settings if deserialization fails
                    ApplyFilterTypeAndSettings(mouseFilter, filterType, entry.FilterSettings);
                }
            }
            else
            {
                // Use filter type and settings
                ApplyFilterTypeAndSettings(mouseFilter, filterType, entry.FilterSettings);
            }
            
            return mouseFilter;
        }
        
        private AxisMappingIncremental CreateIncrementalAxisMapping(AxisMappingEntry entry)
        {
            BleHidAxis axis = BleHidAxis.Z;
            float interval = 0.02f;
            
            if (!string.IsNullOrEmpty(entry.Axis))
            {
                if (Enum.TryParse(entry.Axis, true, out BleHidAxis parsedAxis))
                {
                    axis = parsedAxis;
                }
            }
            
            if (entry.Settings != null && entry.Settings.TryGetValue("interval", out var intervalObj))
            {
                if (intervalObj is float intervalFloat)
                {
                    interval = intervalFloat;
                }
                else if (intervalObj is double intervalDouble)
                {
                    interval = (float)intervalDouble;
                }
            }
            
            Action incrementAction = () => {};
            Action decrementAction = () => {};
            
            if (!string.IsNullOrEmpty(entry.IncrementAction))
            {
                incrementAction = _actionResolver.ResolveAction(entry.IncrementAction);
            }
            
            if (!string.IsNullOrEmpty(entry.DecrementAction))
            {
                decrementAction = _actionResolver.ResolveAction(entry.DecrementAction);
            }
            
            return new AxisMappingIncremental(axis, incrementAction, decrementAction, interval);
        }
        
        private void ApplyFilterTypeAndSettings(MousePositionFilter mouseFilter, string filterType, Dictionary<string, object> filterSettings)
        {
            // Set filter type
            var filterTypeEnum = InputFilterFactory.FilterType.OneEuro;
            if (Enum.TryParse(filterType, true, out InputFilterFactory.FilterType parsedFilterType))
            {
                filterTypeEnum = parsedFilterType;
            }
            
            mouseFilter.SetInputFilter(filterTypeEnum);
            
            // Apply filter-specific settings
            if (filterSettings != null && mouseFilter.Filter != null)
            {
                ApplyFilterSettings(mouseFilter.Filter, filterSettings);
            }
        }
        
        private void ApplyFilterSettings(IInputFilter filter, Dictionary<string, object> settings)
        {
            if (filter == null || settings == null || settings.Count == 0)
                return;
                
            // Use the FilterSerializer to apply settings
            FilterSerializer.ApplySettings(filter, settings);
        }
    }
}
