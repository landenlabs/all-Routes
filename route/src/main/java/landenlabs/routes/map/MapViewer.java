/*
 * Dennis Lang - LanDenLabs.com
 * Copyright (c) 2026 Dennis Lang (LanDen Labs)
 */

package landenlabs.routes.map;

import static landenlabs.routes.utils.DataUtils.getString1x;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.weather.mapsdk.TWCMapSDK;
import com.weather.mapsdk.TWCMapSDKInitializer;
import com.weather.mapsdk.TWCMapView;
import com.weather.mapsdk.interfaces.TWCMapSDKEventCallbacks;
import com.weather.mapsdk.props.MapSdkOptions;
import com.weather.mapsdk.props.TWCMapBounds;
import com.weather.mapsdk.props.TWCMapLatLng;
import com.weather.mapsdk.props.TWCMapMarker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import landenlabs.routes.R;
import landenlabs.routes.logger.AppLog;
import landenlabs.wx_lib_data.location.WLatLng;
import landenlabs.wx_lib_data.location.WLatLngBounds;
import landenlabs.wx_lib_data.logger.ALog;

/**
 * Manage TWC MapSdk (Pangea 5) map view.
 */
@SuppressWarnings({"Convert2Lambda", "unused", "UnnecessaryLocalVariable", "CommentedOutCode"})
public class MapViewer extends TWCMapView {

    //  Initial map state
    private static final WLatLng START_POS = new WLatLng(35.0, -90.0);
    private static final float START_ZOOM = 5f;
    private static final WLatLng ZERO_POS = new WLatLng(0, 0);

    // Keys used to manage UI state.
    private static final String PREF_CAMERA_LAT = "cameraLat";
    private static final String PREF_CAMERA_LNG = "cameraLng";
    private static final String PREF_CAMERA_ZOOM = "cameraZoom";
    private static final String PREF_RASTER_LAYER_ID = "mapRasterLayer";
    private static final String TAG = "MapViewer";

    public static float     BIG_ICON_SCALE = 1f;
    public static final int GPS_MARKER = R.drawable.ic_map_pin_drive;
    public static final int CITY_MARKER = R.drawable.ic_map_marker;
    // TWCMapSDKRasterLayerType name - see com.weather.mapsdk.enums.TWCMapSDKRasterLayerType
    public static final String DEF_MAP_RASTER = "Radar";
    private static final String WIND_RASTER = "WindSpeed"; // closest mapsdk-v2 equivalent to old animated "Windstream" overlay

    private static boolean sApiKeyRegistered = false;

    private final Set<OnWSIMapViewChangedCallback> mapViewChangedCallbacks = new HashSet<>();
    private final Map<Integer, TWCMapMarker> pinMarkers = new HashMap<>();
    private SharedPreferences pref;
    private static final String MAP_NAME = "Map1";
    private String mapName = MAP_NAME;
    @Nullable
    private String activeRasterId = null;
    private boolean doReadyOnce = true;

    // ---------------------------------------------------------------------------------------------
    // Conversions between our WLatLng/WLatLngBounds and the MapSdk TWCMapLatLng/TWCMapBounds types.
    private static WLatLng toWLatLng(TWCMapLatLng ll) {
        return new WLatLng(ll.getLatitude(), ll.getLongitude());
    }

    private static TWCMapLatLng toTWCLatLng(WLatLng ll) {
        return new TWCMapLatLng(ll.latitude, ll.longitude);
    }

    private static TWCMapBounds toTWCBounds(WLatLngBounds bounds) {
        return new TWCMapBounds(
                toTWCLatLng(bounds.northeast),
                toTWCLatLng(bounds.southwest));
    }

    // ---------------------------------------------------------------------------------------------
    // Construct view

    public MapViewer(@NonNull Context context, @Nullable AttributeSet attributeSet) {
        super(context, attributeSet);
        commonInit(context);
    }

    public MapViewer(@NonNull Context context, @Nullable AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        commonInit(context);
    }

    /**
     * Register the MapSdk API key. Safe to call repeatedly - only registers once.
     */
    public static void initBeforeCreate(@NonNull Context context) {
        if (!sApiKeyRegistered) {
            // String apiKey = getString1x(context, "app_name");
            String foo = context.getString(R.string.mapSDKKey);
            MapSdkOptions options = new MapSdkOptions.Builder(foo).build();
            TWCMapSDKInitializer.Companion.get(context).register(options);
            sApiKeyRegistered = true;
        }
    }

    public static boolean isMapSdkAuthorized() {
        return TWCMapSDK.Companion.isAuthenticated();
    }

    public static WLatLng getCameraPos(@Nullable WLatLng latlng, @Nullable WLatLng defCameraPos) {
        return (latlng == null) ? defCameraPos : latlng;
    }

