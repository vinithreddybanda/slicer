package com.vinith.cometslicer;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.Locale;

public final class EventLog {
    public static final String ACTION_LOG_UPDATED = "com.vinith.cometslicer.LOG_UPDATED";
    public static final String ACTION_STATE_UPDATED = "com.vinith.cometslicer.STATE_UPDATED";

    private static final int MAX_LINES = 14;
    private static final ArrayDeque<String> lines = new ArrayDeque<>();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static Context appContext;

    private EventLog() {}

    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    public static synchronized void add(String message) {
        String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
        lines.addFirst(time + "  " + message);
        while (lines.size() > MAX_LINES) {
            lines.removeLast();
        }

        Context ctx = appContext;
        if (ctx != null) {
            main.post(() -> {
                ctx.sendBroadcast(new Intent(ACTION_LOG_UPDATED));
                ctx.sendBroadcast(new Intent(ACTION_STATE_UPDATED));
            });
        }
    }

    public static synchronized String dump() {
        if (lines.isEmpty()) return "Logs will appear here.";
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        return sb.toString().trim();
    }
}
