package com.vinith.cometslicer;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public final class AppLog {
    public static final String ACTION = "com.vinith.cometslicer.LOG";
    private static final int MAX = 18;
    private static final ArrayDeque<String> lines = new ArrayDeque<>();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static Context app;

    private AppLog() {}

    public static void init(Context context) {
        app = context.getApplicationContext();
    }

    public static synchronized void add(String msg) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        lines.addFirst(time + "  " + msg);
        while (lines.size() > MAX) lines.removeLast();

        Context ctx = app;
        if (ctx != null) {
            main.post(() -> ctx.sendBroadcast(new Intent(ACTION)));
        }
        android.util.Log.d("CometSlicer", msg);
    }

    public static synchronized String dump() {
        if (lines.isEmpty()) return "No logs yet.";
        StringBuilder out = new StringBuilder();
        for (String l : lines) out.append(l).append('\n');
        return out.toString().trim();
    }

    public static String safe(String value) {
        return value == null ? "no message" : value;
    }
}
