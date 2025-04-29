package com.inventonater.blehid.core;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.GestureDescription.StrokeDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.Path;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class LocalInputController {
    private static final String TAG = "LocalInputController";

    private final Context context;
    private LocalAccessibilityService accessibilityService;
    private ContinuousScroller continuousScroller;

    public LocalInputController(Context context) {
        this.context = context;
    }

    public void registerAccessibilityService(LocalAccessibilityService service) {
        this.accessibilityService = service;
        Log.d(TAG, "Accessibility service registered");

        continuousScroller = new ContinuousScroller(service);
    }

    public boolean isAccessibilityServiceAvailable() {
        if (accessibilityService == null) {
            Log.e(TAG, "Accessibility service not registered");
            return false;
        }
        return true;
    }

    public boolean isAccessibilityServiceEnabled() {
        String serviceName = context.getPackageName() + "/" +
                LocalAccessibilityService.class.getCanonicalName();

        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

        return enabledServices != null && enabledServices.contains(serviceName);
    }

    public void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public boolean tap(int x, int y) {
        if (!isAccessibilityServiceAvailable()) return false;

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new StrokeDescription(path, 0, 100));

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        accessibilityService.dispatchGesture(builder.build(),
                new AccessibilityService.GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        result[0] = true;
                        latch.countDown();
                    }

                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        result[0] = false;
                        latch.countDown();
                    }
                }, null);

        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Log.e(TAG, "Tap gesture interrupted", e);
            return false;
        }

        return result[0];
    }

    public static boolean performGlobalTap(int x, int y) {
        LocalAccessibilityService service = LocalAccessibilityService.getInstance();
        if (service == null) {
            Log.e(TAG, "Accessibility service not running for global tap");
            return false;
        }

        // Verify service is fully connected and ready
        if (!service.isReady()) {
            Log.e(TAG, "Accessibility service is not ready/connected");
            return false;
        }

        Log.i(TAG, "Attempting global tap at (" + x + ", " + y + ")");

        Path path = new Path();
        path.moveTo(x, y);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new StrokeDescription(path, 0, 150)); // longer duration for more reliable tap

        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] result = {false};

        try {
            service.dispatchGesture(builder.build(),
                    new AccessibilityService.GestureResultCallback() {
                        @Override
                        public void onCompleted(GestureDescription gestureDescription) {
                            result[0] = true;
                            Log.d(TAG, "Global tap completed successfully");
                            latch.countDown();
                        }

                        @Override
                        public void onCancelled(GestureDescription gestureDescription) {
                            result[0] = false;
                            Log.w(TAG, "Global tap was cancelled");
                            latch.countDown();
                        }
                    }, null);

            // Wait for the tap to complete with a longer timeout
            if (!latch.await(1500, TimeUnit.MILLISECONDS)) {
                Log.e(TAG, "Global tap timed out waiting for completion");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during global tap: " + e.getMessage(), e);
            return false;
        }

        return result[0];
    }

    public boolean swipeBegin(float startX, float startY) {
        if (!isAccessibilityServiceAvailable() || continuousScroller == null) return false;
        continuousScroller.begin(startX, startY);
        return true;
    }

    public boolean swipeExtend(float deltaX, float deltaY) {
        if (!isAccessibilityServiceAvailable() || continuousScroller == null) return false;
        continuousScroller.extend(deltaX, deltaY);
        return true;
    }

    public boolean swipeEnd() {
        if (!isAccessibilityServiceAvailable() || continuousScroller == null) return false;
        continuousScroller.end();
        return true;
    }

    public boolean performGlobalAction(int globalAction) {
        if (!isAccessibilityServiceAvailable()) return false;

        boolean result = accessibilityService.performGlobalAction(globalAction);
        Log.d(TAG, "Key event sent: " + globalAction + ", result: " + result);
        return result;
    }

    public boolean performFocusedNodeAction(int action) {
        if (!isAccessibilityServiceAvailable()) return false;

        AccessibilityNodeInfo focusedNode = null;
        try {
            focusedNode = accessibilityService.findFocus(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
            if (focusedNode != null) {
                Log.d(TAG, "Found focused node: " + focusedNode);
                return focusedNode.performAction(action);
            } else {
                Log.w(TAG, "No accessibility focused node found");
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing action on focused node", e);
            return false;
        } finally {
            if (focusedNode != null) {
                focusedNode.recycle();
            }
        }
    }

    public boolean clickFocusedNode() {
        return performFocusedNodeAction(AccessibilityNodeInfo.ACTION_CLICK);
    }
}
