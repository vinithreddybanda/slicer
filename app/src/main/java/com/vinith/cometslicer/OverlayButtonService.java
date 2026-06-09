package com.vinith.cometslicer;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayButtonService extends Service {
    private WindowManager windowManager;
    private TextView button;
    private int lastX;
    private int lastY;
    private float touchX;
    private float touchY;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        button = new TextView(this);
        button.setText("START");
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        button.setBackgroundColor(0xCC7C3AED);
        button.setPadding(26, 18, 26, 18);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 30;
        params.y = 180;

        button.setOnClickListener(v -> toggleBot());
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = params.x;
                    lastY = params.y;
                    touchX = event.getRawX();
                    touchY = event.getRawY();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    params.x = lastX + (int) (event.getRawX() - touchX);
                    params.y = lastY + (int) (event.getRawY() - touchY);
                    windowManager.updateViewLayout(button, params);
                    return true;
                default:
                    return false;
            }
        });

        windowManager.addView(button, params);
    }

    private void toggleBot() {
        if (!BotState.isRunning) {
            if (BotState.accessibility == null) {
                Toast.makeText(this, "Enable accessibility first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!BotState.hasProjection()) {
                Toast.makeText(this, "Open app and allow screen capture first", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent i = new Intent(this, ScreenCaptureService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
            BotState.isRunning = true;
            button.setText("STOP");
            button.setBackgroundColor(0xCCEF4444);
        } else {
            BotState.isRunning = false;
            stopService(new Intent(this, ScreenCaptureService.class));
            button.setText("START");
            button.setBackgroundColor(0xCC7C3AED);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (windowManager != null && button != null) {
            windowManager.removeView(button);
        }
    }
}
