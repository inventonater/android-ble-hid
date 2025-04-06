using UnityEngine;
using UnityEngine.UI;

namespace BleHid
{
    /// <summary>
    /// Bootstrapper for Android BLE HID functionality.
    /// This script automatically creates all the necessary UI components
    /// to operate the BLE HID device functionality.
    /// </summary>
    public class BleHidBootstrapper : MonoBehaviour
    {
        [Header("Canvas Settings")]
        [SerializeField] private bool createCanvas = true;

        [SerializeField] private Canvas existingCanvas;
        [SerializeField] private Vector2 referenceResolution = new Vector2(1080, 1920);

        [Header("UI Settings")]
        [SerializeField] private bool createConnectionPanel = true;

        [SerializeField] private bool createMediaPanel = true;
        [SerializeField] private bool createMousePanel = true;
        [SerializeField] private Color panelColor = new Color(0.1f, 0.1f, 0.1f, 0.8f);
        [SerializeField] private Color buttonColor = new Color(0.2f, 0.2f, 0.2f, 1f);
        [SerializeField] private Color textColor = Color.white;

        [Header("Positioning")]
        [SerializeField] private float topPadding = 50f;

        [SerializeField] private float panelSpacing = 20f;
        [SerializeField] private float buttonSpacing = 10f;

        // References to created objects
        private Canvas mainCanvas;
        private BleHidManager bleManager;
        private BleHidDemoController demoController;

        // UI references
        private GameObject connectionPanel;
        private GameObject mediaPanel;
        private GameObject mousePanel;

        // UI components
        private Button initButton;
        private Button advertiseButton;
        private Text statusText;
        private Image connectionIndicator;

        private Button playPauseButton;
        private Button nextTrackButton;
        private Button prevTrackButton;
        private Button volumeUpButton;
        private Button volumeDownButton;
        private Button muteButton;

        private MouseTouchpad mouseTouchpad;
        private Button leftClickButton;
        private Button rightClickButton;
        private Slider scrollSlider;

        // Initialization state enum
        public enum BootstrapState 
        {
            NotStarted,
            SettingUpCanvas,
            CreatingUI,
            InitializingBleManager,
            ConfiguringControllers,
            Ready,
            Failed
        }
        
        // Current bootstrap state
        private BootstrapState _currentState = BootstrapState.NotStarted;
        public BootstrapState CurrentState => _currentState;
        
        // Initialization retry settings
        [Header("Initialization")]
        [SerializeField] private int maxInitRetries = 3;
        [SerializeField] private float retryDelay = 2.0f;
        private int _retryCount = 0;
        private bool _initializationInProgress = false;
        
        private void Start()
        {
            // Begin initialization sequence using coroutine for better error handling
            StartCoroutine(InitializationSequence());
        }
        
        /// <summary>
        /// Coroutine to handle the full initialization sequence with proper error handling and retries
        /// </summary>
        private IEnumerator InitializationSequence()
        {
            _initializationInProgress = true;
            _currentState = BootstrapState.NotStarted;
            
            // Setup canvas with error handling
            _currentState = BootstrapState.SettingUpCanvas;
            if (!SetupCanvas())
            {
                // Fatal error - can't proceed without canvas
                Debug.LogError("[BleHidBootstrapper] Failed to set up canvas - cannot continue");
                _currentState = BootstrapState.Failed;
                _initializationInProgress = false;
                yield break;
            }
            
            // Setup UI panels
            _currentState = BootstrapState.CreatingUI;
            CreatePanels();
            
            // Wait a frame to ensure UI is properly set up
            yield return null;
            
            // Initialize BLE Manager with retries
            _currentState = BootstrapState.InitializingBleManager;
            bool bleManagerReady = false;
            _retryCount = 0;
            
            while (!bleManagerReady && _retryCount < maxInitRetries)
            {
                bleManagerReady = SetupBleManager();
                
                if (!bleManagerReady)
                {
                    _retryCount++;
                    if (_retryCount < maxInitRetries)
                    {
                        Debug.Log($"[BleHidBootstrapper] BLE Manager initialization failed. Retrying ({_retryCount}/{maxInitRetries})");
                        // Wait before retrying
                        yield return new WaitForSeconds(retryDelay);
                    }
                }
            }
            
            if (!bleManagerReady)
            {
                Debug.LogError("[BleHidBootstrapper] Failed to initialize BLE Manager after multiple attempts");
                // Non-fatal error - can set up UI but BLE functionality won't work
                if (statusText != null)
                {
                    statusText.text = "BLE initialization failed. Please restart the app.";
                }
                _currentState = BootstrapState.Failed;
                _initializationInProgress = false;
                yield break;
            }
            
            // Setup demo controller
            _currentState = BootstrapState.ConfiguringControllers;
            SetupDemoController();
            
            // All done
            _currentState = BootstrapState.Ready;
            _initializationInProgress = false;
            
            Debug.Log("[BleHidBootstrapper] Initialization sequence completed successfully");
        }

