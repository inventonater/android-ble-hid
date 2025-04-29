using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater.BleHid
{
    public class InputDeviceMappingUI : SectionUI
    {
        private readonly InputDeviceMapping _mapping;
        public override string TabName { get; }
        private readonly ActionRegistry _registry;
        
        // For adding new mappings
        private int _selectedEventIndex = 0;
        private int _selectedActionIndex = 0;
        private string[] _eventOptions;
        private string[] _actionOptions;
        private InputEvent[] _eventValues;
        private EInputAction[] _actionValues;
        
        // For dropdown UI
        private bool _showDropdown = false;
        private int _activeDropdownId = 0;
        
        public InputDeviceMappingUI(InputDeviceMapping mapping)
        {
            TabName = mapping.Name;
            _mapping = mapping;

            _registry = BleHidManager.Instance.BleBridge.ActionRegistry;
            
            // Initialize dropdown options
            InitializeDropdownOptions();
        }
        
        private void InitializeDropdownOptions()
        {
            // Get all input events
            _eventValues = InputEvent.GetAll().ToArray();
            _eventOptions = _eventValues.Select(e => e.ToString()).ToArray();
            
            // Get all input actions
            _actionValues = Enum.GetValues(typeof(EInputAction)).Cast<EInputAction>().ToArray();
            _actionOptions = _actionValues.Select(a => a.ToString()).ToArray();
        }

        public override void Update()
        {
        }
        
        private int Popup(int selectedIndex, string[] options)
        {
            // Simple implementation of a dropdown using buttons
            if (GUILayout.Button(options[selectedIndex], GUILayout.Width(200)))
            {
                // Toggle dropdown visibility
                _showDropdown = !_showDropdown;
                _activeDropdownId = _showDropdown ? options.GetHashCode() : 0;
            }
            
            if (_showDropdown && _activeDropdownId == options.GetHashCode())
            {
                GUILayout.BeginVertical("box");
                for (int i = 0; i < options.Length; i++)
                {
                    if (GUILayout.Button(options[i]))
                    {
                        selectedIndex = i;
                        _showDropdown = false;
                        _activeDropdownId = 0;
                    }
                }
                GUILayout.EndVertical();
            }
            
            return selectedIndex;
        }

        public override void DrawUI()
        {
            GUILayout.Label("Current Mappings", GUI.skin.label);
            
            // Display existing mappings with remove buttons
            foreach (var (inputEvent, actions) in _mapping.ButtonMapping)
            {
                GUILayout.BeginVertical("box");
                GUILayout.Label(inputEvent.ToString(), GUI.skin.label);
                
                foreach (var action in actions)
                {
                    GUILayout.BeginHorizontal();
                    if (_registry.TryGetInfo(action, out var info))
                    {
                        GUILayout.Label($"{info.DisplayName} - {info.Description}");
                    }
                    else
                    {
                        GUILayout.Label(action.ToString());
                    }
                    
                    if (GUILayout.Button("Remove", GUILayout.Width(80)))
                    {
                        _mapping.RemoveAction(inputEvent, action);
                    }
                    GUILayout.EndHorizontal();
                }
                
                GUILayout.EndVertical();
            }
            
            // Add new mapping section
            GUILayout.Space(20);
            GUILayout.Label("Add New Mapping", GUI.skin.label);
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("Input Event:", GUILayout.Width(100));
            _selectedEventIndex = Popup(_selectedEventIndex, _eventOptions);
            GUILayout.EndHorizontal();
            
            GUILayout.BeginHorizontal();
            GUILayout.Label("Action:", GUILayout.Width(100));
            _selectedActionIndex = Popup(_selectedActionIndex, _actionOptions);
            GUILayout.EndHorizontal();
            
            if (GUILayout.Button("Add Mapping"))
            {
                InputEvent selectedEvent = _eventValues[_selectedEventIndex];
                EInputAction selectedAction = _actionValues[_selectedActionIndex];
                
                // Add the new mapping
                _mapping.AddAction(selectedEvent, selectedAction);
            }
        }
    }
}
