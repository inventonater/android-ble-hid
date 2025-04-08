using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid.UI
{
    /// <summary>
    /// Centralized logging system for BLE HID components.
    /// </summary>
    public class BleHidLogger
    {
        private static BleHidLogger instance;
        private readonly List<string> logMessages = new List<string>();
        private int maxMessages = 100;
        
        /// <summary>
        /// Singleton instance.
        /// </summary>
        public static BleHidLogger Instance
        {
            get
            {
                if (instance == null)
                {
                    instance = new BleHidLogger();
                }
                return instance;
            }
        }
        
        /// <summary>
        /// Get or set the maximum number of log messages to keep.
        /// </summary>
        public int MaxMessages
        {
            get => maxMessages;
            set => maxMessages = Mathf.Max(1, value);
        }
        
        /// <summary>
        /// Get all log messages.
        /// </summary>
        public List<string> LogMessages => new List<string>(logMessages);
        
        /// <summary>
        /// Get the scroll position for logs UI.
        /// </summary>
        public Vector2 ScrollPosition { get; set; }
        
        /// <summary>
        /// An event that fires when a log message is added.
        /// </summary>
        public event Action<string> OnLogMessageAdded;
        
        private BleHidLogger() 
        {
            // Private constructor for singleton
        }
        
        /// <summary>
        /// Add a log message.
        /// </summary>
        /// <param name="message">The message to log.</param>
        public void Log(string message)
        {
            string timestamp = DateTime.Now.ToString("HH:mm:ss");
            string logEntry = $"{timestamp} - {message}";
            
            logMessages.Add(logEntry);
            
            // Keep the log size reasonable
            while (logMessages.Count > maxMessages)
            {
                logMessages.RemoveAt(0);
            }
            
            // Auto-scroll to bottom
            ScrollPosition = new Vector2(0, float.MaxValue);
            
            // Also log to Unity console
            Debug.Log(logEntry);
            
            // Trigger the event
            OnLogMessageAdded?.Invoke(logEntry);
        }
        
        /// <summary>
        /// Log an error message.
        /// </summary>
        /// <param name="message">The error message.</param>
        public void LogError(string message)
        {
            Log($"ERROR: {message}");
            Debug.LogError(message);
        }
        
        /// <summary>
        /// Log a warning message.
        /// </summary>
        /// <param name="message">The warning message.</param>
        public void LogWarning(string message)
        {
            Log($"WARNING: {message}");
            Debug.LogWarning(message);
        }
        
        /// <summary>
        /// Clear all log messages.
        /// </summary>
        public void Clear()
        {
            logMessages.Clear();
            ScrollPosition = Vector2.zero;
        }
        
        /// <summary>
        /// Draw a log view using OnGUI.
        /// </summary>
        /// <param name="rect">The rect to draw in.</param>
        public void DrawLogView(Rect rect)
        {
            GUILayout.BeginArea(rect);
            
            GUILayout.Label("Log:");
            ScrollPosition = GUILayout.BeginScrollView(ScrollPosition, GUI.skin.box, GUILayout.Height(rect.height - 25));
            
            foreach (string log in logMessages)
            {
                GUILayout.Label(log);
            }
            
            GUILayout.EndScrollView();
            GUILayout.EndArea();
        }
    }
}
