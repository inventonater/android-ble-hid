# Unity-specific ProGuard rules

# Keep our own classes
-keep class com.example.blehid.plugin.** { *; }
-keep class com.example.blehid.core.** { *; }
-keep class com.example.blehid.unity.** { *; } 

# Remove Unity classes
-dontwarn com.unity3d.**
-keep class !com.unity3d.** { *; }

# Specifically remove UnityPlayer
-assumenosideeffects class com.unity3d.player.UnityPlayer {
    *;
}

# General Android rules
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
