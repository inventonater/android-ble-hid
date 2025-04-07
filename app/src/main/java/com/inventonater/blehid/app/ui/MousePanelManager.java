package com.inventonater.blehid.app.ui;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.inventonater.blehid.app.R;
import com.inventonater.blehid.core.BleHidManager;
import com.inventonater.blehid.core.HidMediaService;

/**
 * Manages the mouse panel UI and functionality.
 * Handles touchpad input and mouse button controls.
 */
public class MousePanelManager {
    private static final String TAG = "MousePanelManager";

    public interface Callback {
        void logEvent(String message);
    }

    private final Context context;
    private final View mousePanel;
    private final BleHidManager bleHidManager;
    private final Callback callback;

    private FrameLayout touchpadArea;
    private Button leftButton;
    private Button middleButton;
    private Button rightButton;

    // Touchpad state tracking
    private float lastTouchX;
    private float lastTouchY;
    private boolean isMoving = false;

    /**
     * Creates a new MousePanelManager.
     *
     * @param context The activity context
     * @param mousePanel The mouse panel view
     * @param bleHidManager The BLE HID manager instance
     * @param callback Callback for logging events
     */
    public MousePanelManager(Context context, View mousePanel, BleHidManager bleHidManager, Callback callback) {
        this.context = context;
        this.mousePanel = mousePanel;
        this.bleHidManager = bleHidManager;
        this.callback = callback;

        initializeViews();
        setupListeners();
    }

    /**
     * Initialize the mouse panel UI elements.
     */
    private void initializeViews() {
        touchpadArea = mousePanel.findViewById(R.id.touchpadArea);
        leftButton = mousePanel.findViewById(R.id.leftButton);
        middleButton = mousePanel.findViewById(R.id.middleButton);
        rightButton = mousePanel.findViewById(R.id.rightButton);
    }

    /**
     * Set up click and touch listeners for the mouse panel elements.
     */
    private void setupListeners() {
        // Set up touchpad area for mouse movement
        touchpadArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleTouchpadEvent(event);
            }
        });

        // Set up left mouse button
        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.logEvent("MOUSE: Left click");
                if (bleHidManager.isConnected()) {
                    boolean result = bleHidManager.clickMouseButton(HidMediaService.BUTTON_LEFT);
                    if (!result) {
                        Toast.makeText(context, "Failed to send left click", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up middle mouse button
        middleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.logEvent("MOUSE: Middle click");
                if (bleHidManager.isConnected()) {
                    boolean result = bleHidManager.clickMouseButton(HidMediaService.BUTTON_MIDDLE);
                    if (!result) {
                        Toast.makeText(context, "Failed to send middle click", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Set up right mouse button
        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callback.logEvent("MOUSE: Right click");
                if (bleHidManager.isConnected()) {
                    boolean result = bleHidManager.clickMouseButton(HidMediaService.BUTTON_RIGHT);
                    if (!result) {
                        Toast.makeText(context, "Failed to send right click", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(context, R.string.not_connected, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Handles touch events on the touchpad area.
     * 
     * @param event The MotionEvent
     * @return true if the event was handled, false otherwise
     */
    private boolean handleTouchpadEvent(MotionEvent event) {
        if (!bleHidManager.isConnected()) {
            return false;
        }
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = event.getX();
                lastTouchY = event.getY();
                isMoving = true;
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (isMoving) {
                    float currentX = event.getX();
                    float currentY = event.getY();
                    
                    // Calculate movement delta
                    float deltaX = currentX - lastTouchX;
                    float deltaY = currentY - lastTouchY;
                    
                    // Scale movement (adjust sensitivity as needed)
                    int scaledDeltaX = (int)(deltaX * 0.8);
                    int scaledDeltaY = (int)(deltaY * 0.8);
                    
                    // Only send if there's significant movement
                    if (Math.abs(scaledDeltaX) > 0 || Math.abs(scaledDeltaY) > 0) {
                        // Clamp to valid range (-127 to 127)
                        scaledDeltaX = Math.max(-127, Math.min(127, scaledDeltaX));
                        scaledDeltaY = Math.max(-127, Math.min(127, scaledDeltaY));
                        
                        // Send the mouse movement
                        boolean result = bleHidManager.moveMouse(scaledDeltaX, scaledDeltaY);
                        
                        if (result) {
                            // Update last position if the movement was sent successfully
                            lastTouchX = currentX;
                            lastTouchY = currentY;
                            
                            // Log only for significant movements to avoid spam
                            if (Math.abs(scaledDeltaX) > 5 || Math.abs(scaledDeltaY) > 5) {
                                callback.logEvent("MOUSE: Move X:" + scaledDeltaX + " Y:" + scaledDeltaY);
                            }
                        }
                    }
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isMoving = false;
                return true;
        }
        
        return false;
    }
}
