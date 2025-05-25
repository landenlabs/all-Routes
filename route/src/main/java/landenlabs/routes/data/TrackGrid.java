/*
 * Dennis Lang - LanDenLabs.com
 * Copyright LanDenLabs 2025
 */

package com.landenlabs.routes.data;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.routes.db.SqlDb;
import com.landenlabs.routes.events.EventBase;
import com.landenlabs.routes.events.EventStatus;
import com.wsi.mapsdk.utils.WLatLng;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import landenlabs.wx_lib_data.logger.ALog;

/**
 * Spatial access to tracks using quantized path point grid coordinates which contain list of tracks.
 */
public class TrackGrid {
    public static final int scale1 = 100;      // keep 2 decimal digits = 1.11km at equator
    public static final int scale2 = 1000;
    public static final int scale3 = scale1*scale2;     // Total scale = scale1*scale2 = 5 digits

    private final SqlDb gpsDataBase;
    private final SharedPreferences pref;
    private final LiveQueue<EventBase> liveDataQueue;

    public final SparseArray<SparseArray<TrackIdList>> grid;
    private final SparseArray<Track> summaryTracks = new SparseArray<>();

    private static final String PREF_CAPACITY_LAT = "tg_cap_lat";

    // ---------------------------------------------------------------------------------------------

    public TrackGrid(SqlDb gpsDataBase, SharedPreferences pref, LiveQueue<EventBase> liveDataQueue) {
        this.gpsDataBase = gpsDataBase;
        this.pref = pref;
        this.liveDataQueue = liveDataQueue;
        int capacityX = pref.getInt(PREF_CAPACITY_LAT, 10);
        grid = new SparseArray<>(capacityX);
    }

    public int getTrackCnt() {
        return summaryTracks.size();
    }

    public void clearAll() {
        gpsDataBase.clearTracks();
        grid.clear();
        summaryTracks.clear();
    }

    public long saveTrack(Track track) {
        // if (find(track, 0) == null) {
            long id = gpsDataBase.addTrack(track);
            if (id != -1)
                addTrack(track);
            return id;
        // }
        // return -1;  // duplicate ignored
    }

    public TrackCommon.TrackScore findMatches(@NonNull Track track) {
        synchronized (track) {
            TrackCommon commonIds = new TrackCommon();
            if (false) {
                // Optimize - only test start and end.
                commonIds.add(track.getLatitudeStart(), track.getLongitudeStart(), this);
                if (track.points.size() > 1) {
                    commonIds.add(track.getLatitudeEnd(), track.getLongitudeEnd(), this);
                }
            } else {
                // Accurate - test multiple points.
                int TEST_CNT = 10;
                int step = Math.max(1, track.points.size() / TEST_CNT);
                for (int idx = 0; idx < track.points.size(); idx += step) {
                    commonIds.add(track.points.get(idx), this);
                }
            }

            // commonIds.keepBest(20);
            return commonIds.getScores(this);
        }
    }

    public TrackCommon getCellInfo(@NonNull WLatLng latLng) {
        TrackCommon commonIds = new TrackCommon();
        commonIds.add(latLng.latitude, latLng.longitude, this);
        commonIds.getScores(this);
        return commonIds;
    }

    public static class TrackAndPos {
        public Track track;
        public int pos;
        public TrackAndPos(@NonNull Track track, int pos) {
            this.track = track;
            this.pos = pos;
        }
    }
    @Nullable
    public TrackAndPos find(@NonNull Track track, int startAt) {
        synchronized (summaryTracks) {
            for (int idx = startAt; idx < summaryTracks.size(); idx++) {
                Track other = summaryTracks.get(idx);
                if (track.id == other.id || track.similar(other) || track.similarReverse(other)) {
                    return new TrackAndPos(track, idx);
                }
            }
        }
        return null;
    }

    @Nullable
    public Track getTrack(int id) {
        synchronized (summaryTracks) {
            return summaryTracks.get(id, null);
        }
    }

    /**
     * Load a full track or return provided track if already full.
     */
    public CompletableFuture<Track> getTrackAsync(long trackId, @Nullable Track track) {
        if (track == null || track.isSummary) {
            return gpsDataBase.getTrackAsync(trackId);
        }
        return CompletableFuture.completedFuture(track);
    }

