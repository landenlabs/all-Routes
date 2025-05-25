/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.pages;

import static com.landenlabs.routes.data.ArrayListEx.get;
import static com.landenlabs.routes.data.GpsPoint.GPS_TIME_FMT;
import static com.landenlabs.routes.data.GpsPoint.NO_POINT;
import static com.landenlabs.routes.logger.Analytics.Event.PAGE_WEATHER;
import static com.landenlabs.routes.map.MapViewer.BIG_ICON_SCALE;
import static com.landenlabs.routes.pages.PagesSmartAux.SmartAlert.DRIVE_SPEED_KPH;
import static com.landenlabs.routes.utils.DataUtils.getString1x;
import static com.landenlabs.routes.utils.GpsUtils.getCurrentLocation;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.landenlabs.routes.R;
import com.landenlabs.routes.Record.RecordService;
import com.landenlabs.routes.data.ArrayListEx;
import com.landenlabs.routes.data.GpsPoint;
import com.landenlabs.routes.data.LiveQueue;
import com.landenlabs.routes.data.RouteSettings;
import com.landenlabs.routes.data.Track;
import com.landenlabs.routes.databinding.PageWeatherFragBinding;
import com.landenlabs.routes.events.EventBase;
import com.landenlabs.routes.events.EventSmartAalert;
import com.landenlabs.routes.events.EventStatus;
import com.landenlabs.routes.logger.Analytics;
import com.landenlabs.routes.map.MapMarkers;
import com.landenlabs.routes.map.MapViewer;
import com.landenlabs.routes.pages.PageUtils.TextStatus;
import com.landenlabs.routes.pages.PagesSmartAux.SmartAdapter;
import com.landenlabs.routes.pages.PagesSmartAux.SmartAlert;
import com.landenlabs.routes.pages.PagesSmartAux.SmartAlertLoader;
import com.landenlabs.routes.pages.PagesSmartAux.SmartItemHolder;
import com.landenlabs.routes.pages.PagesSmartAux.SmartViewHelper;
import com.landenlabs.routes.utils.FmtTime;
import com.landenlabs.routes.utils.GpsUtils;
import com.landenlabs.routes.utils.UnitDistance;
import com.landenlabs.routes.utils.UnitSpeed;
import com.landenlabs.routes.utils.UnitTemperature;
import com.mapbox.mapboxsdk.maps.MapView;
import com.weather.pangea.event.MapLongTouchEvent;
import com.weather.pangea.model.overlay.Overlay;
import com.weather.pangea.model.overlay.PolylinePathBuilder;
import com.wsi.mapsdk.map.WSIMapSelectMode;
import com.wsi.mapsdk.map.WSIMapType;
import com.wsi.mapsdk.map.WSIMapView;
import com.wsi.mapsdk.utils.WLatLng;
import com.wsi.mapsdk.utils.WLatLngBounds;
import com.wsi.wxdata.WxTime;

import org.joda.time.DateTime;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.location.WxLocationEx;
import landenlabs.wx_lib_data.logger.ALog;
import landenlabs.wx_lib_data.utils.UtilStr;

/**
 * Record and save a new route (track).
 */