        /// <summary>
        /// Sets up the main canvas for UI elements with error handling
        /// </summary>
        /// <returns>True if canvas was set up successfully, false otherwise</returns>
        private bool SetupCanvas()
        {
            try
            {
                if (createCanvas)
                {
                    // Create canvas GameObject
                    GameObject canvasObject = new GameObject("BleHid_Canvas");
                    mainCanvas = canvasObject.AddComponent<Canvas>();
                    mainCanvas.renderMode = RenderMode.ScreenSpaceOverlay;
    
                    // Add canvas scaler
                    CanvasScaler scaler = canvasObject.AddComponent<CanvasScaler>();
                    scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
                    scaler.referenceResolution = referenceResolution;
                    scaler.screenMatchMode = CanvasScaler.ScreenMatchMode.MatchWidthOrHeight;
                    scaler.matchWidthOrHeight = 0.5f; // Match both width and height equally
    
                    // Add graphic raycaster for input
                    canvasObject.AddComponent<GraphicRaycaster>();
                }
                else if (existingCanvas != null)
                {
                    mainCanvas = existingCanvas;
                }
                else
                {
                    Debug.LogError("[BleHidBootstrapper] No canvas provided. Please either enable 'Create Canvas' or assign an 'Existing Canvas'.");
                    return false;
                }
                
                return (mainCanvas != null);
            }
            catch (Exception e)
            {
                Debug.LogError($"[BleHidBootstrapper] Error setting up canvas: {e.Message}");
                return false;
            }
        }

        /// <summary>
        /// Creates all UI panels and their elements
        /// </summary>
        private void CreatePanels()
        {
            if (mainCanvas == null) return;

            float currentY = topPadding;

            // Create connection panel
            if (createConnectionPanel)
            {
                connectionPanel = CreatePanel("Connection_Panel", "CONNECTION", currentY, 200f);
                currentY += 200f + panelSpacing;

                // Create connection UI elements
                CreateConnectionControls(connectionPanel);
            }

            // Create media controls panel
            if (createMediaPanel)
            {
                mediaPanel = CreatePanel("Media_Panel", "MEDIA CONTROLS", currentY, 300f);
                currentY += 300f + panelSpacing;

                // Create media control buttons
                CreateMediaControls(mediaPanel);
            }

            // Create mouse controls panel
            if (createMousePanel)
            {
                mousePanel = CreatePanel("Mouse_Panel", "MOUSE CONTROLS", currentY, 400f);

                // Create mouse control elements
                CreateMouseControls(mousePanel);
            }
        }

        /// <summary>
        /// Creates a panel with a title
        /// </summary>
        private GameObject CreatePanel(string name, string title, float yPosition, float height)
        {
            // Create panel object and set up RectTransform
            GameObject panel = new GameObject(name);
            panel.transform.SetParent(mainCanvas.transform, false);

            RectTransform rectTransform = panel.AddComponent<RectTransform>();
            float screenWidth = referenceResolution.x * 0.9f; // Use 90% of screen width
            rectTransform.sizeDelta = new Vector2(screenWidth, height);
            rectTransform.anchorMin = new Vector2(0.5f, 1f);
            rectTransform.anchorMax = new Vector2(0.5f, 1f);
            rectTransform.pivot = new Vector2(0.5f, 1f);
            rectTransform.anchoredPosition = new Vector2(0, -yPosition);

            // Add panel background
            Image panelImage = panel.AddComponent<Image>();
            panelImage.color = panelColor;

            // Add panel title
            GameObject titleObject = new GameObject("Title");
            titleObject.transform.SetParent(panel.transform, false);

            RectTransform titleRect = titleObject.AddComponent<RectTransform>();
            titleRect.sizeDelta = new Vector2(screenWidth, 40f);
            titleRect.anchorMin = new Vector2(0, 1);
            titleRect.anchorMax = new Vector2(1, 1);
            titleRect.pivot = new Vector2(0.5f, 1f);
            titleRect.anchoredPosition = Vector2.zero;

            Text titleText = titleObject.AddComponent<Text>();
            titleText.text = title;
            titleText.font = Resources.GetBuiltinResource<Font>("Arial.ttf");
            titleText.fontSize = 24;
            titleText.alignment = TextAnchor.MiddleCenter;
            titleText.color = textColor;

            return panel;
        }

