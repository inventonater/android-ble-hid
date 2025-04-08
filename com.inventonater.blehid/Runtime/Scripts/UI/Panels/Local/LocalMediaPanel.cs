using System;
using UnityEngine;

namespace Inventonater.BleHid.UI.Panels.Local
{
    /// <summary>
    /// Panel for local media control functionality.
    /// Supports common media playback controls like play/pause, next/previous, 
    /// volume adjustments, and app-specific controls.
    /// </summary>
    public class LocalMediaPanel : BaseBleHidPanel
    {
        private BleHidLocalControl localControl;
        private string[] appOptions = new string[] { "General", "YouTube", "Spotify", "Netflix", "Audible" };
        private int selectedAppIndex = 0;
        
        public override bool RequiresConnectedDevice => false;
        
        public override void Initialize(BleHidManager manager)
        {
            base.Initialize(manager);
            
            // Get reference to the local control instance
            try
            {
                localControl = BleHidLocalControl.Instance;
            }
            catch (Exception e)
            {
                logger.LogError($"Failed to get LocalControl instance: {e.Message}");
            }
        }
        
        protected override void DrawPanelContent()
        {
            GUILayout.Label("Media Controls", titleStyle);
            
            #if UNITY_EDITOR
            bool isEditorMode = true;
            #else
            bool isEditorMode = false;
            #endif
            
            // App selector
            GUILayout.BeginHorizontal();
            GUILayout.Label("App:", GUILayout.Width(60));
            selectedAppIndex = GUILayout.Toolbar(selectedAppIndex, appOptions, GUILayout.Height(40));
            GUILayout.EndHorizontal();
            
            GUILayout.Space(10);
            
            // Basic media controls section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Playback Controls", subtitleStyle);
            
            // Media controls row 1
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Previous", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Previous track pressed");
                }
                else if (localControl != null)
                {
                    localControl.PreviousTrack();
                    logger.Log("Sent previous track command");
                }
            }
            
            if (GUILayout.Button("Play/Pause", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Play/Pause pressed");
                }
                else if (localControl != null)
                {
                    localControl.PlayPause();
                    logger.Log("Sent play/pause command");
                }
            }
            
            if (GUILayout.Button("Next", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Next track pressed");
                }
                else if (localControl != null)
                {
                    localControl.NextTrack();
                    logger.Log("Sent next track command");
                }
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // Volume controls section
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label("Volume Controls", subtitleStyle);
            
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Volume -", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Volume down pressed");
                }
                else if (localControl != null)
                {
                    localControl.VolumeDown();
                    logger.Log("Sent volume down command");
                }
            }
            
            if (GUILayout.Button("Mute", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Mute pressed");
                }
                else if (localControl != null)
                {
                    localControl.Mute();
                    logger.Log("Sent mute command");
                }
            }
            
            if (GUILayout.Button("Volume +", GUILayout.Height(60)))
            {
                if (isEditorMode)
                {
                    logger.Log("Volume up pressed");
                }
                else if (localControl != null)
                {
                    localControl.VolumeUp();
                    logger.Log("Sent volume up command");
                }
            }
            GUILayout.EndHorizontal();
            
            GUILayout.EndVertical();
            
            // App-specific controls
            DrawAppSpecificControls();
        }
        
        /// <summary>
        /// Draw controls specific to the selected app.
        /// </summary>
        private void DrawAppSpecificControls()
        {
            // Only show app-specific controls if not "General"
            if (selectedAppIndex == 0) return;
            
            GUILayout.BeginVertical(GUI.skin.box);
            GUILayout.Label($"{appOptions[selectedAppIndex]} Controls", subtitleStyle);
            
            switch (selectedAppIndex)
            {
                case 1: // YouTube
                    DrawYouTubeControls();
                    break;
                case 2: // Spotify
                    DrawSpotifyControls();
                    break;
                case 3: // Netflix
                    DrawNetflixControls();
                    break;
                case 4: // Audible
                    DrawAudibleControls();
                    break;
            }
            
            GUILayout.EndVertical();
        }
        
        private void DrawYouTubeControls()
        {
            #if UNITY_EDITOR
            bool isEditorMode = true;
            #else
            bool isEditorMode = false;
            #endif
            
            // Row 1: Skip Controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Skip Back 10s", GUILayout.Height(50)))
            {
                // This would require custom implementation in LocalInputController
                // For now, just log
                logger.Log("YouTube: Skip back 10s (not implemented)");
            }
            
            if (GUILayout.Button("Skip Forward 10s", GUILayout.Height(50)))
            {
                // This would require custom implementation in LocalInputController
                // For now, just log
                logger.Log("YouTube: Skip forward 10s (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            // Row 2: Additional Controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Fullscreen", GUILayout.Height(50)))
            {
                logger.Log("YouTube: Toggle fullscreen (not implemented)");
            }
            
            if (GUILayout.Button("Captions", GUILayout.Height(50)))
            {
                logger.Log("YouTube: Toggle captions (not implemented)");
            }
            GUILayout.EndHorizontal();
        }
        
        private void DrawSpotifyControls()
        {
            // Row 1: Playlist Controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Shuffle", GUILayout.Height(50)))
            {
                logger.Log("Spotify: Toggle shuffle (not implemented)");
            }
            
            if (GUILayout.Button("Repeat", GUILayout.Height(50)))
            {
                logger.Log("Spotify: Toggle repeat (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            // Row 2: Additional Controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Add to Library", GUILayout.Height(50)))
            {
                logger.Log("Spotify: Add to library (not implemented)");
            }
            
            if (GUILayout.Button("Create Playlist", GUILayout.Height(50)))
            {
                logger.Log("Spotify: Create playlist (not implemented)");
            }
            GUILayout.EndHorizontal();
        }
        
        private void DrawNetflixControls()
        {
            // Row 1: Playback Controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Skip Intro", GUILayout.Height(50)))
            {
                logger.Log("Netflix: Skip intro (not implemented)");
            }
            
            if (GUILayout.Button("Next Episode", GUILayout.Height(50)))
            {
                logger.Log("Netflix: Next episode (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            // Row 2: Additional Controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Fullscreen", GUILayout.Height(50)))
            {
                logger.Log("Netflix: Toggle fullscreen (not implemented)");
            }
            
            if (GUILayout.Button("Subtitles", GUILayout.Height(50)))
            {
                logger.Log("Netflix: Toggle subtitles (not implemented)");
            }
            GUILayout.EndHorizontal();
        }
        
        private void DrawAudibleControls()
        {
            // Row 1: Navigation Controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Skip Back 30s", GUILayout.Height(50)))
            {
                logger.Log("Audible: Skip back 30s (not implemented)");
            }
            
            if (GUILayout.Button("Skip Forward 30s", GUILayout.Height(50)))
            {
                logger.Log("Audible: Skip forward 30s (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            // Row 2: Speed Controls
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Speed 0.75x", GUILayout.Height(50)))
            {
                logger.Log("Audible: Speed 0.75x (not implemented)");
            }
            
            if (GUILayout.Button("Speed 1.0x", GUILayout.Height(50)))
            {
                logger.Log("Audible: Speed 1.0x (not implemented)");
            }
            
            if (GUILayout.Button("Speed 1.5x", GUILayout.Height(50)))
            {
                logger.Log("Audible: Speed 1.5x (not implemented)");
            }
            GUILayout.EndHorizontal();
            
            // Row 3: Bookmarks
            GUILayout.BeginHorizontal();
            if (GUILayout.Button("Add Bookmark", GUILayout.Height(50)))
            {
                logger.Log("Audible: Add bookmark (not implemented)");
            }
            GUILayout.EndHorizontal();
        }
    }
}
