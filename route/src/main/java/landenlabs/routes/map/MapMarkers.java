/*
 * Dennis Lang - LanDenLabs.com
 * Copyright (c) 2026 Dennis Lang (LanDen Labs)
 */

package landenlabs.routes.map;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.weather.mapsdk.TWCMapView;
import com.weather.mapsdk.props.TWCMapLatLng;
import com.weather.mapsdk.props.TWCMapMarker;

import landenlabs.routes.R;
import landenlabs.wx_lib_data.location.WLatLng;

import java.util.HashMap;
import java.util.Map;

/**
 * Manage Map markers
 */
public class MapMarkers {

    public static final String START_MARKER = "Start";
    public static final String POS_MARKER = "Now";
    public static final String END_MARKER = "End";
    private final Map<String, TWCMapMarker> markers = new HashMap<>();
    private TWCMapView map;

    @DrawableRes
    private final Map<String, Integer> iconRes = new HashMap<>();
    @DrawableRes
    private final int defIconRes;

    public MapMarkers(@NonNull Context context, TWCMapView map) {
        this.map = map;

        iconRes.put(START_MARKER, R.drawable.ic_map_pin_green);
        iconRes.put(POS_MARKER, R.drawable.ic_map_pin_drive);
        iconRes.put(END_MARKER, R.drawable.ic_map_pin_red);
        defIconRes = R.drawable.ic_map_pin_def;
    }

    public static void done(MapMarkers mapMarkers) {
        if (mapMarkers != null) {
            mapMarkers.done();
        }
    }
    public void done() {
        clearMarkers();
        map = null;
    }

    synchronized
    public void clearMarkers() {
        for (String key : markers.keySet()) {
            map.removeImageMarker(key);
        }
        markers.clear();
    }

    synchronized
    public void clearMarker(String name) {
        if (markers.remove(name) != null) {
            map.removeImageMarker(name);
        }
    }

    synchronized
    public TWCMapMarker addMarker(WLatLng location, String key) {
        int drawableRes = iconRes.containsKey(key) ? iconRes.get(key) : defIconRes;
        return addMarker(location, key, drawableRes, 1f);
    }

    synchronized
    public TWCMapMarker addMarker(WLatLng location, String key, @DrawableRes int drawableRes, float scale) {
        Drawable drawable = ContextCompat.getDrawable(map.getContext(), drawableRes);
        int widthPx = Math.max(1, Math.round((drawable != null ? drawable.getIntrinsicWidth() : 1) * scale));
        int heightPx = Math.max(1, Math.round((drawable != null ? drawable.getIntrinsicHeight() : 1) * scale));
        TWCMapMarker marker = map.addImageMarker(drawableRes, widthPx, heightPx, key,
                new TWCMapLatLng(location.latitude, location.longitude));
        markers.put(key, marker);
        return marker;
    }
}
