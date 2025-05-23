using System;
using System.Collections.Generic;
using UnityEngine;

namespace Inventonater
{
    public static class DeviceIcons
    {
        private static readonly Dictionary<string, Texture2D> _deviceIcons = new Dictionary<string, Texture2D>();
        private static readonly Dictionary<string, List<string>> _deviceKeywords = new Dictionary<string, List<string>>();
        
        public static readonly Texture2D DefaultIcon;
        
        static DeviceIcons()
        {
            // Initialize with default icons
            RegisterDeviceIcon("speaker", "DeviceIcons/speaker", Color.blue, new[] { "speaker", "audio" });
            RegisterDeviceIcon("lights", "DeviceIcons/lights", Color.yellow, new[] { "lights", "light", "lamp" });
            RegisterDeviceIcon("television", "DeviceIcons/television", Color.green, new[] { "television", "tv", "chrome", "screen", "display" });
            
            // Set default icon
            DefaultIcon = CreateSimpleTexture(Color.grey);
        }
        
        public static void RegisterDeviceIcon(string deviceType, string resourcePath, Color fallbackColor, string[] keywords)
        {
            Texture2D icon = LoadTextureOrFallback(resourcePath, fallbackColor);
            _deviceIcons[deviceType] = icon;
            
            List<string> keywordList = new List<string>(keywords);
            _deviceKeywords[deviceType] = keywordList;
        }
        
        public static Texture2D Get(string name)
        {
            if (string.IsNullOrEmpty(name))
                return DefaultIcon;
                
            string nameLower = name.ToLower();
            
            foreach (var deviceType in _deviceKeywords.Keys)
            {
                foreach (var keyword in _deviceKeywords[deviceType])
                {
                    if (nameLower.Contains(keyword))
                        return _deviceIcons[deviceType];
                }
            }
            
            return DefaultIcon;
        }
        
        public static Texture2D GetByType(string deviceType) => _deviceIcons.GetValueOrDefault(deviceType, DefaultIcon);

        private static Texture2D LoadTextureOrFallback(string resourcePath, Color fallbackColor)
        {
            Texture2D texture = Resources.Load<Texture2D>(resourcePath);
            if (texture != null) return texture;

            Debug.LogWarning($"Failed to load texture from {resourcePath}, using fallback color");
            return CreateSimpleTexture(fallbackColor);
        }

        private static Texture2D CreateSimpleTexture(Color color)
        {
            Texture2D texture = new Texture2D(64, 64);
            Color[] colors = new Color[64 * 64];

            for (int i = 0; i < colors.Length; i++) colors[i] = color;

            texture.SetPixels(colors);
            texture.Apply();
            return texture;
        }
    }
}
