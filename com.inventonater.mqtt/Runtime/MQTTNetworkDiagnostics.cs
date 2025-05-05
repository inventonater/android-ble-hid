using System;
using System.Collections;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text;
using System.IO;
using Best.MQTT;
using UnityEngine;

namespace Inventonater
{
    /// <summary>
    /// Provides network diagnostics for MQTT connections without modifying the existing MQTT client.
    /// Helps diagnose connectivity issues by checking network interfaces, DNS resolution, and port connectivity.
    /// </summary>
    public class MQTTNetworkDiagnostics : MonoBehaviour
    {
        [Header("Target Settings")]
        [SerializeField] private string targetHost = "192.168.1.254";
        [SerializeField] private int targetPort = 1883;
        [SerializeField] private float diagnosticInterval = 10f;
        [SerializeField] private int maxRetries = 5;
        [SerializeField] private float initialRetryDelay = 2f;
        
        [Header("Logging Settings")]
        [SerializeField] private bool logToFile = true;
        [SerializeField] private string logFileName = "mqtt_network_diagnostics.log";
        [SerializeField] private bool verboseLogging = true;
        
        [Header("References")]
        [SerializeField] private InventoMqttClient client;
        
        // Internal state
        private bool _isRunningDiagnostics = false;
        private StringBuilder _logBuilder = new StringBuilder();
        private StreamWriter _logWriter;
        private string _logFilePath;
        private int _currentRetryCount = 0;
        private float _currentRetryDelay;
        
        // Diagnostic results
        [Header("Diagnostic Results")]
        [RO] public bool isNetworkAvailable = false;
        [RO] public bool isDnsResolvable = false;
        [RO] public bool isPortReachable = false;
        [RO] public string lastDiagnosticResult = "Not run yet";
        [RO] public string networkInterfaceInfo = "Not checked";
        [RO] public string lastErrorMessage = "";
        
        private void Awake()
        {
            // Create log file path in persistent data path
            _logFilePath = Path.Combine(Application.persistentDataPath, logFileName);
            _currentRetryDelay = initialRetryDelay;
            
            // Auto-find MQTT client if not assigned
            if (client != null) return;

            client = FindObjectOfType<InventoMqttClient>();
            if (client != null)
            {
                Log("Auto-found InventoClient reference");
            }
        }
        
        private void OnEnable()
        {
            StartDiagnostics();
        }
        
        private void OnDisable()
        {
            StopDiagnostics();
        }
        
        private void OnDestroy()
        {
            CloseLogFile();
        }
        
        /// <summary>
        /// Starts the network diagnostics process
        /// </summary>
        public void StartDiagnostics()
        {
            if (!_isRunningDiagnostics)
            {
                _isRunningDiagnostics = true;
                InitializeLogging();
                StartCoroutine(RunDiagnosticsLoop());
                Log("MQTT Network Diagnostics started");
            }
        }
        
        /// <summary>
        /// Stops the network diagnostics process
        /// </summary>
        public void StopDiagnostics()
        {
            if (_isRunningDiagnostics)
            {
                _isRunningDiagnostics = false;
                StopAllCoroutines();
                Log("MQTT Network Diagnostics stopped");
                CloseLogFile();
            }
        }
        
        /// <summary>
        /// Main diagnostic loop that runs at the specified interval
        /// </summary>
        private IEnumerator RunDiagnosticsLoop()
        {
            while (_isRunningDiagnostics)
            {
                yield return StartCoroutine(PerformFullDiagnostic());
                yield return new WaitForSeconds(diagnosticInterval);
            }
        }
        
        /// <summary>
        /// Performs a comprehensive network diagnostic
        /// </summary>
        private IEnumerator PerformFullDiagnostic()
        {
            Log("Starting network diagnostic...", LogLevel.Info);
            
            // Check network interfaces
            CheckNetworkInterfaces();
            yield return new WaitForSeconds(0.5f);
            
            // If network is available, check DNS resolution
            if (isNetworkAvailable)
            {
                yield return StartCoroutine(CheckDnsResolution());
                
                // If DNS is resolvable, check port connectivity
                if (isDnsResolvable)
                {
                    yield return StartCoroutine(CheckPortConnectivity());
                }
            }
            
            // Check MQTT client state if available
            if (client != null)
            {
                CheckMqttClientState();
            }
            
            // Compile diagnostic summary
            CompileDiagnosticSummary();
            
            // Handle retry logic if needed
            if (!isPortReachable && _currentRetryCount < maxRetries)
            {
                _currentRetryCount++;
                Log($"Connection failed. Retry {_currentRetryCount}/{maxRetries} scheduled in {_currentRetryDelay} seconds", LogLevel.Warning);
                yield return new WaitForSeconds(_currentRetryDelay);
                
                // Exponential backoff for retry delay
                _currentRetryDelay *= 1.5f;
            }
            else if (isPortReachable)
            {
                // Reset retry count and delay if successful
                _currentRetryCount = 0;
                _currentRetryDelay = initialRetryDelay;
            }
            
            Log("Diagnostic complete", LogLevel.Info);
        }
        
