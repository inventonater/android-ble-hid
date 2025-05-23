using System;
using System.Collections.Generic;
using System.IO;
using UnityEngine;

namespace Inventonater
{
    public class LoggingManager
    {
        public static LoggingManager Instance { get; private set; } = new();

        private List<string> logMessages = new List<string>();
        private Vector2 scrollPosition;

        public void Exception(Exception exception)
        {
            Error(exception.Message + "\n" + exception.StackTrace);
            if(exception.InnerException != null) Error(exception.InnerException.Message + "\n" + exception.InnerException.StackTrace);
        }

        public void Warning(string message) => Log(message, false);
        public void Error(string message) => Log(message, true);

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
        }

        public void DrawLogUI()
        {
            GUILayout.Label("Log:");
            scrollPosition = GUILayout.BeginScrollView(scrollPosition, GUI.skin.box, GUILayout.Height(Screen.height * 0.2f));
            foreach (string log in logMessages) { GUILayout.Label(log); }

            GUILayout.EndScrollView();
        }
    }
}