    // ---------------------------------------------------------------------------------------------
    private void commonInit(@NonNull Context context) {
        doReadyOnce = true;
        pref = context.getSharedPreferences(mapName, Context.MODE_PRIVATE);
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        float screenWidthDp = metrics.widthPixels / metrics.scaledDensity;
        BIG_ICON_SCALE = (screenWidthDp > 500) ? 1f : 0.5f;

        setLongPressListener(touch -> {
            if (longTouchListener != null) {
                longTouchListener.onLongTouch(this, touch);
            }
        });

        setTWCMapSDKEventCallbacks(new TWCMapSDKEventCallbacks() {
            @Override
            public void onStyleChanged(String style) {
            }

            @Override
            public void onLayerChanged(com.weather.mapsdk.layers.TWCMapSDKLayer layer) {
            }

            @Override
            public void onFrameTimeChanged(long milli) {
                executeChangedCallbacks(OnWSIMapViewChangedCallback.MAP_STATE_TIMES_CHANGED);
            }

            @Override
            public void onLoading(LoadingStatus status) {
                if (status == LoadingStatus.Start) {
                    executeChangedCallbacks(OnWSIMapViewChangedCallback.MAP_STATE_START_LOADING);
                } else if (status == LoadingStatus.Done) {
                    executeChangedCallbacks(OnWSIMapViewChangedCallback.MAP_STATE_COMPLETED_LOADING);
                } else if (status == LoadingStatus.Error) {
                    executeChangedCallbacks(OnWSIMapViewChangedCallback.MAP_STATE_ERROR_LOADING);
                }
            }
        });
    }

    private boolean startedInit = false;

