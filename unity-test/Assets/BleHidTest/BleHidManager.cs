using UnityEngine;
using System;
using System.Runtime.InteropServices;

/// <summary>
/// Unity C# wrapper for the Android BLE HID plugin.
/// Provides an interface for utilizing the BLE HID functionality in Unity.
/// </summary>
public class BleHidManager : MonoBehaviour
{
    // Events for plugin callbacks
    public event Action<string> OnDeviceConnected;
    public event Action<string> OnDeviceDisconnected;
    public event Action<string, int> OnPairingRequested;
    public event Action<string> OnPairingFailed;
    public event Action<bool> OnAdvertisingStateChanged;

    // Singleton instance
    private static BleHidManager _instance;
    public static BleHidManager Instance
    {
        get
        {
            if (_instance == null)
            {
                GameObject go = new GameObject("BleHidManager");
                _instance = go.AddComponent<BleHidManager>();
                DontDestroyOnLoad(go);
            }
            return _instance;
        }
    }

    // HID key codes (USB HID spec)
    public static class KeyCode
    {
        public const int KEY_A = 0x04;
        public const int KEY_B = 0x05;
        public const int KEY_C = 0x06;
        public const int KEY_D = 0x07;
        public const int KEY_E = 0x08;
        public const int KEY_F = 0x09;
        public const int KEY_G = 0x0A;
        public const int KEY_H = 0x0B;
        public const int KEY_I = 0x0C;
        public const int KEY_J = 0x0D;
        public const int KEY_K = 0x0E;
        public const int KEY_L = 0x0F;
        public const int KEY_M = 0x10;
        public const int KEY_N = 0x11;
        public const int KEY_O = 0x12;
        public const int KEY_P = 0x13;
        public const int KEY_Q = 0x14;
        public const int KEY_R = 0x15;
        public const int KEY_S = 0x16;
        public const int KEY_T = 0x17;
        public const int KEY_U = 0x18;
        public const int KEY_V = 0x19;
        public const int KEY_W = 0x1A;
        public const int KEY_X = 0x1B;
        public const int KEY_Y = 0x1C;
        public const int KEY_Z = 0x1D;
        public const int KEY_1 = 0x1E;
        public const int KEY_2 = 0x1F;
        public const int KEY_3 = 0x20;
        public const int KEY_4 = 0x21;
        public const int KEY_5 = 0x22;
        public const int KEY_6 = 0x23;
        public const int KEY_7 = 0x24;
        public const int KEY_8 = 0x25;
        public const int KEY_9 = 0x26;
        public const int KEY_0 = 0x27;
        public const int KEY_RETURN = 0x28;
        public const int KEY_ESCAPE = 0x29;
        public const int KEY_BACKSPACE = 0x2A;
        public const int KEY_TAB = 0x2B;
        public const int KEY_SPACE = 0x2C;
    }

    private AndroidJavaObject _pluginInstance;
    private AndroidJavaClass _pluginClass;
    private AndroidJavaObject _unityActivity;
    private bool _isInitialized = false;

    private void Awake()
    {
        if (_instance != null && _instance != this)
        {
            Destroy(gameObject);
            return;
        }

        _instance = this;
        DontDestroyOnLoad(gameObject);

#if UNITY_ANDROID && !UNITY_EDITOR
        InitializePlugin();
#else
        Debug.LogWarning("BLE HID functionality is only available on Android devices.");
#endif
    }

    /// <summary>
    /// Initializes the BLE HID plugin.
    /// </summary>
    private void InitializePlugin()
    {
        if (_isInitialized) return;

        try
        {
            Debug.Log("Initializing BLE HID Plugin...");

            // Get the Unity player activity
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            _unityActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");

            // Get the plugin class
            _pluginClass = new AndroidJavaClass("com.example.blehid.unity.BleHidPlugin");

            // Create a callback instance
            AndroidJavaClass callbackClass = new AndroidJavaClass("com.example.blehid.unity.UnityCallback");
            CallbackImpl callbackImpl = new CallbackImpl(this);
            AndroidJavaObject callbackInstance = callbackClass.CallStatic<AndroidJavaObject>("createInstance", callbackImpl);

            // Initialize the plugin
            bool result = _pluginClass.CallStatic<bool>("initialize", _unityActivity);
            if (!result)
            {
                Debug.LogError("Failed to initialize BLE HID Plugin");
                return;
            }

            // Set the callback
            _pluginClass.CallStatic("setCallback", callbackInstance);

            _isInitialized = true;
            Debug.Log("BLE HID Plugin initialized successfully");
        }
        catch (Exception e)
        {
            Debug.LogError("Error initializing BLE HID Plugin: " + e.Message);
        }
    }

