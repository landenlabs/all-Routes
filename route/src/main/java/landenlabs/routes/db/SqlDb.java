/*
 * DatabaseHandler - Java Class for Android
 * Created by G.Capelli on 1/5/2016
 * This file is part of BasicAirData GPS Logger
 *
 * Copyright (C) 2011 BasicAirData
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package landenlabs.routes.db;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import landenlabs.routes.data.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import landenlabs.wx_lib_data.logger.ALog;


/**
 * Manage a SQLite local database.
 *
 * Tables:
 * <li>Track</li>
 */
public class SqlDb extends SQLiteOpenHelper {

    // All Static variables

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    public static final String DATABASE_NAME = "RouteDb1";
    public static final String TABLE_TRACKS = "tracks";

    // Sql DB Columns names
    private static final String KEY_TRACK_ID = "id";                            // 0
    private static final String KEY_TRACK_NAME = "name";                        // 1
    private static final String KEY_TRACK_PATH = "path";                        // 2
    private static final String KEY_TRACK_START_LAT = "start_lat";              // 3
    private static final String KEY_TRACK_START_LNG = "start_lng";              // 4
    private static final String KEY_TRACK_START_MIN = "start_min";              // 5
    private static final String KEY_TRACK_END_LAT = "end_lat";                  // 6
    private static final String KEY_TRACK_END_LNG = "end_lng";                  // 7
    private static final String KEY_TRACK_END_MIN = "end_min";                  // 8
    private static final String KEY_TRACK_MIN_LAT = "min_lat";                  // 9
    private static final String KEY_TRACK_MIN_LNG = "min_lng";                  // 10
    private static final String KEY_TRACK_MAX_LAT = "max_lat";                  // 11
    private static final String KEY_TRACK_MAX_LNG = "max_lng";                  // 12
    private static final String KEY_TRACK_MINUTES = "minutes";                  // 13
    private static final String KEY_TRACK_MILES = "miles";                      // 14
    private static final String KEY_TRACK_NUM_POINTS = "num_pts";               // 15


