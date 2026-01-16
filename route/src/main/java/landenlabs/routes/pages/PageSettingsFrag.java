/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.pages;

import static landenlabs.routes.logger.Analytics.Event.PAGE_SETTINGS;
import static landenlabs.routes.utils.FmtTime.milliToSec;
import static landenlabs.routes.utils.FmtTime.secToMilli;
import static landenlabs.routes.utils.SysUtils.strToDouble;
import static landenlabs.routes.utils.SysUtils.strToInt;
import static landenlabs.routes.utils.SysUtils.strToLong;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import landenlabs.routes.R;
import landenlabs.routes.data.RouteSettings;
import landenlabs.routes.databinding.PageSettingsFragBinding;
import landenlabs.routes.events.EventBase;
import landenlabs.routes.logger.Analytics;

import java.util.Locale;

/**
 * Settings page.
 */
public class PageSettingsFrag extends PageBaseFrag implements View.OnClickListener {
    private PageSettingsFragBinding binding;

    // ---------------------------------------------------------------------------------------------

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = PageSettingsFragBinding.inflate(inflater, container, false);

        init(binding.getRoot());
        Analytics.send(PAGE_SETTINGS);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.showNavBar(true);
        viewModel.showToolBar(true);
        getGlobal().addEventListener(this.getClass().getSimpleName(), this);
        refresh();
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

    @Override
    public void onEvent(@Nullable EventBase event) {
        super.onEvent(event);
        refresh();
    }

    protected void refresh() {
        RouteSettings.init(requireContext());

        binding.setGpsRateSec.setText(  String.valueOf(milliToSec(RouteSettings.gpsRequestMilli)));
        binding.setSaveRateSec.setText(  String.valueOf(milliToSec(RouteSettings.gpsSaveMilli)));
        binding.setGpsMinMeters.setText(  String.valueOf(RouteSettings.gpsMinMeters));
        binding.setMinBnds.setText( String.format(Locale.US, "%.4f", RouteSettings.minBoundsDeg));
        binding.setTrackColor.setImageTintList(ColorStateList.valueOf(
                toRGBA(  RouteSettings.lineStyleStd.getColor(),
                        RouteSettings.lineStyleStd.getOpacity()
                )));
        binding.setTestColor.setImageTintList(ColorStateList.valueOf(
                toRGBA(
                    RouteSettings.lineStyleTest.getColor(),
                    RouteSettings.lineStyleTest.getOpacity()
                )));

        binding.setApply.setOnClickListener(this);
        binding.setCancel.setOnClickListener(this);
        binding.pageBackBtn.setOnClickListener(this);
    }

    public static int toRGBA(int rgb, float opacity) {
        return Color.argb((int)(opacity*255),
                Color.red(rgb),
                Color.green(rgb),
                Color.blue(rgb));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.set_apply) {
           saveSettings();
           viewModel.popBackPage();
        }  else if (id ==  R.id.set_cancel) {
            refresh();
            viewModel.popBackPage();
        } else if (id == R.id.page_backBtn) {
            viewModel.popBackPage();
        }
    }

    private void saveSettings() {
        RouteSettings.gpsRequestMilli = secToMilli( strToLong(binding.setGpsRateSec.getText(), milliToSec(RouteSettings.gpsRequestMilli)));
        RouteSettings.gpsSaveMilli = secToMilli( strToLong(binding.setSaveRateSec.getText(),  milliToSec(RouteSettings.gpsSaveMilli)));
        RouteSettings.gpsMinMeters = strToInt( binding.setGpsMinMeters.getText(), RouteSettings.gpsMinMeters );
        RouteSettings.minBoundsDeg = strToDouble( binding.setMinBnds.getText(), RouteSettings.minBoundsDeg );

        // @ColorInt int clr = toRGBA(RouteSettings.lineStyleStd.getColor(), RouteSettings.lineStyleStd.getOpacity());
        @ColorInt int clr = binding.setTrackColor.getImageTintList().getDefaultColor();
        RouteSettings.lineStyleStd = RouteSettings.makeStroke(clr, 2);
        clr = binding.setTestColor.getImageTintList().getDefaultColor();
        RouteSettings.lineStyleTest = RouteSettings.makeStroke(clr, 1);
        // clr = binding.setRevColor.getImageTintList().getDefaultColor();
        // RouteSettings.lineStyleRev = RouteSettings.makeStroke(clr, 1);

        RouteSettings.save(requireContext());
    }
}