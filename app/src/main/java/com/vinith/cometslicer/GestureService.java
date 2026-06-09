package com.vinith.cometslicer;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

public class GestureService extends AccessibilityService {
    private long lastGestureMs = 0L;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AppLog.init(this);
        Bot.gestureService = this;
        AppLog.add("Accessibility connected");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        AppLog.add("Accessibility interrupted");
    }

    @Override
    public void onDestroy() {
        if (Bot.gestureService == this) {
            Bot.gestureService = null;
        }
        AppLog.add("Accessibility disconnected");
        super.onDestroy();
    }

    public void slice(float x, float y) {
        if (Build.VERSION.SDK_INT < 24) {
            AppLog.add("ERROR: gestures need Android 7+");
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastGestureMs < 90) return;
        lastGestureMs = now;

        float len = 100f;
        Path p = new Path();
        p.moveTo(x - len, y + len);
        p.lineTo(x + len, y - len);

        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(p, 0, 70);
        GestureDescription gesture =
                new GestureDescription.Builder().addStroke(stroke).build();

        AppLog.add("SLICE " + Math.round(x) + "," + Math.round(y));
        dispatchGesture(gesture, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                AppLog.add("Gesture completed");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                AppLog.add("ERROR: gesture cancelled");
            }
        }, null);
    }
}
