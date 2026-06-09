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
        BotState.accessibility = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not needed. This service is only used for dispatchGesture().
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (BotState.accessibility == this) {
            BotState.accessibility = null;
        }
        super.onDestroy();
    }

    public void sliceAt(float x, float y) {
        if (Build.VERSION.SDK_INT < 24) return;

        long now = System.currentTimeMillis();
        if (now - lastGestureMs < 70) return;
        lastGestureMs = now;

        float length = 90f;
        Path path = new Path();
        path.moveTo(x - length, y + length);
        path.lineTo(x + length, y - length);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 55);

        GestureDescription gesture =
                new GestureDescription.Builder().addStroke(stroke).build();

        dispatchGesture(gesture, null, null);
    }
}