        /// <summary>
        /// Checks all network interfaces and their status
        /// </summary>
        private void CheckNetworkInterfaces()
        {
            Log("Checking network interfaces...", LogLevel.Info);
            
            try
            {
                StringBuilder interfaceInfo = new StringBuilder();
                bool anyNetworkAvailable = false;
                
                NetworkInterface[] interfaces = NetworkInterface.GetAllNetworkInterfaces();
                
                interfaceInfo.AppendLine($"Found {interfaces.Length} network interfaces:");
                
                foreach (NetworkInterface ni in interfaces)
                {
                    // Skip interfaces that are down
                    if (ni.OperationalStatus != OperationalStatus.Up)
                    {
                        if (verboseLogging)
                        {
                            interfaceInfo.AppendLine($"- {ni.Name}: {ni.OperationalStatus} (Skipped)");
                        }
                        continue;
                    }
                    
                    interfaceInfo.AppendLine($"- {ni.Name}: {ni.OperationalStatus}");
                    interfaceInfo.AppendLine($"  Description: {ni.Description}");
                    interfaceInfo.AppendLine($"  Type: {ni.NetworkInterfaceType}");
                    interfaceInfo.AppendLine($"  Speed: {ni.Speed / 1000000} Mbps");
                    
                    IPInterfaceProperties ipProps = ni.GetIPProperties();
                    
                    // List IPv4 addresses
                    foreach (UnicastIPAddressInformation ip in ipProps.UnicastAddresses)
                    {
                        if (ip.Address.AddressFamily == AddressFamily.InterNetwork) // IPv4
                        {
                            interfaceInfo.AppendLine($"  IPv4: {ip.Address}, Subnet: {ip.IPv4Mask}");
                            anyNetworkAvailable = true;
                        }
                    }
                    
                    // List DNS servers
                    if (ipProps.DnsAddresses.Count > 0)
                    {
                        interfaceInfo.AppendLine("  DNS Servers:");
                        foreach (IPAddress dns in ipProps.DnsAddresses)
                        {
                            interfaceInfo.AppendLine($"    {dns}");
                        }
                    }
                    
                    // List gateway
                    if (ipProps.GatewayAddresses.Count > 0)
                    {
                        interfaceInfo.AppendLine("  Gateways:");
                        foreach (GatewayIPAddressInformation gateway in ipProps.GatewayAddresses)
                        {
                            interfaceInfo.AppendLine($"    {gateway.Address}");
                        }
                    }
                    
                    interfaceInfo.AppendLine();
                }
                
                networkInterfaceInfo = interfaceInfo.ToString();
                isNetworkAvailable = anyNetworkAvailable;
                
                if (isNetworkAvailable)
                {
                    Log("Network is available", LogLevel.Info);
                    if (verboseLogging)
                    {
                        Log(networkInterfaceInfo, LogLevel.Debug);
                    }
                }
                else
                {
                    Log("No active network interfaces found", LogLevel.Error);
                }
            }
            catch (Exception ex)
            {
                isNetworkAvailable = false;
                lastErrorMessage = $"Error checking network interfaces: {ex.Message}";
                Log(lastErrorMessage, LogLevel.Error);
            }
        }
        
