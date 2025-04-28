package com.inventonater.blehid.core;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.util.Log;

public final class ContinuousScroller {

    private static final String TAG = "ContinuousScroller";

    private final AccessibilityService svc;
    private GestureDescription.StrokeDescription active;
    private float lastX, lastY;
    private boolean inFlight;

    public ContinuousScroller(AccessibilityService svc) { this.svc = svc; }

    /** Call once when the finger goes down. */
    public void begin(float x, float y) {
        Path p = new Path();
        p.moveTo(x, y);                     // Touch-down
        active = new GestureDescription.StrokeDescription(p, 0, 5, /*willContinue=*/true);
        lastX = x; lastY = y;
        dispatch(active, /*nextWillContinue=*/true);
    }

    /** Call every frame with the delta you want to add to the drag. */
    public void extend(float dx, float dy) {
        if (inFlight || active == null) return;                 // still waiting
        Path p = new Path();
        p.moveTo(lastX, lastY);
        p.lineTo(lastX += dx, lastY += dy);
        active = active.continueStroke(p, 0, 5, /*willContinue=*/true);
        dispatch(active, true);
    }

    /** Call when you want to lift the finger. */
    public void end() {
        if (inFlight || active == null) return;
        Path p = new Path();
        p.moveTo(lastX, lastY);                  // No extra movement, just lift
        active = active.continueStroke(p, 0, 5, /*willContinue=*/false);
        dispatch(active, false);
    }

    // ------------------------------------------------------------------------

    private void dispatch(GestureDescription.StrokeDescription stroke, boolean nextWillContinue) {

        GestureDescription gestureDescription = new GestureDescription.Builder().addStroke(stroke).build();
        inFlight = true;
        AccessibilityService.GestureResultCallback callback = new AccessibilityService.GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription d) {
                inFlight = false;
                if (!nextWillContinue) {
                    active = null;       // finished
                    Log.d(TAG, String.format("dispatch onCompleted: %s, %s", lastX, lastY));
                }
            }

            @Override
            public void onCancelled(GestureDescription d) {
                Log.d(TAG, "dispatch onCancelled");

                inFlight = false;
                active = null;
            }
        };
        svc.dispatchGesture(gestureDescription, callback, null /*handler – use main looper*/);
    }
}
