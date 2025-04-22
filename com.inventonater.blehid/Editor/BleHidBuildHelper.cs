using UnityEditor;
using UnityEngine;
using System.IO;
using System;

namespace Inventonater.BleHid.Editor
{
    /// <summary>
    /// Editor utility for helping with plugin build and setup
    /// </summary>
    public class BleHidBuildHelper
    {
        private const string AndroidPluginsFolderPath = "Runtime/Plugins/Android";
        private const string PluginFilename = "BleHid.aar";
        private const string ManifestFilename = "AndroidManifest.xml";
        
        [MenuItem("Inventonater/BLE HID/Setup Plugin")]
        public static void SetupPlugin()
        {
            Debug.Log("Setting up BLE HID Plugin...");
            
            // Make sure Android plugin folder exists
            string packageFolder = GetPackageRootPath();
            string androidPluginsPath = Path.Combine(packageFolder, AndroidPluginsFolderPath);
            
            if (!Directory.Exists(androidPluginsPath))
            {
                Debug.Log($"Creating Android plugins folder: {androidPluginsPath}");
                Directory.CreateDirectory(androidPluginsPath);
            }
            
            // Check if AAR file exists in the unity-test project
            string unityTestPluginPath = Path.Combine(Application.dataPath, "Plugins/Android", PluginFilename);
            
            // This will copy from the unity-test project to our package for migration purposes
            if (File.Exists(unityTestPluginPath))
            {
                Debug.Log("Found existing AAR files in Unity project. Copying to package...");
                
                try
                {
                    File.Copy(unityTestPluginPath, Path.Combine(androidPluginsPath, PluginFilename), true);
                    Debug.Log("Plugin file copied successfully!");
                    
                    // Check if the project's Plugins/Android folder has an AndroidManifest.xml
                    string unityProjectManifestPath = Path.Combine(Application.dataPath, "Plugins/Android", ManifestFilename);
                    if (File.Exists(unityProjectManifestPath))
                    {
                        Debug.Log("Warning: Found existing AndroidManifest.xml in your Unity project. " +
                                  "The BleHid package now includes its own AndroidManifest.xml with all required permissions.");
                    }
                }
                catch (Exception e)
                {
                    Debug.LogError($"Error copying files: {e.Message}");
                }
            }
            else
            {
                Debug.LogWarning("Plugin AAR file not found in Unity project. Please build the Android plugin first.");
                if (EditorUtility.DisplayDialog("Build Plugin", 
                    "Would you like to build the Android plugin now?", 
                    "Yes", "No"))
                {
                    BuildAndroidPlugin();
                }
            }
        }
        
        [MenuItem("Inventonater/BLE HID/Build Android Plugin")]
        public static void BuildAndroidPlugin()
        {
            Debug.Log("Building Android plugin...");
            
            // Ensure users know about the included manifest
            Debug.Log("Note: The BleHid package includes AndroidManifest.xml with all required Bluetooth permissions.");
            
            // Execute gradle build
            string projectRoot = Path.GetFullPath(Path.Combine(Application.dataPath, "../.."));
            string gradleCommand = $"cd {projectRoot} && ./gradlew :unity-plugin:copyToUnity";
            
            Debug.Log($"Executing: {gradleCommand}");
            
            // Execute system command
            System.Diagnostics.Process process = new System.Diagnostics.Process();
            process.StartInfo.FileName = "/bin/bash";
            process.StartInfo.Arguments = $"-c \"{gradleCommand}\"";
            process.StartInfo.UseShellExecute = false;
            process.StartInfo.RedirectStandardOutput = true;
            process.StartInfo.RedirectStandardError = true;
            process.StartInfo.CreateNoWindow = true;
            
            process.OutputDataReceived += (sender, args) => {
                if (!string.IsNullOrEmpty(args.Data))
                    Debug.Log(args.Data);
            };
            
            process.ErrorDataReceived += (sender, args) => {
                if (!string.IsNullOrEmpty(args.Data))
                    Debug.LogError(args.Data);
            };
            
            process.Start();
            process.BeginOutputReadLine();
            process.BeginErrorReadLine();
            process.WaitForExit();
            
            int exitCode = process.ExitCode;
            process.Close();
            
            if (exitCode == 0)
            {
                Debug.Log("Android plugin built successfully!");
                SetupPlugin();
            }
            else
            {
                Debug.LogError($"Build failed with exit code {exitCode}");
            }
        }
        
        /// <summary>
        /// Get the root path of the package
        /// </summary>
        private static string GetPackageRootPath()
        {
            // Find this script's directory
            string[] guids = AssetDatabase.FindAssets("t:Script BleHidBuildHelper");
            if (guids.Length == 0)
            {
                Debug.LogError("Could not find BleHidBuildHelper script in project");
                return null;
            }
            
            string scriptPath = AssetDatabase.GUIDToAssetPath(guids[0]);
            string scriptDirectory = Path.GetDirectoryName(scriptPath);
            string packageRoot = Path.GetDirectoryName(scriptDirectory);
            
            return packageRoot;
        }
    }
}
