package com.example.blehid.app.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.blehid.app.R;
import com.example.blehid.core.HidConstants;
import com.example.blehid.core.BleHidManager;

/**
 * Fragment that handles mouse input controls
 */
public class MouseFragment extends Fragment {
    private static final String TAG = "MouseFragment";
    
    private static final float MOVEMENT_FACTOR = 1.5f;
    
    private View touchpadArea;
    private Button leftButton;
    private Button middleButton;
    private Button rightButton;
    private Button scrollUpButton;
    private Button scrollDownButton;
    
    private BleHidManager bleHidManager;
    private HidEventListener eventListener;
    
    // Touch processing
    private float lastTouchX;
    private float lastTouchY;
    private boolean isTrackingTouch = false;
    
    /**
     * Interface for HID event logging
     */
    public interface HidEventListener {
        void onHidEvent(String event);
    }
    
    public void setBleHidManager(BleHidManager manager) {
        this.bleHidManager = manager;
    }
    
    public void setEventListener(HidEventListener listener) {
        this.eventListener = listener;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mouse, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize mouse controls
        touchpadArea = view.findViewById(R.id.touchpadArea);
        leftButton = view.findViewById(R.id.leftButton);
        middleButton = view.findViewById(R.id.middleButton);
        rightButton = view.findViewById(R.id.rightButton);
        scrollUpButton = view.findViewById(R.id.scrollUpButton);
        scrollDownButton = view.findViewById(R.id.scrollDownButton);
        
        setupControls();
    }
    
    private void setupControls() {
        // Set up touchpad area
        touchpadArea.setOnTouchListener((v, event) -> {
            // Prevent parent views from intercepting touch events
            disableParentScrolling(v);
            
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                disableParentScrolling(v);
            }
            
            handleTouchpadEvent(event);
            return true;
        });
        
        // Set up mouse buttons
        leftButton.setOnClickListener(v -> clickButton(HidConstants.Mouse.BUTTON_LEFT));
        middleButton.setOnClickListener(v -> clickButton(HidConstants.Mouse.BUTTON_MIDDLE));
        rightButton.setOnClickListener(v -> clickButton(HidConstants.Mouse.BUTTON_RIGHT));
        
        // Set up scroll buttons
        scrollUpButton.setOnClickListener(v -> scroll(10)); // Positive = scroll up
        scrollDownButton.setOnClickListener(v -> scroll(-10)); // Negative = scroll down
    }
    
    private boolean handleTouchpadEvent(MotionEvent event) {
        if (bleHidManager == null || !bleHidManager.isConnected()) {
            logEvent("TOUCH IGNORED: No connected device");
            return false;
        }
        
        float x = event.getX();
        float y = event.getY();
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                isTrackingTouch = true;
                return true;
                
            case MotionEvent.ACTION_MOVE:
                if (isTrackingTouch) {
                    // Calculate movement delta
                    float deltaX = x - lastTouchX;
                    float deltaY = y - lastTouchY;
                    
                    // Only send movement if it's significant
                    if (Math.abs(deltaX) > 1 || Math.abs(deltaY) > 1) {
                        // Convert to relative movement values (-127 to 127)
                        int moveX = (int)(deltaX * MOVEMENT_FACTOR);
                        int moveY = (int)(deltaY * MOVEMENT_FACTOR);
                        
                        // Ensure small movements aren't lost but don't force large jumps
                        if (moveX != 0 && Math.abs(moveX) < 2) {
                            moveX = moveX > 0 ? 2 : -2;
                        }
                        if (moveY != 0 && Math.abs(moveY) < 2) {
                            moveY = moveY > 0 ? 2 : -2;
                        }
                        
                        // Clamp values
                        moveX = Math.max(-127, Math.min(127, moveX));
                        moveY = Math.max(-127, Math.min(127, moveY));
                        
                        // Log the original delta values for debugging
                        Log.d(TAG, "TOUCH DELTA - Original: (" + deltaX + ", " + deltaY + ")");
                        
                        // Log before sending to HID service
                        Log.d(TAG, "SENDING TO HID - moveX: " + moveX + ", moveY: " + moveY);
                        
                        // Send mouse movement
                        boolean result = bleHidManager.moveMouse(moveX, moveY);
                        if (result) {
                            logEvent("MOUSE MOVE: deltaXY(" + deltaX + ", " + deltaY + 
                                   ") → moveXY(" + moveX + ", " + moveY + ")");
                            
                            // Add small delay to avoid overwhelming the connection
                            try {
                                Thread.sleep(5);
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        } else {
                            logEvent("MOUSE MOVE FAILED: (" + moveX + ", " + moveY + ")");
                        }
                        
                        // Update last position
                        lastTouchX = x;
                        lastTouchY = y;
                    }
                    return true;
                }
                break;
                
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isTrackingTouch = false;
                break;
        }
        
        return false;
    }
    
    private void clickButton(int button) {
        if (bleHidManager == null || !bleHidManager.isConnected()) {
            logEvent("BUTTON CLICK IGNORED: No connected device");
            return;
        }
        
        // Send a press event first with a short delay before releasing
        logEvent("BUTTON PRESS: Button " + button);
        boolean pressResult = bleHidManager.pressMouseButton(button);
        
        // Add a longer delay for button press (50ms)
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        boolean releaseResult = bleHidManager.releaseMouseButtons();
        boolean result = pressResult && releaseResult;
        
        String buttonName;
        switch (button) {
            case HidConstants.Mouse.BUTTON_LEFT:
                buttonName = "LEFT";
                break;
            case HidConstants.Mouse.BUTTON_RIGHT:
                buttonName = "RIGHT";
                break;
            case HidConstants.Mouse.BUTTON_MIDDLE:
                buttonName = "MIDDLE";
                break;
            default:
                buttonName = "UNKNOWN";
        }
        
        logEvent("MOUSE BUTTON: " + buttonName + (result ? " clicked" : " FAILED"));
    }
    
    private void scroll(int amount) {
        if (bleHidManager == null || !bleHidManager.isConnected()) {
            logEvent("SCROLL IGNORED: No connected device");
            return;
        }
        
        // Increase scroll amount for better recognition
        int scaledAmount = amount * 3;
        
        boolean result = bleHidManager.scrollMouseWheel(scaledAmount);
        logEvent("SCROLL: " + scaledAmount + (result ? "" : " FAILED"));
        
        // Add a small delay after scrolling
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
    }
    
    private void disableParentScrolling(View view) {
        if (view == null) return;
        
        // Request the immediate parent to not intercept touch events
        ViewParent parent = view.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
            
            // Recursively apply to all parent scroll views
            if (parent instanceof View) {
                ViewParent grandParent = parent.getParent();
                while (grandParent != null) {
                    if (grandParent instanceof androidx.core.widget.NestedScrollView ||
                        grandParent instanceof android.widget.ScrollView) {
                        grandParent.requestDisallowInterceptTouchEvent(true);
                    }
                    grandParent = grandParent.getParent();
                }
            }
        }
    }
    
    private void logEvent(String event) {
        if (eventListener != null) {
            eventListener.onHidEvent(event);
        }
    }
}
