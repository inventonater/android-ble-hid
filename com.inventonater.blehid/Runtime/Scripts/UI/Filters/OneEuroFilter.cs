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
        private float x_prev;       // Previous filtered value
        private float dx_prev;      // Previous derivative
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
            x_prev = 0;
            dx_prev = 0;
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
        /// Filter a single float value
        /// </summary>
        /// <param name="x">Input value</param>
        /// <param name="timestamp">Current timestamp (seconds)</param>
        /// <returns>Filtered output value</returns>
        public float Filter(float x, float timestamp)
        {
            if (!initialized)
            {
                x_prev = x;
                dx_prev = 0.0f;
                lastTime = timestamp;
                initialized = true;
                return x;
            }
            
            float deltaTime = timestamp - lastTime;
            if (deltaTime <= 0.0f) deltaTime = 0.001f; // Prevent division by zero
            lastTime = timestamp;
            
            // Estimate derivative
            float dx = (x - x_prev) / deltaTime;
            
            // Filter derivative
            float edx = LowPassFilter(dx, ComputeAlpha(dcutoff, deltaTime), dx_prev);
            dx_prev = edx;
            
            // Adjust cutoff based on derivative
            float cutoff = mincutoff + beta * Mathf.Abs(edx);
            
            // Filter signal
            float ex = LowPassFilter(x, ComputeAlpha(cutoff, deltaTime), x_prev);
            x_prev = ex;
            
            return ex;
        }
        
        /// <summary>
        /// Filter a 2D vector by applying the filter separately to each component
        /// </summary>
        /// <param name="point">Input vector</param>
        /// <param name="timestamp">Current timestamp (seconds)</param>
        /// <returns>Filtered output vector</returns>
        public Vector2 Filter(Vector2 point, float timestamp)
        {
            return new Vector2(
                Filter(point.x, timestamp),
                Filter(point.y, timestamp)
            );
        }
    }
}
