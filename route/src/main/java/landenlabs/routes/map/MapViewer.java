/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.map;

import static com.landenlabs.routes.utils.DataUtils.getString1x;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;

import com.landenlabs.routes.R;
import com.landenlabs.routes.logger.AppLog;
import com.weather.pangea.event.MapLongTouchEvent;
import com.weather.pangea.layer.Layer;
import com.weather.pangea.layer.overlay.SimpleOverlayLayer;
import com.wsi.mapsdk.InventoryOverlay;
import com.wsi.mapsdk.log.MLog;
import com.wsi.mapsdk.map.OnWSIMapViewReadyCallback;
import com.wsi.mapsdk.map.WSIMap;
import com.wsi.mapsdk.map.WSIMapCalloutInfoList;
import com.wsi.mapsdk.map.WSIMapDelegate;
import com.wsi.mapsdk.map.WSIMapGeoOverlay;
import com.wsi.mapsdk.map.WSIMapOptions;
import com.wsi.mapsdk.map.WSIMapRasterLayer;
import com.wsi.mapsdk.map.WSIMapRasterLayerDataDisplayMode;
import com.wsi.mapsdk.map.WSIMapRasterLayerTimeDisplayMode;
import com.wsi.mapsdk.map.WSIMapSelectMode;
import com.wsi.mapsdk.map.WSIMapType;
import com.wsi.mapsdk.map.WSIMapView;
import com.wsi.mapsdk.map.WSIMapViewDelegate;
import com.wsi.mapsdk.map.WSIRasterLayerLoopTimes;
import com.wsi.mapsdk.markers.WSIMarkerView;
import com.wsi.mapsdk.markers.WSIMarkerViewOptions;
import com.wsi.mapsdk.utils.DrawUtils;
import com.wsi.mapsdk.utils.ObjUtils;
import com.wsi.mapsdk.utils.WCameraPosition;
import com.wsi.mapsdk.utils.WLatLng;
import com.wsi.mapsdk.utils.WLatLngBounds;

import org.greenrobot.eventbus.Subscribe;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Manage WSI Map view.
 */
