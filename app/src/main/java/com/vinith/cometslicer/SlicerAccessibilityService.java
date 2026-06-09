package com.vinith.cometslicer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class SlicerAccessibilityService extends AccessibilityService {
    private volatile long lastGestureMs = 0L;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        EventLog.init(this);
        BotState.accessibility = this;
        EventLog.add("Accessibility connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        EventLog.add("Accessibility interrupted");
    }

    @Override
    public void onDestroy() {
        if (BotState.accessibility == this) {
            BotState.accessibility = null;
        }
        EventLog.add("Accessibility disconnected");
        super.onDestroy();
    }

    public void sliceAt(float x, float y) {
        if (Build.VERSION.SDK_INT < 24) {
            EventLog.add("ERROR: gestures need Android 7+");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastGestureMs < 70) return;
        lastGestureMs = now;

        float length = 95f;
        Path path = new Path();
        path.moveTo(x - length, y + length);
        path.lineTo(x + length, y - length);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 65);

        GestureDescription gesture =
                new GestureDescription.Builder().addStroke(stroke).build();

        EventLog.add("SLICE tap/swipe at " + Math.round(x) + "," + Math.round(y));

        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                EventLog.add("Gesture completed");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                EventLog.add("ERROR: gesture cancelled");
            }
        }, null);
    }
}