        /// <summary>
        /// Creates connection control UI elements
        /// </summary>
        private void CreateConnectionControls(GameObject parentPanel)
        {
            RectTransform parentRect = parentPanel.GetComponent<RectTransform>();
            float panelWidth = parentRect.sizeDelta.x;
            float buttonWidth = panelWidth * 0.4f;
            float buttonHeight = 60f;
            float startY = -50f; // Start below title

            // Initialize button
            initButton = CreateButton(parentPanel, "Init_Button", "Initialize",
                new Vector2(-buttonWidth / 2 - buttonSpacing, startY),
                new Vector2(buttonWidth, buttonHeight));

            // Advertise button
            advertiseButton = CreateButton(parentPanel, "Advertise_Button", "Start Advertising",
                new Vector2(buttonWidth / 2 + buttonSpacing, startY),
                new Vector2(buttonWidth, buttonHeight));

            // Status text
            GameObject statusObj = new GameObject("Status_Text");
            statusObj.transform.SetParent(parentPanel.transform, false);

            RectTransform statusRect = statusObj.AddComponent<RectTransform>();
            statusRect.sizeDelta = new Vector2(panelWidth * 0.7f, 40f);
            statusRect.anchorMin = new Vector2(0, 1);
            statusRect.anchorMax = new Vector2(1, 1);
            statusRect.pivot = new Vector2(0.5f, 1f);
            statusRect.anchoredPosition = new Vector2(0, startY - buttonHeight - buttonSpacing);

            statusText = statusObj.AddComponent<Text>();
            statusText.text = "Not initialized";
            statusText.font = Resources.GetBuiltinResource<Font>("Arial.ttf");
            statusText.fontSize = 20;
            statusText.alignment = TextAnchor.MiddleCenter;
            statusText.color = textColor;

            // Connection indicator
            GameObject indicatorObj = new GameObject("Connection_Indicator");
            indicatorObj.transform.SetParent(parentPanel.transform, false);

            RectTransform indicatorRect = indicatorObj.AddComponent<RectTransform>();
            indicatorRect.sizeDelta = new Vector2(20f, 20f);
            indicatorRect.anchorMin = new Vector2(0.5f, 1);
            indicatorRect.anchorMax = new Vector2(0.5f, 1);
            indicatorRect.pivot = new Vector2(0.5f, 1f);
            indicatorRect.anchoredPosition = new Vector2(panelWidth * 0.4f, startY - buttonHeight - buttonSpacing + 10f);

            connectionIndicator = indicatorObj.AddComponent<Image>();
            connectionIndicator.color = Color.red;
        }

        /// <summary>
        /// Creates media control UI elements
        /// </summary>
        private void CreateMediaControls(GameObject parentPanel)
        {
            RectTransform parentRect = parentPanel.GetComponent<RectTransform>();
            float panelWidth = parentRect.sizeDelta.x;
            float buttonWidth = panelWidth * 0.25f;
            float buttonHeight = 60f;
            float startY = -50f; // Start below title

            // Create 2x3 grid of media buttons
            float col1X = -panelWidth / 3;
            float col2X = 0;
            float col3X = panelWidth / 3;

            // Row 1
            playPauseButton = CreateButton(parentPanel, "PlayPause_Button", "Play/Pause",
                new Vector2(col1X, startY),
                new Vector2(buttonWidth, buttonHeight));

            nextTrackButton = CreateButton(parentPanel, "Next_Button", "Next",
                new Vector2(col2X, startY),
                new Vector2(buttonWidth, buttonHeight));

            prevTrackButton = CreateButton(parentPanel, "Prev_Button", "Previous",
                new Vector2(col3X, startY),
                new Vector2(buttonWidth, buttonHeight));

            // Row 2
            volumeUpButton = CreateButton(parentPanel, "VolumeUp_Button", "Vol+",
                new Vector2(col1X, startY - buttonHeight - buttonSpacing),
                new Vector2(buttonWidth, buttonHeight));

            volumeDownButton = CreateButton(parentPanel, "VolumeDown_Button", "Vol-",
                new Vector2(col2X, startY - buttonHeight - buttonSpacing),
                new Vector2(buttonWidth, buttonHeight));

            muteButton = CreateButton(parentPanel, "Mute_Button", "Mute",
                new Vector2(col3X, startY - buttonHeight - buttonSpacing),
                new Vector2(buttonWidth, buttonHeight));
        }

