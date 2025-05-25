/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.pages;

import static android.location.LocationManager.GPS_PROVIDER;
import static com.landenlabs.routes.data.RouteSettings.DAY_TM_FMT;
import static com.landenlabs.routes.data.TrackUtils.RandomInt;
import static com.landenlabs.routes.data.TrackUtils.createRandomTrack;
import static com.landenlabs.routes.logger.Analytics.Event.PAGE_SUMMARY;
import static com.landenlabs.routes.utils.FmtTime.ageMilli;
import static com.landenlabs.routes.utils.FmtTime.formatAge;
import static com.landenlabs.routes.utils.GpsUtils.getCurrentLocation;
import static com.landenlabs.routes.utils.GpsUtils.getGpsProvider;
import static com.landenlabs.routes.utils.SysUtils.getLocationManager;
import static com.landenlabs.routes.utils.SysUtils.isNetworkAvailable2;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.CheckedTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import com.landenlabs.routes.R;
import com.landenlabs.routes.Record.RecordService;
import com.landenlabs.routes.data.GpsPoint;
import com.landenlabs.routes.data.Track;
import com.landenlabs.routes.databinding.PageSummaryFragBinding;
import com.landenlabs.routes.events.EventBase;
import com.landenlabs.routes.logger.Analytics;
import com.landenlabs.routes.pages.PageUtils.ToastUtils;
import com.landenlabs.routes.utils.UnitSpeed;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Home Summary page.
 */

public class PageSummaryFrag extends PageBaseFrag implements View.OnClickListener {
    private PageSummaryFragBinding binding;

    // ---------------------------------------------------------------------------------------------

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = PageSummaryFragBinding.inflate(inflater, container, false);

        init(binding.getRoot());
        Analytics.send(PAGE_SUMMARY);

