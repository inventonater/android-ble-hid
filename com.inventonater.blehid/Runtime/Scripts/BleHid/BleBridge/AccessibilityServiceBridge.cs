using System;
using System.Collections;
using System.Collections.Generic;
using Cysharp.Threading.Tasks;
using UnityEngine;
using Inventonater.BleHid;

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

[MappableAction(
    id: "play_pause", 
    displayName: "Play/Pause", 
    category: "Media", 
    description: "Toggle media playback between play and pause states")]
public bool PlayPause() => _java.Call<bool>("localPlayPause");

[MappableAction(
    id: "next_track", 
    displayName: "Next Track", 
    category: "Media", 
    description: "Skip to the next track")]
public bool NextTrack() => _java.Call<bool>("localNextTrack");

[MappableAction(
    id: "previous_track", 
    displayName: "Previous Track", 
    category: "Media", 
    description: "Go back to the previous track")]
public bool PreviousTrack() => _java.Call<bool>("localPreviousTrack");

[MappableAction(
    id: "volume_up", 
    displayName: "Volume Up", 
    category: "Media", 
    description: "Increase the volume")]
public bool VolumeUp() => _java.Call<bool>("localVolumeUp");

[MappableAction(
    id: "volume_down", 
    displayName: "Volume Down", 
    category: "Media", 
    description: "Decrease the volume")]
public bool VolumeDown() => _java.Call<bool>("localVolumeDown");

[MappableAction(
    id: "mute", 
    displayName: "Mute", 
    category: "Media", 
    description: "Mute or unmute the audio")]
public bool Mute() => _java.Call<bool>("localMute");
        public bool Tap(int x, int y) => _java.Call<bool>("localTap", x, y);
        public bool SwipeBegin(Vector2 begin) => _java.Call<bool>("localSwipeBegin", begin.x, begin.y);
        public bool SwipeExtend(Vector2 delta) => _java.Call<bool>("localSwipeExtend", delta.x, delta.y);
        public bool SwipeEnd() => _java.Call<bool>("localSwipeEnd");

[MappableAction(
    id: "dpad_up", 
    displayName: "D-Pad Up", 
    category: "Navigation", 
    description: "Navigate up")]
public bool DPadUp() => PerformGlobalAction(GlobalAction.DPadUp);

[MappableAction(
    id: "dpad_right", 
    displayName: "D-Pad Right", 
    category: "Navigation", 
    description: "Navigate right")]
public bool DPadRight() => PerformGlobalAction(GlobalAction.DPadRight);

[MappableAction(
    id: "dpad_down", 
    displayName: "D-Pad Down", 
    category: "Navigation", 
    description: "Navigate down")]
public bool DPadDown() => PerformGlobalAction(GlobalAction.DPadDown);

[MappableAction(
    id: "dpad_left", 
    displayName: "D-Pad Left", 
    category: "Navigation", 
    description: "Navigate left")]
public bool DPadLeft() => PerformGlobalAction(GlobalAction.DPadLeft);

[MappableAction(
    id: "dpad_center", 
    displayName: "D-Pad Center", 
    category: "Navigation", 
    description: "Select the focused item")]
public bool DPadCenter() => PerformGlobalAction(GlobalAction.DPadCenter);

[MappableAction(
    id: "back", 
    displayName: "Back", 
    category: "System", 
    description: "Go back to the previous screen")]
public bool Back() => PerformGlobalAction(GlobalAction.Back);

[MappableAction(
    id: "home", 
    displayName: "Home", 
    category: "System", 
    description: "Go to the home screen")]
public bool Home() => PerformGlobalAction(GlobalAction.Home);

[MappableAction(
    id: "recents", 
    displayName: "Recents", 
    category: "System", 
    description: "Show recent apps")]
public bool Recents() => PerformGlobalAction(GlobalAction.Recents);

        enum GlobalAction
        {
            Back = 1,
            Home = 2,
            Recents = 3,
            DPadUp = 16,
            DPadDown = 17,
            DPadLeft = 18,
            DPadRight = 19,
            DPadCenter = 20
        }

        private bool PerformGlobalAction(GlobalAction action) => _java.Call<bool>("performGlobalAction", (int)action);

[MappableAction(
    id: "launch_camera", 
    displayName: "Launch Camera", 
    category: "Apps", 
    description: "Open the camera app")]
public bool LaunchCameraApp() => _java.Call<bool>("launchCameraApp");

[MappableAction(
    id: "launch_video", 
    displayName: "Launch Video", 
    category: "Apps", 
    description: "Open the video capture app")]
public bool LaunchVideoCapture() => _java.Call<bool>("launchVideoCapture");

        [MappableAction(
    id: "click_focused", 
    displayName: "Click Focused Element", 
    category: "Accessibility", 
    description: "Click on the currently focused element")]
public bool ClickFocusedNode() => PerformFocusedNodeAction(AccessibilityAction.Click);
        public bool PerformFocusedNodeAction(AccessibilityAction action) => _java.Call<bool>("localPerformFocusedNodeAction", (int)action);

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