    public SqlDb(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // ---------------------------------------------------------------------------------------------

    private static long toMin(long milli) {
        return TimeUnit.MILLISECONDS.toMinutes(milli);
    }

    /**
     * Creates the tables to store Tracks.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TRACKS_TABLE = "CREATE TABLE " + TABLE_TRACKS + "("
                + KEY_TRACK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"  // 0
                + KEY_TRACK_NAME + " TEXT,"                         // 1
                + KEY_TRACK_PATH + " TEXT,"                         // 2
                + KEY_TRACK_START_LAT + " REAL,"                    // 3
                + KEY_TRACK_START_LNG + " REAL,"                    // 4
                + KEY_TRACK_START_MIN + " LONG,"                    // 5
                + KEY_TRACK_END_LAT + " REAL,"                      // 6
                + KEY_TRACK_END_LNG + " REAL,"                      // 7
                + KEY_TRACK_END_MIN + " LONG,"                      // 8
                + KEY_TRACK_MIN_LAT + " REAL,"                      // 9
                + KEY_TRACK_MIN_LNG + " REAL,"                      // 10
                + KEY_TRACK_MAX_LAT + " REAL,"                      // 11
                + KEY_TRACK_MAX_LNG + " REAL,"                      // 12
                + KEY_TRACK_MINUTES + " REAL,"                      // 13
                + KEY_TRACK_MILES + " REAL,"                        // 14
                + KEY_TRACK_NUM_POINTS + " INTEGER" + ")";         // 15
        db.execSQL(CREATE_TRACKS_TABLE);
    }

    /**
     * Upgrade the database version, altering the corresponding tables.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

// ----------------------------------------------------------------------- LOCATIONS AND PLACEMARKS

    private ContentValues buildRecord(Track track) {
        ContentValues trkvalues = new ContentValues();
   //     trkvalues.put(KEY_TRACK_ID, track.getName());                          // 0
        trkvalues.put(KEY_TRACK_NAME, track.getName());                        // 1
        trkvalues.put(KEY_TRACK_PATH, track.getPathEncoded());                 // 2
        // --- following fields can be computed from path.
        trkvalues.put(KEY_TRACK_START_LAT, track.getLatitudeStart());          // 3
        trkvalues.put(KEY_TRACK_START_LNG, track.getLongitudeStart());         // 4
        trkvalues.put(KEY_TRACK_START_MIN, toMin(track.getMilliStart()));      // 5
        trkvalues.put(KEY_TRACK_END_LAT, track.getLatitudeEnd());              // 6
        trkvalues.put(KEY_TRACK_END_LNG, track.getLongitudeEnd());             // 7
        trkvalues.put(KEY_TRACK_END_MIN, toMin(track.getMilliEnd()));          // 8
        trkvalues.put(KEY_TRACK_MIN_LAT, track.getLatitudeMin());              // 9
        trkvalues.put(KEY_TRACK_MIN_LNG, track.getLongitudeMin());             // 10
        trkvalues.put(KEY_TRACK_MAX_LAT, track.getLatitudeMax());              // 11
        trkvalues.put(KEY_TRACK_MAX_LNG, track.getLongitudeMax());             // 12
        trkvalues.put(KEY_TRACK_MINUTES, track.getDurationMilli());            // 13
        trkvalues.put(KEY_TRACK_MILES, track.getMeters());                     // 14
        trkvalues.put(KEY_TRACK_NUM_POINTS, track.getPointCnt());              // 15
        return trkvalues;
    }

    /**
     * Updates a track record using the given Track data.
     *
     * @param track the Track containing the new values to store
     */
    public long updateTrack(Track track) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues trkvalues = buildRecord(track);
        long rowCnt = -1;

        try {
            db.beginTransaction();
            rowCnt = db.update(TABLE_TRACKS, trkvalues, KEY_TRACK_ID + " = ?",
                    new String[]{String.valueOf(track.getId())});    // Update the corresponding Track
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return (rowCnt == 1) ? track.getId() : -1;
    }

    /**
     * Adds a new track to the Database.
     *
     * @param track the Track to be written into the new record
     * @return the ID of the new Track
     */
    public long addTrack(Track track) {
        if (track.id != -1) {
            track.id = updateTrack(track);
        }
        if (track.id == -1) {
            SQLiteDatabase db = this.getWritableDatabase();
            ContentValues trkvalues = buildRecord(track);
            track.id = db.insert(TABLE_TRACKS, null, trkvalues);
        }
        return track.id;
    }

    /**
     * Deletes the track with the specified ID.
     * The method deletes also Placemarks and Locations associated to the specified track.
     *
     * @param trackID the ID of the Track
     */
    public void deleteTrack(long trackID) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(TABLE_TRACKS, KEY_TRACK_ID + " = ?", new String[]{String.valueOf(trackID)});    // Delete track
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void clearTracks() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.beginTransaction();
            db.delete(TABLE_TRACKS, null, null );    // Delete all track
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Get Track
    public Track getTrack(long trackId) {
        Track track = null;

        String selectQuery = "SELECT  * FROM " + TABLE_TRACKS + " WHERE "
                + KEY_TRACK_ID + " = " + trackId;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                track = new Track(cursor.getInt(0),             //  0 = id
                        cursor.getString(1),                    //  1 = name
                        Track.decodeGps(cursor.getString(2)));  //  2 = path
            }
            cursor.close();
        }
        return track;
    }

    public CompletableFuture<Track> getTrackAsync(long trackId) {
        return CompletableFuture.supplyAsync(() -> {
            return getTrack(trackId);
        });
    }

    // Get last TrackID
    public int getLastTrackID() {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = 0;

        String query = "SELECT " + KEY_TRACK_ID + " FROM " + TABLE_TRACKS + " ORDER BY " + KEY_TRACK_ID + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result = cursor.getInt(0);
            }
            cursor.close();
        }
        return result;
    }

    public CompletableFuture<Integer> getLastTrackIDAsync(long trackId) {
        return CompletableFuture.supplyAsync(this::getLastTrackID);
    }

    // Getting a list of Tracks, with number between startNumber and endNumber
    // Please note that limits both are inclusive!
    public List<Track> getTracksList(long startNumber, long endNumber, boolean summary) {

        List<Track> trackList = new ArrayList<>();

        String selectQuery = "SELECT  * FROM " + TABLE_TRACKS + " WHERE "
                + KEY_TRACK_ID + " BETWEEN " + startNumber + " AND " + endNumber
                + " ORDER BY " + KEY_TRACK_ID + " DESC";

        //Log.w("myApp", "[#] DatabaseHandler.java - getTrackList(" + startNumber + ", " +endNumber + ") ==> " + selectQuery);

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor != null) {
            // looping through all rows and adding to list
            if (cursor.moveToFirst()) {
                do {
                    try {
                        Track trk = new Track(cursor.getInt(0),         //  0 = id
                                cursor.getString(1),                    //  1 = name
                                Track.decodeGps(cursor.getString(2)));  //  2 = path
                        if (summary)
                            trk.summarise();
                        trackList.add(trk);             // Add Track to list
                    } catch (Exception ex) {
                        ALog.e.tagMsg(this, "Failed to load SQL tracks ", ex);
                    }
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
        return trackList;
    }

    public CompletableFuture<List<Track>> getTrackListAsync(long startNumber, long endNumber, boolean summary) {
        return CompletableFuture.supplyAsync(() -> getTracksList(startNumber, endNumber, summary));
    }

    public static class TrackResponse {
        public Track track;
        public Exception exception;

        public TrackResponse(Track track, Exception exception) {
            this.track = track;
            this.exception = exception;
        }
    }
}