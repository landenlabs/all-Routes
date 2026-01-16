/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package landenlabs.routes.pages;

import static landenlabs.routes.data.GpsPoint.GPS_TIME_FMT;
import static landenlabs.routes.logger.Analytics.Event.PAGE_RECORDER;
import static landenlabs.routes.utils.FmtTime.milliToSec;
import static landenlabs.routes.utils.FmtTime.periodFmter;
import static landenlabs.routes.utils.GpsUtils.getCurrentLocation;
import static landenlabs.routes.utils.Ui.getColor;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;

import landenlabs.routes.GlobalHolder;
import landenlabs.routes.R;
import landenlabs.routes.Record.RecordService;
import landenlabs.routes.data.ArrayListEx;
import landenlabs.routes.data.GpsPoint;
import landenlabs.routes.data.RouteSettings;
import landenlabs.routes.data.Track;
import landenlabs.routes.data.TrackCommon;
import landenlabs.routes.databinding.PageRecorderFragBinding;
import landenlabs.routes.events.EventAction;
import landenlabs.routes.events.EventBase;
import landenlabs.routes.events.EventDb;
import landenlabs.routes.events.EventStatus;
import landenlabs.routes.events.EventTrack;
import landenlabs.routes.logger.Analytics;
import landenlabs.routes.map.MapMarkers;
import landenlabs.routes.map.MapViewer;
import landenlabs.routes.pages.PageRoutesAux.RouteAdapter;
import landenlabs.routes.pages.PageRoutesAux.RouteItemHolder;
import landenlabs.routes.pages.PageRoutesAux.RouteViewHelper;
import landenlabs.routes.pages.PageUtils.TextStatus;
import landenlabs.routes.utils.GpsUtils;
import com.mapbox.mapboxsdk.maps.MapView;
import com.weather.pangea.model.overlay.Overlay;
import com.weather.pangea.model.overlay.PolylinePathBuilder;
import com.wsi.mapsdk.map.WSIMapType;
import com.wsi.mapsdk.map.WSIMapView;
import com.wsi.mapsdk.utils.WLatLng;
import com.wsi.mapsdk.utils.WLatLngBounds;

import org.joda.time.DateTime;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Record and save a new route (track).
 */
