package com.webtemplate;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Custom Application class that handles uncaught exceptions.
 * When a crash occurs, copies the stack trace to clipboard and shows a toast.
 */
public class CrashApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        setupCrashHandler();
    }

    private void setupCrashHandler() {
        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            // Get the full stack trace
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            throwable.printStackTrace(pw);
            String stackTrace = sw.toString();

            // Create the crash log
            String crashLog = "===== APPY GENERATED APP CRASH LOG =====\n" +
                    "Thread: " + thread.getName() + "\n" +
                    "Exception: " + throwable.getClass().getSimpleName() + "\n" +
                    "Message: " + throwable.getMessage() + "\n\n" +
                    "Stack Trace:\n" + stackTrace;

            // Try to copy to clipboard
            try {
                // We need to run on main thread for clipboard access
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (clipboard != null) {
                            ClipData clip = ClipData.newPlainText("Crash Log", crashLog);
                            clipboard.setPrimaryClip(clip);
                            Toast.makeText(CrashApplication.this,
                                    "Crash log copied to clipboard. Please share it with the developer.",
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        // Ignore clipboard errors
                    }
                });

                // Give time for the toast to show
                Thread.sleep(2000);
            } catch (Exception e) {
                // Ignore
            }

            // Call the default handler to show the crash dialog
            if (defaultHandler != null) {
                defaultHandler.uncaughtException(thread, throwable);
            } else {
                System.exit(1);
            }
        });
    }
}
