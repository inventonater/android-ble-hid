using System;
using System.Collections;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Provides control of the local Android device using Accessibility and Media Session APIs.
    /// </summary>
    public class BleHidLocalControl : MonoBehaviour
    {
        private static BleHidLocalControl instance;
        private AndroidJavaObject bridgeInstance;
        private bool initialized = false;

        // Events
        public delegate void AccessibilityStatusChangedHandler(bool enabled);
        public event AccessibilityStatusChangedHandler OnAccessibilityStatusChanged;

        /// <summary>
        /// Singleton instance.
        /// </summary>
        public static BleHidLocalControl Instance
        {
            get
            {
                if (instance == null)
                {
                    GameObject obj = new GameObject("BleHidLocalControl");
                    instance = obj.AddComponent<BleHidLocalControl>();
                    DontDestroyOnLoad(obj);
                }
                return instance;
            }
        }

        private void Awake()
        {
            if (instance != null && instance != this)
            {
                Destroy(gameObject);
                return;
            }

            instance = this;
            DontDestroyOnLoad(gameObject);
        }

        /// <summary>
        /// Initialize local control functionality.
        /// </summary>
        /// <param name="maxRetries">Maximum number of retries for initialization</param>
        /// <returns>A coroutine that can be used with StartCoroutine.</returns>
        public IEnumerator Initialize(int maxRetries = 5)
        {
            if (initialized)
            {
                Debug.LogWarning("BleHidLocalControl: Already initialized");
                yield break;
            }

            if (Application.platform != RuntimePlatform.Android)
            {
                Debug.LogWarning("BleHidLocalControl: Only supported on Android");
                yield break;
            }

            // Wait a bit to ensure Unity is fully initialized
            yield return new WaitForSeconds(1.0f);
            
            int retryCount = 0;
            bool success = false;
            
            while (!success && retryCount < maxRetries)
            {
                try
                {
                    // Get bridge instance
                    if (bridgeInstance == null)
                    {
                        AndroidJavaClass bridgeClass = new AndroidJavaClass("com.inventonater.blehid.unity.BleHidUnityBridge");
                        bridgeInstance = bridgeClass.CallStatic<AndroidJavaObject>("getInstance");
                    }
                    
                    // Initialize local control
                    success = bridgeInstance.Call<bool>("initializeLocalControl");
                    
                    if (success)
                    {
                        initialized = true;
                        Debug.Log("BleHidLocalControl: Initialized successfully");
                        
                        try
                        {
                            // Check if accessibility service is enabled
                            bool serviceEnabled = IsAccessibilityServiceEnabled();
                            if (!serviceEnabled)
                            {
                                Debug.LogWarning("BleHidLocalControl: Accessibility service not enabled. Please enable it in settings.");
                            }
                        }
                        catch (Exception ex)
                        {
                            // Don't fail initialization if checking accessibility fails
                            Debug.LogWarning("BleHidLocalControl: Unable to check accessibility status: " + ex.Message);
                        }
                        
                        break;
                    }
                    else
                    {
                        Debug.LogWarning($"BleHidLocalControl: Initialization attempt {retryCount+1} failed");
                    }
                }
                catch (Exception e)
                {
                    Debug.LogWarning($"BleHidLocalControl: Initialization attempt {retryCount+1} failed: {e.Message}");
                }
                
                retryCount++;
                
                // Wait before trying again
                yield return new WaitForSeconds(0.5f);
            }
            
            if (!success)
            {
                Debug.LogError("BleHidLocalControl: Failed to initialize after multiple attempts");
            }
        }
        
        /// <summary>
        /// Checks if the accessibility service is enabled.
        /// </summary>
        /// <returns>True if the service is enabled, false otherwise.</returns>
        public bool IsAccessibilityServiceEnabled()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("isAccessibilityServiceEnabled");
        }

        /// <summary>
    /// Take a picture with the camera using default options.
    /// </summary>
    /// <returns>Coroutine that completes when the picture is taken</returns>
    public IEnumerator TakePicture()
    {
        return TakePicture(null);
    }
    
    /// <summary>
    /// Take a picture with the camera using specified options.
    /// </summary>
    /// <param name="options">Options to configure the camera capture (null for defaults)</param>
    /// <returns>Coroutine that completes when the picture is taken</returns>
    public IEnumerator TakePicture(CameraOptions options)
    {
        if (!CheckInitialized()) yield break;
        
        // Use default options if null
        options = options ?? new CameraOptions();
        
        bool result;
        float waitTime = 3.0f; // Default wait time
        
        // Call the Java bridge with options
        if (bridgeInstance.Call<bool>("takePicture", options.ToAndroidObject()))
        {
            result = true;
            
            // Calculate appropriate wait time based on options
            if (options.TapDelay > 0)
            {
                waitTime = options.TapDelay / 1000f + 2f;
            }
            
            if (options.ReturnDelay > 0)
            {
                waitTime += options.ReturnDelay / 1000f;
            }
        }
        else
        {
            result = false;
        }
        
        Debug.Log($"Taking picture: {(result ? "Success" : "Failed")}");
        
        // Wait for background service to finish its work
        yield return new WaitForSeconds(waitTime);
    }
    
    /// <summary>
    /// Record a video with default settings (5 seconds).
    /// </summary>
    /// <returns>Coroutine that completes when the recording is finished</returns>
    public IEnumerator RecordVideo()
    {
        return RecordVideo(null);
    }
    
    /// <summary>
    /// Record a video with specified duration using default settings.
    /// </summary>
    /// <param name="durationSeconds">Duration in seconds to record</param>
    /// <returns>Coroutine that completes when the recording is finished</returns>
    public IEnumerator RecordVideo(float durationSeconds)
    {
        return RecordVideo(new VideoOptions(durationSeconds));
    }
    
    /// <summary>
    /// Record a video with fully customizable options.
    /// </summary>
    /// <param name="options">Video options to configure the recording (null for defaults)</param>
    /// <returns>Coroutine that completes when the recording is finished</returns>
    public IEnumerator RecordVideo(VideoOptions options)
    {
        if (!CheckInitialized()) yield break;
        
        // Use default options if null
        options = options ?? new VideoOptions();
        
        bool result;
        float waitTime = options.Duration + 2.0f; // Base wait time on video duration
        
        // Call the Java bridge with options
        if (bridgeInstance.Call<bool>("recordVideo", options.ToAndroidObject()))
        {
            result = true;
            
            // Calculate appropriate wait time based on options
            if (options.TapDelay > 0)
            {
                waitTime = options.TapDelay / 1000f + options.Duration + 2f;
            }
            
            if (options.ReturnDelay > 0)
            {
                waitTime += options.ReturnDelay / 1000f;
            }
        }
        else
        {
            result = false;
        }
        
        Debug.Log($"Recording video for {options.Duration} seconds: {(result ? "Success" : "Failed")}");
        
        // Wait for the recording to complete
        yield return new WaitForSeconds(waitTime);
    }
    
    #region Legacy Methods (for backward compatibility)
    
    /// <summary>
    /// Take a picture by launching the camera app and automatically capturing the photo.
    /// </summary>
    /// <returns>Coroutine that completes when the picture is taken</returns>
    [System.Obsolete("Use TakePicture() instead")]
    public IEnumerator TakePictureWithCamera()
    {
        return TakePicture();
    }
    
    /// <summary>
    /// Take a picture with the camera using configurable parameters.
    /// </summary>
    /// <param name="tapDelay">Delay in milliseconds before tapping the shutter button</param>
    /// <param name="returnDelay">Delay in milliseconds before returning to the app</param>
    /// <param name="buttonX">X position of shutter button as a ratio (0.0-1.0)</param>
    /// <param name="buttonY">Y position of shutter button as a ratio (0.0-1.0)</param>
    /// <returns>Coroutine that completes when the picture is taken</returns>
    [System.Obsolete("Use TakePicture(CameraOptions) instead")]
    public IEnumerator TakePictureWithCamera(int tapDelay, int returnDelay, float buttonX, float buttonY)
    {
        var options = new CameraOptions
        {
            TapDelay = tapDelay,
            ReturnDelay = returnDelay,
            ButtonX = buttonX > 0 ? buttonX : 0.5f,
            ButtonY = buttonY > 0 ? buttonY : 0.8f
        };
        
        return TakePicture(options);
    }
    
    /// <summary>
    /// Take a picture with the camera using fully configurable parameters including dialog handling.
    /// </summary>
    /// <param name="tapDelay">Delay in milliseconds before tapping the shutter button</param>
    /// <param name="returnDelay">Delay in milliseconds before returning to the app</param>
    /// <param name="buttonX">X position of shutter button as a ratio (0.0-1.0)</param>
    /// <param name="buttonY">Y position of shutter button as a ratio (0.0-1.0)</param>
    /// <param name="acceptDialogDelay">Delay before tapping the accept dialog button</param>
    /// <param name="acceptXOffset">X offset from center for accept button (0.0-1.0)</param>
    /// <param name="acceptYOffset">Y offset from center for accept button (0.0-1.0)</param>
    /// <returns>Coroutine that completes when the picture is taken</returns>
    [System.Obsolete("Use TakePicture(CameraOptions) instead")]
    public IEnumerator TakePictureWithCamera(int tapDelay, int returnDelay, float buttonX, float buttonY,
                                            int acceptDialogDelay, float acceptXOffset, float acceptYOffset)
    {
        var options = new CameraOptions
        {
            TapDelay = tapDelay,
            ReturnDelay = returnDelay,
            ButtonX = buttonX > 0 ? buttonX : 0.5f,
            ButtonY = buttonY > 0 ? buttonY : 0.8f,
            AcceptDialogDelay = acceptDialogDelay > 0 ? acceptDialogDelay : 300,
            AcceptXOffset = acceptXOffset > 0 ? acceptXOffset : 0.2f,
            AcceptYOffset = acceptYOffset > 0 ? acceptYOffset : 0.05f
        };
        
        return TakePicture(options);
    }
    
    /// <summary>
    /// Record a video with default duration.
    /// </summary>
    /// <param name="duration">Duration in seconds to record video</param>
    /// <returns>Coroutine that completes when the recording is finished</returns>
    [System.Obsolete("Use RecordVideo(float) or RecordVideo(VideoOptions) instead")]
    public IEnumerator RecordVideo(float duration = 5.0f)
    {
        return RecordVideo(new VideoOptions(duration));
    }
    
    /// <summary>
    /// Record a video with configurable parameters.
    /// </summary>
    /// <param name="duration">Duration in seconds to record video</param>
    /// <param name="tapDelay">Delay in milliseconds before tapping the record button</param>
    /// <param name="returnDelay">Delay in milliseconds before returning to the app</param>
    /// <param name="buttonX">X position of record button as a ratio (0.0-1.0)</param>
    /// <param name="buttonY">Y position of record button as a ratio (0.0-1.0)</param>
    /// <returns>Coroutine that completes when the recording is finished</returns>
    [System.Obsolete("Use RecordVideo(VideoOptions) instead")]
    public IEnumerator RecordVideo(float duration, int tapDelay, int returnDelay, float buttonX, float buttonY)
    {
        var options = new VideoOptions(duration)
        {
            TapDelay = tapDelay,
            ReturnDelay = returnDelay,
            ButtonX = buttonX > 0 ? buttonX : 0.5f,
            ButtonY = buttonY > 0 ? buttonY : 0.8f
        };
        
        return RecordVideo(options);
    }
    
    #endregion

        /// <summary>
        /// Opens accessibility settings to enable the service.
        /// </summary>
        public void OpenAccessibilitySettings()
        {
            if (!initialized || bridgeInstance == null)
            {
                Debug.LogError("BleHidLocalControl: Not initialized");
                return;
            }

            bridgeInstance.Call("openAccessibilitySettings");
        }

        #region Media Control Methods

        /// <summary>
        /// Sends a play/pause command to the local media player.
        /// </summary>
        public bool PlayPause()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("localPlayPause");
        }

        /// <summary>
        /// Sends a next track command to the local media player.
        /// </summary>
        public bool NextTrack()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("localNextTrack");
        }

        /// <summary>
        /// Sends a previous track command to the local media player.
        /// </summary>
        public bool PreviousTrack()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("localPreviousTrack");
        }

        /// <summary>
        /// Increases the local media volume.
        /// </summary>
        public bool VolumeUp()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("localVolumeUp");
        }

        /// <summary>
        /// Decreases the local media volume.
        /// </summary>
        public bool VolumeDown()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("localVolumeDown");
        }

        /// <summary>
        /// Toggles mute state for local media.
        /// </summary>
        public bool Mute()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("localMute");
        }

        #endregion

        #region Input Control Methods

        /// <summary>
        /// Performs a tap at the specified coordinates.
        /// </summary>
        public bool Tap(int x, int y)
        {
            if (!CheckInitialized()) return false;
            if (!CheckAccessibilityService()) return false;
            return bridgeInstance.Call<bool>("localTap", x, y);
        }

        /// <summary>
        /// Performs a swipe from (x1,y1) to (x2,y2).
        /// </summary>
        public bool Swipe(int x1, int y1, int x2, int y2)
        {
            if (!CheckInitialized()) return false;
            if (!CheckAccessibilityService()) return false;
            return bridgeInstance.Call<bool>("localSwipe", x1, y1, x2, y2);
        }

        /// <summary>
        /// Performs a directional navigation action.
        /// </summary>
        public bool Navigate(NavigationDirection direction)
        {
            if (!CheckInitialized()) return false;
            if (!CheckAccessibilityService()) return false;
            int dirValue = GetNavigationValue(direction);
            return bridgeInstance.Call<bool>("localNavigate", dirValue);
        }

        #endregion

        #region Navigation Constants

        /// <summary>
        /// Navigation direction constants.
        /// </summary>
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

        private int GetNavigationValue(NavigationDirection direction)
        {
            switch (direction)
            {
                case NavigationDirection.Up:
                    return bridgeInstance.Call<int>("getNavUp");
                case NavigationDirection.Down:
                    return bridgeInstance.Call<int>("getNavDown");
                case NavigationDirection.Left:
                    return bridgeInstance.Call<int>("getNavLeft");
                case NavigationDirection.Right:
                    return bridgeInstance.Call<int>("getNavRight");
                case NavigationDirection.Back:
                    return bridgeInstance.Call<int>("getNavBack");
                case NavigationDirection.Home:
                    return bridgeInstance.Call<int>("getNavHome");
                case NavigationDirection.Recents:
                    return bridgeInstance.Call<int>("getNavRecents");
                default:
                    return bridgeInstance.Call<int>("getNavUp");
            }
        }

        #endregion

        #region Camera Control Methods

        /// <summary>
        /// Launches the default camera app.
        /// </summary>
        public bool LaunchCameraApp()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("launchCameraApp");
        }

        /// <summary>
        /// Launches the camera in photo capture mode.
        /// </summary>
        public bool LaunchPhotoCapture()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("launchPhotoCapture");
        }

        /// <summary>
        /// Launches the camera in video capture mode.
        /// </summary>
        public bool LaunchVideoCapture()
        {
            if (!CheckInitialized()) return false;
            return bridgeInstance.Call<bool>("launchVideoCapture");
        }

        // Removed duplicate methods

        #endregion

        #region Helper Methods

        private bool CheckInitialized()
        {
            if (!initialized || bridgeInstance == null)
            {
                Debug.LogError("BleHidLocalControl: Not initialized");
                return false;
            }
            return true;
        }

        private bool CheckAccessibilityService()
        {
            if (!IsAccessibilityServiceEnabled())
            {
                Debug.LogWarning("BleHidLocalControl: Accessibility service not enabled. Please enable it in settings.");
                return false;
            }
            return true;
        }

        #endregion
    }
}
