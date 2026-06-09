package com.vinith.cometslicer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.content.pm.ServiceInfo;
    import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    private static final String CHANNEL_ID = "capture";
    private static final int NOTIFICATION_ID = 10;

    private HandlerThread workerThread;
    private Handler worker;
    private ImageReader imageReader;
    private VirtualDisplay virtualDisplay;
    private MediaProjection projection;
    private TemplateMatcher matcher;

    private int captureWidth;
    private int captureHeight;
    private int densityDpi;
    private volatile boolean processing;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        matcher = new TemplateMatcher(this);

        workerThread = new HandlerThread("comet-screen-worker");
        workerThread.start();
        worker = new Handler(workerThread.getLooper());

        startProjection();
    }

    private void startProjection() {
        if (!BotState.hasProjection()) {
            stopSelf();
            return;
        }

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);

        BotState.screenWidth = metrics.widthPixels;
        BotState.screenHeight = metrics.heightPixels;

        captureWidth = Math.max(320, (int) (metrics.widthPixels * BotState.captureScale));
        captureHeight = Math.max(600, (int) (metrics.heightPixels * BotState.captureScale));
        densityDpi = metrics.densityDpi;

        projection = BotState.projectionManager.getMediaProjection(
                BotState.projectionResultCode,
                BotState.projectionData
        );

        imageReader = ImageReader.newInstance(captureWidth, captureHeight, PixelFormat.RGBA_8888, 2);

        virtualDisplay = projection.createVirtualDisplay(
                "comet-capture",
                captureWidth,
                captureHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                worker
        );

        imageReader.setOnImageAvailableListener(reader -> {
            if (!BotState.isRunning || processing) {
                Image skip = reader.acquireLatestImage();
                if (skip != null) skip.close();
                return;
            }

            Image image = reader.acquireLatestImage();
            if (image == null) return;

            processing = true;
            worker.post(() -> {
                try {
                    handleImage(image);
                } finally {
                    image.close();
                    processing = false;
                }
            });
        }, worker);
    }

    private void handleImage(Image image) {
        Image.Plane plane = image.getPlanes()[0];
        ByteBuffer buffer = plane.getBuffer();
        int pixelStride = plane.getPixelStride();
        int rowStride = plane.getRowStride();
        int rowPadding = rowStride - pixelStride * captureWidth;

        Bitmap padded = Bitmap.createBitmap(
                captureWidth + rowPadding / pixelStride,
                captureHeight,
                Bitmap.Config.ARGB_8888
        );
        padded.copyPixelsFromBuffer(buffer);
        Bitmap frame = Bitmap.createBitmap(padded, 0, 0, captureWidth, captureHeight);
        padded.recycle();

        Detection detection = matcher.findComet(frame);
        if (detection != null) {
            float realX = detection.x / BotState.captureScale;
            float realY = detection.y / BotState.captureScale;
            BotState.requestSlice(realX, realY);
        }
        frame.recycle();
    }

    @Override
    public void onDestroy() {
        BotState.isRunning = false;

        if (virtualDisplay != null) virtualDisplay.release();
        if (imageReader != null) imageReader.close();
        if (projection != null) projection.stop();

        if (workerThread != null) {
            workerThread.quitSafely();
        }

        super.onDestroy();
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen capture",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);

        return builder
                .setContentTitle("Comet Slicer running")
                .setContentText("Capturing screen and slicing detected comets")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .build();
    }
}