@SuppressWarnings({"StatementWithEmptyBody", "SameParameterValue", "ConstantConditions", "CommentedOutCode", "unused"})
public class PageWeatherFrag extends PageBaseFrag implements
        View.OnClickListener
        , View.OnLongClickListener
        , MapViewer.OnWSIMapViewChangedCallback
        , MapViewer.MapLongTouchListener
        , MapView.OnCameraDidChangeListener {

    private static final int MAP_ZOOM_STREET_LEVEL = 14;

    private PageWeatherFragBinding binding;
    private TextStatus textStatus;
    private MapMarkers markers;
    private Track track = new Track();

    private GpsPoint startGpsPoint = GpsPoint.NO_POINT;
    private GpsPoint endGpsPoint = GpsPoint.NO_POINT;

    private SmartViewHelper helper;
    private SmartAdapter adapter;
    private final ArrayListEx<SmartAlert> smartAlerts = new ArrayListEx<>(3);
    private SmartAlertLoader smartAlertLoader;
    private LiveQueue<EventBase> liveDataQueue = new LiveQueue<>("SmartLoad");
    private final PageWeatherFrag.UpdateProgress updateProgress = new PageWeatherFrag.UpdateProgress();


    // ---------------------------------------------------------------------------------------------

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        authorizeMap(); // Call before inflating MapViewer
        binding = PageWeatherFragBinding.inflate(inflater, container, false);
        init(binding.getRoot());
        Analytics.send(PAGE_WEATHER);
        setUI(null);
        return root;
    }

    @Override
    public void onPause() {
        clearMapLayers();
        getGlobal().removeEventListener(this.getClass().getSimpleName());
        binding.mapViewer.removeOnMapChangedCallback(this);         //  onMapReady()
        binding.mapViewer.removeOnCameraDidChangeListener(this);    //  onCameraDidChange()
        markers = null;
        binding.wxProgress.removeCallbacks(updateProgress);
        liveDataQueue.clear();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.showNavBar(true);
        viewModel.showToolBar(true);
        getGlobal().addEventListener(this.getClass().getSimpleName(), this);
        setBtns(RecordService.isRecording());
        boolean okay = initMap(binding.mapViewer);
        setMap();
        updateProgress.refreshProgress();
        liveDataQueue.observe(getViewLifecycleOwner(), this::handleEvent);
        initSmartList();
    }

    private void handleEvent(EventBase event) {
        if (event instanceof EventSmartAalert) {
            EventSmartAalert eventSmartAalert = (EventSmartAalert)event;
            ALog.i.tagMsg(this, "Handle SmartAlert event ", eventSmartAalert);
            if (eventSmartAalert.percentPending < 0.1f) {
                updatePathMarkers();
                adapter.notifyDataSetChanged();
            }
            if (eventSmartAalert.exception != null) {
                Toast.makeText(requireContext(), "Handle SmartAlert exception\n" + eventSmartAalert.exception.toString(), Toast.LENGTH_LONG).show();
                ALog.e.tagMsg(this, "handle event exception ", eventSmartAalert.exception);
            }
            updateProgress.refreshProgress();
        }
        liveDataQueue.next();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    MenuItem menuShowRadar;
    MenuItem menuShowDDI;
    MenuItem menuUsePOI;

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menu.clear();
        menuInflater.inflate(R.menu.side_menu_wx, menu);
        menuShowDDI = menu.findItem(R.id.menu_ddi);
        menuShowRadar = menu.findItem(R.id.menu_radar);
        menuUsePOI = menu.findItem(R.id.menu_poi);
        refresh();
    }

    @Override
    public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
        @IdRes int id = menuItem.getItemId();
        menuItem.setChecked(!menuItem.isChecked());
        if (menuItem.getItemId() == R.id.menu_settings) {
            navigateTo(R.id.page_settings);
        } else  if (id == R.id.menu_radar) {
            if (menuItem.isChecked()) {  menuShowDDI.setChecked(false); }
            binding.mapViewer.showRadar(menuItem.isChecked());
        } else  if (id == R.id.menu_ddi) {
            if (menuItem.isChecked()) { menuShowRadar.setChecked(false); }
            binding.mapViewer.showDDI(menuItem.isChecked());
        } else  if (id == R.id.menu_wind) {
            binding.mapViewer.showWind(menuItem.isChecked());
        } else if (id == R.id.menu_poi) {
            refresh();
        }
        return false;
    }

    private void setUI(Bundle savedInstanceState) {
        binding.btnCenterCamera.setOnClickListener(this);
        binding.btnCenterBounds.setOnClickListener(this);
        binding.btnCenterCamera.setOnLongClickListener(this);
        binding.btnCenterBounds.setOnLongClickListener(this);
        setChecked(binding.btnCenterCamera, false);
        setChecked(binding.btnCenterBounds, false);
        binding.wxPickBtn.setOnClickListener(this);
        binding.wxClearBtn.setOnClickListener(this);
        binding.wxTitle.setOnClickListener(this);
    }

    private void setMap() {
        if (markers == null && binding.mapViewer.isReady()) {
            markers = new MapMarkers(requireContext(), binding.mapViewer.getWSIMap());
        }
    }

    private void refresh() {
        updatePathMarkers();
        double miles = UnitDistance.METERS.toMiles(track.getMeters());
        binding.wxLength.setText(String.format(Locale.US, helper.distanceFmt, miles));
        binding.wxDuration.setText(String.format(Locale.US, "%.1f hrs", miles / 60));
        int pntCnt = track.getPointCnt();
        binding.wxSteps.setText(String.valueOf(pntCnt));
        if (pntCnt != 0) {
            drawRouteOnMap();
        }

        if (TimeUnit.MILLISECONDS.toMinutes(Math.abs(System.currentTimeMillis() - binding.mapViewer.getFrameMilli())) > 5) {
            binding.mapViewer.setFrameMilli(System.currentTimeMillis());
            binding.wxTitle.setText("MapTime " + DateTime.now().toString("hh:mm a"));
        }
        cameraRefresh();
        refreshWeather();
    }

    private void cameraRefresh() {
        if (isChecked(binding.btnCenterCamera))
            cameraCenter();
        else if (isChecked(binding.btnCenterBounds))
            cameraBounds();
    }
    private void cameraBounds() {
        binding.mapViewer.getWSIMap().setMapType(WSIMapType.LIGHT);
        if (track.getPointCnt() > 1) {
            setMapCoverage();
        }
    }

    private void cameraCenter() {
        binding.mapViewer.getWSIMap().setMapType(WSIMapType.LIGHT);
        Location location = getCurrentLocation(requireActivity());
        if (location != null) {
            startGpsPoint = new GpsPoint(location.getLatitude(), location.getLongitude());
            WLatLng pt = new WLatLng(location.getLatitude(), location.getLongitude());
            binding.mapViewer.setCamera(pt, MAP_ZOOM_STREET_LEVEL, true, 1.0f);
        }
    }

    private void refreshWeather() {
        smartAlerts.clear();
        smartAlertLoader.done();
        if (!track.points.isEmpty()) {
            int metersPerStep = (int)Math.round(track.getMeters()/track.getPointCnt());
            int halfMetersPerStep = metersPerStep / 2;
            int metersTraveled = halfMetersPerStep;
            for (int idx = 0; idx < track.points.size(); idx++) {
                GpsPoint pt = track.points.get(idx);
                double meters = (int)Math.round(track.getMetersTo(idx) + halfMetersPerStep);
                pt.milli =  DateTime.now().plusMinutes((int)Math.round(meters / 1000 / DRIVE_SPEED_KPH * 60)).getMillis();

                smartAlerts.add(new SmartAlert(pt, (idx == 0)
                        ? WxTime.now()
                        : WxTime.comingHours(24), metersTraveled));
                metersTraveled += metersPerStep;
            }
            smartAlertLoader.load(smartAlerts, liveDataQueue);
            // adapter.notifyDataSetChanged();
        }
    }

    private Overlay routeOverlay = null;
    private void drawRouteOnMap() {
        synchronized (track) {
            if (routeOverlay != null) {
                binding.mapViewer.getTopLayer().removeOverlay(routeOverlay);
            }
            if (track.getPointCnt() > 1) {
                routeOverlay  = new PolylinePathBuilder()
                        .setPolyLine(track.toPolyline())
                        .setStrokeStyle(RouteSettings.lineStyleStd)
                        .build();

                binding.mapViewer.getTopLayer().addOverlay(routeOverlay);
            }
        }
    }

    private void clearMapLayers() {
        track.points.clear();
        if (markers != null) {
            markers.clearMarkers();
        }
        routeOverlay = null;
        binding.mapViewer.clearMarkers();
        binding.mapViewer.getTopLayer().clearOverlays();
        refresh();
    }

    private void clearAll() {
        binding.wxTitle.setText("Alerts");
        closeDialog();
        endGpsPoint = NO_POINT;
        smartAlerts.clear();
        adapter.notifyDataSetChanged();
        clearMapLayers();
    }

    public void init(@NonNull View root) {
        super.init(root);
        smartAlertLoader = new SmartAlertLoader( getString1x(requireContext(), "job1"));
    }

    @Override
    public void onEvent(EventBase event) {
        if (event instanceof EventStatus) {
            EventStatus eventStatus = (EventStatus) event;
            textStatus.appendStatus(DateTime.now().toString(GPS_TIME_FMT) + " " + eventStatus.msg, eventStatus.level);
        }
    }

    private void initSmartList() {
        if (helper == null || adapter == null) {
            helper = new SmartViewHelper(requireContext(), R.id.selected, this);
            adapter = new SmartAdapter(helper, smartAlerts, SmartItemHolder::createTrackHolderFor);
        }
        binding.routeList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.routeList.setAdapter(adapter);
        binding.routeList.setHasFixedSize(false);
    }

    public void setBtns(boolean isRecording) {  // RecordService.isRecording()
        binding.mapViewer.clearMarkers();
        binding.mapViewer.setOnLongClickListener((MapViewer.MapLongTouchListener) this);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (view.getTag(helper.clickTagId) instanceof Integer) {
            int smartIdx = (Integer) view.getTag(helper.clickTagId);
            openDialog(smartIdx);
        } else if (id == R.id.sa_close) {
            closeDialog();
        } else  if (id == R.id.wx_pick_btn) {
            startGpsPoint = NO_POINT;
            clearAll();
        } else if (id == R.id.wx_clear_btn) {
            clearAll();
        } else if (id == R.id.btn_center_camera) {
            cameraCenter();
        } else if (id == R.id.btn_center_bounds) {
            cameraBounds();
        } else if (id == R.id.wx_title)  {
            refresh();
        }
    }

    private static final String NO_DATA = "--";
    private void openDialog(int smartIdx) {
        if (smartIdx < 0 || smartIdx >= smartAlerts.size()) {
            return;
        }

        SmartAlert smartAlert = get(smartAlerts, smartIdx, null);
        if (smartAlert != null) {
            binding.dialogSmartAlert.getRoot().setVisibility(View.VISIBLE);
            binding.dialogSmartAlert.saClose.setOnClickListener(this);
            binding.dialogSmartAlert.saTitle.setText(String.format(Locale.US, "[%d] %.10s",
                    smartIdx, WxLocationEx.fmtLocationName(smartAlert.wxLocation)));
            double distMiles = UnitDistance.METERS.toMiles(track.getMetersTo(smartIdx));
            binding.dialogSmartAlert.saDistVal.setText(String.format(Locale.US, "%.1f mi", distMiles));
            double widthMiles = UnitDistance.METERS.toMiles(track.getMeters()/track.getPointCnt());
            binding.dialogSmartAlert.saWidthVal.setText(String.format(Locale.US, "%.1f mi", widthMiles));
            double hours = (distMiles+widthMiles/2) / 60f;
            binding.dialogSmartAlert.saHourVal.setText(String.format(Locale.US, "%.1f hr", hours));
            int minutes = (int)Math.round(hours * 60);
            binding.dialogSmartAlert.saTimeVal.setText(DateTime.now().plusMinutes(minutes).toString("hh:mm a"));
            if (smartAlert.hasValidData) {
                binding.dialogSmartAlert.saAlertVal.setText(
                        UtilStr.joinStrings("\n",  smartAlert.alertType(), smartAlert.alertExpire()));
                binding.dialogSmartAlert.saDdiVal.setText(
                        UtilStr.joinStrings("\n", SmartAlert.DDI_NAMES[smartAlert.drivingDifficultyIndex],
                                FmtTime.toString(smartAlert.drivingDifficultyTime, "hh:mm a") ));
                binding.dialogSmartAlert.saRateVal.setText(String.format(Locale.US, "%.1f cm/hr", smartAlert.precipRate));
                if (smartAlert.snowRate > 0) {
                    binding.dialogSmartAlert.saRateVal.setText(String.format(Locale.US, "%.1f (snow)", smartAlert.snowRate));
                }
                double temperatureF = UnitTemperature.CELSIUS.toFahrenheit(smartAlert.temperatureC);
                binding.dialogSmartAlert.saTempVal.setText(String.format(Locale.US, "%.1f°F", temperatureF));
                double windMph = UnitSpeed.KILOMETERS_PER_HOUR.toMilesPerHour(smartAlert.windSpeedKm);
                binding.dialogSmartAlert.saWindVal.setText(String.format(Locale.US, "%.1f mph", windMph));
            } else {
                binding.dialogSmartAlert.saAlertVal.setText(NO_DATA);
                binding.dialogSmartAlert.saDdiVal.setText(NO_DATA);
                binding.dialogSmartAlert.saRateVal.setText(NO_DATA);
                binding.dialogSmartAlert.saTempVal.setText(NO_DATA);
                binding.dialogSmartAlert.saWindVal.setText(NO_DATA);
            }
        } else {
            Toast.makeText(requireContext(), "No alert available", Toast.LENGTH_LONG).show();
        }
    }
    private void closeDialog() {
        binding.dialogSmartAlert.getRoot().setVisibility(View.GONE);
    }

    @Override
    public void onLongTouch(MapViewer mapViewer, MapLongTouchEvent event) {
        clearAll();
        WLatLng latLng = new WLatLng(event.getCenterLatLng());
        String locStr = String.format(Locale.US, "%.3f,%.3f", latLng.getLatitude(), latLng.getLongitude());
        // Toast.makeText(binding.mapViewer.getContext(), locStr, Toast.LENGTH_SHORT).show();
        ClipboardManager clipboard = (ClipboardManager) binding.mapViewer.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", locStr);
        clipboard.setPrimaryClip(clip);
        smartAlerts.clear();
        if (isValidGps(startGpsPoint)) {
            endGpsPoint = new GpsPoint(latLng.latitude, latLng.longitude);
            track.setPath(startGpsPoint, endGpsPoint, 10, UnitDistance.MILES.toMeters(10));
            track.setPath(startGpsPoint, endGpsPoint, 10, UnitDistance.MILES.toMeters(10));
        } else {
            startGpsPoint = new GpsPoint(latLng.latitude, latLng.longitude);
        }
        refresh();
    }

    private void updatePathMarkers() {
        if (isValidGps(startGpsPoint) && isValidGps(endGpsPoint) && markers != null && track != null) {
            markers.clearMarkers();
            if (smartAlerts.size() > 2) {
                boolean usePoi = menuUsePOI.isChecked();
                try {
                    binding.mapViewer.clearMarkers();
                    // endGpsPoint = NO_POINT;
                    track.clear();
                    int idx = 0;
                    for (SmartAlert smartAlert : smartAlerts) {
                        GpsPoint pt = smartAlert.pt;
                        if (smartAlert.hasValidLoc && usePoi) {
                            pt = new GpsPoint(smartAlert.wxLocation.latitude, smartAlert.wxLocation.longitude);
                        }
                        track.add(pt);
                        markers.addMarker(pt.toLatLng(), "Pt" + idx++, smartAlert.getIcon(requireContext())).setScaleFactor(2f);
                    }
                    return;
                } catch (Exception ex) {
                    ALog.e.tagMsg(this, "path marker exception ", ex);
                }
            } else {
                for (int idx = 1; idx < track.points.size() - 1; idx++) {
                    GpsPoint pt = track.points.get(idx);
                    markers.addMarker(pt.toLatLng(), "Pt" + idx);
                }
            }
        }
        if (isValidGps(startGpsPoint))
            binding.mapViewer.moveMarker(startGpsPoint.toLatLng(), MapViewer.GPS_MARKER, BIG_ICON_SCALE);
        if (isValidGps(endGpsPoint))
            binding.mapViewer.moveMarker(endGpsPoint.toLatLng(), MapViewer.CITY_MARKER, BIG_ICON_SCALE);
    }

    private static boolean isValidGps(GpsPoint gpsPoint) {
        return gpsPoint != null && !GpsPoint.NO_POINT.equals(gpsPoint);
    }

    private boolean isChecked(ImageView view) {
        Object bool = view.getTag(R.id.checkpath_sel);
        return (bool instanceof Boolean) ? (Boolean)bool : false;
    }
    private void setChecked(ImageView view, boolean checked) {
        int BG_OFF = Color.TRANSPARENT;
        int BG_ON = Color.GREEN;
        view.setBackgroundColor(checked ? BG_ON : BG_OFF);
        // view.setTag(R.id.checkpath_sel, checked);
    }
    private void toggleChecked(ImageView view) {
        setChecked(view, !isChecked(view));
    }

    @Override
    public boolean onLongClick(View view) {
        int id = view.getId();
        if (id == R.id.btn_center_camera) {
            toggleChecked(binding.btnCenterCamera);
            setChecked(binding.btnCenterBounds, false);
            cameraRefresh();
            return true;
        } else if (id == R.id.btn_center_bounds) {
            toggleChecked(binding.btnCenterBounds);
            setChecked(binding.btnCenterCamera, false);
            cameraRefresh();
            return true;
        }
        return false;
    }

    public void setMapCoverage() {
        float viewWidth = binding.mapViewer.getWidth();
        float viewHeight = binding.mapViewer.getHeight();
        int margin_h = requireContext().getResources().getDimensionPixelOffset(R.dimen.record_list_max_height) + 10;
        int margin_w = requireContext().getResources().getDimensionPixelOffset(R.dimen.map_trip_margin_width);

        WLatLngBounds bounds = track.getBounds();
        bounds = GpsUtils.minBounds(bounds, RouteSettings.minBoundsDeg);
        // float padPercent = 0.1f;
        // bounds = GpsUtils.padBounds(bounds, padPercent*2, padPercent);
        bounds = GpsUtils.padBounds(bounds,margin_h / viewHeight,  margin_w / viewWidth);
        binding.mapViewer.setCameraBounds(bounds, 1000);
        //refresh();
    }


    //==============================================================================================
    // MapView

    private void authorizeMap() {
        if (!WSIMapView.isAuthorized()) {
            ALog.d.tagMsg(this, "Authorize map");
            MapViewer.initBeforeCreate(requireContext());
        }
    }

    private boolean initMap(MapViewer mapView) {
        boolean okay = true;
        String mapName = mapView.getTag().toString();
        mapView.getWSIMap().setMapType(WSIMapType.LIGHT);
        mapView.addOnMapChangedCallback(this);      // ::onMapReady

        // "RadarSmooth";  // twcRadarMossaic + radarFcst
        String rasterName = "RadarSmooth";
        if (! (okay = mapView.setRasterLayer(rasterName)))
            ALog.w.tagMsg(mapName, "Failed to set map raster layer " + rasterName);
        if (! (okay |= mapView.setOverlayLayers(null)))
            ALog.w.tagMsg(mapName, "Failed to set map overlay layers ");

        mapView.getWSIMap().setActiveRasterLayerTilesTime(System.currentTimeMillis(), WSIMapSelectMode.FLOOR);
        MapViewer.setTimeline(DateTime.now(), mapView);
        // mapView.getWSIMap().updateMapOverlaysVisibility(mapView);
        // mapView.getWSIMap().setMapOverlaysVisibility(true, true, mapView);
        mapView.addOnCameraDidChangeListener(this);     // ::onCameraDidChange
        return okay;
    }

    public boolean isReady() {
        return binding.mapViewer.isReady();
    }

    @Override
    public void onMapReady(WSIMapView wsiMapView, int why) {
        if (why == MAP_STATE_READY) {
            if (markers == null) {
                markers = new MapMarkers(requireContext(), wsiMapView.getWSIMap());
            }

            Location location = getCurrentLocation(requireActivity());
            if (location != null) {
                startGpsPoint = new GpsPoint(location.getLatitude(), location.getLongitude());
                binding.mapViewer.setCamera(new WLatLng(location.getLatitude(), location.getLongitude()), 13, true, 1.0f);
            }

            refresh();
        }
    }

    @Override
    public void onCameraDidChange(boolean animated) {
        //  refreshUi();  causes circular code loop.
    }


    // =============================================================================================
    private class UpdateProgress implements Runnable {

        @Override
        public void run() {
            refreshProgress();
        }

        void refreshProgress() {
            if (binding != null && binding.wxProgress != null) {
                if (smartAlertLoader.isDone()) {
                    binding.wxProgress.setProgress(0);
                } else {
                    binding.wxProgress.setMax(100);
                    int percent = 100 - Math.round(smartAlertLoader.getPendingPercent() * 100);
                    ALog.i.tagMsg(this, "SmartAlert load percent=", percent);
                    binding.wxProgress.setProgress(percent);
                }
                binding.wxTitle.setText(String.format(Locale.US, "Bytes:%,d", smartAlertLoader.networkReadBytes));
            }
        }
    }
}