        /// <summary>
        /// Creates mouse control UI elements
        /// </summary>
        private void CreateMouseControls(GameObject parentPanel)
        {
            RectTransform parentRect = parentPanel.GetComponent<RectTransform>();
            float panelWidth = parentRect.sizeDelta.x;
            float buttonWidth = panelWidth * 0.25f;
            float buttonHeight = 60f;
            float startY = -50f; // Start below title

            // Create touchpad area
            GameObject touchpadObj = new GameObject("Mouse_Touchpad");
            touchpadObj.transform.SetParent(parentPanel.transform, false);

            RectTransform touchpadRect = touchpadObj.AddComponent<RectTransform>();
            float touchpadSize = panelWidth * 0.7f;
            touchpadRect.sizeDelta = new Vector2(touchpadSize, touchpadSize);
            touchpadRect.anchorMin = new Vector2(0.5f, 1);
            touchpadRect.anchorMax = new Vector2(0.5f, 1);
            touchpadRect.pivot = new Vector2(0.5f, 1f);
            touchpadRect.anchoredPosition = new Vector2(0, startY - touchpadSize / 2);

            Image touchpadImage = touchpadObj.AddComponent<Image>();
            touchpadImage.color = new Color(0.8f, 0.8f, 0.8f, 0.5f);

            mouseTouchpad = touchpadObj.AddComponent<MouseTouchpad>();
            mouseTouchpad.touchpadArea = touchpadRect;
            mouseTouchpad.touchpadBackground = touchpadImage;

            // Mouse buttons - positioned below touchpad
            float buttonY = startY - touchpadSize - buttonSpacing;

            leftClickButton = CreateButton(parentPanel, "LeftClick_Button", "Left Click",
                new Vector2(-buttonWidth - buttonSpacing, buttonY),
                new Vector2(buttonWidth, buttonHeight));

            rightClickButton = CreateButton(parentPanel, "RightClick_Button", "Right Click",
                new Vector2(buttonWidth + buttonSpacing, buttonY),
                new Vector2(buttonWidth, buttonHeight));

            // Scroll slider
            GameObject sliderObj = new GameObject("Scroll_Slider");
            sliderObj.transform.SetParent(parentPanel.transform, false);

            RectTransform sliderRect = sliderObj.AddComponent<RectTransform>();
            sliderRect.sizeDelta = new Vector2(panelWidth * 0.8f, 40f);
            sliderRect.anchorMin = new Vector2(0.5f, 1);
            sliderRect.anchorMax = new Vector2(0.5f, 1);
            sliderRect.pivot = new Vector2(0.5f, 1f);
            sliderRect.anchoredPosition = new Vector2(0, buttonY - buttonHeight - buttonSpacing);

            scrollSlider = sliderObj.AddComponent<Slider>();
            scrollSlider.minValue = 0f;
            scrollSlider.maxValue = 1f;
            scrollSlider.value = 0.5f;

            // Add slider background
            GameObject sliderBg = new GameObject("Background");
            sliderBg.transform.SetParent(sliderObj.transform, false);

            RectTransform sliderBgRect = sliderBg.AddComponent<RectTransform>();
            sliderBgRect.anchorMin = Vector2.zero;
            sliderBgRect.anchorMax = Vector2.one;
            sliderBgRect.sizeDelta = Vector2.zero;

            Image sliderBgImage = sliderBg.AddComponent<Image>();
            sliderBgImage.color = new Color(0.3f, 0.3f, 0.3f, 1f);

            // Add slider fill
            GameObject sliderFill = new GameObject("Fill");
            sliderFill.transform.SetParent(sliderObj.transform, false);

            RectTransform sliderFillRect = sliderFill.AddComponent<RectTransform>();
            sliderFillRect.anchorMin = new Vector2(0, 0);
            sliderFillRect.anchorMax = new Vector2(0.5f, 1);
            sliderFillRect.sizeDelta = Vector2.zero;

            Image sliderFillImage = sliderFill.AddComponent<Image>();
            sliderFillImage.color = new Color(0.4f, 0.4f, 0.4f, 1f);

            // Add slider handle
            GameObject sliderHandle = new GameObject("Handle");
            sliderHandle.transform.SetParent(sliderObj.transform, false);

            RectTransform sliderHandleRect = sliderHandle.AddComponent<RectTransform>();
            sliderHandleRect.sizeDelta = new Vector2(20f, 40f);
            sliderHandleRect.anchorMin = new Vector2(0.5f, 0);
            sliderHandleRect.anchorMax = new Vector2(0.5f, 1);
            sliderHandleRect.pivot = new Vector2(0.5f, 0.5f);

            Image sliderHandleImage = sliderHandle.AddComponent<Image>();
            sliderHandleImage.color = new Color(0.7f, 0.7f, 0.7f, 1f);

            // Configure slider components
            scrollSlider.targetGraphic = sliderHandleImage;
            scrollSlider.fillRect = sliderFillRect;
            scrollSlider.handleRect = sliderHandleRect;
            scrollSlider.direction = Slider.Direction.LeftToRight;

            // Add text below slider
            GameObject sliderText = new GameObject("Slider_Text");
            sliderText.transform.SetParent(parentPanel.transform, false);

            RectTransform sliderTextRect = sliderText.AddComponent<RectTransform>();
            sliderTextRect.sizeDelta = new Vector2(panelWidth * 0.8f, 30f);
            sliderTextRect.anchorMin = new Vector2(0.5f, 1);
            sliderTextRect.anchorMax = new Vector2(0.5f, 1);
            sliderTextRect.pivot = new Vector2(0.5f, 1f);
            sliderTextRect.anchoredPosition = new Vector2(0, buttonY - buttonHeight - buttonSpacing - 30f);

            Text sliderLabel = sliderText.AddComponent<Text>();
            sliderLabel.text = "Scroll Wheel";
            sliderLabel.font = Resources.GetBuiltinResource<Font>("Arial.ttf");
            sliderLabel.fontSize = 18;
            sliderLabel.alignment = TextAnchor.MiddleCenter;
            sliderLabel.color = textColor;
        }

