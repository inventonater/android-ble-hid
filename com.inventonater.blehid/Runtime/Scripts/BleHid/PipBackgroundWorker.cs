using System;
using System.Collections.Concurrent;
using System.Threading;
using UnityEngine;

namespace Inventonater.BleHid
{
    /// <summary>
    /// Background worker that runs in a separate thread to ensure functionality
    /// continues even when Unity's main thread may be paused (such as in PiP mode).
    /// </summary>
    public class PipBackgroundWorker
    {
        private Thread workerThread;
        private volatile bool isRunning;
        private int counter = 0;
        
        /// <summary>
        /// Initialize the background worker
        /// </summary>
        public PipBackgroundWorker()
        {
            Debug.Log("PipBackgroundWorker initialized");
        }

        /// <summary>
        /// Start the background worker thread
        /// </summary>
        public void Start()
        {
            if (isRunning && workerThread != null)
            {
                Debug.Log("Background worker is already running");
                return;
            }

            isRunning = true;
            workerThread = new Thread(WorkerThreadFunction);
            workerThread.IsBackground = true; // Make it a background thread so it doesn't prevent app exit
            workerThread.Start();
            
            Debug.Log("Background worker thread started");
            
            // Add a starting entry to the log queue
            Debug.Log($"[{DateTime.Now}] Background worker started");
        }

        /// <summary>
        /// The main function for the worker thread
        /// </summary>
        private void WorkerThreadFunction()
        {
            // We can't call Debug.Log directly from a background thread
            // so we'll add entries to a queue that will be processed on the main thread
            
            Debug.Log("Worker thread function running");
            
            try
            {
                while (isRunning)
                {
                    // Create a timestamp and message
                    string timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");
                    string message = $"Background worker still running (count: {counter++})";
                    
                    // Add to queue for main thread to process
                    Debug.Log($"[{timestamp}] {message}");
                    
                    // Wait for a second
                    Thread.Sleep(1000);
                }
            }
            catch (ThreadAbortException)
            {
                // Thread is being aborted
                Debug.Log($"[{DateTime.Now}] Thread aborted");
            }
            catch (Exception ex)
            {
                // General exception in thread
                Debug.Log($"[{DateTime.Now}] Thread exception: {ex.Message}");
            }
            finally
            {
                Debug.Log($"[{DateTime.Now}] Background worker stopped");
            }
        }

        /// <summary>
        /// Stop the background worker thread
        /// </summary>
        public void Stop()
        {
            isRunning = false;
            
            if (workerThread != null && workerThread.IsAlive)
            {
                // Give the thread a chance to exit cleanly
                workerThread.Join(2000);
                
                // If it's still running, abort it (though this is not recommended in general)
                if (workerThread.IsAlive)
                {
                    try
                    {
                        workerThread.Abort();
                    }
                    catch (Exception ex)
                    {
                        Debug.LogError($"Error aborting thread: {ex.Message}");
                    }
                }
                
                workerThread = null;
            }
            
            Debug.Log("Background worker thread stopped");
        }
    }
}
