using UnityEngine;

namespace Inventonater.BleHid.UI.Filters
{
    /// <summary>
    /// Implementation of the 1€ Filter for mouse pointer smoothing
    /// Original algorithm: https://cristal.univ-lille.fr/~casiez/1euro/
    /// </summary>
    public class OneEuroFilter : IInputFilter
    {
        // Filter parameters
        private float mincutoff;    // Minimum cutoff frequency
        private float beta;         // Cutoff slope (speed coefficient)
        private float dcutoff;      // Derivative cutoff frequency
        
        // Filter state
        private Vector2 x_prev;     // Previous filtered position vector
        private Vector2 dx_prev;    // Previous derivative vector
        private float lastTime;     // Last update timestamp
        private bool initialized;   // Whether filter has been initialized
        
        /// <summary>
        /// Display name of the filter for UI
        /// </summary>
        public string Name => "1€ Filter";
        
        /// <summary>
        /// Brief description of how the filter works
        /// </summary>
        public string Description => "Adaptive filter that adjusts smoothing based on movement speed";
        
        /// <summary>
        /// Creates a new instance of the 1€ Filter
        /// </summary>
        /// <param name="mincutoff">Minimum cutoff frequency (default: 1.0)</param>
        /// <param name="beta">Beta parameter for speed coefficient (default: 0.007)</param>
        /// <param name="dcutoff">Derivative cutoff frequency (default: 1.0)</param>
        public OneEuroFilter(float mincutoff = 1.0f, float beta = 0.007f, float dcutoff = 1.0f)
        {
            this.mincutoff = mincutoff;
            this.beta = beta;
            this.dcutoff = dcutoff;
            Reset();
        }
        
        /// <summary>
        /// Reset filter state
        /// </summary>
        public void Reset()
        {
            initialized = false;
            x_prev = Vector2.zero;
            dx_prev = Vector2.zero;
            lastTime = 0;
        }
        
        /// <summary>
        /// Update filter parameters
        /// </summary>
        /// <param name="mincutoff">Minimum cutoff frequency</param>
        /// <param name="beta">Beta parameter (speed coefficient)</param>
        public void SetParameters(float mincutoff, float beta)
        {
            this.mincutoff = mincutoff;
            this.beta = beta;
        }
        
        /// <summary>
        /// Low-pass filter
        /// </summary>
        private float LowPassFilter(float x, float alpha, float y_prev)
        {
            return alpha * x + (1.0f - alpha) * y_prev;
        }
        
        /// <summary>
        /// Compute alpha value for filter
        /// </summary>
        private float ComputeAlpha(float cutoff, float deltaTime)
        {
            float tau = 1.0f / (2.0f * Mathf.PI * cutoff);
            return 1.0f / (1.0f + tau / deltaTime);
        }
        
        /// <summary>
        /// Filter a 2D vector using the 1€ algorithm
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (seconds)</param>
        /// <returns>Filtered output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            if (!initialized)
            {
                x_prev = point;
                dx_prev = Vector2.zero;
                lastTime = timestamp;
                initialized = true;
                return point;
            }
            
            float deltaTime = timestamp - lastTime;
            if (deltaTime <= 0.0f) deltaTime = 0.001f; // Prevent division by zero
            lastTime = timestamp;
            
            // Estimate derivative as a vector
            Vector2 dx = (point - x_prev) / deltaTime;
            
            // Calculate derivative magnitude for adaptive cutoff
            float dxMagnitude = dx.magnitude;
            
            // Filter derivative components
            float alpha_d = ComputeAlpha(dcutoff, deltaTime);
            Vector2 edx = new Vector2(
                LowPassFilter(dx.x, alpha_d, dx_prev.x),
                LowPassFilter(dx.y, alpha_d, dx_prev.y)
            );
            dx_prev = edx;
            
            // Adjust cutoff based on derivative magnitude
            float cutoff = mincutoff + beta * edx.magnitude;
            
            // Filter signal components
            float alpha_c = ComputeAlpha(cutoff, deltaTime);
            Vector2 ex = new Vector2(
                LowPassFilter(point.x, alpha_c, x_prev.x),
                LowPassFilter(point.y, alpha_c, x_prev.y)
            );
            x_prev = ex;
            
            return ex;
        }
    }
}
