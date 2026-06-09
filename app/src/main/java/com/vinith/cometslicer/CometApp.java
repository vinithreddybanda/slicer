package com.vinith.cometslicer;

import android.app.Application;

public class CometApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.init(this);

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            AppLog.add("CRASH: " + throwable.getClass().getSimpleName());
            AppLog.add("CRASH detail: " + AppLog.safe(throwable.getMessage()));
            android.util.Log.e("CometSlicer", "Uncaught crash", throwable);
        });
    }
}