    /**
     * Start map initialization; onReady callback (added via addOnMapChangedCallback) fires when ready.
     * Safe to call repeatedly (e.g. every onResume) - only starts once per view instance.
     */
    public void startInit() {
        if (startedInit) {
            return;
        }
        startedInit = true;
        initMap(result -> {
            if (result.getMapView() != null && isReady() && isMapSdkAuthorized() && doReadyOnce) {
                doReadyOnce = false;
                setAttribution(com.weather.mapsdk.props.TWCMapAnchor.TOP_RIGHT, 0.0, 0.0);
                ALog.d.tagMsg(this, "Map onMapReady");
                post(() -> {
                    initTWCMap();
                    executeChangedCallbacks(OnWSIMapViewChangedCallback.MAP_STATE_READY);
                });
            } else {
                AppLog.LOG_MAP.w().tagMsg(this, "Map not ready or not authorized ", result.getErrorMsg());
            }
        });
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

    synchronized
    private void executeChangedCallbacks(int why) {
        // ALog.d.tagMsg(this, "Map executeChangedCallbacks why=", why);
        for (OnWSIMapViewChangedCallback callback : mapViewChangedCallbacks) {
            callback.onMapReady(this, why);
        }
    }

    @SuppressLint("SwitchIntDef")
    private void initTWCMap() {
        clearOverlays();
        clearRasters();
        activeRasterId = null;

        restoreMapViewState();

        // Default overlays shown on first load.
        setOverlayLayers(true);
    }

    public void showRadar(boolean checked) {
        if (!isReady()) {
            return;
        }
        if (checked) {
            setDefaultRasterLayer();
        } else {
            clearRasters();
            activeRasterId = null;
        }
    }

    public void showDDI(boolean checked) {
        if (!isReady()) {
            return;
        }
        if (checked) {
            showRaster("RoadWeather");
        } else {
            clearRasters();
            activeRasterId = null;
        }
    }

    public void showWind(boolean checked) {
        if (!isReady()) {
            return;
        }
        if (checked) {
            addRaster(WIND_RASTER);
        } else {
            removeRaster(WIND_RASTER);
        }
    }

    public interface MapLongTouchListener {
        void onLongTouch(MapViewer mapViewer, com.weather.mapsdk.interfaces.TWCMapTouch touch);
    }
    MapLongTouchListener longTouchListener = null;
    public void setOnLongClickListener(MapLongTouchListener longTouchListener) {
        this.longTouchListener = longTouchListener;
    }

    /**
     * Restore saved map state across sessions not just configuration changes.
     * <p>
     * NOTE - SharedPreferences does IO and should be perform using background thread.
     * This implementation is lazy and does it on main thread and could get flagged by StrictMode.
     */
    private void restoreMapViewState() {
        if (isReady()) {
            // Force map camera to specific location on startup if not already set.
            TWCMapLatLng center = getCenter();
            WLatLng cameraPos = (center != null) ? toWLatLng(center) : ZERO_POS;
            if (MapUtils.isSimilar(ZERO_POS, cameraPos)) {
                WLatLng oldPos = new WLatLng(
                        pref.getFloat(PREF_CAMERA_LAT, 0f),
                        pref.getFloat(PREF_CAMERA_LNG, 0f));
                float oldZoom = pref.getFloat(PREF_CAMERA_ZOOM, 0f);
                if (MapUtils.isSimilar(oldPos, ZERO_POS) && !MapUtils.isSimilar(START_POS, cameraPos)) {
                    oldPos = START_POS;
                    oldZoom = START_ZOOM;
                }
                setCenter(toTWCLatLng(oldPos), (double) oldZoom, false);
            }

            setDefaultRasterLayer();
        }
    }

    @Nullable
    public TWCMapMarker moveMarker(@NonNull WLatLng cameraPos, @DrawableRes int markerRes, float markerScale) {
        TWCMapMarker pinMarker = null;
        if (isReady()) {
            pinMarker = pinMarkers.get(markerRes);
            if (pinMarker != null && MapUtils.isSimilar(toWLatLng(pinMarker.getLatLng()), cameraPos)) {
                return pinMarker;
            }

            Drawable drawable = ContextCompat.getDrawable(getContext(), markerRes);
            int widthPx = Math.max(1, Math.round((drawable != null ? drawable.getIntrinsicWidth() : 1) * markerScale));
            int heightPx = Math.max(1, Math.round((drawable != null ? drawable.getIntrinsicHeight() : 1) * markerScale));
            pinMarker = addImageMarker(markerRes, widthPx, heightPx, "pin" + markerRes, toTWCLatLng(cameraPos));
            pinMarkers.put(markerRes, pinMarker);
        }
        return pinMarker;
    }

    public void removeMarker(@DrawableRes int markerRes) {
        TWCMapMarker pinMarker = pinMarkers.remove(markerRes);
        if (pinMarker != null && isReady()) {
            removeImageMarker("pin" + markerRes);
        }
    }

    public void clearPinMarkers() {
        if (isReady()) {
            for (Integer markerRes : pinMarkers.keySet()) {
                removeImageMarker("pin" + markerRes);
            }
        }
        pinMarkers.clear();
    }

    public void setCameraBounds(@NonNull WLatLngBounds bounds) {
        if (isReady()) {
            setBounds(toTWCBounds(bounds));
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public boolean setCamera(@NonNull WLatLng cameraPos, float zoomLevel, boolean isGpsMarker, float iconScale) {
        if (isReady() && !MapUtils.isSimilar(ZERO_POS, cameraPos)) {
            if (zoomLevel == -1) zoomLevel = (float) getZoomLevel();
            setCenter(toTWCLatLng(cameraPos), (double) zoomLevel, true);
            moveMarker(cameraPos, isGpsMarker ? GPS_MARKER : CITY_MARKER, iconScale);

            if (pref != null) {
                pref.edit()
                        .putFloat(PREF_CAMERA_LAT, (float) cameraPos.latitude)
                        .putFloat(PREF_CAMERA_LNG, (float) cameraPos.longitude)
                        .putFloat(PREF_CAMERA_ZOOM, zoomLevel)
                        .apply();
            }
            return true;
        }
        return false;
    }

    public void setTimeline(org.joda.time.DateTime time) {
        if (isReady()) {
            setFrameMilli(time.getMillis());
        }
    }

    public boolean setRasterLayer(@NonNull String layerName) {
        if (!isReady()) {
            return false;
        }
        if (!layerName.equals(activeRasterId)) {
            clearRasters();
            if (!addRaster(layerName)) {
                activeRasterId = null;
                AppLog.LOG_MAP.w().tagMsg(mapName, "Failed to set map raster layer " + layerName);
                return false;
            }
            activeRasterId = layerName;
            if (pref != null) {
                pref.edit().putString(PREF_RASTER_LAYER_ID, layerName).apply();
            }
        }
        return true;
    }

    /**
     * Set default overlays. Old app defaulted to Severe watch/warning alerts + Lightning.
     */
    public boolean setOverlayLayers(boolean showDefaults) {
        clearOverlays();
        boolean foundAll = true;
        if (showDefaults) {
            foundAll &= addOverlay("Severe");
            foundAll &= addOverlay("Lightning");
        }
        return foundAll;
    }

    /**
     * Set default raster layer if none currently active.
     */
    private void setDefaultRasterLayer() {
        showRaster(DEF_MAP_RASTER);
    }

    private void showRaster(String rasterName) {
        setRasterLayer(rasterName);
    }

    public interface OnWSIMapViewChangedCallback {
        int MAP_STATE_READY = 1;
        int MAP_STATE_TIMES_CHANGED = 2;
        int MAP_STATE_START_LOADING = 3;
        int MAP_STATE_COMPLETED_LOADING = 4;
        int MAP_STATE_ERROR_LOADING = 5;

        void onMapReady(MapViewer mapViewer, int why);
    }
}
