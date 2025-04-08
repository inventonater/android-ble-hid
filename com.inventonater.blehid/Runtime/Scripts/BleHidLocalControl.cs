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
        /// Checks if accessibility service is enabled.
        /// </summary>
        public bool IsAccessibilityServiceEnabled()
        {
            if (!initialized || bridgeInstance == null)
            {
                Debug.LogError("BleHidLocalControl: Not initialized");
                return false;
            }

            try
            {
                return bridgeInstance.Call<bool>("isAccessibilityServiceEnabled");
            }
            catch (Exception e)
            {
                Debug.LogError("BleHidLocalControl: Error checking accessibility service: " + e.Message);
                return false;
            }
        }

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