        /// <summary>
        /// Helper method to create a button
        /// </summary>
        private Button CreateButton(GameObject parent, string name, string text, Vector2 position, Vector2 size)
        {
            GameObject buttonObj = new GameObject(name);
            buttonObj.transform.SetParent(parent.transform, false);

            RectTransform buttonRect = buttonObj.AddComponent<RectTransform>();
            buttonRect.sizeDelta = size;
            buttonRect.anchorMin = new Vector2(0.5f, 1);
            buttonRect.anchorMax = new Vector2(0.5f, 1);
            buttonRect.pivot = new Vector2(0.5f, 1f);
            buttonRect.anchoredPosition = position;

            Image buttonImage = buttonObj.AddComponent<Image>();
            buttonImage.color = buttonColor;

            Button button = buttonObj.AddComponent<Button>();
            button.targetGraphic = buttonImage;
            ColorBlock colors = button.colors;
            colors.normalColor = buttonColor;
            colors.highlightedColor = new Color(buttonColor.r + 0.1f, buttonColor.g + 0.1f, buttonColor.b + 0.1f, buttonColor.a);
            colors.pressedColor = new Color(buttonColor.r - 0.1f, buttonColor.g - 0.1f, buttonColor.b - 0.1f, buttonColor.a);
            button.colors = colors;

            GameObject textObj = new GameObject("Text");
            textObj.transform.SetParent(buttonObj.transform, false);

            RectTransform textRect = textObj.AddComponent<RectTransform>();
            textRect.anchorMin = Vector2.zero;
            textRect.anchorMax = Vector2.one;
            textRect.offsetMin = Vector2.zero;
            textRect.offsetMax = Vector2.zero;

            Text buttonText = textObj.AddComponent<Text>();
            buttonText.text = text;
            buttonText.font = Resources.GetBuiltinResource<Font>("Arial.ttf");
            buttonText.fontSize = 18;
            buttonText.alignment = TextAnchor.MiddleCenter;
            buttonText.color = textColor;

            return button;
        }

