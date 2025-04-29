using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Manages user-defined mappings between input buttons and actions.
    /// </summary>
    public static class UserMappingManager
    {
        private static readonly Dictionary<string, string> _buttonToActionMap = new Dictionary<string, string>();
        
        /// <summary>
        /// Sets a mapping between a button ID and an action ID.
        /// </summary>
        /// <param name="buttonId">The ID of the button to map</param>
        /// <param name="actionId">The ID of the action to map to the button</param>
        public static void SetMapping(string buttonId, string actionId)
        {
            _buttonToActionMap[buttonId] = actionId;
            LoggingManager.Instance.Log($"UserMappingManager: Mapped button {buttonId} to action {actionId}");
        }
        
        /// <summary>
        /// Tries to get the action ID mapped to a button ID.
        /// </summary>
        /// <param name="buttonId">The ID of the button to get the mapping for</param>
        /// <param name="actionId">The ID of the mapped action, if found</param>
        /// <returns>True if a mapping was found, false otherwise</returns>
        public static bool TryGetActionForButton(string buttonId, out string actionId)
        {
            return _buttonToActionMap.TryGetValue(buttonId, out actionId);
        }
        
        /// <summary>
        /// Removes a mapping for a button ID.
        /// </summary>
        /// <param name="buttonId">The ID of the button to remove the mapping for</param>
        /// <returns>True if a mapping was removed, false otherwise</returns>
        public static bool RemoveMapping(string buttonId)
        {
            if (_buttonToActionMap.ContainsKey(buttonId))
            {
                _buttonToActionMap.Remove(buttonId);
                LoggingManager.Instance.Log($"UserMappingManager: Removed mapping for button {buttonId}");
                return true;
            }
            return false;
        }
        
        /// <summary>
        /// Clears all mappings.
        /// </summary>
        public static void ClearMappings()
        {
            _buttonToActionMap.Clear();
            LoggingManager.Instance.Log("UserMappingManager: Cleared all mappings");
        }
        
        /// <summary>
        /// Gets all current mappings.
        /// </summary>
        /// <returns>A dictionary of button IDs to action IDs</returns>
        public static Dictionary<string, string> GetAllMappings()
        {
            return new Dictionary<string, string>(_buttonToActionMap);
        }
        
        /// <summary>
        /// Saves the current mappings to PlayerPrefs.
        /// </summary>
        public static void SaveMappings()
        {
            // Convert mappings to JSON
            var mappingsJson = JsonUtility.ToJson(new SerializableMappings(_buttonToActionMap));
            
            // Save to PlayerPrefs
            PlayerPrefs.SetString("UserMappings", mappingsJson);
            PlayerPrefs.Save();
            
            LoggingManager.Instance.Log("UserMappingManager: Saved mappings to PlayerPrefs");
        }
        
        /// <summary>
        /// Loads mappings from PlayerPrefs.
        /// </summary>
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
            public List<SerializableMapping> Mappings = new List<SerializableMapping>();
            
            public SerializableMappings() { }
            
            public SerializableMappings(Dictionary<string, string> mappings)
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
            public string ButtonId;
            public string ActionId;
        }
    }
}
