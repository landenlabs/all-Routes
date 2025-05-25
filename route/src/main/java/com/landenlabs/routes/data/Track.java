/*
 * IBM Confidential
 * Copyright IBM Corp. 2016, 2021. Copyright WSI Corporation 1998, 2015
 */

package com.landenlabs.routes.data;

import static com.landenlabs.routes.data.GpsPoint.NO_POINT;
import static com.landenlabs.routes.map.MapUtils.isSimilar;
import static com.landenlabs.routes.utils.GpsUtils.metersBetween;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.landenlabs.routes.utils.UnitDistance;
import com.landenlabs.routes.utils.UnitSpeed;
import com.weather.pangea.geom.Polyline;
import com.wsi.mapsdk.utils.WLatLng;
import com.wsi.mapsdk.utils.WLatLngBounds;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Manage an array of GPS points which define a track traveled.
 */
public class Track {

    public static final String NAME_REV = "Rev";
    public static final String NAME_TEST = "Test";
    public static final String NAME_SAME = "Same";

    private static final long BASE_MIN = TimeUnit.DAYS.toMinutes((2020 - 1970) * 365);
    private static final long NO_ID = -1;
    /**
     * Latitude/Longitude accuracy
     * <p>
     * https://gis.stackexchange.com/questions/8650/measuring-accuracy-of-latitude-and-longitude
     * places  degrees      N/S or E/W     E/W at         E/W at       E/W at
     * at                   equator        lat=23N/S      lat=45N/S    lat=67N/S
     * ------- -------      ----------     ----------     ---------    ---------
     * 0       1            111.32 km      102.47 km      78.71 km     43.496 km
     * 1       0.1          11.132 km      10.247 km      7.871 km     4.3496 km
     * 2       0.01         1.1132 km      1.0247 km      787.1 m      434.96 m
     * 3       0.001        111.32 m       102.47 m       78.71 m      43.496 m
     * 4       0.0001       11.132 m       10.247 m       7.871 m      4.3496 m
     */
    private static final double GPS_SCALE = 1e4;    // TODO - move to RouteSettings;

    // -- Data
    public long id = NO_ID;
    public String name;

    // Sync class when accessing points directly.
    public ArrayGps points = new ArrayGps();

    // -- Computed
    public double meters = -1;
    public WLatLngBounds bounds = null;
    public double metersPerSecond = -1;
    public int summaryPtSize = -1;
    public boolean isSummary = false;

    // ---------------------------------------------------------------------------------------------

    public Track() {
        id = NO_ID;
        name = "";
    }

    public Track(@NonNull ArrayGps points) {
        this.id = NO_ID;
        this.points = points;
        this.name = getDefName(getMilliStart());
    }

    public Track(long id, @NonNull String name, @NonNull ArrayGps points) {
        this.id = id;
        this.name = name;
        this.points = points;
    }

    public Track(@NonNull Track track) {
        id = track.id;
        name = track.name;
        points = track.points;
        meters = track.meters;
        bounds = track.bounds;
        metersPerSecond = track.metersPerSecond;
        summaryPtSize = track.summaryPtSize;
        isSummary = track.isSummary;
    }

    public static boolean isValid(@Nullable Track track) {
        return track != null && track.isValid();
    }

    public boolean isValid() {
        return points != null && points.size() > 0;
    }


    synchronized
    @NonNull
    public Track clone(@NonNull Track track) {
        synchronized (track) {
            if (name.equals(track.name)) {
                // update track
                for (int idx = track.points.size(); idx < points.size(); idx++) {
                    track.points.add(points.get(idx));
                }
            } else {
                track.id = id;
                track.name = name;
                track.points = new ArrayGps(points);
            }
            track.meters = meters;
            track.bounds = bounds;
            track.metersPerSecond = metersPerSecond;
            track.summaryPtSize = summaryPtSize;
            track.isSummary = isSummary;
        }
        return track;
    }

    synchronized
    @NonNull
    public Track clone() {
        Track newTrack = new Track(id, name, new ArrayGps(points));
        newTrack.meters = meters;
        newTrack.bounds = bounds;
        newTrack.metersPerSecond = metersPerSecond;
        newTrack.summaryPtSize = summaryPtSize;
        newTrack.isSummary = isSummary;
        return newTrack;
    }

    @NonNull
    @Override
    public String toString() {
        return "Track:" + logString();
    }

