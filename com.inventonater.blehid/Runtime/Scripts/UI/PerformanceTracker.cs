using System.Collections.Generic;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Tracks and calculates various performance metrics including message rates and timing
    /// </summary>
    public class PerformanceTracker
    {
        // Time-based metrics
        private Queue<float> _messageTimestamps = new Queue<float>();
        private float _lastPerSecondUpdateTime;
        private float _currentMessagesPerSecond = 0;
        
        // Frame rate metrics
        private float _fpsUpdateInterval = 0.5f; // How often to update FPS (in seconds)
        private float _lastFpsUpdateTime;
        private int _frameCount = 0;
        private float _currentFps = 0;
        
        /// <summary>
        /// Current messages per second rate
        /// </summary>
        public float MessagesPerSecond => _currentMessagesPerSecond;
        
        /// <summary>
        /// Current frames per second rate
        /// </summary>
        public float FramesPerSecond => _currentFps;
        
        /// <summary>
        /// Initialize the performance tracker
        /// </summary>
        public PerformanceTracker()
        {
            _lastPerSecondUpdateTime = Time.time;
            _lastFpsUpdateTime = Time.time;
        }
        
        /// <summary>
        /// Track a new message being sent
        /// </summary>
        public void TrackMessage()
        {
            _messageTimestamps.Enqueue(Time.time);
        }
        
        /// <summary>
        /// Update performance metrics, should be called every frame
        /// </summary>
        public void Update()
        {
            float currentTime = Time.time;
            
            // Update message rate
            // Remove timestamps older than 1 second
            while (_messageTimestamps.Count > 0 && _messageTimestamps.Peek() < currentTime - 1.0f)
            {
                _messageTimestamps.Dequeue();
            }
            
            _currentMessagesPerSecond = _messageTimestamps.Count;
            
            // Update FPS counter
            _frameCount++;
            
            // Calculate FPS and reset counter if update interval has elapsed
            if (currentTime - _lastFpsUpdateTime > _fpsUpdateInterval)
            {
                _currentFps = _frameCount / (currentTime - _lastFpsUpdateTime);
                _frameCount = 0;
                _lastFpsUpdateTime = currentTime;
            }
        }
        
        /// <summary>
        /// Reset the performance tracker
        /// </summary>
        public void Reset()
        {
            _messageTimestamps.Clear();
            _frameCount = 0;
            _lastFpsUpdateTime = Time.time;
            _lastPerSecondUpdateTime = Time.time;
            _currentMessagesPerSecond = 0;
            _currentFps = 0;
        }
    }
}
