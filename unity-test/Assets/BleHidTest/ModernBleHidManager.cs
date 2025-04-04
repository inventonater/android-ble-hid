using UnityEngine;
using System;
using System.Runtime.InteropServices;
using System.Collections;

/// <summary>
/// Unity C# wrapper for the Android BLE HID plugin.
/// Uses the modern BluetoothHidDevice API implementation.
/// </summary>
public class ModernBleHidManager : MonoBehaviour
{
    // Events for plugin callbacks
    public event Action<string> OnDeviceConnected;
    public event Action<string> OnDeviceDisconnected;
    public event Action<string, int> OnPairingRequested;
    public event Action<string> OnPairingFailed;

    // Singleton instance
    private static ModernBleHidManager _instance;
    public static ModernBleHidManager Instance
    {
        get
        {
            if (_instance == null)
            {
                GameObject go = new GameObject("ModernBleHidManager");
                _instance = go.AddComponent<ModernBleHidManager>();
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
        public const int KEY_MINUS = 0x2D;
        public const int KEY_EQUAL = 0x2E;
        public const int KEY_LEFT_BRACKET = 0x2F;
        public const int KEY_RIGHT_BRACKET = 0x30;
        public const int KEY_BACKSLASH = 0x31;
        public const int KEY_SEMICOLON = 0x33;
        public const int KEY_APOSTROPHE = 0x34;
        public const int KEY_GRAVE = 0x35;
        public const int KEY_COMMA = 0x36;
        public const int KEY_PERIOD = 0x37;
        public const int KEY_SLASH = 0x38;
        public const int KEY_CAPS_LOCK = 0x39;
        public const int KEY_LEFT_CTRL = 0xE0;
        public const int KEY_LEFT_SHIFT = 0xE1;
        public const int KEY_LEFT_ALT = 0xE2;
        public const int KEY_LEFT_GUI = 0xE3;
        public const int KEY_RIGHT_CTRL = 0xE4;
        public const int KEY_RIGHT_SHIFT = 0xE5;
        public const int KEY_RIGHT_ALT = 0xE6;
        public const int KEY_RIGHT_GUI = 0xE7;
    }

    // Mouse button constants
    public static class MouseButton
    {
        public const int LEFT = 0x01;
        public const int RIGHT = 0x02;
        public const int MIDDLE = 0x04;
    }

    // Media key constants
    public static class MediaKey
    {
        public const int PLAY_PAUSE = 0xCD;
        public const int SCAN_NEXT = 0xB5;
        public const int SCAN_PREVIOUS = 0xB6;
        public const int VOLUME_UP = 0xE9;
        public const int VOLUME_DOWN = 0xEA;
        public const int MUTE = 0xE2;
    }

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
            Debug.Log("Initializing Modern BLE HID Plugin...");

            // Get the Unity player activity
            AndroidJavaClass unityPlayer = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
            _unityActivity = unityPlayer.GetStatic<AndroidJavaObject>("currentActivity");

            // Get the plugin class using the new modern implementation
            _pluginClass = new AndroidJavaClass("com.inventonater.blehid.unity.ModernBleHidPlugin");

            // Create a callback instance
            AndroidJavaClass callbackClass = new AndroidJavaClass("com.inventonater.blehid.unity.UnityCallback");
            CallbackImpl callbackImpl = new CallbackImpl(this);
            AndroidJavaObject callbackInstance = callbackClass.CallStatic<AndroidJavaObject>("createInstance", callbackImpl);

            // Initialize the plugin
            bool result = _pluginClass.CallStatic<bool>("initialize", _unityActivity);
            if (!result)
            {
                Debug.LogError("Failed to initialize Modern BLE HID Plugin");
                return;
            }

            // Set the callback
            _pluginClass.CallStatic("setCallback", callbackInstance);

            _isInitialized = true;
            Debug.Log("Modern BLE HID Plugin initialized successfully");
        }
        catch (Exception e)
        {
            Debug.LogError("Error initializing Modern BLE HID Plugin: " + e.Message);
        }
    }

    /// <summary>
    /// Checks if BLE HID device mode is supported on this device.
    /// </summary>
    public bool IsHidDeviceSupported()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("isHidDeviceSupported");
#else
        return false;
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
    /// Gets the name of the connected device.
    /// </summary>
    public string GetConnectedDeviceName()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return null;
        return _pluginClass.CallStatic<string>("getConnectedDeviceName");
#else
        return null;
#endif
    }

    #region Keyboard Methods

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
    /// Types a string by sending multiple key presses.
    /// </summary>
    public bool TypeString(string text)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("typeString", text);
#else
        Debug.LogWarning("BLE HID string typing is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Releases all pressed keys.
    /// </summary>
    public bool ReleaseAllKeys()
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("releaseAllKeys");
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
        return _pluginClass.CallStatic<bool>("sendKey", keyCode);
#else
        Debug.LogWarning("BLE HID key pressing is only available on Android devices.");
        return false;
#endif
    }

    #endregion

    #region Mouse Methods

    /// <summary>
    /// Moves the mouse pointer relative to its current position.
    /// </summary>
    public bool MoveMouseRelative(int x, int y)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("moveMouseRelative", x, y);
#else
        Debug.LogWarning("BLE HID mouse movement is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Presses a mouse button.
    /// </summary>
    public bool PressMouseButton(int button)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("pressMouseButton", button);
#else
        Debug.LogWarning("BLE HID mouse button pressing is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Releases a mouse button.
    /// </summary>
    public bool ReleaseMouseButton(int button)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("releaseMouseButton", button);
#else
        Debug.LogWarning("BLE HID mouse button releasing is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Clicks a mouse button (press and release).
    /// </summary>
    public bool ClickMouseButton(int button)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("clickMouseButton", button);
#else
        Debug.LogWarning("BLE HID mouse button clicking is only available on Android devices.");
        return false;
#endif
    }

    /// <summary>
    /// Scrolls the mouse wheel.
    /// </summary>
    public bool ScrollMouseWheel(int amount)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("scrollMouseWheel", amount);
#else
        Debug.LogWarning("BLE HID mouse wheel scrolling is only available on Android devices.");
        return false;
#endif
    }

    #endregion

    #region Media Methods

    /// <summary>
    /// Sends a media key command.
    /// </summary>
    public bool SendMediaKey(int mediaKey)
    {
#if UNITY_ANDROID && !UNITY_EDITOR
        if (!_isInitialized) return false;
        return _pluginClass.CallStatic<bool>("sendMediaKey", mediaKey);
#else
        Debug.LogWarning("BLE HID media key sending is only available on Android devices.");
        return false;
#endif
    }

    #endregion

    /// <summary>
    /// Coroutine to release keys after a delay.
    /// </summary>
    private IEnumerator ReleaseKeysAfterDelay(float delay)
    {
        yield return new WaitForSeconds(delay);
        ReleaseAllKeys();
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
        private readonly ModernBleHidManager _manager;

        public CallbackImpl(ModernBleHidManager manager) : base("com.inventonater.blehid.unity.UnityCallback")
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
            // This is a legacy callback that we don't need in the modern implementation
            // since we use registerApp from BluetoothHidDevice API instead of advertising
            Debug.Log($"Advertising state changed: {(isAdvertising ? "started" : "stopped")}");
        }
    }
}