@SuppressWarnings({"Convert2Lambda", "unused", "UnnecessaryLocalVariable", "CommentedOutCode"})
public class MapViewer extends WSIMapView
        implements DefaultLifecycleObserver
        , LifecycleObserver
        , WSIMapViewDelegate
        , WSIMapDelegate
        , OnWSIMapViewReadyCallback {

    //  Initial map state
    private static final WLatLng START_POS = new WLatLng(35.0, -90.0);
    private static final float START_ZOOM = 5f;
    private static final String START_NAME = "Place";
    private static final WLatLng ZERO_POS = new WLatLng(0, 0);
    private static final float DEF_MARKER_SCALE = 1.0f;

    // Keys used to manage UI state.
    private static final String PREF_CAMERA_LAT = "cameraLat";
    private static final String PREF_CAMERA_LNG = "cameraLng";
    private static final String PREF_CAMERA_ZOOM = "cameraZoom";
    private static final String PREF_MAP_TYPE = "mapType";
    private static final String PREF_RASTER_LAYER_ID = "mapRasterLayer";
    private static final String PREF_OVERLAYS_ON = "mapOverlaysOn";
    private static final String TAG = "MapViewer";

    public static float     BIG_ICON_SCALE = 1f;
    public static final int GPS_MARKER = R.drawable.ic_map_pin_drive;
    public static final int CITY_MARKER = R.drawable.ic_map_marker;
    public static final String DEF_MAP_RASTER = "RadarWithModel"; // "RadarSmooth"  "twcRadarMossaic"  "radarFcst"

    private final Set<OnWSIMapViewChangedCallback> mapViewChangedCallbacks = new HashSet<>();
    private final Map<Integer, WSIMarkerView> pinMarkers = new HashMap<>();
    WSIMapType  mapType = WSIMapType.LIGHT;
    private SharedPreferences pref;
    private static final String MAP_NAME = "Map1";
    private String mapName = MAP_NAME;
    private List<InventoryOverlay> showOverlays = null;             // MapSDK overlays
    private boolean doReadyOnce = true;



    // ---------------------------------------------------------------------------------------------
    // 3. Construct view
    public MapViewer(@NonNull Context context, WSIMapOptions wsiMapOptions, @NonNull String mapName) {
        super(context, wsiMapOptions);
        this.mapName = mapName;
        commonInit(context);
    }

    public MapViewer(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        commonInit(context);
    }

    public MapViewer(@NonNull Context context, @Nullable AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        commonInit(context);
    }

    public static void initBeforeCreate(@NonNull Context context) {
        MLog.init(context);
        // adb shell setprop log.tag.MLog DEBUG
        // MLog.MIN_LEVEL = ALog.MIN_LEVEL;

        WSIMapType.CUSTOM1.title = "TWC’s Dark";
        WSIMapType.CUSTOM1.mapUrl = context.getString(R.string.map_style_url);
        WSIMapType.CUSTOM1.isDark = true;

        //  1. Set API key provided by TWC.
        WSIMapView.setApiKey(context, getString1x(context, "app_name"));

        //  2. Initialize Map prior to creating view.
        WSIMapView.initBeforeCreate(context, null);
    }

    public static WLatLng getCameraPos(@Nullable WLatLng latlng, @Nullable WLatLng defCameraPos) {
        return (latlng == null) ? defCameraPos : latlng;
    }

    // ---------------------------------------------------------------------------------------------

    @Nullable
    public static WSIMapRasterLayer findRasterLayer(WSIMapView mapView, String layerId) {
        WSIMapRasterLayer found = null;
        if (mapView.isReady()) {
            List<WSIMapRasterLayer> layers = mapView.getWSIMap().getAvailableRasterLayers();
            for (WSIMapRasterLayer layer : layers) {
                if (sameRasterLayer(layer, layerId)) {
                    found = layer;
                    break;
                }
            }
        }

        return found;
    }

    /**
     * Set Active Raster layer and optionally enable custom looping behavior.
     */
    public static void setActiveRasterLayer(
            Context context,
            @Nullable WSIMapView wsiMapView,
            @Nullable WSIMapRasterLayer rasterLayer) {
        if (wsiMapView != null && wsiMapView.hasWSIMap()  /* wsiMapView.isReady() */) {
            if (rasterLayer != null &&
                    !sameRasterLayer(rasterLayer, wsiMapView.getWSIMap().getActiveRasterLayer())) {
                wsiMapView.getWSIMap().setActiveRasterLayer(rasterLayer, wsiMapView);

                // Set optional custom animation looping frame limits based on network connection speed.
                final int ALL_AVAILABLE_FRAMES = 0;
                final int FAST_LOOP_LIMIT = ALL_AVAILABLE_FRAMES;
                final int SLOW_LOOP_LIMIT = 20;
                wsiMapView.getWSIMapViewController().setRasterLayerFrameLoopingLimit(FAST_LOOP_LIMIT, SLOW_LOOP_LIMIT);
            }
        } else {
            AppLog.LOG_MAP.e().tagMsg(TAG, "MapViewer Failed to set raster layer ", DbgToString(wsiMapView));
        }
    }

    public static String DbgToString(@Nullable WSIMapView mapViewer) {
        String msg = String.valueOf(System.identityHashCode(mapViewer));
        if (mapViewer == null) {
            return " Map NULL";
        } else if (mapViewer.isReady()) {
            return msg + " Map camera=" + new WLatLng(mapViewer.getWSIMap().getCameraPosition().getTarget());
        } else {
            return msg + " Map NOT ready";
        }
    }

    public static boolean sameRasterLayer(
            WSIMapRasterLayer rasterLayer1,
            WSIMapRasterLayer rasterLayer2) {
        return rasterLayer1 != null && rasterLayer2 != null && rasterLayer1.getName()
                .equals(rasterLayer2.getName());
    }

    private static boolean sameRasterLayer(WSIMapRasterLayer rasterLayer1, String rasterLayer2Id) {
        return rasterLayer1 != null && rasterLayer1.getName().equals(rasterLayer2Id);
    }

    public void onCreateLifecycleEvent() {
    }

    public void onStartLifecycleEvent() {
        this.onStart();
    }

    public void onResumeLifecycleEvent() {
        this.onResume();
    }

    public void onPauseLifecycleEvent() {
        this.onPause();
    }

    public void onStopLifecycleEvent() {
        this.onStop();
    }

    public void onDestroyLifecycleEvent() {
        try {
            onStopLifecycleEvent();
            this.pinMarkers.clear();
            this.mapViewChangedCallbacks.clear();
            this.setDelegate(null);
            this.setCalloutPresentation(null);
            this.getWSIMap().setDelegate(null);
            super.setOnMapReadyCallback(null);
            this.onDestroy();
            removeAllViews();
        } catch (Throwable tr) {
            ALog.e.tagMsg(this, "onDestroyLifecycleEvent ", tr);
        }
    }

    // ---------------------------------------------------------------------------------------------
    private void commonInit(@NonNull Context context) {
        doReadyOnce = true;
        pref = context.getSharedPreferences(mapName, Context.MODE_PRIVATE);
        mapType = WSIMapType.LIGHT;
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = metrics.widthPixels / metrics.scaledDensity;
        BIG_ICON_SCALE = (screenWidthDp > 500) ? 1f : 0.5f;
        this.setDelegate(this);
        this.getWSIMap().setDelegate(this);
        super.setOnMapReadyCallback(this);
        // initOverlays();
    }

    private void initOverlays() {
        List<InventoryOverlay> defOverlays = new ArrayList<>();
        // defOverlays.add(InventoryOverlay.WATCHWARNING_FLOOD_GLOBAL);
        // defOverlays.add(InventoryOverlay.WATCHWARNING_OTHER_GLOBAL);
        // defOverlays.add(InventoryOverlay.WATCHWARNING_TROPICAL_GLOBAL);
        // defOverlays.add(InventoryOverlay.WATCHWARNING_WINTER_GLOBAL);
        defOverlays.add(InventoryOverlay.SDK_WATCHWARNING_SEVERE_GLOBAL);
        defOverlays.add(InventoryOverlay.LIGHTNINGGLOBAL);
        setOverlayLayers(defOverlays);
    }

    @Override
    public void setOnMapReadyCallback(OnWSIMapViewReadyCallback readyCallback) {
        throw new IllegalStateException("use addOnMapChangedCallback");
    }

    public void addOnMapChangedCallback(OnWSIMapViewChangedCallback callback) {
        mapViewChangedCallbacks.add(callback);
    }

    public void removeOnMapChangedCallback(OnWSIMapViewChangedCallback callback) {
        mapViewChangedCallbacks.remove(callback);
    }

    public void clearOnMapChangedCallbacks() {
        mapViewChangedCallbacks.clear();
    }

    @Override
    public void onMapReady(WSIMapView wsiMapView) {
        if (this.isReady() && WSIMapView.isAuthorized() && doReadyOnce) {
            doReadyOnce = false;
            // wsiMapView.getWSIMap().setMapType(mapType);
            getWSIMap().setAttributionGravityMargins(Gravity.TOP | Gravity.RIGHT, -1, 20, 20, -1);
            ALog.d.tagMsg(this, "Map onMapReady");
            this.post(new Runnable() {
                @Override
                public void run() {
                    initWSIMap();
                    executeChangedCallbacks(wsiMapView, OnWSIMapViewChangedCallback.MAP_STATE_READY);
                }
            });
        } else {
            AppLog.LOG_MAP.w().tagMsg(this, "Map not ready or not authorized");
        }
    }

    synchronized
    private void executeChangedCallbacks(WSIMapView wsiMapView, int why) {
        ALog.d.tagMsg(this, "Map executeChangedCallbacks why=", why);
        for (OnWSIMapViewChangedCallback callback : mapViewChangedCallbacks) {
            callback.onMapReady(wsiMapView, why);
        }
    }

    @SuppressLint("SwitchIntDef")
    private void initWSIMap() {
        final WSIMap wsiMap = this.getWSIMap();
        wsiMap.clearOverlayLayers();
        wsiMap.clearRasterLayers();

        // Set lightning center.
        WSIMapView.setLightningLocation(START_POS.getLatitude(), START_POS.getLongitude());

        restoreMapViewState();
        initOverlays();

        // Move Map Attribution so it is always visible, reserve space for legend.
        int px = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                getResources().getDisplayMetrics()));
        switch (getResources().getConfiguration().orientation) {
            default:
                // case Configuration.ORIENTATION_SQUARE:
            case Configuration.ORIENTATION_UNDEFINED:
            case Configuration.ORIENTATION_LANDSCAPE:
                wsiMap.setAttributionGravityMargins(Gravity.TOP | Gravity.RIGHT, 0, 0, 0, 0);
                break;
            case Configuration.ORIENTATION_PORTRAIT:
                wsiMap.setAttributionGravityMargins(Gravity.TOP | Gravity.RIGHT, 0, 0, 0, px);
                break;

        }

        // Set optional callback delegate to monitor data loading progress.
        // wsiMap.setDelegate(this);

        // Override default looping speed.
        final WSIMapViewController wsiController = this.getWSIMapViewController();
        wsiController.setRasterLayerFrameLoopingSpeed(
                TimeUnit.MINUTES.toMillis(5),   // data step (5 minutes)
                TimeUnit.SECONDS.toMillis(8),   // total play time
                TimeUnit.SECONDS.toMillis(2),   // dwell "now"  [Not currently implemented]
                TimeUnit.SECONDS.toMillis(2));  // dwell "end"

        //EventBus.getDefault().register(this);   // See onMapLongTouch
        // getWSIMap().getEventBus().register(this);
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onMapLongTouch(MapLongTouchEvent mapTouchEvent) {
        ALog.d.tagMsg(this, "onMapLongTouch");
        if (longTouchListener != null) {
            longTouchListener.onLongTouch(this, mapTouchEvent);
        }
    }

    public void showRadar(boolean checked) {
        if (checked)
            setDefaultRasterLayer();
        else
            this.getWSIMap().clearRasterLayers();
    }
    public void showDDI(boolean checked) {
        if (checked) {
            showRaster("RoadWeather");
            // showRaster("TrafficFlowsOverRoadWeather");
        } else {
            this.getWSIMap().clearRasterLayers();
        }
    }
    public void showWind(boolean checked) {
        showOverlay("Windstream", checked);
    }

    public interface MapLongTouchListener {
        void onLongTouch(MapViewer mapViewer, MapLongTouchEvent mapLongTouchEvent);
    }
    MapLongTouchListener longTouchListener = null;
    public void setOnLongClickListener(MapLongTouchListener longTouchListener) {
        this.longTouchListener = longTouchListener;
    }

    /**
     * Save map camera and map style in preferences for later restore.
     * <p>
     * NOTE - SharedPreferences does IO and should be perform using background thread.
     * This implementation is lazy and does it on main thread and could get flagged by StrictMode.
     */
    private void saveMapViewState() {
        if (this.isReady()) {
            WLatLng latlng = WCameraPosition.getTarget(this.getWSIMap().getCameraPosition());
            double zoom = this.getWSIMap().getCameraPosition().getZoom();
            String activeRasterId = (this.getWSIMap().getActiveRasterLayer() != null) ?
                    this.getWSIMap().getActiveRasterLayer().getName() : "";

            // Save map state - camera, zoom, map type, units, raster
            pref.edit().putFloat(PREF_CAMERA_LAT, (float) latlng.getLatitude())
                    .putFloat(PREF_CAMERA_LNG, (float) latlng.getLongitude())
                    .putFloat(PREF_CAMERA_ZOOM, (float) zoom)
                    .putString(PREF_MAP_TYPE, this.getWSIMap().getMapType().name())
                    .putString(PREF_RASTER_LAYER_ID, activeRasterId)
                    .apply();

            // Save active overlays
            List<WSIMapGeoOverlay> overlays = this.getWSIMap().getAllGeoOverlays();
            Set<String> overlaysOn = new HashSet<>();
            for (WSIMapGeoOverlay overlay : overlays) {
                if (overlay.isTurnedOn(this)) {
                    overlaysOn.add(overlay.getName());
                }
            }

            pref.edit().putStringSet(PREF_OVERLAYS_ON, overlaysOn).apply();
        }
    }

    /**
     * Restore saved map state across sessions not just configuration changes.
     * <p>
     * NOTE - SharedPreferences does IO and should be perform using background thread.
     * This implementation is lazy and does it on main thread and could get flagged by StrictMode.
     */
    private void restoreMapViewState() {
        if (this.isReady()) {
            WSIMap wsiMap = this.getWSIMap();

            // Foce map camera to specific location on startup and rotation if not already set.
            WLatLng cameraPos = WCameraPosition.getTarget(wsiMap.getCameraPosition());
            if (MapUtils.isSimilar(ZERO_POS, cameraPos)) {
                WLatLng oldPos = new WLatLng(
                        ObjUtils.getPref(pref, PREF_CAMERA_LAT, 0f),
                        ObjUtils.getPref(pref, PREF_CAMERA_LNG, 0f));
                float oldZoom = ObjUtils.getPref(pref, PREF_CAMERA_ZOOM, 0f);
                if (MapUtils.isSimilar(oldPos, ZERO_POS)
                        && !MapUtils.isSimilar(START_POS, WCameraPosition.getTarget(wsiMap.getCameraPosition()))) {
                    oldPos = START_POS;
                    oldZoom = START_ZOOM;
                }
                wsiMap.moveCamera(WCameraPosition.newLatLngZoom(oldPos, oldZoom));
            }

            setMapType(mapType);

            setDefaultRasterLayer();
            // setActiveRasterLayer(getContext(), this, findRasterLayer(this, mapName));

            // Restore map Overlay state
            Set<String> overlaysOn = pref.getStringSet(PREF_OVERLAYS_ON, null);
            if (overlaysOn != null) {
                /*
                for (String overlayName : overlaysOn) {
                    setActiveOverlayByName(getContext(), mapView, overlayName);
                }
                 */
            } else if (showOverlays != null) {
                setOverlayLayers(showOverlays);
            }
        }
    }

    @Nullable
    public WSIMarkerView moveMarker(@NonNull WLatLng cameraPos, @DrawableRes int markerRes, float markerScale) {
        WSIMarkerView pinMarker = null;
        if (isReady()) {
            WSIMap wsiMap = this.getWSIMap();
            com.weather.pangea.model.overlay.Icon icon = DrawUtils
                    .getMarkerIconFromDrawableSized(getContext(), markerRes, null);

            pinMarker = pinMarkers.get(markerRes);
            if (pinMarker != null) {
                if (pinMarker.getPosition().equals(cameraPos)) {
                    return pinMarker;
                }
                wsiMap.removeMarker(pinMarker);
            }
            pinMarker = wsiMap.addMarker(new WSIMarkerViewOptions()
                    .position(cameraPos)
                    .icon(icon));
            pinMarker.setScaleFactor(markerScale);
            pinMarkers.put(markerRes, pinMarker);

            WSIMapView.setLightningLocation(cameraPos.getLatitude(), cameraPos.getLongitude());
        }
        return pinMarker;
    }

    public void removeMarker(@DrawableRes int markerRes) {
        WSIMarkerView pinMarker = pinMarkers.get(markerRes);
        if (pinMarker != null && isReady()) {
            getWSIMap().removeMarker(pinMarker);
        }
    }

    public void clearMarkers() {
        if (isReady()) {
            WSIMap wsiMap = this.getWSIMap();
            for (WSIMarkerView pinMarker : pinMarkers.values()) {
                wsiMap.removeMarker(pinMarker);
            }
        }
        pinMarkers.clear();
    }

    public SimpleOverlayLayer getTopLayer() {
        if (isReady()) {
            Layer layer = this.getWSIMap().getLayer(0);
            if (layer instanceof SimpleOverlayLayer) {
                return (SimpleOverlayLayer)layer;
            }
        }
        return null;
    }

    @NonNull
    public WSIMapType getMapType() {
        if (this.isReady()) {
            return this.getWSIMap().getMapType();
        }
        return this.mapType;
    }

    public void setMapType(@NonNull WSIMapType mapType) {
        this.mapType = mapType;
        if (this.isReady()) {
            this.getWSIMap().setMapType(mapType);
        }
    }

    public void setCameraBounds(@NonNull WLatLngBounds bounds, int durationMs) {
        getWSIMap().animateCamera(bounds, durationMs);
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean setCamera(@NonNull WLatLng cameraPos, float zoomLevel, boolean isGpsMarker, float iconScale) {
        if (!MapUtils.isSimilar(ZERO_POS, cameraPos)) {
            if (zoomLevel == -1) zoomLevel = (float)getWSIMap().getCameraPosition().getZoom();
            getWSIMap().moveCamera(WCameraPosition.newLatLngZoom(cameraPos, zoomLevel));
            moveMarker(cameraPos, isGpsMarker ? GPS_MARKER : CITY_MARKER, iconScale);

            if (pref != null) {
                pref.edit()     // TODO - is this really needed
                        .putFloat(PREF_CAMERA_LAT, (float) cameraPos.latitude)
                        .putFloat(PREF_CAMERA_LNG, (float) cameraPos.longitude)
                        .putFloat(PREF_CAMERA_ZOOM, zoomLevel)
                        .apply();
            }
            return true;
        }
        return false;
    }

    public void setFrameMilli(long milli) {
        getWSIMap().setActiveRasterLayerTilesTime(milli, WSIMapSelectMode.FLOOR);
    }
    public long getFrameMilli() {
        return getWSIMap().getFrameTime();
    }

    public boolean setRasterLayer(@NonNull String layerName) {
        WSIMapRasterLayer rasterLayer = findRasterLayer(this, layerName);
        if (rasterLayer != null) {
            setActiveRasterLayer(getContext(), this, rasterLayer);
            if (pref != null) {
                pref.edit().putString(PREF_RASTER_LAYER_ID, layerName).apply();
            }
            return true;
        }
        return false;
    }

    public boolean setOverlayLayers(@Nullable List<InventoryOverlay> showOverlays) {
        this.showOverlays = showOverlays;
        WSIMap wsiMap = this.getWSIMap();
        if (wsiMap.isReady())  wsiMap.clearOverlayLayers();
        boolean foundAll = true;

        if (showOverlays != null) {
            List<WSIMapGeoOverlay> availableOverlays = wsiMap.getAllGeoOverlays();
            for (InventoryOverlay overlay : showOverlays) {
                @SuppressLint("RestrictedApi")
                WSIMapGeoOverlay layer = findOverlay(availableOverlays, overlay.name);
                if (layer != null) {
                    wsiMap.addOverlay(layer, this);
                } else {
                    foundAll = false;
                }
            }
        }
        return foundAll;
    }

    @Nullable
    private WSIMapGeoOverlay findOverlay(List<WSIMapGeoOverlay> overlays, String findName) {
        for (WSIMapGeoOverlay overlay : overlays) {
            if (overlay.getName().equals(findName)) {
                return overlay;
            }
        }
        return null;
    }

    /**
     * Set default raster layer if none currently active.
     */
    private void setDefaultRasterLayer() {
        showRaster( DEF_MAP_RASTER);
    }

    private void showRaster(String rasterName) {
        if (this.isReady()) {
            WSIMapRasterLayer rasterLayer = findRasterLayer(this, rasterName);
            if (rasterLayer != null) {
                setActiveRasterLayer(getContext(), this, rasterLayer);
                return;
            }
        }
        Toast.makeText(getContext(), rasterName + " Raster not available", Toast.LENGTH_LONG).show();
    }

    private void showOverlay(@NonNull String overlayName, boolean enable) {
        if (this.isReady()) {
            WSIMap wsiMap = this.getWSIMap();
            List<WSIMapGeoOverlay> availableOverlays = wsiMap.getAllGeoOverlays();
            @SuppressLint("RestrictedApi")
            WSIMapGeoOverlay layer = findOverlay(availableOverlays, overlayName);
            if (layer != null) {
                if (enable) {
                    wsiMap.addOverlay(layer, this);
                } else {
                    wsiMap.removeOverlay(layer);
                }
            } else {
                Toast.makeText(getContext(), overlayName + " Overlay not available", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void play(boolean isLooping) {
        if (isReady()) {
            getWSIMap().setActiveRasterLayerDataDisplayMode(isLooping
                    ? WSIMapRasterLayerDataDisplayMode.LOOPING
                    : WSIMapRasterLayerDataDisplayMode.STATIC);
        }
    }

    public boolean isPlaying() {
        return isReady() &&
                getWSIMap().getActiveRasterLayerDataDisplayMode() == WSIMapRasterLayerDataDisplayMode.LOOPING;
    }

    public void setTimeline(DateTime time) {
        setTimeline(time, this);
    }

    public static void setTimeline(@NonNull DateTime time, @NonNull WSIMapView mapView) {
        if (mapView.isReady()) {
            boolean isFuture = DateTime.now().isBefore(time);
            mapView.getWSIMap().setActiveRasterLayerTimeDisplayMode(isFuture
                    ? WSIMapRasterLayerTimeDisplayMode.PAST_FUTURE
                    : WSIMapRasterLayerTimeDisplayMode.PAST);
            mapView.getWSIMap().setActiveRasterLayerTilesTime(time.getMillis(), isFuture
                    ? WSIMapSelectMode.NEAREST
                    : WSIMapSelectMode.FLOOR);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // OnWSIMapViewReadyCallback
    @Override
    public void onDismissGeoCalloutView(View view, Object o) {
    }

    @Override
    public View onOpenGeoCalloutView(@NonNull WSIMapView wsiMapView, @NonNull WSIMapCalloutInfoList wsiMapCalloutInfoList, Object o) {
        return null;
    }

    @Override
    public boolean isGeoCalloutOpen(@NonNull View view, @Nullable Object o) {
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // WSIMapDelegate
    @Override
    public void onActiveRasterLayerTilesFrameChanged(
            int playIdx, int totalPlaySteps,
            long playTimeMilli, long imageTimeMilli,
            @Nullable WSIRasterLayerLoopTimes loopTimes) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onActiveRasterLayerTilesFrameChanged");
        executeChangedCallbacks(this, OnWSIMapViewChangedCallback.MAP_STATE_TIMES_CHANGED);
    }

    @Override
    public void onActiveRasterLayerTilesUpdateFailed(String s) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onActiveRasterLayerTilesUpdateFailed");
    }

    @Override
    public void onActiveRasterLayerDataDisplayModeChanged(WSIMapRasterLayerDataDisplayMode wsiMapRasterLayerDataDisplayMode) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onActiveRasterLayerDataDisplayModeChanged");
    }

    @Override
    public void onActiveRasterLayerTimeDisplayModeChanged(WSIMapRasterLayerTimeDisplayMode wsiMapRasterLayerTimeDisplayMode) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onActiveRasterLayerTimeDisplayModeChanged");
    }

    @Override
    public void onActveRasterLayerLoopTimesChanged(WSIRasterLayerLoopTimes wsiRasterLayerLoopTimes) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onActveRasterLayerLoopTimesChanged");
        executeChangedCallbacks(this, OnWSIMapViewChangedCallback.MAP_STATE_TIMES_CHANGED);
    }

    @Override
    public void onActiveRasterLayerChanged(@NonNull WSIMapRasterLayer layer) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onActiveRasterLayerChanged");
    }

    @Override
    public void onGeoOverlayUpdated(WSIMapGeoOverlay wsiMapGeoOverlay) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onGeoOverlayUpdated");
    }

    @Override
    public void onGeoOverlayUpdateFailed(WSIMapGeoOverlay wsiMapGeoOverlay) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onGeoOverlayUpdateFailed");
    }

    @Override
    public void onDataStartLoading() {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onDataStartLoading");
        executeChangedCallbacks(this, OnWSIMapViewChangedCallback.MAP_STATE_START_LOADING);
    }

    @Override
    public void onDataCompleted() {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onDataCompleted");
        executeChangedCallbacks(this, OnWSIMapViewChangedCallback.MAP_STATE_COMPLETED_LOADING);
    }

    @Override
    public void onDataFailed(String s) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onDataFailed");
        // executeChangedCallbacks(this, OnWSIMapViewChangedCallback.MAP_STATE_ERROR_LOADING);
    }

    @Override
    public void onTimeChanged(String s, long l) {
        AppLog.LOG_MAP.i().tagMsg(TAG, "onTimeChanged");
    }

    /* Debug
    DO NOT DELETE THIS CODE
    public void addRectangle(WLatLngBounds bounds, @ColorInt int lineColor, int strokeWidth, String name) {
        Layer layer = getWSIMap().getLayer(0);
        if (layer instanceof SimpleOverlayLayer) {
            SimpleOverlayLayer overlayLayer = (SimpleOverlayLayer) layer;
            Overlay overlay = overlays.get(name);
            if (overlay != null)
                overlayLayer.removeOverlay(overlay);

            List<LatLng> points = new ArrayList<>(5);
            points.add(bounds.northeast.toPangeaLL());
            points.add(new LatLng(bounds.southwest.latitude, bounds.northeast.longitude));
            points.add(new LatLng(bounds.southwest.latitude, bounds.southwest.longitude));
            points.add(new LatLng(bounds.northeast.latitude, bounds.southwest.longitude));
            points.add(bounds.northeast.toPangeaLL());
            List<Polyline> polylines = new ArrayList<>(1);

            overlay = new PolylinePathBuilder()
                        .setPolyLine(new Polyline(points))
                        .setStrokeStyle(new StrokeStyleBuilder().setColor(lineColor).setWidth(strokeWidth).build())
                        .build();
            overlays.put(name, overlay);
            overlayLayer.addOverlay(overlay);
        }
    }
     */

    public interface OnWSIMapViewChangedCallback {
        int MAP_STATE_READY = 1;
        int MAP_STATE_TIMES_CHANGED = 2;
        int MAP_STATE_START_LOADING = 3;
        int MAP_STATE_COMPLETED_LOADING = 4;
        int MAP_STATE_ERROR_LOADING = 5;

        void onMapReady(WSIMapView wsiMapView, int why);
    }
}
