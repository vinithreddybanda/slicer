package com.vinith.cometslicer;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayButtonService extends Service {
    private WindowManager windowManager;
    private LinearLayout panel;
    private TextView button;
    private TextView logView;
    private int lastX;
    private int lastY;
    private float touchX;
    private float touchY;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUi();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventLog.init(this);

        if (!Settings.canDrawOverlays(this)) {
            EventLog.add("ERROR: overlay permission missing");
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(14, 14, 14, 14);
        panel.setBackgroundColor(0xCC111827);

        button = new TextView(this);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(14);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        button.setPadding(26, 18, 26, 18);

        logView = new TextView(this);
        logView.setTextColor(0xFFE5E7EB);
        logView.setTextSize(10);
        logView.setPadding(0, 10, 0, 0);
        logView.setMaxWidth(560);

        panel.addView(button);
        panel.addView(logView);

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
        panel.setOnTouchListener((v, event) -> {
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
                    windowManager.updateViewLayout(panel, params);
                    return true;
                default:
                    return false;
            }
        });

        windowManager.addView(panel, params);

        IntentFilter filter = new IntentFilter();
        filter.addAction(EventLog.ACTION_LOG_UPDATED);
        filter.addAction(EventLog.ACTION_STATE_UPDATED);
        registerReceiver(receiver, filter);

        EventLog.add("Floating logs ready");
        refreshUi();
    }

    private void toggleBot() {
        if (!BotState.isRunning && !BotState.isStarting) {
            if (BotState.accessibility == null) {
                EventLog.add("ERROR: enable accessibility first");
                Toast.makeText(this, "Enable accessibility first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!BotState.hasProjection()) {
                EventLog.add("ERROR: allow screen capture in main app first");
                Toast.makeText(this, "Allow screen capture first", Toast.LENGTH_SHORT).show();
                Intent openApp = new Intent(this, MainActivity.class);
                openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(openApp);
                return;
            }

            BotState.isStarting = true;
            EventLog.add("START pressed: launching capture service");

            Intent i = new Intent(this, ScreenCaptureService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);

            refreshUi();
        } else {
            EventLog.add("STOP pressed");
            BotState.isRunning = false;
            BotState.isStarting = false;
            stopService(new Intent(this, ScreenCaptureService.class));
            refreshUi();
        }
    }

    private void refreshUi() {
        if (button == null || logView == null) return;

        if (BotState.isRunning) {
            button.setText("STOP");
            button.setBackgroundColor(0xCCEF4444);
        } else if (BotState.isStarting) {
            button.setText("STARTING...");
            button.setBackgroundColor(0xCCF59E0B);
        } else {
            button.setText("START");
            button.setBackgroundColor(0xCC7C3AED);
        }

        logView.setText(EventLog.dump());
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(receiver);
        } catch (Exception ignored) {}

        if (windowManager != null && panel != null) {
            windowManager.removeView(panel);
        }
        super.onDestroy();
    }
}