        /// <summary>
        /// Sets up the BleHidManager with dependency injection and error handling
        /// </summary>
        /// <returns>True if successful, false otherwise</returns>
        private bool SetupBleManager()
        {
            try
            {
                // Get reference to the singleton instance
                bleManager = BleHidManager.Instance;
                
                if (bleManager == null)
                {
                    Debug.LogError("[BleHidBootstrapper] Failed to get BleHidManager instance");
                    return false;
                }
                
                // Set UI references
                bleManager.SetStatusText(statusText);
                bleManager.SetConnectionIndicator(connectionIndicator);
                
                // Check if initialization succeeded
                if (!bleManager.IsInitialized() && bleManager.CurrentInitState != BleHidManager.InitState.Initializing)
                {
                    // Try to initialize if not already initializing
                    if (!bleManager.InitializePlugin())
                    {
                        if (bleManager.CurrentInitState == BleHidManager.InitState.InitializationFailed)
                        {
                            Debug.LogError("[BleHidBootstrapper] BleHidManager initialization failed");
                            return false;
                        }
                        else if (bleManager.CurrentInitState == BleHidManager.InitState.Unsupported)
                        {
                            Debug.LogWarning("[BleHidBootstrapper] BLE peripheral mode not supported on this device");
                            // We consider this a 'success' as we've detected the limitation properly
                            return true;
                        }
                        else if (bleManager.CurrentInitState == BleHidManager.InitState.WaitingForPermissions)
                        {
                            Debug.Log("[BleHidBootstrapper] Waiting for permission response");
                            // This is not a failure, just waiting
                            return false;
                        }
                    }
                }
                
                return true;
            }
            catch (Exception e)
            {
                Debug.LogError($"[BleHidBootstrapper] Error setting up BleHidManager: {e.Message}");
                return false;
            }
        }

        /// <summary>
        /// Sets up the BleHidDemoController with error handling
        /// </summary>
        private void SetupDemoController()
        {
            try
            {
                // Check if a controller already exists to avoid duplicates
                BleHidDemoController existingController = FindObjectOfType<BleHidDemoController>();
                if (existingController != null)
                {
                    Debug.Log("[BleHidBootstrapper] Using existing BleHidDemoController");
                    demoController = existingController;
                }
                else
                {
                    GameObject controllerObject = new GameObject("BleHidDemoController");
                    demoController = controllerObject.AddComponent<BleHidDemoController>();
                }
    
                // Configure the controller with dependencies
                if (demoController != null)
                {
                    // Set the BleHidManager reference first (important for initialization order)
                    demoController.bleManager = bleManager;
                    
                    // Set autoInitialize property to false since we're handling init here
                    demoController.autoInitialize = false;
                    
                    // Configure controller with UI elements
                    demoController.initButton = initButton;
                    demoController.advertiseButton = advertiseButton;
                    demoController.statusText = statusText;
                    demoController.connectionIndicator = connectionIndicator;
    
                    demoController.playPauseButton = playPauseButton;
                    demoController.nextTrackButton = nextTrackButton;
                    demoController.prevTrackButton = prevTrackButton;
                    demoController.volumeUpButton = volumeUpButton;
                    demoController.volumeDownButton = volumeDownButton;
                    demoController.muteButton = muteButton;
    
                    demoController.mouseTouchpad = mouseTouchpad;
                    demoController.leftClickButton = leftClickButton;
                    demoController.rightClickButton = rightClickButton;
                    demoController.scrollSlider = scrollSlider;
                }
                else
                {
                    Debug.LogError("[BleHidBootstrapper] Failed to create BleHidDemoController");
                }
            }
            catch (Exception e)
            {
                Debug.LogError($"[BleHidBootstrapper] Error setting up BleHidDemoController: {e.Message}");
            }
        }
    }
}