        /// <summary>
        /// Checks DNS resolution for the target host
        /// </summary>
        private IEnumerator CheckDnsResolution()
        {
            Log($"Resolving host: {targetHost}", LogLevel.Info);
            isDnsResolvable = false;
            
            // Skip DNS resolution if the target is already an IP address
            if (IPAddress.TryParse(targetHost, out _))
            {
                Log($"{targetHost} is already an IP address, skipping DNS resolution", LogLevel.Info);
                isDnsResolvable = true;
                yield break;
            }
            
            // Use a separate thread for DNS resolution to avoid blocking
            bool resolutionComplete = false;
            IPAddress[] addresses = null;
            Exception resolutionError = null;
            
            // Start async DNS resolution
            try
            {
                System.Threading.ThreadPool.QueueUserWorkItem(_ => 
                {
                    try
                    {
                        addresses = Dns.GetHostAddresses(targetHost);
                        resolutionComplete = true;
                    }
                    catch (Exception ex)
                    {
                        resolutionError = ex;
                        resolutionComplete = true;
                    }
                });
            }
            catch (Exception ex)
            {
                lastErrorMessage = $"Error starting DNS resolution thread: {ex.Message}";
                Log(lastErrorMessage, LogLevel.Error);
                isDnsResolvable = false;
                yield break;
            }
            
            // Wait for resolution to complete with timeout
            float timeoutSeconds = 5f;
            float elapsed = 0f;
            while (!resolutionComplete && elapsed < timeoutSeconds)
            {
                yield return new WaitForSeconds(0.1f);
                elapsed += 0.1f;
            }
                
            // Check results
            if (!resolutionComplete)
            {
                Log($"DNS resolution timed out after {timeoutSeconds} seconds", LogLevel.Error);
                isDnsResolvable = false;
            }
            else if (resolutionError != null)
            {
                lastErrorMessage = $"DNS resolution error: {resolutionError.Message}";
                Log(lastErrorMessage, LogLevel.Error);
                isDnsResolvable = false;
            }
            else if (addresses != null && addresses.Length > 0)
            {
                StringBuilder addressList = new StringBuilder();
                addressList.AppendLine($"Resolved {targetHost} to:");
                
                foreach (IPAddress address in addresses)
                {
                    addressList.AppendLine($"- {address}");
                }
                
                Log(addressList.ToString(), LogLevel.Info);
                isDnsResolvable = true;
            }
            else
            {
                Log($"Could not resolve any IP addresses for {targetHost}", LogLevel.Error);
                isDnsResolvable = false;
            }
        }
        
        /// <summary>
        /// Checks TCP connectivity to the target host and port
        /// </summary>
        private IEnumerator CheckPortConnectivity()
        {
            Log($"Testing connectivity to {targetHost}:{targetPort}", LogLevel.Info);
            isPortReachable = false;
            
            // Use a separate thread for TCP connection to avoid blocking
            bool connectionComplete = false;
            bool connectionSuccessful = false;
            Exception connectionError = null;
            
            // Start async TCP connection
            try
            {
                System.Threading.ThreadPool.QueueUserWorkItem(_ => 
                {
                    try
                    {
                        using (TcpClient client = new TcpClient())
                        {
                            // Set a connection timeout
                            IAsyncResult result = client.BeginConnect(targetHost, targetPort, null, null);
                            connectionSuccessful = result.AsyncWaitHandle.WaitOne(TimeSpan.FromSeconds(5));
                            
                            if (connectionSuccessful)
                            {
                                // Complete the connection
                                client.EndConnect(result);
                            }
                            
                            connectionComplete = true;
                        }
                    }
                    catch (Exception ex)
                    {
                        connectionError = ex;
                        connectionComplete = true;
                    }
                });
            }
            catch (Exception ex)
            {
                lastErrorMessage = $"Error starting TCP connection thread: {ex.Message}";
                Log(lastErrorMessage, LogLevel.Error);
                isPortReachable = false;
                yield break;
            }
            
            // Wait for connection attempt to complete with timeout
            float timeoutSeconds = 6f;
            float elapsed = 0f;
            while (!connectionComplete && elapsed < timeoutSeconds)
            {
                yield return new WaitForSeconds(0.1f);
                elapsed += 0.1f;
            }
                
            // Check results
            if (!connectionComplete)
            {
                Log($"TCP connection attempt timed out after {timeoutSeconds} seconds", LogLevel.Error);
                isPortReachable = false;
            }
            else if (connectionError != null)
            {
                lastErrorMessage = $"TCP connection error: {connectionError.Message}";
                Log(lastErrorMessage, LogLevel.Error);
                isPortReachable = false;
            }
            else if (connectionSuccessful)
            {
                Log($"Successfully connected to {targetHost}:{targetPort}", LogLevel.Success);
                isPortReachable = true;
            }
            else
            {
                Log($"Failed to connect to {targetHost}:{targetPort}", LogLevel.Error);
                isPortReachable = false;
            }
        }
        
