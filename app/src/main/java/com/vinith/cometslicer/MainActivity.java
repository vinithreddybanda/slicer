package com.vinith.cometslicer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_CAPTURE = 5101;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private TextView status;
    private TextView logBox;

    private final Runnable refresher = new Runnable() {
        @Override
        public void run() {
            refresh();
            ui.postDelayed(this, 700);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.init(this);
        AppLog.add("Main opened");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 54, 36, 36);

        TextView title = new TextView(this);
        title.setText("Comet Slicer v3");
        title.setTextSize(26);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, 20, 0, 20);

        Button overlay = new Button(this);
        overlay.setText("1. Enable overlay permission");
        overlay.setOnClickListener(v -> openOverlayPermission());

        Button gesture = new Button(this);
        gesture.setText("2. Enable gesture accessibility");
        gesture.setOnClickListener(v -> {
            AppLog.add("Opening accessibility settings");
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        Button capture = new Button(this);
        capture.setText("3. Allow screen capture");
        capture.setOnClickListener(v -> requestCapturePermission());

        Button show = new Button(this);
        show.setText("4. Show floating start/logs");
        show.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                openOverlayPermission();
                return;
            }
            startService(new Intent(this, OverlayLogService.class));
            AppLog.add("Floating panel requested");
            Toast.makeText(this, "Floating panel shown", Toast.LENGTH_SHORT).show();
            refresh();
        });

        logBox = new TextView(this);
        logBox.setTextSize(12);
        logBox.setPadding(0, 18, 0, 0);

        root.addView(title);
        root.addView(status);
        root.addView(overlay);
        root.addView(gesture);
        root.addView(capture);
        root.addView(show);
        root.addView(logBox);
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33) {
            try {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 44);
            } catch (Throwable t) {
                AppLog.add("Notification permission skipped: " + t.getClass().getSimpleName());
            }
        }

        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.removeCallbacks(refresher);
        ui.post(refresher);
    }

    @Override
    protected void onPause() {
        ui.removeCallbacks(refresher);
        super.onPause();
    }

    private void openOverlayPermission() {
        try {
            if (Settings.canDrawOverlays(this)) {
                AppLog.add("Overlay permission already OK");
                Toast.makeText(this, "Overlay already enabled", Toast.LENGTH_SHORT).show();
                return;
            }
            AppLog.add("Opening overlay permission");
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivity(intent);
        } catch (Throwable t) {
            AppLog.add("ERROR overlay settings: " + t.getClass().getSimpleName());
            Toast.makeText(this, "Could not open overlay settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestCapturePermission() {
        try {
            MediaProjectionManager manager =
                    (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            if (manager == null) {
                AppLog.add("ERROR: MediaProjectionManager null");
                Toast.makeText(this, "Screen capture not available", Toast.LENGTH_SHORT).show();
                return;
            }
            Bot.projectionManager = manager;
            AppLog.add("Requesting screen capture permission");
            startActivityForResult(manager.createScreenCaptureIntent(), REQ_CAPTURE);
        } catch (Throwable t) {
            AppLog.add("ERROR capture request: " + t.getClass().getSimpleName());
            Toast.makeText(this, "Could not request screen capture", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bot.projectionResultCode = resultCode;
            Bot.projectionData = data;
            AppLog.add("Screen capture permission saved");
            Toast.makeText(this, "Screen capture ready", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQ_CAPTURE) {
            AppLog.add("ERROR: screen capture permission denied");
        }
        refresh();
    }

    private void refresh() {
        if (status == null || logBox == null) return;

        String botState = Bot.running ? "RUNNING" : (Bot.starting ? "STARTING" : "STOPPED");
        boolean overlayOk = false;
        try {
            overlayOk = Settings.canDrawOverlays(this);
        } catch (Throwable ignored) {}

        status.setText(
                "Overlay: " + (overlayOk ? "OK" : "missing") +
                "\nAccessibility: " + (Bot.gestureService != null ? "OK" : "missing") +
                "\nScreen capture: " + (Bot.hasCapturePermission() ? "OK" : "missing") +
                "\nBot: " + botState +
                "\n\nThis v3 build avoids startup receivers/application hooks to prevent auto-close."
        );
        logBox.setText("Logs:\n" + AppLog.dump());
    }
}
