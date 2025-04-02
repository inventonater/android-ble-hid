package com.inventonater.hid.app.ui;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.inventonater.hid.app.R;
import com.inventonater.hid.core.BleHid;
import com.inventonater.hid.core.api.MouseButton;

public class SimpleMouseActivity extends AppCompatActivity {
    private View mousepadView;
    private Button leftButton;
    private Button rightButton;
    private Button middleButton;
    private TextView statusText;
    
    // Tracking for touch events
    private float lastX = 0;
    private float lastY = 0;
    
    // Movement settings
    private static final float SENSITIVITY = 1.0f;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mouse);
        
        // Initialize UI components
        mousepadView = findViewById(R.id.mousepadView);
        leftButton = findViewById(R.id.leftButton);
        rightButton = findViewById(R.id.rightButton);
        middleButton = findViewById(R.id.middleButton);
        statusText = findViewById(R.id.statusText);
        
        // Check if we're connected
        updateConnectionStatus();
        
        // Set up touch listener for mousepad
        mousepadView.setOnTouchListener(this::handleMousepadTouch);
        
        // Set up mouse button listeners
        leftButton.setOnClickListener(v -> handleButtonClick(MouseButton.LEFT));
        rightButton.setOnClickListener(v -> handleButtonClick(MouseButton.RIGHT));
        middleButton.setOnClickListener(v -> handleButtonClick(MouseButton.MIDDLE));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        updateConnectionStatus();
    }
    
    private boolean handleMousepadTouch(View v, MotionEvent event) {
        if (!BleHid.isConnected()) {
            updateConnectionStatus();
            return false;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Record initial touch position
                lastX = event.getX();
                lastY = event.getY();
                return true;
                
            case MotionEvent.ACTION_MOVE:
                // Calculate movement delta
                float deltaX = (event.getX() - lastX) * SENSITIVITY;
                float deltaY = (event.getY() - lastY) * SENSITIVITY;
                
                // Move the mouse
                if (Math.abs(deltaX) > 0.5 || Math.abs(deltaY) > 0.5) {
                    // Clamp to valid range (-127 to 127)
                    int moveX = (int) Math.max(-127, Math.min(127, deltaX));
                    int moveY = (int) Math.max(-127, Math.min(127, deltaY));
                    
                    // Send mouse movement command
                    boolean success = BleHid.moveMouse(moveX, moveY);
                    
                    if (!success) {
                        Toast.makeText(this, "Failed to send mouse movement", Toast.LENGTH_SHORT).show();
                    }
                    
                    // Update last position
                    lastX = event.getX();
                    lastY = event.getY();
                }
                return true;
                
            case MotionEvent.ACTION_UP:
                return true;
        }
        
        return false;
    }
    
    private void handleButtonClick(MouseButton button) {
        if (!BleHid.isConnected()) {
            updateConnectionStatus();
            return;
        }
        
        // Send button click command
        boolean success = BleHid.clickMouseButton(button);
        
        if (!success) {
            Toast.makeText(this, "Failed to send button click", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateConnectionStatus() {
        if (BleHid.isConnected()) {
            statusText.setText("Status: Connected");
        } else {
            statusText.setText("Status: Not connected");
            Toast.makeText(this, "Not connected to a device", Toast.LENGTH_SHORT).show();
        }
    }
}
