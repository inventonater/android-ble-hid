using UnityEngine;

namespace Inventonater
{
    /// <summary>
    /// Options for video recording operations.
    /// This class provides a clean way to configure video operations with sensible defaults.
    /// </summary>
    public class VideoOptions
    {
        /// <summary>
        /// Duration to record in seconds
        /// </summary>
        public float Duration { get; set; } = 5.0f;
        
        /// <summary>
        /// Delay before tapping the record button (in milliseconds)
        /// </summary>
        public int TapDelay { get; set; } = 0;
        
        /// <summary>
        /// Delay before returning to the app (in milliseconds)
        /// </summary>
        public int ReturnDelay { get; set; } = 0;
        
        /// <summary>
        /// X position of record button as a ratio (0.0-1.0)
        /// </summary>
        public float ButtonX { get; set; } = 0.5f;
        
        /// <summary>
        /// Y position of record button as a ratio (0.0-1.0)
        /// </summary>
        public float ButtonY { get; set; } = 0.8f;
        
        /// <summary>
        /// Delay before tapping the accept dialog button (in milliseconds)
        /// </summary>
        public int AcceptDialogDelay { get; set; } = 300;
        
        /// <summary>
        /// X offset from center for accept button (0.0-1.0)
        /// </summary>
        public float AcceptXOffset { get; set; } = 0.2f;
        
        /// <summary>
        /// Y offset from center for accept button (0.0-1.0)
        /// </summary>
        public float AcceptYOffset { get; set; } = 0.05f;
        
        /// <summary>
        /// Create a new VideoOptions instance with default values
        /// </summary>
        public VideoOptions()
        {
            // Default constructor with default values
        }
        
        /// <summary>
        /// Create VideoOptions with specified duration
        /// </summary>
        /// <param name="durationSeconds">Duration to record in seconds</param>
        public VideoOptions(float durationSeconds)
        {
            Duration = durationSeconds;
        }
        
        /// <summary>
        /// Convert to Android object for use with bridge
        /// </summary>
        internal AndroidJavaObject ToAndroidObject()
        {
            using (AndroidJavaObject videoParams = new AndroidJavaObject("android.os.Bundle"))
            {
                // Always add duration
                videoParams.Call("putLong", BleHidConstants.OptionsParams.VideoDuration, (long)(Duration * 1000));
                
                if (TapDelay > 0)
                    videoParams.Call("putInt", BleHidConstants.OptionsParams.TapDelay, TapDelay);
                    
                if (ReturnDelay > 0)
                    videoParams.Call("putInt", BleHidConstants.OptionsParams.ReturnDelay, ReturnDelay);
                    
                if (ButtonX != 0.5f)
                    videoParams.Call("putFloat", BleHidConstants.OptionsParams.ButtonX, ButtonX);
                    
                if (ButtonY != 0.8f)
                    videoParams.Call("putFloat", BleHidConstants.OptionsParams.ButtonY, ButtonY);
                    
                if (AcceptDialogDelay != 300)
                    videoParams.Call("putInt", BleHidConstants.OptionsParams.AcceptDialogDelay, AcceptDialogDelay);
                    
                if (AcceptXOffset != 0.2f)
                    videoParams.Call("putFloat", BleHidConstants.OptionsParams.AcceptXOffset, AcceptXOffset);
                    
                if (AcceptYOffset != 0.05f)
                    videoParams.Call("putFloat", BleHidConstants.OptionsParams.AcceptYOffset, AcceptYOffset);
                
                return videoParams;
            }
        }
    }
}
