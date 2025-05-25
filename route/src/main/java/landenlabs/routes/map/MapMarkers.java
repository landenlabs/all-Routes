/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.map;

import android.content.Context;

import androidx.annotation.NonNull;

import com.landenlabs.routes.R;
import com.wsi.mapsdk.map.WSIMap;
import com.wsi.mapsdk.markers.WSIMarkerView;
import com.wsi.mapsdk.markers.WSIMarkerViewOptions;
import com.wsi.mapsdk.utils.DrawUtils;
import com.wsi.mapsdk.utils.WLatLng;

import java.util.HashMap;
import java.util.Map;

/**
 * Manage Map markers
 */
public class MapMarkers {

    public static final String START_MARKER = "Start";
    public static final String POS_MARKER = "Now";
    public static final String END_MARKER = "End";
    private final Map<String, WSIMarkerView> markers = new HashMap<>();
    private WSIMap map;

    private final Map<String, com.weather.pangea.model.overlay.Icon> icons = new HashMap<>();
    private final com.weather.pangea.model.overlay.Icon defIcon;

    public MapMarkers(@NonNull Context context, WSIMap map) {
        this.map = map;

        // DrawUtils.getMarkerIconFromDrawableSized(getContext(), markerRes, null);
        icons.put(START_MARKER, DrawUtils.getMarkerIconFromDrawableSized(context, R.drawable.ic_map_pin_green, null));
        icons.put(POS_MARKER, DrawUtils.getMarkerIconFromDrawableSized(context, R.drawable.ic_map_pin_drive, null));
        icons.put(END_MARKER, DrawUtils.getMarkerIconFromDrawableSized(context, R.drawable.ic_map_pin_red, null));
        defIcon = DrawUtils.getMarkerIconFromDrawableSized(context, R.drawable.ic_map_pin_def, null);
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
        for (Map.Entry<String, WSIMarkerView> entry : markers.entrySet()) {
            map.removeMarker(entry.getValue());
        }
        markers.clear();
    }

    synchronized
    public void clearMarker(String name) {
        WSIMarkerView marker = markers.get(name);
        if (marker != null) {
            map.removeMarker(marker);
        }
        markers.remove(name);
    }

    synchronized
    public void addMarker(WLatLng location, String key) {
        addMarker(location, key, icons.containsKey(key) ? icons.get(key) : defIcon);
    }
    synchronized
    public WSIMarkerView addMarker(WLatLng location, String key, com.weather.pangea.model.overlay.Icon icon) {
        WSIMarkerView marker = markers.get(key);
        if (marker != null) {
            map.removeMarker(marker);
        }
        marker = map.addMarker(new WSIMarkerViewOptions()
                .position(location)
                .icon(icon)
                .title(key));
        markers.put(key, marker);
        return marker;
    }
}