    @NonNull
    public String logString() {
        return String.format(Locale.US, "#Pts:%d Miles:%.0f, Min:%d, MPH:%.0f",
                getPointCnt(),
                UnitDistance.METERS.toMiles(getMeters()),
                getDurationMilli() / 60000,
                UnitSpeed.METERS_PER_SECOND.toMilesPerHour(getMetersPerSecond()));
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static String getDefName(long milli) {
        return new DateTime(milli).toString("EEE MMM/dd hh:mm");
    }

    synchronized
    @NonNull
    public String getPathEncoded() {
        return encodeGps(points);
    }

    synchronized
    public int getPointCnt() {
        return isSummarised() ? summaryPtSize : points.size();
    }

    synchronized
    public double getLatitudeStart() {
        return points.first(NO_POINT).latitude;
    }

    synchronized
    public double getLongitudeStart() {
        return points.first(NO_POINT).longitude;
    }

    synchronized
    public long getMilliStart() {
        return points.isEmpty() ? System.currentTimeMillis() : points.first(NO_POINT).milli;
    }

    synchronized
    public double getLatitudeEnd() {
        return points.last(NO_POINT).latitude;
    }

    synchronized
    public double getLongitudeEnd() {
        return points.last(NO_POINT).longitude;
    }

    synchronized
    public long getMilliEnd() {
        return points.isEmpty() ? System.currentTimeMillis() : points.last(NO_POINT).milli;
    }

    // -- Compute
    public double getLatitudeMin() {
        return getBounds().southwest.latitude;
    }

    public double getLongitudeMin() {
        return getBounds().southwest.longitude;
    }

    public double getLatitudeMax() {
        return getBounds().northeast.latitude;
    }

    public double getLongitudeMax() {
        return getBounds().northeast.longitude;
    }

    public long getDurationMilli() {
        return getMilliEnd() - getMilliStart();
    }

    public double getMeters() {
        if (meters < 0 || !isSummarised()) {
            synchronized (points) {
                meters = 0;
                GpsPoint pt1 = points.get(0, NO_POINT);
                for (int idx = 1; idx < points.size(); idx++) {
                    GpsPoint pt2 = points.get(idx, NO_POINT);
                    meters += metersBetween(pt1.latitude, pt1.longitude, pt2.latitude, pt2.longitude);
                    pt1 = pt2;
                }
            }
        }
        return meters;
    }

    public double getMetersTo(int toIdx) {
        synchronized (points) {
            meters = 0;
            GpsPoint pt1 = points.get(0, NO_POINT);
            for (int idx = 1; idx < Math.min(points.size(), toIdx); idx++) {
                GpsPoint pt2 = points.get(idx, NO_POINT);
                meters += metersBetween(pt1.latitude, pt1.longitude, pt2.latitude, pt2.longitude);
                pt1 = pt2;
            }
            return meters;
        }
    }

    public double getMetersPerSecond() {
        if (metersPerSecond < 0 || !isSummarised()) {
            metersPerSecond = getMeters() / TimeUnit.MILLISECONDS.toSeconds(getDurationMilli());
        }
        return metersPerSecond;
    }

    public WLatLngBounds getBounds() {
        if (bounds == null || !isSummarised()) {
            WLatLngBounds.Builder builder = WLatLngBounds.builder();
            synchronized (points) {
                bounds = null;
                for (GpsPoint point : points) {
                    builder.include(new WLatLng(point.latitude, point.longitude));
                }
            }
            bounds = builder.build();
        }
        return bounds;
    }

    public boolean isSummarised() {
        return isSummary && (summaryPtSize >= points.size());
    }

    public void resetSummary() {
        // Force values to be re-computed from point array.
        meters = -1;
        bounds = null;
        metersPerSecond = -1;
        summaryPtSize = -1;
    }

    /**
     * Save memory and remove all of the internal Gps points.
     */
    synchronized
    public Track summarise() {
        if (points.size() > 0 && !isSummarised()) {
            resetSummary();
            getMeters();
            getBounds();
            summaryPtSize = points.size();
            ArrayGps summaryList = new ArrayGps(2);
            summaryList.add(points.first(NO_POINT).clone());
            summaryList.add(points.last(NO_POINT).clone());
            points = summaryList;
            isSummary = true;
        }
        return this;
    }

    /**
     * Clone and return reduced version without path points.
     */
    synchronized
    @NonNull
    public Track cloneSummary() {
        Track track = new Track(id, name, points);
        track.meters = meters;
        track.bounds = bounds;
        track.metersPerSecond = metersPerSecond;
        track.summaryPtSize = summaryPtSize;
        track.isSummary = isSummary;
        return track.summarise();
    }

    synchronized
    public GpsPoint get(int idx) {
        return points.get(idx, NO_POINT);
    }

    synchronized
    public void add(GpsPoint gpsPoint) {
        points.add(gpsPoint);
    }

    synchronized
    public void clear() {
        points.clear();
    }

    // ---------------------------------------------------------------------------------------------
    public static ArrayGps decodeGps(final String encodedPath) {
        int len = encodedPath.length();

        // For speed we preallocate to an upper bound on the final length, then
        // truncate the array before returning.
        final ArrayGps path = new ArrayGps();
        int index = 0;
        int min = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            min += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            path.add(new GpsPoint(TimeUnit.MINUTES.toMillis(min + BASE_MIN), lat / GPS_SCALE, lng / GPS_SCALE));
        }

        return path;
    }

