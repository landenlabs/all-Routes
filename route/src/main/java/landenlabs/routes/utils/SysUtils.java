/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.utils;

import static android.content.Context.LOCATION_SERVICE;

import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.text.SpannableStringBuilder;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.FragmentActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.lang.ref.WeakReference;
import java.util.Objects;

import landenlabs.wx_lib_data.logger.ALog;


/**
 * Miscellaneous system utility functions,.
 */
public class SysUtils {

    @SuppressWarnings("unchecked")
    @NonNull
    public static <T> T getServiceSafe(@NonNull Context context, @NonNull String service) {
        //noinspection unchecked
        return (T) Objects.requireNonNull(context.getSystemService(service));
    }

    public static boolean hasRef(WeakReference<?> ref) {
        return ref != null && ref.get() != null;
    }

    public static LocationManager getLocationManager(@NonNull Context appContext) {
        return getServiceSafe(appContext, LOCATION_SERVICE);
    }
    public static NotificationManager getNotificationManager(@NonNull Context appContext) {
        return SysUtils.getServiceSafe(appContext, Context.NOTIFICATION_SERVICE);
    }
    public static FusedLocationProviderClient getFusedLocation(@NonNull Context appContext) {
        return LocationServices.getFusedLocationProviderClient(appContext);
    }

    public static String getAppVersion(@NonNull Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            return "NoVersion";
        }
    }

    // See duplicate in UtilSpan SSJoin
    public static CharSequence joinCS(CharSequence... csList) {
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        for (CharSequence cs : csList) {
            ssb.append(cs);
        }
        return ssb;
    }

    public static int strToInt(CharSequence str, int def) {
        try {
            return Integer.parseInt(str.toString());
        } catch (Exception ex) {
            return def;
        }
    }
    public static long strToLong(CharSequence str, long def) {
        try {
            return Long.parseLong(str.toString());
        } catch (Exception ex) {
            return def;
        }
    }
    public static double strToDouble(CharSequence str, double def) {
        try {
            return Double.parseDouble(str.toString());
        } catch (Exception ex) {
            return def;
        }
    }

    public static void restartApp(Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(context.getPackageName());
        ComponentName componentName = intent.getComponent();
        Intent mainIntent = Intent.makeRestartActivityTask(componentName);
        context.startActivity(mainIntent);
        Runtime.getRuntime().exit(0);
    }

    /**
     * @return True if network available (cellular, wifi or ethernet)
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    public static boolean isNetworkAvailable2(@NonNull Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            try {
                // @SuppressLint("MissingPermission")
                NetworkCapabilities capabilities =
                        connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
                if (capabilities != null) {
                /*
                    TRANSPORT_CELLULAR,
                    TRANSPORT_WIFI,
                    TRANSPORT_BLUETOOTH,
                    TRANSPORT_ETHERNET,
                    TRANSPORT_VPN,
                    TRANSPORT_WIFI_AWARE,
                    TRANSPORT_LOWPAN,
                    TRANSPORT_TEST,
                 */
                    if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true;
                    } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                        return true;
                    }
                }
            } catch (Exception ex) {
                // AppCenter crash for Android 11 - SecurityException: Package android does not belong to 10346
                ALog.e.tagMsg("isNetworkAvailable", "error ", ex);
            }

            /*
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()) {
                return true;
            }
            MLog.w.tagMsg(TAG, "Network unavailable ", capabilities, " ", netInfo);
             */
            return false;
        }
        ALog.w.tagMsg("isNetworkAvailable", "Network offline ");
        return false;
    }

    // https://stackoverflow.com/questions/58320487/using-fragmentcontainerview-with-navigation-component
    private static final boolean NAV_FRAG_BUG_WORK_AROUND = true;

    public static NavController getNavController(FragmentActivity context, @IdRes int fragRes) {
        if (NAV_FRAG_BUG_WORK_AROUND) {
            NavHostFragment navFrag = (NavHostFragment) context.getSupportFragmentManager().findFragmentById(fragRes);
            if (navFrag != null) {
                return navFrag.getNavController();
            }
        }
        return Navigation.findNavController(context, fragRes);
    }
}
