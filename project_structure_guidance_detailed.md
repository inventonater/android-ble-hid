Below is a detailed specification you can give to your engineers on how to organize and set up the development environment to ensure the **core Java capabilities** are properly exposed as a **Unity plugin**, while **maintaining** the Unity project’s configuration in good working order. The approach uses a **modular Gradle project** in Android Studio (or a Gradle-compatible environment) and then packaging an AAR plugin that Unity can import. The instructions specifically reference the AndroidManifest.xml snippet you provided (which resides in `Assets/Plugins/Android/AndroidManifest.xml` in the Unity project), though it can also be embedded in the AAR if you prefer. 

---

## 1. Overview of the Setup

We will create a multi-module Gradle project with:
1. **Core Module** – a pure Java (or Android library) module, containing your main logic.  
2. **Unity Plugin Module** – an Android library module that wraps your core module for Unity.  
3. **Test App Module** (optional) – a regular Android app (for verifying the plugin’s or core’s functionality in a non-Unity context).

The Unity side will then import your final plugin (AAR + any additional dependencies) into the `Assets/Plugins/Android` folder. 

Your `Assets/Plugins/Android/AndroidManifest.xml` will remain the “umbrella” manifest for the entire Unity project. As you can see, it includes standard Unity player activity definitions plus relevant Bluetooth permissions/features. **Because of the new Android BLE permission changes in API level 31+,** you have the `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, and `BLUETOOTH_CONNECT` permissions. The `<activity>` elements are placeholders for either `UnityPlayerActivity` or `UnityPlayerGameActivity`.

---

## 2. Directory Layout

A typical layout for the Android Studio project:

```plaintext
MyPluginProject/
├── settings.gradle               # includes all submodules
├── build.gradle                  # top-level build config
├── core/
│   ├── build.gradle             # config for core logic
│   └── src/
│       └── main/
│           └── java/
│               └── com/yourcompany/core/...
├── unity-plugin/
│   ├── build.gradle             # config for Unity plugin module
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml (optional - see below)
│           └── java/
│               └── com/yourcompany/unityplugin/...
└── testapp/                      # optional test app module
    ├── build.gradle
    └── src/
        └── main/
            ├── AndroidManifest.xml
            └── java/
                └── com/yourcompany/testapp/...
