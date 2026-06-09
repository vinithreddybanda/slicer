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
    private static final int REQ_CAPTURE = 5101;
    private TextView status;
    private TextView logBox;

    private final BroadcastReceiver logReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refresh();
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
        title.setText("Comet Slicer v2");
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
            AppLog.add("Floating button/log panel requested");
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
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 44);
        }
        refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(AppLog.ACTION);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(logReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(logReceiver, filter);
        }
    }

    @Override
    protected void onStop() {
        try {
            unregisterReceiver(logReceiver);
        } catch (Exception ignored) {}
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void openOverlayPermission() {
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
    }

    private void requestCapturePermission() {
        MediaProjectionManager manager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        Bot.projectionManager = manager;
        AppLog.add("Requesting screen capture permission");
        startActivityForResult(manager.createScreenCaptureIntent(), REQ_CAPTURE);
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
        status.setText(
                "Overlay: " + (Settings.canDrawOverlays(this) ? "OK" : "missing") +
                "\nAccessibility: " + (Bot.gestureService != null ? "OK" : "missing") +
                "\nScreen capture: " + (Bot.hasCapturePermission() ? "OK" : "missing") +
                "\nBot: " + botState +
                "\n\nNote: after STOP, Android may require screen capture permission again."
        );
        logBox.setText("Logs:\n" + AppLog.dump());
    }
}
