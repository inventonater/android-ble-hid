using UnityEngine;
using System.Collections.Generic;

namespace Inventonater.BleHid.UI.Filters
{
    /// <summary>
    /// Factory for creating and managing input filter instances
    /// </summary>
    public static class InputFilterFactory
    {
        /// <summary>
        /// Enumeration of available filter types
        /// </summary>
        public enum FilterType
        {
            None,               // No filtering
            OneEuro,            // 1€ filter (adaptive) 
            ExponentialMA,      // Exponential Moving Average
            Kalman,             // Kalman filter with velocity model
            DoubleExponential,  // Double exponential smoothing (Holt)
            Predictive          // Predictive latency compensation
        }
        
        /// <summary>
        /// Get a descriptive name for a filter type
        /// </summary>
        public static string GetFilterName(FilterType type)
        {
            switch (type)
            {
                case FilterType.None: 
                    return "No Filter";
                case FilterType.OneEuro: 
                    return "1€ Filter";
                case FilterType.ExponentialMA: 
                    return "EMA Filter";
                case FilterType.Kalman:
                    return "Kalman Filter";
                case FilterType.DoubleExponential:
                    return "Double Exp";
                case FilterType.Predictive:
                    return "Predictive";
                default: 
                    return "Unknown Filter";
            }
        }
        
        /// <summary>
        /// Get a list of all available filter types for UI selection
        /// </summary>
        public static List<FilterType> GetAvailableFilterTypes()
        {
            return new List<FilterType>
            {
                FilterType.None,
                FilterType.OneEuro, 
                FilterType.ExponentialMA,
                FilterType.Kalman,
                FilterType.DoubleExponential,
                FilterType.Predictive
            };
        }
        
        /// <summary>
        /// Create a new input filter instance of the specified type
        /// </summary>
        /// <param name="type">Type of filter to create</param>
        /// <param name="param1">First parameter value</param>
        /// <param name="param2">Second parameter value</param>
        /// <returns>New filter instance</returns>
        public static IInputFilter CreateFilter(FilterType type, float param1 = 1.0f, float param2 = 0.007f)
        {
            switch (type)
            {
                case FilterType.None:
                    return new NoFilter();
                    
                case FilterType.OneEuro:
                    return new OneEuroFilter(param1, param2);
                    
                case FilterType.ExponentialMA:
                    return new ExponentialMovingAverageFilter(param1, param2);
                    
                case FilterType.Kalman:
                    return new KalmanFilter(param1, param2);
                    
                case FilterType.DoubleExponential:
                    return new DoubleExponentialFilter(param1, param2);
                    
                case FilterType.Predictive:
                    return new PredictiveFilter(param1, param2);
                    
                default:
                    Debug.LogWarning($"Unknown filter type: {type}, defaulting to OneEuro");
                    return new OneEuroFilter();
            }
        }
        
        /// <summary>
        /// Get parameter display info for a specific filter type
        /// </summary>
        /// <param name="type">Filter type</param>
        /// <param name="param1Name">Name of first parameter</param>
        /// <param name="param1Min">Minimum value for first parameter</param>
        /// <param name="param1Max">Maximum value for first parameter</param>
        /// <param name="param1Default">Default value for first parameter</param>
        /// <param name="param2Name">Name of second parameter</param>
        /// <param name="param2Min">Minimum value for second parameter</param>
        /// <param name="param2Max">Maximum value for second parameter</param>
        /// <param name="param2Default">Default value for second parameter</param>
        public static void GetParameterInfo(
            FilterType type,
            out string param1Name, out float param1Min, out float param1Max, out float param1Default,
            out string param2Name, out float param2Min, out float param2Max, out float param2Default)
        {
            switch (type)
            {
                case FilterType.None:
                    param1Name = "N/A";
                    param1Min = 0;
                    param1Max = 0;
                    param1Default = 0;
                    param2Name = "N/A";
                    param2Min = 0;
                    param2Max = 0;
                    param2Default = 0;
                    break;
                
                case FilterType.OneEuro:
                    param1Name = "Smoothing";
                    param1Min = 0.1f;
                    param1Max = 5.0f;
                    param1Default = 1.0f;
                    param2Name = "Response";
                    param2Min = 0.001f;
                    param2Max = 0.1f;
                    param2Default = 0.007f;
                    break;
                
                case FilterType.ExponentialMA:
                    param1Name = "Smoothing";
                    param1Min = 0.05f;
                    param1Max = 1.0f;
                    param1Default = 0.5f;
                    param2Name = "Min Change";
                    param2Min = 0.0001f;
                    param2Max = 0.01f;
                    param2Default = 0.0001f;
                    break;
                
                case FilterType.Kalman:
                    param1Name = "Process Noise";
                    param1Min = 0.0001f;
                    param1Max = 0.01f;
                    param1Default = 0.001f;
                    param2Name = "Measurement Noise";
                    param2Min = 0.01f;
                    param2Max = 1.0f;
                    param2Default = 0.1f;
                    break;
                    
                case FilterType.DoubleExponential:
                    param1Name = "Level Smoothing";
                    param1Min = 0.1f;
                    param1Max = 0.9f;
                    param1Default = 0.5f;
                    param2Name = "Trend Smoothing";
                    param2Min = 0.01f;
                    param2Max = 0.5f;
                    param2Default = 0.1f;
                    break;
                    
                case FilterType.Predictive:
                    param1Name = "Prediction Time";
                    param1Min = 0.01f;
                    param1Max = 0.2f;
                    param1Default = 0.05f;
                    param2Name = "Smoothing";
                    param2Min = 0.1f;
                    param2Max = 0.9f;
                    param2Default = 0.5f;
                    break;
                
                default:
                    param1Name = "Unknown";
                    param1Min = 0;
                    param1Max = 1;
                    param1Default = 0.5f;
                    param2Name = "Unknown";
                    param2Min = 0;
                    param2Max = 1;
                    param2Default = 0.5f;
                    break;
            }
        }
    }
}