```

- The **`core/`** module has the pure Java or minimal Android code containing your logic.  
- The **`unity-plugin/`** module is an **Android Library** (`com.android.library`) that depends on the `core/` module. It will produce an AAR that can be dropped into Unity.  
- The **`testapp/`** module is for testing outside of Unity.  

**Note:** In this approach, your existing `Assets/Plugins/Android/AndroidManifest.xml` remains in the Unity project. You can decide if you want to keep it there or let the plugin’s AAR provide its own manifest. Both are valid. Since you said “hardcoding the `Assets/Plugins/Android/AndroidManifest.xml` is totally fine,” you can keep it as is, and your library modules won’t need to replicate it.

---

## 3. Detailed Steps

### 3.1. Core Module Setup

1. **Create the core module** (e.g., `core`). You can choose to make it:
   - A **plain Java library** (`apply plugin: 'java-library'`), if you only use standard Java classes.  
   - An **Android library** (`apply plugin: 'com.android.library'`), if your code references Android APIs.

2. **Add your Java sources** under `core/src/main/java/com/yourcompany/core/...`.
3. If it’s an **Android library**, you’d have a minimal `AndroidManifest.xml` under `core/src/main/AndroidManifest.xml`. Typically, you keep this empty or minimal (no `<application>` tag) so it doesn’t create conflicts. For a pure Java library, no manifest is needed.

4. **Gradle dependencies**: In `core/build.gradle`, just specify any dependencies you need. For instance:
   ```groovy
   plugins {
       id 'com.android.library'
   }

   android {
       compileSdkVersion 33
       // No defaultConfig if you want to keep minSdk flexible or match Unity 
   }

   dependencies {
       // Only add what's needed for the core logic
   }
   ```
5. Verify you can run a **unit test** (if any) in this module to confirm your logic is correct.

### 3.2. Unity Plugin Module Setup

1. **Create the unity-plugin module** as an Android library. In your `unity-plugin/build.gradle`, you’ll have:
   ```groovy
   plugins {
       id 'com.android.library'
   }

   android {
       compileSdkVersion 33
       
       defaultConfig {
           // minSdkVersion 19 or whatever Unity requires
           // targetSdkVersion 33
       }

       // If you want to override or tweak merging behavior, you can add it here
   }

   dependencies {
       implementation project(':core') // Link to your core library
       // Other dependencies if your plugin needs them
   }
   ```

2. **In `unity-plugin/src/main/java/...`:** write your Unity plugin wrapper classes. These classes typically have static methods or singletons that Unity calls via the C# layer using `AndroidJavaClass` / `AndroidJavaObject`.
   - Example:
     ```java
     package com.yourcompany.unityplugin;

     public class UnityPluginBridge {
         public static void doSomething() {
             // calls into core logic
             com.yourcompany.core.YourCoreClass.someMethod();
         }
     }
     ```
3. **Manifest**: If your plugin needs specific permissions or declares services/receivers, you can create or modify `unity-plugin/src/main/AndroidManifest.xml`. However, since you already have a full `AndroidManifest.xml` in the Unity project (`Assets/Plugins/Android/AndroidManifest.xml`), you might not need to maintain a separate one here. 
   - If you do add a manifest, do **not** override `<application>` or `<uses-sdk>` to avoid conflicts with Unity. Only declare the minimal pieces your plugin absolutely needs (like custom permissions if relevant). 
   - If you have no custom plugin-level manifest requirements, you can omit it entirely and rely on Unity’s main manifest in the `Assets/Plugins/Android` folder.

4. **Build the unity-plugin**. This will produce an AAR file in something like `unity-plugin/build/outputs/aar/unity-plugin-release.aar`. That file is what you will import into Unity.

### 3.3. (Optional) Test App Module Setup

1. **Create the test app** module (e.g., `testapp`) as a normal **Android application** (`apply plugin: 'com.android.application'`).
2. Add dependencies:
   ```groovy
   dependencies {
       implementation project(':core')
       implementation project(':unity-plugin')
       // or just project(':core') if you only want to test the core
   }
   ```
3. **Test** that you can invoke your plugin (or core) logic from a normal Android activity. This verifies everything is wired properly outside of Unity.

### 3.4. Gradle Settings

In your top-level `settings.gradle`, you’ll include all modules:
```groovy
include ':core', ':unity-plugin', ':testapp'
```

In your top-level `build.gradle`, you can define your repositories (mavenCentral, google) and plugin versions. Usually it looks like:
```groovy
buildscript {
   repositories {
       google()
       mavenCentral()
   }
   dependencies {
       classpath 'com.android.tools.build:gradle:8.0.1'
   }
}