    /// <summary>
    /// Checks if BLE peripheral mode is supported on this device.
    /// </summary>
    public bool IsBlePeripheralSupported()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("isBlePeripheralSupported");
#else
        return false;
#endif
    }

    /// <summary>
    /// Starts advertising the BLE HID device.
    /// </summary>
    public bool StartAdvertising()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("startAdvertising");
#else
        Debug.LogWarning("BLE HID advertising is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Stops advertising the BLE HID device.
    /// </summary>
    public void StopAdvertising()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return;
        _pluginClass.CallStatic("stopAdvertising");
#else
        Debug.LogWarning("BLE HID advertising is only available on Android devices.");
#endif
    }

    /// <summary>
    /// Checks if the device is connected to a host.
    /// </summary>
    public bool IsConnected()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("isConnected");
#else
        return false;
#endif
    }

    /// <summary>
    /// Gets the address of the connected device.
    /// </summary>
    public string GetConnectedDeviceAddress()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return null;
        return _pluginClass.CallStatic<string>("getConnectedDeviceAddress");
#else
        return null;
#endif
    }

    /// <summary>
    /// Sends a keyboard HID report with the specified key.
    /// </summary>
    public bool SendKey(int keyCode)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("sendKey", keyCode);
#else
        Debug.LogWarning("BLE HID key sending is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Sends multiple keyboard HID keys simultaneously.
    /// </summary>
    public bool SendKeys(int[] keyCodes)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("sendKeys", keyCodes);
#else
        Debug.LogWarning("BLE HID key sending is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Releases all pressed keys.
    /// </summary>
    public bool ReleaseKeys()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("releaseKeys");
#else
        Debug.LogWarning("BLE HID key releasing is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Sends a key press and then releases it after a short delay.
    /// </summary>
    public bool SendKeyPress(int keyCode)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        
        bool result = _pluginClass.CallStatic<bool>("sendKey", keyCode);
        if (result)
        {
            StartCoroutine(ReleaseKeysAfterDelay(0.1f));
        }
        return result;
#else
        Debug.LogWarning("BLE HID key pressing is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Coroutine to release keys after a delay.
    /// </summary>
    private System.Collections.IEnumerator ReleaseKeysAfterDelay(float delay)
    {
        yield return new WaitForSeconds(delay);
        ReleaseKeys();
    }

    private void OnDestroy()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (_isInitialized)
        {
            _pluginClass.CallStatic("close");
            _isInitialized = false;
        }
#endif
    }

    /// <summary>
    /// Implementation of the UnityCallback interface to receive events from the plugin.
    /// </summary>
    private class CallbackImpl : AndroidJavaProxy
    {
        private readonly BleHidManager _manager;

        public CallbackImpl(BleHidManager manager) : base("com.example.blehid.unity.UnityCallback")
        {
            _manager = manager;
        }

        public void onPairingRequested(string deviceAddress, int variant)
        {
            Debug.Log($"Pairing requested by {deviceAddress}, variant: {variant}");
            _manager.OnPairingRequested?.Invoke(deviceAddress, variant);
        }

        public void onDeviceConnected(string deviceAddress)
        {
            Debug.Log($"Device connected: {deviceAddress}");
            _manager.OnDeviceConnected?.Invoke(deviceAddress);
        }

        public void onDeviceDisconnected(string deviceAddress)
        {
            Debug.Log($"Device disconnected: {deviceAddress}");
            _manager.OnDeviceDisconnected?.Invoke(deviceAddress);
        }

        public void onPairingFailed(string deviceAddress)
        {
            Debug.Log($"Pairing failed with {deviceAddress}");
            _manager.OnPairingFailed?.Invoke(deviceAddress);
        }

        public void onAdvertisingStateChanged(bool isAdvertising)
        {
            Debug.Log($"Advertising state changed: {(isAdvertising ? "started" : "stopped")}");
            _manager.OnAdvertisingStateChanged?.Invoke(isAdvertising);
        }
    }
}
