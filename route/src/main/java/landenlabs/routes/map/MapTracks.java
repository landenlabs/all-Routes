/*
 * Dennis Lang - LanDenLabs.com
 * Copyright (c) 2026 Dennis Lang (LanDen Labs)
 */

package landenlabs.routes.map;

import android.graphics.Color;
import android.util.SparseArray;

import androidx.lifecycle.LifecycleOwner;

import com.weather.mapsdk.props.TWCMapLatLng;

import landenlabs.routes.data.LiveQueue;
import landenlabs.routes.data.RouteSettings;
import landenlabs.routes.data.RouteSettings.LineStyle;
import landenlabs.routes.data.Track;
import landenlabs.routes.data.TrackGrid;
import landenlabs.routes.data.TrackIdList;
import landenlabs.routes.utils.GpsUtils;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import landenlabs.wx_lib_data.location.WLatLngBounds;
import landenlabs.wx_lib_data.logger.ALog;


/**
 * Manage rendering diagnostic data on map using Track paths.
 */
public class MapTracks {
    private  MapViewer mapViewer;
    private  TrackGrid trackGrid;
    private final LiveQueue<Track> liveFullTracks;

    // Track polylines persist (keyed by Track.getKey()) so multiple selected tracks can coexist.
    private final Set<String> trackPolylineIds = new HashSet<>();
    // Debug (grid/bounds/cells) overlays are fully rebuilt on every observeTracks() call.
    private final Set<String> debugPolylineIds = new HashSet<>();
    private final Set<String> debugPolygonIds = new HashSet<>();
    private WLatLngBounds bounds = null;

    public static boolean  showBounds = false;
    public static boolean  showGrid = false;
    public static boolean  showCells = false;

    public static MapTracks create(
            MapTracks mapTracks,
            MapViewer mapViewer,
            TrackGrid trackGrid,
            LifecycleOwner owner) {
        done(mapTracks);
        return new MapTracks(mapViewer, trackGrid, owner);
    }

    public MapTracks(MapViewer mapViewer, TrackGrid trackGrid, LifecycleOwner owner) {
        this.mapViewer = mapViewer;
        this.trackGrid = trackGrid;
        this.liveFullTracks = new LiveQueue<>();
        liveFullTracks.observe(owner, this::observeTracks);
    }

    public static void done(MapTracks mapTracks) {
        if (mapTracks != null) {
            mapTracks.done();
        }
    }
    public void done() {
        ALog.d.tagMsg(this, "MapTracks done");
        liveFullTracks.clear();
        clearTrackPolylines();
        clearDebugOverlays();
        mapViewer = null;
        trackGrid = null;
        bounds = null;
    }

    private void clearTrackPolylines() {
        if (mapViewer != null) {
            for (String id : trackPolylineIds) {
                mapViewer.removeMarkerPolyline(id);
            }
        }
        trackPolylineIds.clear();
    }

    private void clearDebugOverlays() {
        if (mapViewer != null) {
            for (String id : debugPolylineIds) {
                mapViewer.removeMarkerPolyline(id);
            }
            for (String id : debugPolygonIds) {
                mapViewer.removeMarkerPolygon(id);
            }
        }
        debugPolylineIds.clear();
        debugPolygonIds.clear();
    }

    public void addTrack(Track track) {
        trackGrid.getTrackAsync(track.id, track).whenComplete((fullTrack, exception) -> {
            if (exception == null && fullTrack != null) {
                liveFullTracks.postValue(fullTrack);
            }
        });
    }

    private void addPolyline(String id, ArrayList<TWCMapLatLng> points, LineStyle style, Set<String> idSet) {
        mapViewer.addMarkerPolyline(id, points, style.argbColor(), style.getWidth(), style.getDashPattern());
        idSet.add(id);
    }

    private void addPolygon(String id, ArrayList<TWCMapLatLng> points, int fillColor, double fillOpacity, Set<String> idSet) {
        mapViewer.addMarkerPolygon(id, points, fillColor, fillOpacity, fillColor, 0);
        idSet.add(id);
    }

