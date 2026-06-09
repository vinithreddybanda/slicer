package com.vinith.cometslicer;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;

public final class Bot {
    private Bot() {}

    public static volatile boolean running = false;
    public static volatile boolean starting = false;

    public static volatile MediaProjectionManager projectionManager;
    public static volatile Intent projectionData;
    public static volatile int projectionResultCode = 0;

    public static volatile GestureService gestureService;

    public static volatile int screenWidth = 0;
    public static volatile int screenHeight = 0;
    public static volatile float scale = 0.50f;

    public static boolean hasCapturePermission() {
        return projectionManager != null && projectionData != null && projectionResultCode != 0;
    }

    public static void clearCapturePermission() {
        projectionData = null;
        projectionResultCode = 0;
    }

    public static void slice(float x, float y) {
        GestureService service = gestureService;
        if (service == null) {
            AppLog.add("ERROR: gesture service not connected");
            return;
        }
        service.slice(x, y);
    }
}