        /// <summary>
        /// Checks the MQTT client state if available
        /// </summary>
        private void CheckMqttClientState()
        {
            if (client == null)
            {
                Log("No MQTT client reference available", LogLevel.Warning);
                return;
            }
            
            try
            {
                // Access the client field using reflection to avoid modifying InventoClient
                var clientField = typeof(InventoMqttClient).GetField("client", System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
                if (clientField != null)
                {
                    var client = clientField.GetValue(this.client) as MQTTClient;
                    if (client != null)
                    {
                        Log($"MQTT client state: {client.State}", LogLevel.Info);
                    }
                    else
                    {
                        Log("MQTT client is null", LogLevel.Warning);
                    }
                }
            }
            catch (Exception ex)
            {
                Log($"Error checking MQTT client state: {ex.Message}", LogLevel.Error);
            }
        }
        
        /// <summary>
        /// Compiles a summary of the diagnostic results
        /// </summary>
        private void CompileDiagnosticSummary()
        {
            StringBuilder summary = new StringBuilder();
            summary.AppendLine("=== MQTT Network Diagnostic Summary ===");
            summary.AppendLine($"Timestamp: {DateTime.Now}");
            summary.AppendLine($"Target: {targetHost}:{targetPort}");
            summary.AppendLine($"Network Available: {isNetworkAvailable}");
            summary.AppendLine($"DNS Resolvable: {isDnsResolvable}");
            summary.AppendLine($"Port Reachable: {isPortReachable}");
            
            if (!string.IsNullOrEmpty(lastErrorMessage))
            {
                summary.AppendLine($"Last Error: {lastErrorMessage}");
            }
            
            lastDiagnosticResult = summary.ToString();
            Log(lastDiagnosticResult, LogLevel.Info);
        }
        
        /// <summary>
        /// Initializes the log file
        /// </summary>
        private void InitializeLogging()
        {
            if (logToFile)
            {
                try
                {
                    // Create or append to log file
                    _logWriter = new StreamWriter(_logFilePath, true);
                    _logWriter.AutoFlush = true;
                    
                    _logWriter.WriteLine("\n\n=== MQTT Network Diagnostics Log ===");
                    _logWriter.WriteLine($"Started: {DateTime.Now}");
                    _logWriter.WriteLine($"Target: {targetHost}:{targetPort}");
                    _logWriter.WriteLine("=====================================\n");
                }
                catch (Exception ex)
                {
                    Debug.LogError($"Failed to initialize log file: {ex.Message}");
                    logToFile = false;
                }
            }
        }
        
        /// <summary>
        /// Closes the log file
        /// </summary>
        private void CloseLogFile()
        {
            if (_logWriter != null)
            {
                try
                {
                    _logWriter.WriteLine($"\nDiagnostics stopped: {DateTime.Now}");
                    _logWriter.Close();
                    _logWriter = null;
                }
                catch (Exception ex)
                {
                    Debug.LogError($"Error closing log file: {ex.Message}");
                }
            }
        }
        
        /// <summary>
        /// Log levels for different types of messages
        /// </summary>
        private enum LogLevel
        {
            Debug,
            Info,
            Warning,
            Error,
            Success
        }
        
        /// <summary>
        /// Logs a message to the console and optionally to a file
        /// </summary>
        private void Log(string message, LogLevel level = LogLevel.Info)
        {
            // Skip debug messages if verbose logging is disabled
            if (level == LogLevel.Debug && !verboseLogging)
            {
                return;
            }
            
            string timestamp = DateTime.Now.ToString("HH:mm:ss.fff");
            string logMessage = $"[MQTT_DIAG][{timestamp}] {message}";
            
            // Log to console with appropriate log level - using a consistent tag for easier filtering in logcat
            switch (level)
            {
                case LogLevel.Error:
                    Debug.LogError(logMessage);
                    break;
                case LogLevel.Warning:
                    Debug.LogWarning(logMessage);
                    break;
                default:
                    Debug.Log(logMessage);
                    break;
            }
            
            // Log to file if enabled
            if (logToFile && _logWriter != null)
            {
                try
                {
                    _logWriter.WriteLine(logMessage);
                }
                catch (Exception ex)
                {
                    Debug.LogError($"Failed to write to log file: {ex.Message}");
                    logToFile = false;
                }
            }
        }
    }
    
    /// <summary>
    /// Attribute to make fields read-only in the inspector
    /// </summary>
    public class ROAttribute : PropertyAttribute { }
    
    #if UNITY_EDITOR
    /// <summary>
    /// Custom property drawer for ReadOnly attribute
    /// </summary>
    [UnityEditor.CustomPropertyDrawer(typeof(ROAttribute))]
    public class ReadOnlyDrawer : UnityEditor.PropertyDrawer
    {
        public override void OnGUI(Rect position, UnityEditor.SerializedProperty property, GUIContent label)
        {
            GUI.enabled = false;
            UnityEditor.EditorGUI.PropertyField(position, property, label, true);
            GUI.enabled = true;
        }
    }
    #endif
}
