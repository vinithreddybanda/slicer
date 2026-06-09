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
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayLogService extends Service {
    private WindowManager windowManager;
    private LinearLayout panel;
    private TextView button;
    private TextView logs;

    private int startX;
    private int startY;
    private float downX;
    private float downY;
    private long downTime;
    private WindowManager.LayoutParams params;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.init(this);

        if (!Settings.canDrawOverlays(this)) {
            AppLog.add("ERROR: overlay permission missing");
            stopSelf();
            return;
        }

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(14, 14, 14, 14);
        panel.setBackgroundColor(0xDD111827);

        button = new TextView(this);
        button.setGravity(Gravity.CENTER);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(14);
        button.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        button.setPadding(28, 18, 28, 18);

        logs = new TextView(this);
        logs.setTextColor(0xFFE5E7EB);
        logs.setTextSize(10);
        logs.setMaxWidth(620);
        logs.setPadding(0, 10, 0, 0);

        panel.addView(button);
        panel.addView(logs);

        int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 24;
        params.y = 180;

        button.setOnClickListener(v -> toggle());

        panel.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX = params.x;
                    startY = params.y;
                    downX = event.getRawX();
                    downY = event.getRawY();
                    downTime = System.currentTimeMillis();
                    return false;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        params.x = startX + (int) dx;
                        params.y = startY + (int) dy;
                        windowManager.updateViewLayout(panel, params);
                        return true;
                    }
                    return false;
                default:
                    return false;
            }
        });

        windowManager.addView(panel, params);

        IntentFilter filter = new IntentFilter(AppLog.ACTION);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        AppLog.add("Overlay logs ready");
        refresh();
    }

    private void toggle() {
        if (Bot.running || Bot.starting) {
            AppLog.add("STOP pressed");
            Bot.running = false;
            Bot.starting = false;
            stopService(new Intent(this, CaptureService.class));
            refresh();
            return;
        }

        if (Bot.gestureService == null) {
            AppLog.add("ERROR: enable accessibility first");
            Toast.makeText(this, "Enable accessibility first", Toast.LENGTH_SHORT).show();
            openMain();
            return;
        }

        if (!Bot.hasCapturePermission()) {
            AppLog.add("ERROR: allow screen capture first");
            Toast.makeText(this, "Allow screen capture first", Toast.LENGTH_SHORT).show();
            openMain();
            return;
        }

        AppLog.add("START pressed");
        Bot.starting = true;
        Intent i = new Intent(this, CaptureService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
        else startService(i);
        refresh();
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void refresh() {
        if (button == null || logs == null) return;

        if (Bot.running) {
            button.setText("STOP");
            button.setBackgroundColor(0xCCEF4444);
        } else if (Bot.starting) {
            button.setText("STARTING");
            button.setBackgroundColor(0xCCF59E0B);
        } else {
            button.setText("START");
            button.setBackgroundColor(0xCC7C3AED);
        }

        logs.setText(AppLog.dump());
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(receiver);
        } catch (Exception ignored) {}

        if (windowManager != null && panel != null) {
            try {
                windowManager.removeView(panel);
            } catch (Exception ignored) {}
        }
        super.onDestroy();
    }
}