        binding.sumPerm.setOnClickListener(this);
        binding.sumGps.setOnClickListener(this);
        binding.sumNetwork.setOnClickListener(this);
        binding.dbgAddTracks.setOnClickListener(this);
        binding.dbgClearTracks.setOnClickListener(this);

        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.showNavBar(true);
        viewModel.showToolBar(true);
        getGlobal().addEventListener(this.getClass().getSimpleName(), this);
        repeatRefresh();
    }

    private void repeatRefresh() {
        if (root != null) {
            refresh();
            root.postDelayed(this::repeatRefresh, 5000);
        }
    }

    @Override
    public void onPause() {
        getGlobal().removeEventListener(this.getClass().getSimpleName());
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // Called when orientation is changed. Need to refresh the TableLayout.
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        refresh();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.menu_settings) {
            navigateTo(R.id.page_settings);
        }
        return false;
    }

    @Override
    public void onEvent(@Nullable EventBase event) {
        super.onEvent(event);
        refresh();
    }

    private static final int[] AGE_COLORS = {Color.TRANSPARENT, 0x80FFFF00, 0x80FF8000,  0x80FF0000 };
    protected void refresh() {
        if (root == null || !root.isAttachedToWindow() || root.getWindowVisibility() != View.VISIBLE) {
            ALog.d.tagMsg(this, "Refresh ignored");
            return;
        }
        ALog.d.tagMsg(this, "Refresh");
        Location loc = getCurrentLocation(requireContext());
        binding.sumTime.setText( DateTime.now().toString(DAY_TM_FMT));
        binding.sumTimeGps.setText(loc == null ? "none" : formatAge(loc.getTime(),  "%1$d %2$s ago"));
        if (loc != null)
            setBgColor(binding.sumTimeGps, TimeUnit.MILLISECONDS.toMinutes(ageMilli(loc.getTime())) / 10, AGE_COLORS);

        boolean gpsFine = ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean gpsCoarse = ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        binding.sumPerm.setChecked(gpsFine | gpsCoarse);
        flash(binding.sumPerm);
        binding.sumGps.setChecked(GPS_PROVIDER.equals(getGpsProvider(requireContext(), null)));
        binding.sumNetwork.setChecked(isNetworkAvailable2(requireContext()));
        flash(binding.sumNetwork);

        Location currentLocation = getCurrentLocation(requireContext());
        if (currentLocation != null) {
            binding.sumSpeed.setText(String.format(Locale.US, getString(R.string.speed_fmt),
                    UnitSpeed.METERS_PER_SECOND.toMilesPerHour(currentLocation.getSpeed())));
        } else {
            binding.sumSpeed.setText(R.string.speed_parked);
        }

        binding.sumLoc.setText(loc == null ? "No location" : String.format(Locale.US, "%.3f,%.3f", loc.getLatitude(), loc.getLongitude()));

        binding.sumRoutes.setText(String.format(Locale.US, getString(R.string.routes_fmt), viewModel.globalHolder.trackGrid.getTrackCnt()));
        if (RecordService.isRecording()) {
            binding.sumRecNum.setText(String.format(Locale.US, "%d pts", RecordService.getNumPoints()));
        } else {
            binding.sumRecNum.setText("Idle");
        }
    }

    private void setBgColor(TextView tv, long idx, int[] colors) {
        tv.setBackgroundColor(colors[Math.min(colors.length-1, (int)idx)]);
    }

    public static void flash(CheckedTextView cb) {
        if (cb.isChecked()) {
            cb.clearAnimation();
        } else {
            Animation mAnimation = new AlphaAnimation(1, 0);
            mAnimation.setDuration(500);
            mAnimation.setInterpolator(new LinearInterpolator());
            mAnimation.setRepeatCount(Animation.INFINITE);
            mAnimation.setRepeatMode(Animation.REVERSE);
            cb.startAnimation(mAnimation);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.sum_perm) {
            boolean b =
                    startIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                || startIntent(Settings.ACTION_APPLICATION_SETTINGS)
                || startIntent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
        } else if (id == R.id.sum_gps) {
            StringBuilder strBld = new StringBuilder("GPS providers:");
            List<String> providers = getLocationManager(requireContext()).getProviders(true);
            if (providers.isEmpty()) {
                strBld.append(" NONE\nEnable Location Service");
            } else {
                for (String provider : providers) {
                    strBld.append("\n \u25FE ").append(provider);
                }
            }

            ToastUtils.show(root, strBld.toString());
        } else if (id == R.id.sum_network) {
            // startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            // startActivity(new Intent(Settings.ACTION_NETWORK_OPERATOR_SETTINGS));
            startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
        } else if (id == R.id.dbg_add_tracks) {
            Location location = getCurrentLocation(requireContext());
            if (location != null) {
                GpsPoint startPt = new GpsPoint(0, location.getLatitude(), location.getLongitude());
                Track testTrack = null;
                for (int idx = 0; idx < 10; idx++) {
                    // testTrack = createDirectionTrack(getGlobal().pref, startPt, idx * 30, 10000);
                    testTrack = createRandomTrack(getGlobal().pref, startPt, testTrack);
                    long added = getGlobal().trackGrid.saveTrack(testTrack);
                    ALog.i.tagMsg(this, "addTrack id=", added);
                    added = getGlobal().trackGrid.saveTrack(testTrack.reverseTack().addMilli(TimeUnit.HOURS.toMillis(RandomInt(1,8))));
                    ALog.i.tagMsg(this, "addTrack id=", added);
                }
                // getGlobal().getDbTracks(FETCH_DETAILS);
                refresh();
            } else {
                ToastUtils.show(root, "Unable to get current location\nEnable location services");
            }
        } else if (id == R.id.dbg_clear_tracks) {
            ToastUtils.show(root, "Clear Tracks\nAre you sure ?", "Yes", "No")
                    .observe(getViewLifecycleOwner(), btnIdx -> {
                        if (btnIdx == 0) {
                            getGlobal().trackGrid.clearAll();
                            // getGlobal().trackGrid.loadTracks();
                            root.post(this::refresh);
                        }
                    });
        }
    }

    // ACTION_APPLICATION_SETTINGS,
    // ACTION_MANAGE_APPLICATIONS_SETTINGS
    // ACTION_MANAGE_ALL_APPLICATIONS_SETTINGS

    // ACTION_APP_NOTIFICATION_SETTINGS
    // ACTION_APPLICATION_DETAILS_SETTINGS
    //     intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
    //     intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    //     intent.addCategory(Intent.CATEGORY_DEFAULT);
    //    intent.addCategory(Intent.CATEGORY_CAR_MODE);
    private boolean startIntent(String msg) {
        try {
            Uri packageName = Uri.fromParts("package",  requireActivity().getPackageName(), null);
            Intent intent = new Intent(msg, packageName);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //     intent.addCategory(Intent.CATEGORY_CAR_MODE);
            requireActivity().startActivity(intent);  // startIntentWithResult(requireActivity(), intent);
            ALog.d.tagMsg(this, "success opening app settings ", msg);
            return true;
        } catch (Exception ignore) {
            Toast.makeText(getContext(), "Failed to open app settings " + msg, Toast.LENGTH_LONG).show();
        }
        return false;
    }
}