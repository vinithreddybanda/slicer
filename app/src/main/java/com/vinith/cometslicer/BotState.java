package com.vinith.cometslicer;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;

public final class BotState {
    private BotState() {}

    public static volatile boolean isRunning = false;
    public static volatile boolean isStarting = false;
    public static volatile int screenWidth = 0;
    public static volatile int screenHeight = 0;
    public static volatile float captureScale = 0.50f;

    public static volatile SlicerAccessibilityService accessibility;
    public static volatile MediaProjectionManager projectionManager;
    public static volatile int projectionResultCode;
    public static volatile Intent projectionData;

    public static boolean hasProjection() {
        return projectionData != null && projectionManager != null && projectionResultCode != 0;
    }

    public static void clearProjection() {
        projectionData = null;
        projectionResultCode = 0;
    }

    public static void requestSlice(float x, float y) {
        SlicerAccessibilityService service = accessibility;
        if (service != null) {
            service.sliceAt(x, y);
        } else {
            EventLog.add("ERROR: accessibility service not connected");
        }
    }
}
