/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.landenlabs.routes.R;
import com.landenlabs.routes.logger.Externals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Save crash report and show it on next run of app.
 */
@SuppressWarnings({"Convert2Lambda", "StringBufferReplaceableByString"})
public class CatchAppExceptions implements Thread.UncaughtExceptionHandler {

    private static final String CRASH_REPORT = "CrashReport";
    private static final String START_MILLI = "StartMilli";
    private final FragmentActivity context;
    private final SharedPreferences prefs;
    private final Thread.UncaughtExceptionHandler originalHandler;

    // ---------------------------------------------------------------------------------------------

    /**
     * Creates a reporter instance
     *
     * @throws NullPointerException if the parameter is null
     */
    public CatchAppExceptions(FragmentActivity context) throws NullPointerException {
        originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        this.context = context;

        StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskReads();
        StrictMode.allowThreadDiskWrites();
        // prefs = PrefUtil.getSharedPref(context, CRASH_REPORT);
        prefs = context.getPreferences( Context.MODE_PRIVATE);
        prefs.edit().putLong(START_MILLI, System.currentTimeMillis()).apply();

        // if (AppCfg.isDebug()) {
            String msg = prefs.getString(CRASH_REPORT, "");
            ALog.i.tagMsg(this, "Crash report loaded len=", (msg == null) ? -1 : msg.length());
            if (!TextUtils.isEmpty(msg)) {
                prefs.edit().remove(CRASH_REPORT).apply();
                showCrashDialog(msg);
            }
        // }
        StrictMode.setThreadPolicy(policy);
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {

        try {
            String stackTrace = Log.getStackTraceString(ex);
            Log.e("AllThreadPenalty", "Exception", ex);
            saveCrashReport(thread, ex);

            Externals.handleCrash(ex);

            if (originalHandler != null) {
                originalHandler.uncaughtException(thread, ex);
            }
        } catch (Throwable tr) {
            // ignore
            ALog.none.tagMsg(this, tr);
        }

        // Auto restart app on crash if 10 seconds have expired since startup to avoid case
        // where failure goes into a rapid crash/start cycle.
        long startMilli = prefs.getLong(START_MILLI, 0L);
        if (System.currentTimeMillis() - startMilli > TimeUnit.SECONDS.toMillis(10)) {
            // restartApp(context);
        }
    }

    /**
     * Save stack trace in preferences to display on next app run.
     */
    private void saveCrashReport(Thread thread, Throwable ex) {
        try {
            StringBuilder report = new StringBuilder();
            report.append("Thread:")
                    .append(thread.getName())
                    .append("@")
                    .append(thread.getId())
                    .append("\n");
            report.append("Exception:\n")
                    .append(ex.getMessage())
                    .append("\nCause\n")
                    .append(ex.getCause())
                    .append("\n");
            report.append("Stack:\n");
            final Writer result = new StringWriter();
            final PrintWriter printWriter = new PrintWriter(result);
            ex.printStackTrace(printWriter);
            report.append(result);
            printWriter.close();
            report.append('\n');

            StrictMode.ThreadPolicy policy = StrictMode.allowThreadDiskWrites();
            String msg = report.toString();
            prefs.edit().putString(CRASH_REPORT, msg).commit();
            ALog.i.tagMsg(this, "Crash report saved, len=", msg.length() );
            StrictMode.setThreadPolicy(policy);
        } catch (Throwable ignore) {
            ALog.none.tagMsg(this, ignore);
        }
    }

    /**
     * Show crash trace in Alert dialog, pause main_menu app from running.
     */
    private void showCrashDialog(final String msg) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setView(R.layout.crash_dlg);
        AlertDialog dialog = builder.show();

        ((TextView) dialog.findViewById(R.id.crash_text)).setText(msg);

        dialog.findViewById(R.id.crash_cont_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.cancel();
                        // System.exit(0);
                    }
                }
        );

        dialog.findViewById(R.id.crash_email_btn).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent sendIntent = new Intent(Intent.ACTION_SEND);
                            StringBuilder body = new StringBuilder("Trace\n\n")
                                    .append(msg);
                            sendIntent.setType("message/rfc822");
                            sendIntent.putExtra(Intent.EXTRA_TEXT, body.toString());
                            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Crash report");
                            sendIntent.setType("message/rfc822");
                            context.startActivity(sendIntent);
                            // startIntentWithResult(sendIntent);
                        } catch (Throwable tr) {
                            ALog.none.tagMsg(this, tr);
                        }
                        dialog.cancel();
                        // System.exit(0);
                    }
                }
        );
        // Looper.loop();
    }

    // This current does not work, the goal is to get email program to return to main app after email sent. 
    //  context.startActivityForResult(Intent.createChooser(sendIntent, "Choose an Email client :"), 800);                           );
    public static void startIntentWithResult(FragmentActivity context, Intent intent) {
        final ActivityResultLauncher<Intent> launcher = context.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // launcher.unregister();
                    } else {
                    }
                });
        launcher.launch(intent);
    }
}
