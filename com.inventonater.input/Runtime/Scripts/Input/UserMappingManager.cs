using System.Collections.Generic;
using UnityEngine;

namespace Inventonater
{
    public static class UserMappingManager
    {
        private static readonly Dictionary<ButtonEvent, MappableActionId> _buttonToActionMap = new();
        
        public static void SetMapping(ButtonEvent buttonId, MappableActionId actionId)
        {
            _buttonToActionMap[buttonId] = actionId;
            LoggingManager.Instance.Log($"UserMappingManager: Mapped button {buttonId} to action {actionId}");
        }

        public static bool TryGetActionForButton(ButtonEvent buttonId, out MappableActionId actionId) => _buttonToActionMap.TryGetValue(buttonId, out actionId);

        public static bool RemoveMapping(ButtonEvent buttonId)
        {
            if (!_buttonToActionMap.Remove(buttonId)) return false;
            LoggingManager.Instance.Log($"UserMappingManager: Removed mapping for button {buttonId}");
            return true;
        }
        
        public static void SaveMappings()
        {
            // Convert mappings to JSON
            var mappingsJson = JsonUtility.ToJson(new SerializableMappings(_buttonToActionMap));
            
            // Save to PlayerPrefs
            PlayerPrefs.SetString("UserMappings", mappingsJson);
            PlayerPrefs.Save();
            
            LoggingManager.Instance.Log("UserMappingManager: Saved mappings to PlayerPrefs");
        }
        
        public static void LoadMappings()
        {
            if (PlayerPrefs.HasKey("UserMappings"))
            {
                var mappingsJson = PlayerPrefs.GetString("UserMappings");
                var mappings = JsonUtility.FromJson<SerializableMappings>(mappingsJson);
                
                _buttonToActionMap.Clear();
                foreach (var mapping in mappings.Mappings)
                {
                    _buttonToActionMap[mapping.ButtonId] = mapping.ActionId;
                }
                
                LoggingManager.Instance.Log($"UserMappingManager: Loaded {_buttonToActionMap.Count} mappings from PlayerPrefs");
            }
            else
            {
                LoggingManager.Instance.Log("UserMappingManager: No saved mappings found in PlayerPrefs");
            }
        }
        
        // Helper class for serialization
        [System.Serializable]
        private class SerializableMappings
        {
            public List<SerializableMapping> Mappings = new();
            
            public SerializableMappings() { }
            
            public SerializableMappings(Dictionary<ButtonEvent, MappableActionId> mappings)
            {
                foreach (var kvp in mappings)
                {
                    Mappings.Add(new SerializableMapping { ButtonId = kvp.Key, ActionId = kvp.Value });
                }
            }
        }
        
        [System.Serializable]
        private class SerializableMapping
        {
            public ButtonEvent ButtonId;
            public MappableActionId ActionId;
        }
    }
}
