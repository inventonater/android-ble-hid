using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;

namespace Inventonater
{
    public class InputDeviceMappingUI : SectionUI
    {
        private readonly InputBinding _binding;
        public override string TabName { get; }

        // For adding new mappings
        private int _selectedEventIndex = 0;
        private int _selectedActionIndex = 0;
        private string[] _eventOptions;
        private string[] _actionOptions;
        private ButtonEvent[] _eventValues;
        private MappableActionId[] _actionValues;
        
        // For dropdown UI
        private bool _showDropdown = false;
        private int _activeDropdownId = 0;
        
        public InputDeviceMappingUI(InputBinding binding)
        {
            TabName = binding.Name;
            _binding = binding;

            // Initialize dropdown options
            InitializeDropdownOptions();
        }
        
        private void InitializeDropdownOptions()
        {
            // Get all input events
            _eventValues = ButtonEvent.GetAll().ToArray();
            _eventOptions = _eventValues.Select(e => e.ToString()).ToArray();
            
            // Get all input actions
            _actionValues = Enum.GetValues(typeof(MappableActionId)).Cast<MappableActionId>().ToArray();
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
            var buttonMapEntries = _binding.Map.Buttons;
            foreach (var entry in buttonMapEntries)
            {
                var inputEvent = entry.ButtonEvent;
                var actionId = entry.ActionId;

                GUILayout.BeginVertical("box");
                GUILayout.Label(inputEvent.ToString(), GUI.skin.label);
                
                GUILayout.BeginHorizontal();
                if (_binding.Registry.TryGetMappedAction(actionId, out MappedActions mappedActions))
                {
                    foreach (var mappedAction in mappedActions.ActionInfos) GUILayout.Label($"{mappedAction.DisplayName} - {mappedAction.Description}");
                }
                else
                {
                    GUILayout.Label(actionId.ToString());
                }

                if (GUILayout.Button("Remove", GUILayout.Width(80)))
                {
                    _binding.Map.Remove(entry);
                }
                GUILayout.EndHorizontal();

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
                ButtonEvent selectedEvent = _eventValues[_selectedEventIndex];
                MappableActionId selectedAction = _actionValues[_selectedActionIndex];
                
                // Add the new mapping
                _binding.Add(selectedEvent, selectedAction);
            }
        }
    }
}
