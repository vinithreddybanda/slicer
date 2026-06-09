package com.vinith.cometslicer;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final int REQ_MEDIA_PROJECTION = 701;

    private TextView status;
    private TextView logs;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventLog.init(this);
        EventLog.add("App opened");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(36, 56, 36, 36);

        TextView title = new TextView(this);
        title.setText("Comet Slicer");
        title.setTextSize(26);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);

        status = new TextView(this);
        status.setTextSize(15);
        status.setPadding(0, 22, 0, 22);

        Button overlay = new Button(this);
        overlay.setText("1. Enable overlay button");
        overlay.setOnClickListener(v -> requestOverlay());

        Button access = new Button(this);
        access.setText("2. Enable accessibility gestures");
        access.setOnClickListener(v -> {
            EventLog.add("Opening accessibility settings");
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        Button capture = new Button(this);
        capture.setText("3. Allow screen capture");
        capture.setOnClickListener(v -> requestScreenCapture());

        Button startOverlay = new Button(this);
        startOverlay.setText("Show floating start/stop button + logs");
        startOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlay();
                return;
            }
            startService(new Intent(this, OverlayButtonService.class));
            EventLog.add("Overlay button shown");
            Toast.makeText(this, "Overlay button shown", Toast.LENGTH_SHORT).show();
            refreshStatus();
        });

        logs = new TextView(this);
        logs.setTextSize(12);
        logs.setPadding(0, 20, 0, 0);

        root.addView(title);
        root.addView(status);
        root.addView(overlay);
        root.addView(access);
        root.addView(capture);
        root.addView(startOverlay);
        root.addView(logs);
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 44);
        }
        refreshStatus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(EventLog.ACTION_LOG_UPDATED);
        filter.addAction(EventLog.ACTION_STATE_UPDATED);
        registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(receiver);
        } catch (Exception ignored) {}
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void requestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            EventLog.add("Opening overlay permission");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            EventLog.add("Overlay permission already OK");
            Toast.makeText(this, "Overlay already enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestScreenCapture() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        BotState.projectionManager = manager;
        EventLog.add("Requesting screen capture permission");
        startActivityForResult(manager.createScreenCaptureIntent(), REQ_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_MEDIA_PROJECTION && resultCode == RESULT_OK && data != null) {
            BotState.projectionResultCode = resultCode;
            BotState.projectionData = data;
            EventLog.add("Screen capture permission saved");
            Toast.makeText(this, "Screen capture ready", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQ_MEDIA_PROJECTION) {
            EventLog.add("ERROR: screen capture permission denied");
        }
        refreshStatus();
    }

    private void refreshStatus() {
        String text =
                "Overlay: " + (Settings.canDrawOverlays(this) ? "OK" : "not enabled") +
                "\nAccessibility: " + (BotState.accessibility != null ? "OK" : "not enabled / reconnect app after enabling") +
                "\nScreen capture: " + (BotState.hasProjection() ? "OK" : "not allowed") +
                "\nBot: " + (BotState.isRunning ? "RUNNING" : (BotState.isStarting ? "STARTING" : "STOPPED")) +
                "\n\nImportant: Android allows one MediaProjection session per permission. After STOP/crash, tap Allow screen capture again.";
        status.setText(text);
        logs.setText("Logs:\n" + EventLog.dump());
    }
}