    public void observeTracks(Track fullTrack) {
        if (mapViewer == null || !mapViewer.isReady()) {
            return;
        }

        bounds = GpsUtils.union(bounds, fullTrack.getBounds());
        bounds = GpsUtils.minBounds(bounds, RouteSettings.minBoundsDeg);
        mapViewer.setCameraBounds(bounds);

        clearDebugOverlays();

        if (true) {
            // Draw track path.
            LineStyle lineStyle = RouteSettings.lineStyleStd;
            if (fullTrack.name.contains(Track.NAME_REV))
                lineStyle = RouteSettings.lineStyleRev;
            else if (fullTrack.name.contains(Track.NAME_TEST))
                lineStyle = RouteSettings.lineStyleTest;
            addPolyline(fullTrack.getKey(), fullTrack.toLatLngList(), lineStyle, trackPolylineIds);
        }

        float step = 1f / TrackGrid.scale1;
        if (showGrid && bounds != null) {
            // Draw Grid lines
            float minLat = TrackGrid.truncate(bounds.southwest.latitude) - step;
            float maxLat = TrackGrid.truncate(bounds.northeast.latitude) + step;
            float minLng = TrackGrid.truncate(bounds.southwest.longitude) - step;
            float maxLng = TrackGrid.truncate(bounds.northeast.longitude) + step;
            int gridIdx = 0;
            for (float lat = minLat; lat <= maxLat; lat += step) {
                ArrayList<TWCMapLatLng> polyline = new ArrayList<>((int) ((maxLng - minLng) / step) + 1);
                for (float lng = minLng; lng <= maxLng; lng += step) {
                    polyline.add(new TWCMapLatLng(lat, lng));
                }
                addPolyline("gridH" + (gridIdx++), polyline, RouteSettings.gridStyleRev, debugPolylineIds);
            }
            for (float lng = minLng; lng <= maxLng; lng += step) {
                ArrayList<TWCMapLatLng> polyline = new ArrayList<>((int) ((maxLng - minLng) / step) + 1);
                for (float lat = minLat; lat <= maxLat; lat += step) {
                    polyline.add(new TWCMapLatLng(lat, lng));
                }
                addPolyline("gridV" + (gridIdx++), polyline, RouteSettings.gridStyleRev, debugPolylineIds);
            }
        }

        if (showBounds && bounds != null) {
            // Draw bounding box.
            ArrayList<TWCMapLatLng> box = new ArrayList<>(5);
            TWCMapLatLng southEast = new TWCMapLatLng(bounds.northeast.latitude, bounds.southwest.longitude);
            TWCMapLatLng northWest = new TWCMapLatLng(bounds.southwest.latitude, bounds.northeast.longitude);
            box.add(northWest);
            box.add(new TWCMapLatLng(bounds.northeast.latitude, bounds.northeast.longitude));
            box.add(southEast);
            box.add(new TWCMapLatLng(bounds.southwest.latitude, bounds.southwest.longitude));
            box.add(northWest);

            addPolygon("bounds", box, Color.RED, 0.3, debugPolygonIds);
        }

        if (showCells && bounds != null) {
            // Draw grid cells
            for (int latIdx = 0; latIdx < trackGrid.grid.size(); latIdx++) {
                int latBoxI = trackGrid.grid.keyAt(latIdx);
                float latBoxF = latBoxI / (float) TrackGrid.scale3 - 90f;
                SparseArray<TrackIdList> lngArray = trackGrid.grid.valueAt(latIdx);

                for (int lngIdx = 0; lngIdx < lngArray.size(); lngIdx++) {
                    int lngBoxI = lngArray.keyAt(lngIdx);
                    float lngBoxF = lngBoxI / (float) TrackGrid.scale3 - 180f;

                    ArrayList<TWCMapLatLng> cell = new ArrayList<>(5);
                    cell.add(new TWCMapLatLng(latBoxF, lngBoxF));
                    cell.add(new TWCMapLatLng(latBoxF + step, lngBoxF));
                    cell.add(new TWCMapLatLng(latBoxF + step, lngBoxF + step));
                    cell.add(new TWCMapLatLng(latBoxF, lngBoxF + step));
                    cell.add(new TWCMapLatLng(latBoxF, lngBoxF));

                    addPolygon("cell" + latBoxI + "_" + lngBoxI, cell, Color.GREEN, 0.3, debugPolygonIds);
                }
            }
        }

        // Advance to next item in live queue
        liveFullTracks.next();
    }

}
