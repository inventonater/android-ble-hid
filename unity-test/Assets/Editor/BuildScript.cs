using UnityEditor;
using UnityEngine;
using System.IO;
using UnityEditor.Build;
using UnityEditor.Build.Reporting;

public class Build
{
    public static void PerformBuild()
    {
        Debug.Log("Starting Android build...");

        string[] scenes = { "Assets/BleHidTest/BleHidSimpleScene.unity" };
        string buildPath = Path.Combine(Application.dataPath, "../Build/BleHidTest.apk");

        // Make sure the Build directory exists
        Directory.CreateDirectory(Path.GetDirectoryName(buildPath));

        // Build options
        BuildPlayerOptions buildPlayerOptions = new BuildPlayerOptions
        {
            scenes = scenes,
            locationPathName = buildPath,
            target = BuildTarget.Android,
            options = BuildOptions.Development
        };

        // Perform the build
        BuildReport report = BuildPipeline.BuildPlayer(buildPlayerOptions);
        BuildSummary summary = report.summary;

        if (summary.result == BuildResult.Succeeded)
        {
            Debug.Log("Build succeeded: " + buildPath);
        }
        else
        {
            Debug.Log("Build failed");
        }
    }
}
