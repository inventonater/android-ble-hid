using UnityEngine;
using UnityEngine.UI;

/// <summary>
/// Demo controller for BLE HID functionality.
/// Provides a simple UI to demonstrate media and mouse control capabilities.
/// </summary>
public class BleHidDemoController : MonoBehaviour
{
    [Header("UI References")]
    [SerializeField] private Button initButton;
    [SerializeField] private Button advertiseButton;
    [SerializeField] private Text statusText;
    [SerializeField] private Image connectionIndicator;
    
    [Header("Media Controls")]
    [SerializeField] private Button playPauseButton;
    [SerializeField] private Button nextTrackButton;
    [SerializeField] private Button prevTrackButton;
    [SerializeField] private Button volumeUpButton;
    [SerializeField] private Button volumeDownButton;
    [SerializeField] private Button muteButton;
    
    [Header("Mouse Controls")]
    [SerializeField] private MouseTouchpad mouseTouchpad; // Custom touchpad component for mouse movement
    [SerializeField] private Button leftClickButton;
    [SerializeField] private Button rightClickButton;
    [SerializeField] private Slider scrollSlider;
    
    [Header("Settings")]
    [SerializeField] private float mouseSensitivity = 5f;
    
    // Reference to BLE HID manager
    private BleHidManager bleManager;
    
    private void Start()
    {
        // Setup BleHidManager with UI references
        GameObject managerObject = new GameObject("BleHidManager");
        bleManager = managerObject.AddComponent<BleHidManager>();
        
        // Pass UI references to the manager
        if (statusText != null && connectionIndicator != null)
        {
            // Set private fields through reflection for demo purposes
            var statusField = typeof(BleHidManager).GetField("statusText", 
                System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic);
            var indicatorField = typeof(BleHidManager).GetField("connectionIndicator", 
                System.Reflection.BindingFlags.Instance | System.Reflection.BindingFlags.NonPublic);
            
            if (statusField != null && indicatorField != null)
            {
                statusField.SetValue(bleManager, statusText);
                indicatorField.SetValue(bleManager, connectionIndicator);
            }
        }
        
        // Setup touchpad mouse control
        if (mouseTouchpad != null)
        {
            // Subscribe to the touchpad's mouse move event
            mouseTouchpad.OnMouseMove += HandleMouseMovement;
        }
        
        // Setup button listeners
        SetupButtons();
        
        // Subscribe to connection events
        bleManager.OnConnected += OnDeviceConnected;
        bleManager.OnDisconnected += OnDeviceDisconnected;
        
        // Initialize automatically if desired
        // bleManager.InitializePlugin();
    }
    
    private void SetupButtons()
    {
        // System buttons
        if (initButton != null)
        {
            initButton.onClick.AddListener(() => {
                bleManager.InitializePlugin();
                SetControlButtonsInteractable(bleManager.IsInitialized() && bleManager.IsBlePeripheralSupported());
            });
        }
        
        if (advertiseButton != null)
        {
            advertiseButton.onClick.AddListener(() => {
                if (bleManager.IsAdvertising())
                {
                    bleManager.StopAdvertising();
                    advertiseButton.GetComponentInChildren<Text>().text = "Start Advertising";
                }
                else
                {
                    bleManager.StartAdvertising();
                    advertiseButton.GetComponentInChildren<Text>().text = "Stop Advertising";
                }
            });
        }
        
        // Media control buttons
        if (playPauseButton != null)
            playPauseButton.onClick.AddListener(() => bleManager.PlayPause());
        
        if (nextTrackButton != null)
            nextTrackButton.onClick.AddListener(() => bleManager.NextTrack());
        
        if (prevTrackButton != null)
            prevTrackButton.onClick.AddListener(() => bleManager.PreviousTrack());
        
        if (volumeUpButton != null)
            volumeUpButton.onClick.AddListener(() => bleManager.VolumeUp());
        
        if (volumeDownButton != null)
            volumeDownButton.onClick.AddListener(() => bleManager.VolumeDown());
        
        if (muteButton != null)
            muteButton.onClick.AddListener(() => bleManager.Mute());
        
        // Mouse control buttons
        if (leftClickButton != null)
            leftClickButton.onClick.AddListener(() => bleManager.ClickMouseButton(BleHidManager.HidConstants.BUTTON_LEFT));
        
        if (rightClickButton != null)
            rightClickButton.onClick.AddListener(() => bleManager.ClickMouseButton(BleHidManager.HidConstants.BUTTON_RIGHT));
        
        // Set all media and mouse control buttons initially disabled
        SetControlButtonsInteractable(false);
    }
    
    private void Update()
    {
        // Mouse touchpad input is handled via events
        
        // Handle scroll wheel input if available and connected
        if (scrollSlider != null && bleManager.IsConnected())
        {
            // Map slider value (0-1) to scroll range (-127 to 127)
            float normalizedValue = (scrollSlider.value - 0.5f) * 2f; // -1 to 1
            int scrollAmount = Mathf.RoundToInt(normalizedValue * 127);
            
            // Only send scroll if there's actual input
            if (Mathf.Abs(scrollAmount) > 5)
            {
                bleManager.ScrollMouseWheel(scrollAmount);
                
                // Reset slider to center after use
                scrollSlider.value = 0.5f;
            }
        }
    }
    
    private void OnDeviceConnected()
    {
        SetControlButtonsInteractable(true);
        
        if (advertiseButton != null)
        {
            advertiseButton.interactable = false;
            advertiseButton.GetComponentInChildren<Text>().text = "Connected";
        }
    }
    
    private void OnDeviceDisconnected()
    {
        // Disable control buttons when disconnected
        SetControlButtonsInteractable(false);
        
        if (advertiseButton != null)
        {
            advertiseButton.interactable = true;
            advertiseButton.GetComponentInChildren<Text>().text = "Start Advertising";
        }
    }
    
    private void SetControlButtonsInteractable(bool interactable)
    {
        // Set media buttons interactable state
        if (playPauseButton != null) playPauseButton.interactable = interactable;
        if (nextTrackButton != null) nextTrackButton.interactable = interactable;
        if (prevTrackButton != null) prevTrackButton.interactable = interactable;
        if (volumeUpButton != null) volumeUpButton.interactable = interactable;
        if (volumeDownButton != null) volumeDownButton.interactable = interactable;
        if (muteButton != null) muteButton.interactable = interactable;
        
        // Set mouse buttons interactable state
        if (leftClickButton != null) leftClickButton.interactable = interactable;
        if (rightClickButton != null) rightClickButton.interactable = interactable;
        if (scrollSlider != null) scrollSlider.interactable = interactable;
    }
    
    /// <summary>
    /// Handle mouse movement from the touchpad
    /// </summary>
    private void HandleMouseMovement(Vector2 movementDelta)
    {
        if (bleManager != null && bleManager.IsConnected())
        {
            // Apply sensitivity and send movement to BLE HID manager
            bleManager.MoveMouse(movementDelta, mouseSensitivity);
        }
    }
    
    private void OnDestroy()
    {
        // Clean up event subscriptions
        if (bleManager != null)
        {
            bleManager.OnConnected -= OnDeviceConnected;
            bleManager.OnDisconnected -= OnDeviceDisconnected;
        }
        
        // Unsubscribe from touchpad events
        if (mouseTouchpad != null)
        {
            mouseTouchpad.OnMouseMove -= HandleMouseMovement;
        }
    }
}
