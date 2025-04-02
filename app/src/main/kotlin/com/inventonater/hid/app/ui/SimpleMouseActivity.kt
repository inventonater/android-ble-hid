package com.inventonater.hid.app.ui

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.inventonater.hid.app.R
import com.inventonater.hid.core.BleHid
import com.inventonater.hid.core.api.MouseButton

/**
 * Activity for controlling mouse movements and button clicks.
 * Uses a simplified UI for demonstrating BLE HID mouse capabilities.
 */
class SimpleMouseActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SimpleMouseActivity"
        
        // Sensitivity of mouse movement (higher = more sensitive)
        private const val MOVEMENT_SENSITIVITY = 0.5f
    }
    
    // UI components
    private lateinit var touchpadArea: View
    private lateinit var leftButton: Button
    private lateinit var rightButton: Button
    private lateinit var middleButton: Button
    private lateinit var statusText: TextView
    
    // Tracking for mouse movement
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mouse)
        
        // Initialize UI components
        touchpadArea = findViewById(R.id.touchpadArea)
        leftButton = findViewById(R.id.leftButton)
        rightButton = findViewById(R.id.rightButton)
        middleButton = findViewById(R.id.middleButton)
        statusText = findViewById(R.id.statusText)
        
        // Update connection status
        updateStatus()
        
        // Setup touch listeners
        touchpadArea.setOnTouchListener(this::onMousepadTouch)
        
        // Setup button click listeners for mouse buttons
        leftButton.setOnClickListener { handleButtonClick(MouseButton.LEFT) }
        rightButton.setOnClickListener { handleButtonClick(MouseButton.RIGHT) }
        middleButton.setOnClickListener { handleButtonClick(MouseButton.MIDDLE) }
    }
    
    override fun onResume() {
        super.onResume()
        updateStatus()
    }
    
    private fun onMousepadTouch(v: View, event: MotionEvent): Boolean {
        if (!BleHid.isConnected()) {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show()
            return false
        }
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Store initial position
                lastX = event.x
                lastY = event.y
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                // Calculate movement delta
                val deltaX = event.x - lastX
                val deltaY = event.y - lastY
                
                // Update last position
                lastX = event.x
                lastY = event.y
                
                // Apply sensitivity and convert to int values
                val moveX = (deltaX * MOVEMENT_SENSITIVITY).toInt()
                val moveY = (deltaY * MOVEMENT_SENSITIVITY).toInt()
                
                // Only send if there's actual movement
                if (moveX != 0 || moveY != 0) {
                    val success = BleHid.moveMouse(moveX, moveY)
                    
                    if (!success) {
                        Toast.makeText(this, "Failed to send mouse movement", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }
            
            else -> return false
        }
    }
    
    private fun handleButtonClick(button: MouseButton) {
        if (!BleHid.isConnected()) {
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Send mouse button click
        val success = BleHid.clickMouseButton(button)
        
        if (!success) {
            Toast.makeText(this, "Failed to send mouse button click", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateStatus() {
        if (BleHid.isConnected()) {
            statusText.text = "Status: Connected"
            enableControls(true)
        } else {
            statusText.text = "Status: Not connected"
            enableControls(false)
        }
    }
    
    private fun enableControls(enabled: Boolean) {
        touchpadArea.isEnabled = enabled
        leftButton.isEnabled = enabled
        rightButton.isEnabled = enabled
        middleButton.isEnabled = enabled
        
        // Change the alpha for visual feedback
        val alpha = if (enabled) 1.0f else 0.5f
        touchpadArea.alpha = alpha
        leftButton.alpha = alpha
        rightButton.alpha = alpha
        middleButton.alpha = alpha
    }
}
