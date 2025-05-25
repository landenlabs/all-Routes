/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.map;

import android.graphics.Color;
import android.util.SparseArray;

import androidx.lifecycle.LifecycleOwner;

import com.landenlabs.routes.data.LiveQueue;
import com.landenlabs.routes.data.RouteSettings;
import com.landenlabs.routes.data.Track;
import com.landenlabs.routes.data.TrackGrid;
import com.landenlabs.routes.data.TrackIdList;
import com.landenlabs.routes.utils.GpsUtils;
import com.weather.pangea.geom.LatLng;
import com.weather.pangea.geom.Polygon;
import com.weather.pangea.geom.Polyline;
import com.weather.pangea.model.overlay.FillStyle;
import com.weather.pangea.model.overlay.FillStyleBuilder;
import com.weather.pangea.model.overlay.Overlay;
import com.weather.pangea.model.overlay.PolygonPathBuilder;
import com.weather.pangea.model.overlay.PolylinePathBuilder;
import com.weather.pangea.model.overlay.StrokeStyle;
import com.wsi.mapsdk.utils.WLatLngBounds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import landenlabs.wx_lib_data.logger.ALog;


/**
 * Manage rendering diagnostic data on map using Track paths.
 */
public class MapTracks {
    private  MapViewer mapViewer;
    private  TrackGrid trackGrid;
    private final LiveQueue<Track> liveFullTracks;
    private final HashMap<String, Overlay> mapOverlays = new HashMap<>();
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
        mapViewer.getTopLayer().clearOverlays();
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
        mapViewer = null;
        trackGrid = null;
        bounds = null;
        mapOverlays.clear();
    }

    public void addTrack(Track track) {
        Overlay existingOverlay = null; // mapOverlays.get(track.getKey());
        if (existingOverlay == null) {
            trackGrid.getTrackAsync(track.id, track).whenComplete((fullTrack, exception) -> {
                if (exception == null && fullTrack != null) {
                    liveFullTracks.postValue(fullTrack);
                }
            });
        } else {
            addOverlay(existingOverlay);
        }
    }

    private void addOverlay(Overlay newOverlay) {
        mapViewer.getTopLayer().addOverlay(newOverlay);
        // mapViewer.post(() -> mapViewer.getTopLayer().addOverlay(newOverlay));
    }

    public void observeTracks(Track fullTrack) {
        if (mapViewer == null || !mapViewer.isReady()) {
            return;
        }
        
        mapViewer.getTopLayer().clearOverlays();
        for (Overlay overlay : mapOverlays.values()) {
            addOverlay(overlay);
        }

        // fullTrack.count = -1;
        bounds = GpsUtils.union(bounds, fullTrack.getBounds());
        bounds = GpsUtils.minBounds(bounds, RouteSettings.minBoundsDeg);
        mapViewer.setCameraBounds(bounds, 1000);

        if (true) {
            // Draw track path.
            StrokeStyle lineStyle = RouteSettings.lineStyleStd;
            if (fullTrack.name.contains(Track.NAME_REV))
                lineStyle = RouteSettings.lineStyleRev;
            else if (fullTrack.name.contains(Track.NAME_TEST))
                lineStyle = RouteSettings.lineStyleTest;
            Overlay newOverlay = new PolylinePathBuilder()
                    .setPolyLine(fullTrack.toPolyline())
                    .setStrokeStyle(lineStyle)
                    .build();
            mapOverlays.put(fullTrack.getKey(), newOverlay);
            addOverlay(newOverlay);
        }

        float step = 1f / TrackGrid.scale1;
        if (showGrid && bounds != null) {
            // Draw Grid lines
            float minLat = TrackGrid.truncate(bounds.southwest.latitude) - step;
            float maxLat = TrackGrid.truncate(bounds.northeast.latitude) + step;
            float minLng = TrackGrid.truncate(bounds.southwest.longitude) - step;
            float maxLng = TrackGrid.truncate(bounds.northeast.longitude) + step;
            for (float lat = minLat; lat <= maxLat; lat += step) {
                ArrayList<LatLng> polyline = new ArrayList<>((int) ((maxLng - minLng) / step) + 1);
                for (float lng = minLng; lng <= maxLng; lng += step) {
                    polyline.add(new LatLng(lat, lng));
                }
                addOverlay(new PolylinePathBuilder()
                        .setPolyLine(new Polyline(polyline))
                        .setStrokeStyle(RouteSettings.gridStyleRev)
                        .build());
            }
            for (float lng = minLng; lng <= maxLng; lng += step) {
                ArrayList<LatLng> polyline = new ArrayList<>((int) ((maxLng - minLng) / step) + 1);
                for (float lat = minLat; lat <= maxLat; lat += step) {
                    polyline.add(new LatLng(lat, lng));
                }
                addOverlay(new PolylinePathBuilder()
                        .setPolyLine(new Polyline(polyline))
                        .setStrokeStyle(RouteSettings.gridStyleRev)
                        .build());
            }
        }

        if (showBounds && bounds != null) {
            // Draw bounding box.
            ArrayList<Polyline> polylines = new ArrayList<>(1);
            LatLng southEast = new LatLng(bounds.northeast.latitude, bounds.southwest.longitude);
            LatLng northWest = new LatLng(bounds.southwest.latitude, bounds.northeast.longitude);

            polylines.add(new Polyline(Arrays.asList(
                    northWest,
                    bounds.northeast.toPangeaLL(),
                    southEast,
                    bounds.southwest.toPangeaLL(),
                    northWest)));

            FillStyle bndsStyle = new FillStyleBuilder().setColor(Color.RED).setOpacity(0.3f).build();
            addOverlay(new PolygonPathBuilder()
                    .setPolygon(new Polygon(polylines))
                    .setFillStyle(bndsStyle)
                    .build());
        }

        if (showCells && bounds != null) {
            // Draw grid cells
            FillStyle boxStyle = new FillStyleBuilder().setColor(Color.GREEN).setOpacity(0.3f).build();
            for (int latIdx = 0; latIdx < trackGrid.grid.size(); latIdx++) {
                int latBoxI = trackGrid.grid.keyAt(latIdx);
                float latBoxF = latBoxI / (float) TrackGrid.scale3 - 90f;
                SparseArray<TrackIdList> lngArray = trackGrid.grid.valueAt(latIdx);

                for (int lngIdx = 0; lngIdx < lngArray.size(); lngIdx++) {
                    int lngBoxI = lngArray.keyAt(lngIdx);
                    float lngBoxF = lngBoxI / (float) TrackGrid.scale3 - 180f;

                    ArrayList<Polyline> boxlines = new ArrayList<>(1);
                    boxlines.add(new Polyline(Arrays.asList(
                            new LatLng(latBoxF, lngBoxF),
                            new LatLng(latBoxF + step, lngBoxF),
                            new LatLng(latBoxF + step, lngBoxF + step),
                            new LatLng(latBoxF, lngBoxF + step),
                            new LatLng(latBoxF, lngBoxF)
                    )));

                    addOverlay(new PolygonPathBuilder()
                            .setPolygon(new Polygon(boxlines))
                            .setFillStyle(boxStyle)
                            .build());
                }
            }
        }

        // Advance to next item in live queue
        liveFullTracks.next();
    }

}
