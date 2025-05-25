package com.landenlabs.routes.logger;

import android.app.Activity;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.Task;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.Trace;

public class Externals {

    public static Trace  init(@NonNull Activity activity, @Nullable ActivityResultLauncher<IntentSenderRequest> launcher ) {

        //  https://firebase.google.com/docs/perf-mon/console?platform=android&authuser=0
        Trace trace = FirebasePerformance.getInstance().newTrace("startup");
        trace.start();  // Firebase performance monitor

        // Reports:
        //    https://console.firebase.google.com/project/twc-auto-max1/crashlytics/app/android:com.landenlabs_dev.routes/issues
        // Logging:
        //    adb shell setprop log.tag.FirebaseCrashlytics DEBUG
        //    adb logcat -s FirebaseCrashlytics
        //    adb shell setprop log.tag.FirebaseCrashlytics INFO
       FirebaseCrashlytics.getInstance().log("onCreate");

        //
        // adb shell setprop log.tag.FA VERBOSE
        // adb shell setprop log.tag.FA-SVC VERBOSE
        // adb logcat -s FA FA-SVC
        Analytics.init(activity);

        // https://developer.android.com/guide/playcore/in-app-updates/kotlin-java#java
        AppUpdateManager appUpdateManager = AppUpdateManagerFactory.create(activity);
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();
        appUpdateInfoTask.addOnSuccessListener(appUpdateInfo -> {
            // This example applies an immediate update. To apply a flexible update
            // instead, pass in AppUpdateType.FLEXIBLE
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                if (launcher != null) {
                    // Request the update.
                    appUpdateManager.startUpdateFlowForResult(
                            // Pass the intent that is returned by 'getAppUpdateInfo()'.
                            appUpdateInfo,
                            // an activity result launcher registered via registerForActivityResult
                            launcher,
                            // Or pass 'AppUpdateType.FLEXIBLE' to newBuilder() for
                            // flexible updates.
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build());
                } else {
                    Toast.makeText(activity, "New version is available", Toast.LENGTH_LONG).show();
                }
            }
        });

        return trace;
    }

    public static void handleCrash(Throwable tr) {
        // See how to send custom parameters
        //   https://firebase.google.com/docs/crashlytics/customize-crash-reports?authuser=0&hl=en&platform=android
        //
        // Reports
        //  https://console.firebase.google.com/project/twc-auto-max1/crashlytics/app/android:com.landenlabs_dev.routes/issues
        FirebaseCrashlytics.getInstance().recordException(tr);
    }
}
