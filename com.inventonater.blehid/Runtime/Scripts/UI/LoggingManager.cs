using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages logging functionality for the BLE HID system
    /// </summary>
    public class LoggingManager
    {
        public static LoggingManager Instance { get; private set; } = new();
        private LoggingManager() { }

        private List<string> logMessages = new List<string>();
        private Vector2 scrollPosition;
        
        /// <summary>
        /// Add a log entry with timestamp
        /// </summary>
        public void AddLogEntry(string entry)
        {
            string timestamp = System.DateTime.Now.ToString("HH:mm:ss");
            string logEntry = timestamp + " - " + entry;

            // Add to list
            logMessages.Add(logEntry);

            // Keep the log size reasonable
            if (logMessages.Count > 100)
            {
                logMessages.RemoveAt(0);
            }

            // Auto-scroll to bottom
            scrollPosition = new Vector2(0, float.MaxValue);

            // Also log to Unity console
            Debug.Log(logEntry);
        }
        
        /// <summary>
        /// Draw the log UI
        /// </summary>
        public void DrawLogUI()
        {
            GUILayout.Label("Log:");
            scrollPosition = GUILayout.BeginScrollView(scrollPosition, GUI.skin.box, GUILayout.Height(Screen.height * 0.2f));
            foreach (string log in logMessages)
            {
                GUILayout.Label(log);
            }
            GUILayout.EndScrollView();
        }
        
        /// <summary>
        /// Get all log messages
        /// </summary>
        public List<string> GetLogMessages()
        {
            return new List<string>(logMessages);
        }
    }
}
