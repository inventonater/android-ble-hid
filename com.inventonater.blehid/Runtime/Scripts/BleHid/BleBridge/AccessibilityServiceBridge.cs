using System;
using System.Collections;
using System.Collections.Generic;
using Cysharp.Threading.Tasks;
using UnityEngine;

namespace Inventonater.BleHid
{
    [Serializable]
    public class AccessibilityServiceBridge
    {
        private readonly JavaBridge _java;
        public AccessibilityServiceBridge(JavaBridge java) => _java = java;

        private bool _isInitialized;
        public bool IsInitialized => _isInitialized;

        public async UniTask<bool> Initialize()
        {
            while (!_isInitialized)
            {
                await UniTask.Delay(500);
                _isInitialized = CheckAccessibilityService(direct: true);
            }

            var success = Application.isEditor || _java.Call<bool>("initializeLocalControl");
            if (success) LoggingManager.Instance.Log("AccessibilityServiceBridge: Initialized successfully");
            else LoggingManager.Instance.Error($"AccessibilityServiceBridge: Initialization attempt failed");

            return success;
        }

        private bool CheckAccessibilityService(bool direct = false)
        {
            if (Application.isEditor) return true;

            if (!direct) return _java.Call<bool>("isAccessibilityServiceEnabled");
            try
            {
                AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");
                AndroidJavaObject context = currentActivity.Call<AndroidJavaObject>("getApplicationContext");
                AndroidJavaClass settingsSecure = new AndroidJavaClass("android.provider.Settings$Secure");
                AndroidJavaObject contentResolver = context.Call<AndroidJavaObject>("getContentResolver");
                string enabledServices = settingsSecure.CallStatic<string>("getString", contentResolver, "enabled_accessibility_services");
                string packageName = context.Call<string>("getPackageName");
                string serviceName = packageName + "/com.inventonater.blehid.core.LocalAccessibilityService";
                bool isEnabled = enabledServices != null && enabledServices.Contains(serviceName);
                LoggingManager.Instance.Log($"BleHidLocalControl: Direct accessibility check - Service {(isEnabled ? "IS" : "is NOT")} enabled");
                return isEnabled;
            }
            catch (Exception e)
            {
                LoggingManager.Instance.Error("BleHidLocalControl: Error checking accessibility service status: " + e.Message);
                return false;
            }
        }


        public void OpenAccessibilitySettings(bool direct = false)
        {
            if (Application.isEditor) return;

            if (!direct)
            {
                _java.Call("openAccessibilitySettings");
                return;
            }

            try
            {
                AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
                AndroidJavaObject currentActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");

                AndroidJavaClass intentClass = new AndroidJavaClass("android.content.Intent");
                AndroidJavaObject intent = new AndroidJavaObject("android.content.Intent", "android.settings.ACCESSIBILITY_SETTINGS");

                intent.Call<AndroidJavaObject>("addFlags", intentClass.GetStatic<int>("FLAG_ACTIVITY_NEW_TASK"));

                currentActivity.Call("startActivity", intent);
                Debug.Log("BleHidLocalControl: Opened accessibility settings via direct intent");
            }
            catch (Exception e)
            {
                Debug.LogError("BleHidLocalControl: Failed to open accessibility settings: " + e.Message);
                throw; // Rethrow to allow the caller to handle it
            }
        }

        public bool PlayPause() => _java.Call<bool>("localPlayPause");
        public bool NextTrack() => _java.Call<bool>("localNextTrack");
        public bool PreviousTrack() => _java.Call<bool>("localPreviousTrack");
        public bool VolumeUp() => _java.Call<bool>("localVolumeUp");
        public bool VolumeDown() => _java.Call<bool>("localVolumeDown");
        public bool Mute() => _java.Call<bool>("localMute");
        public bool Tap(int x, int y) => _java.Call<bool>("localTap", x, y);

        public bool Swipe(Vector2 begin, Vector2 end)
        {
            Vector2Int beginInt = new Vector2Int(Mathf.RoundToInt(begin.x), Mathf.RoundToInt(begin.y));
            Vector2Int endInt = new Vector2Int(Mathf.RoundToInt(end.x), Mathf.RoundToInt(end.y));
            return Swipe(beginInt, endInt);
        }

        public bool Swipe(Vector2Int begin, Vector2Int end) => _java.Call<bool>("localSwipe", begin.x, begin.y, end.x, end.y);

        public bool DPadUp() => PerformGlobalAction(GlobalAction.Up);
        public bool DPadRight() => PerformGlobalAction(GlobalAction.Right);
        public bool DPadDown() => PerformGlobalAction(GlobalAction.Down);
        public bool DPadLeft() => PerformGlobalAction(GlobalAction.Left);
        public bool Back() => PerformGlobalAction(GlobalAction.Back);
        public bool Home() => PerformGlobalAction(GlobalAction.Home);
        public bool Recents() => PerformGlobalAction(GlobalAction.Recents);

        enum GlobalAction
        {
            Back = 1,
            Home = 2,
            Recents = 3,
            Up = 16,
            Down = 17,
            Left = 18,
            Right = 19
        }

        private bool PerformGlobalAction(GlobalAction action) => _java.Call<bool>("performGlobalAction", action);


        public bool LaunchCameraApp() => _java.Call<bool>("launchCameraApp");
        public bool LaunchVideoCapture() => _java.Call<bool>("launchVideoCapture");

        /// <summary>
        /// Performs the specified action on the currently focused accessibility node.
        /// </summary>
        /// <param name="action">The accessibility action to perform</param>
        /// <returns>True if the action was performed successfully, false otherwise</returns>
        public bool PerformFocusedNodeAction(AccessibilityAction action) => _java.Call<bool>("localPerformFocusedNodeAction", (int)action);

        /// <summary>
        /// Clicks on the currently focused accessibility node.
        /// </summary>
        /// <returns>True if the click was performed successfully, false otherwise</returns>
        public bool ClickFocusedNode() => PerformFocusedNodeAction(AccessibilityAction.Click);

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
