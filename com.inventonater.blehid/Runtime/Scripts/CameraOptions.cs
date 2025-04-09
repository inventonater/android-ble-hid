using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Options for camera picture capture operations.
    /// This class provides a clean way to configure camera operations with sensible defaults.
    /// </summary>
    public class CameraOptions
    {
        /// <summary>
        /// Delay before tapping the shutter button (in milliseconds)
        /// </summary>
        public int TapDelay { get; set; } = 0;
        
        /// <summary>
        /// Delay before returning to the app (in milliseconds)
        /// </summary>
        public int ReturnDelay { get; set; } = 0;
        
        /// <summary>
        /// X position of shutter button as a ratio (0.0-1.0)
        /// </summary>
        public float ButtonX { get; set; } = 0.5f;
        
        /// <summary>
        /// Y position of shutter button as a ratio (0.0-1.0)
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
        /// Create a new CameraOptions instance with default values
        /// </summary>
        public CameraOptions()
        {
            // Default constructor with default values
        }
        
        /// <summary>
        /// Convert to Android object for use with bridge
        /// </summary>
        internal AndroidJavaObject ToAndroidObject()
        {
            using (AndroidJavaObject cameraParams = new AndroidJavaObject("android.os.Bundle"))
            {
                if (TapDelay > 0)
                    cameraParams.Call("putInt", BleHidConstants.OptionsParams.TapDelay, TapDelay);
                    
                if (ReturnDelay > 0)
                    cameraParams.Call("putInt", BleHidConstants.OptionsParams.ReturnDelay, ReturnDelay);
                    
                if (ButtonX != 0.5f)
                    cameraParams.Call("putFloat", BleHidConstants.OptionsParams.ButtonX, ButtonX);
                    
                if (ButtonY != 0.8f)
                    cameraParams.Call("putFloat", BleHidConstants.OptionsParams.ButtonY, ButtonY);
                    
                if (AcceptDialogDelay != 300)
                    cameraParams.Call("putInt", BleHidConstants.OptionsParams.AcceptDialogDelay, AcceptDialogDelay);
                    
                if (AcceptXOffset != 0.2f)
                    cameraParams.Call("putFloat", BleHidConstants.OptionsParams.AcceptXOffset, AcceptXOffset);
                    
                if (AcceptYOffset != 0.05f)
                    cameraParams.Call("putFloat", BleHidConstants.OptionsParams.AcceptYOffset, AcceptYOffset);
                
                return cameraParams;
            }
        }
    }
}
