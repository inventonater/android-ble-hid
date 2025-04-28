using System;
using System.Collections.Generic;
using System.IO;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class LoggingManager
    {
        public static LoggingManager Instance { get; private set; } = new();

        private List<string> logMessages = new List<string>();
        private Vector2 scrollPosition;
        private readonly string logFileName = "ble_hid_events.log";
        private string logFilePath;
        private bool logToFile = false;

        private LoggingManager()
        {
            // Initialize file logging
            logFilePath = Path.Combine(Application.persistentDataPath, logFileName);
            Debug.Log($"LoggingManager initialized. Log file path: {logFilePath}");

            // Create the log file or clear it if it exists
            try { File.WriteAllText(logFilePath, $"[{DateTime.Now}] BLE HID Logging started\n"); }
            catch (Exception ex)
            {
                Debug.LogError($"Failed to initialize log file: {ex.Message}");
                logToFile = false;
            }
        }

        public void Exception(Exception exception) => Error(exception.Message);
        public void Error(string message) => Log(message, true);

        /// <summary>
        /// Add a log entry with timestamp
        /// </summary>
        public void Log(string entry, bool isError = false)
        {
            if (string.IsNullOrEmpty(entry)) return;

            string timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");
            string logEntry = timestamp + " - " + entry;

            // Add to list
            logMessages.Add(logEntry);

            // Keep the log size reasonable
            if (logMessages.Count > 100) { logMessages.RemoveAt(0); }

            // Auto-scroll to bottom
            scrollPosition = new Vector2(0, float.MaxValue);

            // Also log to Unity console
            if (isError) Debug.LogError(logEntry);
            else Debug.Log(logEntry);

            // Log to file if enabled
            if (logToFile)
            {
                try { File.AppendAllText(logFilePath, logEntry + "\n"); }
                catch (Exception ex)
                {
                    Debug.LogError($"Failed to write to log file: {ex.Message}");
                    logToFile = false; // Disable file logging if it fails
                }
            }
        }

        /// <summary>
        /// Draw the log UI
        /// </summary>
        public void DrawLogUI()
        {
            GUILayout.Label("Log:");
            scrollPosition = GUILayout.BeginScrollView(scrollPosition, GUI.skin.box, GUILayout.Height(Screen.height * 0.2f));
            foreach (string log in logMessages) { GUILayout.Label(log); }

            GUILayout.EndScrollView();
        }

        /// <summary>
        /// Get all log messages
        /// </summary>
        public List<string> GetLogMessages()
        {
            return new List<string>(logMessages);
        }

        /// <summary>
        /// Read log entries directly from the log file
        /// </summary>
        /// <param name="maxLines">Maximum number of lines to read from the end of the file</param>
        /// <returns>String with the log entries from the file</returns>
        public string ReadLogFile(int maxLines = 100)
        {
            try
            {
                if (!File.Exists(logFilePath)) return "No log file entries found.";

                string[] allLines = File.ReadAllLines(logFilePath);
                int linesToRead = Math.Min(maxLines, allLines.Length);

                if (linesToRead > 0)
                {
                    string[] selectedLines = new string[linesToRead];
                    Array.Copy(allLines, allLines.Length - linesToRead, selectedLines, 0, linesToRead);
                    return string.Join("\n", selectedLines);
                }

                return "No log file entries found.";
            }
            catch (Exception ex) { return $"Error reading log file: {ex.Message}"; }
        }
    }
}
