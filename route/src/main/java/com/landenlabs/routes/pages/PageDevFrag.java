/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.pages;

import static android.content.Context.ACTIVITY_SERVICE;
import static com.landenlabs.routes.logger.Analytics.Event.PAGE_DEV;
import static com.landenlabs.routes.pages.PageUtils.FragUtilSettings.addCol;
import static com.landenlabs.routes.pages.PageUtils.FragUtilSettings.addRow;
import static com.landenlabs.routes.pages.PageUtils.FragUtilSettings.addRowBtn;
import static com.landenlabs.routes.pages.PageUtils.FragUtilSettings.setCard;
import static com.landenlabs.routes.utils.GpsUtils.getCurrentLocation;
import static com.landenlabs.routes.utils.GpsUtils.getLastKnownLocation;
import static com.landenlabs.routes.utils.GpsUtils.logString;
import static com.landenlabs.routes.utils.SysUtils.getAppVersion;
import static com.landenlabs.routes.utils.SysUtils.getLocationManager;
import static com.landenlabs.routes.utils.SysUtils.getServiceSafe;
import static com.landenlabs.routes.utils.SysUtils.restartApp;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.LocationManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.SystemClock;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;

import com.landenlabs.routes.R;
import com.landenlabs.routes.Record.RecordBase;
import com.landenlabs.routes.databinding.PageDevFragBinding;
import com.landenlabs.routes.logger.Analytics;
import com.landenlabs.routes.utils.GpsUtils;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Developer status/action page.
 */
public class PageDevFrag extends PageBaseFrag implements View.OnClickListener {
    private static final String DATE_FMT = "dd-MMM-yyyy";
    private static final boolean CLOSE = false;
    private static final boolean OPEN = true;
    private static final @ColorInt

    int CLICKABLE_BG_COLOR = 0x40FFFF00;
    private LinearLayout holderLv;

    private PageDevFragBinding binding;