@SuppressWarnings({"StatementWithEmptyBody", "SameParameterValue", "ConstantConditions", "CommentedOutCode", "unused"})
public class PageRecorderFrag extends PageBaseFrag implements
        View.OnClickListener
        , View.OnLongClickListener
        , MapViewer.OnWSIMapViewChangedCallback
        , MapView.OnCameraDidChangeListener {

    private static final int MAP_ZOOM_STREET_LEVEL = 14;

    private PageRecorderFragBinding binding;
    private TextStatus textStatus;
    private MapMarkers markers;
    private Track track = new Track();
    private final UpdateProgress updateProgress = new UpdateProgress();

    private RouteViewHelper helper;
    private RouteAdapter adapter;
    private final ArrayListEx<Track>  matchTracks = new ArrayListEx<>(3);

    private int BG_OFF = Color.TRANSPARENT;
    private int BG_ON = Color.GREEN;

    // ---------------------------------------------------------------------------------------------

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        authorizeMap(); // Call before inflating MapViewer
        binding = PageRecorderFragBinding.inflate(inflater, container, false);
        init(binding.getRoot());
        Analytics.send(PAGE_RECORDER);
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
        binding.recProgress.removeCallbacks(updateProgress);
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        viewModel.showNavBar(false);
        viewModel.showToolBar(true);
        getGlobal().addEventListener(this.getClass().getSimpleName(), this);
        setBtns(RecordService.isRecording());
        initMap(binding.mapViewer);
        setMap();
        GlobalHolder.sendEvent(getGlobal(), new EventAction(EventAction.Action.REQ_REC_TRACK));
        updateProgress.refreshProgress();
        initMatchList();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    MenuItem menuShowRadar;
    MenuItem menuShowDDI;

    @Override
    public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
        menu.clear();
        menuInflater.inflate(R.menu.side_menu_record, menu);
        menuShowDDI = menu.findItem(R.id.menu_ddi);
        menuShowRadar = menu.findItem(R.id.menu_radar);
        refresh();
    }

    @SuppressLint("NotifyDataSetChanged")
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
        }
        // binding.mapViewer.play(true);
        // refresh();
        return false;
    }


    private void setUI(Bundle savedInstanceState) {
        // binding.btnToggleWsi.setOnClickListener(this);
        binding.btnCenterCamera.setOnClickListener(this);
        binding.btnCenterBounds.setOnClickListener(this);
        binding.btnCenterCamera.setOnLongClickListener(this);
        binding.btnCenterBounds.setOnLongClickListener(this);
        setChecked(binding.btnCenterCamera, true);
        setChecked(binding.btnCenterBounds, false);
        binding.recStartStop.setOnClickListener(this);
        binding.recPause.setOnClickListener(this);
        textStatus = new TextStatus(binding.recData);
        updateProgress.refreshProgress();
    }

    private void setMap() {
        if (markers == null && binding.mapViewer.isReady()) {
            markers = new MapMarkers(requireContext(), binding.mapViewer.getWSIMap());
        }
    }

    private void refresh() {
        if (!binding.mapViewer.isReady())
            return;

        if (RecordService.isRecording()) {
            binding.mapViewer.clearMarkers();
        }

        int pntCnt = track.getPointCnt();
        binding.recCount.setText(String.valueOf(pntCnt));
        if (pntCnt == 0) {
            binding.recDuration.setText("Idle");
        } else {
            org.joda.time.Duration duration = new org.joda.time.Duration(track.getDurationMilli());
            binding.recDuration.setText(periodFmter.print(duration.toPeriod()));

            setMap();
            if (markers != null) {
                markers.clearMarker(MapMarkers.END_MARKER);
                if (pntCnt == 1) {
                    markers.addMarker(track.get(0).toLatLng(), MapMarkers.START_MARKER);
                } else {
                    if (pntCnt > 2) {
                        if (false) {
                            // Add markers along route or draw route path (see drawRouteOnMap)
                            markers.addMarker(track.get(pntCnt - 2).toLatLng(), track.get(pntCnt - 2).logString());
                        }
                    }
                    markers.addMarker(track.get(pntCnt - 1).toLatLng(),
                            RecordService.isRecording() ? MapMarkers.POS_MARKER : MapMarkers.END_MARKER);
                }
            }

            drawRouteOnMap();
        }
        updateProgress.refreshProgress();

        refreshCamera();
    }

    private void refreshCamera() {
        refreshCamera(isChecked(binding.btnCenterCamera));
    }
    private void refreshCamera(boolean centerOnCurrent) {
        binding.mapViewer.getWSIMap().setMapType(WSIMapType.LIGHT);
        if (centerOnCurrent || track.getPointCnt() < 2) {
            Location location = getCurrentLocation(requireActivity());

            if (location != null) {
                WLatLng pt = new WLatLng(location.getLatitude(), location.getLongitude());
                binding.mapViewer.setCamera(pt, MAP_ZOOM_STREET_LEVEL, true, 1.0f);
                if (markers != null) {
                    markers.addMarker(pt, MapMarkers.END_MARKER);
                }
            }
        } else {
            setMapCoverage();
        }
    }

    private void setStopMarker() {
        int pntCnt = track.getPointCnt();
        if (pntCnt > 0 && markers != null) {
            markers.clearMarker(MapMarkers.POS_MARKER);
            markers.addMarker(track.get(pntCnt - 1).toLatLng(), MapMarkers.END_MARKER);
        }
    }

    private Overlay routeOverlay = null;
    private void drawRouteOnMap() {
        synchronized (track) {
   //         binding.mapViewer.getTopLayer().clearOverlays();
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
        if (binding.mapViewer.isReady() && binding.mapViewer.getTopLayer() != null) {
            binding.mapViewer.getTopLayer().clearOverlays();
        }
        refresh();
    }

    public void init(@NonNull View root) {
        super.init(root);
    }

    GpsPoint lastGpsPoint = GpsPoint.NO_POINT;

    @Override
    public void onEvent(EventBase event) {
        if (event instanceof EventStatus) {
            EventStatus eventStatus = (EventStatus) event;
            textStatus.appendStatus(DateTime.now().toString(GPS_TIME_FMT) + " " + eventStatus.msg, eventStatus.level);
        } else if (event instanceof EventTrack) {
            EventTrack eventTrack = (EventTrack) event;
            if (eventTrack.data != null && eventTrack.data.points != null) {
                GpsPoint nextGpsPoint = eventTrack.data.points.last(GpsPoint.NO_POINT);
                if (nextGpsPoint.milli == lastGpsPoint.milli) {
                    // TODO - indicate update but no new gps.
                } else {
                    String msg = nextGpsPoint.logString();
                    SpannableString ss = new SpannableString(msg);
                    ss.setSpan(new ForegroundColorSpan(Color.YELLOW), 0, msg.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    textStatus.appendStatus(ss, ALog.INFO);
                    lastGpsPoint = nextGpsPoint;
                    showMatch(getGlobal().trackGrid.findMatches(eventTrack.data));
                }
                track = eventTrack.data.clone(track);
                getGlobal().notifyTrack(requireContext(), track);
            }
            refresh();
        }

        updateProgress.refreshProgress();
    }


    private void showMatch(TrackCommon.TrackScore trackScore) {
        matchTracks.clear();
        long recordId = track != null ? track.id : -2;
        if (trackScore.dowHourId != recordId && trackScore.dowHourId != -1) {
            matchTracks.add(getGlobal().trackGrid.getTrack(trackScore.dowHourId));
        }
        if (trackScore.dowId != recordId && trackScore.dowId != -1 && trackScore.dowId != trackScore.dowHourId) {
            matchTracks.add(getGlobal().trackGrid.getTrack(trackScore.dowId));
        }
        if (trackScore.hourId != recordId && trackScore.hourId != -1 && trackScore.hourId != trackScore.dowHourId) {
            matchTracks.add(getGlobal().trackGrid.getTrack(trackScore.dowHourId));
        }
        adapter.notifyDataSetChanged();
    }

    private void initMatchList() {
        if (helper == null || adapter == null) {
            helper = new RouteViewHelper(requireContext(), R.id.selected, this);
            helper.showCheckbox = true;
            // helper.showOnlyChecked = false;
            helper.selectedSet.clear();
            adapter = new RouteAdapter(helper, matchTracks, RouteItemHolder::createTrackHolderFor);
        }

        binding.routeList.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.routeList.setAdapter(adapter);
        binding.routeList.setHasFixedSize(false);
    }

    public void setBtns(boolean isRecording) {  // RecordService.isRecording()
        if (isRecording) {
            binding.mapViewer.clearMarkers();
            binding.recData.setText("");
            binding.recStartStop.setBackgroundTintList(ColorStateList.valueOf(getColor(binding.recStartStop, R.color.bg_stop)));
            binding.recStartStop.setText("Stop");
            binding.recPause.setEnabled(true);
        } else {
            binding.recStartStop.setBackgroundTintList(ColorStateList.valueOf(getColor(binding.recStartStop, R.color.bg_start)));
            binding.recStartStop.setText("Start");
            binding.recPause.setEnabled(false);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.rec_start_stop) {
            setBtns(!RecordService.isRecording());
            if (RecordService.isRecording()) {
                RecordService.requestStopRecording(requireContext());
                setStopMarker();
                // getGlobal().gpsDataBase.addTrack(track);
                getGlobal().sendEvent(new EventDb(EventDb.DbAction.add));
            } else {
                clearMapLayers();
                track.clear();
                RecordService.requestStartRecording(requireContext(), getGlobal().nextId(), getGlobal());
            }
            refreshCamera(false);
        } else if (id == R.id.rec_pause) {
            if (view.getTag() == null) {
                RecordService.requestPauseRecording(requireContext());
                setStopMarker();
                ((TextView)view).setText(R.string.resume);
                view.setTag(1);
            } else {
                RecordService.requestResumeRecording(requireContext());
                // setStopMarker();
                ((TextView)view).setText(R.string.pause);
                view.setTag(null);
            }
        } else if (id == R.id.btn_center_camera) {
            refreshCamera(true);
        } else if (id == R.id.btn_center_bounds) {
            refreshCamera(false);
        }
    }

    private boolean isChecked(ImageView view) {
        Object bool = view.getTag(R.id.checkpath_sel);
        return (bool instanceof Boolean) ? (Boolean)bool : false;
    }
    private void setChecked(ImageView view, boolean checked) {
        view.setBackgroundColor(checked ? BG_ON : BG_OFF);
        view.setTag(R.id.checkpath_sel, checked);
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
            refreshCamera();
            return true;
        } else if (id == R.id.btn_center_bounds) {
            toggleChecked(binding.btnCenterBounds);
            setChecked(binding.btnCenterCamera, false);
            refreshCamera();
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

    // =============================================================================================
    private class UpdateProgress implements Runnable {

        @Override
        public void run() {
            refreshProgress();
        }

        void refreshProgress() {
            if (binding != null && binding.recProgress != null) {
                if (RecordService.isRecording()) {
                    binding.recProgress.setMax(milliToSec(RouteSettings.gpsSaveMilli));
                    int val = milliToSec(System.currentTimeMillis() - track.getMilliEnd());
                    int maxVal = binding.recProgress.getMax();
                    val = val % maxVal;
                    binding.recProgress.setProgress(val);
        //            binding.getRoot().postDelayed(this::refreshProgress, 1000);
                } else {
                    binding.recProgress.setProgress(0);
                }
            }
        }
    }

    //==============================================================================================
    // MapView

    private void authorizeMap() {
        if (!WSIMapView.isAuthorized()) {
            ALog.d.tagMsg(this, "Authorize map");
            MapViewer.initBeforeCreate(requireContext());
        }
    }

    private void initMap(MapViewer mapView) {
        String mapName = mapView.getTag().toString();
        mapView.getWSIMap().setMapType(WSIMapType.LIGHT);
        mapView.addOnMapChangedCallback(this);      // ::onMapReady

        // "RadarSmooth";  // twcRadarMossaic + radarFcst
        String rasterName = "NoRaster";
        if (!mapView.setRasterLayer(rasterName))
            ALog.w.tagMsg(mapName, "Failed to set map raster layer " + rasterName);
        if (!mapView.setOverlayLayers(null))
            ALog.w.tagMsg(mapName, "Failed to set map overlay layers ");

        MapViewer.setTimeline(DateTime.now(), mapView);
        // mapView.getWSIMap().updateMapOverlaysVisibility(mapView);
        // mapView.getWSIMap().setMapOverlaysVisibility(true, true, mapView);
        mapView.addOnCameraDidChangeListener(this);     // ::onCameraDidChange
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
                binding.mapViewer.setCamera(new WLatLng(location.getLatitude(), location.getLongitude()), 13, true, 1.0f);
            }
        }
        refresh();
    }

    @Override
    public void onCameraDidChange(boolean animated) {
        //  refreshUi();  causes circular code loop.
    }
}