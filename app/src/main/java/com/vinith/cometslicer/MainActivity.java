package com.vinith.cometslicer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        Button capture = new Button(this);
        capture.setText("3. Allow screen capture");
        capture.setOnClickListener(v -> requestScreenCapture());

        Button startOverlay = new Button(this);
        startOverlay.setText("Show floating start/stop button");
        startOverlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlay();
                return;
            }
            startService(new Intent(this, OverlayButtonService.class));
            Toast.makeText(this, "Overlay button shown", Toast.LENGTH_SHORT).show();
            refreshStatus();
        });

        root.addView(title);
        root.addView(status);
        root.addView(overlay);
        root.addView(access);
        root.addView(capture);
        root.addView(startOverlay);
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 44);
        }
        refreshStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private void requestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Overlay already enabled", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestScreenCapture() {
        MediaProjectionManager manager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        BotState.projectionManager = manager;
        startActivityForResult(manager.createScreenCaptureIntent(), REQ_MEDIA_PROJECTION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_MEDIA_PROJECTION && resultCode == RESULT_OK && data != null) {
            BotState.projectionResultCode = resultCode;
            BotState.projectionData = data;
            Toast.makeText(this, "Screen capture ready", Toast.LENGTH_SHORT).show();
        }
        refreshStatus();
    }

    private void refreshStatus() {
        String text =
                "Overlay: " + (Settings.canDrawOverlays(this) ? "OK" : "not enabled") +
                "\nAccessibility: " + (BotState.accessibility != null ? "OK" : "not enabled / not connected yet") +
                "\nScreen capture: " + (BotState.hasProjection() ? "OK" : "not allowed") +
                "\n\nOpen your game, tap the floating button, and it will swipe only matched comet templates. Skulls are ignored.";
        status.setText(text);
    }
}
