package com.vinith.cometslicer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class CaptureService extends Service {
    private static final String CHANNEL = "capture";
    private static final int NOTIF = 910;

    private HandlerThread thread;
    private Handler worker;
    private ImageReader reader;
    private VirtualDisplay display;
    private MediaProjection projection;
    private TemplateMatcher matcher;

    private int width;
    private int height;
    private volatile boolean busy = false;
    private volatile boolean started = false;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.init(this);
        AppLog.add("Capture service onCreate");

        createChannel();

        try {
            Notification n = notification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(NOTIF, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                startForeground(NOTIF, n);
            }
            AppLog.add("Foreground notification started");
        } catch (Throwable t) {
            AppLog.add("ERROR: foreground failed " + t.getClass().getSimpleName());
            stopSelf();
            return;
        }

        thread = new HandlerThread("capture-worker");
        thread.start();
        worker = new Handler(thread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (started) return START_NOT_STICKY;
        started = true;
        worker.post(this::startCaptureSafe);
        return START_NOT_STICKY;
    }

    private void startCaptureSafe() {
        try {
            startCapture();
        } catch (Throwable t) {
            AppLog.add("ERROR: start capture " + t.getClass().getSimpleName());
            AppLog.add("ERROR detail: " + AppLog.safe(t.getMessage()));
            Bot.running = false;
            Bot.starting = false;
            stopSelf();
        }
    }

    private void startCapture() {
        if (!Bot.hasCapturePermission()) {
            AppLog.add("ERROR: capture permission missing");
            Bot.running = false;
            Bot.starting = false;
            stopSelf();
            return;
        }

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);

        Bot.screenWidth = dm.widthPixels;
        Bot.screenHeight = dm.heightPixels;

        width = Math.max(320, Math.round(dm.widthPixels * Bot.scale));
        height = Math.max(540, Math.round(dm.heightPixels * Bot.scale));

        AppLog.add("Screen " + dm.widthPixels + "x" + dm.heightPixels);
        AppLog.add("Capture " + width + "x" + height + " scale=" + Bot.scale);

        matcher = new TemplateMatcher(this);

        projection = Bot.projectionManager.getMediaProjection(Bot.projectionResultCode, Bot.projectionData);
        if (projection == null) {
            AppLog.add("ERROR: projection null. Allow capture again.");
            Bot.clearCapturePermission();
            Bot.running = false;
            Bot.starting = false;
            stopSelf();
            return;
        }

        projection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                AppLog.add("Projection stopped");
                Bot.clearCapturePermission();
                Bot.running = false;
                Bot.starting = false;
                stopSelf();
            }
        }, worker);

        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);

        display = projection.createVirtualDisplay(
                "comet-capture",
                width,
                height,
                dm.densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(),
                null,
                worker
        );

        if (display == null) {
            AppLog.add("ERROR: virtual display null");
            Bot.running = false;
            Bot.starting = false;
            stopSelf();
            return;
        }

        reader.setOnImageAvailableListener(this::onImage, worker);

        Bot.starting = false;
        Bot.running = true;
        AppLog.add("Capture RUNNING");
    }

    private void onImage(ImageReader imageReader) {
        if (!Bot.running || busy) {
            Image drop = imageReader.acquireLatestImage();
            if (drop != null) drop.close();
            return;
        }

        Image image = imageReader.acquireLatestImage();
        if (image == null) return;

        busy = true;
        try {
            process(image);
        } catch (Throwable t) {
            AppLog.add("ERROR: process frame " + t.getClass().getSimpleName());
        } finally {
            image.close();
            busy = false;
        }
    }

    private void process(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * width;

        Bitmap padded = Bitmap.createBitmap(
                width + rowPadding / pixelStride,
                height,
                Bitmap.Config.ARGB_8888
        );
        padded.copyPixelsFromBuffer(buffer);

        Bitmap frame = Bitmap.createBitmap(padded, 0, 0, width, height);
        padded.recycle();

        Detection d = matcher.find(frame);
        if (d != null) {
            float realX = d.x / Bot.scale;
            float realY = d.y / Bot.scale;
            Bot.slice(realX, realY);
        }

        frame.recycle();
    }

    @Override
    public void onDestroy() {
        AppLog.add("Capture service onDestroy");
        Bot.running = false;
        Bot.starting = false;

        if (reader != null) {
            try {
                reader.setOnImageAvailableListener(null, null);
                reader.close();
            } catch (Throwable ignored) {}
        }

        if (display != null) {
            try {
                display.release();
            } catch (Throwable ignored) {}
        }

        if (projection != null) {
            try {
                projection.stop();
            } catch (Throwable ignored) {}
        }

        // Permission token is no longer reusable after projection stops on many Android versions.
        Bot.clearCapturePermission();

        if (thread != null) {
            thread.quitSafely();
        }

        super.onDestroy();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(
                    CHANNEL,
                    "Screen capture",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(c);
        }
    }

    private Notification notification() {
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);

        return b.setContentTitle("Comet Slicer")
                .setContentText("Screen capture is running")
                .setSmallIcon(android.R.drawable.ic_menu_view)
                .setOngoing(true)
                .build();
    }
}