    public void loadTracksAsync(boolean summary) {
        gpsDataBase.getTrackListAsync(0, Integer.MAX_VALUE, summary)
                .whenComplete((input, exception) -> {
                    summaryTracks.clear();
                    if (exception != null) {
                        ALog.d.tagMsg(this, exception);
                        liveDataQueue.postValue(new EventStatus(ALog.getErrorMsg(exception), ALog.ERROR));
                    } else {
                        if (input != null) {
                            for (Track track : input) {
                                summaryTracks.put((int)track.id, track);
                            }
                        }
                        ALog.d.tagMsg(this, summaryTracks);
                        liveDataQueue.postValue(new EventStatus("Got Track List", ALog.INFO));
                    }
                });
    }

    public void loadTracks( ) {
        CompletableFuture.supplyAsync(this::buildGridFromDbTracks).whenComplete( (grid, exception) -> {
            if (exception != null) {
                ALog.d.tagMsg(this, exception);
                liveDataQueue.postValue(new EventStatus(ALog.getErrorMsg(exception), ALog.ERROR));
            } else {
                ALog.d.tagMsg(this, summaryTracks);
                liveDataQueue.postValue(new EventStatus("Loaded Tracks", ALog.INFO));
            }
        });
    }

    public TrackGrid buildGridFromDbTracks() {
        grid.clear();
        summaryTracks.clear();
        String selectQuery = "SELECT  * FROM " + SqlDb.TABLE_TRACKS;

        SQLiteDatabase db = gpsDataBase.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    try {
                        Track trk = new Track(cursor.getInt(0),         //  0 = id
                                cursor.getString(1),                    //  1 = name
                                Track.decodeGps(cursor.getString(2)));  //  2 = path
                        addTrack(trk);
                    } catch (Exception ex) {
                        ALog.e.tagMsg(this, "Failed to load SQL tracks ", ex);
                    }
                } while (cursor.moveToNext());
                pref.edit().putInt(PREF_CAPACITY_LAT, grid.size()).apply();
            }
            cursor.close();
        }
        return this;
    }


    /*
     * Latitude/Longitude accuracy
     *
     https://gis.stackexchange.com/questions/8650/measuring-accuracy-of-latitude-and-longitude
     places  degrees      N/S or E/W     E/W at         E/W at       E/W at
     at                   equator        lat=23N/S      lat=45N/S    lat=67N/S
     ------- -------      ----------     ----------     ---------    ---------
     0       1            111.32 km      102.47 km      78.71 km     43.496 km
     1       0.1          11.132 km      10.247 km      7.871 km     4.3496 km
     2       0.01         1.1132 km      1.0247 km      787.1 m      434.96 m
     3       0.001        111.32 m       102.47 m       78.71 m      43.496 m
     4       0.0001       11.132 m       10.247 m       7.871 m      4.3496 m
     */

    synchronized
    public void addTrack(@NonNull Track track ) {

        // TODO - only add if not duplicate, if duplicate track DOW and time.
        TrackCells trackCells = new TrackCells(grid);
        GridPos prevGridPos = null;
        GpsPoint prevGpsPos = null;

        for (int idx = 0; idx < track.points.size(); idx++) {
            // Quantize to 2 decimal (scale1) for now, with room to goto 5 (scale1+scale2)
            // "int" can hold ~9 digits (Integer.MAX_VALUE 0x7FFFFFFF is 2,147,483,647)
            /*
            GridPos gridPos = new GridPos(track.points.get(idx), scale1, scale2);
            trackCells.updateCells(prevGridPos, gridPos, track);
            prevGridPos = gridPos;
             */
            GpsPoint newGpsPos = track.points.get(idx);
            trackCells.updateCells(prevGpsPos, newGpsPos, track);
            prevGpsPos = newGpsPos;
        }
        // pref.edit().putInt(PREF_CAPACITY_LAT, grid.size()).apply();
        summaryTracks.put((int)track.id, track.cloneSummary());  // Reduce memory, remove path points.
    }

    public static float truncate(double val) {
        return (int)(val * scale1)/(float)scale1;
    }

    public void removeDuplicateTrack(Track newTrack) {
        // TODO - remove or merge duplicate track in SQL to prevent bloating.
        // How do we merge Day-of-week and Time ?
    }

    synchronized
    public void deleteTrack(long id) {
        summaryTracks.remove((int)id);
        gpsDataBase.deleteTrack(id);    // TODO async
    }

    public SparseArray<Track> getSummaryTracks() {
        return summaryTracks;
    }

    // ==============================================================================================
    public static class GridPos {
        public int latDegI;
        public int lngDegI;

        public GridPos(GpsPoint val, int scale1, int scale2) {
            latDegI = doScale((val.latitude + 90)%180, scale1, scale2);  // 0..180
            lngDegI = doScale((val.longitude+180)%360, scale1, scale2);  // 0..360
        }
        public GridPos(double lat, double lng, int scale1, int scale2) {
            latDegI = doScale((lat + 90)%180, scale1, scale2);  // 0..180
            lngDegI = doScale((lng+180)%360, scale1, scale2);  // 0..360
        }
        public GridPos(int latDegI, int lngDegI) {
            this.latDegI = latDegI;
            this.lngDegI = lngDegI;
        }
        public static int doScale(double valueF, int scale1, int scale2) {
            return (int) (valueF * scale1) * scale2;
        }

        @NonNull
        @Override
        public String toString() {
            float latDegF = latDegI / (float) TrackGrid.scale3 - 90f;
            float lngDegF = lngDegI / (float) TrackGrid.scale3 - 180f;
            return String.format(Locale.US, "GridPos[%d, %d] GpsPos[%.4f,%.4f]", latDegI, lngDegI, latDegF, lngDegF);
        }
    }

    // ==============================================================================================
    static class TrackCells {
        SparseArray<TrackIdList> addArrayIfEmpty = new SparseArray<>();
        TrackIdList addListIfEmpty = new TrackIdList();
        final SparseArray<SparseArray<TrackIdList>> grid;

        public TrackCells(SparseArray<SparseArray<TrackIdList>> grid) {
            this.grid = grid;
        }

        public void updateCells(GpsPoint prevGpsPos, GpsPoint endGpsPos, Track track) {
            GridPosIter iter = new GridPosIter(prevGpsPos, endGpsPos);
            while (iter.hasMore()) {
                updateCell(iter.next(), track);
            }
            updateCell(new GridPos(endGpsPos, scale1, scale2), track);
        }

        public void updateCell(GridPos gridPos, Track track) {
            // ALog.d.tagMsg(this, "updateCell at ", gridPos);
            SparseArray<TrackIdList> latArray = grid.get(gridPos.latDegI, addArrayIfEmpty);
            if (latArray == addArrayIfEmpty) {
                grid.put(gridPos.latDegI, latArray);
                addArrayIfEmpty = new SparseArray<>();
            }
            TrackIdList lngIdList = latArray.get(gridPos.lngDegI, addListIfEmpty);
            if (lngIdList == addListIfEmpty) {
                latArray.put(gridPos.lngDegI, lngIdList);
                addListIfEmpty = new TrackIdList();
            }
            lngIdList.ids.add((int) track.id); // TODO - which is correct: "int" or "long"
        }
    }

    // ==============================================================================================
    static class GridPosIter {  // Linear
        final GpsPoint begPos;
        double dLatDeg;
        double dLngDeg;
        int cnt;
        int idx = 0;

        public GridPosIter(@Nullable GpsPoint begPos, @NonNull GpsPoint endPos) {
            this.begPos = begPos;
            if (begPos != null) {
                dLatDeg = (endPos.latitude - begPos.latitude);
                dLngDeg = (endPos.longitude - begPos.longitude);
                cnt = (int)Math.round(Math.max(Math.abs(dLatDeg), Math.abs(dLngDeg)) * scale1);
                if (cnt > 0) {
                    cnt += 1;
                    dLatDeg /= cnt;
                    dLngDeg /= cnt;
                }
            } else {
                dLatDeg = dLngDeg = cnt = 0;
            }
        }

        public boolean hasMore() {
            return idx < cnt;
        }
        public GridPos next() {
            idx++;
            double latDegF = (begPos.latitude + dLatDeg * idx);
            double lngDegF = (begPos.longitude + dLngDeg * idx);
            return new GridPos(latDegF, lngDegF, scale1, scale2);
        }
    }
}