    @NonNull
    public static String encodeGps(final ArrayGps path) {
        long lastLat = 0;
        long lastLng = 0;
        long lastMin = BASE_MIN;

        final StringBuffer result = new StringBuffer();

        for (final GpsPoint point : path) {
            long min = TimeUnit.MILLISECONDS.toMinutes(point.milli);
            long lat = Math.round(point.latitude * GPS_SCALE);
            long lng = Math.round(point.longitude * GPS_SCALE);

            long dMin = min - lastMin;
            long dLat = lat - lastLat;
            long dLng = lng - lastLng;

            encode(dMin, result);
            encode(dLat, result);
            encode(dLng, result);

            lastMin = min;
            lastLat = lat;
            lastLng = lng;
        }
        return result.toString();
    }

    private static void encode(long v, StringBuffer result) {
        v = v < 0 ? ~(v << 1) : v << 1;
        while (v >= 0x20) {
            result.append(Character.toChars((int) ((0x20 | (v & 0x1f)) + 63)));
            v >>= 5;
        }
        result.append(Character.toChars((int) (v + 63)));
    }

    synchronized
    public Polyline toPolyline() {
        List<com.weather.pangea.geom.LatLng> polyPoints = new ArrayList<>();
        for (int idx = 0; idx < points.size(); idx++) {
            GpsPoint point = points.get(idx);
            polyPoints.add(new com.weather.pangea.geom.LatLng(point.latitude, point.longitude));
        }
        return new Polyline(polyPoints);
    }

    public String getKey() {
        return getName() + getId() + getMilliEnd();
    }

    synchronized
    public Track reverseTack() {
        ArrayGps pointsRev = new ArrayGps(points);
        int cnt = points.size();
        int idx2 = cnt - 1;
        for (int idx1 = 0; idx1 < idx2; idx1++, idx2--) {
            pointsRev.set(idx1, points.get(idx2).clone());
            pointsRev.set(idx2, points.get(idx1).clone());
        }
        for (int idx = 0; idx < cnt; idx++) {
            pointsRev.get(idx).milli = points.get(idx).milli;
        }
        return new Track(-1, NAME_REV + name, pointsRev);
    }

    synchronized
    public Track addMilli(long addMillis) {
        for (int idx = 0; idx < points.size(); idx++) {
            points.get(idx).milli += addMillis;
        }
        return this;
    }

    public boolean similar(Track o2) {
        return isSimilar(getLatitudeStart(), getLongitudeStart(), o2.getLatitudeStart(), o2.getLongitudeStart())
                && isSimilar(getLatitudeEnd(), getLongitudeEnd(), o2.getLatitudeEnd(), o2.getLongitudeEnd());
    }

    public boolean similarReverse(Track o2) {
        return isSimilar(getLatitudeEnd(), getLongitudeEnd(), o2.getLatitudeStart(), o2.getLongitudeStart())
                && isSimilar(getLatitudeStart(), getLongitudeStart(), o2.getLatitudeEnd(), o2.getLongitudeEnd());
    }

    public void setPath(GpsPoint firstGps, GpsPoint lastGps, int maxSteps, double minMeters) {
        clear();
        int depth = 0;
        add(firstGps);
        addBetween(firstGps, lastGps, maxSteps, minMeters, depth);
        add(lastGps);
    }

    private void addBetween(GpsPoint firstGps, GpsPoint lastGps, int maxSteps, double minMeters, int depth) {
        GpsPoint midGps = GpsBetween(firstGps, lastGps);
        depth++;
        if (maxSteps < 2) return;
        double meters = metersBetweenGps(firstGps, midGps);
        if (meters < minMeters) return;
        addBetween(firstGps, midGps, maxSteps/2, minMeters, depth);
        add(midGps);
        addBetween(midGps, lastGps, maxSteps/2, minMeters, depth);
    }

    public double metersBetweenGps(GpsPoint gps1, GpsPoint gps2) {
        return metersBetween(gps1.latitude, gps1.longitude, gps2.latitude, gps2.longitude);
    }

    public GpsPoint GpsBetween(GpsPoint gps1, GpsPoint gps2) {
        return new GpsPoint((gps1.latitude + gps2.latitude) / 2, (gps1.longitude + gps2.longitude) / 2);
    }

}