allprojects {
   repositories {
       google()
       mavenCentral()
   }
}
```

---

## 4. Building and Exporting the Unity Plugin

After implementing your code, do the following to create the AAR:

1. **Open a terminal** in the project root (where `gradlew` resides).
2. **Build** the plugin module in release mode:
   ```bash
   ./gradlew :unity-plugin:assembleRelease
   ```
3. The resulting AAR is typically found at:
   ```
   unity-plugin/build/outputs/aar/unity-plugin-release.aar
   ```

If your `core` module is a pure Java library, you can similarly build it with:
```bash
./gradlew :core:build
```
This yields a `core.jar` or `core-debug.jar` in `core/build/libs/`.

**Note**: You can choose to bundle `core` into the plugin’s AAR if the Gradle script is set up that way. By default, an Android library references the `core` library as a dependency, so the final AAR should contain the compiled classes from `core`. Verify that your final AAR indeed includes them (you can unzip the AAR and see if the classes from `core` are present). If not, you can either:
- Place the `core.jar` in Unity’s `Assets/Plugins/Android` folder alongside the AAR; or
- Modify the plugin’s Gradle config to embed the core library’s classes.  

For simplicity, we often just keep the code in one AAR. In that case, one file (the AAR) is all you add to Unity.

---

## 5. Integrating the Plugin into Unity

In your **Unity project**:

1. **Create or use** the folder `Assets/Plugins/Android/`.  
2. Copy the resulting `unity-plugin-release.aar` (and, if necessary, `core.jar`) into that folder.
3. **AndroidManifest**: You have a complete manifest in `Assets/Plugins/Android/AndroidManifest.xml`, which includes the Unity player activities plus permissions for Bluetooth. 
   - That means your plugin’s final AndroidManifest is effectively a **merged** version of the Unity-provided one plus the plugin’s (if the plugin has one). 
   - If you do not have an AndroidManifest in the plugin AAR, then no merge conflict arises. 
   - If you do have a plugin AndroidManifest (with any additional features/services), then Unity’s Gradle build merges them. 
4. **Check Inspector**: In the Unity Editor, select your `unity-plugin-release.aar` file and ensure it’s marked for **Android**.  
5. **Implementation**: In your C# scripts, call your plugin code via `AndroidJavaClass` or `AndroidJavaObject`. For example:
   ```csharp
   using UnityEngine;

   public class ExampleUsage : MonoBehaviour {
       void Start() {
           #if UNITY_ANDROID && !UNITY_EDITOR
           AndroidJavaClass pluginClass = new AndroidJavaClass("com.yourcompany.unityplugin.UnityPluginBridge");
           pluginClass.CallStatic("doSomething");
           #endif
       }
   }
   ```
6. **Build** your Unity project for Android. Unity will run a Gradle build, merging your manifest (`Assets/Plugins/Android/AndroidManifest.xml`) with any plugin manifest. The final `.apk` (or `.aab`) will contain the compiled code from the plugin plus the correct manifest permissions and activities.

7. **Verification**: 
   - Check **`Temp/gradleOut/launcher/build/outputs/logs/manifest-merger-*.txt`** or the final merged `AndroidManifest.xml` in the build folder to see the merged result. 
   - You can also use **Android Studio’s** APK Analyzer or `aapt dump badging <APK path>` to confirm the final permissions.

---

## 6. Maintenance and Best Practices

1. **Keep the `AndroidManifest.xml` in Unity minimal** – only declare the Unity Player activities, your required permissions/features (e.g., BLE), and any hardware features. This is what you currently have, and it should be sufficient.  
2. **Avoid specifying `<uses-sdk>`** in library manifests. Let Unity control the minSdk and targetSdk.  
3. **Test** your plugin logic in the optional `testapp` module first to confirm correctness outside of Unity.  
4. **Document** your plugin API for the Unity team: which Java methods to call, what parameters are needed, etc.  
5. **Watch out** for any references to `UnityPlayer.currentActivity` in your plugin Java code. Typically, you pass the Activity from C# to Java if needed. Don’t rely on the plugin automatically referencing `com.unity3d.player.UnityPlayer` unless you want a direct Unity dependency in Java.  
6. If you want to handle advanced Android features like Services, Broadcast Receivers, or custom Activities, keep them in the plugin’s `AndroidManifest.xml`. Mark them with your plugin package name. This avoids collisions with Unity or other plugins.  
7. Keep an eye on **Android version changes**: BLE permissions changed significantly in Android 12 (API 31). The usage of `android:maxSdkVersion="30"` for BLUETOOTH/BLE might be valid or might need updating as you support new OS versions.  
8. If you ever need a custom application class in your plugin, you’ll have to add `tools:replace="android:name"` in the Unity manifest or your plugin’s manifest. This is advanced usage – typically you avoid it if possible.  

---

## 7. Summary for Engineers

**Checklist** for your dev team:

1. **Create** a multi-module Gradle project:
   - `core` (Java or Android library)  
   - `unity-plugin` (Android library, depends on `core`)  
   - (Optional) `testapp` (Android application)  
2. **Implement** your core logic in `core`.  
3. **Implement** Unity-specific bridging code in `unity-plugin`, referencing `core`.  
4. **Build** the `unity-plugin` module to produce an AAR.  
5. **Place** the AAR (and `core.jar` if needed) into `Assets/Plugins/Android/` in the Unity project.  
6. **Use** the existing `AndroidManifest.xml` in `Assets/Plugins/Android/` to declare required permissions, activities, etc.  
7. **Invoke** the plugin from C# scripts via the AndroidJava* APIs.  
8. **Test** by building a debug APK from Unity and verifying the plugin is functional.  

By following these steps, you’ll ensure:
- The **Java capabilities** are properly exposed to Unity.  
- The **Unity project configuration** (particularly the manifest, permissions, BLE usage) remains intact and is recognized in final builds.  
- The codebase is modular, maintainable, and easy to test.

That’s the recommended environment setup. Once you have it in place, future changes to your Java code or plugin structure will be straightforward, as you can easily rebuild and drop in the updated AAR for Unity.