using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class AccessibilityServiceBridge
    {
        public enum NavigationDirection
        {
            Up,
            Down,
            Left,
            Right,
            Back,
            Home,
            Recents
        }

        private Dictionary<NavigationDirection, int> NavigationValues { get; } = new Dictionary<NavigationDirection, int>();

        private readonly JavaBridge _java;

        public AccessibilityServiceBridge(JavaBridge java)
        {
            _java = java;
            var success = _java.Call<bool>("initializeLocalControl");

            if (!success)
            {
                LoggingManager.Instance.AddLogError($"AccessibilityServiceBridge: Initialization attempt failed");
                return;
            }

            Debug.Log("AccessibilityServiceBridge: Initialized successfully");

            try
            {
                bool serviceEnabled = IsAccessibilityServiceEnabled();
                if (!serviceEnabled) Debug.LogWarning("BleHidLocalControl: Accessibility service not enabled. Please enable it in settings.");

                NavigationValues.Add(NavigationDirection.Up, _java.Call<int>("getNavUp"));
                NavigationValues.Add(NavigationDirection.Left, _java.Call<int>("getNavLeft"));
                NavigationValues.Add(NavigationDirection.Right, _java.Call<int>("getNavRight"));
                NavigationValues.Add(NavigationDirection.Down, _java.Call<int>("getNavDown"));
                NavigationValues.Add(NavigationDirection.Back, _java.Call<int>("getNavBack"));
                NavigationValues.Add(NavigationDirection.Home, _java.Call<int>("getNavHome"));
                NavigationValues.Add(NavigationDirection.Recents, _java.Call<int>("getNavRecents"));
            }
            catch (Exception ex)
            {
                // Don't fail initialization if checking accessibility fails
                Debug.LogWarning("BleHidLocalControl: Unable to check accessibility status: " + ex.Message);
            }
        }

        public void OpenAccessibilitySettings() => _java.Call("openAccessibilitySettings");

        public bool IsAccessibilityServiceEnabled()
        {
            if (Application.isEditor) return true;
            return _java.Call<bool>("isAccessibilityServiceEnabled");
        }

        public bool PlayPause() => _java.Call<bool>("localPlayPause");
        public bool NextTrack() => _java.Call<bool>("localNextTrack");
        public bool PreviousTrack() => _java.Call<bool>("localPreviousTrack");
        public bool VolumeUp() => _java.Call<bool>("localVolumeUp");
        public bool VolumeDown() => _java.Call<bool>("localVolumeDown");
        public bool Mute() => _java.Call<bool>("localMute");
        public bool Tap(int x, int y) => _java.Call<bool>("localTap", x, y);
        public bool Swipe(int x1, int y1, int x2, int y2) => _java.Call<bool>("localSwipe", x1, y1, x2, y2);

        public bool Navigate(NavigationDirection direction)
        {
            int dirValue = NavigationValues[direction];
            return _java.Call<bool>("localNavigate", dirValue);
        }

        public bool LaunchCameraApp() => _java.Call<bool>("launchCameraApp");
        public bool LaunchVideoCapture() => _java.Call<bool>("launchVideoCapture");

        /// <summary>
        /// Take a picture with the camera using specified options.
        /// </summary>
        /// <param name="options">Options to configure the camera capture (null for defaults)</param>
        /// <returns>Coroutine that completes when the picture is taken</returns>
        public IEnumerator TakePicture(CameraOptions options = null)
        {
            options ??= new CameraOptions();
            float waitTime = 3.0f; // Default wait time

            // Call the Java bridge with options
            if (_java.Call<bool>("takePicture", options.ToAndroidObject()))
            {
                if (options.TapDelay > 0) waitTime = options.TapDelay / 1000f + 2f;
                if (options.ReturnDelay > 0) waitTime += options.ReturnDelay / 1000f;
            }

            yield return new WaitForSeconds(waitTime);
        }

        public IEnumerator RecordVideo(VideoOptions options = null)
        {
            options ??= new VideoOptions();
            float waitTime = options.Duration + 2.0f; // Base wait time on video duration

            // Call the Java bridge with options
            if (_java.Call<bool>("recordVideo", options.ToAndroidObject()))
            {
                if (options.TapDelay > 0) waitTime = options.TapDelay / 1000f + options.Duration + 2f;
                if (options.ReturnDelay > 0) waitTime += options.ReturnDelay / 1000f;
            }

            yield return new WaitForSeconds(waitTime);
        }
    }
}