    // ---------------------------------------------------------------------------------------------

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = PageDevFragBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        init(root);
        Analytics.send(PAGE_DEV);
        holderLv = binding.settingsHolder;
        binding.pageBackBtn.setOnClickListener(this);
        refresh();
        return root;
    }

    public void onClick(View view) {
        @IdRes int id = view.getId();
        if (id == R.id.page_backBtn) {
            viewModel.popBackPage();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.showNavBar(true);
        viewModel.showToolBar(false);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    protected void refresh() {
        holderLv.removeAllViews();
        fillAppSettingsCard(addCard(holderLv, CLOSE));
        // fillDeviceSettingsCard(addCard(holderLv, CLOSE));
        fillOSSettingsCard(addCard(holderLv, CLOSE));
        fillAboutSettingsCard(addCard(holderLv, CLOSE));
        fillNotificationCard(addCard(holderLv, CLOSE));
        fillActionSettingsCard(addCard(holderLv, CLOSE));
        fillDebugSettingsCard(addCard(holderLv, CLOSE));
        fillGpsSettingsCard(addCard(holderLv, CLOSE));
        debugShowMemory(CLOSE);
    }

    private void debugShowMemory(boolean opened) {
        fillMemorySettingsCard(addCard(holderLv, opened));
    }

    private GridLayout addCard(ViewGroup holder, boolean opened) {
        View cardVw = getLayoutInflater().inflate(R.layout.card_grid_expander, holder, false);
        final GridLayout gridVw = cardVw.findViewById(R.id.settings_grid);
        holder.addView(cardVw);
        ImageView expander = cardVw.findViewById(R.id.card_expander);
        cardVw.setOnClickListener(view -> {
            TransitionManager.beginDelayedTransition(gridVw, new AutoTransition());
            if (gridVw.getVisibility() == View.VISIBLE) {
                gridVw.setVisibility(View.GONE);
                expander.setImageResource(R.drawable.scroll_down);
            } else {
                gridVw.setVisibility(View.VISIBLE);
                expander.setImageResource(R.drawable.scroll_up);
            }
        });
        if (opened == CLOSE) {
            gridVw.setVisibility(View.GONE);
            expander.setImageResource(R.drawable.scroll_down);
        }
        return gridVw;
    }

    private void addClick(View viewAddClick, View.OnClickListener clickListener) {
        viewAddClick.setBackgroundColor(CLICKABLE_BG_COLOR);
        viewAddClick.setOnClickListener(clickListener);
    }

    private void addNotify(View viewAddClick, @StringRes int strRes) {
        viewAddClick.setBackgroundColor(CLICKABLE_BG_COLOR);
        // viewAddClick.setOnClickListener(view -> { ToastUtils.toastOver(root, R.id.notify_box1, getString(R.string.setting_about_traffic));});
    }

    // ---------------------------------------------------------------------------------------------

    private void fillAppSettingsCard(GridLayout card) {
        setCard(card, "app", R.drawable.logo);
        card.removeAllViews();

        boolean hasFineGPS = ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        addRow(card, "GPS Perm: ", hasFineGPS ? "Granted" : "Denied");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean hasBgGPS = ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            addRow(card, "Bg GPS: ", hasBgGPS ? "Granted" : "Denied");
        }
        if (Build.VERSION.SDK_INT > 32) {
            boolean hasFgSvc = ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.FOREGROUND_SERVICE) == PackageManager.PERMISSION_GRANTED;
            addRow(card, "Fg Srv: ", hasFgSvc ? "Granted" : "Denied");
        }
    }

    private void fillOSSettingsCard(GridLayout card) {
        setCard(card, "os", R.drawable.logo);
        card.removeAllViews();
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), PackageManager.GET_PERMISSIONS);

            addCol(card, "--- Permissions ---");
            for (String perm : pInfo.requestedPermissions) {
                boolean granted = ActivityCompat.checkSelfPermission(requireContext(), perm) == PackageManager.PERMISSION_GRANTED;
                addRow(card, perm.replaceAll("android.*permission.", ""), granted ? "Enabled" : "Disabled");
            }

            String appVersion = pInfo.versionName;
            long appInstallMillis = pInfo.lastUpdateTime;
            addRow(card, "App Version:", appVersion);
            addRow(card, "App Installed:", new DateTime(appInstallMillis).toString(DATE_FMT));
        } catch (Exception ignore) {
        }

        addRow(card, " ", "");    // filler
        try {
            DateTime buildDt = new DateTime(Long.parseLong(getString(R.string.routes_buildTimeMilli)));
            addRow(card, "Built:", buildDt.toString(DATE_FMT));
        } catch (Exception ignore) {
        }
        addRow(card, "OS Prod: ", Build.PRODUCT);
        addRow(card, "OS Vern: ", Build.VERSION.RELEASE);
        ActivityManager actMgr = getServiceSafe(requireActivity(), Context.ACTIVITY_SERVICE);
        ConfigurationInfo info = actMgr.getDeviceConfigurationInfo();
        addRow(card, "Open GL: ", "v" + info.getGlEsVersion());
    }

    private void fillNotificationCard(GridLayout holderGv) {
        setCard(holderGv, "notifications", R.drawable.logo);
        holderGv.removeAllViews();
        addRowBtn(holderGv, "Refresh", view -> fillNotificationCard(holderGv));
        /*
        addRowBtn(holderGv, "Clear",   view -> RecordBase.clearIt());
        addRow(holderGv, "Next", "Id=" + globalHolder.notifyId);
        for (NotifyBase.NotifyInfo info : NotifyBase.NOTIFY_INFOS.values()) {
            String id = info.type.toString() + " " + info.notifyId;
            addRow(holderGv, id, "Title=" + info.title);
            addRow(holderGv, id, "MsgId=" + info.tracker.msgId);
            addRow(holderGv, id, "Notified=" + NotifyBase.NotifyInfo.fmtMilli(info.notifiedMilli));
            addRow(holderGv, id, "Expires=" + NotifyBase.NotifyInfo.fmtMilli(info.expiresMilli));
            addRow(holderGv, id, "Dismissed=" + NotifyBase.NotifyInfo.fmtMilli(info.dismissedMilli));
        }
        */

        addRow(holderGv, "", "");
        addRowBtn(holderGv, "Clear", view -> RecordBase.clearActive(requireContext()));
        addRow(holderGv, "--Id--", "--Active--");
        NotificationManager nMgr = getServiceSafe(requireContext(), Context.NOTIFICATION_SERVICE);
        StatusBarNotification[] activeArray = nMgr.getActiveNotifications();
        for (StatusBarNotification active : activeArray) {
            String id = active.getNotification().category + " " + active.getId();
            addRow(holderGv, id, "Title=" + active.getNotification().extras.getString(Notification.EXTRA_TITLE));
            addRow(holderGv, id, "Msg=" + active.getNotification().extras.getString(Notification.EXTRA_MESSAGES));
        }
    }

    private void fillAboutSettingsCard(GridLayout card) {
        setCard(card, "about", R.drawable.logo);
        card.removeAllViews();
        addRow(card, "AppName", getString(requireContext().getApplicationInfo().labelRes));
        addRow(card, "AppVern", getAppVersion(requireContext()));
        addRow(card, "Package", requireContext().getPackageName());

        // addRow(card, "MapSDK", getString(R.string.mapsdk_version));
        addRow(card, "Pangea", com.weather.pangea.BuildConfig.PANGEA_VERSION_NAME);
        addRow(card, "Mapbox", com.mapbox.mapboxsdk.BuildConfig.MAPBOX_VERSION_STRING);

        addRow(card, "Java", getString(R.string.routes_javaVersion));
        addRow(card, "Compile", getString(R.string.routes_compileSdkVersion)); // requireContext().getApplicationInfo().compileSdkVersionCodename)
        addRow(card, "Target", getString(R.string.routes_targetSdkVersion));
        addRow(card, "MinSdk", String.valueOf(requireContext().getApplicationInfo().minSdkVersion));

        addRow(card, "Locale", Locale.getDefault().getDisplayName());
        addRow(card, "GMT offset", String.valueOf(TimeUnit.MILLISECONDS.toHours(TimeZone.getDefault().getRawOffset())));

        addRow(card, "Build", ALog.isDebugApp() ? "DEBUG" : "RELEASE");
        addRow(card, "Brand", Build.BRAND);
        addRow(card, "Model", Build.MODEL);
    }

    private void fillActionSettingsCard(GridLayout card) {
        setCard(card, "action", R.drawable.logo);
        card.removeAllViews();
        addRowBtn(card, "Open Language Settings", view -> startIntent(Settings.ACTION_LOCALE_SETTINGS));
        addRowBtn(card, "Open Date/Time Settings", view -> startIntent(Settings.ACTION_DATE_SETTINGS));
        // addRowBtn(card, "Open App Settings", view -> startIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)); // Auto
        addRowBtn(card, "Open App Settings", view -> startIntent(Settings.ACTION_APPLICATION_SETTINGS));

        addRowBtn(card, "Restart App", view -> {
            restartApp(requireContext());
        });
        addRowBtn(card, "Crash App", view -> {
            throw new NullPointerException("Requested Crash");
        });
        /*
        addRowBtn(card, "Clear Cache", view -> {
            requireContext().getCacheDir().delete();
            DataBaseModule.deleteDb(requireContext());
            tone(70, 80);
        });  // Does not remove SQL and may be doing nothing.
         */
    }

    private void  startIntent(String action) {
        try {
            startActivity(new Intent(action));
        } catch (Throwable tr) {
            Toast.makeText(requireContext(), "Sorry - action not available", Toast.LENGTH_LONG).show();
        }
    }

    private void fillDebugSettingsCard(GridLayout holderGv) {
        setCard(holderGv, "debug", R.drawable.logo);
        holderGv.removeAllViews();
        // Debug - show screen size.
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        addRow(holderGv, "Width:",
                String.format(Locale.US, "%d(px) %.0f(dp)", metrics.widthPixels, metrics.widthPixels / metrics.density));
        addRow(holderGv, "Height:",
                String.format(Locale.US, "%d(px) %.0f(dp)", metrics.heightPixels, metrics.heightPixels / metrics.density));
        addRow(holderGv, "Px per Dp:",
                String.format(Locale.US, "%.1f", metrics.density));
    }

    private void fillGpsSettingsCard(GridLayout holderGv) {
        setCard(holderGv, "gps", R.drawable.logo);
        holderGv.removeAllViews();
        addRowBtn(holderGv, "Force GPS",   view -> {
            GpsUtils.requestLocation(requireContext()); fillGpsSettingsCard(holderGv);} );
        addRow(holderGv, "Last Loc", logString(getCurrentLocation(requireContext())));
        addRow(holderGv, "Last GPS", logString(getLastKnownLocation(getLocationManager(requireContext()), LocationManager.GPS_PROVIDER)));

        boolean gpsFine = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        addRow(holderGv, "Perm Fine", String.valueOf(gpsFine));
        boolean gpsCoarse = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        addRow(holderGv, "Perm Coarse", String.valueOf(gpsCoarse));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            boolean gpsBg = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            addRow(holderGv, "Perm Bg Gps", String.valueOf(gpsBg));
        }
        List<String> providers = getLocationManager(requireContext()).getProviders(true);
        for (String provider : providers) {
            addRow(holderGv, "Provider", provider);
        }
    }

    private void fillPerformanceSettingsCard(GridLayout holderGv) {
        setCard(holderGv, "performance", R.drawable.logo);
        holderGv.removeAllViews();
        // TODO - measure time to load various weather objects.
        // addCol(holderGv, getString(R.string.twc_performance));

        long readKB = TrafficStats.getUidRxBytes(android.os.Process.myUid()) / 1024;
        //    addNotify(addRow(holderGv, "Net Rcv(KB):", String.valueOf(readKB)), R.string.setting_about_traffic);
        Duration elapsedDuration = new Duration(SystemClock.elapsedRealtime());
        addRow(holderGv, "Boot Hrs:", String.format(Locale.US, "%.1f", elapsedDuration.getStandardMinutes() / 60f));
    }

    private void fillMemorySettingsCard(GridLayout holderGv) {
        setCard(holderGv, "memory", R.drawable.logo);
        holderGv.removeAllViews();
        Context context = requireContext();
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        Debug.MemoryInfo[] memInfos = activityManager.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});
        Debug.MemoryInfo memoryInfo = memInfos[0];
        addRow(holderGv, "javaHeap=", memoryInfo.getMemoryStat("summary.java-heap"));
        addRow(holderGv, "nativeHeap=", memoryInfo.getMemoryStat("summary.native-heap"));

        addRow(holderGv, "graphics=", memoryInfo.getMemoryStat("summary.graphics"));
        addCol(holderGv, " [ Clear Memory ]").setOnClickListener(view -> {
            // Runtime.getRuntime().gc();
            System.gc();
            debugShowMemory(OPEN);
        });
    }
}