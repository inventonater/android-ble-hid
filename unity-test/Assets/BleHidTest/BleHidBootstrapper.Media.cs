using System;
using UnityEngine;
using UnityEngine.UI;
using TMPro;

namespace BleHid
{
    public partial class BleHidBootstrapper
    {
        // Media control buttons
        private Button playPauseButton;
        private Button previousButton;
        private Button nextButton;
        private Button volumeUpButton;
        private Button volumeDownButton;
        private Button muteButton;

        private void CreateMediaPanelUI()
        {
            float buttonWidth = 300;
            float buttonHeight = 100;
            float buttonSpacing = 20;
            
            // Create media control buttons
            playPauseButton = CreateButton(mediaPanel.transform, "PlayPauseButton", new Vector2(0, 200), 
                new Vector2(buttonWidth, buttonHeight), "Play/Pause", OnPlayPauseClicked);
            
            previousButton = CreateButton(mediaPanel.transform, "PreviousButton", new Vector2(-buttonWidth - buttonSpacing, 200), 
                new Vector2(buttonWidth, buttonHeight), "Previous", OnPreviousClicked);
            
            nextButton = CreateButton(mediaPanel.transform, "NextButton", new Vector2(buttonWidth + buttonSpacing, 200), 
                new Vector2(buttonWidth, buttonHeight), "Next", OnNextClicked);
            
            volumeUpButton = CreateButton(mediaPanel.transform, "VolumeUpButton", new Vector2(0, 100), 
                new Vector2(buttonWidth, buttonHeight), "Volume Up", OnVolumeUpClicked);
            
            volumeDownButton = CreateButton(mediaPanel.transform, "VolumeDownButton", new Vector2(0, 0), 
                new Vector2(buttonWidth, buttonHeight), "Volume Down", OnVolumeDownClicked);
            
            muteButton = CreateButton(mediaPanel.transform, "MuteButton", new Vector2(0, -100), 
                new Vector2(buttonWidth, buttonHeight), "Mute", OnMuteClicked);
        }
        
        // Media control button handlers
        private void OnPlayPauseClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send play/pause: No device connected");
                return;
            }
            
            bleHidManager.PlayPause();
        }

        private void OnPreviousClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send previous: No device connected");
                return;
            }
            
            bleHidManager.PreviousTrack();
        }

        private void OnNextClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send next: No device connected");
                return;
            }
            
            bleHidManager.NextTrack();
        }

        private void OnVolumeUpClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send volume up: No device connected");
                return;
            }
            
            bleHidManager.VolumeUp();
        }

        private void OnVolumeDownClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send volume down: No device connected");
                return;
            }
            
            bleHidManager.VolumeDown();
        }

        private void OnMuteClicked()
        {
            if (!bleHidManager.IsConnected)
            {
                AddLogEntry("Cannot send mute: No device connected");
                return;
            }
            
            bleHidManager.Mute();
        }
    }
}
